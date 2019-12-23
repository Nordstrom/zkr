package zkr

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.jmx.JmxConfig
import io.micrometer.jmx.JmxMeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.time.Duration

class PkeMeter(val prefix: String) {

    init {
        val registry = JmxMeterRegistry(object : JmxConfig {
            override fun step(): Duration {
                return Duration.ofSeconds(10)
            }

            override fun get(k: String): String? {
                return null
            }

            override fun domain(): String {
                return "muontrap-$prefix"
            }
        }, Clock.SYSTEM)
        registry.config().namingConvention(NamingConvention.dot)
        Metrics.addRegistry(registry)

    }

    val attempt = Metrics.globalRegistry.counter("$prefix.attempt.count")
    val error = Metrics.globalRegistry.counter("$prefix.error.count")
    val success = Metrics.globalRegistry.counter("$prefix.success.count")

    fun attempts() {
        attempt.increment()
    }

    fun errors(e: Exception) {
        logger.trace("$e")
        error.increment()
    }

    fun successes() {
        success.increment()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
} //-PkeMeter