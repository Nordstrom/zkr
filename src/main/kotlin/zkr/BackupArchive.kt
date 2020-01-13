package zkr

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

class BackupArchive(private val name: String, private val compress: Boolean = false, val s3bucket: String = "", val s3region: String = "") : OutputStream() {
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
        var out = if (name == "-") {
            System.out
        } else {
            FileOutputStream(file)
        }
        if (compress) {
            out = GZIPOutputStream(out)
        }

        if (s3bucket.isNotEmpty()) {
            val region = Region.of(s3region)
            println("region $s3region -> $region -> ${region.id()}")
            val s3 = S3Client.builder()
                    .region(region)
                    .build()

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3bucket)
                            .key(file)
                            .build(),
                    RequestBody.fromBytes(os.toByteArray())
            )
        }

        out.write(os.toByteArray())
        out.close()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddhhmmz").withZone(ZoneId.of("Z"))
    }
}