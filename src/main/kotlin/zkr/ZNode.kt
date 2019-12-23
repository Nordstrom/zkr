package zkr

import org.apache.jute.Record
import org.apache.zookeeper.txn.TxnHeader
import java.text.SimpleDateFormat
import java.util.*

interface ZNode<T : Any> {
    val options: ZkrOptions?
    val zk: ZkClient

    fun process(hdr: TxnHeader, txn: T?)

    companion object {
        private const val HDR_FORMAT = "%-29s sid=0x%-16s cxid=0x%-3s zxid=0x%-3s %-16s"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        fun txn2String(hdr: TxnHeader, txn: Record?, short: Boolean = false): String {
            val data = if (txn == null) "TXN_NULL" else "$txn".trim()
            return txnHeaderString(hdr, txn) + ", data=" + data
        }

        private fun txnHeaderString(hdr: TxnHeader, txn: Record?): String {
            val date = DATE_FORMAT.format(Date(hdr.time))
            val scid = java.lang.Long.toHexString(hdr.clientId)
            val cxid = java.lang.Long.toHexString(hdr.cxid.toLong())
            val zxid = java.lang.Long.toHexString(hdr.zxid)
            val type = ZkOpCode.op2String(hdr.type) + "." + if (txn == null) "class=" else txn.javaClass.simpleName
            return String.format(HDR_FORMAT, date, scid, cxid, zxid, type)
        }

        //TODO ZNodeBase factory
//        fun create(options:ZkrOptions, hdr:TxnHeader, txn:Record?) : ZNodeBase {
//            when (txn) {
//                is CreateTxn -> ZNodeCreate(options)
//            }
//        }
    }//-companion

} //-ZNode