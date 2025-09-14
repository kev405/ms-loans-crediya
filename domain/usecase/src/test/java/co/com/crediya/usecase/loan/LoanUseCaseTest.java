package co.com.crediya.usecase.loan;

import co.com.crediya.model.customer.UserData;                   // <-- ajusta si tu dominio lo nombra distinto
import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import co.com.crediya.model.loan.ChangeLoanStatus;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanStatusChanged;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.model.pageable.Pageable;
import co.com.crediya.model.stateloan.StateLoan;
import co.com.crediya.model.stateloan.gateways.StateLoanRepository;
import co.com.crediya.model.tx.gateway.TxRunner;
import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.model.typeloan.gateways.TypeLoanRepository;
import co.com.crediya.model.value.Email;
import co.com.crediya.model.value.InterestRate;
import co.com.crediya.model.value.Money;
import co.com.crediya.model.value.TermMonths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanUseCaseTest {

    LoanRepository loanRepo      = mock(LoanRepository.class);
    Notification  notification   = mock(Notification.class);
    TypeLoanRepository typeRepo  = mock(TypeLoanRepository.class);
    StateLoanRepository stateRepo= mock(StateLoanRepository.class);
    CustomerGateway customerGw   = mock(CustomerGateway.class);
    TxRunner txRunner            = mock(TxRunner.class);

    LoanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoanUseCase(loanRepo, notification, typeRepo, stateRepo, customerGw, txRunner);

        // TxRunner passthrough
        when(txRunner.required(any())).thenAnswer(inv -> ((Supplier<Mono<?>>)inv.getArgument(0)).get());
        when(txRunner.readOnly(any())).thenAnswer(inv -> ((Supplier<Mono<?>>)inv.getArgument(0)).get());
        when(txRunner.readOnlyMany(any())).thenAnswer(inv -> ((Supplier<Flux<?>>)inv.getArgument(0)).get());

        // publishers no-nulos por defecto (evita NPE en .then(...) si olvidas stub en un test)
        when(typeRepo.findById(any())).thenReturn(Mono.never());
        when(stateRepo.findByName(anyString())).thenReturn(Mono.never());
        when(stateRepo.findById(any())).thenReturn(Mono.never());
        when(loanRepo.save(any())).thenReturn(Mono.never());
    }

    // ---------------- helpers de dominio ----------------

    static Loan loan(String email, String typeId, BigDecimal amount, int term, String stateId) {
        return new Loan(
                UUID.randomUUID().toString(),
                new Money(amount),
                new TermMonths(term),
                new Email(email),
                stateId,
                typeId
        );
    }

    static TypeLoan typeLoan(BigDecimal min, BigDecimal max) {
        return new TypeLoan(
                "TYPE-1",
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

    static ChangeLoanStatus change(String loanId, String newStateId, String reason) {
        return new ChangeLoanStatus(loanId, newStateId, reason);
    }

    // ---------------- tests de create() (tus casos + happy path) ----------------

    @Test
    void create_whenAmountOutOfRange_emitsDomainValidation() {
        var email  = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var input  = loan(email, typeId, new BigDecimal("10000000"), 12, null); // monto fuera de rango

        var type = typeLoan(new BigDecimal("1000"), new BigDecimal("5000"));

        when(customerGw.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));

        StepVerifier.create(useCase.create(input))
                .expectError(DomainValidationException.class)
                .verify();

        verify(typeRepo).findById(UUID.fromString(typeId));
        verify(customerGw).existsByEmail(email);
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_whenTypeLoanMissing_emitsDomainNotFound() {
        var email  = "ok@example.com";
        var typeId = UUID.randomUUID().toString();
        var input  = loan(email, typeId, new BigDecimal("3000"), 12, null);

        when(customerGw.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.create(input))
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

        when(customerGw.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));
        when(stateRepo.findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.create(input))
                .expectError(DomainNotFoundException.class)
                .verify();

        verify(typeRepo).findById(UUID.fromString(typeId));
        verify(stateRepo).findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME);
        verifyNoInteractions(loanRepo);
    }

    @Test
    void create_happyPath_savesAndReturnsLoan() {
        var email   = "ok@example.com";
        var typeId  = UUID.randomUUID().toString();
        var input   = loan(email, typeId, new BigDecimal("3000"), 12, null);
        var type    = typeLoan(new BigDecimal("1000"), new BigDecimal("5000"));
        var pending = state(UUID.randomUUID().toString(), LoanUseCase.DEFAULT_PENDING_STATE_NAME);

        var saved = loan(email, typeId, new BigDecimal("3000"), 12, pending.id());

        when(customerGw.existsByEmail(email)).thenReturn(Mono.just(true));
        when(typeRepo.findById(UUID.fromString(typeId))).thenReturn(Mono.just(type));
        when(stateRepo.findByName(LoanUseCase.DEFAULT_PENDING_STATE_NAME)).thenReturn(Mono.just(pending));
        when(loanRepo.save(any(Loan.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.create(input))
                .expectNext(saved)
                .verifyComplete();

        // capturar que el loan guardado traía el pending state
        var captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepo).save(captor.capture());
        assertThat(captor.getValue().stateLoanId()).isEqualTo(pending.id());
    }

    // ---------------- tests de changeLoanStatus() ----------------

    @Test
    void changeLoanStatus_whenLoanMissing_emitsDomainNotFound() {
        var cmd = change("00000000-0000-0000-0000-000000000000", "STATE-NEW", "ok");

        when(loanRepo.findById(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.changeLoanStatus(cmd))
                .expectError(DomainNotFoundException.class)
                .verify();

        verify(loanRepo).findById(any());
        verifyNoMoreInteractions(loanRepo, stateRepo, typeRepo, customerGw);
    }

    @Test
    void changeLoanStatus_whenNewStateMissing_emitsDomainNotFound() {
        var existing = loan("u@e.com", UUID.randomUUID().toString(), new BigDecimal("2000"), 10, "STATE-OLD");
        var cmd = change(existing.id(), UUID.randomUUID().toString(), "reason-x");

        when(loanRepo.findById(UUID.fromString(cmd.loanId()))).thenReturn(Mono.just(existing));
        when(stateRepo.findById(UUID.fromString(cmd.newStateId()))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.changeLoanStatus(cmd))
                .expectError(DomainNotFoundException.class)
                .verify();

        verify(loanRepo).findById(UUID.fromString(cmd.loanId()));
        verify(stateRepo).findById(UUID.fromString(cmd.newStateId()));
        verifyNoMoreInteractions(loanRepo, stateRepo, typeRepo, customerGw);
    }

    @Test
    void changeLoanStatus_happyPath_buildsUpdatedLoan_andEmitsLoanStatusChanged() {
        // loan actual
        var typeId = UUID.randomUUID().toString();
        var oldState = "STATE-OLD";
        var existing = loan("u@e.com", typeId, new BigDecimal("2500"), 18, oldState);

        // target state
        var newState = state(UUID.randomUUID().toString(), "APPROVED");
        var cmd = change(existing.id(), newState.id(), "approved-after-review");

        // al guardar devolvemos el mismo loan con el nuevo estado:
        var updated = new Loan(existing.id(), existing.amount(), existing.termMonths(),
                existing.email(), newState.id(), existing.typeLoanId());

        // para armar LoanStatusChanged:
        var type = new TypeLoan(typeId, "Libre Inversión",
                new Money(new BigDecimal("1000")),
                new Money(new BigDecimal("10000")),
                new InterestRate(new BigDecimal("0.015")),
                true);

        UserData customer = mock(UserData.class);

        // stubs
        when(loanRepo.findById(UUID.fromString(cmd.loanId()))).thenReturn(Mono.just(existing));
        when(stateRepo.findById(UUID.fromString(cmd.newStateId()))).thenReturn(Mono.just(newState));
        when(loanRepo.save(any(Loan.class))).thenReturn(Mono.just(updated));
        when(stateRepo.findById(UUID.fromString(updated.stateLoanId()))).thenReturn(Mono.just(newState));
        when(typeRepo.findById(UUID.fromString(updated.typeLoanId()))).thenReturn(Mono.just(type));
        when(customerGw.findByEmail(updated.email().value())).thenReturn(Mono.just(customer));

        StepVerifier.create(useCase.changeLoanStatus(cmd))
                .assertNext(changed -> {
                    // estructura general
                    assertThat(changed).isInstanceOf(LoanStatusChanged.class);
                    assertThat(changed.loan()).isEqualTo(updated);
                    assertThat(changed.stateName()).isEqualTo("APPROVED");
                    assertThat(changed.typeName()).isEqualTo("Libre Inversión");
                    assertThat(changed.reason()).isEqualTo("approved-after-review");
                    assertThat(changed.userData()).isEqualTo(customer);
                })
                .verifyComplete();

        // se guardó un loan con el nuevo estado
        var captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepo).save(captor.capture());
        assertThat(captor.getValue().stateLoanId()).isEqualTo(newState.id());

        verifyNoInteractions(notification); // el método aún no envía notificaciones
    }

    // ---------------- tests de execute() y getAllLoans() ----------------

    @Test
    void execute_delegatesToReadOnly_andReturnsPageable() {
        var filter = new ManualReviewFilter("john", null, null, null, null);
        var s1 = new LoanSummary(
                UUID.randomUUID().toString(), new BigDecimal("5000"), 12,
                "a@b.com", "Alice", "Libre", new BigDecimal("0.015"),
                "PENDIENTE", new BigDecimal("2000"), new BigDecimal("400")
        );
        var page = new Pageable<>(List.of(s1), 1L, 0, 10);

        when(loanRepo.findForManualReview(filter, 0, 10)).thenReturn(Mono.just(page));

        StepVerifier.create(useCase.execute(filter, 0, 10))
                .expectNext(page)
                .verifyComplete();

        verify(txRunner).readOnly(any());
        verify(loanRepo).findForManualReview(filter, 0, 10);
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
    }
}
