package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.DebtCapacity;
import reactor.core.publisher.Mono;

public interface DebtCapacitySQS {

    Mono<Void> sendMessage(DebtCapacity debtCapacity);

}
