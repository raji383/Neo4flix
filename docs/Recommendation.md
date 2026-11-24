## Hybrid Recommendation System

Neo4flix uses a **custom Hybrid Recommendation System** to recommend movies based on three signals:

1. **Collaborative Filtering (45%)** – Finds users with similar movie-rating behavior and uses their ratings to predict which movies the current user may like.
2. **Genre-Based Recommendation (35%)** – Analyzes the genres of movies the user has rated and calculates their preferences for each genre.
3. **Global Movie Rating (20%)** – Considers the average rating of each movie from all users.

The recommendation algorithm is implemented using **Java and Cypher queries** with Neo4j. It does not rely on Neo4j Graph Data Science (GDS) or an external recommendation library.

### 1. How the Recommendation System Works

The recommendation process follows these steps:

1. Retrieve the current user's ratings.
2. Find other users who rated the same movies.
3. Calculate similarity between users using the Pearson Correlation Coefficient.
4. Select similar users with positive similarity and at least 3 movies rated in common.
5. Predict ratings for movies that similar users have rated but the current user has not.
6. Calculate the user's genre preferences.
7. Combine the available recommendation signals into a hybrid score.
8. Sort the movies by their hybrid score and return the top 10 recommendations.

### 2. Collaborative Filtering

Collaborative Filtering recommends movies based on the preferences of users with similar rating behavior.

#### 2.1 Find Common Ratings

The first Cypher query finds movies rated by both the current user and other users.

```cypher
MATCH (currentUser:User {id: $userId})
      -[r1:RATED]->(movie:Movie)

MATCH (otherUser:User)-[r2:RATED]->(movie)

WHERE otherUser <> currentUser

RETURN
    otherUser.id AS userId,
    otherUser.username AS username,
    movie.id AS movieId,
    r1.value AS currentRating,
    r2.value AS otherRating
```

**Explanation:**

- `MATCH` finds the current user and their rated movies.
- The second `MATCH` finds other users who rated those same movies.
- `WHERE otherUser <> currentUser` excludes the current user.
- `r1.value` represents the current user's rating.
- `r2.value` represents the other user's rating.

**Example:**

Imagine the following ratings:

| Movie | Current User | User A | User B |
|---|---:|---:|---:|
| Inception | 5 | 5 | 2 |
| Interstellar | 4 | 4 | 3 |
| The Matrix | 5 | 5 | 1 |
| Avatar | 3 | 3 | 5 |

The query identifies the common movies and returns the rating pairs needed to calculate user similarity.

#### 2.2 Calculate User Similarity Using Pearson Correlation

After retrieving common ratings, Java calculates the similarity between users using the **Pearson Correlation Coefficient**.

**Pearson similarity formula**

```math
r_{xy}
=
\frac{
\displaystyle\sum_{i=1}^{n}(x_i-\bar{x})(y_i-\bar{y})
}{
\sqrt{
\displaystyle\sum_{i=1}^{n}(x_i-\bar{x})^2
\displaystyle\sum_{i=1}^{n}(y_i-\bar{y})^2
}
}
```

Where:

- \(x_i\): Current user's rating.
- \(y_i\): Other user's rating.
- \(\bar{x}\): Average rating of the current user on common movies.
- \(\bar{y}\): Average rating of the other user on common movies.

**How to interpret the result:**

| Pearson Score | Meaning |
|---|---|
| Close to +1 | Users have similar rating behavior |
| Close to 0 | No strong linear relationship |
| Close to -1 | Users tend to rate movies differently |

The implementation also applies these conditions:

- Similarity must be positive.
- Users must have at least 3 common rated movies.

This avoids relying on users with very few shared ratings.

#### 2.3 Find Candidate Movies From Similar Users

Once similar users have been identified, the following query retrieves movies they rated that the current user has not rated yet.

```cypher
MATCH (currentUser:User {id: $userId})
MATCH (similarUser:User)-[r:RATED]->(movie:Movie)

WHERE similarUser.id IN $similarUserIds
  AND NOT EXISTS {
      MATCH (currentUser)-[:RATED]->(movie)
  }

RETURN
    movie.id AS movieId,
    movie.title AS title,
    similarUser.id AS userId,
    r.value AS rating
```

**Explanation:**

- `$userId` is the current user's ID.
- `$similarUserIds` contains the IDs of users selected by the similarity algorithm.
- `similarUser` represents one of those similar users.
- `r.value` is the rating given by that similar user.
- `NOT EXISTS` excludes movies already rated by the current user.

**Example:**

Suppose User A and User B are similar to the current user:

| Movie | User A Rating | User B Rating | Already Rated by Current User? |
|---|---:|---:|---|
| Dune | 5 | 4 | No |
| Joker | 4 | 5 | No |
| Titanic | 3 | 2 | Yes |

The query returns Dune and Joker, but excludes Titanic.

#### 2.4 Predict a Movie Rating

Java calculates a predicted rating using the similarity scores of the users who rated the movie.

**Weighted predicted rating**

```math
\operatorname{PredictedRating}(m)
=
\frac{
\displaystyle\sum_{u \in U_m}
\operatorname{similarity}(u)\times\operatorname{rating}(u,m)
}{
\displaystyle\sum_{u \in U_m}\operatorname{similarity}(u)
}
```

Where:

- \(U_m\) is the set of similar users who rated movie \(m\).
- \(similarity(u)\) is the Pearson similarity score.
- \(rating(u,m)\) is the user's rating for that movie.

**Example:**

For a movie rated by two similar users:

| User | Similarity | Movie Rating |
|---|---:|---:|
| User A | 0.90 | 5 |
| User B | 0.70 | 4 |

```math
\begin{aligned}
\operatorname{PredictedRating}
&=
\frac{
(0.90\times5)+(0.70\times4)
}{
0.90+0.70
}\\
&=
\frac{7.30}{1.60}
\approx 4.56
\end{aligned}
```

The predicted rating is approximately **4.56/5**.

### 3. Genre-Based Recommendation

Genre-based recommendation analyzes the genres of movies the current user has rated.

The idea is simple: if a user consistently gives high ratings to Science Fiction movies, the system should prioritize other movies belonging to that genre.

#### 3.1 Calculate Genre Preferences

The preference for a genre is the user's average rating for movies in that
genre, normalized to the range from 0 to 1:

```math
\operatorname{Preference}(g)
=
\frac{
\displaystyle\operatorname{avg}
\left(
\{\,r(u,m)\mid m\in M_g\,\}
\right)
}{5}
=
\frac{\displaystyle\sum_{m\in M_g}r(u,m)}
{5\left|M_g\right|}
```

Where \(M_g\) is the set of movies rated by the user that belong to genre
\(g\), and \(r(u,m)\) is the user's rating for movie \(m\).

For a candidate movie that belongs to several genres, the final genre score
is the average of those genre preferences:

```math
\operatorname{GenreScore}(m)
=
\operatorname{avg}
\left(
\{\,\operatorname{Preference}(g)\mid g\in G_m\,\}
\right)
```

Where \(G_m\) is the set of genres associated with movie \(m\).

```cypher
MATCH (u:User {id: $userId})-[r:RATED]->(ratedMovie:Movie)
      -[:HAS_GENRE]->(g:Genre)

WITH u, g, avg(r.value) / 5.0 AS preferenceScore

MATCH (candidate:Movie)-[:HAS_GENRE]->(g)

WHERE NOT EXISTS {
    MATCH (u)-[:RATED]->(candidate)
}

WITH candidate, avg(preferenceScore) AS genreScore

RETURN
    candidate.id AS movieId,
    candidate.title AS title,
    genreScore
ORDER BY genreScore DESC
```

**Explanation step by step:**

**Step 1 – Find the user's rated movies and their genres**

```cypher
MATCH (u:User {id: $userId})-[r:RATED]->(ratedMovie:Movie)
      -[:HAS_GENRE]->(g:Genre)
```

This finds all movies rated by the user and the genres connected to those movies.

For example:

- Inception → Science Fiction
- Interstellar → Science Fiction
- The Dark Knight → Action

**Step 2 – Calculate the preference score for each genre**

```cypher
WITH u, g, avg(r.value) / 5.0 AS preferenceScore
```

The system calculates the average rating given by the user to movies belonging to each genre.

The result is divided by 5 to normalize it between 0 and 1.

**Example:**

| Genre | User Ratings | Average | Preference Score |
|---|---|---:|---:|
| Science Fiction | 5, 4, 5 | 4.67 | 0.93 |
| Action | 4, 3 | 3.50 | 0.70 |
| Drama | 2, 3 | 2.50 | 0.50 |

**Step 3 – Find candidate movies belonging to preferred genres**

```cypher
MATCH (candidate:Movie)-[:HAS_GENRE]->(g:Genre)
```

This finds movies connected to the genres the user has previously rated.

**Step 4 – Exclude movies already rated by the user**

```cypher
WHERE NOT EXISTS {
    MATCH (u)-[:RATED]->(candidate)
}
```

The system avoids recommending movies the user has already rated.

**Step 5 – Calculate the final genre score**

```cypher
WITH candidate, avg(preferenceScore) AS genreScore
```

If a movie belongs to multiple genres, the system calculates the average preference score across those genres.

Finally, movies are ordered by their genre score in descending order.

### 4. Retrieve Available Movies and Global Ratings

The system also uses a query to retrieve movies that the current user has not rated, along with their genres and global rating information.

```cypher
MATCH (movie:Movie)
WHERE NOT EXISTS {
    MATCH (:User {id: $userId})-[:RATED]->(movie)
}

OPTIONAL MATCH (movie)-[:HAS_GENRE]->(genre:Genre)
OPTIONAL MATCH (:User)-[r:RATED]->(movie)

RETURN
    movie.id AS movieId,
    movie.title AS title,
    coalesce(movie.director, '') AS director,
    coalesce(movie.description, '') AS description,
    coalesce(movie.releaseYear, 0) AS releaseYear,
    coalesce(collect(DISTINCT genre.name), []) AS genres,
    coalesce(movie.imageUrl, '') AS imageUrl,
    coalesce(avg(r.value), 0.0) AS averageRating,
    count(r) AS ratingCount
```

**Explanation:**

- `MATCH (movie:Movie)` retrieves movies.
- `NOT EXISTS` excludes movies already rated by the current user.
- `OPTIONAL MATCH` allows movies without genres or ratings to remain in the result.
- `collect(DISTINCT genre.name)` retrieves the movie's unique genres.
- `avg(r.value)` calculates the average rating from all users.
- `count(r)` counts the ratings received by the movie.
- `coalesce()` provides default values when a property or result is null.

**Example:**

| Movie | Genres | Average Rating | Rating Count |
|---|---|---:|---:|
| Dune | Sci-Fi, Adventure | 4.5 | 120 |
| Joker | Drama, Crime | 4.2 | 95 |
| Avatar | Sci-Fi, Adventure | 4.1 | 200 |

The global average rating is used as another signal in the hybrid recommendation score.

### 5. Hybrid Recommendation Score

After calculating the collaborative, genre-based, and global rating signals, Java combines them into one score.

The weights used by the implementation are:

| Recommendation Signal | Weight |
|---|---:|
| Collaborative Filtering | 45% |
| Genre-Based Recommendation | 35% |
| Global Average Rating | 20% |

**Normalized hybrid score**

```math
\operatorname{HybridScore}(m)
=
\frac{
0.45\,C(m)
+
0.35\,G(m)
+
0.20\,R(m)
}{
W(m)
}
```

Where:

- \(C\) = Predicted collaborative rating divided by 5.
- \(G\) = Genre preference score.
- \(R\) = Global average rating divided by 5.
- \(W\) = Sum of the weights of the available signals.

The implementation only includes collaborative and genre signals when their values are available. The global rating signal is always included.

**Example:**

Suppose a candidate movie has:

- Collaborative predicted rating = 4.5/5
- Genre score = 0.80
- Global average rating = 4.0/5

All three signals are available, so:

```math
\begin{aligned}
C(m) &= 4.5/5 = 0.90\\
G(m) &= 0.80\\
R(m) &= 4.0/5 = 0.80\\
W(m) &= 0.45+0.35+0.20 = 1.00
\end{aligned}
```

```math
\begin{aligned}
\operatorname{HybridScore}(m)
&=
(0.45\times0.90)
+
(0.35\times0.80)
+
(0.20\times0.80)\\
&=
0.405+0.280+0.160\\
&=
\boxed{0.845}
\end{aligned}
```

The final score is **0.845 (84.5%)**.

This percentage is a normalized ranking score, not a probability that the user will watch or like the movie.

If the collaborative signal is unavailable, its weight is excluded and the remaining available weights are used to normalize the score.

### 6. Final Recommendation Ranking

After calculating the hybrid score for candidate movies:

1. The system sorts movies by hybrid score in descending order.
2. Movies with higher scores appear first.
3. The service returns the top 10 recommendations.

This approach combines individual user preferences, similarities between users, and the overall popularity of movies.

### 7. Technologies Used

| Technology | Purpose |
|---|---|
| Java | Recommendation calculations and business logic |
| Spring Boot | Recommendation microservice |
| Neo4j | Graph database |
| Cypher | Retrieve users, ratings, movies, and genres |
| Neo4jClient | Execute Cypher queries from Java |
| Pearson Correlation | Calculate similarity between users |
| Hybrid Scoring | Combine recommendation signals |

### 8. Important Implementation Notes

- Ratings are represented as `RATED` relationships between `User` and `Movie` nodes.
- Genre associations are represented using `HAS_GENRE` relationships.
- Pearson similarity is calculated in Java, using rating pairs retrieved from Neo4j.
- A similar user must have positive similarity and at least 3 common rated movies to be selected.
- The recommendation weights are manually configured: 45%, 35%, and 20%.
- The system returns the top 10 movies based on their hybrid scores.
- The recommendation score is used for ranking and should not be interpreted as a probability.

