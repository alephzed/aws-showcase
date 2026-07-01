package com.showcase.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.showcase.scheduling.domain.AvailabilitySlot;
import com.showcase.scheduling.domain.SlotStatus;
import com.showcase.scheduling.repo.AvailabilitySlotRepository;

/**
 * Unit tests for the slot-listing behaviour: it always restricts to AVAILABLE
 * slots (never BOOKED) and routes to the correct repository query for each
 * combination of the optional [from, to) start-time window.
 */
@ExtendWith(MockitoExtension.class)
class SlotQueryServiceTest {

    private static final UUID PROVIDER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-07-01T09:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-07-01T12:00:00Z");

    @Mock
    private AvailabilitySlotRepository slots;

    private SlotQueryService service;

    @BeforeEach
    void setUp() {
        service = new SlotQueryService(slots);
    }

    @Test
    void noWindowListsAllAvailableForProviderOnly() {
        List<AvailabilitySlot> expected = List.of();
        when(slots.findByProviderIdAndStatusOrderByStartTsAsc(PROVIDER, SlotStatus.AVAILABLE))
                .thenReturn(expected);

        assertThat(service.listAvailable(PROVIDER, null, null)).isSameAs(expected);

        // Only-available guarantee: the BOOKED path is never queried.
        verify(slots).findByProviderIdAndStatusOrderByStartTsAsc(PROVIDER, SlotStatus.AVAILABLE);
        verify(slots, never()).findByProviderIdAndStatusAndStartTsGreaterThanEqualAndStartTsLessThanOrderByStartTsAsc(
                any(), any(), any(), any());
    }

    @Test
    void bothBoundsUsesHalfOpenWindowQueryWithAvailableStatus() {
        List<AvailabilitySlot> expected = List.of();
        when(slots.findByProviderIdAndStatusAndStartTsGreaterThanEqualAndStartTsLessThanOrderByStartTsAsc(
                PROVIDER, SlotStatus.AVAILABLE, FROM, TO)).thenReturn(expected);

        assertThat(service.listAvailable(PROVIDER, FROM, TO)).isSameAs(expected);

        verify(slots).findByProviderIdAndStatusAndStartTsGreaterThanEqualAndStartTsLessThanOrderByStartTsAsc(
                PROVIDER, SlotStatus.AVAILABLE, FROM, TO);
    }

    @Test
    void fromOnlyUsesLowerBoundQuery() {
        when(slots.findByProviderIdAndStatusAndStartTsGreaterThanEqualOrderByStartTsAsc(
                eq(PROVIDER), eq(SlotStatus.AVAILABLE), eq(FROM))).thenReturn(List.of());

        service.listAvailable(PROVIDER, FROM, null);

        verify(slots).findByProviderIdAndStatusAndStartTsGreaterThanEqualOrderByStartTsAsc(
                PROVIDER, SlotStatus.AVAILABLE, FROM);
    }

    @Test
    void toOnlyUsesUpperBoundQuery() {
        when(slots.findByProviderIdAndStatusAndStartTsLessThanOrderByStartTsAsc(
                eq(PROVIDER), eq(SlotStatus.AVAILABLE), eq(TO))).thenReturn(List.of());

        service.listAvailable(PROVIDER, null, TO);

        verify(slots).findByProviderIdAndStatusAndStartTsLessThanOrderByStartTsAsc(
                PROVIDER, SlotStatus.AVAILABLE, TO);
    }
}
