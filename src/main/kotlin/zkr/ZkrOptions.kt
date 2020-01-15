package zkr

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.lang.invoke.MethodHandles
import java.util.regex.Pattern

class ZkrOptions {
    @CommandLine.Option(
            names = ["--verbose", "-v"],
            description = ["Verbose (DEBUG) logging level (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var verbose: Boolean = false

    @CommandLine.Option(
            names = ["--zookeeper", "-z"],
            description = ["Target ZooKeeper host:port (default: \${DEFAULT-VALUE})"],
            defaultValue = "localhost:2181"
    )
    var host: String = "localhost:2181"

    @CommandLine.Option(
            names = ["--session-timeout-ms", "-s"],
            description = ["ZooKeeper session timeout in milliseconds (default: \${DEFAULT-VALUE})"],
            defaultValue = "30000"
    )
    var sessionTimeoutMs: Long = 30 * 1000

    @CommandLine.Option(
            names = ["--not-leader", "-l"],
            description = ["Perform backup/restore even if not ZooKeeper ensemble leader (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var notLeader: Boolean = false

    @CommandLine.Option(
            names = ["--exclude", "-e"],
            description = ["Comma-delimited list of paths to exclude (regex)"],
            split = ","
    )
    var excludes: List<Pattern> = mutableListOf()

    @CommandLine.Option(
            names = ["--include", "-i"],
            description = ["Comma-delimited list of paths to include (regex)"],
            split = ","
    )
    var includes: List<Pattern> = mutableListOf()

    @CommandLine.Option(
            names = ["--s3-bucket"],
            description = ["S3 bucket containing Exhibitor transaction logs/backups or zkr backup files"],
            defaultValue = ""
    )
    var s3bucket: String = ""

    @CommandLine.Option(
            names = ["--s3-region"],
            description = ["AWS Region (default: \${DEFAULT-VALUE})"],
            defaultValue = "us-west-2"
    )
    var s3region: String = "us-west-2"

    @CommandLine.Option(
            names = ["--path", "-p"],
            description = ["ZooKeeper root path for backup/restore (default: \${DEFAULT-VALUE})"],
            defaultValue = "/"
    )
    var path: String = "/"

    @CommandLine.Parameters(index = "0", description = ["Transaction log or backup file"], arity = "1")
    //TODO If S3 versioning, use '?' and parse
    lateinit var file: String

    fun shouldInclude(path: String): Boolean {
        return !isPathExcluded(path) && isPathIncluded(path)
    }

    fun isPathExcluded(path: String): Boolean {
        logger.debug("excludes=$excludes, path=$path")
        if (excludes.isEmpty()) {
            return false
        }
        var ignored = false
        for (pattern in excludes) {
            if (pattern.matcher(path).find()) {
                logger.debug("exclude path: {} matching pattern: {}", path, pattern.pattern())
                ignored = true
                break
            }
        }
        logger.debug("path=$path, exclude=$ignored")
        return ignored
    }

    fun isPathIncluded(path: String): Boolean {
        logger.debug("includes=$includes, path=$path")
        if (includes.isEmpty()) {
            return true
        }
        var included = false
        for (pattern in includes) {
            if (pattern.matcher(path).find()) {
                logger.debug("include path: {} matching pattern: {}", path, pattern.pattern())
                included = true
                break
            }
        }
        logger.debug("path=$path, include=$included")
        return included
    }

    override fun toString(): String {
        return """
verbose=$verbose, host=$host, file=$file, include=$includes, exclude=$excludes, s3bucket=$s3bucket, s3region=$s3region
        """.trimIndent()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZkrOptions
