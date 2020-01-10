package zkr

//
// Code for Backup migrated to kotlin from https://github.com/boundary/zoocreeper
//

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Stat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.system.exitProcess

@CommandLine.Command(
        name = "backup",
        description = ["Backup ZooKeeper znodes"],
        usageHelpWidth = 120
)
class Backup : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    @CommandLine.Mixin
    lateinit var backupOptions: BackupOptions

    lateinit var cancel: Channel<Unit>

    //TODO: Don't use class variable!!
    var numberNodes = 0

    @ObsoleteCoroutinesApi
    @InternalCoroutinesApi
    override fun run() {
        //TODO set logging level
        logger.debug("options : $options")
        logger.debug("backup  : $backupOptions")

        cancel = CliHelper.trapSignal("INT")
        runBlocking {
            logger.info("CROSSING.THE.STREAMS")
            val jobs = mutableListOf<Job>(launch(Dispatchers.IO) { runBackup() })

            // Wait for cancel signal
            select<Unit> {
                cancel.onReceive {
                    logger.info("STREAMS.CROSSED")
                    jobs.forEach { it.cancelAndJoin() }
                    logger.info("PROTON.PACK.OVERLOAD")
                }
            }
        } //-runBlocking
        logger.info("TOTAL.PROTONIC.REVERSAL")

        exitProcess(0)

    }

    @ObsoleteCoroutinesApi
    @InternalCoroutinesApi
    suspend fun runBackup() {
        val dur = Duration.ofMinutes(backupOptions.repeatMin)

        logger.info("backup     : ${options.txnLog}")
        logger.info("compression: ${backupOptions.compress}")
        logger.info("excluding  : ${options.excludes}")
        logger.info("repeat     : $dur, ${dur.toMillis()} ms")
        logger.info("dry-run    : ${options.dryRun}")

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

    fun doBackup() {
        numberNodes = 0
        val t0 = Instant.now()
        val timestamp = DATE_FORMATTER.format(t0)
        val suffix = if (backupOptions.compress) "gz" else "json"
        val file = "${options.txnLog}-$timestamp.$suffix"
        logger.info("backup to  : $file")
        //TODO S3
        var os: OutputStream
        os = if ("-" == options.txnLog) {
            System.out
        } else {
            BufferedOutputStream(FileOutputStream(file))
        }
        try {
            if (backupOptions.compress) {
                os = GZIPOutputStream(os)
            }
            backup(os)
        } finally {
            os.flush()
            os.close()
        }
        logger.info("Summary ${summary(file, numberNodes, t0)}")
    }

    fun backup(os: OutputStream) {
        val zk = ZkClient(host = options.host, connect = true)
        var jgen: JsonGenerator? = null
        try {
            jgen = JsonFactory().createGenerator(os)
            if (backupOptions.pretty) {
                jgen.prettyPrinter = DefaultPrettyPrinter()
            }
            jgen.writeStartObject()
            if (zk.zk?.exists(options.path, false) == null) {
                logger.warn("Root path not found: ${options.path}")
            } else {
                doBackup(zk.zk, jgen, options.path)
            }
            jgen.writeEndObject()
        } finally {
            jgen?.close()
            zk.close()
        }
    }

    private fun doBackup(zk: ZooKeeper?, jgen: JsonGenerator?, path: String) {
        try {
            val stat = Stat()
            var acls: List<ACL> = ZNode.nullToEmpty(zk!!.getACL(path, stat))
            if (stat.ephemeralOwner != 0L && !backupOptions.ephemeral) {
                logger.debug("Skipping ephemeral node: $path")
                return
            }
            val dataStat = Stat()
            var data = zk.getData(path, false, dataStat)
            var i = 0
            while (stat.compareTo(dataStat) != 0 && i < backupOptions.maxRetries) {
                logger.warn("Retrying getACL / getData to read consistent state")
                acls = zk.getACL(path, stat)
                data = zk.getData(path, false, dataStat)
                i++
            }
            check(stat.compareTo(dataStat) == 0) { "Unable to read consistent data for znode: $path" }
            logger.debug("Backing up node: $path")
            dumpNode(jgen, path, stat, acls, data)
            numberNodes++
            val childPaths: List<String> = ZNode.nullToEmpty(zk.getChildren(path, false, null))
            Collections.sort(childPaths)
            for (childPath in childPaths) {
                val fullChildPath: String = ZNode.createFullPath(path, childPath)
                if (!options.shouldExclude(fullChildPath)) {
                    doBackup(zk, jgen, fullChildPath)
                }
            }

        } catch (e: KeeperException.NoNodeException) {
            logger.warn("Node disappeared during backup: path=$path")
        } catch (e: KeeperException) {
            logger.warn("Unable to read znode: $e")

        }
    }

    private fun dumpNode(jgen: JsonGenerator?, path: String?, stat: Stat, acls: List<ACL>, data: ByteArray?) {
//        if (data != null && path != null && path.contains("/kafka/config/users/")) {
        if (data != null && path != null) {
            logger.debug(".dump-node\npath=$path\ndata_s=${String(data)}\ndata_a=${Arrays.toString(data)}")
        }
        jgen!!.writeObjectFieldStart(path)
        // The number of changes to the ACL of this znode.
        jgen.writeNumberField(ZNode.FIELD_AVERSION, stat.aversion)
        // The time in milliseconds from epoch when this znode was created.
        jgen.writeNumberField(ZNode.FIELD_CTIME, stat.ctime)
        // The number of changes to the children of this znode.
        jgen.writeNumberField(ZNode.FIELD_CVERSION, stat.cversion)
        // The zxid of the change that caused this znode to be created.
        jgen.writeNumberField(ZNode.FIELD_CZXID, stat.czxid)
        // The length of the data field of this znode.
        // jgen.writeNumberField("dataLength", stat.getDataLength());
        // The session id of the owner of this znode if the znode is an ephemeral node. If it is not an ephemeral node,
        // it will be zero.
        jgen.writeNumberField(ZNode.FIELD_EPHEMERAL_OWNER, stat.ephemeralOwner)
        // The time in milliseconds from epoch when this znode was last modified.
        jgen.writeNumberField(ZNode.FIELD_MTIME, stat.mtime)
        // The zxid of the change that last modified this znode.
        jgen.writeNumberField(ZNode.FIELD_MZXID, stat.mzxid)
        // The number of children of this znode.
        jgen.writeNumberField("numChildren", stat.numChildren)
        // last modified children?
        jgen.writeNumberField(ZNode.FIELD_PZXID, stat.pzxid)
        // The number of changes to the data of this znode.
        jgen.writeNumberField(ZNode.FIELD_VERSION, stat.version)
        if (data != null) {
            jgen.writeBinaryField(ZNode.FIELD_DATA, data)
        } else {
            jgen.writeNullField(ZNode.FIELD_DATA)
        }
        jgen.writeArrayFieldStart(ZNode.FIELD_ACLS)
        for (acl in acls) {
            jgen.writeStartObject()
            jgen.writeStringField(ZNode.FIELD_ACL_ID, acl.id.id)
            jgen.writeStringField(ZNode.FIELD_ACL_SCHEME, acl.id.scheme)
            jgen.writeNumberField(ZNode.FIELD_ACL_PERMS, acl.perms)
            jgen.writeEndObject()
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
    }

    private fun summary(file: String, numberNodes: Int, start: Instant): String = """

  ,-----------.  
(_\  ZooKeeper \ title   : $file
   | Reaper    | nodes   : $numberNodes
   | Summary   | duration: ${Duration.between(start, Instant.now())}
  _|           |
 (_/_____(*)___/
          \\
           ))
           ^
""".trimIndent()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddhhmmz").withZone(ZoneId.of("Z"))
    }
}