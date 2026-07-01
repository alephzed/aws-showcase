package com.showcase.scheduling.events;

import java.util.UUID;

/**
 * Payload of the AppointmentBooked domain event. Carries just what consumers
 * need plus a correlation id for cross-service tracing.
 */
public class AppointmentBookedEvent {

    private final UUID appointmentId;
    private final UUID slotId;
    private final UUID patientId;
    private final String correlationId;

    public AppointmentBookedEvent(UUID appointmentId, UUID slotId, UUID patientId, String correlationId) {
        this.appointmentId = appointmentId;
        this.slotId = slotId;
        this.patientId = patientId;
        this.correlationId = correlationId;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
