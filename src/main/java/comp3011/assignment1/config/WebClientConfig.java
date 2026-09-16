package comp3011.assignment1.config;

//configures the shared webclient used for outbound speech-to-text requests.
//system proxy settings are supported so the client also works in the titan environment.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

// @configuration tells spring this class defines application configuration and the beans it should manage
@Configuration
public class WebClientConfig {

	// bean tells spring to run method and manage returned object so it can be injected elsewhere
	@Bean
	public WebClient webClient() {

		// use system proxy properties so outbound requests work in titan
		HttpClient httpClient = HttpClient.create().proxyWithSystemProperties();

		return WebClient.builder().baseUrl("https://api.openai.com")
				.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}
}
