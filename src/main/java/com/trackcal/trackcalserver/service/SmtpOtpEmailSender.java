package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.exception.OtpException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpOtpEmailSender implements OtpEmailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailFromAddress;

    public SmtpOtpEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String mailFromAddress
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailFromAddress = mailFromAddress;
    }

    @Override
    public void sendRegistrationOtp(String email, String otp, long expirationMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailFromAddress == null || mailFromAddress.isBlank()) {
            throw new OtpException("Email OTP is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(email);
        message.setSubject("Your Track Cal verification code");
        message.setText(buildPlainTextMessage(otp, expirationMinutes));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
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
