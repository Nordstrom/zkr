package zkr

import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.txn.CreateTxn
import org.apache.zookeeper.txn.TxnHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zkr.ZNodeTxn.Companion.txn2String
import java.lang.invoke.MethodHandles

class ZNodeTxnCreate(override val options: ZkrOptions, override val zk: ZkClient, private val overwrite: Boolean = false, private val dryRun: Boolean = false) : ZNodeTxn<CreateTxn> {

    override fun process(hdr: TxnHeader, txn: CreateTxn?) {
        val txnString = txn2String(hdr, txn)
        // Only create persistent (i.e., non-ephemeral) znodes.
        if (!txn?.ephemeral!!) {
            if (shouldExclude(txn.path)) {
                logger.info("EXCLUDE: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }
            if (dryRun) {
                logger.info("PRETEND: txn=${txn.javaClass.simpleName}, path=${txn.path}")
                return
            }
            if (options.verbose) logger.info(txnString)

            create(txn)
        }
    }

    private fun create(txn: CreateTxn?) {
        if (txn != null) {
            zk.createZNode(txn.path, txn.data, txn.acl, CreateMode.PERSISTENT, overwrite)
            logger.info("Created znode at path=${txn.path}")
        } else {
            logger.warn("Cannot process null txn")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-ZNodeTxnCreate