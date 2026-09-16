package comp3011.assignment1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
//Performs the second dispatch after the asynchronous Mono has completed.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//Lets the test verify that Spring started asynchronous request processing.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
//Stores the result of the first asynchronous request.
import org.springframework.test.web.servlet.MvcResult;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.service.TranscriptionService;
import reactor.core.publisher.Mono;

// Loads only the MVC layer needed to test TranscriptionController.
// The real STT/OpenAI path is not started.
@WebMvcTest(TranscriptionController.class)
public class TranscriptionControllerTest {

	// Lets us simulate HTTP requests to the controller without launching a real web
	// server.
	@Autowired
	private MockMvc mockMvc;

	// Replaces the real TranscriptionService with a mock.
	// This prevents tests from calling the real Cloud STT service.
	@MockitoBean
	private TranscriptionService transcriptionService;

	@Test
	void receiveAudio_returnsStubbedTranscription() throws Exception {

		// ARRANGE:
		// Create a fake WebM file matching the multipart field expected
		// by TranscriptionController.
		MockMultipartFile audioFile = new MockMultipartFile("audio", // Multipart field name
				"recording.webm", // Uploaded filename
				"audio/webm", // Content type
				new byte[] { 1, 2, 3, 4 } // Small fake audio payload
		);

		// Replace the real Cloud STT behaviour with a predictable result.
		// No external OpenAI request is made during this regression test.
		when(transcriptionService.transcribe(any(AudioData.class))).thenReturn(Mono.just("hello from stub"));

		// ACT - PHASE 1:
		// Start the multipart request. Because the controller returns Mono<String>,
		// Spring MVC processes the response asynchronously.
		MvcResult mvcResult = mockMvc.perform(multipart("/api/transcriptions").file(audioFile))

				// Verify that the controller actually entered asynchronous processing.
				.andExpect(request().asyncStarted())

				// Save the request so we can inspect its completed response next.
				.andReturn();

		// ACT + ASSERT - PHASE 2:
		// Dispatch the completed asynchronous result back through Spring MVC.
		mockMvc.perform(asyncDispatch(mvcResult))

				// The completed request should return HTTP 200.
				.andExpect(status().isOk())

				// The client should receive exactly the transcription
				// produced by our stubbed service.
				.andExpect(content().string("hello from stub"));
	}

	@Test
	void transcribe_whenServiceFails_returnsStandard500Error() throws Exception {

		// ARRANGE:
		// Simulate an asynchronous failure from the transcription service.
		when(transcriptionService.transcribe(any(AudioData.class)))
				.thenReturn(Mono.error(new RuntimeException("simulated transcription failure")));

		// Create a small fake audio upload.
		MockMultipartFile audioFile = new MockMultipartFile("audio", "recording.webm", "audio/webm",
				"fake audio data".getBytes());

		// ACT:
		// Because the controller returns Mono<String>, Spring begins
		// asynchronous request processing instead of completing immediately.
		MvcResult mvcResult = mockMvc.perform(multipart("/api/transcriptions").file(audioFile))
				.andExpect(request().asyncStarted()).andReturn();

		// ASSERT:
		// Dispatch the completed asynchronous result back through Spring MVC.
		mockMvc.perform(asyncDispatch(mvcResult))

				// The global exception handler should convert the unexpected
				// transcription failure into HTTP 500.
				.andExpect(status().isInternalServerError())

				// Verify the standard ErrorResponse contract.
				.andExpect(jsonPath("$.status").value(500))

				.andExpect(jsonPath("$.error").value("Internal Server Error"))

				.andExpect(jsonPath("$.message").value("An unexpected server error occurred."))

				.andExpect(jsonPath("$.path").value("/api/transcriptions"))

				.andExpect(jsonPath("$.timestamp").exists());
	}
}