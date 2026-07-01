package com.showcase.scheduling.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.showcase.scheduling.domain.AvailabilitySlot;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {
}
