package com.showcase.scheduling.events;

public interface AppointmentEventPublisher {

    void publishAppointmentBooked(AppointmentBookedEvent event);
}
