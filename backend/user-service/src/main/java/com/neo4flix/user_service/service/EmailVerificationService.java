package com.neo4flix.user_service.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.neo4flix.user_service.exception.ApiException;
import com.neo4flix.user_service.model.EmailVerificationChallenge;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.EmailVerificationRepository;
import com.neo4flix.user_service.repository.UserRepository;

@Service
public class EmailVerificationService {

        private static final String TWO_FACTOR_ACTIVATION = "TWO_FACTOR_ACTIVATION";
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final long OTP_VALIDITY_SECONDS = 900;

    private final EmailVerificationRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository challengeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

        public String createTwoFactorActivationChallenge(User user) {

                if (user.isTwoFactorEnabled()) {
                        throw ApiException.conflict("Two-factor authentication is already enabled");
                }

                return createChallenge(user, TWO_FACTOR_ACTIVATION);
        }

        public void cancelPendingTwoFactorActivation(String userId) {
                List<EmailVerificationChallenge> pendingChallenges = challengeRepository
                                .findAllByUserIdAndConsumedFalse(userId);
                challengeRepository.deleteAll(pendingChallenges);
        }

        private String createChallenge(User user, String purpose) {

        // Invalidate previous unused challenges
        List<EmailVerificationChallenge> oldChallenges = challengeRepository.findAllByUserIdAndConsumedFalse(
                user.getId());

        challengeRepository.deleteAll(oldChallenges);

        // Generate a secure six-digit OTP
        String otp = String.format(
                "%0" + OTP_LENGTH + "d",
                secureRandom.nextInt(1_000_000));

        Instant now = Instant.now();

        EmailVerificationChallenge challenge = new EmailVerificationChallenge();

        challenge.setId(UUID.randomUUID().toString());
        challenge.setUserId(user.getId());
        challenge.setPurpose(purpose);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(
                now.plusSeconds(OTP_VALIDITY_SECONDS));
        challenge.setAttempts(0);
        challenge.setConsumed(false);

        challengeRepository.save(challenge);

        try {
            emailService.sendEmailVerificationOtp(
                    user.getEmail(),
                    user.getUsername(),
                    otp);
        } catch (MailException exception) {

            challengeRepository.delete(challenge);

            throw new IllegalStateException(
                    "Unable to send email verification code.",
                    exception);
        }

        return challenge.getId();
    }

    public void verifyEmail(String challengeId, String otp) {

        EmailVerificationChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> ApiException.unauthorized(
                        "Invalid or expired verification code"));

        if (challenge.isConsumed()
                || !Instant.now().isBefore(challenge.getExpiresAt())
                || challenge.getAttempts() >= MAX_ATTEMPTS) {

            challenge.setConsumed(true);
            challengeRepository.save(challenge);

            throw ApiException.unauthorized(
                    "Invalid or expired verification code");
        }

        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {

            challenge.setAttempts(challenge.getAttempts() + 1);

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
            }

            challengeRepository.save(challenge);

            throw ApiException.unauthorized(
                    "Invalid or expired verification code");
        }

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        // Mark the email as verified
        user.setEmailVerified(true);
        if (TWO_FACTOR_ACTIVATION.equals(challenge.getPurpose())) {
            user.setTwoFactorEnabled(true);
        }
        userRepository.save(user);

        // OTP is single-use
        challenge.setConsumed(true);
        challengeRepository.save(challenge);
    }
}