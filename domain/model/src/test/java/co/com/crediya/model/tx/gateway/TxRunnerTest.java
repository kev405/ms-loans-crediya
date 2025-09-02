package co.com.crediya.model.tx.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class TxRunnerTest {

    static class TestTxRunner implements TxRunner {
        final AtomicInteger requiredCalls = new AtomicInteger();
        final AtomicInteger requiredManyCalls = new AtomicInteger();

        @Override public <T> Mono<T> required(Supplier<Mono<T>> action) {
            requiredCalls.incrementAndGet();
            return Mono.defer(action);  // respeta la pereza
        }
        @Override public <T> Flux<T> requiredMany(Supplier<Flux<T>> action) {
            requiredManyCalls.incrementAndGet();
            return Flux.defer(action);
        }
    }

    @Test
    void readOnly_delegatesTo_required() {
        var runner = new TestTxRunner();

        StepVerifier.create(runner.readOnly(() -> Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();

        assertEquals(1, runner.requiredCalls.get());
        assertEquals(0, runner.requiredManyCalls.get());
    }

    @Test
    void readOnlyMany_delegatesTo_requiredMany() {
        var runner = new TestTxRunner();

        StepVerifier.create(runner.readOnlyMany(() -> Flux.just(1, 2, 3)))
                .expectNext(1, 2, 3)
                .verifyComplete();

        assertEquals(0, runner.requiredCalls.get());
        assertEquals(1, runner.requiredManyCalls.get());
    }
}
