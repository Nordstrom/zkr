package zkr

//
// Code for Backup migrated to kotlin from https://github.com/boundary/zoocreeper
//

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
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.util.zip.GZIPInputStream

@CommandLine.Command(
        name = "restore",
        description = ["Restore ZooKeeper znodes from backup"],
        usageHelpWidth = 120
)
class Restore : Runnable {
    @CommandLine.Mixin
    lateinit var options: ZkrOptions

    @CommandLine.Mixin
    lateinit var restoreOptions: RestoreOptions

    private val path: MutableList<BackupZNode> = Lists.newArrayList()

    override fun run() {
        logger.debug("options : $options")
        logger.debug("restore : $restoreOptions")

        //TODO --dry-run
        //TODO S3
        var ins: InputStream? = null
        try {
            ins = if ("-" == options.txnLog) {
                logger.info("Restoring from stdin")
                BufferedInputStream(System.`in`)
            } else {
                BufferedInputStream(FileInputStream(options.txnLog))
            }
            if (restoreOptions.compress) {
                ins = GZIPInputStream(ins)
            }
            restore(ins)
        } finally {
            ins?.close()
        }

    }

    fun restore(inputStream: InputStream?) {
        var zk: ZkClient? = null
        var jp: JsonParser? = null
        try {
            jp = JsonFactory().createParser(inputStream)
            zk = ZkClient(host = options.host, connect = true)
            doRestore(jp, zk.zk)
        } finally {
            zk?.close()
            jp?.close()
        }
    }

    private fun doRestore(jp: JsonParser?, zk: ZooKeeper?) {
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
                logger.info("Skipping ephemeral ZNode: {}", zNode.path)
                continue
            }
            if (!zNode.path.startsWith(options.path)) {
                logger.info("Skipping ZNode (not under root path '{}'): {}", options.path, zNode.path)
                continue
            }
            if (options.shouldExclude(zNode.path)) {
                continue
            }
            for (pathComponent in path) {
                if (createdPaths.add(pathComponent.path)) {
                    restoreNode(zk, pathComponent)
                }
            }
        }
    }

    private fun restoreNode(zk: ZooKeeper?, zNode: BackupZNode) {
        createPath(zk!!, getParentPath(zNode.path))
        try {
            zk.create(zNode.path, zNode.data, zNode.acls, CreateMode.PERSISTENT)
            logger.info("Created node: {}", zNode.path)
        } catch (e: NodeExistsException) {
            if (restoreOptions.overwrite) { // TODO: Compare with current data / acls
                zk.setACL(zNode.path, zNode.acls, -1)
                zk.setData(zNode.path, zNode.data, -1)
            } else {
                logger.warn("Node already exists: {}", zNode.path)
            }
        }
    }

    private fun expectNextToken(jp: JsonParser, expected: JsonToken) {
        if (jp.nextToken() != expected) {
            throw IOException(String.format("Expected: %s, Found: %s", expected, jp.currentToken))
        }
    }

    private fun expectCurrentToken(jp: JsonParser, expected: JsonToken) {
        val currentToken = jp.currentToken
        if (currentToken != expected) {
            throw IOException(String.format("Expected: %s, Found: %s", expected, currentToken))
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
            logger.info("Creating path: {}", path)
            try {
                zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
            } catch (e: NodeExistsException) { // Race condition
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
            if (ZNode.FIELD_EPHEMERAL_OWNER.equals(fieldName)) {
                ephemeralOwner = jp.longValue
            } else if (ZNode.FIELD_DATA.equals(fieldName)) {
                data = if (jp.currentToken == JsonToken.VALUE_NULL) {
                    null
                } else {
                    jp.binaryValue
                }
            } else if (ZNode.FIELD_ACLS.equals(fieldName)) {
                readACLs(jp, acls)
            } else {
                logger.debug("Ignored field: {}", fieldName)
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
            if (ZNode.FIELD_ACL_SCHEME.equals(fieldName)) {
                scheme = jp.valueAsString
            } else if (ZNode.FIELD_ACL_ID.equals(fieldName)) {
                id = jp.valueAsString
            } else if (ZNode.FIELD_ACL_PERMS.equals(fieldName)) {
                perms = jp.intValue
            } else {
                throw IOException("Unexpected field: $fieldName")
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


    data class BackupZNode(val path: String, val ephemeralOwner: Long, val data: ByteArray?, val acls: List<ACL>)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val REQUIRED_ZNODE_FIELDS = ImmutableList.of<String>(ZNode.FIELD_EPHEMERAL_OWNER, ZNode.FIELD_DATA, ZNode.FIELD_ACLS)
        private val REQUIRED_ACL_FIELDS = ImmutableList.of<String>(ZNode.FIELD_ACL_SCHEME, ZNode.FIELD_ACL_ID, ZNode.FIELD_ACL_PERMS)
    } //-companion

} //-Restore
