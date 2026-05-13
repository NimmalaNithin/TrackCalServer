package com.trackcal.trackcalserver.repository;

import com.trackcal.trackcalserver.model.PendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {
    Optional<PendingRegistration> findByEmail(String email);
    void deleteByEmail(String email);
}
