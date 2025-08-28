package co.com.crediya.webclient.gateway;

import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.webclient.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerGatewayAdapter implements CustomerGateway {

    private final WebClient authWebClient;
    private final AuthProperties props;

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return authWebClient.get()
                .uri(props.getExistsPath(), email)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(e -> Mono.just(false));
    }
}
