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