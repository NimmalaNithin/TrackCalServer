package com.trackcal.trackcalserver.repository;

import com.trackcal.trackcalserver.model.AnalyticsEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsEntryRepository extends MongoRepository<AnalyticsEntry, String> {
    List<AnalyticsEntry> findByUserIdOrderByEntryDateDesc(String userId);

    Optional<AnalyticsEntry> findByUserIdAndEntryDate(String userId, LocalDate entryDate);
}
