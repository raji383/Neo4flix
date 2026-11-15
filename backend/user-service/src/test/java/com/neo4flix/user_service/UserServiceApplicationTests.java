package com.neo4flix.user_service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
				.andExpect(jsonPath("$.email").value("azraji@example.com"));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertTrue(passwordEncoder.matches("123456", userCaptor.getValue().getPasswordHash()));
	}

}
