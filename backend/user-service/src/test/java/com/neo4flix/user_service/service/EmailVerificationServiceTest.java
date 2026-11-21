package com.neo4flix.user_service.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neo4flix.user_service.exception.ApiException;
import com.neo4flix.user_service.model.EmailVerificationChallenge;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.EmailVerificationRepository;
import com.neo4flix.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                emailService);
    }

    @Test
    void validActivationCodeEnablesTwoFactorAndConsumesChallenge() {
        User user = new User();
        user.setId("user-id");

        EmailVerificationChallenge challenge = activationChallenge();
        when(challengeRepository.findById("challenge-id"))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        service.verifyEmail("challenge-id", "123456");

        assertTrue(user.isEmailVerified());
        assertTrue(user.isTwoFactorEnabled());
        assertTrue(challenge.isConsumed());
        verify(userRepository).save(user);
        verify(challengeRepository).save(challenge);
    }

    @Test
    void invalidActivationCodeDoesNotEnableTwoFactor() {
        EmailVerificationChallenge challenge = activationChallenge();
        when(challengeRepository.findById("challenge-id"))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("000000", "otp-hash")).thenReturn(false);

        assertThrows(ApiException.class, () -> service.verifyEmail("challenge-id", "000000"));

        assertFalse(challenge.isConsumed());
        assertTrue(challenge.getAttempts() == 1);
        verify(challengeRepository).save(challenge);
    }

    private EmailVerificationChallenge activationChallenge() {
        EmailVerificationChallenge challenge = new EmailVerificationChallenge();
        challenge.setId("challenge-id");
        challenge.setUserId("user-id");
        challenge.setPurpose("TWO_FACTOR_ACTIVATION");
        challenge.setOtpHash("otp-hash");
        challenge.setExpiresAt(Instant.now().plusSeconds(60));
        challenge.setAttempts(0);
        challenge.setConsumed(false);
        return challenge;
    }
}