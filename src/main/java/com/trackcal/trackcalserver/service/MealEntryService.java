package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.dto.DailySummaryResponse;
import com.trackcal.trackcalserver.dto.MealEntryRequest;
import com.trackcal.trackcalserver.dto.MealEntryResponse;
import com.trackcal.trackcalserver.model.MealEntry;
import com.trackcal.trackcalserver.model.UserProfile;
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
    private final UserProfileRepository profileRepository;
    private final CurrentUserService currentUserService;

    public MealEntryService(
            MealEntryRepository mealEntryRepository,
            UserProfileRepository profileRepository,
            CurrentUserService currentUserService
    ) {
        this.mealEntryRepository = mealEntryRepository;
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
    }

    public DailySummaryResponse getDailySummary(String email, LocalDate date) {
        String userId = currentUserService.requireUserId(email);
        List<MealEntryResponse> meals = mealEntryRepository
                .findByUserIdAndEntryDateOrderByCreatedAtDesc(userId, date)
                .stream()
                .map(this::toResponse)
                .toList();

        int calories = meals.stream().mapToInt(MealEntryResponse::getCalories).sum();
        int protein = meals.stream().mapToInt(MealEntryResponse::getProtein).sum();
        int carbs = meals.stream().mapToInt(MealEntryResponse::getCarbs).sum();
        int fat = meals.stream().mapToInt(MealEntryResponse::getFat).sum();
        Integer target = profileRepository.findByUserId(userId).map(UserProfile::getCalorieTarget).orElse(null);

        return DailySummaryResponse.builder()
                .date(date)
                .calorieTarget(target)
                .calories(calories)
                .protein(protein)
                .carbs(carbs)
                .fat(fat)
                .meals(meals)
                .build();
    }

    public MealEntryResponse addMeal(String email, MealEntryRequest request) {
        String userId = currentUserService.requireUserId(email);
        MealEntry meal = MealEntry.builder()
                .userId(userId)
                .entryDate(request.getEntryDate())
                .name(request.getName().trim())
                .mealType(request.getMealType())
                .calories(request.getCalories())
                .protein(defaultValue(request.getProtein()))
                .carbs(defaultValue(request.getCarbs()))
                .fat(defaultValue(request.getFat()))
                .createdAt(Instant.now())
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
                .fat(defaultValue(meal.getFat()))
                .build();
    }

    private Integer defaultValue(Integer value) {
        return value == null ? 0 : value;
    }
}
