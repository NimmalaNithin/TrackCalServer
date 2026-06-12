package com.trackcal.trackcalserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_details")
public class UserProfile {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

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
    private Instant createdAt;
    private Instant updatedAt;
}
