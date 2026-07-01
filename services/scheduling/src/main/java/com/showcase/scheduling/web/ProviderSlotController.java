package com.showcase.scheduling.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.showcase.scheduling.service.SlotQueryService;

@RestController
@RequestMapping("/providers")
public class ProviderSlotController {

    private final SlotQueryService slotQueryService;

    public ProviderSlotController(SlotQueryService slotQueryService) {
        this.slotQueryService = slotQueryService;
    }

    /**
     * Lists a provider's AVAILABLE slots ordered by start time. Optional
     * {@code from}/{@code to} ISO-8601 params filter by the half-open start-time
     * window [from, to).
     */
    @GetMapping("/{providerId}/slots")
    public List<SlotResponse> listSlots(
            @PathVariable UUID providerId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        return slotQueryService.listAvailable(providerId, from, to).stream()
                .map(SlotResponse::new)
                .collect(Collectors.toList());
    }
}
