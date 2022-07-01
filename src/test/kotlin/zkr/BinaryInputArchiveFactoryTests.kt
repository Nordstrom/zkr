package zkr

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class BinaryInputArchiveFactoryTests {
    @Test
    fun `can read a local transaction log`() {
        BinaryInputArchiveFactory(txnLog = "src/test/resources/log.1").create()
    }

    @Test
    fun `can read local Exhibitor backup (gzip) transaction log`() {
        BinaryInputArchiveFactory(txnLog = "src/test/resources/bak/log.1/1577209078000").create()
    }

    @Test
    fun `should throw exception for missing file`() {
        shouldThrow<FileNotFoundException> {
            BinaryInputArchiveFactory(txnLog = "where-is-waldo").create()
        }
    }

    @Test
    fun `should throws exception for invalid file`() {
        shouldThrow<InvalidMagicNumberException> {
            BinaryInputArchiveFactory(txnLog = "src/test/resources/not-log.1").create()
        }
    }
}