package zkr

//
// Code for Backup migrated to kotlin from https://github.com/boundary/zoocreeper
//

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Id
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.IOException
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.Instant

@CommandLine.Command(
        name = "restore",
        description = ["Restore ZooKeeper znodes from backup"],
        subcommands = [
            CommandLine.HelpCommand::class
        ],
        usageHelpWidth = 120
)
class Restore : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    @CommandLine.Mixin
    lateinit var restoreOptions: RestoreOptions

    private val path = mutableListOf<BackupZNode>()

    //TODO: Don't use class variable!!
    private var numberNodes = 0

    override fun run() {
        Zkr.logLevel(this.javaClass.`package`.name, if (options.verbose) Level.DEBUG else Level.INFO)
        logger.debug("options : $options")
        logger.debug("restore : $restoreOptions")

        val ins = BackupArchiveInputStream(file = options.file, compress = restoreOptions.compress, s3bucket = options.s3bucket, s3region = options.s3region)
        ins.use { it ->
            restore(it)
        }

    }

    private fun restore(inputStream: InputStream?) {
        numberNodes = 0
        val t0 = Instant.now()
        val zkc = ZkClient(host = options.host, connect = !restoreOptions.dryRun, sessionTimeoutMillis = options.sessionTimeoutMs)
        var jp: JsonParser? = null
        try {
            jp = JsonFactory().createParser(inputStream)
            doRestore(jp, zkc)
        } finally {
            zkc.close()
            jp?.close()
        }

        logger.info("Summary ${summary(options.file, numberNodes, t0)}")
    }

    private fun doRestore(jp: JsonParser?, zkc: ZkClient) {
        expectNextToken(jp!!, JsonToken.START_OBJECT)
        val createdPaths: MutableSet<String> = Sets.newHashSet()
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            val zNode = readZNode(jp, jp.currentName)
            // We are the root
            if (path.isEmpty()) {
                path.add(zNode)
            } else {
                val it = path.listIterator(path.size)
                while (it.hasPrevious()) {
                    val parent = it.previous()
                    if (zNode.path.startsWith(parent.path)) {
                        break
                    }
                    it.remove()
                }
                path.add(zNode)
            }
            if (zNode.ephemeralOwner != 0L) {
                logger.info("Skipping ephemeral znode: ${zNode.path}")
                continue
            }
            if (!zNode.path.startsWith(options.path)) {
                logger.info("Skipping znode (not under root path '${options.path}'): ${zNode.path}")
                continue
            }
            if (!options.shouldInclude(zNode.path)) {
                logger.debug("skip restore node: ${zNode.path}")
                continue
            }
            for (pathComponent in path) {
                if (createdPaths.add(pathComponent.path)) {
                    restoreNode(zkc, pathComponent)
                }
            }
        }
    }

    private fun restoreNode(zkc: ZkClient, zNode: BackupZNode) {
        ++numberNodes
        if (restoreOptions.dryRun) {
            logger.info("PRETEND: $zNode")
            return
        }
        zkc.zk?.let { createPath(it, getParentPath(zNode.path)) }
        try {
            zkc.zk?.create(zNode.path, zNode.data, zNode.acls, CreateMode.PERSISTENT)
            logger.info("Created node: ${zNode.path}")
        } catch (e: NodeExistsException) {
            if (restoreOptions.overwrite) { // TODO: Compare with current data / acls
                logger.warn("OVERWRITE ${zNode.path}")
                zkc.zk?.setACL(zNode.path, zNode.acls, -1)
                zkc.zk?.setData(zNode.path, zNode.data, -1)
            } else {
                logger.warn("Node already exists: ${zNode.path}")
            }
        }
    }

    private fun expectNextToken(jp: JsonParser, expected: JsonToken) {
        if (jp.nextToken() != expected) {
            throw IOException("Expected: $expected, Found: ${jp.currentToken}")
        }
    }

    private fun expectCurrentToken(jp: JsonParser, expected: JsonToken) {
        val currentToken = jp.currentToken
        if (currentToken != expected) {
            throw IOException("Expected: $expected, Found: ${currentToken}")
        }
    }

    private fun getParentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else "/"
    }

    private fun createPath(zk: ZooKeeper, path: String) {
        if ("/" == path) {
            return
        }
        if (zk.exists(path, false) == null) {
            createPath(zk, getParentPath(path))
            logger.info("Creating path: $path")
            try {
                zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
            } catch (e: NodeExistsException) { // Race condition
                logger.error("Race Condition detected: $e")
            }
        }
    }

    private fun readZNode(jp: JsonParser, path: String): BackupZNode {
        expectNextToken(jp, JsonToken.START_OBJECT)
        var ephemeralOwner: Long = 0
        var data: ByteArray? = null
        val acls: MutableList<ACL> = Lists.newArrayList()
        val seenFields: MutableSet<String> = Sets.newHashSet()
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextValue()
            val fieldName = jp.currentName
            seenFields.add(fieldName)
            when (fieldName) {
                BackupZNode.FIELD_EPHEMERAL_OWNER -> {
                    ephemeralOwner = jp.longValue
                }
                BackupZNode.FIELD_DATA -> {
                    data = if (jp.currentToken == JsonToken.VALUE_NULL) {
                        null
                    } else {
                        jp.binaryValue
                    }
                }
                BackupZNode.FIELD_ACLS -> {
                    readACLs(jp, acls)
                }
                else -> {
                    logger.debug("Ignored field: $fieldName")
                }
            }
        }
        if (!seenFields.containsAll(REQUIRED_ZNODE_FIELDS)) {
            throw IOException("Missing required fields: $REQUIRED_ZNODE_FIELDS")
        }
        return BackupZNode(path, ephemeralOwner, data, acls)
    }

    private fun readACLs(jp: JsonParser, acls: MutableList<ACL>) {
        expectCurrentToken(jp, JsonToken.START_ARRAY)
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            acls.add(readACL(jp))
        }
    }

    private fun readACL(jp: JsonParser): ACL {
        expectCurrentToken(jp, JsonToken.START_OBJECT)
        var scheme: String? = null
        var id: String? = null
        var perms = -1
        val seenFields: MutableSet<String> = Sets.newHashSet()
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextValue()
            val fieldName = jp.currentName
            seenFields.add(fieldName)
            when (fieldName) {
                BackupZNode.FIELD_ACL_SCHEME -> {
                    scheme = jp.valueAsString
                }
                BackupZNode.FIELD_ACL_ID -> {
                    id = jp.valueAsString
                }
                BackupZNode.FIELD_ACL_PERMS -> {
                    perms = jp.intValue
                }
                else -> {
                    throw IOException("Unexpected field: $fieldName")
                }
            }
        }
        if (!seenFields.containsAll(REQUIRED_ACL_FIELDS)) {
            throw IOException("Missing required ACL fields: $REQUIRED_ACL_FIELDS")
        }
        val zkId: Id
        zkId = if (Ids.ANYONE_ID_UNSAFE.scheme == scheme && Ids.ANYONE_ID_UNSAFE.id == id) {
            Ids.ANYONE_ID_UNSAFE
        } else {
            Id(scheme, id)
        }
        return ACL(perms, zkId)
    }

    private fun summary(file: String, numberNodes: Int, start: Instant): String = """

  ,-----------.  
(_\  ZooKeeper \ file    : ${if (options.s3bucket.isNotEmpty()) "s3://${options.s3bucket}/" else ""}$file
   | Reaper    | znodes  : $numberNodes
   | Summary   | duration: ${Duration.between(start, Instant.now())}
  _|           |
 (_/_____(*)___/
          \\
           ))
           ^
""".trimIndent()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val REQUIRED_ZNODE_FIELDS = ImmutableList.of<String>(BackupZNode.FIELD_EPHEMERAL_OWNER, BackupZNode.FIELD_DATA, BackupZNode.FIELD_ACLS)
        private val REQUIRED_ACL_FIELDS = ImmutableList.of<String>(BackupZNode.FIELD_ACL_SCHEME, BackupZNode.FIELD_ACL_ID, BackupZNode.FIELD_ACL_PERMS)
    } //-companion

} //-Restore
