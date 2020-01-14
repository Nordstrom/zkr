package zkr

import com.google.common.primitives.Ints
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.ACL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ZkClient(val host: String, val connect: Boolean = true) {
    var zk: ZooKeeper? = null

    init {
        if (!connect) {
            logger.info("no connection to ZooKeeper for --dry-run")
        } else {
            val connected = CountDownLatch(1)
            logger.info("connecting to $host")
            zk = ZooKeeper(host, Ints.checkedCast(ZK_SESSION_TIMEOUT_MS), Watcher { event ->
                if (event.state == Watcher.Event.KeeperState.SyncConnected) {
                    connected.countDown()
                }
            })
            try {
                if (!connected.await(ZK_SESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    throw IOException("Timeout out connecting to: $host")
                }
                logger.info("connected")
            } catch (e: InterruptedException) {
                try {
                    zk!!.close()
                } catch (e1: InterruptedException) {
                    logger.error("$e1")
                }
                throw e
            }
        }
    } //-init

    fun createZNode(path: String, data: ByteArray?, acls: List<ACL>, mode: CreateMode, overwrite: Boolean = false) {
        logger.trace("create-znode:path=|$path|")
        logger.trace("create-znode:acls=|$acls|")
        logger.trace("create-znode:mode=|$mode|")
        if (data != null) {
            logger.trace("create-znode:data=|${String(data)}|")
        }

        createPath(path)
        logger.trace("create-znode.zk-create")
        try {
            val actual = zk?.create(path, data, acls, mode)
            logger.trace("create-znode.OK:actual=|$actual|")
        } catch (e: NodeExistsException) {
            if (overwrite) {
                logger.info("OVERWRITE: $path")
                setAcls(path, acls)
                if (data != null) {
                    setData(path, data)
                }
                logger.trace("create-znode.OK:path=|$path|")
            } else {
                logger.warn("Node already exists: path=$path")
            }
        }
    }


    fun deleteZNode(path: String) {
        logger.trace("delete-znode:path=|$path|")
        zk?.delete(path, -1)
    }

    fun exists(path: String, watch: Boolean = false): Boolean {
        return zk?.exists(path, watch) == null
    }

    fun setAcls(path: String, acls: List<ACL>) {
        logger.trace("set-acls:path=|$path|")
        logger.trace("set-acls:acls=|$acls|")
        createPath(path)
        val stat = zk?.setACL(path, acls, -1)
        logger.trace("set-acls:stat=$stat")
    }

    fun setData(path: String, data: ByteArray) {
        logger.trace("set-data:path=|$path|")
        logger.trace("create-znode:data=|${String(data)}|")
        createPath(path)
        val stat = zk?.setData(path, data, -1)
        logger.trace("set-data:stat=$stat")
    }


    fun createPath(path: String) {
        logger.trace("create-path:path=|$path|")
        if ("/" == path) {
            return
        }
        if (zk?.exists(path, false) == null) {
            createPath(getParentPath(path))
            try {
                logger.trace("zk.create-path:path=|$path|, acls=${Ids.OPEN_ACL_UNSAFE}")
                val actual = zk?.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
                logger.trace("zk.create-path:actual=|$actual|")
            } catch (e: NodeExistsException) { // Race condition
                logger.error("create-path-error: $e")
            }
        }
    }

    fun close() {
        zk?.close()
        if (connect) logger.info("close connection to $host")
    }

    private fun getParentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else "/"
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        val ZK_SESSION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30)
    }
}