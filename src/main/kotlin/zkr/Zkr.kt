package zkr

import kotlinx.coroutines.Runnable
import org.apache.jute.BinaryInputArchive
import org.apache.jute.Record
import org.apache.zookeeper.server.util.SerializeUtils
import org.apache.zookeeper.txn.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import zkr.Zkr.Companion.VERSION
import java.io.EOFException
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.zip.Adler32
import java.util.zip.Checksum
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

@CommandLine.Command(
        name = "zkr",
        description = ["ZooKeeper Reaper - utility to view and replay ZooKeeper transaction logs and backups"],
        mixinStandardHelpOptions = true,
        version = [VERSION],
        subcommands = [CommandLine.HelpCommand::class],
        usageHelpWidth = 120,
        footer = ["v$VERSION"]
)
class Zkr : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions
    lateinit var zk: ZkClient

    companion object {
        const val VERSION = "0.1β"
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Zkr()).execute(*args)
            exitProcess(exitCode)
        } //-main
    } //-companion


    override fun run() {
        try {
            logger.info("excluding: ${options.excludes}")
            if (options.overwrite) logger.warn("overwriting nodes!")
            logger.debug("options: $options")
            zk = ZkClient(options)

            val stream = BinaryInputArchiveFactory(
                    txnLog = options.txnLog,
                    s3bucket = options.s3bucket,
                    s3region = options.s3region
            ).create()

            process(stream)

        } catch (e: Exception) {
            logger.error("$e")
        }
    }

    fun process(stream: BinaryInputArchive) {
        var numTxn = 0
        var isActive = true
        var earliest = Long.MAX_VALUE
        var latest = Long.MIN_VALUE
        val time = measureTimeMillis {
            while (isActive) {
                var crcValue: Long
                var bytes: ByteArray
                try {
                    crcValue = stream.readLong("crcvalue")
                    bytes = stream.readBuffer("txnEntry")
                } catch (e: EOFException) {
                    logger.error("EOF $e, after $numTxn txns")
                    isActive = false
                    break
                }
                if (bytes.isEmpty()) { // Since we preallocate, we define EOF to be an empty transaction
                    logger.info("ZooKeeper txn log: EOF")
                    isActive = false
                    break
                }
                val crc: Checksum = Adler32()
                crc.update(bytes, 0, bytes.size)
                if (crcValue != crc.value) {
                    throw IOException("CRC does not match " + crcValue + " vs " + crc.value)
                }

                val hdr = TxnHeader()
                val txn = SerializeUtils.deserializeTxn(bytes, hdr)
                earliest = min(earliest, hdr.time)
                latest = max(latest, hdr.time)
                if (!options.info) {
                    processTxn(hdr, txn)
                }

                if (stream.readByte("EOR") != 'B'.toByte()) {
                    logger.info("Last transaction was partial.")
                    throw EOFException("Last transaction was partial.")
                }
                numTxn++
            } //-while
        } //-time

//        val e = Date(earliest).toInstant()
//        val l = Date(latest).toInstant()
//        logger.info("$numTxn transactions in ${Duration.ofMillis(time)}")
//        logger.info("  from: $e")
//        logger.info("  to  : $l")
//        logger.info("  dur : ${Duration.between(e, l)}")
        logger.info("Summary ${summary(numTxn, Date(earliest).toInstant(), Date(latest).toInstant(), time)}")
    }

    fun processTxn(hdr: TxnHeader, txn: Record?) {
        when (txn) {
            //exhibitor: Create-Persistent
            is CreateTxn -> {
                ZNodeCreate(zk = zk, options = options).process(hdr, txn)
            }
            //exhibitor: Delete
            is DeleteTxn -> {
                ZNodeDelete(zk = zk, options = options).process(hdr, txn)
            }
            //exhibitor: SetData
            is SetDataTxn -> {
                ZNodeSetData(zk = zk, options = options).process(hdr, txn)
            }
            is SetACLTxn -> {
                ZNodeSetACL(zk = zk, options = options).process(hdr, txn)
            }
            is MultiTxn -> {
                txn.txns.forEach {
                    processTxn(hdr, it)
                }
            }
            else -> {
                ZNodeIgnore(zk = zk, options = options).process(hdr, txn)
            }
        }
    }

    private fun summary(numberTxn: Int, earliest: Instant, latest: Instant, millis: Long): String = """

  ,-----------.  
(_\  ZooKeeper \ title    : ${options.txnLog}
   | Reaper    | txn      : $numberTxn
   | Summary   | from     : $earliest
  _|           | to       : $latest
 (_/_____(*)___/ duration : ${Duration.between(earliest, latest)}
          \\     playback : ${Duration.ofMillis(millis)}
           ))
           ^
""".trimIndent()

} //-Zkr
