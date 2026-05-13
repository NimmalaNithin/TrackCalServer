package com.trackcal.trackcalserver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEntryRequest {
    @NotNull(message = "Date is required")
    private LocalDate entryDate;

    @DecimalMin(value = "25.0", message = "Weight must be at least 25 kg")
    @DecimalMax(value = "350.0", message = "Weight must be realistic")
    private Double weightKg;

    @Min(value = 0, message = "Exercise calories cannot be negative")
    @Max(value = 5000, message = "Exercise calories are too high")
    private Integer exerciseCalories;
}
