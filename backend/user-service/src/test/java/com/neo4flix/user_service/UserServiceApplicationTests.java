package com.neo4flix.user_service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTests {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@MockitoBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void invalidEmailReturnsValidationMessage() throws Exception {
		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456","email":"raji.com"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details").value("Email should be valid"));
	}

	@Test
	void registrationRejectsExistingUsername() throws Exception {
		when(userRepository.existsByUsername("azraji")).thenReturn(true);

		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456","email":"azraji@example.com"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details").value("Username already exists"));
	}

	@Test
	void registrationRejectsExistingEmail() throws Exception {
		when(userRepository.existsByEmail("azraji@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456","email":"azraji@example.com"}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details").value("Email already exists"));
	}

	@Test
	void registrationPersistsUserWithHashedPassword() throws Exception {
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456","email":"azraji@example.com"}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.username").value("azraji"))
				.andExpect(jsonPath("$.email").value("azraji@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.jwtToken").isNotEmpty());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertTrue(passwordEncoder.matches("123456", userCaptor.getValue().getPasswordHash()));
		assertEquals("USER", userCaptor.getValue().getRole());
	}

	

	@Test
	void loginReturnsJwtForValidCredentials() throws Exception {
		User user = new User();
		user.setId("user-id");
		user.setUsername("azraji");
		user.setEmail("azraji@example.com");
		user.setRole("ADMIN");
		user.setPasswordHash(passwordEncoder.encode("123456"));
		when(userRepository.findByUsername("azraji")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("azraji"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.jwtToken").isNotEmpty());
	}

	@Test
	void profileReturnsStoredRole() throws Exception {
		User user = new User();
		user.setId("user-id");
		user.setUsername("azraji");
		user.setEmail("azraji@example.com");
		user.setRole("ADMIN");
		when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

		mockMvc.perform(get("/api/users/me")
					.header("X-Authenticated-User-Id", "user-id"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void loginRejectsUnknownUsername() throws Exception {
		when(userRepository.findByUsername("azraji")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456"}
							"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.details").value("Invalid username or password"));
	}

	@Test
	void loginRejectsIncorrectPassword() throws Exception {
		User user = new User();
		user.setId("user-id");
		user.setUsername("azraji");
		user.setEmail("azraji@example.com");
		user.setPasswordHash(passwordEncoder.encode("different-password"));
		when(userRepository.findByUsername("azraji")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username":"azraji","password":"123456"}
							"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.details").value("Invalid username or password"));
	}

}
