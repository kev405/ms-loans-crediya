//package co.com.crediya.sqs.sender;
//
//import co.com.crediya.model.loan.Loan;
//import co.com.crediya.model.loan.LoanStatusChanged;
//import co.com.crediya.model.value.Email;
//import co.com.crediya.model.value.Money;
//import co.com.crediya.model.value.TermMonths;
//import co.com.crediya.sqs.sender.config.SQSSenderProperties;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//import software.amazon.awssdk.services.sqs.SqsAsyncClient;
//import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
//import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class SQSSenderTest {
//
//    private SQSSenderProperties properties;
//    private SqsAsyncClient client;
//    private SQSSender sender;
//
//    @BeforeEach
//    void setUp() {
//        properties = mock(SQSSenderProperties.class);
//        client     = mock(SqsAsyncClient.class);
//        sender     = new SQSSender(properties, client);
//    }
//
//    @Test
//    void send_happyPath_callsClient_withBuiltRequest_andReturnsMessageId() {
//        // Arrange
//        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue";
//        String body     = "hello-world";
//        when(properties.queueUrl()).thenReturn(queueUrl);
//
//        var response = SendMessageResponse.builder().messageId("mid-123").build();
//        when(client.sendMessage(any(SendMessageRequest.class)))
//                .thenReturn(CompletableFuture.completedFuture(response));
//
//        // Act + Assert
//        StepVerifier.create(sender.send(body))
//                .expectNext("mid-123")
//                .verifyComplete();
//
//        // Verificamos el request construido
//        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
//        verify(client).sendMessage(reqCap.capture());
//        SendMessageRequest req = reqCap.getValue();
//        assertThat(req.queueUrl()).isEqualTo(queueUrl);
//        assertThat(req.messageBody()).isEqualTo(body);
//
//        verifyNoMoreInteractions(client);
//    }
//
//    @Test
//    void send_whenClientFails_propagatesError() {
//        // Arrange
//        when(properties.queueUrl()).thenReturn("http://dummy");
//        var boom = new RuntimeException("boom");
//        CompletableFuture<SendMessageResponse> failed = new CompletableFuture<>();
//        failed.completeExceptionally(boom);
//
//        when(client.sendMessage(any(SendMessageRequest.class))).thenReturn(failed);
//
//        // Act + Assert
//        StepVerifier.create(sender.send("x"))
//                .expectErrorMatches(e -> e.getMessage().contains("boom"))
//                .verify();
//
//        verify(client).sendMessage(any(SendMessageRequest.class));
//    }
//
//    @Test
//    void sendMessage_buildsExactJson_andDelegatesToSend() {
//        // Arrange: Loan y LoanStatusChanged
//        String loanId = UUID.randomUUID().toString();
//        String email  = "user@example.com";
//        String typeId = UUID.randomUUID().toString();
//        String stateId= UUID.randomUUID().toString();
//
//        var loan = new Loan(
//                loanId,
//                new Money(new BigDecimal("1234.50")), // toString => "1234.50"
//                new TermMonths(12),
//                new Email(email),
//                stateId,
//                typeId
//        );
//
//        String stateName = "APPROVED";
//        String typeName  = "Libre Inversión";
//        String reason    = "ok";
//        var changed = new LoanStatusChanged(loan, stateName, typeName, reason, /*customer*/ null);
//
//        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue";
//        when(properties.queueUrl()).thenReturn(queueUrl);
//        when(client.sendMessage(any(SendMessageRequest.class)))
//                .thenReturn(CompletableFuture.completedFuture(
//                        SendMessageResponse.builder().messageId("mid-999").build()
//                ));
//
//        // JSON que esperamos EXACTAMENTE (sin espacios)
//        String expectedJson = String.format(
//                "{\"loanId\":\"%s\",\"email\":\"%s\",\"amount\":%s,\"state\":\"%s\",\"type\":\"%s\",\"reason\":\"%s\"}",
//                loanId, email, "1234.50", stateName, typeName, reason
//        );
//
//        // Act
//        Mono<Void> result = sender.sendMessage(changed);
//
//        // Assert: completa OK y el body es el esperado
//        StepVerifier.create(result).verifyComplete();
//
//        var reqCap = ArgumentCaptor.forClass(SendMessageRequest.class);
//        verify(client).sendMessage(reqCap.capture());
//        SendMessageRequest req = reqCap.getValue();
//        assertThat(req.queueUrl()).isEqualTo(queueUrl);
//        assertThat(req.messageBody()).isEqualTo(expectedJson);
//    }
//}
//
