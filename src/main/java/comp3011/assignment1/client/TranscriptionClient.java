package comp3011.assignment1.client;

//handles communication with the external speech-to-text api.
//builds the multipart audio request and converts the api response into our model.

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.model.TranscriptionResponse;
import reactor.core.publisher.Mono;

// @Component tells spring to create and manage this class as a bean, allowing it to be injected into other classes
@Component
public class TranscriptionClient {
	private final WebClient webClient;
	private final String apiKey;
	private final String transcriptionUrl;

	public TranscriptionClient(WebClient webClient,
			// @Value tells spring to inject this value from external configuration instead of hard-coding it
			@Value("${openai.api.key}") String apiKey,
			@Value("${openai.transcription.url}") String transcriptionUrl) {

		this.webClient = webClient;
		this.apiKey = apiKey;
		this.transcriptionUrl = transcriptionUrl;
	}

	// use Mono to represent an asynchronous result without blocking while waiting
	public Mono<TranscriptionResponse> transcribe(AudioData audio) {

		// build multipart/form-data request expected by transcription api
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

		// send request asynchronously by returning a mono
		return webClient.post().uri(transcriptionUrl).headers(headers -> headers.setBearerAuth(apiKey))
				.bodyValue(bodyBuilder.build()).retrieve().bodyToMono(TranscriptionResponse.class);
	}
}
