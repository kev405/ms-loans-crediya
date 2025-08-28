package co.com.crediya.model.customer.gateways;

import reactor.core.publisher.Mono;

public interface CustomerGateway {
    Mono<Boolean> existsByEmail(String email);
}
