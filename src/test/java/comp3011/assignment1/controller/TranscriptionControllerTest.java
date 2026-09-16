package comp3011.assignment1.controller;

//mvc regression tests for multipart transcription requests and asynchronous errors.

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

// real STT/OpenAI path is not started.
@WebMvcTest(TranscriptionController.class)
public class TranscriptionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TranscriptionService transcriptionService;

	@Test
	void receiveAudio_returnsStubbedTranscription() throws Exception {
		// Create a fake WebM file matching the multipart field expected by TranscriptionController.
		MockMultipartFile audioFile = new MockMultipartFile("audio", // Multipart field name
				"recording.webm", // Uploaded filename
				"audio/webm", // Content type
				new byte[] { 1, 2, 3, 4 } // Small fake audio payload
		);

		// mock the external work while testing the real multipart controller path
		when(transcriptionService.transcribe(any(AudioData.class))).thenReturn(Mono.just("hello from stub"));

		// mono responses enter spring mvc asynchronous processing first		
		MvcResult mvcResult = mockMvc.perform(multipart("/api/transcriptions").file(audioFile))
				.andExpect(request().asyncStarted())
				.andReturn();

		// now dispatch the completed asynchronous result back through Spring MVC.
		mockMvc.perform(asyncDispatch(mvcResult))

				// The completed request should return HTTP 200.
				.andExpect(status().isOk())

				// The client should receive exactly the transcription produced by our stubbed service.
				.andExpect(content().string("hello from stub"));
	}

	// stimulate failure
	@Test
	void transcribe_whenServiceFails_returnsStandard500Error() throws Exception {
		when(transcriptionService.transcribe(any(AudioData.class)))
				.thenReturn(Mono.error(new RuntimeException("simulated transcription failure")));

		MockMultipartFile audioFile = new MockMultipartFile("audio", "recording.webm", "audio/webm",
				"fake audio data".getBytes());

		MvcResult mvcResult = mockMvc.perform(multipart("/api/transcriptions").file(audioFile))
				.andExpect(request().asyncStarted()).andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))

				.andExpect(status().isInternalServerError())

				.andExpect(jsonPath("$.status").value(500))

				.andExpect(jsonPath("$.error").value("Internal Server Error"))

				.andExpect(jsonPath("$.message").value("An unexpected server error occurred."))

				.andExpect(jsonPath("$.path").value("/api/transcriptions"))

				.andExpect(jsonPath("$.timestamp").exists());
	}
}