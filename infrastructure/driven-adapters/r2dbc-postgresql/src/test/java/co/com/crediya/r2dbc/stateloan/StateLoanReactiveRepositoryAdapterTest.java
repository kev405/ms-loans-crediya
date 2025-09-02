package co.com.crediya.r2dbc.stateloan;

import co.com.crediya.model.stateloan.StateLoan;
import co.com.crediya.r2dbc.stateloan.entity.StateLoanEntity;
import co.com.crediya.r2dbc.stateloan.mapper.StateLoanEntityMapper;
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
class StateLoanReactiveRepositoryAdapterTest {

    @Mock
    StateLoanReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    StateLoanEntityMapper entityMapper;

    @InjectMocks
    StateLoanReactiveRepositoryAdapter adapter;

    @Test
    void findById_delegatesToRepo_andMapsToDomain() {
        UUID id = UUID.randomUUID();
        StateLoanEntity entity = mock(StateLoanEntity.class);
        StateLoan domain = mock(StateLoan.class);

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
    void findById_whenEmpty_completesWithoutValues_andDoesNotMap() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findById(id))
                .verifyComplete();

        verify(repository).findById(id);
        verifyNoInteractions(entityMapper);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void findByName_delegatesToRepo_andMapsToDomain() {
        var name = "PENDING_REVIEW";
        StateLoanEntity entity = mock(StateLoanEntity.class);
        StateLoan domain = mock(StateLoan.class);

        when(repository.findByName(name)).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(adapter.findByName(name))
                .expectNext(domain)
                .verifyComplete();

        verify(repository).findByName(name);
        verify(entityMapper).toDomain(entity);
        verifyNoMoreInteractions(repository, entityMapper, mapper);
    }

    @Test
    void findByName_error_isPropagated_withoutMapping() {
        var name = "UNKNOWN";
        when(repository.findByName(name))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(adapter.findByName(name))
                .expectErrorMessage("boom")
                .verify();

        verify(repository).findByName(name);
        verifyNoInteractions(entityMapper);
        verifyNoMoreInteractions(repository, mapper);
    }
}
