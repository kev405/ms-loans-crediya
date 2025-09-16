package co.com.crediya.sqs.sender;

import co.com.crediya.model.loan.DebtCapacity;
import co.com.crediya.model.loan.gateways.DebtCapacitySQS;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSenderDebtCapacity implements DebtCapacitySQS {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent {}", response.messageId()))
                .doOnError(e -> log.error("Error sending to SQS", e))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrlDebtCapacity())
                .messageBody(message)
                .build();
    }


    @Override
    public Mono<Void> sendMessage(DebtCapacity debtCapacity) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(debtCapacity))
                .doOnNext(jsonMessage -> log.info("Sending message to SQS: {}", jsonMessage))
                .flatMap(this::send)
                .then();
    }
}
