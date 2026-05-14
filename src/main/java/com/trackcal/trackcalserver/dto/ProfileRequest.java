package com.trackcal.trackcalserver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    @NotNull(message = "Age is required")
    @Min(value = 13, message = "Age must be at least 13")
    @Max(value = 120, message = "Age must be realistic")
    private Integer age;

    @NotBlank(message = "Sex is required")
    private String sex;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "90.0", message = "Height must be at least 90 cm")
    @DecimalMax(value = "250.0", message = "Height must be realistic")
    @Digits(integer = 3, fraction = 2, message = "Height can have up to 2 decimal places")
    private Double heightCm;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "25.0", message = "Weight must be at least 25 kg")
    @DecimalMax(value = "350.0", message = "Weight must be realistic")
    @Digits(integer = 3, fraction = 2, message = "Weight can have up to 2 decimal places")
    private Double weightKg;

    @NotNull(message = "Target weight is required")
    @DecimalMin(value = "25.0", message = "Target weight must be at least 25 kg")
    @DecimalMax(value = "350.0", message = "Target weight must be realistic")
    @Digits(integer = 3, fraction = 2, message = "Target weight can have up to 2 decimal places")
    private Double targetWeightKg;

    @NotBlank(message = "Activity level is required")
    private String activityLevel;

    @NotBlank(message = "Target strategy is required")
    private String targetStrategy;

    @Min(value = 7, message = "Plan must be at least 7 days")
    @Max(value = 730, message = "Plan must be 730 days or fewer")
    private Integer daysToTarget;

    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    @Min(value = 0, message = "Daily calorie adjustment cannot be negative")
    @Max(value = 1000, message = "Daily calorie adjustment cannot exceed 1000")
    private Integer dailyCalorieAdjustment;
}
