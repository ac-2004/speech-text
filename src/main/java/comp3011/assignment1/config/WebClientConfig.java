package comp3011.assignment1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// configuration tells spring class contains configs for it to manage
@Configuration
public class WebClientConfig {
	
	// bean tells spring to run method and manage returned object
	@Bean
	public WebClient webClient() {
		return WebClient.builder()
				.baseUrl("https://api.openai.com")
				.build();
	}
}
