package co.com.crediya.api.error;

import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler();

    @Test
    void dataIntegrityViolation_returns409() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con la excepción “de base de datos”
        var ex = new DataIntegrityViolationException("duplicate key");
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.CONFLICT, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void unknownError_returns500() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con una excepción desconocida
        var ex = new RuntimeException("unexpected error");
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void domainValidationException_returns422() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con una excepción de validación
        var ex = new DomainValidationException("INVALID_EMAIL", "format");
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void webInputException_returns400() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con una excepción de entrada mal formada
        var ex = new org.springframework.web.server.ServerWebInputException("bad input");
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void responseStatusException_returnsMappedStatus() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con una ResponseStatusException personalizada
        var ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "teapot");
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.I_AM_A_TEAPOT, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void errorResponseException_returnsMappedStatus() {
        // 1) Arrange: un exchange de prueba
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        // 2) Act: invocar el handler con una ErrorResponseException personalizada
        var ex = new org.springframework.web.ErrorResponseException(HttpStatus.FORBIDDEN);
        handler.handle(exchange, ex).block(); // bloqueamos en test para esperar el write

        // 3) Assert: status y content-type correctos
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    static class DummyController {
        public void handle(DummyDto dto) {}
    }

    static class DummyDto {
        String email;
    }

    private WebExchangeBindException newBindEx(String field, Object rejected, String message) throws Exception {
        var method = DummyController.class.getMethod("handle", DummyDto.class);
        var param  = new MethodParameter(method, 0);
        var target = new DummyDto();
        var br     = new BeanPropertyBindingResult(target, "dummyDto");
        br.addError(new FieldError("dummyDto", field, rejected, false, null, null, message));
        return new WebExchangeBindException(param, br);
    }

    @Test
    void webExchangeBindException_returns400() throws Exception {
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        var ex = newBindEx("email", "bad@", "must be a well-formed email");
        handler.handle(exchange, ex).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void runtimeException_messageNull() {
        var request  = MockServerHttpRequest.get("/loans").build();
        var exchange = MockServerWebExchange.from(request);

        var ex = new RuntimeException();
        handler.handle(exchange, ex).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertEquals("application/problem+json",
                Objects.requireNonNull(
                        exchange.getResponse().getHeaders().getContentType()).toString());
    }

    @Test
    void nonHttpStatus_usesToStringInTitle() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/weird").build()
        );

        var nonStandard = HttpStatusCode.valueOf(499);           // no-HttpStatus
        var ex = new ResponseStatusException(nonStandard, "weird");

        handler.handle(exchange, ex).block();

        // clave: ¡bloquear!
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("\"status\":499"));
        assertTrue(body.contains("\"title\":\"" + nonStandard.toString() + "\""));
        assertEquals("application/problem+json",
                exchange.getResponse().getHeaders().getContentType().toString());
    }

    @Test
    void whenWriteFails_handlerSets500AndCompletes() {
        // 1) Mockeamos exchange y response
        var exchange  = mock(ServerWebExchange.class, RETURNS_DEEP_STUBS);
        var response  = mock(ServerHttpResponse.class, RETURNS_DEEP_STUBS);

        // Request real para que ServerRequest.create(...) tenga path y headers válidos
        var request = MockServerHttpRequest.get("/loans").build();

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);

        // Infra mínima que usa writeTo(...)
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());

        // Simulamos que escribir el body falla
        when(response.writeWith(any())).thenReturn(Mono.error(new RuntimeException("boom")));
        // Y que completar la respuesta funciona
        when(response.setComplete()).thenReturn(Mono.empty());

        // 2) Disparamos un error que mapea a 409 (no 500) para ver el cambio a 500
        var ex = new DataIntegrityViolationException("duplicate key");

        // 3) Act
        handler.handle(exchange, ex).block();

        // 4) Assert: primero intenta 409, falla al escribir, y luego cae al 500 y completa
        InOrder inOrder = inOrder(response);
        inOrder.verify(response).setStatusCode(HttpStatus.CONFLICT);             // antes de escribir
        verify(response).writeWith(any());                                        // intento de escritura
        inOrder.verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // onErrorResume
        verify(response).setComplete();                                           // onErrorResume
    }
}
