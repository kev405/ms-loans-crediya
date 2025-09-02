package co.com.crediya.r2dbc.tx;

import co.com.crediya.model.tx.gateway.TxRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

@Component
public class SpringTxRunner implements TxRunner {

    private final TransactionalOperator tx;
    private final TransactionalOperator readOnlyTx;

    public SpringTxRunner(@Qualifier("txOperator") TransactionalOperator tx,
                          @Qualifier("readOnlyTx") TransactionalOperator readOnlyTx) {
        this.tx = tx;
        this.readOnlyTx = readOnlyTx;
    }

    @Override
    public <T> Mono<T> required(Supplier<Mono<T>> action) {
        return Mono.defer(action).as(tx::transactional);
    }
    @Override
    public <T> Flux<T> requiredMany(Supplier<Flux<T>> action) {
        return Flux.defer(action).as(tx::transactional);
    }

    @Override
    public <T> Mono<T> readOnly(Supplier<Mono<T>> action) {
        return Mono.defer(action).as(readOnlyTx::transactional);
    }
    @Override
    public <T> Flux<T> readOnlyMany(Supplier<Flux<T>> action) {
        return Flux.defer(action).as(readOnlyTx::transactional);
    }
}
