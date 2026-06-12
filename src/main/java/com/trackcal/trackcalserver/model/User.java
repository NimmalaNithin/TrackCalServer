package com.trackcal.trackcalserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private String password;

    private String authProvider;

    private String providerId;

    private String profilePictureUrl;

    private List<String> roles;

    private Instant createdAt;

    private Instant updatedAt;
}
