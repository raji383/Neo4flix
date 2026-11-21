package com.neo4flix.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neo4flix.user_service.dto.RegisterRequest;
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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, JWT_SECRET, 3_600_000L);
    }

    @Test
    void registerStoresHashedPasswordAndReturnsJwtWithUserIdentity() {
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

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
                .build()
                .parseSignedClaims(response.getJwtToken())
                .getPayload();
        assertEquals(storedUser.getId(), claims.getSubject());
        assertEquals("USER", claims.get("role"));
    }
}
