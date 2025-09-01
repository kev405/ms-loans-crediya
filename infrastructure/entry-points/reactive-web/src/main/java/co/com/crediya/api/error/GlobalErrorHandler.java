package co.com.crediya.api.error;

import co.com.crediya.model.exceptions.DomainValidationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final HandlerStrategies STRATEGIES =
            HandlerStrategies.withDefaults();

    private static final ServerResponse.Context CTX = new ServerResponse.Context() {
        @Override public List<HttpMessageWriter<?>> messageWriters() { return STRATEGIES.messageWriters(); }
        @Override public List<ViewResolver>        viewResolvers()  { return STRATEGIES.viewResolvers(); }
    };

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var req    = ServerRequest.create(exchange, STRATEGIES.messageReaders());
        var status = mapStatus(ex);
        var body   = toProblemDetail(req, ex, status);

        log.error(ex.getMessage(), ex);

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(BodyInserters.fromValue(body))
                .flatMap(resp -> resp.writeTo(exchange, CTX))
                .onErrorResume(writeErr -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    private HttpStatusCode mapStatus(Throwable ex) {
        if (ex instanceof WebExchangeBindException)
            return HttpStatus.BAD_REQUEST;
        if (ex instanceof ServerWebInputException)
            return HttpStatus.BAD_REQUEST;
        if (ex instanceof DataIntegrityViolationException)
            return HttpStatus.CONFLICT;
        if (ex instanceof ResponseStatusException rse)
            return rse.getStatusCode();
        if (ex instanceof ErrorResponseException ere)
            return ere.getStatusCode();
        if (ex instanceof DomainValidationException)
            return HttpStatus.UNPROCESSABLE_ENTITY; // 422 negocio
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ProblemDetail toProblemDetail(ServerRequest req, Throwable ex,
                                          HttpStatusCode status) {
        var pd = ProblemDetail.forStatus(status);
        pd.setTitle(titleFor(status));
        pd.setDetail(safeDetail(ex));
        pd.setProperty("path", req.path());
        pd.setProperty("timestamp", Instant.now().toString());

        if (ex instanceof WebExchangeBindException be) {
            var fieldErrors = be.getFieldErrors().stream()
                    .map(fe -> Map.of(
                            "field", fe.getField(),
                            "message", fe.getDefaultMessage(),
                            "rejectedValue",
                            String.valueOf(fe.getRejectedValue())))
                    .collect(Collectors.toList());
            pd.setProperty("errors", fieldErrors);
        }

        return pd;
    }

    private String titleFor(HttpStatusCode s) {
        return (s instanceof HttpStatus hs) ? hs.getReasonPhrase() :
                s.toString();
    }

    private String safeDetail(Throwable ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() :
                ex.getMessage();
    }

}
