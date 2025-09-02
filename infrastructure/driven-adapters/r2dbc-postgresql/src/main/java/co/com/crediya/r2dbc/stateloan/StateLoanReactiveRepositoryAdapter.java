package co.com.crediya.r2dbc.stateloan;

import co.com.crediya.model.stateloan.StateLoan;
import co.com.crediya.model.stateloan.gateways.StateLoanRepository;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import co.com.crediya.r2dbc.stateloan.entity.StateLoanEntity;
import co.com.crediya.r2dbc.stateloan.mapper.StateLoanEntityMapper;
import reactor.core.publisher.Mono;
import java.util.UUID;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StateLoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    StateLoan,
    StateLoanEntity,
    UUID,
    StateLoanReactiveRepository
> implements StateLoanRepository {

    private final StateLoanEntityMapper entityMapper;

    public StateLoanReactiveRepositoryAdapter(StateLoanReactiveRepository repository, ObjectMapper mapper, StateLoanEntityMapper entityMapper) {
        super(repository, mapper, entityMapper::toDomain);
        this.entityMapper = entityMapper;
    }

    @Override public Mono<StateLoan> findById(UUID id) { return repository.findById(id).map(entityMapper::toDomain); }
    @Override public Mono<StateLoan> findByName(String name) { return repository.findByName(name).map(entityMapper::toDomain); }


}
