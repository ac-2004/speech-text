package comp3011.assignment1.client;

import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.ByteArrayResource;

import comp3011.assignment1.model.TranscriptionResponse;

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
		
		// build multipart/form-data request
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		
		// wrap audio bytes so webclient can send them as file
		ByteArrayResource audioResource = new ByteArrayResource(audio.data()) {
			@Override
			public String getFilename() {
				return audio.filename();
			}
		};
		
		// fields expected by transcription api
		bodyBuilder.part("file", audioResource);
		bodyBuilder.part("model",  "gpt-4o-mini-transcribe");
		
		// send request asynchronously
		return webClient.post()
		        .uri("/v1/audio/transcriptions")
		        .headers(headers -> headers.setBearerAuth(apiKey))
		        .bodyValue(bodyBuilder.build())
		        .retrieve()
		        .bodyToMono(TranscriptionResponse.class)
		        .map(response -> response.text());
	}
}

