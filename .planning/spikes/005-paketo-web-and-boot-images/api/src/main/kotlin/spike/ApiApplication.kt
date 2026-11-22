package spike

import java.time.Duration
import java.time.Instant
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@SpringBootApplication
class ApiApplication

@RestController
class SpikeController {
    @GetMapping("/api/hello") fun hello() = mapOf("hello" to "from boot", "at" to Instant.now().toString())

    /** Five events, 500 ms apart: arrival times show whether a proxy buffers the stream. */
    @GetMapping("/api/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(): Flux<ServerSentEvent<String>> =
        Flux.interval(Duration.ofMillis(500)).take(5).map { ServerSentEvent.builder("tick $it at ${Instant.now()}").id("$it").build() }
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
