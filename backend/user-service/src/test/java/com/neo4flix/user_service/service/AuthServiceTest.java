package com.neo4flix.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neo4flix.user_service.dto.request.LoginRequest;
import com.neo4flix.user_service.dto.request.RegisterRequest;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                twoFactorService,
                emailVerificationService,
                JWT_SECRET,
                3_600_000L);
    }

    @Test
    void registerStoresHashedPasswordAndReturnsRegistrationConfirmation() {
        when(userRepository.existsByUsername("azraji")).thenReturn(false);
        when(userRepository.existsByEmail("azraji@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RegisterRequest request = new RegisterRequest();
        request.setUsername("azraji");
        request.setEmail("azraji@example.com");
        request.setPassword("StrongPass1!");
        request.setConfirmPassword("StrongPass1!");

        var response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User storedUser = userCaptor.getValue();
        assertEquals("hashed-password", storedUser.getPasswordHash());
        assertNotEquals(request.getPassword(), storedUser.getPasswordHash());
        assertEquals(storedUser.getId(), response.getId());
        assertEquals("USER", response.getRole());
        assertEquals("Registration successful. You can now sign in.", response.getMessage());
    }

    @Test
    void loginReturnsJwtWithUserIdentityWhenTwoFactorIsDisabled() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("azraji");
        user.setEmail("azraji@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole("USER");

        when(userRepository.findByUsername("azraji")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1!", "hashed-password")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsername("azraji");
        request.setPassword("StrongPass1!");

        var response = authService.login(request);

        assertFalse(response.isRequiresTwoFactor());
        assertNotNull(response.getJwtToken());

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
                .build()
                .parseSignedClaims(response.getJwtToken())
                .getPayload();
        assertEquals(user.getId(), claims.getSubject());
        assertEquals("USER", claims.get("role"));
    }
}
