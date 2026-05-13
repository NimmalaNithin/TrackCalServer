package com.trackcal.trackcalserver.service;

import com.mongodb.client.result.DeleteResult;
import com.trackcal.trackcalserver.dto.ProfileRequest;
import com.trackcal.trackcalserver.dto.ProfileResponse;
import com.trackcal.trackcalserver.exception.UserNotFoundException;
import com.trackcal.trackcalserver.model.AnalyticsEntry;
import com.trackcal.trackcalserver.model.MealEntry;
import com.trackcal.trackcalserver.model.User;
import com.trackcal.trackcalserver.model.UserProfile;
import com.trackcal.trackcalserver.repository.UserProfileRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ProfileService {
    private final UserProfileRepository profileRepository;
    private final CurrentUserService currentUserService;
    private final MongoTemplate mongoTemplate;

    public ProfileService(
            UserProfileRepository profileRepository,
            CurrentUserService currentUserService,
            MongoTemplate mongoTemplate
    ) {
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
        this.mongoTemplate = mongoTemplate;
    }

    public ProfileResponse getProfile(String email) {
        String userId = currentUserService.requireUserId(email);
        return profileRepository.findByUserId(userId).map(this::toResponse).orElse(null);
    }

    public void deleteAccount(String email) {
        User user = currentUserService.requireUser(email);
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId())), AnalyticsEntry.class);
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId())), MealEntry.class);
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId())), UserProfile.class);

        DeleteResult deletedUser = mongoTemplate.remove(Query.query(Criteria.where("_id").is(user.getId())), User.class);
        if (deletedUser.getDeletedCount() == 0) {
            throw new UserNotFoundException("User not found");
        }
    }

    public ProfileResponse saveProfile(String email, ProfileRequest request) {
        String userId = currentUserService.requireUserId(email);
        Targets targets = calculateTargets(request);
        UserProfile profile = profileRepository.findByUserId(userId).orElseGet(UserProfile::new);

        profile.setUserId(userId);
        profile.setAge(request.getAge());
        profile.setSex(request.getSex());
        profile.setHeightCm(request.getHeightCm());
        profile.setWeightKg(request.getWeightKg());
        profile.setTargetWeightKg(request.getTargetWeightKg());
        profile.setActivityLevel(request.getActivityLevel());
        profile.setGoal(request.getGoal());
        profile.setTargetStrategy(request.getTargetStrategy());
        profile.setTargetDate(request.getTargetDate());
        profile.setDaysToTarget(resolveDaysToTarget(request));
        profile.setDailyDeficit(targets.dailyDeficit());
        profile.setMaintenanceCalories(targets.maintenanceCalories());
        profile.setCalorieTarget(targets.calories());
        profile.setProteinTarget(targets.protein());
        profile.setCarbTarget(targets.carbs());
        profile.setFatTarget(targets.fat());
        profile.setUpdatedAt(Instant.now());

        return toResponse(profileRepository.save(profile));
    }

    private Targets calculateTargets(ProfileRequest request) {
        double sexAdjustment = "female".equalsIgnoreCase(request.getSex()) ? -161 : 5;
        double bmr = 10 * request.getWeightKg() + 6.25 * request.getHeightCm() - 5 * request.getAge() + sexAdjustment;
        double multiplier = switch (request.getActivityLevel().toLowerCase()) {
            case "light" -> 1.375;
            case "moderate" -> 1.55;
            case "active" -> 1.725;
            case "athlete" -> 1.9;
            default -> 1.2;
        };
        int maintenanceCalories = (int) Math.round(bmr * multiplier);
        int dailyDeficit = calculateDailyDeficit(request);
        int calories = Math.max(1200, maintenanceCalories - dailyDeficit);

        int protein = (int) Math.round(request.getWeightKg() * 1.8);
        int fat = (int) Math.round((calories * 0.25) / 9);
        int carbs = Math.max(0, (int) Math.round((calories - protein * 4 - fat * 9) / 4.0));

        return new Targets(maintenanceCalories, calories, dailyDeficit, protein, carbs, fat);
    }

    private int calculateDailyDeficit(ProfileRequest request) {
        if ("manual".equalsIgnoreCase(request.getTargetStrategy()) && request.getDailyDeficit() != null) {
            return request.getDailyDeficit();
        }

        if (
                "timeline".equalsIgnoreCase(request.getTargetStrategy())
                        && request.getTargetWeightKg() != null
                        && resolveDaysToTarget(request) != null
        ) {
            double weightChangeKg = request.getWeightKg() - request.getTargetWeightKg();
            int deficit = (int) Math.round((weightChangeKg * 7700) / resolveDaysToTarget(request));
            return Math.max(-1000, Math.min(1000, deficit));
        }

        return switch (request.getGoal().toLowerCase()) {
            case "lose" -> 400;
            case "gain" -> -300;
            default -> 0;
        };
    }

    private Integer resolveDaysToTarget(ProfileRequest request) {
        if (request.getTargetDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), request.getTargetDate());
            return days > 0 ? (int) Math.min(days, 730) : null;
        }

        return request.getDaysToTarget();
    }

    private ProfileResponse toResponse(UserProfile profile) {
        return ProfileResponse.builder()
                .age(profile.getAge())
                .sex(profile.getSex())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .targetWeightKg(profile.getTargetWeightKg())
                .activityLevel(profile.getActivityLevel())
                .goal(profile.getGoal())
                .targetStrategy(profile.getTargetStrategy())
                .daysToTarget(profile.getDaysToTarget())
                .targetDate(profile.getTargetDate())
                .dailyDeficit(profile.getDailyDeficit())
                .maintenanceCalories(profile.getMaintenanceCalories())
                .calorieTarget(profile.getCalorieTarget())
                .proteinTarget(profile.getProteinTarget())
                .carbTarget(profile.getCarbTarget())
                .fatTarget(profile.getFatTarget())
                .build();
    }

    private record Targets(
            int maintenanceCalories,
            int calories,
            int dailyDeficit,
            int protein,
            int carbs,
            int fat
    ) {
    }
}
