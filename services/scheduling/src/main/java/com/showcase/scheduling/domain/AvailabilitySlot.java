package com.showcase.scheduling.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "availability_slots")
public class AvailabilitySlot {

    @Id
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "start_ts", nullable = false)
    private OffsetDateTime startTs;

    @Column(name = "end_ts", nullable = false)
    private OffsetDateTime endTs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlotStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    protected AvailabilitySlot() {
        // JPA
    }

    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE;
    }

    /**
     * Domain transition: reserve this slot for the given appointment.
     * Refuses to book a slot that is not currently AVAILABLE.
     */
    public void book(UUID appointmentId) {
        if (!isAvailable()) {
            throw new IllegalStateException("slot " + id + " is not AVAILABLE");
        }
        this.status = SlotStatus.BOOKED;
        this.appointmentId = appointmentId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public OffsetDateTime getStartTs() {
        return startTs;
    }

    public OffsetDateTime getEndTs() {
        return endTs;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }
}
