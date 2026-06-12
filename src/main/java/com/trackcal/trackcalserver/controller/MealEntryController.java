package com.trackcal.trackcalserver.controller;

import com.trackcal.trackcalserver.dto.MealEntryRequest;
import com.trackcal.trackcalserver.dto.MealEntryResponse;
import com.trackcal.trackcalserver.service.MealEntryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meals")
public class MealEntryController {
    private final MealEntryService mealEntryService;

    public MealEntryController(MealEntryService mealEntryService) {
        this.mealEntryService = mealEntryService;
    }

    @GetMapping
    public List<MealEntryResponse> getMeals(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return mealEntryService.getMeals(authentication.getName(), date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealEntryResponse addMeal(
            Authentication authentication,
            @Valid @RequestBody MealEntryRequest request
    ) {
        return mealEntryService.addMeal(authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeal(Authentication authentication, @PathVariable String id) {
        mealEntryService.deleteMeal(authentication.getName(), id);
    }
}
