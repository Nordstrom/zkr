package zkr

import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNode.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeCreate(override val options: ZkrOptions, override val zk: ZkClient) : ZNode<CreateTxn> {

    override fun process(hdr: TxnHeader, txn: CreateTxn?) {
        val txnString = txn2String(hdr, txn)
        // Only create persistent (i.e., non-ephemeral) znodes.
        if (!txn?.ephemeral!!) {
            if (options.verbose) logger.info(txnString)

            if (options.dryRun) {
                logger.info("PRETEND: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }

            create(hdr, txn)
        }
    }

    private fun create(hdr: TxnHeader, txn: CreateTxn?) {
        if (txn != null) {
            try {
                zk.createZNode(txn.path, txn.data, txn.acl, CreateMode.PERSISTENT)
                logger.info("Created node: path=${txn.path}")
            } catch (e: NodeExistsException) {
                if (options.overwrite) { // TODO: Compare with current data / acls
                    logger.info("Overwrite node: path=${txn.path}")
                    zk.setAcls(txn.path, txn.acl)
                    if (txn.data != null) {
                        zk.setData(txn.path, txn.data)
                    }
                } else {
                    logger.warn("Node already exists: path=${txn.path}")
                }
            }
        } else {
            logger.warn("Cannot create null txn")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeCreate