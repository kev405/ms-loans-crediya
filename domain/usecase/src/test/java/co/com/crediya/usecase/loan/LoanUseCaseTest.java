package co.com.crediya.usecase.loan;

import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.stateloan.StateLoan; // si existe este POJO/record
import co.com.crediya.model.stateloan.gateways.StateLoanRepository;
import co.com.crediya.model.tx.gateway.TxRunner;
import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.model.typeloan.gateways.TypeLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import co.com.crediya.model.value.Email;
import co.com.crediya.model.value.InterestRate;
import co.com.crediya.model.value.Money;
import co.com.crediya.model.value.TermMonths;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanUseCaseTest {

    LoanRepository loanRepo = mock(LoanRepository.class);
    TypeLoanRepository typeRepo = mock(TypeLoanRepository.class);
    StateLoanRepository stateRepo = mock(StateLoanRepository.class);
    CustomerGateway customerGateway = mock(CustomerGateway.class);
    TxRunner txRunner = mock(TxRunner.class);

    LoanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoanUseCase(loanRepo, typeRepo, stateRepo, customerGateway, txRunner);

        // TxRunner passthrough
        when(txRunner.required(any())).thenAnswer(inv -> ((Supplier<Mono<?>>) inv.getArgument(0)).get());
        when(txRunner.readOnlyMany(any())).thenAnswer(inv -> ((Supplier<Flux<?>>) inv.getArgument(0)).get());

        // <<< stubs por defecto para evitar NPE en .then(... <<<
        when(typeRepo.findById(any())).thenReturn(Mono.never());            // publisher no nulo
        when(stateRepo.findByName(anyString())).thenReturn(Mono.never());   // publisher no nulo
        when(loanRepo.save(any())).thenReturn(Mono.never());                // idem
    }

    static Loan loan(String email, String typeId, BigDecimal amount, int term, String stateId) {
        return new Loan(
                null,
                new Money(amount),
                new TermMonths(term),
                new Email(email),
                stateId,
                typeId
        );
    }

    static TypeLoan typeLoan(BigDecimal min, BigDecimal max) {
        return new TypeLoan(
                "type-1",
                "PERSONAL",
                new Money(min),
                new Money(max),
                new InterestRate(new BigDecimal("0.02")),
                true
        );
    }

    static StateLoan state(String id, String name) {
        return new StateLoan(id, name, "");
    }

    @Test
    void create_whenCustomerNotFound_emitsDomainValidation() {
        var email  = "nobody@example.com";
        var typeId = UUID.randomUUID().toString();
        var loan   = loan(email, typeId, new BigDecimal("1000"), 12, null);

        when(customerGateway.existsByEmail(email)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.create(loan))
                .expectError(DomainValidationException.class)
                .verify();

        verify(customerGateway).existsByEmail(email);
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_whenAmountOutOfRange_emitsDomainValidation() {
        var email  = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var loan   = loan(email, typeId, new BigDecimal("10000000"), 12, null); // ojo sin '_'

        var type = typeLoan(new BigDecimal("1000"), new BigDecimal("5000"));

        when(customerGateway.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));

        StepVerifier.create(useCase.create(loan))
                .expectError(DomainValidationException.class)
                .verify();

        verify(typeRepo).findById(UUID.fromString(typeId));
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_whenTypeLoanMissing_emitsDomainNotFound() {
        var email  = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var loan   = loan(email, typeId, new BigDecimal("3000"), 12, null);

        when(customerGateway.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.empty());
        // stateRepo ya está en Mono.never()

        StepVerifier.create(useCase.create(loan))
                .expectError(DomainNotFoundException.class)
                .verify();

        verify(typeRepo).findById(UUID.fromString(typeId));
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_whenPendingStateMissing_emitsDomainNotFound() {
        var email = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var input = loan(email, typeId, new BigDecimal("3000"), 12, null);
        var type = typeLoan(new BigDecimal("1000"), new BigDecimal("5000"));

        when(customerGateway.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));
        when(stateRepo.findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.create(input))
                .expectError(DomainNotFoundException.class)
                .verify();

        verify(customerGateway).existsByEmail(email);
        verify(typeRepo).findById(UUID.fromString(typeId));
        verify(stateRepo).findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME);
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_happyPath_savesAndReturnsLoan() {
        var email = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var input = loan(email, typeId, new BigDecimal("3000"), 12, null);
        var type = typeLoan(new BigDecimal("1000"), new BigDecimal("5000"));
        var pending = state(UUID.randomUUID().toString(), LoanUseCase.DEFAULT_PENDING_STATE_NAME);

        // Loan que esperamos que se construya dentro del use case (id null, estado=PENDING)
        // y Loan que retorna el repo con id asignado:
        var toSave = loan(email, typeId, new BigDecimal("3000"), 12, pending.id());
        var saved  = loan(email, typeId, new BigDecimal("3000"), 12, pending.id());

        when(customerGateway.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));
        when(stateRepo.findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME)).thenReturn(Mono.just(pending));
        when(loanRepo.save(any(Loan.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.create(input))
                .expectNext(saved)
                .verifyComplete();

        verify(customerGateway).existsByEmail(email);
        verify(typeRepo).findById(UUID.fromString(typeId));
        verify(stateRepo).findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME);
        verify(loanRepo).save(any(Loan.class));
        verifyNoMoreInteractions(loanRepo, typeRepo, stateRepo, customerGateway);
    }

    @Test
    void getAllLoans_forwardsToReadOnlyMany() {
        var l1 = mock(Loan.class);
        var l2 = mock(Loan.class);

        when(loanRepo.findAll()).thenReturn(Flux.just(l1, l2));

        StepVerifier.create(useCase.getAllLoans())
                .expectNext(l1, l2)
                .verifyComplete();

        verify(txRunner).readOnlyMany(any());
        verify(loanRepo).findAll();
        verifyNoMoreInteractions(txRunner, loanRepo);
    }
}
