package comp3011.assignment1.concurrency;

//load regression test that sends 250 concurrent http transcription requests.
//blocking work is simulated to check the application stays responsive under contention.


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.service.TranscriptionService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TranscriptionConcurrencyTest {

	@LocalServerPort
	private int port;

	// Prevents the test from making real OpenAI requests.
	@MockitoBean
	private TranscriptionService transcriptionService;

	@Test
	void transcriptionEndpoint_handlesMoreThan200ConcurrentRequests() throws Exception {

		int requestCount = 250;

		// simulate blocking downstream work on a scheduler intended for blocking tasks
		when(transcriptionService.transcribe(any(AudioData.class))).thenAnswer(invocation -> Mono.fromCallable(() -> {

			// Simulate a blocking external operation.
			Thread.sleep(500);

			return "stub transcription";

		}).subscribeOn(Schedulers.boundedElastic()));

		// give the load client enough connections to create the intended concurrency
		ConnectionProvider connectionProvider = ConnectionProvider.builder("concurrency-test-pool")
				.maxConnections(requestCount).pendingAcquireMaxCount(requestCount)
				.pendingAcquireTimeout(Duration.ofSeconds(10)).build();

		HttpClient httpClient = HttpClient.create(connectionProvider);

		WebClient webClient = WebClient.builder().baseUrl("http://localhost:" + port)
				.clientConnector(new ReactorClientHttpConnector(httpClient)).build();

		List<CompletableFuture<String>> requests = new ArrayList<>();

		long startTime = System.nanoTime();

		// Launch all 250 HTTP requests without waiting between requests.
		for (int i = 0; i < requestCount; i++) {

			ByteArrayResource audioResource = new ByteArrayResource(
					"fake audio data".getBytes(StandardCharsets.UTF_8)) {
				@Override
				public String getFilename() {
					return "recording.webm";
				}
			};

			CompletableFuture<String> request = webClient.post().uri("/api/transcriptions")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(BodyInserters.fromMultipartData("audio", audioResource)).retrieve().bodyToMono(String.class)
					.toFuture();

			requests.add(request);
		}

		// wait until every in-flight request has completed
		CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0])).join();

		long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;

		// Verify every request returned correctly.
		for (CompletableFuture<String> request : requests) {
			assertEquals("stub transcription", request.get());
		}

		assertEquals(requestCount, requests.size());

		// threshold to detect major concurrency regressions.
		assertTrue(elapsedMillis < 15_000, "Concurrent requests took too long: " + elapsedMillis + " ms");

		// Clean up the client's connection pool.
		connectionProvider.disposeLater().block();
	}
}