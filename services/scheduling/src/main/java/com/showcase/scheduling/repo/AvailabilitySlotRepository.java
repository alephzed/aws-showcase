package com.showcase.scheduling.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.showcase.scheduling.domain.AvailabilitySlot;
import com.showcase.scheduling.domain.SlotStatus;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    // Derived queries backing GET /providers/{id}/slots. Split by which bounds
    // of the optional [from, to) start-time window are present, so no synthetic
    // sentinel timestamps (which would overflow Postgres timestamptz) are needed.

    List<AvailabilitySlot> findByProviderIdAndStatusOrderByStartTsAsc(
            UUID providerId, SlotStatus status);

    List<AvailabilitySlot> findByProviderIdAndStatusAndStartTsGreaterThanEqualOrderByStartTsAsc(
            UUID providerId, SlotStatus status, OffsetDateTime from);

    List<AvailabilitySlot> findByProviderIdAndStatusAndStartTsLessThanOrderByStartTsAsc(
            UUID providerId, SlotStatus status, OffsetDateTime to);

    List<AvailabilitySlot> findByProviderIdAndStatusAndStartTsGreaterThanEqualAndStartTsLessThanOrderByStartTsAsc(
            UUID providerId, SlotStatus status, OffsetDateTime from, OffsetDateTime to);
}
