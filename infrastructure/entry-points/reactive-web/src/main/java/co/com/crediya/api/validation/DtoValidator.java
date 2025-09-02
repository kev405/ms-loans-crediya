package co.com.crediya.api.validation;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoValidator {
    private final jakarta.validation.Validator validator;

    public <T> Mono<T> validate(T target) {
        return Mono.fromCallable(() -> {
            var v = validator.validate(target);
            if (!v.isEmpty()) throw new jakarta.validation.ConstraintViolationException(v);
            return target;
        });
    }
}
