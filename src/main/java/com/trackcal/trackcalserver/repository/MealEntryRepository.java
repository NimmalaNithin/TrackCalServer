package com.trackcal.trackcalserver.repository;

import com.trackcal.trackcalserver.model.MealEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealEntryRepository extends MongoRepository<MealEntry, String> {
    List<MealEntry> findByUserIdAndEntryDate(String userId, String entryDate);
    List<MealEntry> findByUserId(String userId);
}
