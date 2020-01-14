package zkr

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.*
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BackupArchiveOutputStream(val name: String, val compress: Boolean = false, val s3bucket: String = "", val s3region: String = "") : OutputStream() {
    private val os = ByteArrayOutputStream()
    var file: String

    init {
        val t0 = Instant.now()
        val timestamp = DATE_FORMATTER.format(t0)
        val suffix = if (compress) "gz" else "json"
        file = "${name}-$timestamp.$suffix"
    }

    override fun write(b: Int) {
        os.write(b)
    }

    override fun close() {
        os.close()

        var out: OutputStream = FileOutputStream(file)
        if (compress) {
            out = GZIPOutputStream(out)
        }
        out.write(os.toByteArray())
        out.close()

        // For S3, we must write to local file, then upload since api does not support OutputStream
        if (s3bucket.isNotEmpty()) {
            val path = Paths.get(file)
            val region = Region.of(s3region)
            val s3 = S3Client.builder()
                    .region(region)
                    .build()
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3bucket)
                            .key(file)
                            .build(),
                    RequestBody.fromFile(path)
            )
            Files.delete(Paths.get(file))
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddhhmmz").withZone(ZoneId.of("Z"))
    }
} //BackupArchiveOutputStream

class BackupArchiveInputStream(file: String, compress: Boolean = false, s3bucket: String = "", s3region: String = "") : InputStream() {
    private var ins: InputStream

    init {
        val std = (file == "-")
        ins = if (s3bucket.isNotEmpty()) {
            logger.info("Restoring from S3: bucket=${s3bucket}, region=${s3region}, key=$file")
            val region = Region.of(s3region)
            val s3 = S3Client.builder()
                    .region(region)
                    .build()
            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(s3bucket)
                            .key(file)
                            // TODO parse/set versionId if '?' in file
                            .build(),
                    ResponseTransformer.toInputStream()
            )
        } else {
            logger.info("Restoring from file: $file")
            BufferedInputStream(FileInputStream(file))
        }

        if (compress) {
            logger.info("Restoring compressed archive")
            ins = GZIPInputStream(ins)
        }

    }

    override fun read(): Int {
        return ins.read()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}