```
MATCH (current:User {id: $userId})
      -[currentRating:RATED]->(currentMovie:Movie)

WHERE currentRating.value >= $minRating

WITH current, collect(DISTINCT currentMovie) AS currentLiked

MATCH (other:User)-[otherRating:RATED]->(otherMovie:Movie)

WHERE other.id <> current.id
  AND otherRating.value >= $minRating

WITH currentLiked, other,
     collect(DISTINCT otherMovie) AS otherLiked

WITH other, currentLiked, otherLiked,
     size([m IN currentLiked WHERE m IN otherLiked])
     AS commonMovies

WITH other, commonMovies,
     size(currentLiked) + size(otherLiked) - commonMovies
     AS totalMovies

WHERE commonMovies > 0

RETURN
    other.id AS userId,
    commonMovies,
    toFloat(commonMovies) / totalMovies AS similarity

ORDER BY similarity DESC, commonMovies DESC
LIMIT $limit
```

```
MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie)
      -[:HAS_GENRE]->(g:Genre)

WITH g,
     sum(CASE
         WHEN r.value >= 4 THEN r.value
         ELSE 0
     END) AS positiveScore,
     sum(CASE
         WHEN r.value <= 2 THEN 1
         ELSE 0
     END) AS dislikes

RETURN
    g.name AS genre,
    positiveScore,
    dislikes

ORDER BY positiveScore DESC
```

```
MATCH (current:User {id: $userId})

MATCH (similar:User)-[similarRating:RATED]->(candidate:Movie)

WHERE similar.id IN $similarUserIds
  AND similarRating.value >= $minRating

AND NOT EXISTS {
    MATCH (current)-[:RATED]->(candidate)
}

RETURN
    candidate,
    count(DISTINCT similar) AS recommendedByUsers,
    avg(similarRating.value) AS averageRating

ORDER BY recommendedByUsers DESC, averageRating DESC

LIMIT $limit
```

```

  movies: RecommendedMovie[] = [
    {
      id: '1',
      title: 'Interstellar',
      year: 2014,
      genres: ['Sci-Fi', 'Drama'],
      imageUrl: 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg',
      rating: 8.7,
      recommendationScore: 98,
      description: 'A journey beyond the stars to save humanity.'
    },
    {
      id: '2',
      title: 'Inception',
      year: 2010,
      genres: ['Action', 'Sci-Fi'],
      imageUrl: 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
      rating: 8.8,
      recommendationScore: 95,
      description: 'Enter the world where dreams become reality.'
    },
    {
      id: '3',
      title: 'The Dark Knight',
      year: 2008,
      genres: ['Action', 'Crime'],
      imageUrl: 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg',
      rating: 9.0,
      recommendationScore: 93,
      description: 'A hero faces his greatest challenge.'
    },
    {
      id: '4',
      title: 'Dune: Part Two',
      year: 2024,
      genres: ['Sci-Fi', 'Adventure'],
      imageUrl: 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg',
      rating: 8.5,
      recommendationScore: 91,
      description: 'The journey to control the destiny of Arrakis.'
    }
  ];
```