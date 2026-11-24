package com.neo4flix.user_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "JWT_SECRET=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "eureka.client.enabled=false",
        "spring.neo4j.authentication.username=test",
        "spring.neo4j.authentication.password=test"
})
@AutoConfigureMockMvc
class UserServiceIntegrationTest {

    private static final String VALID_REGISTRATION = """
            {"username":"azraji","password":"StrongPass1!","confirmPassword":"StrongPass1!","email":"azraji@example.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void registrationEndpointReturnsCreatedUserAndToken() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("azraji"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.jwtToken").doesNotExist());
    }

    @Test
    void registrationEndpointRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"azraji","password":"StrongPass1!","confirmPassword":"StrongPass1!","email":"invalid-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("Email should be valid"));
    }

    @Test
    void loginEndpointReturnsTokenForValidCredentials() throws Exception {
        User user = new User();
        user.setId("user-id");
        user.setUsername("azraji");
        user.setEmail("azraji@example.com");
        user.setRole("ADMIN");
        user.setPasswordHash(passwordEncoder.encode("StrongPass1!"));
        when(userRepository.findByUsername("azraji")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"azraji","password":"StrongPass1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("azraji"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.jwtToken").isNotEmpty());
    }

    @Test
    void profileEndpointUsesAuthenticatedGatewayUserId() throws Exception {
        User user = new User();
        user.setId("user-id");
        user.setUsername("azraji");
        user.setEmail("azraji@example.com");
        user.setRole("ADMIN");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header("X-Authenticated-User-Id", "user-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("azraji"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
