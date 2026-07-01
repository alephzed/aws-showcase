package com.showcase.scheduling.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.showcase.scheduling.domain.Appointment;
import com.showcase.scheduling.domain.AvailabilitySlot;
import com.showcase.scheduling.events.AppointmentBookedEvent;
import com.showcase.scheduling.events.AppointmentEventPublisher;
import com.showcase.scheduling.repo.AppointmentRepository;
import com.showcase.scheduling.repo.AvailabilitySlotRepository;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final AvailabilitySlotRepository slots;
    private final AppointmentRepository appointments;
    private final AppointmentEventPublisher publisher;

    public BookingService(AvailabilitySlotRepository slots,
                          AppointmentRepository appointments,
                          AppointmentEventPublisher publisher) {
        this.slots = slots;
        this.appointments = appointments;
        this.publisher = publisher;
    }

    /**
     * Books an AVAILABLE slot in a single transaction: flip AVAILABLE -> BOOKED
     * (guarded by the optimistic @Version and the unique partial index), persist
     * the appointment, then publish AppointmentBooked.
     *
     * @throws SlotUnavailableException (mapped to 409) if the slot is missing,
     *         not available, or taken concurrently.
     */
    @Transactional
    public Appointment book(UUID slotId, UUID patientId, String correlationId) {
        AvailabilitySlot slot = slots.findById(slotId)
                .orElseThrow(() -> new SlotUnavailableException("slot not found: " + slotId));

        if (!slot.isAvailable()) {
            throw new SlotUnavailableException("slot not available: " + slotId);
        }

        UUID appointmentId = UUID.randomUUID();
        slot.book(appointmentId);

        try {
            // Flush now so the optimistic version bump and the unique partial index
            // are enforced inside this method, converting a race into a 409.
            slots.saveAndFlush(slot);
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new SlotUnavailableException("slot was taken concurrently: " + slotId, e);
        }

        Appointment appointment = appointments.save(
                new Appointment(appointmentId, patientId, slot.getProviderId(), slotId, correlationId));

        publisher.publishAppointmentBooked(new AppointmentBookedEvent(
                appointment.getId(), slotId, patientId, correlationId));

        log.info("Booked appointment={} slot={} patient={} correlationId={}",
                appointment.getId(), slotId, patientId, correlationId);
        return appointment;
    }
}
