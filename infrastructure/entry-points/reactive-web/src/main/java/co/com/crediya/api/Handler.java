package co.com.crediya.api;

import co.com.crediya.api.dto.loan.CreateLoanRequest;
import co.com.crediya.api.dto.loan.LoanResponse;
import co.com.crediya.api.mapper.loan.LoanDTOMapper;
import co.com.crediya.api.validation.DtoValidator;
import co.com.crediya.usecase.loan.LoanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final LoanUseCase loanUseCase;
    private final LoanDTOMapper mapper;
    private final DtoValidator validator;

    public Mono<ServerResponse> createLoan(ServerRequest req) {
        return req.bodyToMono(CreateLoanRequest.class)
                .doOnSubscribe(s -> log.info("POST create loan"))
                .flatMap(validator::validate)
                .map(mapper::toDomain)
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

    public Mono<ServerResponse> getLoanById(ServerRequest req) {
        // Implementa cuando lo necesites
        return ServerResponse.noContent().build();
    }
}
