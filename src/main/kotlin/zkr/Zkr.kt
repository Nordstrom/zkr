package zkr

import kotlinx.coroutines.Runnable
import org.apache.jute.BinaryInputArchive
import org.apache.jute.Record
import org.apache.zookeeper.server.persistence.FileHeader
import org.apache.zookeeper.server.persistence.FileTxnLog
import org.apache.zookeeper.server.util.SerializeUtils
import org.apache.zookeeper.txn.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.*
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.util.zip.Adler32
import java.util.zip.Checksum
import java.util.zip.GZIPInputStream
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

@CommandLine.Command(
        name = "zkr",
        description = ["ZooKeeper Reaper - utility to view and replay ZooKeeper transaction logs and backups"],
        mixinStandardHelpOptions = true,
        version = ["0.1α"],
        subcommands = [CommandLine.HelpCommand::class],
        usageHelpWidth = 120
)
class Zkr : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions
    lateinit var zk: ZkClient

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Zkr()).execute(*args)
            exitProcess(exitCode)
        } //-main

        const val TXNARCHIVE_MAGIC = 0x1f8b0800

    } //-companion


    override fun run() {
        try {
            logger.info("Excluding: ${options.exclude}")
            zk = ZkClient(options)
            logger.debug("options: $options")

            val stream = getArchive(options.txnLog)
            process(stream)

        } catch (e: Exception) {
            logger.error("$e")
        }
    }

    private fun getArchive(txnLog: String): BinaryInputArchive {
        val fis = FileInputStream(txnLog)
        var stream = BinaryInputArchive(DataInputStream(fis))
        var fhdr = FileHeader()
        fhdr.deserialize(stream, "fileheader")
        logger.debug("magic=${fhdr.magic} == 0x${fhdr.magic.toString(16)} == ${CliHelper.intToAscii(fhdr.magic)}")

        when (fhdr.magic) {
            FileTxnLog.TXNLOG_MAGIC -> {
            }
            TXNARCHIVE_MAGIC -> {
                logger.warn("Reading gzip compressed file")
                val gfis = FileInputStream(txnLog)
                val gzipStream = GZIPInputStream(gfis)
                stream = BinaryInputArchive(DataInputStream(gzipStream))
                fhdr = FileHeader()
                fhdr.deserialize(stream, "fileheader")
                logger.debug("magic=${fhdr.magic} == 0x${fhdr.magic.toString(16)} == ${CliHelper.intToAscii(fhdr.magic)}")
            }
            else -> {
                throw InvalidMagicNumberException("Invalid magic number for ${txnLog}")
            }
        }

        return stream
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

} //-App
