package zkr

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Stat

//TODO use kotlinx serialization. ACL and Stat may require custom serializers
@Serializable
data class BackupZNode(
        val path: String,
        val ephemeralOwner: Long,
        val data: ByteArray?,
        val acls: List<@ContextualSerialization ACL>,
        @ContextualSerialization
        val stat: Stat? = null
) {

    override fun toString(): String {
        val dat = if (data == null) "DATA NULL" else String(data)
        return "path=$path, ephemeralOwner=$ephemeralOwner, data=$dat, acls=$acls, stat=$stat"
    }

    companion object {
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
    }
} //-BackupZNode
