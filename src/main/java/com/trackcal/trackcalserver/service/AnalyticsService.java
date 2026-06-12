package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.dto.AnalyticsEntryRequest;
import com.trackcal.trackcalserver.dto.AnalyticsEntryResponse;
import com.trackcal.trackcalserver.dto.AnalyticsSummaryResponse;
import com.trackcal.trackcalserver.model.AnalyticsEntry;
import com.trackcal.trackcalserver.repository.AnalyticsEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalyticsService {
    private final AnalyticsEntryRepository analyticsEntryRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(
            AnalyticsEntryRepository analyticsEntryRepository,
            CurrentUserService currentUserService
    ) {
        this.analyticsEntryRepository = analyticsEntryRepository;
        this.currentUserService = currentUserService;
    }

    public AnalyticsSummaryResponse getSummary(String email) {
        String userId = currentUserService.requireUserId(email);
        List<AnalyticsEntryResponse> entries = analyticsEntryRepository
                .findByUserIdOrderByEntryDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        List<Double> weights = entries.stream()
                .map(AnalyticsEntryResponse::getWeightKg)
                .filter(weight -> weight != null)
                .toList();
        Double latestWeight = weights.isEmpty() ? null : weights.getFirst();
        Double previousWeight = weights.size() < 2 ? null : weights.get(1);
        Double weightChange = latestWeight == null || previousWeight == null ? null : latestWeight - previousWeight;
        Integer exerciseCalories = entries.stream()
                .mapToInt(entry -> entry.getExerciseCalories() == null ? 0 : entry.getExerciseCalories())
                .sum();

        return AnalyticsSummaryResponse.builder()
                .latestWeightKg(latestWeight)
                .weightChangeKg(weightChange)
                .totalExerciseCalories(exerciseCalories)
                .entries(entries.stream()
                        .sorted(Comparator.comparing(AnalyticsEntryResponse::getEntryDate))
                        .toList())
                .build();
    }

    public AnalyticsEntryResponse saveEntry(String email, AnalyticsEntryRequest request) {
        String userId = currentUserService.requireUserId(email);
        AnalyticsEntry entry = analyticsEntryRepository
                .findByUserIdAndEntryDate(userId, request.getEntryDate())
                .orElseGet(AnalyticsEntry::new);
        Instant now = Instant.now();

        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(now);
        }
        entry.setUserId(userId);
        entry.setEntryDate(request.getEntryDate());
        if (request.getWeightKg() != null) {
            entry.setWeightKg(request.getWeightKg());
        }
        if (request.getExerciseCalories() != null) {
            entry.setExerciseCalories(request.getExerciseCalories());
        }
        entry.setUpdatedAt(now);

        return toResponse(analyticsEntryRepository.save(entry));
    }

    private AnalyticsEntryResponse toResponse(AnalyticsEntry entry) {
        return AnalyticsEntryResponse.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .weightKg(entry.getWeightKg())
                .exerciseCalories(entry.getExerciseCalories())
                .build();
    }
}
