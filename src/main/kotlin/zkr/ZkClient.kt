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

class ZkClient(options:ZkrOptions) {
    private val zk: ZooKeeper

    init {
        val connected = CountDownLatch(1)
        logger.info("connecting to {}", options.host)
        zk = ZooKeeper(options.host, Ints.checkedCast(ZK_SESSION_TIMEOUT_MS), Watcher { event ->
            if (event.state == Watcher.Event.KeeperState.SyncConnected) {
                connected.countDown()
            }
        })
        try {
            if (!connected.await(ZK_SESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw IOException("Timeout out connecting to: $${options.host}")
            }
            logger.info("connected")
        } catch (e: InterruptedException) {
            try {
                zk.close()
            } catch (e1: InterruptedException) {
                e1.printStackTrace()
            }
            throw e
        }
    }

    fun createZNode(path: String, data: ByteArray?, acls: List<ACL>, mode: CreateMode) {
        createPath(path)
        zk.create(path, data, acls, mode)
    }

    fun deleteZNode(path: String) {
        zk.delete(path, -1)
    }

    fun setAcls(path: String, acls: List<ACL>) {
        createPath(path)
        zk.setACL(path, acls, -1)
    }

    fun setData(path: String, data: ByteArray) {
        createPath(path)
        zk.setData(path, data, -1)
    }


    private fun createPath(path: String) {
        if ("/" == path) {
            return
        }
        if (zk.exists(path, false) == null) {
            createPath(getParentPath(path))
            try {
                zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
            } catch (e: NodeExistsException) { // Race condition
                logger.error("$e")
            }
        }
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