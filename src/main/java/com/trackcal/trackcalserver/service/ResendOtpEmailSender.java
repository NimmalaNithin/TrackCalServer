package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.exception.OtpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendOtpEmailSender implements OtpEmailSender {
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;

    public ResendOtpEmailSender(
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.from:}") String fromAddress
    ) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendRegistrationOtp(String email, String otp, long expirationMinutes) {
        if (apiKey == null || apiKey.isBlank() || fromAddress == null || fromAddress.isBlank()) {
            throw new OtpException("Email API is not configured");
        }

        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to", List.of(email),
                "subject", "Your Track Cal verification code",
                "text", buildPlainTextMessage(otp, expirationMinutes)
        );

        try {
            restClient.post()
                    .uri(RESEND_EMAILS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new OtpException("Unable to send OTP. Please try again later");
        }
    }

    private String buildPlainTextMessage(String otp, long expirationMinutes) {
        return """
                Your Track Cal verification code is %s.

                This code expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(otp, expirationMinutes);
    }
}
