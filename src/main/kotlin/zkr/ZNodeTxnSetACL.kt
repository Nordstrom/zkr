package zkr

import org.apache.zookeeper.txn.SetACLTxn
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNodeTxn.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeTxnSetACL(override val options: ZkrOptions, override val zk: ZkClient, private val restore: Boolean = false) : ZNodeTxn<SetACLTxn> {

    override fun process(hdr: TxnHeader, txn: SetACLTxn?) {
        val txnString = txn2String(hdr, txn)
        var s = txnString
        s += "\n  path = ${txn?.path}"
        s += "\n  acls = ${txn?.acl}"

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

            zk.setAcls(txn.path, txn.acl)
            logger.info("setAcls at path=${txn.path}")
        } else {
            ZNodeTxnSetData.logger.warn("Cannot setAcls on null txn: $txnString")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeTxnSetACL