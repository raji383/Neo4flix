package com.neo4flix.rating_service.dto.response;

import java.time.ZonedDateTime;

public record WatchlistItemDTO(String id, String title, String imageUrl,
                               Integer releaseYear, ZonedDateTime addedAt) {}