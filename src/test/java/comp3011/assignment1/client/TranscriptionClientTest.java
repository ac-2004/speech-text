package comp3011.assignment1.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.TranscriptionResponse;

class TranscriptionClientTest {

	private HttpServer stubServer;
	private TranscriptionClient transcriptionClient;

	private String receivedAuthorization;
	private String receivedContentType;
	private String receivedRequestBody;

	@BeforeEach
	void setUp() throws IOException {

		// Start a local fake transcription server on a free port.
		stubServer = HttpServer.create(new InetSocketAddress(0), 0);

		// Handle requests sent to the transcription endpoint.
		stubServer.createContext("/v1/audio/transcriptions", this::handleTranscriptionRequest);

		stubServer.start();

		int port = stubServer.getAddress().getPort();

		String stubUrl = "http://localhost:" + port + "/v1/audio/transcriptions";

		// Use the real client but point it at the fake server.
		transcriptionClient = new TranscriptionClient(WebClient.builder().build(), "test-api-key", stubUrl);
	}

	@AfterEach
	void tearDown() {

		// Stop the fake server after the test finishes.
		if (stubServer != null) {
			stubServer.stop(0);
		}
	}

	@Test
	void transcribe_sendsMultipartRequestAndParsesResponse() {

		// Create fake audio data for the request.
		AudioData audio = new AudioData("fake audio".getBytes(StandardCharsets.UTF_8), "recording.webm", "audio/webm");

		// Call the real TranscriptionClient.
		TranscriptionResponse response = transcriptionClient.transcribe(audio).block();

		// Check that the response was parsed correctly.
		assertEquals("stub transcription", response.text());

		assertEquals(12, response.usage().inputTokens());

		assertEquals(4, response.usage().outputTokens());

		// Check that Bearer authentication was included.
		assertEquals("Bearer test-api-key", receivedAuthorization);

		// Check that the request was multipart/form-data.
		assertTrue(receivedContentType.startsWith("multipart/form-data"));

		// Check that important multipart values were sent.
		assertTrue(receivedRequestBody.contains("recording.webm"));

		assertTrue(receivedRequestBody.contains("gpt-4o-mini-transcribe"));
	}

	private void handleTranscriptionRequest(HttpExchange exchange) throws IOException {

		// Store request details so the test can check them later.
		receivedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");

		receivedContentType = exchange.getRequestHeaders().getFirst("Content-Type");

		receivedRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);

		// Return a fake response shaped like the real STT API response.
		String responseBody = """
				{
				    "text": "stub transcription",
				    "usage": {
				        "type": "tokens",
				        "input_tokens": 12,
				        "output_tokens": 4,
				        "total_tokens": 16
				    }
				}
				""";

		byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

		exchange.getResponseHeaders().add("Content-Type", "application/json");

		exchange.sendResponseHeaders(200, responseBytes.length);

		try (OutputStream outputStream = exchange.getResponseBody()) {

			outputStream.write(responseBytes);
		}
	}
}