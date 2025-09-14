package co.com.crediya.sqs.sender;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanStatusChanged;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements Notification {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent {}", response.messageId()))
                .doOnError(e -> log.error("Error sending to SQS", e))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }

    @Override
    public Mono<Void> sendMessage(LoanStatusChanged loanStatusChanged) {
        String message = String.format(
                "{\"loanId\":\"%s\",\"email\":\"%s\",\"amount\":%s,\"state\":\"%s\",\"type\":\"%s\",\"reason\":\"%s\",\"name\":\"%s\"}",
                loanStatusChanged.loan().id(), loanStatusChanged.loan().email().value(), loanStatusChanged.loan().amount().value(), loanStatusChanged.stateName(), loanStatusChanged.typeName(), loanStatusChanged.reason(), loanStatusChanged.userData().name());
        log.info("Sending message to SQS: {}", message);
        return send(message)
                .doOnSuccess(id -> log.info("SQS send OK id={}", id))
                .doOnError(e -> log.error("SQS send FAILED", e))
                .then();
    }
}
