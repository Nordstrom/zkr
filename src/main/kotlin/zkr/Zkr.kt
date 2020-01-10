package zkr

import kotlinx.coroutines.Runnable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import zkr.Zkr.Companion.VERSION
import java.lang.invoke.MethodHandles
import kotlin.system.exitProcess

@CommandLine.Command(
        name = "zkr",
        description = ["ZooKeeper Reaper - ZooKeeper backup/restore utility"],
        mixinStandardHelpOptions = true,
        version = [VERSION],
        subcommands = [
            CommandLine.HelpCommand::class,
            Backup::class,
            Logs::class,
            Restore::class
        ],
        usageHelpWidth = 120,
        footer = ["v$VERSION"]
)
class Zkr : Runnable {

    companion object {
        const val VERSION = "0.2α"
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Zkr()).execute(*args)
            exitProcess(exitCode)
        } //-main
    } //-companion

    override fun run() = CommandLine(Zkr()).usage(System.out)

} //-Zkr
