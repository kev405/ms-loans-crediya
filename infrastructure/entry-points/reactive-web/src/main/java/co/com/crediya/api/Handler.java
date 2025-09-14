package co.com.crediya.api;

import co.com.crediya.api.dto.loan.ChangeStatusLoanRequest;
import co.com.crediya.api.dto.loan.CreateLoanRequest;
import co.com.crediya.api.dto.loan.LoanResponse;
import co.com.crediya.api.dto.pageable.PageResponse;
import co.com.crediya.api.mapper.loan.LoanDTOMapper;
import co.com.crediya.api.validation.DtoValidator;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.model.pageable.LoanStatus;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.usecase.loan.LoanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final LoanUseCase loanUseCase;
    private final LoanDTOMapper mapper;
    private final DtoValidator validator;
    private final Notification notification;

    public Mono<ServerResponse> createLoan(ServerRequest req) {
        Mono<CreateLoanRequest> body = req.bodyToMono(CreateLoanRequest.class);

        Mono<String> subject = req.principal()
                .cast(JwtAuthenticationToken.class)
                .map(auth -> auth.getToken().getClaims().get("email").toString());

        return Mono.zip(body, subject)
                .doOnSubscribe(s -> log.info("POST create loan"))
                .flatMap(validator::validate)
                .map(tuple -> {
                    var createReq = tuple.getT1();
                    var email = tuple.getT2();
                    log.info("Creating loan for email: {}", email);
                    CreateLoanRequest reqWithEmail = new CreateLoanRequest(
                            createReq.amount(),
                            createReq.termMonths(),
                            email,
                            createReq.typeLoanId(),
                            null
                    );
                    return mapper.toDomain(reqWithEmail);
                })
                .flatMap(loanUseCase::create)
                .map(mapper::toResponse)
                .flatMap(resp -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> getAllLoans(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(loanUseCase.getAllLoans().map(mapper::toResponse), LoanResponse.class);
    }

    public Mono<ServerResponse> changeLoanStatus(ServerRequest req) {
        return req.bodyToMono(ChangeStatusLoanRequest.class)
                .doOnSubscribe(s -> log.info("PATCH change loan status"))
                .flatMap(validator::validate)
                .map(mapper::toDomain)
                .flatMap(loanUseCase::changeLoanStatus)
                .flatMap(changed ->
                        notification.sendMessage(changed).thenReturn(changed.loan())            // seguimos con el Loan para la respuesta
                )
                .map(mapper::toResponse)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> getLoanById(ServerRequest req) {
        // Implementa cuando lo necesites
        return ServerResponse.noContent().build();
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        String search = req.queryParam("search").orElse(null);
        String statusCsv = req.queryParam("status").orElse(null);
        String typeId = req.queryParam("typeLoanId").orElse(null);
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);

        var minAmount = req.queryParam("minAmount").map(BigDecimal::new).orElse(null);
        var maxAmount = req.queryParam("maxAmount").map(BigDecimal::new).orElse(null);

        Set<LoanStatus> statuses = statusCsv == null || statusCsv.isBlank()
                ? Set.of(LoanStatus.PENDING_REVIEW, LoanStatus.REJECTED, LoanStatus.MANUAL_REVIEW)
                : Arrays.stream(statusCsv.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(LoanStatus::valueOf).collect(Collectors.toSet());

        var filter = new ManualReviewFilter(search, statuses, typeId, minAmount, maxAmount);

        return loanUseCase.execute(filter, page, size)
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new PageResponse<>(
                                p.content(), p.totalElements(), p.page(), p.size(), p.totalPages()
                        )));
    }
}
