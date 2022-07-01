package zkr

import org.apache.curator.test.TestingServer
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date
import picocli.CommandLine

class ZkrTests {
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
    fun `should get missing arg log message`() {
        val args = emptyArray<String>()
        CommandLine(Zkr()).execute(*args)
        //TODO how to capture and search stderr for expected content?
    }

    @Test
    fun `should get FileNotFound log message`() {
        // We can either specify --dry-run, or create TestingServer (see ZkClientTests) so
        // the implicit connection to ZooKeeper does not affect the test.
        val args = arrayOf("--dry-run", "not-a-file")
        CommandLine(Zkr()).execute(*args)
        //TODO how to capture and search stderr for expected content?
    }

    @Test
    fun `can process CREATE znode transaction`() {
            opts.host = "localhost:$ZK_PORT"
            opts.file = "nolog"
            opts.excludes = emptyList()
            val restore = LogsOptions()
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
            txn.acl = mutableListOf(
                ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE)
            )
            logs.processTxn(hdr, txn)

            //TODO asserts
    }
}
