package co.com.crediya.r2dbc.typeloan;

import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.r2dbc.typeloan.entity.TypeLoanEntity;
import co.com.crediya.r2dbc.typeloan.mapper.TypeLoanEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TypeLoanReactiveRepositoryAdapterTest {

    @Mock
    TypeLoanReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    TypeLoanEntityMapper entityMapper;

    @InjectMocks
    TypeLoanReactiveRepositoryAdapter adapter;

    @Test
    void findById_delegatesAndMapsToDomain() {
        UUID id = UUID.randomUUID();
        var entity = mock(TypeLoanEntity.class);
        var domain = mock(TypeLoan.class);

        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(adapter.findById(id))
                .expectNext(domain)
                .verifyComplete();

        verify(repository).findById(id);
        verify(entityMapper).toDomain(entity);
        verifyNoMoreInteractions(repository, entityMapper, mapper);
    }

    @Test
    void findById_whenEmpty_completesWithoutValue_andDoesNotMap() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findById(id))
                .verifyComplete();

        verify(repository).findById(id);
        verifyNoInteractions(entityMapper);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void findById_whenRepoErrors_propagatesError_withoutMapping() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(adapter.findById(id))
                .expectErrorMessage("boom")
                .verify();

        verify(repository).findById(id);
        verifyNoInteractions(entityMapper);
        verifyNoMoreInteractions(repository, mapper);
    }
}
