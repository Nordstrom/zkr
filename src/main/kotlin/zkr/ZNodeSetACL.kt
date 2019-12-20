package zkr

import org.apache.zookeeper.txn.SetACLTxn
import org.apache.zookeeper.txn.TxnHeader
import zkr.ZNode.Companion.txn2String

class ZNodeSetACL(override val options: ZkrOptions) : ZNode<SetACLTxn> {

    override fun process(hdr: TxnHeader, txn: SetACLTxn?) {
        val txnString = txn2String(hdr, txn)
        var s = txnString
        s += "\n  path = ${txn?.path}"
        s += "\n  acls = ${txn?.acl}"
        if (options.verbose) println(s)
        overwrite(hdr, txn, txnString)
    }

} //-ZNodeSetACL