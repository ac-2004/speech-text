package comp3011.assignment1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.client.TranscriptionClient;
import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.GlobalStatsResponse;
import comp3011.assignment1.model.TranscriptionResponse;
import comp3011.assignment1.model.TranscriptionUsage;
import reactor.core.publisher.Mono;

class TranscriptionServiceTest {

	@Test
	void transcribe_returnsTextAndUpdatesGlobalStatistics() {

		// Mock the external transcription client.
		TranscriptionClient transcriptionClient = mock(TranscriptionClient.class);

		// Use the real statistics service.
		GlobalStatisticsService globalStatisticsService = new GlobalStatisticsService();

		// Test the real transcription service.
		TranscriptionService transcriptionService = new TranscriptionService(transcriptionClient,
				globalStatisticsService);

		AudioData audio = new AudioData("fake audio".getBytes(StandardCharsets.UTF_8), "recording.webm", "audio/webm");

		TranscriptionUsage usage = new TranscriptionUsage(10, 5);

		TranscriptionResponse response = new TranscriptionResponse("hello world", usage);

		// Pretend the STT API returned the response above.
		when(transcriptionClient.transcribe(any(AudioData.class))).thenReturn(Mono.just(response));

		// Run the real service logic.
		String transcription = transcriptionService.transcribe(audio).block();

		// Check that the transcription text is returned.
		assertEquals("hello world", transcription);

		// Read the statistics after transcription.
		GlobalStatsResponse stats = globalStatisticsService.getStats();

		// Check that token usage was recorded correctly.
		assertEquals(10, stats.inputTokens());

		assertEquals(5, stats.outputTokens());
	}
}