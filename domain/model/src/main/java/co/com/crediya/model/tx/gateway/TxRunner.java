package co.com.crediya.model.tx.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.function.Supplier;

public interface TxRunner {
    <T> Mono<T> required(Supplier<Mono<T>> action);
    <T> Flux<T> requiredMany(Supplier<Flux<T>> action);

    default <T> Mono<T> readOnly(Supplier<Mono<T>> action) { return required(action); }
    default <T> Flux<T> readOnlyMany(Supplier<Flux<T>> action) { return requiredMany(action); }
}
