package co.com.crediya.sqs.sender;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanStatusChanged;
import co.com.crediya.model.customer.UserData;
import co.com.crediya.model.value.Email;
import co.com.crediya.model.value.Money;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSSenderNotificationTest {

    @Test
    void send_buildsRequestWithQueueUrlAndBody_andReturnsMessageId() {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);

        when(props.queueUrlNotification()).thenReturn("https://sqs.aws/queue-notif");
        var response = SendMessageResponse.builder().messageId("notif-123").build();
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        var sender = new SQSSenderNotification(props, client);

        StepVerifier.create(sender.send("{\"hello\":\"notif\"}"))
                .expectNext("notif-123")
                .verifyComplete();

        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(reqCap.capture());

        var req = reqCap.getValue();
        assertThat(req.queueUrl()).isEqualTo("https://sqs.aws/queue-notif");
        assertThat(req.messageBody()).isEqualTo("{\"hello\":\"notif\"}");

        verify(props).queueUrlNotification();
        verifyNoMoreInteractions(client, props);
    }

    @Test
    void sendMessage_formatsJsonFromLoanStatusChanged_andCompletes() {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);

        when(props.queueUrlNotification()).thenReturn("https://sqs.aws/queue-notif");
        var response = SendMessageResponse.builder().messageId("ok-1").build();
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Mock de LoanStatusChanged y anidados
        var changed = mock(LoanStatusChanged.class);
        var loan    = mock(Loan.class);
        var email   = mock(Email.class);
        var money   = mock(Money.class);
        var user    = mock(UserData.class);

        when(changed.loan()).thenReturn(loan);
        when(changed.stateName()).thenReturn("APPROVED");
        when(changed.typeName()).thenReturn("PERSONAL");
        when(changed.reason()).thenReturn("approved-after-review");
        when(changed.userData()).thenReturn(user);

        when(loan.id()).thenReturn("loan-123");
        when(loan.email()).thenReturn(email);
        when(email.value()).thenReturn("a@b.com");
        when(loan.amount()).thenReturn(money);
        when(money.value()).thenReturn(new BigDecimal("1000")); // -> "1000" en String.format

        when(user.name()).thenReturn("Ana");

        var sender = new SQSSenderNotification(props, client);

        StepVerifier.create(sender.sendMessage(changed))
                .verifyComplete();

        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(reqCap.capture());

        var expectedJson =
                "{\"loanId\":\"loan-123\",\"email\":\"a@b.com\",\"amount\":1000," +
                        "\"state\":\"APPROVED\",\"type\":\"PERSONAL\",\"reason\":\"approved-after-review\",\"name\":\"Ana\"}";
        assertThat(reqCap.getValue().messageBody()).isEqualTo(expectedJson);
        assertThat(reqCap.getValue().queueUrl()).isEqualTo("https://sqs.aws/queue-notif");
    }

    @Test
    void send_whenClientFails_propagatesError() {
        var props  = mock(SQSSenderProperties.class);
        var client = mock(SqsAsyncClient.class);

        when(props.queueUrlNotification()).thenReturn("https://sqs.aws/queue-notif");
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));

        var sender = new SQSSenderNotification(props, client);

        StepVerifier.create(sender.send("{\"x\":1}"))
                .expectErrorMessage("down")
                .verify();
    }
}
