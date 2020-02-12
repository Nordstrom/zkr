package zkr

import ch.qos.logback.classic.Level
import org.apache.jute.BinaryInputArchive
import org.apache.jute.Record
import org.apache.zookeeper.server.util.SerializeUtils
import org.apache.zookeeper.txn.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
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
import kotlin.system.measureTimeMillis

@CommandLine.Command(
        name = "logs",
        description = ["View/write ZooKeeper/Exhibitor transaction logs and backups"],
        subcommands = [
            CommandLine.HelpCommand::class
        ],
        usageHelpWidth = 120
)
class Logs : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    @CommandLine.Mixin
    lateinit var restore: LogsOptions

    lateinit var zk: ZkClient

    override fun run() {
        Zkr.logLevel(this.javaClass.`package`.name, if (options.verbose) Level.DEBUG else Level.INFO)
        try {
            logger.info("excluding  : ${options.excludes}")
            if (restore.overwrite) logger.warn("overwrite  : ${restore.overwrite} !!")
            zk = ZkClient(
                    host = options.host,
                    connect = !(restore.info && restore.restore),
                    sessionTimeoutMillis = options.sessionTimeoutMs,
                    superDigestPassword = options.superDigestPassword
            )

            val stream = BinaryInputArchiveFactory(
                    txnLog = options.file,
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
                    logger.debug("ZooKeeper txn log: EOF")
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
                if (!restore.info) {
                    processTxn(hdr, txn)
                }

                if (stream.readByte("EOR") != 'B'.toByte()) {
                    logger.info("Last transaction was partial.")
                    throw EOFException("Last transaction was partial.")
                }
                numTxn++
            } //-while
        } //-time

        logger.info("Summary ${summary(numTxn, Date(earliest).toInstant(), Date(latest).toInstant(), time)}")
    }

    fun processTxn(hdr: TxnHeader, txn: Record?) {
        when (txn) {
            //exhibitor: Create-Persistent
            is CreateTxn -> {
                ZNodeTxnCreate(zk = zk, options = options, overwrite = restore.overwrite, restore = restore.restore).process(hdr, txn)
            }
            //exhibitor: Delete
            is DeleteTxn -> {
                ZNodeTxnDelete(zk = zk, options = options, restore = restore.restore).process(hdr, txn)
            }
            //exhibitor: SetData
            is SetDataTxn -> {
                ZNodeTxnSetData(zk = zk, options = options, restore = restore.restore).process(hdr, txn)
            }
            is SetACLTxn -> {
                ZNodeTxnSetACL(zk = zk, options = options, restore = restore.restore).process(hdr, txn)
            }
            is MultiTxn -> {
                txn.txns.forEach {
                    processTxn(hdr, it)
                }
            }
            else -> {
                ZNodeTxnIgnore(zk = zk, options = options).process(hdr, txn)
            }
        }
    }

    private fun summary(numberTxn: Int, earliest: Instant, latest: Instant, millis: Long): String = """

  ,-----------.  
(_\  ZooKeeper \ title    : ${options.file}
   | Reaper    | txn      : $numberTxn
   | Summary   | from     : $earliest
  _|           | to       : $latest
 (_/_____(*)___/ duration : ${Duration.between(earliest, latest)}
          \\     playback : ${Duration.ofMillis(millis)}
           ))
           ^
""".trimIndent()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}