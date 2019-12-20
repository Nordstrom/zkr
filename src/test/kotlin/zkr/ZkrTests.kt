package zkr

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTest {
    @Test
    fun nop() {
        assertTrue(42 == 42)
    }
}

class AppTests : Spek({
    describe("app") {
        it("nop-test") {
            assertEquals(42, 42)
        }
    }
})
