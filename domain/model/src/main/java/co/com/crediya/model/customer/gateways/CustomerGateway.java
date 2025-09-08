package co.com.crediya.model.customer.gateways;

import co.com.crediya.model.customer.UserData;
import reactor.core.publisher.Mono;

public interface CustomerGateway {
    Mono<Boolean> existsByEmail(String email);
    Mono<UserData> findByEmail(String email);
}
