package comp3011.assignment1.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.TranscriptionResponse;
import reactor.core.publisher.Mono;

// use component because it communicates with an external system
@Component
public class TranscriptionClient {
	private final WebClient webClient;
	private final String apiKey;
	private final String transcriptionUrl;

	public TranscriptionClient(WebClient webClient, @Value("${openai.api.key}") String apiKey,
			@Value("${openai.transcription.url}") String transcriptionUrl) {

		this.webClient = webClient;
		this.apiKey = apiKey;
		this.transcriptionUrl = transcriptionUrl;
	}

	public Mono<TranscriptionResponse> transcribe(AudioData audio) {

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
		bodyBuilder.part("model", "gpt-4o-mini-transcribe");

		// send request asynchronously
		return webClient.post().uri(transcriptionUrl).headers(headers -> headers.setBearerAuth(apiKey))
				.bodyValue(bodyBuilder.build()).retrieve().bodyToMono(TranscriptionResponse.class);
	}
}
