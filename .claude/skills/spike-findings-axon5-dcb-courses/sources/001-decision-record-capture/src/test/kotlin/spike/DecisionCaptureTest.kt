package spike

import io.axoniq.framework.testcontainer.AxonServerContainer
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID
import java.util.concurrent.CompletableFuture
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import spike.capture.DecisionRecord.Outcome
import spike.capture.DecisionRecorder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecisionCaptureTest {
    private val server =
        AxonServerContainer("docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot").withDevMode(true).withDcbContext(true)

    @BeforeAll fun start() = server.start()
    @AfterAll fun stop() = server.stop()

    fun cases() = Variant.entries.flatMap { v -> listOf("in-memory", "axon-server").map { arrayOf<Any>(v, it) } }

    private fun backend(name: String) = if (name == "in-memory") Backend.InMemory else Backend.AxonServer(server.axonServerAddress)

    private fun dump(recorder: DecisionRecorder) = recorder.records.forEach { println("  RECORD $it") }

    @ParameterizedTest(name = "{0} on {1}")
    @MethodSource("cases")
    fun `records criteria, marker, sourced and appended events, and outcomes`(variant: Variant, backendName: String) {
        val recorder = DecisionRecorder()
        val axon = startAxon(variant, backend(backendName), recorder, retries = false)
        try {
            val gw = axon.getComponent(CommandGateway::class.java)
            val course = "course-" + UUID.randomUUID()
            gw.send(CreateCourse(course, 1)).getResultMessage().get()
            gw.send(Subscribe(course, "s1")).resultAs(SubscribeResult::class.java).get() shouldBe SubscribeResult.Subscribed
            gw.send(Subscribe(course, "s2")).resultAs(SubscribeResult::class.java).get() shouldBe SubscribeResult.Rejected("course-full")
            println("== $variant on $backendName"); dump(recorder)

            recorder.records shouldHaveSize 3
            val (create, ok, full) = recorder.records
            create.outcome.shouldBeInstanceOf<Outcome.Committed>()
            create.appendCriteria shouldBe "AnyEvent" // no sourcing, no entity: AppendCondition.none(), i.e. no consistency check
            create.consistencyMarker shouldBe "INFINITY"
            ok.sourcingCriteria.single() shouldContain course
            ok.sourcedEvents shouldHaveSize 1
            ok.appendedEvents shouldHaveSize 1
            ok.consistencyMarker!!.isNotBlank() shouldBe true
            ok.outcome.shouldBeInstanceOf<Outcome.Committed>().markerAfterCommit!!.isNotBlank() shouldBe true
            full.sourcedEvents shouldHaveSize 2
            full.outcome shouldBe Outcome.NothingAppended
        } finally {
            axon.shutdown()
        }
    }

    @ParameterizedTest(name = "{0} on {1}")
    @MethodSource("cases")
    fun `concurrent subscriptions conflict, retry and never exceed capacity`(variant: Variant, backendName: String) {
        val recorder = DecisionRecorder()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(12)
        val axon = startAxon(variant, backend(backendName), recorder, retries = true)
        try {
            val gw = axon.getComponent(CommandGateway::class.java)
            val course = "course-" + UUID.randomUUID()
            gw.send(CreateCourse(course, 3)).getResultMessage().get()
            recorder.records.clear()
            val results =
                (1..12).map { i -> CompletableFuture.supplyAsync({ gw.send(Subscribe(course, "s$i")).resultAs(SubscribeResult::class.java).join() }, pool) }
                    .let { CompletableFuture.allOf(*it.toTypedArray()).thenApply { _ -> it.map { f -> f.join() } } }
                    .get()
            println("== $variant on $backendName: results=${results.groupingBy { it }.eachCount()}")
            val conflicts = recorder.records.filter { it.outcome is Outcome.Conflict }
            println("  attempts=${recorder.records.size} conflicts=${conflicts.size} maxAttempt=${recorder.records.maxOf { it.attempt }}")
            conflicts.take(1).forEach { println("  CONFLICT ${it.outcome}") }

            if (variant == Variant.ENGINE) conflicts.forEach { (it.outcome as Outcome.Conflict).conflictingEvent!! shouldContain "StudentSubscribed" }
            recorder.records.filter { it.outcome is Outcome.Committed }.forEach {
                (it.outcome as Outcome.Committed).markerAfterCommit!! shouldContain "GlobalIndexConsistencyMarker"
            }
            results.count { it == SubscribeResult.Subscribed } shouldBe 3
            results.count { it == SubscribeResult.Rejected("course-full") } shouldBe 9
            recorder.records.count { it.outcome is Outcome.Committed } shouldBe 3
            recorder.records.filter { it.attempt > 1 }.forEach { r ->
                recorder.records.any { it.commandId == r.commandId && it.attempt == r.attempt - 1 && it.outcome is Outcome.Conflict } shouldBe true
            }
        } finally {
            axon.shutdown()
        }
    }
}
