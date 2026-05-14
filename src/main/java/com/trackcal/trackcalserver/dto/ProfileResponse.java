package com.trackcal.trackcalserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProfileResponse {
    private Integer age;
    private String sex;
    private Double heightCm;
    private Double weightKg;
    private Double targetWeightKg;
    private String activityLevel;
    private String goal;
    private String targetStrategy;
    private Integer daysToTarget;
    private LocalDate targetDate;
    private Integer dailyCalorieAdjustment;
    private Integer maintenanceCalories;
    private Integer calorieTarget;
    private Integer proteinTarget;
    private Integer carbTarget;
    private Integer fatTarget;
}
