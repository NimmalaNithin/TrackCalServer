package com.trackcal.trackcalserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AnalyticsEntryResponse {
    private String id;
    private LocalDate entryDate;
    private Double weightKg;
    private Integer exerciseCalories;
}
