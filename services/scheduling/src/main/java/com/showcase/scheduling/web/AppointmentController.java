package com.showcase.scheduling.web;

import java.util.UUID;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.showcase.scheduling.domain.Appointment;
import com.showcase.scheduling.service.BookingService;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final BookingService bookingService;

    public AppointmentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request) {
        String correlationId = request.getCorrelationId() != null && !request.getCorrelationId().isBlank()
                ? request.getCorrelationId()
                : UUID.randomUUID().toString();

        Appointment appointment = bookingService.book(request.getSlotId(), request.getPatientId(), correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AppointmentResponse(appointment));
    }
}
