package co.com.crediya.webclient.gateway;

import co.com.crediya.webclient.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerGatewayAdapterFindByEmailTest {

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

    private static JwtAuthenticationToken jwtAuth(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .headers(h -> h.put("alg", "none"))
                .claims(c -> c.putAll(Map.of("sub", "user", "email", "user@example.com")))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void findByEmail_withJwt_setsBearerHeader_encodesEmail_andReturnsUser() {
        when(props.getEmailInfoPath()).thenReturn("/customers/email/{email}");

        var seen = new AtomicReference<ClientRequest>();
        // JSON del usuario; ajusta campos si tu UserData es distinto
        String body = """
            {"name":"Ana","lastName":"Gómez","salary":4000000}
        """;
        var adapter = newAdapter(req -> {
            seen.set(req);
            return Mono.just(json(HttpStatus.OK, body));
        });

        var auth = jwtAuth("tkn-123");

        StepVerifier.create(
                        adapter.findByEmail("a@b.com")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                )
                .assertNext(user -> {
                    assertThat(user.name()).isEqualTo("Ana");
                    assertThat(user.lastName()).isEqualTo("Gómez");
                    // Si salary es BigDecimal en tu modelo:
                    assertThat(user.salary()).isEqualByComparingTo(new BigDecimal("4000000"));
                })
                .verifyComplete();

        // Verifica método, URL (email codificado) y header Authorization
        var req = seen.get();
        assertEquals(HttpMethod.GET, req.method());
        assertEquals("http://host/customers/email/a%40b.com", req.url().toString());
        assertThat(req.headers().getFirst("Authorization")).isEqualTo("Bearer tkn-123");

        verify(props).getEmailInfoPath();
    }

    @Test
    void findByEmail_withNonJwtAuth_emitsIllegalStateException() {
        when(props.getEmailInfoPath()).thenReturn("/customers/email/{email}");

        var adapter = newAdapter(req -> Mono.just(json(HttpStatus.OK, "{}")));

        var nonJwt = new TestingAuthenticationToken("u", "p"); // no es Jwt
        StepVerifier.create(
                        adapter.findByEmail("a@b.com")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(nonJwt))
                )
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("JWT Token not found"))
                .verify();

        // No hay request si falla por auth
        verifyNoInteractions(props);
    }

    @Test
    void findByEmail_404_propagatesWebClientResponseException() {
        when(props.getEmailInfoPath()).thenReturn("/customers/email/{email}");
        var adapter = newAdapter(req -> Mono.just(json(HttpStatus.NOT_FOUND, "")));

        var auth = jwtAuth("tkn-123");

        StepVerifier.create(
                        adapter.findByEmail("missing@x.com")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                )
                .expectError(WebClientResponseException.NotFound.class)
                .verify();

        verify(props).getEmailInfoPath();
    }

    @Test
    void findByEmail_networkError_propagatesError() {
        when(props.getEmailInfoPath()).thenReturn("/customers/email/{email}");
        var adapter = newAdapter(req -> Mono.error(new RuntimeException("boom")));

        var auth = jwtAuth("tkn-123");

        StepVerifier.create(
                        adapter.findByEmail("a@b.com")
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                )
                .expectErrorMatches(e -> e.getMessage().contains("boom"))
                .verify();

        verify(props).getEmailInfoPath();
    }

    @Test
    void findByEmail_withoutSecurityContext_completesEmpty() {
        when(props.getEmailInfoPath()).thenReturn("/customers/email/{email}");
        var adapter = newAdapter(req -> Mono.error(new AssertionError("no debería invocar WebClient")));

        // No ponemos contexto -> ReactiveSecurityContextHolder.getContext() está vacío
        StepVerifier.create(adapter.findByEmail("a@b.com"))
                .verifyComplete();

        // No hubo request
        verifyNoInteractions(props);
    }
}
