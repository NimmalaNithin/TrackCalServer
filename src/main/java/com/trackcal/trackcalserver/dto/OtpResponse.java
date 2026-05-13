package com.trackcal.trackcalserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpResponse {
    private String email;
    private long expiresInSeconds;
    private long resendAvailableInSeconds;
}
