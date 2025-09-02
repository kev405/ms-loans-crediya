package co.com.crediya.r2dbc.loantest;

import co.com.crediya.model.loan.Loan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import co.com.crediya.r2dbc.loan.LoanReactiveRepository;
import co.com.crediya.r2dbc.loan.LoanReactiveRepositoryAdapter;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import co.com.crediya.r2dbc.loan.mapper.LoanEntityMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanReactiveRepositoryAdapterTest {

    @Mock
    LoanReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    LoanEntityMapper entityMapper;

    @InjectMocks
    LoanReactiveRepositoryAdapter adapter;

    @Test
    void save_mapsDomainToEntity_callsRepo_andMapsBackToDomain() {
        Loan domainIn   = mock(Loan.class);
        LoanEntity ent  = mock(LoanEntity.class);
        Loan domainOut  = mock(Loan.class);

        when(entityMapper.toEntity(domainIn)).thenReturn(ent);
        when(repository.save(ent)).thenReturn(Mono.just(ent));
        when(entityMapper.toDomain(ent)).thenReturn(domainOut);

        StepVerifier.create(adapter.save(domainIn))
                .expectNext(domainOut)
                .verifyComplete();

        verify(entityMapper).toEntity(domainIn);
        verify(repository).save(ent);
        verify(entityMapper).toDomain(ent);
        verifyNoMoreInteractions(repository, entityMapper, mapper);
    }

    @Test
    void findById_delegatesToRepo_andMapsToDomain() {

        UUID id = UUID.randomUUID();
        LoanEntity ent = mock(LoanEntity.class);
        Loan domain = mock(Loan.class);

        when(repository.findById(id)).thenReturn(Mono.just(ent));
        when(entityMapper.toDomain(ent)).thenReturn(domain);

        StepVerifier.create(adapter.findById(id))
                .expectNext(domain)
                .verifyComplete();

        verify(repository).findById(id);
        verify(entityMapper).toDomain(ent);
        verifyNoMoreInteractions(repository, entityMapper, mapper);
    }

    @Test
    void findById_whenEmpty_completesWithoutValues() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findById(id))
                .verifyComplete();

        verify(repository).findById(id);
        verifyNoInteractions(entityMapper);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void findAll_delegatesToRepo_andMapsEachElement() {
        LoanEntity e1 = mock(LoanEntity.class);
        LoanEntity e2 = mock(LoanEntity.class);
        Loan d1 = mock(Loan.class);
        Loan d2 = mock(Loan.class);

        when(repository.findAll()).thenReturn(Flux.just(e1, e2));
        when(entityMapper.toDomain(e1)).thenReturn(d1);
        when(entityMapper.toDomain(e2)).thenReturn(d2);

        StepVerifier.create(adapter.findAll())
                .expectNext(d1, d2)
                .verifyComplete();

        verify(repository).findAll();
        verify(entityMapper).toDomain(e1);
        verify(entityMapper).toDomain(e2);
        verifyNoMoreInteractions(repository, entityMapper, mapper);
    }
}
