package zkr

import org.apache.jute.Record
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNode.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeIgnore(override val options: ZkrOptions, override val zk: ZkClient) : ZNode<Record> {

    override fun process(hdr: TxnHeader, txn: Record?) {
        val txnString = txn2String(hdr, txn)
        // TODO shorter message txn.type, path
        if (options.verbose || options.overwrite || options.dryRun) logger.info(txnString)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeIgnore