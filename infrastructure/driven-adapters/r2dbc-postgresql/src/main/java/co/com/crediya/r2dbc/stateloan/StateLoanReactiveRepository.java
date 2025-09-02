package co.com.crediya.r2dbc.stateloan;

import co.com.crediya.r2dbc.stateloan.entity.StateLoanEntity;
import reactor.core.publisher.Mono;
import java.util.UUID;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface StateLoanReactiveRepository extends ReactiveCrudRepository<StateLoanEntity, UUID>, ReactiveQueryByExampleExecutor<StateLoanEntity> {

    Mono<StateLoanEntity> findByName(String name);

}
