package co.com.crediya.webclient.gateway;

import co.com.crediya.model.customer.UserData;
import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.webclient.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
                .onErrorResume(e -> Mono.just(false))
                .doOnSuccess(response -> log.info("Petition complete successfully, existsByEmail response: {}", response));
    }

    @Override
    public Mono<UserData> findByEmail(String email) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    if (securityContext.getAuthentication() instanceof JwtAuthenticationToken) {
                        String jwtToken = ((JwtAuthenticationToken) securityContext.getAuthentication()).getToken().getTokenValue();

                        return authWebClient.get()
                                .uri(props.getEmailInfoPath(), email)
                                .headers(headers -> headers.setBearerAuth(jwtToken))
                                .retrieve()
                                .bodyToMono(UserData.class);
                    } else {
                        return Mono.error(new IllegalStateException("JWT Token not found in security context"));
                    }
                })
                .doOnError(e -> log.error("Error in findByEmail: {}", e.getMessage()));
    }
}
