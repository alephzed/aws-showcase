package com.showcase.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.showcase.scheduling.domain.Appointment;
import com.showcase.scheduling.domain.AvailabilitySlot;
import com.showcase.scheduling.domain.SlotStatus;
import com.showcase.scheduling.events.AppointmentBookedEvent;
import com.showcase.scheduling.events.AppointmentEventPublisher;
import com.showcase.scheduling.repo.AppointmentRepository;
import com.showcase.scheduling.repo.AvailabilitySlotRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final UUID PROVIDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PATIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SLOT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String CORRELATION_ID = "corr-123";

    @Mock
    private AvailabilitySlotRepository slots;
    @Mock
    private AppointmentRepository appointments;
    @Mock
    private AppointmentEventPublisher publisher;

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(slots, appointments, publisher);
    }

    @Test
    void booksAvailableSlotFlipsItToBookedAndPublishesEvent() {
        AvailabilitySlot slot = slot(SlotStatus.AVAILABLE);
        when(slots.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(appointments.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.book(SLOT_ID, PATIENT_ID, CORRELATION_ID);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(slot.getAppointmentId()).isEqualTo(result.getId());
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(result.getSlotId()).isEqualTo(SLOT_ID);
        verify(slots).saveAndFlush(slot);

        ArgumentCaptor<AppointmentBookedEvent> event = ArgumentCaptor.forClass(AppointmentBookedEvent.class);
        verify(publisher).publishAppointmentBooked(event.capture());
        assertThat(event.getValue().getAppointmentId()).isEqualTo(result.getId());
        assertThat(event.getValue().getSlotId()).isEqualTo(SLOT_ID);
        assertThat(event.getValue().getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(event.getValue().getCorrelationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void bookingAlreadyBookedSlotThrowsAndPublishesNothing() {
        AvailabilitySlot slot = slot(SlotStatus.BOOKED);
        when(slots.findById(SLOT_ID)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.book(SLOT_ID, PATIENT_ID, CORRELATION_ID))
                .isInstanceOf(SlotUnavailableException.class);

        verify(slots, never()).saveAndFlush(any());
        verify(appointments, never()).save(any());
        verify(publisher, never()).publishAppointmentBooked(any());
    }

    @Test
    void bookingMissingSlotThrows() {
        when(slots.findById(SLOT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(SLOT_ID, PATIENT_ID, CORRELATION_ID))
                .isInstanceOf(SlotUnavailableException.class);

        verify(publisher, never()).publishAppointmentBooked(any());
    }

    private static AvailabilitySlot slot(SlotStatus status) {
        AvailabilitySlot slot = newInstance(AvailabilitySlot.class);
        set(slot, "id", SLOT_ID);
        set(slot, "providerId", PROVIDER_ID);
        set(slot, "startTs", OffsetDateTime.parse("2026-07-01T09:00:00+00:00"));
        set(slot, "endTs", OffsetDateTime.parse("2026-07-01T09:30:00+00:00"));
        set(slot, "status", status);
        return slot;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
