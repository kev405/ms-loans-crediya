package co.com.crediya.webclient.gateway;

import co.com.crediya.webclient.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerGatewayAdapterTest {

    private final AuthProperties props = mock(AuthProperties.class);

    private CustomerGatewayAdapter newAdapter(ExchangeFunction ef) {
        WebClient wc = WebClient.builder()
                .baseUrl("http://host")
                .exchangeFunction(ef)
                .build();
        return new CustomerGatewayAdapter(wc, props);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }

    @Test
    void existsByEmail_when200True_emitsTrue_andBuildsUri() {

        when(props.getExistsPath()).thenReturn("/customers/exists?email={email}");
        var seen = new AtomicReference<ClientRequest>();
        var adapter = newAdapter(req -> {
            seen.set(req);
            return Mono.just(json(HttpStatus.OK, "true"));
        });

        StepVerifier.create(adapter.existsByEmail("a@b.com"))
                .expectNext(true)
                .verifyComplete();

        assertEquals(HttpMethod.GET, seen.get().method());
        assertEquals("http://host/customers/exists?email=a%40b.com", seen.get().url().toString());
        verify(props).getExistsPath();
    }

    @Test
    void existsByEmail_when200False_emitsFalse() {
        when(props.getExistsPath()).thenReturn("/customers/exists?email={email}");
        var adapter = newAdapter(req -> Mono.just(json(HttpStatus.OK, "false")));

        StepVerifier.create(adapter.existsByEmail("c@d.com"))
                .expectNext(false)
                .verifyComplete();

        verify(props).getExistsPath();
    }

    @Test
    void existsByEmail_when500_emitsFalse_dueToOnErrorResume() {
        when(props.getExistsPath()).thenReturn("/customers/exists?email={email}");
        var adapter = newAdapter(req -> Mono.just(json(HttpStatus.INTERNAL_SERVER_ERROR, "")));

        StepVerifier.create(adapter.existsByEmail("x@y.com"))
                .expectNext(false)
                .verifyComplete();

        verify(props).getExistsPath();
    }

    @Test
    void existsByEmail_whenNetworkError_emitsFalse_dueToOnErrorResume() {
        when(props.getExistsPath()).thenReturn("/customers/exists?email={email}");
        var adapter = newAdapter(req -> Mono.error(new RuntimeException("boom")));

        StepVerifier.create(adapter.existsByEmail("x@y.com"))
                .expectNext(false)
                .verifyComplete();

        verify(props).getExistsPath();
    }
}
