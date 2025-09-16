package co.com.crediya.config;

import co.com.crediya.model.loan.gateways.DebtCapacitySQS;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.model.typeloan.gateways.TypeLoanRepository;
import co.com.crediya.model.stateloan.gateways.StateLoanRepository;
import co.com.crediya.model.customer.gateways.CustomerGateway; // si tu LoanUseCase lo usa
import co.com.crediya.model.tx.gateway.TxRunner;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class UseCasesConfigTest {

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            String[] beanNames = context.getBeanDefinitionNames();

            boolean useCaseBeanFound = false;
            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseBeanFound = true;
                    break;
                }
            }

            assertTrue(useCaseBeanFound, "No beans ending with 'Use Case' were found");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        @Bean
        LoanRepository loanRepository() { return mock(LoanRepository.class); }

        @Bean
        Notification notification() { return mock(Notification.class); }

        @Bean
        DebtCapacitySQS debtCapacitySQS() { return mock(DebtCapacitySQS.class); }

        @Bean
        TypeLoanRepository typeLoanRepository() { return mock(TypeLoanRepository.class); }

        @Bean
        StateLoanRepository stateLoanRepository() { return mock(StateLoanRepository.class); }

        @Bean
        CustomerGateway customerGateway() { return mock(CustomerGateway.class); }

        @Bean
        TxRunner txRunner() {
            return new TxRunner() {
                @Override
                public <T> Mono<T> required(Supplier<Mono<T>> work) {
                    return work.get();
                }
                @Override
                public <T> Flux<T> requiredMany(Supplier<Flux<T>> action) {
                    return action.get();
                }
            };
        }
    }
}