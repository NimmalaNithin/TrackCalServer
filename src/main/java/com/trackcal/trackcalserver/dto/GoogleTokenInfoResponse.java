package com.trackcal.trackcalserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleTokenInfoResponse {
    private String aud;
    private String sub;
    private String email;
    private String name;
    private String picture;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("email_verified")
    private String emailVerified;
}
