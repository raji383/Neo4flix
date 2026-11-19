package com.neo4flix.rating_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.neo4flix.rating_service.dto.response.WatchlistItemDTO;
import com.neo4flix.rating_service.repository.WatchlistRepository;
import com.neo4flix.rating_service.repository.WatchlistQueryRepository;

import lombok.RequiredArgsConstructor;
@Service 
@RequiredArgsConstructor 
public class WatchlistService {
     private final WatchlistRepository repo;
     private final WatchlistQueryRepository queryRepo;

    public void add(String userId, String movieId)    { repo.add(userId, movieId); }
    public void remove(String userId, String movieId) { repo.remove(userId, movieId); }
    public List<WatchlistItemDTO> list(String userId) { return queryRepo.findByUser(userId); }
    public boolean contains(String userId, String movieId) { return repo.isInWatchlist(userId, movieId); }
}
