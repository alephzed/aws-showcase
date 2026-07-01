package com.showcase.scheduling.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.showcase.scheduling.domain.AvailabilitySlot;

/**
 * Read model for a provider availability slot exposed by
 * {@code GET /providers/{providerId}/slots}.
 */
public class SlotResponse {

    private final UUID slotId;
    private final UUID providerId;
    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final String status;

    public SlotResponse(AvailabilitySlot slot) {
        this.slotId = slot.getId();
        this.providerId = slot.getProviderId();
        this.start = slot.getStartTs();
        this.end = slot.getEndTs();
        this.status = slot.getStatus().name();
    }

    public UUID getSlotId() {
        return slotId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public String getStatus() {
        return status;
    }
}
