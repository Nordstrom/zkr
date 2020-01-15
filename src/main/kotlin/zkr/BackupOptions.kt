package zkr

import picocli.CommandLine

class BackupOptions {
    @CommandLine.Option(
            names = ["--compress", "-c"],
            description = ["Compress output (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var compress: Boolean = false

    @CommandLine.Option(
            names = ["--ephemeral", "-f"],
            description = ["Backup ephemeral znodes (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var ephemeral: Boolean = false

    @CommandLine.Option(
            names = ["--max-retries", "-m"],
            description = ["Maximum number of retries to read consistent data (default: \${DEFAULT-VALUE})"],
            defaultValue = "5"
    )
    var maxRetries: Long = 5

    @CommandLine.Option(
            names = ["--pretty"],
            description = ["Pretty print JSON output (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var pretty: Boolean = false

    @CommandLine.Option(
            names = ["--repeat.min", "-r"],
            description = ["Perform periodic backup every <repeatMin> minutes"]
    )
    var repeatMin: Long = -1

    override fun toString(): String {
        return """
compress=$compress, ephemeral=$ephemeral, repeat-min=$repeatMin, pretty=$pretty 
        """.trimIndent()
    }

} //-BackupOptions