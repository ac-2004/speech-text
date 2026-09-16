package comp3011.assignment1.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import comp3011.assignment1.client.TranscriptionClient;
import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.TranscriptionResponse;
import comp3011.assignment1.model.TranscriptionUsage;
import reactor.core.publisher.Mono;

@ExtendWith(OutputCaptureExtension.class)
class TranscriptionServiceLoggingTest {

	@Test
	void successfulTranscription_writesOperationalLogs(CapturedOutput output) {

		TranscriptionClient transcriptionClient = mock(TranscriptionClient.class);

		GlobalStatisticsService globalStatisticsService = new GlobalStatisticsService();

		TranscriptionService transcriptionService = new TranscriptionService(transcriptionClient,
				globalStatisticsService);

		AudioData audio = new AudioData(new byte[] { 1, 2, 3 }, "recording.webm", "audio/webm");

		TranscriptionUsage usage = new TranscriptionUsage(10, 5);

		TranscriptionResponse response = new TranscriptionResponse("hello world", usage);

		when(transcriptionClient.transcribe(any(AudioData.class))).thenReturn(Mono.just(response));

		transcriptionService.transcribe(audio).block();

		assertTrue(output.getOut().contains("Transcription request started"));

		assertTrue(output.getOut().contains("Transcription request completed"));
	}

	@Test
	void failedTranscription_writesSafeErrorLog(CapturedOutput output) {

		TranscriptionClient transcriptionClient = mock(TranscriptionClient.class);

		GlobalStatisticsService globalStatisticsService = new GlobalStatisticsService();

		TranscriptionService transcriptionService = new TranscriptionService(transcriptionClient,
				globalStatisticsService);

		AudioData audio = new AudioData(new byte[] { 1, 2, 3 }, "recording.webm", "audio/webm");

		when(transcriptionClient.transcribe(any(AudioData.class)))
				.thenReturn(Mono.error(new RuntimeException("sensitive-error-details")));

		try {
			transcriptionService.transcribe(audio).block();
		} catch (RuntimeException exception) {
			// Expected because the mock simulates an STT failure.
		}

		assertTrue(output.getOut().contains("Transcription request started"));

		assertTrue(output.getOut().contains("Transcription request failed"));

		assertTrue(!output.getOut().contains("sensitive-error-details"));
	}
}
