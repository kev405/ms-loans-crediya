package co.com.crediya.r2dbc.loan;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import co.com.crediya.r2dbc.loan.mapper.LoanEntityMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Loan/* change for domain model */,
        LoanEntity/* change for adapter model */,
        UUID,
        LoanReactiveRepository
> implements LoanRepository {

    private final LoanEntityMapper entityMapper;

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper, LoanEntityMapper entityMapper) {
        super(repository, mapper, entityMapper::toDomain);
        this.entityMapper = entityMapper;
    }

    @Override
    public Mono<Loan> save(Loan loan) {


        LoanEntity entity = entityMapper.toEntity(loan);
        return repository.save(entity).map(entityMapper::toDomain);
    }

    @Override public Mono<Loan> findById(UUID id) { return repository.findById(id).map(entityMapper::toDomain); }
    @Override public Flux<Loan> findAll() { return repository.findAll().map(entityMapper::toDomain); }

}
