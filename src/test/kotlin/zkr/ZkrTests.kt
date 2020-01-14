package zkr

import org.apache.curator.test.TestingServer
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import picocli.CommandLine
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


// kotlin-test
class ZkrTest {
    @Test
    fun nop() {
        assertTrue(42 == 42)
    }
}

// spek-framework
class ZkrTests : Spek({

    val ZK_PORT = 12181

    describe("zkr") {
        lateinit var zk: TestingServer
        lateinit var opts: ZkrOptions
        beforeEachTest {
            zk = TestingServer(ZK_PORT, true)

            opts = ZkrOptions()
            opts.host = "localhost:$ZK_PORT"
        }
        afterEachTest { zk.close() }

        it("nop-test") {
            assertEquals(42, 42)
        }

        it("should get missing arg log message") {
            val args = emptyArray<String>()
            CommandLine(Zkr()).execute(*args)
            //TODO how to capture and search stderr for expected content?
        }

        it("should get FileNotFoundException log message") {
            //NB: We can either specify --dry-run, or create TestingServer (see ZkClientTests) so
            // the implicit connection to ZooKeeper does not affect the test.
            val args = arrayOf<String>("--dry-run", "not-a-file")
            CommandLine(Zkr()).execute(*args)
            //TODO how to capture and search stderr for expected content?
        }

        it("can process CREATE znode transaction") {
            val app = Zkr()
            opts.host = "localhost:2181"
            opts.file = "nolog"
            opts.excludes = emptyList()
            val restore = RestoreOptions()
            val logs = Logs()
            logs.options = opts
            logs.restore = restore
            logs.zk = ZkClient(host = opts.host, connect = true)

            val hdr = TxnHeader()
            hdr.clientId = 111
            hdr.cxid = 222
            hdr.time = Date().time
            hdr.type = ZooDefs.OpCode.ping
            hdr.zxid = 333
            val txn = CreateTxn()
            txn.path = "/"
            val acls = mutableListOf<ACL>(
                    ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE)
            )
            txn.acl = acls
            logs.processTxn(hdr, txn)

            //TODO asserts
        }

    } //-describe

}) //-Spek
