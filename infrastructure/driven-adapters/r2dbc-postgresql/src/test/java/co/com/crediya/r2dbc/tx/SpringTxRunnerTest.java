package co.com.crediya.r2dbc.tx;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;

@ExtendWith(MockitoExtension.class)
class SpringTxRunnerTest {

    private TransactionalOperator tx = mock(TransactionalOperator.class);

    private TransactionalOperator readOnlyTx =
            mock(TransactionalOperator.class);

    private SpringTxRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SpringTxRunner(tx, readOnlyTx);
        when(tx.transactional(any(Mono.class))).thenAnswer(
                inv -> inv.getArgument(0));
        when(tx.transactional(any(Flux.class))).thenAnswer(
                inv -> inv.getArgument(0));

        when(readOnlyTx.transactional(any(Mono.class))).thenAnswer(
                inv -> inv.getArgument(0));
        when(readOnlyTx.transactional(any(Flux.class))).thenAnswer(
                inv -> inv.getArgument(0));
    }

    @Test
    void required_usesTx_and_isLazy() {
        var calls = new AtomicInteger(0);
        var action = (java.util.function.Supplier<Mono<String>>) () ->
                Mono.fromCallable(() -> { calls.incrementAndGet(); return "ok"; });

        Mono<String> mono = runner.required(action);

        verify(tx, times(1)).transactional(any(Mono.class));
        verifyNoInteractions(readOnlyTx);

        org.junit.jupiter.api.Assertions.assertEquals(0, calls.get());

        StepVerifier.create(mono)
                .expectNext("ok")
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());

        verifyNoMoreInteractions(tx, readOnlyTx);
    }

    @Test
    void readOnly_usesReadOnlyTx_and_notTx() {
        var calls = new AtomicInteger(0);
        var action = (java.util.function.Supplier<Mono<Integer>>) () ->
                Mono.fromCallable(() -> { calls.incrementAndGet(); return 42; });

        Mono<Integer> mono = runner.readOnly(action);

        verify(readOnlyTx, times(1)).transactional(any(Mono.class));
        verifyNoInteractions(tx);
        org.junit.jupiter.api.Assertions.assertEquals(0, calls.get());

        StepVerifier.create(mono)
                .expectNext(42)
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());
        verifyNoMoreInteractions(tx, readOnlyTx);
    }

    @Test
    void requiredMany_usesTx_and_isLazy() {
        var calls = new AtomicInteger(0);
        var action = (java.util.function.Supplier<Flux<String>>) () ->
                Flux.range(1, 3)
                        .map(i -> { calls.incrementAndGet(); return "n" + i; });

        Flux<String> flux = runner.requiredMany(action);

        verify(tx, times(1)).transactional(any(Flux.class));
        verifyNoInteractions(readOnlyTx);

        org.junit.jupiter.api.Assertions.assertEquals(0, calls.get());

        StepVerifier.create(flux)
                .expectNext("n1", "n2", "n3")
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(3, calls.get());

        verifyNoMoreInteractions(tx, readOnlyTx);
    }

    @Test
    void readOnlyMany_usesReadOnlyTx_and_notTx() {
        var calls = new AtomicInteger(0);
        var action = (java.util.function.Supplier<Flux<Integer>>) () ->
                Flux.range(1, 3)
                        .map(i -> { calls.incrementAndGet(); return i * 10; });

        Flux<Integer> flux = runner.readOnlyMany(action);

        verify(readOnlyTx, times(1)).transactional(any(Flux.class));
        verifyNoInteractions(tx);
        org.junit.jupiter.api.Assertions.assertEquals(0, calls.get());

        StepVerifier.create(flux)
                .expectNext(10, 20, 30)
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(3, calls.get());
        verifyNoMoreInteractions(tx, readOnlyTx);
    }
}
