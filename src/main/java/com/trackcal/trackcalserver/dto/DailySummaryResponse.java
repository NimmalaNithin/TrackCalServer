package com.trackcal.trackcalserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DailySummaryResponse {
    private LocalDate date;
    private Integer calorieTarget;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fiber;
    private Integer fat;
    private Double weightKg;
    private Integer exerciseCalories;
    private List<MealEntryResponse> meals;
}
