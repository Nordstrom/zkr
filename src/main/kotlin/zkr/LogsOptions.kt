package zkr

import picocli.CommandLine

class LogsOptions {
    @CommandLine.Option(
            names = ["--restore", "-r"],
            description = ["Execute (restore) transactions (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var restore: Boolean = false

    @CommandLine.Option(
            names = ["--overwrite-existing", "-o"],
            description = ["Overwrite existing znodes (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var overwrite: Boolean = false

    @CommandLine.Option(
            names = ["--info"],
            description = ["Print information about transaction log or backup then exit  (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var info: Boolean = false

    override fun toString(): String {
        return """
overwrite=$overwrite, info=$info 
        """.trimIndent()
    }

} //-RestoreOptions