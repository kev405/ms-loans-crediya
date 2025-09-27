package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.ReportMessage;
import reactor.core.publisher.Mono;

public interface ReportSQS {

    Mono<Void> sendMessage(ReportMessage reportMessage);

}
