package co.com.crediya.sqs.sender;

import co.com.crediya.model.loan.DebtCapacity;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSSenderDebtCapacityTest {

    @Test
    void send_buildsRequestWithQueueUrlAndBody_andReturnsMessageId() {
        // Mocks
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);
        var mapper = mock(ObjectMapper.class);

        when(props.queueUrlDebtCapacity()).thenReturn("https://sqs.aws/queue-debt");
        var response = SendMessageResponse.builder().messageId("msg-123").build();
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        var sender = new SQSSenderDebtCapacity(props, client, mapper);

        StepVerifier.create(sender.send("{\"hello\":\"world\"}"))
                .expectNext("msg-123")
                .verifyComplete();

        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(reqCap.capture());

        var req = reqCap.getValue();
        assertThat(req.queueUrl()).isEqualTo("https://sqs.aws/queue-debt");
        assertThat(req.messageBody()).isEqualTo("{\"hello\":\"world\"}");

        verify(props).queueUrlDebtCapacity();
        verifyNoMoreInteractions(client, props, mapper);
    }

    @Test
    void sendMessage_serializesDebtCapacity_andCompletes() throws Exception {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);
        var mapper = mock(ObjectMapper.class);

        when(props.queueUrlDebtCapacity()).thenReturn("https://sqs.aws/queue-debt");

        var debt = mock(DebtCapacity.class);
        when(mapper.writeValueAsString(debt)).thenReturn("{\"cap\":\"ok\"}");

        var response = SendMessageResponse.builder().messageId("id-999").build();
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        var sender = new SQSSenderDebtCapacity(props, client, mapper);

        StepVerifier.create(sender.sendMessage(debt))
                .verifyComplete();

        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(reqCap.capture());
        assertThat(reqCap.getValue().messageBody()).isEqualTo("{\"cap\":\"ok\"}");
        assertThat(reqCap.getValue().queueUrl()).isEqualTo("https://sqs.aws/queue-debt");

        verify(mapper).writeValueAsString(debt);
    }

    @Test
    void sendMessage_whenJsonSerializationFails_propagatesError_andDoesNotCallSQS() throws Exception {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);
        var mapper = mock(ObjectMapper.class);
        var debt   = mock(DebtCapacity.class);

        doThrow(new JsonProcessingException("boom") {}).when(mapper).writeValueAsString(debt);

        var sender = new SQSSenderDebtCapacity(props, client, mapper);

        StepVerifier.create(sender.sendMessage(debt))
                .expectErrorMessage("boom")
                .verify();

        verify(mapper).writeValueAsString(debt);
        verifyNoInteractions(client);
    }

    @Test
    void send_whenClientFails_propagatesError() {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);
        var mapper = mock(ObjectMapper.class);

        when(props.queueUrlDebtCapacity()).thenReturn("https://sqs.aws/queue-debt");
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("sqs-down")));

        var sender = new SQSSenderDebtCapacity(props, client, mapper);

        StepVerifier.create(sender.send("{\"x\":1}"))
                .expectErrorMessage("sqs-down")
                .verify();
    }
}

