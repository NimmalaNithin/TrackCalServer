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
@Document(collection = "meals")
public class MealEntry {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate entryDate;

    private String name;
    private String mealType;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fiber;
    private Integer fat;
    private Instant createdAt;
}
