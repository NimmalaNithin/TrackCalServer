package com.trackcal.trackcalserver.repository;

import com.trackcal.trackcalserver.model.OAuthLoginToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OAuthLoginTokenRepository extends MongoRepository<OAuthLoginToken, String> {
    Optional<OAuthLoginToken> findByCodeHash(String codeHash);
}
