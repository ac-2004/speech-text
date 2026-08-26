package comp3011.assignment1.service;

import org.springframework.stereotype.Service;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.client.TranscriptionClient;
import reactor.core.publisher.Mono;


@Service
public class TranscriptionService {
	private final TranscriptionClient transcriptionClient;
	private final GlobalStatisticsService globalStatisticsService;
	
	public TranscriptionService(TranscriptionClient transcriptionClient, GlobalStatisticsService globalStatisticsService) {
		this.transcriptionClient = transcriptionClient;
		this.globalStatisticsService = globalStatisticsService;
	}
	
	public Mono<String> transcribe(AudioData audio) {
		
		return transcriptionClient.transcribe(audio)
				.map(response -> {
					globalStatisticsService.addUsage(
								response.usage().inputTokens(),							response.usage().outputTokens()
);
					return response.text();
				});
	}
}
