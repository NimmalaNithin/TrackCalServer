package com.trackcal.trackcalserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyRegistrationOtpRequest {
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "\\d{6}", message = "OTP must be a 6 digit code")
    @NotBlank(message = "OTP is required")
    private String otp;
}
