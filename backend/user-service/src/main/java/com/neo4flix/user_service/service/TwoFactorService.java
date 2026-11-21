
package com.neo4flix.user_service.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.neo4flix.user_service.exception.ApiException;
import com.neo4flix.user_service.model.LoginChallenge;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.LoginChallengeRepository;

@Service
public class TwoFactorService {

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final long OTP_VALIDITY_SECONDS = 300;

    private final LoginChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorService(
            LoginChallengeRepository challengeRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public String createLoginChallenge(User user) {

        // Generate a cryptographically secure six-digit OTP
        String otp = String.format(
                "%0" + OTP_LENGTH + "d",
                secureRandom.nextInt(1_000_000)
        );

        LoginChallenge challenge = new LoginChallenge();

        challenge.setId(UUID.randomUUID().toString());
        challenge.setUserId(user.getId());
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setCreatedAt(Instant.now());
        challenge.setExpiresAt(
                Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)
        );
        challenge.setAttempts(0);
        challenge.setConsumed(false);

        challengeRepository.save(challenge);

        try {
            emailService.sendLoginOtp(
                    user.getEmail(),
                    user.getUsername(),
                    otp
            );
        } catch (MailException exception) {
            // Do not leave a usable challenge if email delivery fails
            challengeRepository.delete(challenge);
            throw new IllegalStateException(
                    "Unable to send verification email.",
                    exception
            );
        }

        return challenge.getId();
    }

    public String verifyLoginChallenge(String challengeId, String otp) {

        LoginChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() ->
                        ApiException.unauthorized("Invalid or expired verification code")
                );

        if (challenge.isConsumed()
                || !Instant.now().isBefore(challenge.getExpiresAt())
                || challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);

            throw ApiException.unauthorized(
                    "Invalid or expired verification code"
            );
        }

        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {

            challenge.setAttempts(challenge.getAttempts() + 1);

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
            }

            challengeRepository.save(challenge);

            throw ApiException.unauthorized(
                    "Invalid or expired verification code"
            );
        }

        // OTP is single-use
        challenge.setConsumed(true);
        challengeRepository.save(challenge);

        return challenge.getUserId();
    }
}
