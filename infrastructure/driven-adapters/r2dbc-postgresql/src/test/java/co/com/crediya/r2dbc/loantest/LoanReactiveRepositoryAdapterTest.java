package co.com.crediya.r2dbc.loantest;

import co.com.crediya.model.customer.UserData;
import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.pageable.LoanStatus;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.r2dbc.loan.LoanReactiveRepository;
import co.com.crediya.r2dbc.loan.LoanReactiveRepositoryAdapter;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import co.com.crediya.r2dbc.loan.mapper.LoanEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ajustado para cubrir el enriquecimiento con CustomerGateway.
 */
@ExtendWith(MockitoExtension.class)
class LoanReactiveRepositoryAdapterTest {

    @Mock LoanReactiveRepository repository;
    @Mock ObjectMapper mapper;
    @Mock LoanEntityMapper entityMapper;
    @Mock DatabaseClient client;
    @Mock CustomerGateway customerGateway;

    private LoanReactiveRepositoryAdapter adapter;

    @BeforeEach
    void init() {
        adapter = new LoanReactiveRepositoryAdapter(
                repository, mapper, entityMapper, client, customerGateway
        );
    }

    // ---------- básicos: save / findById / findAll ----------

    @Test
    void save_mapsDomainToEntity_callsRepo_andMapsBackToDomain() {
        Loan domainIn = mock(Loan.class);
        LoanEntity ent = mock(LoanEntity.class);
        Loan domainOut = mock(Loan.class);

        when(entityMapper.toEntity(domainIn)).thenReturn(ent);
        when(repository.save(ent)).thenReturn(Mono.just(ent));
        when(entityMapper.toDomain(ent)).thenReturn(domainOut);

        StepVerifier.create(adapter.save(domainIn))
                .expectNext(domainOut)
                .verifyComplete();

        verify(entityMapper).toEntity(domainIn);
        verify(repository).save(ent);
        verify(entityMapper).toDomain(ent);
        verifyNoMoreInteractions(repository, entityMapper, mapper, customerGateway);
    }

    @Test
    void findById_delegatesToRepo_andMapsToDomain() {
        var id = UUID.randomUUID();
        var ent = mock(LoanEntity.class);
        var domain = mock(Loan.class);

        when(repository.findById(id)).thenReturn(Mono.just(ent));
        when(entityMapper.toDomain(ent)).thenReturn(domain);

        StepVerifier.create(adapter.findById(id))
                .expectNext(domain)
                .verifyComplete();

        verify(repository).findById(id);
        verify(entityMapper).toDomain(ent);
        verifyNoMoreInteractions(repository, entityMapper, mapper, customerGateway);
    }

    @Test
    void findAll_delegatesToRepo_andMapsEachElement() {
        var e1 = mock(LoanEntity.class);
        var e2 = mock(LoanEntity.class);
        var d1 = mock(Loan.class);
        var d2 = mock(Loan.class);

        when(repository.findAll()).thenReturn(Flux.just(e1, e2));
        when(entityMapper.toDomain(e1)).thenReturn(d1);
        when(entityMapper.toDomain(e2)).thenReturn(d2);

        StepVerifier.create(adapter.findAll())
                .expectNext(d1, d2)
                .verifyComplete();

        verify(repository).findAll();
        verify(entityMapper).toDomain(e1);
        verify(entityMapper).toDomain(e2);
        verifyNoMoreInteractions(repository, entityMapper, mapper, customerGateway);
    }

    // ---------- findForManualReview: enriquecido con CustomerGateway ----------

