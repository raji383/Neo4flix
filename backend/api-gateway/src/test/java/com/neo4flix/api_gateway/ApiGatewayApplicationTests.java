package com.neo4flix.api_gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

import com.neo4flix.api_gateway.security.Filter;
import com.neo4flix.api_gateway.security.jwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"API_GATEWAY_PORT=0",
		"JWT_SECRET=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
		"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

	private static final String JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

	@Autowired
	private GlobalCorsProperties globalCorsProperties;

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
	}

	@Test
	void corsAllowsConfiguredFrontendPatchPreflight() {
		CorsConfiguration corsConfiguration = globalCorsProperties.getCorsConfigurations().get("/**");
		assertNotNull(corsConfiguration);
		assertEquals("http://localhost:4200", corsConfiguration.checkOrigin("http://localhost:4200"));
		assertTrue(corsConfiguration.getAllowedMethods().contains("PATCH"));

		testClient().options()
				.uri("/api/movies")
				.header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "PATCH")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200")
				.expectHeader().value("Access-Control-Allow-Methods",
						methods -> assertTrue(methods.contains("PATCH")));
	}

	@Test
	void corsHeadersAreIncludedOnLoginPostResponses() {
		testClient().post()
				.uri("/api/auth/login")
				.header("Origin", "http://localhost:4200")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{}")
				.exchange()
				.expectStatus().isEqualTo(503)
				.expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200");
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
