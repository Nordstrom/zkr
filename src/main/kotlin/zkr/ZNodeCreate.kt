package zkr

import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import zkr.ZNode.Companion.txn2String

class ZNodeCreate(override val options: ZkrOptions) : ZNode<CreateTxn> {

    override fun process(hdr: TxnHeader, txn: CreateTxn?) {
        val txnString = txn2String(hdr, txn)
        // Only create persistent (i.e., non-ephemeral) znodes.
        if (!txn?.ephemeral!!) {
            var s = txnString
            if (txn.data != null) {
                s += "\n  path = ${txn.path}"
                s += "\n  acls = ${txn.acl}"
                s += "\n  data = ${String(txn.data)}"
            }
            if (options.verbose) println(s)
            overwrite(hdr, txn, txnString)
        }
    }

} //-ZNodeCreate