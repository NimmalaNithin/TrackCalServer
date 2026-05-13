package com.trackcal.trackcalserver.controller;


import com.trackcal.trackcalserver.dto.AuthResponse;
import com.trackcal.trackcalserver.dto.LoginRequest;
import com.trackcal.trackcalserver.dto.OAuthExchangeRequest;
import com.trackcal.trackcalserver.dto.OtpResponse;
import com.trackcal.trackcalserver.dto.RegisterRequest;
import com.trackcal.trackcalserver.dto.ResendOtpRequest;
import com.trackcal.trackcalserver.dto.VerifyRegistrationOtpRequest;
import com.trackcal.trackcalserver.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String OAUTH_STATE_COOKIE = "trackcal_oauth_state";

    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/request-otp")
    public OtpResponse requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return authService.requestRegistrationOtp(request);
    }

    @PostMapping("/register/resend-otp")
    public OtpResponse resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendRegistrationOtp(request);
    }

    @PostMapping("/register/verify-otp")
    public AuthResponse verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequest request) {
        return authService.verifyRegistrationOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/google/authorize")
    public ResponseEntity<Void> googleAuthorize() {
        String state = authService.createOAuthState();
        ResponseCookie stateCookie = ResponseCookie.from(OAUTH_STATE_COOKIE, state)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth/google")
                .maxAge(Duration.ofMinutes(10))
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .location(authService.buildGoogleAuthorizationUri(state))
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = OAUTH_STATE_COOKIE, required = false) String stateCookie
    ) {
        ResponseCookie clearStateCookie = ResponseCookie.from(OAUTH_STATE_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth/google")
                .maxAge(0)
                .build();

        if (error != null || code == null || state == null || stateCookie == null || !state.equals(stateCookie) || !authService.isValidOAuthState(state)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                    .location(URI.create(frontendUrl + "/oauth/callback?error=google_auth_failed"))
                    .build();
        }

        String oneTimeCode = authService.handleGoogleCallback(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .location(URI.create(frontendUrl + "/oauth/callback?code=" + oneTimeCode))
                .build();
    }

    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeOAuthCode(@Valid @RequestBody OAuthExchangeRequest request) {
        return authService.exchangeOAuthCode(request);
    }

}
