package zkr

import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import picocli.CommandLine
import java.lang.NullPointerException
import java.lang.System.err
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    describe("zkr") {
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
            //TODO how to capture and search stderr for expectedf content?
        }

        it("can handle CREATE znode transaction") {
            val app = Zkr()
            val opts = ZkrOptions()
            opts.host = "nohost:1234"
            opts.txnLog = "nolog"
            opts.dryRun = true
            opts.exclude = emptyList()
            //TODO mock ZkClient if not 'dryRun'
            app.zk = ZkClient(opts)

            app.options = opts
            val hdr = TxnHeader()
            hdr.clientId = 111
            hdr.cxid = 222
            hdr.time = Date().time
            hdr.type = ZooDefs.OpCode.ping
            hdr.zxid = 333
            val txn = CreateTxn()
            txn.path = "/"
            app.processTxn(hdr, txn)

            //TODO asserts
        }

    } //-describe

}) //-Spek
