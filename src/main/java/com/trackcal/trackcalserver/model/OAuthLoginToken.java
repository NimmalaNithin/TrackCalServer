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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "oauth_login_tokens")
public class OAuthLoginToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String codeHash;

    private String appToken;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private Instant expiresAt;
}
