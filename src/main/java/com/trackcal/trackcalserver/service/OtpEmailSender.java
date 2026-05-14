package com.trackcal.trackcalserver.service;

public interface OtpEmailSender {
    void sendRegistrationOtp(String email, String otp, long expirationMinutes);
}
