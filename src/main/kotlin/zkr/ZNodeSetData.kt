package zkr

import org.apache.zookeeper.txn.SetDataTxn
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNode.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeSetData(override val options: ZkrOptions, override val zk: ZkClient) : ZNode<SetDataTxn> {

    override fun process(hdr: TxnHeader, txn: SetDataTxn?) {
        val txnString = txn2String(hdr, txn)
        var s = txnString
        if (txn?.data != null) {
            s += "\n  path = ${txn.path}"
            s += "\n  data = ${String(txn.data)}"
        }
        // SetDataTxn
        if (txn != null) {
            if (shouldExclude(txn.path)) {
                ZNodeCreate.logger.info("EXCLUDE: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }
            if (options.verbose) logger.info(s)

            if (options.dryRun) {
                logger.info("PRETEND: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }

            if (txn.data != null) {
                zk.setData(txn.path, txn.data)
                logger.info("setData for ${txn.path}")
            }
        } else {
            logger.warn("Cannot setData on null txn: $txnString")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeSetData