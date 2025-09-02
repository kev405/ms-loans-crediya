package co.com.crediya.model.stateloan.gateways;

import co.com.crediya.model.stateloan.StateLoan;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface StateLoanRepository {
    Mono<StateLoan> findById(UUID id);
    Mono<StateLoan> findByName(String name); // para “PENDING_REVIEW”
}
