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

class ZkClient(val host: String, val connect: Boolean = true, superDigestPassword: String = "", sessionTimeoutMillis: Long = 30000) {
    var zk: ZooKeeper? = null

    init {
        if (!connect) {
            logger.debug("no connection to ZooKeeper for --dry-run")
        } else {
            val connected = CountDownLatch(1)
            logger.debug("connecting to $host")
            zk = ZooKeeper(host, Ints.checkedCast(sessionTimeoutMillis), Watcher { event ->
                if (event.state == Watcher.Event.KeeperState.SyncConnected) {
                    connected.countDown()
                }
            })
            try {
                if (!connected.await(sessionTimeoutMillis, TimeUnit.MILLISECONDS)) {
                    throw IOException("Timeout out connecting to: $host")
                }
                logger.debug("connected")
                if (superDigestPassword.isNotBlank()) {
                    logger.debug("using superdigest")
                    zk!!.addAuthInfo("digest", "super:$superDigestPassword".toByteArray())
                }
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
        createPath(path)
        try {
            val actual = zk?.create(path, data, acls, mode)
        } catch (e: NodeExistsException) {
            if (overwrite) {
                logger.debug("OVERWRITE: $path")
                setAcls(path, acls)
                if (data != null) {
                    setData(path, data)
                }
            } else {
                logger.warn("Node already exists: path=$path")
            }
        }
    }


    fun deleteZNode(path: String) {
        zk?.delete(path, -1)
    }

    fun exists(path: String, watch: Boolean = false): Boolean {
        return zk?.exists(path, watch) == null
    }

    fun setAcls(path: String, acls: List<ACL>) {
        createPath(path)
        val stat = zk?.setACL(path, acls, -1)
    }

    fun setData(path: String, data: ByteArray) {
        createPath(path)
        val stat = zk?.setData(path, data, -1)
    }


    fun createPath(path: String) {
        if ("/" == path) {
            return
        }
        if (zk?.exists(path, false) == null) {
            createPath(getParentPath(path))
            try {
                val actual = zk?.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
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
    }
} //-ZkClient

class ZkSocketClient(val zkString: String) : Closeable {
    val connected: Boolean
    val socket: Socket

    init {
        val parts = zkString.split(":")
        val host = parts[0]
        val port = parts[1].toInt()
        logger.debug("zk-socket connecting to $host:$port")
        socket = Socket(host, port)
        connected = true
        logger.debug("zk-socket $connected")
    }

    val reader = Scanner(socket.getInputStream())
    val writer = socket.getOutputStream()

    fun isLeaderOrStandalone(): Boolean {
        var leader = false
        if (connected) {
            write("stat")
            val lines = read()
            val mode = lines.filter { it.contains("Mode:") }
            leader = mode.any { it.contains("standalone", ignoreCase = true) || it.contains("leader", ignoreCase = true) }
            logger.debug("$zkString: mode=$mode, is-leader=$leader")
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} // ZkSocketClient