    @Test
    void findForManualReview_enrichesWithCustomerData_andBuildsArgs() {
        var statusSet = Set.of(LoanStatus.PENDING_REVIEW, LoanStatus.REJECTED);
        var typeId = UUID.randomUUID().toString();
        var filter = new ManualReviewFilter(
                "john", statusSet, typeId, new BigDecimal("1000.00"), new BigDecimal("5000.00")
        );
        int page = 2, size = 5;
        long expectedOffset = (long) page * size;

        // Filas base (lo que devuelve el repo)
        var base1 = new LoanSummary(
                UUID.randomUUID().toString(),
                new BigDecimal("5000"), 12,
                "a@b.com", "IGNORAR", "Libre",
                new BigDecimal("0.015"), "PENDIENTE",
                new BigDecimal("1111"), new BigDecimal("400")
        );
        var base2 = new LoanSummary(
                UUID.randomUUID().toString(),
                new BigDecimal("7000"), 24,
                "c@d.com", "IGNORAR", "Vehículo",
                new BigDecimal("0.012"), "RECHAZADO",
                new BigDecimal("2222"), new BigDecimal("600")
        );

        when(repository.findForManualReview(any(String[].class), any(), any(), any(), any(), anyLong(), anyInt()))
                .thenReturn(Flux.just(base1, base2));
        when(repository.countForManualReview(any(String[].class), any(), any(), any(), any()))
                .thenReturn(Mono.just(20L));

        // Stubs de cliente
        // ⚠ Cambia el tipo Customer por el de tu proyecto si tiene otro nombre/paquete
        UserData cust1 = mock(UserData.class);
        when(cust1.name()).thenReturn("Alice");
        when(cust1.lastName()).thenReturn("Smith");
        when(cust1.salary()).thenReturn(new BigDecimal("2000"));

        UserData cust2 = mock(UserData.class);
        when(cust2.name()).thenReturn("Bob");
        when(cust2.lastName()).thenReturn("Johnson");
        when(cust2.salary()).thenReturn(new BigDecimal("3000"));

        when(customerGateway.findByEmail("a@b.com")).thenReturn(Mono.just(cust1));
        when(customerGateway.findByEmail("c@d.com")).thenReturn(Mono.just(cust2));

        // Act
        var mono = adapter.findForManualReview(filter, page, size);

        // Assert contenido enriquecido y meta
        StepVerifier.create(mono)
                .assertNext(p -> {
                    assertThat(p.totalElements()).isEqualTo(20L);
                    assertThat(p.page()).isEqualTo(page);
                    assertThat(p.size()).isEqualTo(size);
                    assertThat(p.content()).hasSize(2);

                    var enriched1 = p.content().get(0);
                    assertThat(enriched1.id()).isEqualTo(base1.id());
                    assertThat(enriched1.amount()).isEqualByComparingTo(base1.amount());
                    assertThat(enriched1.termMonths()).isEqualTo(base1.termMonths());
                    assertThat(enriched1.applicantEmail()).isEqualTo("a@b.com");
                    assertThat(enriched1.applicantName()).isEqualTo("Alice Smith");
                    assertThat(enriched1.typeLoanName()).isEqualTo(base1.typeLoanName());
                    assertThat(enriched1.interestRateMonthly()).isEqualByComparingTo(base1.interestRateMonthly());
                    assertThat(enriched1.status()).isEqualTo(base1.status());
                    assertThat(enriched1.baseSalary()).isEqualByComparingTo("2000");
                    assertThat(enriched1.monthlyApprovedDebt()).isEqualByComparingTo(base1.monthlyApprovedDebt());

                    var enriched2 = p.content().get(1);
                    assertThat(enriched2.applicantEmail()).isEqualTo("c@d.com");
                    assertThat(enriched2.applicantName()).isEqualTo("Bob Johnson");
                    assertThat(enriched2.baseSalary()).isEqualByComparingTo("3000");
                })
                .verifyComplete();

        // Verificación de parámetros al repo
        var stCap = ArgumentCaptor.forClass(String[].class);
        var searchCap = ArgumentCaptor.forClass(String.class);
        var typeCap = ArgumentCaptor.forClass(UUID.class);
        var minCap = ArgumentCaptor.forClass(BigDecimal.class);
        var maxCap = ArgumentCaptor.forClass(BigDecimal.class);
        var offsetCap = ArgumentCaptor.forClass(Long.class);
        var sizeCap = ArgumentCaptor.forClass(Integer.class);

        verify(repository).findForManualReview(
                stCap.capture(), searchCap.capture(), typeCap.capture(),
                minCap.capture(), maxCap.capture(), offsetCap.capture(), sizeCap.capture()
        );
        assertThat(List.of(stCap.getValue())).containsExactlyInAnyOrder("PENDING_REVIEW", "REJECTED");
        assertThat(searchCap.getValue()).isEqualTo("john");
        assertThat(typeCap.getValue()).isEqualTo(UUID.fromString(typeId));
        assertThat(minCap.getValue()).isEqualByComparingTo("1000.00");
        assertThat(maxCap.getValue()).isEqualByComparingTo("5000.00");
        assertThat(offsetCap.getValue()).isEqualTo(expectedOffset);
        assertThat(sizeCap.getValue()).isEqualTo(size);

        verify(customerGateway).findByEmail("a@b.com");
        verify(customerGateway).findByEmail("c@d.com");
        verifyNoMoreInteractions(customerGateway);
    }

    @Test
    void findForManualReview_whenCustomerNotFound_usesFallbackNulls() {
        var filter = new ManualReviewFilter("x", Set.of(), "   ", null, null);
        int page = 0, size = 2;

        var base = new LoanSummary(
                UUID.randomUUID().toString(),
                new BigDecimal("3000"), 6,
                "missing@e.com", "IGNORAR", "Libre",
                new BigDecimal("0.02"), "MANUAL_REVIEW",
                new BigDecimal("9999"), new BigDecimal("250")
        );

        when(repository.findForManualReview(any(String[].class), any(), any(), any(), any(), anyLong(), anyInt()))
                .thenReturn(Flux.just(base));
        when(repository.countForManualReview(any(String[].class), any(), any(), any(), any()))
                .thenReturn(Mono.just(1L));

        // Cliente NO encontrado ⇒ Mono.empty()
        when(customerGateway.findByEmail("missing@e.com")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findForManualReview(filter, page, size))
                .assertNext(p -> {
                    assertThat(p.totalElements()).isEqualTo(1L);
                    assertThat(p.page()).isEqualTo(0);
                    assertThat(p.size()).isEqualTo(2);
                    assertThat(p.content()).hasSize(1);

                    var enriched = p.content().get(0);
                    assertThat(enriched.applicantEmail()).isEqualTo("missing@e.com");
                    assertThat(enriched.applicantName()).isNull();   // fallback
                    assertThat(enriched.baseSalary()).isNull();      // fallback
                    // el resto se conserva
                    assertThat(enriched.amount()).isEqualByComparingTo(base.amount());
                    assertThat(enriched.monthlyApprovedDebt()).isEqualByComparingTo(base.monthlyApprovedDebt());
                })
                .verifyComplete();

        verify(customerGateway).findByEmail("missing@e.com");
    }
}
