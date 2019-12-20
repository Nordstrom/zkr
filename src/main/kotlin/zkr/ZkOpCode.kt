package zkr

import org.apache.zookeeper.ZooDefs

object ZkOpCode {
    enum class ZkOpCodes {
        notification,
        create,
        delete,
        exists,
        getData,
        setData,
        getACL,
        setACL,
        getChildren,
        sync,
        ping,
        getChildren2,
        check,
        multi,
        create2,
        reconfig,
        checkWatches,
        removeWatches,
        createContainer,
        deleteContainer,
        createTTL,
        auth,
        setWatches,
        sasl,
        createSession,
        closeSession,
        error
    }

    fun op2String(op: Int): String {
        return when (op) {
            ZooDefs.OpCode.auth -> ZkOpCodes.auth.name
            ZooDefs.OpCode.check -> ZkOpCodes.check.name
            ZooDefs.OpCode.checkWatches -> ZkOpCodes.checkWatches.name
            ZooDefs.OpCode.closeSession -> ZkOpCodes.closeSession.name
            ZooDefs.OpCode.create -> ZkOpCodes.create.name
            ZooDefs.OpCode.create2 -> ZkOpCodes.create2.name
            ZooDefs.OpCode.createContainer -> ZkOpCodes.createContainer.name
            ZooDefs.OpCode.createSession -> ZkOpCodes.createSession.name
            ZooDefs.OpCode.createTTL -> ZkOpCodes.createTTL.name
            ZooDefs.OpCode.delete -> ZkOpCodes.delete.name
            ZooDefs.OpCode.deleteContainer -> ZkOpCodes.deleteContainer.name
            ZooDefs.OpCode.error -> ZkOpCodes.error.name
            ZooDefs.OpCode.exists -> ZkOpCodes.exists.name
            ZooDefs.OpCode.getACL -> ZkOpCodes.getACL.name
            ZooDefs.OpCode.getChildren -> ZkOpCodes.getChildren.name
            ZooDefs.OpCode.getChildren2 -> ZkOpCodes.getChildren2.name
            ZooDefs.OpCode.getData -> ZkOpCodes.getData.name
            ZooDefs.OpCode.multi -> ZkOpCodes.multi.name
            ZooDefs.OpCode.notification -> ZkOpCodes.notification.name
            ZooDefs.OpCode.ping -> ZkOpCodes.ping.name
            ZooDefs.OpCode.reconfig -> ZkOpCodes.reconfig.name
            ZooDefs.OpCode.removeWatches -> ZkOpCodes.removeWatches.name
            ZooDefs.OpCode.sasl -> ZkOpCodes.sasl.name
            ZooDefs.OpCode.setACL -> ZkOpCodes.setACL.name
            ZooDefs.OpCode.setData -> ZkOpCodes.setData.name
            ZooDefs.OpCode.setWatches -> ZkOpCodes.setWatches.name
            ZooDefs.OpCode.sync -> ZkOpCodes.sync.name

            else -> "unknown OpCode $op"
        }
    }
}
