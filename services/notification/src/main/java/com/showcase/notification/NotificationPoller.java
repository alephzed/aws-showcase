package com.showcase.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Polls the Notification SQS queue for AppointmentBooked events (delivered inside
 * the EventBridge envelope), logs a stubbed confirmation, and deletes the message.
 */
@Component
public class NotificationPoller {

    private static final Logger log = LoggerFactory.getLogger(NotificationPoller.class);

    private final SqsClient sqs;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public NotificationPoller(SqsClient sqs,
                              ObjectMapper objectMapper,
                              @Value("${app.sqs.queue-url}") String queueUrl) {
        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelayString = "${app.sqs.poll-delay-ms:1000}")
    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(10)
                .build();

        List<Message> messages = sqs.receiveMessage(request).messages();
        for (Message message : messages) {
            try {
                handle(message);
            } catch (Exception e) {
                // Leave the message on the queue; SQS will redeliver and eventually DLQ it.
                log.error("Failed to process message {}: {}", message.messageId(), e.getMessage(), e);
                continue;
            }
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        }
    }

    private void handle(Message message) throws Exception {
        JsonNode envelope = objectMapper.readTree(message.body());
        JsonNode detail = envelope.path("detail");
        String appointmentId = detail.path("appointmentId").asText(null);
        String correlationId = detail.path("correlationId").asText(null);

        if (appointmentId == null) {
            log.warn("Received message without appointmentId in detail: {}", message.body());
            return;
        }

        // Stubbed confirmation delivery (real SMS/email provider is out of scope).
        log.info("confirmation sent for appointment {} (correlationId={})", appointmentId, correlationId);
    }
}
