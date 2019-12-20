package zkr

import org.apache.zookeeper.txn.DeleteTxn
import org.apache.zookeeper.txn.TxnHeader
import zkr.ZNode.Companion.txn2String

class ZNodeDelete(override val options: ZkrOptions) : ZNode<DeleteTxn> {

    override fun process(hdr: TxnHeader, txn: DeleteTxn?) {
        val txnString = txn2String(hdr, txn)
        val s = "$txnString\n  path = ${txn?.path}"
        if (options.verbose) println(s)
        overwrite(hdr, txn, txnString)
    }

} //-ZNodeDelete