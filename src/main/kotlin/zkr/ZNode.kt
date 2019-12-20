package zkr

import org.apache.jute.Record
import org.apache.zookeeper.txn.TxnHeader
import java.text.SimpleDateFormat
import java.util.*

interface ZNode<T> {
    val options: ZkrOptions

    fun process(hdr: TxnHeader, txn: T?)

    fun overwrite(hdr: TxnHeader, txn: T?, txnString: String) {
        if (options.overwrite) println("TODO_OVERWRITE: $txnString")
    }


    companion object {
        const val HDR_FORMAT = "%-29s sid=0x%-16s cxid=0x%-3s zxid=0x%-3s %-16s"
        const val TXN_FORMAT = "%-29s sid=0x%-16s cxid=0x%-3s zxid=0x%-3s %-16s %s"
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        fun txn2String(hdr: TxnHeader, txn: Record?): String {
            val data = if (txn == null) "TXN NULL" else "$txn".trim()
            return txnHeaderString(hdr, txn) + data
        }

        fun txnHeaderString(hdr: TxnHeader, txn: Record?): String {
            val date = DATE_FORMAT.format(Date(hdr.time))
            val scid = java.lang.Long.toHexString(hdr.clientId)
            val cxid = java.lang.Long.toHexString(hdr.cxid.toLong())
            val zxid = java.lang.Long.toHexString(hdr.zxid)
            val type = ZkOpCode.op2String(hdr.type) + "." + if (txn == null) "unk" else txn.javaClass.simpleName
            return String.format(HDR_FORMAT, date, scid, cxid, zxid, type)
        }
    }//-companion

} //-ZNode