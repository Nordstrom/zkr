package zkr

import org.apache.jute.Record
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNodeTxn.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeTxnIgnore(override val options: ZkrOptions, override val zk: ZkClient) : ZNodeTxn<Record> {

    override fun process(hdr: TxnHeader, txn: Record?) {
        val txnString = txn2String(hdr, txn)
        logger.debug(txnString)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeTxnIgnore