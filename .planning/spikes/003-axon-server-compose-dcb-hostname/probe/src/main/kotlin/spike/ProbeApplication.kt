package spike

import java.util.UUID
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventhandling.annotation.Event
import org.axonframework.messaging.eventhandling.gateway.EventGateway
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@Event(namespace = "spike", name = "ProbePinged", version = "1")
data class ProbePinged(@field:EventTag(key = "probe") val probeId: String, val from: String)

@SpringBootApplication
class ProbeApplication {
    /** Spike 001 check: a ConfigurationEnhancer bean is picked up by Spring Boot and can decorate the storage engine. */
    @Bean
    fun captureEnhancer() = org.axonframework.common.configuration.ConfigurationEnhancer { registry ->
        registry.registerDecorator(EventStorageEngine::class.java, 0) { _, _, delegate ->
            object : EventStorageEngine by delegate {
                override fun appendEvents(
                    condition: org.axonframework.eventsourcing.eventstore.AppendCondition,
                    context: org.axonframework.messaging.core.unitofwork.ProcessingContext?,
                    events: List<org.axonframework.eventsourcing.eventstore.TaggedEventMessage<*>>,
                ) = delegate.appendEvents(condition, context, events).also {
                    println("ENHANCER-DECORATOR saw append: criteria=${condition.criteria()} marker=${condition.consistencyMarker()} events=${events.size}")
                }
            }
        }
    }

    @Bean
    fun probe(
        gateway: EventGateway,
        @Qualifier("org.axonframework.eventsourcing.eventstore.EventStorageEngine") engine: EventStorageEngine,
        @Value("\${probe.from:host}") from: String,
        @Value("\${probe.hold-seconds:0}") holdSeconds: Long,
    ) = ApplicationRunner {
        val probeId = UUID.randomUUID().toString()
        gateway.publish(null, ProbePinged(probeId, from)).get()
        val read: List<EventMessage> =
            engine
                .source(SourcingCondition.conditionFor(EventCriteria.havingTags(Tag.of("probe", probeId))))
                .reduce(mutableListOf<EventMessage>()) { acc, entry: MessageStream.Entry<EventMessage> ->
                    acc!!.also { it += entry.message() }
                }
                .get()!!
        println("PROBE RESULT from=$from probeId=$probeId events=${read.size} types=${read.map { it.type() }} payload=${read.map { it.payload()?.let { p -> if (p is ByteArray) String(p) else p.toString() } }}")
        if (holdSeconds > 0) {
            Thread.sleep(holdSeconds * 1000)
            val again = UUID.randomUUID().toString()
            gateway.publish(null, ProbePinged(again, "$from-after-hold")).get()
            println("PROBE AFTER HOLD published $again")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ProbeApplication>(*args)
}
