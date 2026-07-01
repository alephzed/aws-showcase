package com.showcase.scheduling.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.showcase.scheduling.domain.AvailabilitySlot;
import com.showcase.scheduling.domain.SlotStatus;
import com.showcase.scheduling.repo.AvailabilitySlotRepository;

/**
 * Read-side queries over provider availability. Booking stays in
 * {@link BookingService}; this only exposes AVAILABLE slots for browsing.
 */
@Service
public class SlotQueryService {

    private final AvailabilitySlotRepository slots;

    public SlotQueryService(AvailabilitySlotRepository slots) {
        this.slots = slots;
    }

    /**
     * Returns a provider's AVAILABLE slots ordered by start time, optionally
     * filtered to the half-open window [from, to) on start time. Either bound
     * may be null to leave that side open.
     */
    @Transactional(readOnly = true)
    public List<AvailabilitySlot> listAvailable(UUID providerId, OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null) {
            return slots.findByProviderIdAndStatusAndStartTsGreaterThanEqualAndStartTsLessThanOrderByStartTsAsc(
                    providerId, SlotStatus.AVAILABLE, from, to);
        }
        if (from != null) {
            return slots.findByProviderIdAndStatusAndStartTsGreaterThanEqualOrderByStartTsAsc(
                    providerId, SlotStatus.AVAILABLE, from);
        }
        if (to != null) {
            return slots.findByProviderIdAndStatusAndStartTsLessThanOrderByStartTsAsc(
                    providerId, SlotStatus.AVAILABLE, to);
        }
        return slots.findByProviderIdAndStatusOrderByStartTsAsc(providerId, SlotStatus.AVAILABLE);
    }
}
