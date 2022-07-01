package zkr

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class ZkrOptionsTests {
    private lateinit var options: ZkrOptions

    @BeforeEach
    fun init() {
        options = ZkrOptions()
    }

    @Test
    fun `can exclude a path`() {
        options.excludes = listOf(
            Pattern.compile("/kafka")
        )

       options.isPathExcluded("/kafka").shouldBeTrue()
       options.isPathExcluded("/zookeeper").shouldBeFalse()
    }

    @Test
    fun `can exclude many paths`() {
        options.excludes = listOf(
            Pattern.compile("/kafka"),
            Pattern.compile("/zookeeper")
        )
        options.isPathExcluded("/kafka").shouldBeTrue()
        options.isPathExcluded("/zookeeper").shouldBeTrue()
        options.isPathExcluded("/notzookeeper").shouldBeFalse()
    }

    @Test
    fun `can exclude sub-paths`() {
        options.excludes = listOf(
                Pattern.compile("/kafka")
        )

        options.isPathExcluded("/kafka/config").shouldBeTrue()
        options.isPathExcluded("/kafka/config/topics").shouldBeTrue()
    }

    @Test
    fun `should not exclude parent path`() {
        options.excludes = listOf(
            Pattern.compile("/kafka/kafka-acl/Topic")
        )

        options.isPathExcluded("/kafka").shouldBeFalse()
        options.isPathExcluded("/kafka/kafka-acl/Topic").shouldBeTrue()
        options.isPathExcluded("/kafka/kafka-acl/Topic/blue.meanies").shouldBeTrue()
    }

    @Test
    fun `can exclude paths with regex`() {
        options.excludes = listOf(
            Pattern.compile("/kafka/.*changes")
        )

        options.isPathExcluded("/kafka/kafka-acl").shouldBeFalse()
        options.isPathExcluded("/kafka/kafka-acl-changes").shouldBeTrue()
    }

    @Test
    fun `can include a path`() {
        options.includes = listOf(
            Pattern.compile("/kafka")
        )

        options.isPathIncluded("/kafka").shouldBeTrue()
        options.isPathIncluded("/zookeeper").shouldBeFalse()
        options.isPathIncluded("/").shouldBeFalse()
    }

    @Test
    fun `can include many paths`() {
        options.includes = listOf(
            Pattern.compile("/kafka"),
            Pattern.compile("/zookeeper")
        )

        options.isPathIncluded("/kafka").shouldBeTrue()
        options.isPathIncluded("/zookeeper").shouldBeTrue()
        options.isPathIncluded("/notzookeeper").shouldBeFalse()
    }

    @Test
    fun `can include sub-paths`() {
        options.includes = listOf(
            Pattern.compile("/kafka")
        )

        options.isPathIncluded("/kafka").shouldBeTrue()
        options.isPathIncluded("/kafka/config").shouldBeTrue()
        options.isPathIncluded("/kafka/config/topics").shouldBeTrue()
    }

    @Test
    fun `should not include parent path`() {
        options.includes = listOf(
            Pattern.compile("/kafka/kafka-acl/Topic")
        )

        options.isPathIncluded("/kafka").shouldBeFalse()
        options.isPathIncluded("/kafka/kafka-acl/Topic").shouldBeTrue()
        options.isPathIncluded("/kafka/kafka-acl/Topic/blue.meanies").shouldBeTrue()
    }

    @Test
    fun `can include paths with regex`() {
        options.includes = listOf(
            Pattern.compile("/kafka/.*changes")
        )

        options.isPathIncluded("/kafka/kafka-acl").shouldBeFalse()
        options.isPathIncluded("/kafka/kafka-acl-changes").shouldBeTrue()
    }

    @Test
    fun `can include paths with mutually exclusive includes and excludes`() {
        options.includes = listOf(
            Pattern.compile("/kafka/kafka-acl/Topic")
        )
        options.excludes = listOf(
            Pattern.compile("/kafka/kafka-acl/Group")
        )

        options.shouldInclude("/kafka/kafka-acl").shouldBeFalse()
        options.shouldInclude("/kafka/kafka-acl/Topic").shouldBeTrue()
        options.shouldInclude("/kafka/kafka-acl/Group").shouldBeFalse()
        options.shouldInclude("/kafka/kafka-acl/Group/blue.meanies").shouldBeFalse()
    }

    @Test
    fun `should not include path with same includes and excludes`() {
        options.includes = listOf(
                Pattern.compile("/kafka/kafka-acl"),
                Pattern.compile("/kafka/kafka-acl/Topic")
        )
        options.excludes = listOf(
                Pattern.compile("/kafka/kafka-acl/Topic")
        )

        options.shouldInclude("/kafka/kafka-acl/Topic").shouldBeFalse()
        options.shouldInclude("/kafka/kafka-acl/Topic/blue.meanies").shouldBeFalse()
        options.shouldInclude("/kafka/kafka-acl/Group").shouldBeTrue()
    }

    @Test
    fun `should include using explicit separator`() {
        options.includes = listOf(
            Pattern.compile("/kafka/kafka-acl/")
        )

        options.isPathIncluded("/").shouldBeFalse()
        options.isPathIncluded("/kafka").shouldBeFalse()
        options.isPathIncluded("/kafka/kafka-acl-changes").shouldBeFalse()
        options.isPathIncluded("/kafka/kafka-acl/").shouldBeTrue()
        options.isPathIncluded("/kafka/kafka-acl/Topic").shouldBeTrue()
    }
}
