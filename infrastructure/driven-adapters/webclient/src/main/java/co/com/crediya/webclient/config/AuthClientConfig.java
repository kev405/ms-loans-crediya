package co.com.crediya.webclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AuthClientConfig {
    @Bean
    @ConfigurationProperties(prefix = "adapters.auth")
    public AuthProperties authProperties() { return new AuthProperties(); }

    @Bean
    public WebClient authWebClient(AuthProperties props) {
        return WebClient.builder().baseUrl(props.getBaseUrl()).build();
    }
}
