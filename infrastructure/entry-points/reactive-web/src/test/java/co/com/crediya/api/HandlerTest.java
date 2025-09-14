package co.com.crediya.api;

import co.com.crediya.api.dto.loan.ChangeStatusLoanRequest;
import co.com.crediya.api.dto.loan.CreateLoanRequest;
import co.com.crediya.api.dto.loan.LoanResponse;
import co.com.crediya.api.dto.pageable.PageResponse;
import co.com.crediya.api.mapper.loan.LoanDTOMapper;
import co.com.crediya.api.validation.DtoValidator;
import co.com.crediya.model.loan.ChangeLoanStatus;
import co.com.crediya.model.loan.LoanStatusChanged;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.model.pageable.LoanStatus;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.model.pageable.Pageable;
import co.com.crediya.usecase.loan.LoanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    // Mocks
    private final LoanUseCase loanUseCase = mock(LoanUseCase.class);
    private final Notification notification = mock(Notification.class);
    private final LoanDTOMapper mapper    = mock(LoanDTOMapper.class);
    private final DtoValidator validator  = mock(DtoValidator.class);

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var handler = new Handler(loanUseCase, mapper, validator, notification);

        // 1) Router base
        RouterFunction<ServerResponse> base =
                route(POST("/loans"), handler::createLoan)
                        .andRoute(GET("/loans"), handler::getAllLoans)
                        .andRoute(GET("/loans/list"), handler::list)
                        .andRoute(GET("/loans/{id}"), handler::getLoanById)
                        .andRoute(PATCH("/loans/status"), handler::changeLoanStatus);

        // 2) Inyectamos un JwtAuthenticationToken como principal en el EXCHANGE
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("email", "user@example.com")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        RouterFunction<ServerResponse> router =
                base.filter((request, next) -> {
                    // Mutamos el exchange para fijar el principal
                    var newExchange = request.exchange()
                            .mutate()
                            .principal(Mono.just(auth))
                            .build();

                    // Creamos un nuevo ServerRequest con ese exchange
                    var newReq = ServerRequest.create(
                            newExchange,
                            HandlerStrategies.withDefaults().messageReaders()
                    );

                    return next.handle(newReq);
                });

        client = WebTestClient.bindToRouterFunction(router)
                .configureClient()
                .responseTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Test
    void createLoan_usesEmailFromJwt_andReturns201WithBody() {
        // Arrange
        var typeId = UUID.randomUUID().toString();
        var bodyDto = new CreateLoanRequest(
                new BigDecimal("3000.00"), 12, null, typeId, null
        );

        var domainIn  = mock(co.com.crediya.model.loan.Loan.class);
        var savedLoan = mock(co.com.crediya.model.loan.Loan.class);
        var response  = new LoanResponse(
                UUID.randomUUID().toString(), new BigDecimal("3000.00"),
                12, "user@example.com", "PENDING_REVIEW", typeId
        );

        // STUBS (antes del Act)
        // validator(validate(tuple)) → devuelve el mismo tuple (body + subject)
        when(validator.validate(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(mapper.toDomain(any(CreateLoanRequest.class))).thenReturn(domainIn);
        when(loanUseCase.create(domainIn)).thenReturn(Mono.just(savedLoan));
        when(mapper.toResponse(savedLoan)).thenReturn(response);

        // Act + Assert
        client.post().uri("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyDto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(LoanResponse.class)
                .isEqualTo(response);

        // Verificar que el email del JWT fue inyectado en el DTO pasado al mapper
        var dtoCaptor = ArgumentCaptor.forClass(CreateLoanRequest.class);
        verify(mapper).toDomain(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().email()).isEqualTo("user@example.com");

        verify(validator).validate(any());
        verify(loanUseCase).create(domainIn);
        verify(mapper).toResponse(savedLoan);
        verifyNoMoreInteractions(loanUseCase, mapper, validator);
    }

    @Test
    void list_parsesQuery_buildsFilter_callsUseCase_andReturnsPageResponse() {
        // Arrange: página “de dominio” (mock) con getters usados por el handler
        @SuppressWarnings("unchecked")
        var pageDomain = (Pageable<LoanSummary>) mock(Pageable.class);

        var r1 = new LoanSummary(
                UUID.randomUUID().toString(),
                new BigDecimal("5000000"),
                24,
                "juan.perez@example.com",
                "Juan Pérez",
                "Préstamo de Libre Inversión",
                new BigDecimal("0.015"),
                "APROBADO",
                new BigDecimal("2500000"),
                new BigDecimal("450000")
        );
        var r2 = new LoanSummary(
                UUID.randomUUID().toString(),
                new BigDecimal("12000000"),
                36,
                "ana.gomez@example.com",
                "Ana Gómez",
                "Crédito de Vehículo",
                new BigDecimal("0.012"),
                "PENDIENTE",
                new BigDecimal("4000000"),
                new BigDecimal("800000")
        );

        when(pageDomain.content()).thenReturn(List.of(r1, r2));
        when(pageDomain.totalElements()).thenReturn(2L);
        when(pageDomain.page()).thenReturn(2);
        when(pageDomain.size()).thenReturn(5);
        when(pageDomain.totalPages()).thenReturn(10);

        when(loanUseCase.execute(any(ManualReviewFilter.class), eq(2), eq(5)))
                .thenReturn(Mono.just(pageDomain));

        // Act + Assert con tipo genérico correcto
        var type = new ParameterizedTypeReference<PageResponse<LoanSummary>>() {};
        client.get().uri(uriBuilder ->
                        uriBuilder.path("/loans/list")
                                .queryParam("search", "john")
                                .queryParam("status", "PENDING_REVIEW,REJECTED")
                                .queryParam("typeLoanId", "TYPE-123")
                                .queryParam("minAmount", "1000.00")
                                .queryParam("maxAmount", "5000.00")
                                .queryParam("page", "2")
                                .queryParam("size", "5")
                                .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(type)
                .value(pr -> {
                    assertThat(pr.content()).containsExactly(r1, r2); // requiere equals en LoanSummary
                    assertThat(pr.totalElements()).isEqualTo(2L);
                    assertThat(pr.page()).isEqualTo(2);
                    assertThat(pr.size()).isEqualTo(5);
                    assertThat(pr.totalPages()).isEqualTo(10);
                });

        // Capturamos y validamos el filtro construido
        var captor = ArgumentCaptor.forClass(ManualReviewFilter.class);
        verify(loanUseCase).execute(captor.capture(), eq(2), eq(5));
        var filter = captor.getValue();
        assertThat(filter.search()).isEqualTo("john");
        assertThat(filter.typeLoanId()).isEqualTo("TYPE-123");
        assertThat(filter.minAmount()).isEqualByComparingTo("1000.00");
        assertThat(filter.maxAmount()).isEqualByComparingTo("5000.00");
        assertThat(filter.statuses()).containsExactlyInAnyOrder(
                LoanStatus.PENDING_REVIEW, LoanStatus.REJECTED
        );

        verifyNoMoreInteractions(loanUseCase, mapper, validator);
    }

    @Test
    void getLoanById_returns204NoContent_forNow() {
        client.get().uri("/loans/{id}", "whatever")
                .exchange()
                .expectStatus().isNoContent();

        verifyNoInteractions(loanUseCase, mapper, validator);
    }

    @Test
    void changeLoanStatus_happyPath_sendsNotification_andReturns200WithBody() {
        // Arrange
        var dto = new ChangeStatusLoanRequest(
                UUID.randomUUID().toString(),           // loanId
                UUID.randomUUID().toString(),           // newStateId
                "reason-x"
        );

        var cmd = mock(ChangeLoanStatus.class);
        var changed = mock(LoanStatusChanged.class);
        var savedLoan = mock(co.com.crediya.model.loan.Loan.class);
        var response = new LoanResponse(
                UUID.randomUUID().toString(),
                new BigDecimal("4500.00"),
                18,
                "someone@example.com",
                "APPROVED",
                UUID.randomUUID().toString()
        );

        when(validator.validate(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(mapper.toDomain(any(ChangeStatusLoanRequest.class))).thenReturn(cmd);
        when(loanUseCase.changeLoanStatus(cmd)).thenReturn(Mono.just(changed));
        when(changed.loan()).thenReturn(savedLoan);
        when(notification.sendMessage(changed)).thenReturn(Mono.empty());
        when(mapper.toResponse(savedLoan)).thenReturn(response);

        // Act + Assert
        client.patch().uri("/loans/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(LoanResponse.class)
                .isEqualTo(response);

        // Verificaciones clave
        verify(validator).validate(any());
        verify(mapper).toDomain(any(ChangeStatusLoanRequest.class));
        verify(loanUseCase).changeLoanStatus(cmd);
        verify(notification).sendMessage(changed);
        verify(mapper).toResponse(savedLoan);
        verifyNoMoreInteractions(loanUseCase, mapper, validator, notification);
    }

    @Test
    void changeLoanStatus_whenNotificationFails_propagates5xx_andDoesNotMapResponse() {
        var dto = new ChangeStatusLoanRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "reason-y"
        );

        var cmd = mock(ChangeLoanStatus.class);
        var changed = mock(LoanStatusChanged.class);
        var savedLoan = mock(co.com.crediya.model.loan.Loan.class);

        when(validator.validate(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(mapper.toDomain(any(ChangeStatusLoanRequest.class))).thenReturn(cmd);
        when(loanUseCase.changeLoanStatus(cmd)).thenReturn(Mono.just(changed));
        when(changed.loan()).thenReturn(savedLoan);
        when(notification.sendMessage(changed)).thenReturn(Mono.error(new RuntimeException("sqs down")));

        client.patch().uri("/loans/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is5xxServerError();

        // Se validó y se llamó a use case, pero NO se mapeó respuesta porque falló notificación
        verify(validator).validate(any());
        verify(mapper).toDomain(any(ChangeStatusLoanRequest.class));
        verify(loanUseCase).changeLoanStatus(cmd);
        verify(notification).sendMessage(changed);
        verify(mapper, never()).toResponse(any());
        verifyNoMoreInteractions(loanUseCase, mapper, validator, notification);
    }

    @Test
    void getAllLoans_happyPath_streamsNdjson_andMapsEachItem() {
        var d1 = mock(co.com.crediya.model.loan.Loan.class);
        var d2 = mock(co.com.crediya.model.loan.Loan.class);

        var r1 = new LoanResponse(
                UUID.randomUUID().toString(), new BigDecimal("1000.00"),
                6, "a@b.com", "PENDING_REVIEW", UUID.randomUUID().toString()
        );
        var r2 = new LoanResponse(
                UUID.randomUUID().toString(), new BigDecimal("2500.00"),
                12, "c@d.com", "REJECTED", UUID.randomUUID().toString()
        );

        when(loanUseCase.getAllLoans()).thenReturn(Flux.just(d1, d2));
        when(mapper.toResponse(d1)).thenReturn(r1);
        when(mapper.toResponse(d2)).thenReturn(r2);

        client.get().uri("/loans")
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
                .expectBodyList(LoanResponse.class)
                .hasSize(2)
                .contains(r1, r2);

        verify(loanUseCase).getAllLoans();
        verify(mapper).toResponse(d1);
        verify(mapper).toResponse(d2);
        verifyNoMoreInteractions(loanUseCase, mapper, validator, notification);
    }
}
