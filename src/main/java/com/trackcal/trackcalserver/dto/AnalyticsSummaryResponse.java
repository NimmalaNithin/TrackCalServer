package com.trackcal.trackcalserver.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalyticsSummaryResponse {
    private Double latestWeightKg;
    private Double weightChangeKg;
    private Integer totalExerciseCalories;
    private List<AnalyticsEntryResponse> entries;
}
