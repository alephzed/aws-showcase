package com.showcase.scheduling.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.showcase.scheduling.domain.Appointment;

public class AppointmentResponse {

    private final UUID appointmentId;
    private final UUID patientId;
    private final UUID providerId;
    private final UUID slotId;
    private final String status;
    private final String correlationId;
    private final OffsetDateTime createdAt;

    public AppointmentResponse(Appointment appointment) {
        this.appointmentId = appointment.getId();
        this.patientId = appointment.getPatientId();
        this.providerId = appointment.getProviderId();
        this.slotId = appointment.getSlotId();
        this.status = appointment.getStatus().name();
        this.correlationId = appointment.getCorrelationId();
        this.createdAt = appointment.getCreatedAt();
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public String getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
