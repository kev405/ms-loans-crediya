package co.com.crediya.model.typeloan.gateways;

import co.com.crediya.model.typeloan.TypeLoan;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface TypeLoanRepository {
    Mono<TypeLoan> findById(UUID id);
}
