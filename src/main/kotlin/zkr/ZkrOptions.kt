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

    @CommandLine.Option(
            names = ["--overwrite-existing", "-o"],
            description = ["Overwrite existing znodes (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var overwrite: Boolean = false

    @CommandLine.Option(
            names = ["--zookeeper", "-z"],
            description = ["Target ZooKeeper host:port (default: \${DEFAULT-VALUE})"],
            defaultValue = "localhost:2181")
    lateinit var host: String

    @CommandLine.Option(
            names = ["--exclude", "-e"],
            description = ["Comma-delimited list of paths to exclude (default: \${DEFAULT-VALUE})"],
            split = ",",
            defaultValue = ""
    )
    lateinit var exclude: List<String>

    @CommandLine.Option(
            names = ["--s3-bucket"],
            description = ["S3 bucket containing Exhibitor transaction logs or backup files"],
            defaultValue = ""
    )
    lateinit var s3bucket: String

    @CommandLine.Option(
            names = ["--s3-region"],
            description = ["AWS Region (default: \${DEFAULT-VALUE})"],
            defaultValue = "us-west-2"
    )
    lateinit var s3region: String

    @CommandLine.Parameters(index = "0", description = ["Log or backup file to restore"], arity = "1")
    lateinit var txnLog: String

    override fun toString(): String {
        return """
dry-run=$dryRun, verbose=$verbose, overwrite=$overwrite, host=$host, txnLog=$txnLog, exclude=$exclude, s3bucket=$s3bucket, s3region=$s3region
        """.trimIndent()
    }

} //-ZkrOptions