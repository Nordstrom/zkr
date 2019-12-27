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
import java.util.zip.Adler32
import java.util.zip.Checksum
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
            logger.info("excluding: ${options.exclude}")
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
                processTxn(hdr, txn)

                if (stream.readByte("EOR") != 'B'.toByte()) {
                    logger.info("Last transaction was partial.")
                    throw EOFException("Last transaction was partial.")
                }
                numTxn++
            } //-while
        } //-time

        logger.info("$numTxn transactions in ${Duration.ofMillis(time)}")
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

} //-Zkr
