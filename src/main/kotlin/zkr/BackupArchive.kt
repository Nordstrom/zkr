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

class BackupArchiveOutputStream(val path: String, val compress: Boolean = false, val s3bucket: String = "", val s3region: String = "", val dryRun: Boolean = false) : OutputStream() {
    private val os = ByteArrayOutputStream()
    var filepath: String = ""
    var filebase: String = path
    var filename: String

    init {
        val t0 = Instant.now()
        val timestamp = DATE_FORMATTER.format(t0)
        val suffix = if (compress) "gz" else "json"
        val regex = """(.+)/(.+)""".toRegex()
        val matchResult = regex.matchEntire(path)
        if (matchResult != null) {
            val (fp, fn) = matchResult.destructured
            filepath = if (fp.isNotEmpty()) "$fp/" else ""
            filebase = fn
        }
        filename = "$filepath${filebase}-$timestamp.$suffix"
    }

    override fun write(b: Int) {
        os.write(b)
    }

    override fun close() {
        os.close()
        if (dryRun) return

        // If contains path(s), create them first
        if (filepath.isNotEmpty()) {
            File(filepath).mkdirs()
        }
        var out: OutputStream = FileOutputStream(filename)
        if (compress) {
            out = GZIPOutputStream(out)
        }
        out.write(os.toByteArray())
        out.close()

        // For S3, we must write to local file, then upload
        if (s3bucket.isNotEmpty()) {
            //TODO if S3, create in temporary directory since we are going to delete it anyway
            val path = Paths.get(filename)
            val region = Region.of(s3region)
            val s3 = S3Client.builder()
                    .region(region)
                    .build()
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3bucket)
                            .key(filename)
                            .build(),
                    RequestBody.fromFile(path)
            )
            Files.delete(Paths.get(filename))
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmX").withZone(ZoneId.of("Z"))
    }
} //BackupArchiveOutputStream

class BackupArchiveInputStream(file: String, compress: Boolean = false, s3bucket: String = "", s3region: String = "") : InputStream() {
    private var ins: InputStream

    init {
        ins = if (s3bucket.isNotEmpty()) {
            logger.debug("Restoring from S3: bucket=${s3bucket}, region=${s3region}, key=$file")
            val region = Region.of(s3region)
            val s3 = S3Client.builder()
                    .region(region)
                    .build()
            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(s3bucket)
                            .key(file)
                            .build(),
                    ResponseTransformer.toInputStream()
            )
        } else {
            logger.debug("Restoring from file: $file")
            BufferedInputStream(FileInputStream(file))
        }

        if (compress) {
            logger.debug("Restoring compressed archive")
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