package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.dto.AuthResponse;
import com.trackcal.trackcalserver.dto.GoogleTokenInfoResponse;
import com.trackcal.trackcalserver.dto.GoogleTokenResponse;
import com.trackcal.trackcalserver.dto.LoginRequest;
import com.trackcal.trackcalserver.dto.OAuthExchangeRequest;
import com.trackcal.trackcalserver.dto.OtpResponse;
import com.trackcal.trackcalserver.dto.RegisterRequest;
import com.trackcal.trackcalserver.dto.ResendOtpRequest;
import com.trackcal.trackcalserver.dto.VerifyRegistrationOtpRequest;
import com.trackcal.trackcalserver.exception.EmailAlreadyExistsException;
import com.trackcal.trackcalserver.exception.InvalidCredentialsException;
import com.trackcal.trackcalserver.exception.OtpException;
import com.trackcal.trackcalserver.exception.UserNotFoundException;
import com.trackcal.trackcalserver.model.OAuthLoginToken;
import com.trackcal.trackcalserver.model.PendingRegistration;
import com.trackcal.trackcalserver.model.User;
import com.trackcal.trackcalserver.repository.OAuthLoginTokenRepository;
import com.trackcal.trackcalserver.repository.PendingRegistrationRepository;
import com.trackcal.trackcalserver.repository.UserRepository;
import com.trackcal.trackcalserver.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
public class AuthService {
    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final String LOCAL_PROVIDER = "LOCAL";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final Duration OAUTH_STATE_TTL = Duration.ofMinutes(10);
    private static final Duration OAUTH_LOGIN_CODE_TTL = Duration.ofMinutes(5);
    private static final int OTP_MIN = 100000;
    private static final int OTP_BOUND = 900000;

    private final UserRepository userRepository;
    private final OAuthLoginTokenRepository oauthLoginTokenRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final RestClient restClient;
    private final OtpEmailSender otpEmailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${google.client-secret:}")
    private String googleClientSecret;

    @Value("${google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.registration-otp.expiration-minutes:10}")
    private long registrationOtpExpirationMinutes;

    @Value("${app.registration-otp.resend-cooldown-seconds:60}")
    private long registrationOtpResendCooldownSeconds;

    public AuthService(
            UserRepository userRepository,
            OAuthLoginTokenRepository oauthLoginTokenRepository,
            PendingRegistrationRepository pendingRegistrationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            OtpEmailSender otpEmailSender
    ) {
        this.userRepository = userRepository;
        this.oauthLoginTokenRepository = oauthLoginTokenRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.otpEmailSender = otpEmailSender;
        this.restClient = RestClient.create();
    }

    public OtpResponse requestRegistrationOtp(RegisterRequest request) {
        try {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new EmailAlreadyExistsException("Email already exists");
            }

            String otp = generateOtp();
            Instant now = Instant.now();
            Instant expiresAt = now.plus(Duration.ofMinutes(registrationOtpExpirationMinutes));
            Instant resendAvailableAt = now.plus(Duration.ofSeconds(registrationOtpResendCooldownSeconds));

            PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(normalizedEmail)
                    .orElseGet(PendingRegistration::new);
            if (pendingRegistration.getCreatedAt() == null) {
                pendingRegistration.setCreatedAt(now);
            }
            pendingRegistration.setFirstName(request.getFirstName());
            pendingRegistration.setLastName(request.getLastName());
            pendingRegistration.setEmail(normalizedEmail);
            pendingRegistration.setPassword(passwordEncoder.encode(request.getPassword()));
            pendingRegistration.setOtpHash(hash(otp));
            pendingRegistration.setExpiresAt(expiresAt);
            pendingRegistration.setResendAvailableAt(resendAvailableAt);
            pendingRegistration.setUpdatedAt(now);

            pendingRegistrationRepository.save(pendingRegistration);
            otpEmailSender.sendRegistrationOtp(normalizedEmail, otp, registrationOtpExpirationMinutes);

            return new OtpResponse(
                    normalizedEmail,
                    secondsUntil(expiresAt),
                    secondsUntil(resendAvailableAt)
            );
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException("Email already exists");
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    public OtpResponse resendRegistrationOtp(ResendOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new OtpException("Please submit the registration form again"));

        if (pendingRegistration.getExpiresAt() == null || pendingRegistration.getExpiresAt().isBefore(Instant.now())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new OtpException("OTP expired. Please submit the registration form again");
        }

        if (pendingRegistration.getResendAvailableAt() != null && pendingRegistration.getResendAvailableAt().isAfter(Instant.now())) {
            return new OtpResponse(
                    normalizedEmail,
                    secondsUntil(pendingRegistration.getExpiresAt()),
                    secondsUntil(pendingRegistration.getResendAvailableAt())
            );
        }

        String otp = generateOtp();
        Instant now = Instant.now();
        Instant resendAvailableAt = now.plus(Duration.ofSeconds(registrationOtpResendCooldownSeconds));
        pendingRegistration.setOtpHash(hash(otp));
        pendingRegistration.setResendAvailableAt(resendAvailableAt);
        pendingRegistration.setUpdatedAt(now);
        pendingRegistrationRepository.save(pendingRegistration);
        otpEmailSender.sendRegistrationOtp(normalizedEmail, otp, registrationOtpExpirationMinutes);

        return new OtpResponse(
                normalizedEmail,
                secondsUntil(pendingRegistration.getExpiresAt()),
                secondsUntil(resendAvailableAt)
        );
    }

