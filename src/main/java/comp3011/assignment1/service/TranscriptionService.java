package comp3011.assignment1.service;

import org.springframework.stereotype.Service;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.client.TranscriptionClient;
import reactor.core.publisher.Mono;

@Service
public class TranscriptionService {
	private final TranscriptionClient transcriptionClient;
	public TranscriptionService(TranscriptionClient transcriptionClient) {
		this.transcriptionClient = transcriptionClient;
	}
	
	public Mono<String> transcribe(AudioData audio) {
		
		return transcriptionClient.transcribe(audio);
	}
}
