package zkr

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.lang.invoke.MethodHandles

object CliHelper {
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    fun trapSignal(signal: String): ReceiveChannel<Unit> {
        val channel = Channel<Unit>()

        Signal.handle(Signal(signal)) { sig ->
            logger.warn("signaled:$sig")
            GlobalScope.launch {
                channel.send(Unit)
            }
        }

        return channel
    }

    fun intToAscii(value: Int): String? {
        return hexToAscii(value.toString(16))
    }

    fun hexToAscii(hexStr: String): String? {
        val output = java.lang.StringBuilder("")
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }
} //-CliHelper