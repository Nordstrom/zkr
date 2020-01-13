package zkr

import picocli.CommandLine

class ZkrOptions {
    @CommandLine.Option(
            names = ["--dry-run", "-d"],
            description = ["Do not actually perform the actions (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var dryRun: Boolean = false

    @CommandLine.Option(
            names = ["--verbose", "-v"],
            description = ["Verbose logging output (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var verbose: Boolean = false

//    @CommandLine.Option(
//            names = ["--overwrite-existing", "-o"],
//            description = ["Overwrite existing znodes (default: \${DEFAULT-VALUE})"],
//            defaultValue = "false"
//    )
//    var overwrite: Boolean = false

    @CommandLine.Option(
            names = ["--zookeeper", "-z"],
            description = ["Target ZooKeeper host:port (default: \${DEFAULT-VALUE})"],
            defaultValue = "localhost:2181")
    var host: String = "localhost:2181"

    //TODO regex
    @CommandLine.Option(
            names = ["--exclude", "-e"],
            description = ["Comma-delimited list of paths to exclude"],
            split = ","
    )
    var excludes: List<String> = mutableListOf()
    //TODO includes? regex

    @CommandLine.Option(
            names = ["--s3-bucket"],
            description = ["S3 bucket containing Exhibitor transaction logs or backup files"],
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

    @CommandLine.Parameters(index = "0", description = ["Log or backup file"], arity = "1")
    //TODO If S3 versioning, use '?' and parse
    lateinit var file: String

    override fun toString(): String {
        return """
verbose=$verbose, host=$host, file=$file, exclude=$excludes, s3bucket=$s3bucket, s3region=$s3region
        """.trimIndent()
    }

    fun shouldExclude(path: String): Boolean {
        val excluded = excludes.filter { path.startsWith(it) }
        return excluded.isNotEmpty()
    }


} //-ZkrOptions