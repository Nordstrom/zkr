package zkr

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.regex.Pattern
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZkrOptionsTests : Spek({

    describe("zkr exclude") {
        val options = ZkrOptions()

        it("can exclude a path") {
            options.excludes = listOf(
                    Pattern.compile("/kafka")
            )
            assertTrue(options.isPathExcluded("/kafka"))
            assertFalse(options.isPathExcluded("/zookeeper"))
        }

        it("can exclude multiple paths") {
            options.excludes = listOf(
                    Pattern.compile("/kafka"),
                    Pattern.compile("/zookeeper")
            )
            assertTrue(options.isPathExcluded("/kafka"))
            assertTrue(options.isPathExcluded("/zookeeper"))
            assertFalse(options.isPathExcluded("/notzookeeper"))
        }

        it("can exclude sub-paths") {
            options.excludes = listOf(
                    Pattern.compile("/kafka")
            )
            assertTrue(options.isPathExcluded("/kafka/config"))
            assertTrue(options.isPathExcluded("/kafka/config/topics"))
        }

        it("does not exclude parent path") {
            options.excludes = listOf(
                    Pattern.compile("/kafka/kafka-acl/Topic")
            )
            assertFalse(options.isPathExcluded("/kafka"))
            assertTrue(options.isPathExcluded("/kafka/kafka-acl/Topic"))
            assertTrue(options.isPathExcluded("/kafka/kafka-acl/Topic/blue.meanies"))
        }

        it("can exclude regex paths") {
            options.excludes = listOf(
                    Pattern.compile("/kafka/.*changes")
            )
            assertFalse(options.isPathExcluded("/kafka/kafka-acl"))
            assertTrue(options.isPathExcluded("/kafka/kafka-acl-changes"))
        }
    }//-describe-exclude

    describe("zkr include") {
        val options = ZkrOptions()

        it("can include a path") {
            options.includes = listOf(
                    Pattern.compile("/kafka")
            )
            assertTrue(options.isPathIncluded("/kafka"))
            assertFalse(options.isPathIncluded("/zookeeper"))
        }

        it("can include multiple paths") {
            options.includes = listOf(
                    Pattern.compile("/kafka"),
                    Pattern.compile("/zookeeper")
            )
            assertTrue(options.isPathIncluded("/kafka"))
            assertTrue(options.isPathIncluded("/zookeeper"))
            assertFalse(options.isPathIncluded("/notzookeeper"))
        }

        it("can include sub-paths") {
            options.includes = listOf(
                    Pattern.compile("/kafka")
            )
            assertTrue(options.isPathIncluded("/kafka/config"))
            assertTrue(options.isPathIncluded("/kafka/config/topics"))
        }

        it("does not include parent path") {
            options.includes = listOf(
                    Pattern.compile("/kafka/kafka-acl/Topic")
            )
            assertFalse(options.isPathIncluded("/kafka"))
            assertTrue(options.isPathIncluded("/kafka/kafka-acl/Topic"))
            assertTrue(options.isPathIncluded("/kafka/kafka-acl/Topic/blue.meanies"))
        }

        it("can include regex paths") {
            options.includes = listOf(
                    Pattern.compile("/kafka/.*changes")
            )
            assertFalse(options.isPathIncluded("/kafka/kafka-acl"))
            assertTrue(options.isPathIncluded("/kafka/kafka-acl-changes"))
        }
    }//-describe-include

    describe("zkr should include") {
        val options = ZkrOptions()

        it("should include path with mutually exclusive includes/excludes") {
            options.includes = listOf(
                    Pattern.compile("/kafka/kafka-acl/Topic")
            )
            options.excludes = listOf(
                    Pattern.compile("/kafka/kafka-acl/Group")
            )

            assertFalse(options.shouldInclude("/kafka/kafka-acl"), "should not include '/kafka/kafka-acl'")
            assertTrue(options.shouldInclude("/kafka/kafka-acl/Topic"), "should include '/kafka/kafka-acl/Topic'")
            assertFalse(options.shouldInclude("/kafka/kafka-acl/Group"), "should not include '/kafka/kafka-acl/Group'")
            assertFalse(options.shouldInclude("/kafka/kafka-acl/Group/blue.meanies"), "should not include '/kafka/kafka-acl/Group/blue.meanies'")
        }

        it("should not include path with same includes/excludes") {
            options.includes = listOf(
                    Pattern.compile("/kafka/kafka-acl"),
                    Pattern.compile("/kafka/kafka-acl/Topic")
            )
            options.excludes = listOf(
                    Pattern.compile("/kafka/kafka-acl/Topic")
            )

            assertFalse(options.shouldInclude("/kafka/kafka-acl/Topic"), "should not include '/kafka/kafka-acl/Topic'")
            assertFalse(options.shouldInclude("/kafka/kafka-acl/Topic/blue.meanies"), "should not include '/kafka/kafka-acl/Topic/blue.meanies'")
            assertTrue(options.shouldInclude("/kafka/kafka-acl/Group"), "should include '/kafka/kafka-acl/Group'")
        }

        it("should include only") {
            options.includes = listOf(
                    Pattern.compile("/kafka/kafka-acl/")
            )
            assertFalse(options.isPathIncluded("/"), "should not include '/'")
            assertFalse(options.isPathIncluded("/kafka"), "should not include '/kafka'")
            assertFalse(options.isPathIncluded("/kafka/kafka-acl-changes"), "should not include '/kafka/kafka-acl-changes'")
            assertTrue(options.isPathIncluded("/kafka/kafka-acl/"), "should include '/kafka/kafka-acl/'")
            assertTrue(options.isPathIncluded("/kafka/kafka-acl/Topic"), "should include '/kafka/kafka-acl/Topic")

        }
    }

}) //-Spek
