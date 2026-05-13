package com.trackcal.trackcalserver.controller;

import com.trackcal.trackcalserver.dto.AnalyticsEntryRequest;
import com.trackcal.trackcalserver.dto.AnalyticsEntryResponse;
import com.trackcal.trackcalserver.dto.AnalyticsSummaryResponse;
import com.trackcal.trackcalserver.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AnalyticsSummaryResponse getSummary(Authentication authentication) {
        return analyticsService.getSummary(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalyticsEntryResponse saveEntry(
            Authentication authentication,
            @Valid @RequestBody AnalyticsEntryRequest request
    ) {
        return analyticsService.saveEntry(authentication.getName(), request);
    }
}
