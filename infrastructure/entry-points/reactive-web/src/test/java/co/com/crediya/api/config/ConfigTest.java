package co.com.crediya.api.config;

import co.com.crediya.api.Handler;
import co.com.crediya.api.RouterRest;
import co.com.crediya.api.mapper.loan.LoanDTOMapper;
import co.com.crediya.api.validation.DtoValidator;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.usecase.loan.LoanUseCase;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@ContextConfiguration(classes = {RouterRest.class, Handler.class})
@WebFluxTest
@EnableConfigurationProperties(LoanPath.class)
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private LoanUseCase loanUseCase;

    @MockitoBean
    private LoanDTOMapper loanDTOMapper;

    @MockitoBean
    private DtoValidator dtoValidator;

    @MockitoBean
    private Notification notification;

    @BeforeEach
    void setUp() {
        Mockito.when(loanUseCase.getAllLoans()).thenReturn(Flux.empty());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void corsConfigurationShouldAllowOrigins() {
        webTestClient.get()
                .uri("/api/v1/loans")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}