package zkr

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BinaryInputArchiveFactoryTests : Spek({

    describe("binary-input-archive-factory tests") {

        it("nop-test") {
            assertEquals(42, 42)
        }

        it("can read local transaction log") {
            BinaryInputArchiveFactory(txnLog = "src/test/resources/log.1").create()
        }

        it("can read local Exhibitor backup (gzip) transaction log") {
            BinaryInputArchiveFactory(txnLog = "src/test/resources/bak/log.1/1577209078000").create()
        }

        it("throws exception for non-existent file") {
            assertFailsWith<FileNotFoundException> {
                BinaryInputArchiveFactory(txnLog = "where-is-waldo").create()
            }
        }

        it("throws exception for invalid file") {
            assertFailsWith<InvalidMagicNumberException> {
                BinaryInputArchiveFactory(txnLog = "src/test/resources/not-log.1").create()
            }
        }

    } //-describe

}) //-Spek
