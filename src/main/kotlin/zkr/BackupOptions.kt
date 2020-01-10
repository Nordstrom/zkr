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
            names = ["--ephemeral", "-eph"],
            description = ["Backup ephemeral znodes (default: \${DEFAULT-VALUE})"],
            defaultValue = "false"
    )
    var ephemeral: Boolean = false

    //TODO maxRetries (5)
    var maxRetries = 5

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