package comp3011.assignment1.service;

import org.springframework.stereotype.Service;


import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.client.TranscriptionClient;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class TranscriptionService {
	private final TranscriptionClient transcriptionClient;
	private final GlobalStatisticsService globalStatisticsService;
	private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);
	
	public TranscriptionService(TranscriptionClient transcriptionClient, GlobalStatisticsService globalStatisticsService) {
		this.transcriptionClient = transcriptionClient;
		this.globalStatisticsService = globalStatisticsService;
	}
	
	public Mono<String> transcribe(AudioData audio) {
		logger.info("Transcription request started");
		
		return transcriptionClient.transcribe(audio)
				.map(response -> {
					globalStatisticsService.addUsage(
								response.usage().inputTokens(),							response.usage().outputTokens()
);
					logger.info("Transcription request completed");
					
					return response.text();
				})
				.doOnError(error ->
                logger.error(
                        "Transcription request failed: {}",
                        error.getClass().getSimpleName()
                )
                );
	}
}
