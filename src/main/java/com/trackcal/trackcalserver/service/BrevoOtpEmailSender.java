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
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
public class BrevoOtpEmailSender implements OtpEmailSender {
    private static final String BREVO_EMAILS_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoOtpEmailSender(
            @Value("${app.email.brevo.api-key:}") String apiKey,
            @Value("${app.email.brevo.from-email:}") String fromEmail,
            @Value("${app.email.brevo.from-name:Track Cal}") String fromName
    ) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void sendRegistrationOtp(String email, String otp, long expirationMinutes) {
        if (apiKey == null || apiKey.isBlank() || fromEmail == null || fromEmail.isBlank()) {
            throw new OtpException("Email API is not configured");
        }

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", fromName,
                        "email", fromEmail
                ),
                "to", List.of(Map.of("email", email)),
                "subject", "Your Track Cal verification code",
                "htmlContent", buildHtmlMessage(otp, expirationMinutes),
                "textContent", buildPlainTextMessage(otp, expirationMinutes)
        );

        try {
            restClient.post()
                    .uri(BREVO_EMAILS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new OtpException("Unable to send OTP. Please try again later");
        }
    }

    private String buildHtmlMessage(String otp, long expirationMinutes) {
        return """
                <p>Your Track Cal verification code is <strong>%s</strong>.</p>
                <p>This code expires in %d minutes. If you did not request this, you can ignore this email.</p>
                """.formatted(otp, expirationMinutes);
    }

    private String buildPlainTextMessage(String otp, long expirationMinutes) {
        return """
                Your Track Cal verification code is %s.

                This code expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(otp, expirationMinutes);
    }
}
