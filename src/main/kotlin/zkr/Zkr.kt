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
import java.io.EOFException
import java.io.FileInputStream
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.util.zip.Adler32
import java.util.zip.Checksum
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

@CommandLine.Command(
        name = "zkr",
        description = ["ZooKeeper Reaper"],
        mixinStandardHelpOptions = true,
        version = ["0.1α"],
        subcommands = [
            CommandLine.HelpCommand::class
        ],
        usageHelpWidth = 120
)
class Zkr : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun main(args: Array<String>) {
            logger.debug("CROSSING.THE.STREAMS")
            val exitCode = CommandLine(Zkr()).execute(*args)
            logger.debug("TOTAL.PROTONIC.REVERSAL")
            exitProcess(exitCode)
        } //-main

    } //-companion

    override fun run() {
        logger.debug("STREAMS.CROSSED")

        try {
            val fis = FileInputStream(options.txnLog)
            val stream = BinaryInputArchive.getArchive(fis)
            val fhdr = FileHeader()
            fhdr.deserialize(stream, "fileheader")
            if (fhdr.magic != FileTxnLog.TXNLOG_MAGIC) {
                throw InvalidMagicNumberException("Invalid magic number for ${options.txnLog}")
            }
            println("ZooKeeper txn log: dbid=${fhdr.dbid}, format.version=${fhdr.version}")

            process(stream)

        } catch (e: Exception) {
            logger.error("ERR: $e")
        }

        logger.debug("PROTON.PACK.OVERLOAD")
    }

    private fun process(stream: BinaryInputArchive) {
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
                    println("ZooKeeper txn log: EOF")
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
                handleTxn(hdr, txn)

                if (stream.readByte("EOR") != 'B'.toByte()) {
                    System.err.println("Last transaction was partial.")
                    throw EOFException("Last transaction was partial.")
                }
                numTxn++
            } //-while
        } //-time

        println("$numTxn transactions in ${Duration.ofMillis(time)}")
    }

    private fun handleTxn(hdr: TxnHeader, txn: Record?) {
        logger.debug(".handle-txn: ${txn?.javaClass?.simpleName}")
        when (txn) {
            //exhibitor: Create-Persistent
            is CreateTxn -> {
                ZNodeCreate(options).process(hdr, txn)
            }
            //exhibitor: Delete
            is DeleteTxn -> {
                ZNodeDelete(options).process(hdr, txn)
            }
            //exhibitor: SetData
            is SetDataTxn -> {
                ZNodeSetData(options).process(hdr, txn)
            }
            is SetACLTxn -> {
                ZNodeSetACL(options).process(hdr, txn)
            }
            is MultiTxn -> {
                txn.txns.forEach {
                    handleTxn(hdr, it)
                }
            }
            else -> {
                val s = ZNode.txnHeaderString(hdr, txn)
                if (options.verbose || options.overwrite) println("IGNORE $s")
            }
        }
    }

} //-App
