package com.trackcal.trackcalserver.controller;

import com.trackcal.trackcalserver.dto.DailySummaryResponse;
import com.trackcal.trackcalserver.service.MealEntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {
    private final MealEntryService mealEntryService;

    public SummaryController(MealEntryService mealEntryService) {
        this.mealEntryService = mealEntryService;
    }

    @GetMapping
    public DailySummaryResponse getDailySummary(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return mealEntryService.getDailySummary(authentication.getName(), date);
    }
}
