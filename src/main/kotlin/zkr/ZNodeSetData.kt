package zkr

import org.apache.zookeeper.txn.SetDataTxn
import org.apache.zookeeper.txn.TxnHeader
import zkr.ZNode.Companion.txn2String

class ZNodeSetData(override val options: ZkrOptions) : ZNode<SetDataTxn> {

    override fun process(hdr: TxnHeader, txn: SetDataTxn?) {
        val txnString = txn2String(hdr, txn)
        var s = txnString
        if (txn?.data != null) {
            s += "\n  path = ${txn.path}"
            s += "\n  data = ${String(txn.data)}"
        }
        if (options.verbose) println(s)
        overwrite(hdr, txn, txnString)
    }

} //-ZNodeSetData