package com.trackcal.trackcalserver.dto;

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
public class MealEntryRequest {
    @NotBlank(message = "Meal name is required")
    private String name;

    @NotBlank(message = "Meal type is required")
    private String mealType;

    @NotNull(message = "Date is required")
    private LocalDate entryDate;

    @NotNull(message = "Calories are required")
    @Min(value = 0, message = "Calories cannot be negative")
    @Max(value = 10000, message = "Calories are too high")
    private Integer calories;

    @Min(value = 0, message = "Protein cannot be negative")
    @Max(value = 1000, message = "Protein is too high")
    private Integer protein;

    @Min(value = 0, message = "Carbs cannot be negative")
    @Max(value = 1000, message = "Carbs are too high")
    private Integer carbs;

    @Min(value = 0, message = "Fiber cannot be negative")
    @Max(value = 1000, message = "Fiber is too high")
    private Integer fiber;

    @Min(value = 0, message = "Fat cannot be negative")
    @Max(value = 1000, message = "Fat is too high")
    private Integer fat;
}
