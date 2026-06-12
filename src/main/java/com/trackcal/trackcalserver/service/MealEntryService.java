package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.dto.DailySummaryResponse;
import com.trackcal.trackcalserver.dto.MealEntryRequest;
import com.trackcal.trackcalserver.dto.MealEntryResponse;
import com.trackcal.trackcalserver.model.AnalyticsEntry;
import com.trackcal.trackcalserver.model.MealEntry;
import com.trackcal.trackcalserver.model.UserProfile;
import com.trackcal.trackcalserver.repository.AnalyticsEntryRepository;
import com.trackcal.trackcalserver.repository.MealEntryRepository;
import com.trackcal.trackcalserver.repository.UserProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class MealEntryService {
    private final MealEntryRepository mealEntryRepository;
    private final AnalyticsEntryRepository analyticsEntryRepository;
    private final UserProfileRepository profileRepository;
    private final CurrentUserService currentUserService;

    public MealEntryService(
            MealEntryRepository mealEntryRepository,
            AnalyticsEntryRepository analyticsEntryRepository,
            UserProfileRepository profileRepository,
            CurrentUserService currentUserService
    ) {
        this.mealEntryRepository = mealEntryRepository;
        this.analyticsEntryRepository = analyticsEntryRepository;
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
    }

    public DailySummaryResponse getDailySummary(String email, LocalDate date) {
        String userId = currentUserService.requireUserId(email);
        List<MealEntryResponse> meals = getMealsByUserIdAndDate(userId, date.toString());

        int calories = meals.stream().mapToInt(MealEntryResponse::getCalories).sum();
        int protein = meals.stream().mapToInt(MealEntryResponse::getProtein).sum();
        int carbs = meals.stream().mapToInt(MealEntryResponse::getCarbs).sum();
        int fiber = meals.stream().mapToInt(MealEntryResponse::getFiber).sum();
        int fat = meals.stream().mapToInt(MealEntryResponse::getFat).sum();
        Integer target = profileRepository.findByUserId(userId).map(UserProfile::getCalorieTarget).orElse(null);
        AnalyticsEntry analyticsEntry = analyticsEntryRepository.findByUserIdAndEntryDate(userId, date).orElse(null);

        return DailySummaryResponse.builder()
                .date(date)
                .calorieTarget(target)
                .calories(calories)
                .protein(protein)
                .carbs(carbs)
                .fiber(fiber)
                .fat(fat)
                .weightKg(analyticsEntry == null ? null : analyticsEntry.getWeightKg())
                .exerciseCalories(analyticsEntry == null ? null : defaultValue(analyticsEntry.getExerciseCalories()))
                .meals(meals)
                .build();
    }

    public List<MealEntryResponse> getMeals(String email, LocalDate date) {
        String userId = currentUserService.requireUserId(email);
        return getMealsByUserIdAndDate(userId, date.toString());
    }

    public MealEntryResponse addMeal(String email, MealEntryRequest request) {
        String userId = currentUserService.requireUserId(email);
        Instant now = Instant.now();
        MealEntry meal = MealEntry.builder()
                .userId(userId)
                .entryDate(request.getEntryDate().toString())
                .name(request.getName().trim())
                .mealType(request.getMealType())
                .calories(request.getCalories())
                .protein(defaultValue(request.getProtein()))
                .carbs(defaultValue(request.getCarbs()))
                .fiber(defaultValue(request.getFiber()))
                .fat(defaultValue(request.getFat()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(mealEntryRepository.save(meal));
    }

    public void deleteMeal(String email, String id) {
        String userId = currentUserService.requireUserId(email);
        MealEntry meal = mealEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meal entry not found"));

        if (!userId.equals(meal.getUserId())) {
            throw new AccessDeniedException("You cannot delete this meal");
        }

        mealEntryRepository.delete(meal);
    }

    private MealEntryResponse toResponse(MealEntry meal) {
        return MealEntryResponse.builder()
                .id(meal.getId())
                .name(meal.getName())
                .mealType(meal.getMealType())
                .entryDate(meal.getEntryDate())
                .calories(defaultValue(meal.getCalories()))
                .protein(defaultValue(meal.getProtein()))
                .carbs(defaultValue(meal.getCarbs()))
                .fiber(defaultValue(meal.getFiber()))
                .fat(defaultValue(meal.getFat()))
                .build();
    }

    private List<MealEntryResponse> getMealsByUserIdAndDate(String userId, String date) {
        return mealEntryRepository
                .findByUserIdAndEntryDate(userId,date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Integer defaultValue(Integer value) {
        return value == null ? 0 : value;
    }
}
