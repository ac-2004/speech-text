package comp3011.assignment1.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

// use component because it communicates with an external system
@Component
public class TranscriptionClient {
	private final WebClient webClient;
	private final String apiKey;
	
	public TranscriptionClient(
			WebClient webClient, 
			@Value("${openai.api.key}") String apiKey) {
		this.webClient = webClient;
		this.apiKey = apiKey;
	}
	
	
	public Mono<String> transcribe(AudioData audio) {
		
		return Mono.just("client placeholder");
	}
}

