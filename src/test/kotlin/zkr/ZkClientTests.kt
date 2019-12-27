package zkr

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import org.apache.curator.test.TestingServer
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Id

class ZkClientTests : Spek({

    val ZK_PORT = 12181

    describe("zk-client nop") {
        it("nop-test") {
            assertEquals(42, 42)
        }

    }

    describe("zk-client tests") {
        lateinit var zk: TestingServer
        lateinit var opts: ZkrOptions
        beforeEachTest {
            zk = TestingServer(ZK_PORT, true)

            opts = ZkrOptions()
            opts.host = "localhost:$ZK_PORT"
        }
        afterEachTest { zk.close() }


        it("can initialize with ZooKeeper") {
            ZkClient(opts)
        }

        it("TODO: can create a znode") {
            val path = "/kafka/config/topics/chief.blue.meanie"
            val data = """
                {"version":1,"partitions":{"2":[1],"1":[1],"0":[1]}}
            """.trimIndent().toByteArray()
            val acls = mutableListOf<ACL>(
                    ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE)
//                    , ACL(ZooDefs.Perms.ALL, Id("sasl", "developer"))
            )
            println("acls.tested=$acls")
            println("acls.unsafe=${ZooDefs.Ids.OPEN_ACL_UNSAFE}")
            val mode = CreateMode.PERSISTENT
            val zkc = ZkClient(opts)
            //TODO
//            try {
//                zkc.createZNode(path, data, acls, mode, true)
//            } catch (e: KeeperException.NodeExistsException) {
//                println("ERR: $e, cause=${e.cause}")
//            }

            //TODO get path, data, acls from TestingServer and verify
        }

    } //-describe

}) //-Spek
