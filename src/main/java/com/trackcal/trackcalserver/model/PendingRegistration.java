package com.trackcal.trackcalserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "pending_registrations")
public class PendingRegistration {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private String password;

    private String otpHash;

    private Instant resendAvailableAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant createdAt;

    private Instant updatedAt;
}
