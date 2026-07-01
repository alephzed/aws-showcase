package com.showcase.scheduling.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * Publishes AppointmentBooked to the custom EventBridge bus.
 * Source=scheduling, DetailType=AppointmentBooked.
 */
@Component
public class EventBridgeAppointmentEventPublisher implements AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventBridgeAppointmentEventPublisher.class);
    private static final String SOURCE = "scheduling";
    private static final String DETAIL_TYPE = "AppointmentBooked";

    private final EventBridgeClient client;
    private final ObjectMapper objectMapper;
    private final String busName;

    public EventBridgeAppointmentEventPublisher(EventBridgeClient client,
                                                ObjectMapper objectMapper,
                                                @Value("${app.eventbridge.bus-name}") String busName) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.busName = busName;
    }

    @Override
    public void publishAppointmentBooked(AppointmentBookedEvent event) {
        String detail = toDetailJson(event);
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(busName)
                .source(SOURCE)
                .detailType(DETAIL_TYPE)
                .detail(detail)
                .build();

        PutEventsResponse response = client.putEvents(PutEventsRequest.builder()
                .entries(entry)
                .build());

        if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
            String reason = response.entries().isEmpty() ? "unknown" : response.entries().get(0).errorMessage();
            throw new IllegalStateException("EventBridge PutEvents failed: " + reason);
        }
        log.info("Published AppointmentBooked for appointment={} correlationId={}",
                event.getAppointmentId(), event.getCorrelationId());
    }

    private String toDetailJson(AppointmentBookedEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("appointmentId", event.getAppointmentId().toString());
        node.put("slotId", event.getSlotId().toString());
        node.put("patientId", event.getPatientId().toString());
        node.put("correlationId", event.getCorrelationId());
        return node.toString();
    }
}
