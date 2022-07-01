package zkr

import org.apache.curator.test.TestingServer
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.ACL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ZkClientTests {
    private val ZK_PORT = 12181
    private lateinit var zk: TestingServer
    private lateinit var opts: ZkrOptions

    @BeforeEach
    fun setup() {
        zk = TestingServer(ZK_PORT, true)
        opts = ZkrOptions()
        opts.host = "localhost:$ZK_PORT"
    }

    @AfterEach
    fun teardown() {
        zk.close()
    }

    @Test
    fun `can initialize with ZooKeeper`() {
        ZkClient(opts.host, true)
    }

    @Test @Disabled
    fun `can create a znode`() {
        //val path = "/kafka/config/topics/chief.blue.meanie"
        //val data = """
        //    {"version":1,"partitions":{"2":[1],"1":[1],"0":[1]}}
        //""".trimIndent().toByteArray()
        val acls = mutableListOf(
            ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE)
            //, ACL(ZooDefs.Perms.ALL, Id("sasl", "developer"))
        )
        println("acls.tested=$acls")
        println("acls.unsafe=${ZooDefs.Ids.OPEN_ACL_UNSAFE}")
        //val mode = CreateMode.PERSISTENT
        ZkClient(opts.host, true)
        //TODO
        //try {
        //    zkc.createZNode(path, data, acls, mode, true)
        //} catch (e: KeeperException.NodeExistsException) {
        //    println("ERR: $e, cause=${e.cause}")
        //}
        //TODO get path, data, ACLs from TestingServer and verify
    }
}
