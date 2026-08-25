package comp3011.assignment1.config;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

// configuration tells spring class contains configs for it to manage
@Configuration
public class WebClientConfig {
	
	// bean tells spring to run method and manage returned object
	@Bean
	public WebClient webClient() {
		
		HttpClient httpClient =
		        HttpClient.create()
		                  .proxyWithSystemProperties();
		
		
		return WebClient.builder()
				.baseUrl("https://api.openai.com")
				.clientConnector(
						new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
