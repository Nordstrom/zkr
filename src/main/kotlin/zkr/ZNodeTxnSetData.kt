package zkr

import org.apache.zookeeper.txn.SetDataTxn
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNodeTxn.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeTxnSetData(override val options: ZkrOptions, override val zk: ZkClient, private val restore: Boolean = false) : ZNodeTxn<SetDataTxn> {

    override fun process(hdr: TxnHeader, txn: SetDataTxn?) {
        val txnString = txn2String(hdr, txn)
        var s = txnString
        if (txn?.data != null) {
            s += "\n  path = ${txn.path}"
            s += "\n  data = ${String(txn.data)}"
        }
        // SetDataTxn
        if (txn != null) {
            if (options.isPathExcluded(txn.path)) {
                ZNodeTxnCreate.logger.info("EXCLUDE: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }
            if (!restore) {
                logger.info("PRETEND: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }
            logger.debug(s)

            if (txn.data != null) {
                zk.setData(txn.path, txn.data)
                logger.info("setData at path=${txn.path}")
            }
        } else {
            logger.warn("Cannot setData on null txn: $txnString")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeTxnSetData