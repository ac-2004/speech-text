package comp3011.assignment1.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.service.TranscriptionService;
import reactor.core.publisher.Mono;


// Starts the real Spring Boot application on a random available port.
//
// This is stronger than using MockMvc for this particular test because
// we want to exercise the actual HTTP server and controller layer under
// concurrent load.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class TranscriptionConcurrencyTest {

    // Spring stores the randomly selected server port here.
    // Using a random port avoids clashes with another application
    // that may already be running on port 8080.
    @LocalServerPort
    private int port;


    // Replace the real transcription service with a Mockito mock.
    //
    // This is important because this test is checking OUR application's
    // concurrency behaviour. We do not want 250 real OpenAI API requests.
    @MockitoBean
    private TranscriptionService transcriptionService;


    @Test
    void transcriptionEndpoint_handlesMoreThan200ConcurrentRequests()
            throws Exception {

        // ARRANGE ------------------------------------------------------------

        // The HD rubric requires more than 200 simultaneous HTTP requests.
        // We use 250 to clearly exceed that requirement.
        int requestCount = 250;


        // Simulate an asynchronous transcription operation.
        //
        // Each request takes approximately 200 ms, but Mono.delay() does not
        // block a server thread while it is waiting.
        //
        // This helps demonstrate why reactive/non-blocking processing is useful
        // when many requests overlap.
        when(transcriptionService.transcribe(any(AudioData.class)))
                .thenReturn(
                        Mono.delay(Duration.ofMillis(200))
                                .thenReturn("stub transcription")
                );


        // Build a test WebClient manually.
        //
        // This client sends real HTTP requests to the Spring Boot server
        // started above on the random port.
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();


        // Store the future representing each HTTP request.
        //
        // We collect them first rather than immediately waiting for each one.
        // That allows many requests to remain in flight at the same time.
        List<CompletableFuture<String>> requests =
                new ArrayList<>();


        // Measure total elapsed time so we can detect accidental
        // serialisation or major performance regressions.
        long startTime = System.nanoTime();


        // ACT ----------------------------------------------------------------

        // Launch 250 HTTP requests.
        for (int i = 0; i < requestCount; i++) {

            // Build a small fake file for the multipart upload.
            //
            // ByteArrayResource represents the binary file contents.
            // getFilename() is overridden because Spring needs a filename
            // in order to treat the multipart field as an uploaded file.
            ByteArrayResource audioResource =
                    new ByteArrayResource(
                            "fake audio data".getBytes()
                    ) {

                        @Override
                        public String getFilename() {
                            return "recording.webm";
                        }
                    };


            CompletableFuture<String> request =
                    webClient.post()

                            // Send the request through the real
                            // TranscriptionController endpoint.
                            .uri("/api/transcriptions")

                            // The controller expects multipart/form-data,
                            // just like the browser sends with FormData.
                            .contentType(MediaType.MULTIPART_FORM_DATA)

                            // Add a multipart file field named "audio".
                            //
                            // This must match:
                            // @RequestParam("audio")
                            // in the TranscriptionController.
                            .body(
                                    BodyInserters.fromMultipartData(
                                            "audio",
                                            audioResource
                                    )
                            )

                            // Perform the HTTP request and treat non-2xx
                            // responses as errors.
                            .retrieve()

                            // The transcription endpoint returns plain text.
                            .bodyToMono(String.class)

                            // Convert the reactive Mono into a CompletableFuture
                            // so all requests can be collected and awaited.
                            .toFuture();


            requests.add(request);
        }


        // Wait until every one of the 250 requests has completed.
        //
        // If one request completes exceptionally, join() will surface
        // the error and the test will fail.
        CompletableFuture.allOf(
                requests.toArray(new CompletableFuture[0])
        ).join();


        // Calculate total execution time in milliseconds.
        long elapsedMillis =
                (System.nanoTime() - startTime) / 1_000_000;


        // ASSERT -------------------------------------------------------------

        // Every request must return the expected transcription.
        //
        // This proves that the application did not merely survive the load;
        // all individual HTTP requests still completed correctly.
        for (CompletableFuture<String> request : requests) {

            assertEquals(
                    "stub transcription",
                    request.get()
            );
        }


        // Confirm that exactly 250 requests were created and completed.
        assertEquals(
                requestCount,
                requests.size()
        );


        // Performance regression check.
        //
        // We intentionally use a generous threshold because test machines
        // vary in speed. The goal is not to benchmark exact milliseconds.
        //
        // Instead, this guards against a regression where requests become
        // heavily serialised, blocked, or otherwise take an excessive amount
        // of time under concurrent load.
        assertTrue(
                elapsedMillis < 10_000,
                "Concurrent requests took too long: "
                        + elapsedMillis
                        + " ms"
        );
    }
}