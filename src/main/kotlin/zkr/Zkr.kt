package zkr

import ch.qos.logback.classic.Level
import kotlinx.coroutines.Runnable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import zkr.Zkr.Companion.VERSION
import java.lang.invoke.MethodHandles
import kotlin.system.exitProcess

@CommandLine.Command(
        name = "zkr",
        header = ["ZooKeeper Reaper v$VERSION - ZooKeeper backup/restore utility"],
        mixinStandardHelpOptions = true,
        version = [VERSION],
        subcommands = [
            CommandLine.HelpCommand::class,
            Backup::class,
            Logs::class,
            Restore::class
        ],
        usageHelpWidth = 120
)
class Zkr : Runnable {

    companion object {
        const val VERSION = "0.5"
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Zkr()).execute(*args)
            exitProcess(exitCode)
        } //-main

        fun logLevel(packageName: String, level: Level = Level.DEBUG) {
            val logger = LoggerFactory.getLogger(packageName) as ch.qos.logback.classic.Logger
            logger.level = level
        }
    } //-companion

    override fun run() = CommandLine(Zkr()).usage(System.out)

} //-Zkr
