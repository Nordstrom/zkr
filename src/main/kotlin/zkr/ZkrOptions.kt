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

    @CommandLine.Parameters(index = "0", description = ["Log or backup file to restore"], arity = "1")
    lateinit var txnLog: String
}