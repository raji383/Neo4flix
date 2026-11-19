package com.neo4flix.movie_service.config.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class TmdbClientTest {

    @Test
    void discoverMoviesDeserializesJsonResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/discover/movie", exchange -> {
            byte[] response = """
                    {"results":[{"id":42}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            TmdbClient client = new TmdbClient(
                    "http://localhost:" + server.getAddress().getPort(),
                    "test-token"
            );

            JsonNode result = client.discoverMovies(1);

            assertEquals(42, result.path("results").get(0).path("id").asInt());
        } finally {
            server.stop(0);
        }
    }
}
