package zkr

//
// Code for Backup migrated to kotlin from https://github.com/boundary/zoocreeper
//

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import kotlinx.serialization.UnstableDefault
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Stat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.system.exitProcess

@CommandLine.Command(
        name = "backup",
        description = ["Backup ZooKeeper znodes"],
        subcommands = [
            CommandLine.HelpCommand::class
        ],
        usageHelpWidth = 120
)
class Backup : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    @CommandLine.Mixin
    lateinit var backupOptions: BackupOptions

    private lateinit var cancel: Channel<Unit>

    //TODO: Don't use class variable!!
    private var numberNodes = 0

    @UnstableDefault
    @ObsoleteCoroutinesApi
    @InternalCoroutinesApi
    override fun run() {
        Zkr.logLevel(this.javaClass.`package`.name, if (options.verbose) Level.DEBUG else Level.INFO)
        logger.debug("options : $options")
        logger.debug("backup  : $backupOptions")

        cancel = CliHelper.trapSignal("INT")
        runBlocking {
            val jobs = mutableListOf(launch(Dispatchers.IO) { runBackup() })

            // Wait for cancel signal
            select<Unit> {
                cancel.onReceive {
                    jobs.forEach { it.cancelAndJoin() }
                }
            }
        } //-runBlocking

        exitProcess(0)

    }

    @UnstableDefault
    @ObsoleteCoroutinesApi
    @InternalCoroutinesApi
    suspend fun runBackup() {
        val dur = Duration.ofMinutes(backupOptions.repeatMin)

        logger.debug("backup     : ${options.file}")
        logger.debug("compression: ${backupOptions.compress}")
        logger.debug("excluding  : ${options.excludes}")
        logger.debug("including  : ${options.includes}")
        logger.debug("repeat     : $dur, ${dur.toMillis()} ms")

        if (backupOptions.repeatMin > 0) {
            val ticktock = ticker(delayMillis = dur.toMillis(), initialDelayMillis = 0)
            var tick = true
            while (NonCancellable.isActive) {
                select<Unit> {
                    ticktock.onReceive {
                        try {

                            logger.info(if (tick) "tick" else "tock")
                            tick = !tick
                            doBackup()

                        } catch (e: Exception) {
                            logger.error("$e")
                        }
                    } //-ticktock
                } //-select
            } //-while

        } else {
            doBackup()
            cancel.send(Unit)
        }
    }

    @UnstableDefault
    private fun doBackup() {
        // Only execute backup if connector to ensemble leader
        if (!options.notLeader) {
            ZkSocketClient(options.host).use {
                if (!it.isLeaderOrStandalone()) {
                    logger.warn("ZooKeeper '${options.host}' is not the 'leader' or 'stand-alone', skipping backup.")
                    return
                } else {
                    logger.debug("ZooKeeper '${options.host}' is leader or standalone")
                }
            }
        }

        val zkc = ZkClient(
                host = options.host,
                connect = true,
                sessionTimeoutMillis = options.sessionTimeoutMs,
                superdigest = options.superdigest
        )

        numberNodes = 0
        val t0 = Instant.now()
        val os = BackupArchiveOutputStream(path = options.file, compress = backupOptions.compress, s3bucket = options.s3bucket, s3region = options.s3region, dryRun = backupOptions.dryRun)
        logger.debug("backup to  : ${os.filename}")

        backup(zkc, os)

        logger.info("Summary ${summary(os.filename, numberNodes, t0)}")
    }

    @UnstableDefault
    private fun backup(zkc: ZkClient, os: OutputStream) {
        var jgen: JsonGenerator? = null
        try {
            jgen = JsonFactory().createGenerator(os)
            if (backupOptions.pretty) {
                jgen.prettyPrinter = DefaultPrettyPrinter()
            }
            jgen.writeStartObject()
            if (zkc.exists(options.path)) {
                logger.warn("Root path not found: ${options.path}")
            } else {
                doBackup(zkc, jgen, options.path)
            }
            jgen.writeEndObject()
        } finally {
            //NB: this will also call 'close()' on the output stream.
            jgen?.close()
            zkc.close()
        }
    }

    @UnstableDefault
    private fun doBackup(zkc: ZkClient, jgen: JsonGenerator?, path: String) {
        try {
            val zk = zkc.zk
            val stat = Stat()
            //TODO ZkClient.getAcls() ??
            var acls: List<ACL> = nullToEmpty(zk!!.getACL(path, stat))
            if (stat.ephemeralOwner != 0L && !backupOptions.ephemeral) {
                logger.debug("skip ephemeral node: $path")
                return
            }
            val dataStat = Stat()
            //TODO ZkClient.getData()
            var data = zk.getData(path, false, dataStat)
            var i = 0
            //TODO ZkClient?
            while (stat.compareTo(dataStat) != 0 && i < backupOptions.maxRetries) {
                logger.warn("Retrying getACL / getData to read consistent state")
                acls = zk.getACL(path, stat)
                data = zk.getData(path, false, dataStat)
                i++
            }
            check(stat.compareTo(dataStat) == 0) { "Unable to read consistent data for znode: $path" }
            if (options.shouldInclude(path)) {
                logger.debug("backup node: $path")
                if (!backupOptions.dryRun) dumpNode(jgen, path, stat, acls, data)
                numberNodes++
            } else {
                logger.debug("skip backup node: $path")
            }
            val childPaths: MutableList<String> = nullToEmpty(zk.getChildren(path, false, null))
            childPaths.sort()
            for (childPath in childPaths) {
                val fullChildPath = createFullPath(path, childPath)
                doBackup(zkc, jgen, fullChildPath)
            }
        } catch (e: KeeperException.NoNodeException) {
            logger.warn("Node disappeared during backup: path=$path")
        } catch (e: KeeperException) {
            logger.warn("Unable to read znode: $e")
        }
    }

    private fun dumpNode(jgen: JsonGenerator?, path: String?, stat: Stat, acls: List<ACL>, data: ByteArray?) {
        if (data != null && path != null) {
            logger.debug("dump-node\npath=$path\ndata_s=${String(data)}\ndata_a=${Arrays.toString(data)}")
        }
        jgen!!.writeObjectFieldStart(path)
        // The number of changes to the ACL of this znode.
        jgen.writeNumberField(BackupZNode.FIELD_AVERSION, stat.aversion)
        // The time in milliseconds from epoch when this znode was created.
        jgen.writeNumberField(BackupZNode.FIELD_CTIME, stat.ctime)
        // The number of changes to the children of this znode.
        jgen.writeNumberField(BackupZNode.FIELD_CVERSION, stat.cversion)
        // The zxid of the change that caused this znode to be created.
        jgen.writeNumberField(BackupZNode.FIELD_CZXID, stat.czxid)
        // The length of the data field of this znode.
        // jgen.writeNumberField("dataLength", stat.getDataLength());
        // The session id of the owner of this znode if the znode is an ephemeral node. If it is not an ephemeral node,
        // it will be zero.
        jgen.writeNumberField(BackupZNode.FIELD_EPHEMERAL_OWNER, stat.ephemeralOwner)
        // The time in milliseconds from epoch when this znode was last modified.
        jgen.writeNumberField(BackupZNode.FIELD_MTIME, stat.mtime)
        // The zxid of the change that last modified this znode.
        jgen.writeNumberField(BackupZNode.FIELD_MZXID, stat.mzxid)
        // The number of children of this znode.
        jgen.writeNumberField("numChildren", stat.numChildren)
        // last modified children?
        jgen.writeNumberField(BackupZNode.FIELD_PZXID, stat.pzxid)
        // The number of changes to the data of this znode.
        jgen.writeNumberField(BackupZNode.FIELD_VERSION, stat.version)
        if (data != null) {
            jgen.writeBinaryField(BackupZNode.FIELD_DATA, data)
        } else {
            jgen.writeNullField(BackupZNode.FIELD_DATA)
        }
        jgen.writeArrayFieldStart(BackupZNode.FIELD_ACLS)
        for (acl in acls) {
            jgen.writeStartObject()
            jgen.writeStringField(BackupZNode.FIELD_ACL_ID, acl.id.id)
            jgen.writeStringField(BackupZNode.FIELD_ACL_SCHEME, acl.id.scheme)
            jgen.writeNumberField(BackupZNode.FIELD_ACL_PERMS, acl.perms)
            jgen.writeEndObject()
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
    }

    private fun createFullPath(path: String, childPath: String): String {
        return if (path.endsWith("/")) {
            path + childPath
        } else {
            "$path/$childPath"
        }
    }

    private fun <T> nullToEmpty(original: List<T>?): MutableList<T> {
        return original as MutableList<T>? ?: mutableListOf()
    }

    private fun summary(file: String, numberNodes: Int, start: Instant): String = """

  ,-----------.  
(_\  ZooKeeper \ file    : ${if (options.s3bucket.isNotEmpty()) "s3://${options.s3bucket}/" else ""}$file
   | Reaper    | znodes  : $numberNodes
   | Summary   | duration: ${Duration.between(start, Instant.now())}
  _|           | includes: ${options.includes}
 (_/_____(*)___/ excludes: ${options.excludes}
          \\
           ))
           ^
""".trimIndent()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}