    public AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new OtpException("OTP expired. Please register again"));

        if (pendingRegistration.getExpiresAt() == null || pendingRegistration.getExpiresAt().isBefore(Instant.now())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new OtpException("OTP expired. Please register again");
        }

        if (!MessageDigest.isEqual(hash(request.getOtp()).getBytes(StandardCharsets.UTF_8), pendingRegistration.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            throw new OtpException("Invalid OTP. Please re-enter the code");
        }

        try {
            if (userRepository.existsByEmail(normalizedEmail)) {
                pendingRegistrationRepository.delete(pendingRegistration);
                throw new EmailAlreadyExistsException("Email already exists");
            }

            User user = User.builder()
                    .firstName(pendingRegistration.getFirstName())
                    .lastName(pendingRegistration.getLastName())
                    .email(normalizedEmail)
                    .password(pendingRegistration.getPassword())
                    .authProvider(LOCAL_PROVIDER)
                    .roles(List.of("ROLE_USER"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User savedUser = userRepository.save(user);
            pendingRegistrationRepository.delete(pendingRegistration);

            String token = jwtService.generateToken(savedUser.getEmail());

            return new AuthResponse(
                    token,
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    null
            );
        } catch (DuplicateKeyException ex) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new EmailAlreadyExistsException("Email already exists");
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            String normalizedEmail = normalizeEmail(request.getEmail());
            User existingUser = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()) {
                throw new InvalidCredentialsException("Use Google to log in to this account");
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(existingUser.getEmail());

            return new AuthResponse(
                    token,
                    existingUser.getEmail(),
                    existingUser.getFirstName(),
                    existingUser.getLastName(),
                    null
            );

        } catch (BadCredentialsException | UserNotFoundException ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (DataAccessException ex) {
            throw ex;
        }
    }

    public URI buildGoogleAuthorizationUri(String state) {
        requireGoogleOAuthConfig();
        return UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .build()
                .toUri();
    }

    public String createOAuthState() {
        String nonce = randomUrlToken(24);
        long issuedAt = Instant.now().toEpochMilli();
        String payload = nonce + "." + issuedAt;
        return payload + "." + sign(payload);
    }

    public boolean isValidOAuthState(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }

        String[] parts = state.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String payload = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            long issuedAt = Long.parseLong(parts[1]);
            return Instant.ofEpochMilli(issuedAt).plus(OAUTH_STATE_TTL).isAfter(Instant.now());
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public String handleGoogleCallback(String code) {
        GoogleTokenResponse googleTokens = exchangeAuthorizationCode(code);
        if (googleTokens == null || googleTokens.getIdToken() == null || googleTokens.getIdToken().isBlank()) {
            throw new InvalidCredentialsException("Invalid Google authorization response");
        }

        GoogleTokenInfoResponse tokenInfo = verifyGoogleCredential(googleTokens.getIdToken());
        String email = normalizeEmail(tokenInfo.getEmail());
        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateGoogleUser(existingUser, tokenInfo))
                .orElseGet(() -> createGoogleUser(tokenInfo));
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail());

        AuthResponse response = new AuthResponse(
                token,
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getProfilePictureUrl()
        );
        String oneTimeCode = randomUrlToken(32);
        oauthLoginTokenRepository.save(OAuthLoginToken.builder()
                .codeHash(hash(oneTimeCode))
                .appToken(response.getToken())
                .email(response.getEmail())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .profilePictureUrl(response.getProfilePictureUrl())
                .expiresAt(Instant.now().plus(OAUTH_LOGIN_CODE_TTL))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        return oneTimeCode;
    }

    public AuthResponse exchangeOAuthCode(OAuthExchangeRequest request) {
        OAuthLoginToken loginToken = oauthLoginTokenRepository.findByCodeHash(hash(request.getCode()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid OAuth login code"));
        oauthLoginTokenRepository.delete(loginToken);

        if (loginToken.getExpiresAt() == null || loginToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("OAuth login code expired");
        }

        return new AuthResponse(
                loginToken.getAppToken(),
                loginToken.getEmail(),
                loginToken.getFirstName(),
                loginToken.getLastName(),
                loginToken.getProfilePictureUrl()
        );
    }

    private GoogleTokenResponse exchangeAuthorizationCode(String code) {
        requireGoogleOAuthConfig();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", googleRedirectUri);
        body.add("grant_type", "authorization_code");

        try {
            return restClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RestClientException ex) {
            throw new InvalidCredentialsException("Unable to exchange Google authorization code");
        }
    }

    private GoogleTokenInfoResponse verifyGoogleCredential(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new InvalidCredentialsException("Google OAuth is not configured");
        }

        try {
            ResponseEntity<GoogleTokenInfoResponse> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={credential}", credential)
                    .retrieve()
                    .toEntity(GoogleTokenInfoResponse.class);
            GoogleTokenInfoResponse tokenInfo = response.getBody();

            if (
                    tokenInfo == null
                            || !Objects.equals(googleClientId, tokenInfo.getAud())
                            || tokenInfo.getEmail() == null
                            || tokenInfo.getEmail().isBlank()
                            || !"true".equalsIgnoreCase(tokenInfo.getEmailVerified())
                            || tokenInfo.getSub() == null
                            || tokenInfo.getSub().isBlank()
            ) {
                throw new InvalidCredentialsException("Invalid Google credential");
            }

            return tokenInfo;
        } catch (RestClientException ex) {
            throw new InvalidCredentialsException("Invalid Google credential");
        }
    }

    private void requireGoogleOAuthConfig() {
        if (
                googleClientId == null || googleClientId.isBlank()
                        || googleClientSecret == null || googleClientSecret.isBlank()
                        || googleRedirectUri == null || googleRedirectUri.isBlank()
        ) {
            throw new InvalidCredentialsException("Google OAuth is not configured");
        }
    }

    private String randomUrlToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOtp() {
        return String.valueOf(OTP_MIN + secureRandom.nextInt(OTP_BOUND));
    }

    private long secondsUntil(Instant instant) {
        if (instant == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), instant).getSeconds());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign OAuth state", ex);
        }
    }

    private String hash(String value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash OAuth login code", ex);
        }
    }

    private User createGoogleUser(GoogleTokenInfoResponse tokenInfo) {
        Instant now = Instant.now();
        return User.builder()
                .firstName(resolveFirstName(tokenInfo))
                .lastName(resolveLastName(tokenInfo))
                .email(normalizeEmail(tokenInfo.getEmail()))
                .authProvider(GOOGLE_PROVIDER)
                .providerId(tokenInfo.getSub())
                .profilePictureUrl(tokenInfo.getPicture())
                .roles(List.of("ROLE_USER"))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private User updateGoogleUser(User user, GoogleTokenInfoResponse tokenInfo) {
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(resolveFirstName(tokenInfo));
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(resolveLastName(tokenInfo));
        }
        user.setAuthProvider(GOOGLE_PROVIDER);
        user.setProviderId(tokenInfo.getSub());
        user.setProfilePictureUrl(tokenInfo.getPicture());
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(List.of("ROLE_USER"));
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        return user;
    }

    private String resolveFirstName(GoogleTokenInfoResponse tokenInfo) {
        if (tokenInfo.getGivenName() != null && !tokenInfo.getGivenName().isBlank()) {
            return tokenInfo.getGivenName();
        }
        return tokenInfo.getName() == null || tokenInfo.getName().isBlank() ? "Google" : tokenInfo.getName().split(" ")[0];
    }

    private String resolveLastName(GoogleTokenInfoResponse tokenInfo) {
        return tokenInfo.getFamilyName() == null || tokenInfo.getFamilyName().isBlank() ? "User" : tokenInfo.getFamilyName();
    }
}
