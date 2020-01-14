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
import java.io.Closeable
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException

class ZkClient(val host: String, val connect: Boolean = true) {
    var zk: ZooKeeper? = null

    init {
        if (!connect) {
            logger.debug("no connection to ZooKeeper for --dry-run")
        } else {
            val connected = CountDownLatch(1)
            logger.debug("connecting to $host")
            zk = ZooKeeper(host, Ints.checkedCast(ZK_SESSION_TIMEOUT_MS), Watcher { event ->
                if (event.state == Watcher.Event.KeeperState.SyncConnected) {
                    connected.countDown()
                }
            })
            try {
                if (!connected.await(ZK_SESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    throw IOException("Timeout out connecting to: $host")
                }
                logger.debug("connected")
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
                logger.debug("OVERWRITE: $path")
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
        if (connect) logger.debug("close connection to $host")
    }

    private fun getParentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else "/"
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        val ZK_SESSION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30)
    }
} //-ZkClient

class ZkSocketClient(zkString: String) : Closeable {
    val connected: Boolean
    val socket: Socket

    init {
        val parts = zkString.split(":")
        val host = parts[0]
        val port = parts[1].toInt()
        socket = Socket(host, port)
        connected = true
    }

    val reader = Scanner(socket.getInputStream())
    val writer = socket.getOutputStream()

    fun isLeader(): Boolean {
        var leader = false
        if (connected) {
            write("stat")
            val lines = read()
            leader = lines.filter { it.contains("Mode:") }.contains("leader")
        }
        return leader
    }

    fun read(): MutableList<String> {
        val lines = mutableListOf<String>()
        try {
            while (connected) {
                lines.add(reader.nextLine())
            }
        } catch (e: NoSuchElementException) {
            //We don't have any more to read
        }

        return lines
    }

    fun write(message: String) {
        if (connected) {
            writer.write("$message\n".toByteArray(Charset.defaultCharset()))
        }
    }

    override fun close() {
        reader.close()
        writer.close()
        socket.close()
    }

} // ZkSocketClient