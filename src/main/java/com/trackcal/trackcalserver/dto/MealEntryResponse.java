package com.trackcal.trackcalserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MealEntryResponse {
    private String id;
    private String name;
    private String mealType;
    private LocalDate entryDate;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fiber;
    private Integer fat;
}
