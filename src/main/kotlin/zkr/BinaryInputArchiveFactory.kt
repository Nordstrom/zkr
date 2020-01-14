package zkr

import org.apache.jute.BinaryInputArchive
import org.apache.zookeeper.server.persistence.FileHeader
import org.apache.zookeeper.server.persistence.FileTxnLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.util.zip.GZIPInputStream

class BinaryInputArchiveFactory(
        val txnLog: String,
        val s3bucket: String = "",
        val s3region: String = "us-west-2") {

    fun create(): BinaryInputArchive {
        var stream: BinaryInputArchive
        stream = BinaryInputArchive(DataInputStream(inputStream()))
        var fhdr = FileHeader()
        fhdr.deserialize(stream, "fileheader")

        when (fhdr.magic) {
            FileTxnLog.TXNLOG_MAGIC -> {
                logger.info("Reading transaction log")
            }
            TXNARCHIVE_MAGIC -> {
                stream = BinaryInputArchive(DataInputStream(gzipInputStream()))
                fhdr = FileHeader()
                fhdr.deserialize(stream, "fileheader")
            }
            else -> {
                throw InvalidMagicNumberException("Invalid magic number for ${txnLog}")
            }
        }

        return stream
    }

    private fun inputStream(): InputStream {
        return if (s3bucket.isNotBlank()) {
            s3InputStream()
        } else {
            FileInputStream(txnLog)
        }
    }

    private fun gzipInputStream(): InputStream {
        logger.info("Reading gzip compressed transaction log")
        return GZIPInputStream(inputStream())
    }

    private fun s3InputStream(): InputStream {
        logger.info("Reading transaction log from S3: bucket=${s3bucket}, region=${s3region}, key=$txnLog")
        val region = Region.of(s3region)
        val s3 = S3Client.builder()
                .region(region)
                .build()
        //TODO parse versionId if '?' in file
        return s3.getObject(
                GetObjectRequest.builder()
                        .bucket(s3bucket)
                        .key(txnLog)
                        // TODO parse/set versionId if '?' in txnLog
                        .build(),
                ResponseTransformer.toInputStream()
        )
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private const val TXNARCHIVE_MAGIC = 0x1f8b0800
    }
} //-BinaryInputArchiveFactory
