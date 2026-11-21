package com.neo4flix.rating_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;

import com.neo4flix.rating_service.dto.response.WatchlistItemDTO;
import com.neo4flix.rating_service.repository.WatchlistQueryRepository;
import com.neo4flix.rating_service.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private WatchlistQueryRepository watchlistQueryRepository;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistRepository, watchlistQueryRepository);
    }

    @Test
    void addDelegatesUserAndMovieToRepository() {
        watchlistService.add("user-123", "movie-123");

        verify(watchlistRepository).add("user-123", "movie-123");
    }

    @Test
    void removeDelegatesUserAndMovieToRepository() {
        watchlistService.remove("user-123", "movie-123");

        verify(watchlistRepository).remove("user-123", "movie-123");
    }

    @Test
    void listReturnsUsersWatchlistItems() {
        List<WatchlistItemDTO> items = List.of(
                new WatchlistItemDTO(
                        "movie-123",
                        "Example Film",
                        "https://example.test/poster.png",
                        2020,
                        ZonedDateTime.parse("2026-01-01T10:00:00Z")));
        when(watchlistQueryRepository.findByUser("user-123")).thenReturn(items);

        assertEquals(items, watchlistService.list("user-123"));
    }

    @Test
    void containsReturnsRepositoryMembership() {
        when(watchlistRepository.isInWatchlist("user-123", "movie-123")).thenReturn(true);

        assertTrue(watchlistService.contains("user-123", "movie-123"));
    }
}
