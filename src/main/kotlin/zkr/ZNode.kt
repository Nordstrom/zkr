package zkr

import org.apache.jute.Record
import org.apache.zookeeper.txn.TxnHeader
import java.text.SimpleDateFormat
import java.util.*

interface ZNode<T : Any> {
    val options: ZkrOptions
    val zk: ZkClient

    fun process(hdr: TxnHeader, txn: T?)

    fun shouldExclude(path: String): Boolean {
        val excluded = options.excludes.filter { path.startsWith(it) }
        return excluded.isNotEmpty()
    }


    companion object {
        private const val HDR_FORMAT = "%-29s sid=0x%-16s cxid=0x%-3s zxid=0x%-3s %-16s"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        fun txn2String(hdr: TxnHeader, txn: Record?): String {
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

        fun createFullPath(path: String, childPath: String): String {
            return if (path.endsWith("/")) {
                path + childPath
            } else {
                "$path/$childPath"
            }
        }

        fun <T> nullToEmpty(original: List<T>?): List<T> {
            return original ?: emptyList()
        }

        const val FIELD_AVERSION = "aversion"
        const val FIELD_CTIME = "ctime"
        const val FIELD_CVERSION = "cversion"
        const val FIELD_CZXID = "czxid"
        const val FIELD_EPHEMERAL_OWNER = "ephemeralOwner"
        const val FIELD_MTIME = "mtime"
        const val FIELD_MZXID = "mzxid"
        const val FIELD_PZXID = "pzxid"
        const val FIELD_VERSION = "version"
        const val FIELD_DATA = "data"
        const val FIELD_ACLS = "acls"
        const val FIELD_ACL_ID = "id"
        const val FIELD_ACL_SCHEME = "scheme"
        const val FIELD_ACL_PERMS = "perms"
    }//-companion

} //-ZNode