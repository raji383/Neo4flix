package com.neo4flix.api_gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"API_GATEWAY_PORT=0",
		"JWT_SECRET=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
		"server.ssl.enabled=false",
		"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

	@Autowired
	private GlobalCorsProperties globalCorsProperties;

	@LocalServerPort
	private int port;

	@Test
	void corsAllowsConfiguredFrontendPatchPreflight() {
		CorsConfiguration corsConfiguration = globalCorsProperties.getCorsConfigurations().get("/**");
		assertNotNull(corsConfiguration);
		assertEquals("https://localhost:4200", corsConfiguration.checkOrigin("https://localhost:4200"));
		assertNull(corsConfiguration.checkOrigin("http://localhost:4200"));
		assertTrue(corsConfiguration.getAllowedMethods().contains("PATCH"));

		testClient().options()
				.uri("/api/movies")
				.header("Origin", "https://localhost:4200")
				.header("Access-Control-Request-Method", "PATCH")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("Access-Control-Allow-Origin", "https://localhost:4200")
				.expectHeader().value("Access-Control-Allow-Methods",
						methods -> assertTrue(methods.contains("PATCH")));
	}

	@Test
	void corsHeadersAreIncludedOnLoginPostResponses() {
		testClient().post()
				.uri("/api/auth/login")
				.header("Origin", "https://localhost:4200")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{}")
				.exchange()
				.expectStatus().isEqualTo(503)
				.expectHeader().valueEquals("Access-Control-Allow-Origin", "https://localhost:4200");
	}

	@Test
	void protectedRequestWithoutTokenReturnsUnauthorized() {
		testClient().get()
				.uri("/api/movies")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.code").isEqualTo("UNAUTHORIZED");
	}

	@Test
	void twoFactorSettingsRequireAuthentication() {
		testClient().post()
				.uri("/api/auth/2fa/enable")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	private WebTestClient testClient() {
		return WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
	}

	@Test
	void gatewayUsesJwtSubjectAsAuthenticatedUserId() {
		String token = Jwts.builder()
				.setSubject("user-123")
				.claim("role", "USER")
				.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
				.compact();
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/movies")
						.header("Authorization", "Bearer " + token)
						.build());
		AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
		Filter filter = new Filter(new jwtUtil(JWT_SECRET));

		filter.filter(exchange, forwarded -> {
			forwardedExchange.set(forwarded);
			return Mono.empty();
		}).block();

		assertNotNull(forwardedExchange.get());
		assertEquals("user-123", forwardedExchange.get().getRequest()
				.getHeaders().getFirst("X-Authenticated-UserID"));
	}
}
