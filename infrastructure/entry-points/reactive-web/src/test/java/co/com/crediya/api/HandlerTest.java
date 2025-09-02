package co.com.crediya.api;

import co.com.crediya.api.dto.loan.CreateLoanRequest;
import co.com.crediya.api.dto.loan.LoanResponse;
import co.com.crediya.api.mapper.loan.LoanDTOMapper;
import co.com.crediya.api.validation.DtoValidator;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.usecase.loan.LoanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    private final LoanUseCase loanUseCase = mock(LoanUseCase.class);
    private final LoanDTOMapper mapper    = mock(LoanDTOMapper.class);
    private final DtoValidator validator  = mock(DtoValidator.class);

    private WebTestClient client;

    private static final String LOANS      = "/api/loans";
    private static final String LOANS_BY_ID = "/api/loans/{id}";

    @BeforeEach
    void setUp() {
        var handler = new Handler(loanUseCase, mapper, validator);

        RouterFunction<ServerResponse> router =
                route(POST(LOANS), handler::createLoan)
                        .andRoute(GET(LOANS), handler::getAllLoans)
                        .andRoute(GET(LOANS_BY_ID), handler::getLoanById);

        client = WebTestClient
                .bindToRouterFunction(router)
                .configureClient()
                .responseTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Test
    void createLoan_happyPath_returns201AndBody() {
        var typeLoanId = UUID.randomUUID().toString();
        var stateLoanId = UUID.randomUUID().toString();

        var dto = new CreateLoanRequest(
                BigDecimal.valueOf(3000),
                12,
                "ok@example.com",
                typeLoanId,
                stateLoanId
        );

        var domainIn   = mock(Loan.class);
        var savedLoan  = mock(Loan.class);
        var respDto    = new LoanResponse(
                UUID.randomUUID().toString(), // id
                BigDecimal.valueOf(3000),
                12,
                "ok@example.com",
                "PENDING_REVIEW",
                typeLoanId
        );

        when(validator.validate(any(CreateLoanRequest.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));  // “pasa” la validación
        when(mapper.toDomain(dto)).thenReturn(domainIn);
        when(loanUseCase.create(domainIn)).thenReturn(Mono.just(savedLoan));
        when(mapper.toResponse(savedLoan)).thenReturn(respDto);

        client.post().uri(LOANS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(LoanResponse.class)
                .isEqualTo(respDto);

        verify(validator).validate(any(CreateLoanRequest.class));
        verify(mapper).toDomain(dto);
        verify(loanUseCase).create(domainIn);
        verify(mapper).toResponse(savedLoan);
        verifyNoMoreInteractions(loanUseCase, mapper, validator);
    }

    @Test
    void getAllLoans_happyPath_returns200AndNdjson() {

        var l1 = mock(Loan.class);
        var l2 = mock(Loan.class);

        var r1 = new LoanResponse("id1", BigDecimal.valueOf(1000), 6, "a@b.com", "PENDING_REVIEW", "type1");
        var r2 = new LoanResponse("id2", BigDecimal.valueOf(2000), 12, "c@d.com", "PENDING_REVIEW", "type2");

        when(loanUseCase.getAllLoans()).thenReturn(Flux.just(l1, l2));
        when(mapper.toResponse(l1)).thenReturn(r1);
        when(mapper.toResponse(l2)).thenReturn(r2);

        client.get().uri(LOANS)
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
                .expectBodyList(LoanResponse.class)
                .hasSize(2)
                .contains(r1, r2);

        verify(loanUseCase).getAllLoans();
        verify(mapper).toResponse(l1);
        verify(mapper).toResponse(l2);
        verifyNoMoreInteractions(loanUseCase, mapper, validator);
    }

    @Test
    void getLoanById_returns204_forNow() {
        client.get().uri(LOANS_BY_ID, "any-id")
                .exchange()
                .expectStatus().isNoContent();

        verifyNoInteractions(loanUseCase, mapper, validator);
    }
}
