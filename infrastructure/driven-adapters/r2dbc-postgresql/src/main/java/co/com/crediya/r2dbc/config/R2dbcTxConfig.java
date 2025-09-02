package co.com.crediya.r2dbc.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Configuration
@EnableTransactionManagement
public class R2dbcTxConfig {

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory cf) {
        return new R2dbcTransactionManager(cf);
    }

    @Bean
    public TransactionalOperator txOperator(ReactiveTransactionManager tm) {
        var def = new DefaultTransactionDefinition();
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return TransactionalOperator.create(tm, def);
    }

    @Bean(name = "readOnlyTx")
    public TransactionalOperator readOnlyTx(ReactiveTransactionManager tm) {
        var def = new DefaultTransactionDefinition();
        def.setReadOnly(true);
        return TransactionalOperator.create(tm, def);
    }
}
