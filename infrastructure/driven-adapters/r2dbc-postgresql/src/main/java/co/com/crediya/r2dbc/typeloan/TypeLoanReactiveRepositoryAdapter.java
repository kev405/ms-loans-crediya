package co.com.crediya.r2dbc.typeloan;

import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.model.typeloan.gateways.TypeLoanRepository;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import co.com.crediya.r2dbc.typeloan.entity.TypeLoanEntity;
import co.com.crediya.r2dbc.typeloan.mapper.TypeLoanEntityMapper;
import reactor.core.publisher.Mono;
import java.util.UUID;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TypeLoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    TypeLoan,
    TypeLoanEntity,
    UUID,
    TypeLoanReactiveRepository
> implements TypeLoanRepository {

    private final TypeLoanEntityMapper entityMapper;

    public TypeLoanReactiveRepositoryAdapter(TypeLoanReactiveRepository repository, ObjectMapper mapper, TypeLoanEntityMapper entityMapper) {
        super(repository, mapper, entityMapper::toDomain);
        this.entityMapper = entityMapper;
    }

    @Override
    public Mono<TypeLoan> findById(UUID id) {
        return repository.findById(id).map(entityMapper::toDomain);
    }

}
