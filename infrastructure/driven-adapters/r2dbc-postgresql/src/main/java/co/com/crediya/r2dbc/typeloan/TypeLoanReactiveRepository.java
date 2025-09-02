package co.com.crediya.r2dbc.typeloan;

import co.com.crediya.r2dbc.typeloan.entity.TypeLoanEntity;
import java.util.UUID;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface TypeLoanReactiveRepository extends ReactiveCrudRepository<TypeLoanEntity, UUID>, ReactiveQueryByExampleExecutor<TypeLoanEntity> {

}
