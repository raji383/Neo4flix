// Demo-only data for local Neo4flix development.
// Run in Neo4j Browser at http://localhost:7474. Re-running is safe.

UNWIND [
    {
        id: 'demo-neo4flix-movie-1',
        title: 'Neon Horizon',
        director: 'Avery Lane',
        description: 'A courier uncovers a secret hidden beneath a rain-soaked city.',
        releaseYear: 2022,
        createdBy: 'demo-neo4flix-user-1',
        genres: ['Action', 'Sci-Fi']
    },
    {
        id: 'demo-neo4flix-movie-2',
        title: 'Clockwork Harbor',
        director: 'Morgan Reed',
        description: 'A quiet harbor town races to solve a series of impossible puzzles.',
        releaseYear: 2020,
        createdBy: 'demo-neo4flix-user-1',
        genres: ['Mystery', 'Drama']
    },
    {
        id: 'demo-neo4flix-movie-3',
        title: 'The Paper Comet',
        director: 'Riley Chen',
        description: 'Two siblings build a homemade spacecraft for one last summer.',
        releaseYear: 2024,
        createdBy: 'demo-neo4flix-user-2',
        genres: ['Adventure', 'Comedy']
    },
    {
        id: 'demo-neo4flix-movie-4',
        title: 'Winter Signal',
        director: 'Jordan Ellis',
        description: 'A remote radio operator receives a message from an abandoned station.',
        releaseYear: 2019,
        createdBy: 'demo-neo4flix-user-2',
        genres: ['Thriller', 'Mystery']
    }
] AS data
MERGE (m:Movie {id: data.id})
SET m.title = data.title,
    m.director = data.director,
    m.description = data.description,
    m.releaseYear = data.releaseYear,
    m.createdBy = data.createdBy
WITH m, data
UNWIND data.genres AS genreName
MERGE (g:Genre {name: genreName})
MERGE (m)-[:HAS_GENRE]->(g);

UNWIND [
    {id: 'demo-neo4flix-user-1', username: 'demo-user-1'},
    {id: 'demo-neo4flix-user-2', username: 'demo-user-2'},
    {id: 'demo-neo4flix-user-3', username: 'demo-user-3'},
    {id: 'demo-neo4flix-user-4', username: 'demo-user-4'},
    {id: 'demo-neo4flix-user-5', username: 'demo-user-5'},
    {id: 'demo-neo4flix-user-6', username: 'demo-user-6'},
    {id: 'demo-neo4flix-user-7', username: 'demo-user-7'},
    {id: 'demo-neo4flix-user-8', username: 'demo-user-8'}
] AS data
MERGE (u:User {id: data.id})
SET u.username = data.username;

UNWIND [
    {userId: 'demo-neo4flix-user-1', movieId: 'demo-neo4flix-movie-1', value: 5},
    {userId: 'demo-neo4flix-user-2', movieId: 'demo-neo4flix-movie-1', value: 4},
    {userId: 'demo-neo4flix-user-3', movieId: 'demo-neo4flix-movie-1', value: 3},
    {userId: 'demo-neo4flix-user-1', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-2', movieId: 'demo-neo4flix-movie-2', value: 4},
    {userId: 'demo-neo4flix-user-1', movieId: 'demo-neo4flix-movie-4', value: 2},
    {userId: 'demo-neo4flix-user-2', movieId: 'demo-neo4flix-movie-4', value: 3},
    {userId: 'demo-neo4flix-user-3', movieId: 'demo-neo4flix-movie-4', value: 4},
    {userId: 'demo-neo4flix-user-4', movieId: 'demo-neo4flix-movie-1', value: 4},
    {userId: 'demo-neo4flix-user-5', movieId: 'demo-neo4flix-movie-1', value: 4},
    {userId: 'demo-neo4flix-user-6', movieId: 'demo-neo4flix-movie-1', value: 4},
    {userId: 'demo-neo4flix-user-4', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-5', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-6', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-7', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-8', movieId: 'demo-neo4flix-movie-2', value: 5},
    {userId: 'demo-neo4flix-user-4', movieId: 'demo-neo4flix-movie-3', value: 5},
    {userId: 'demo-neo4flix-user-5', movieId: 'demo-neo4flix-movie-3', value: 4},
    {userId: 'demo-neo4flix-user-6', movieId: 'demo-neo4flix-movie-3', value: 5},
    {userId: 'demo-neo4flix-user-4', movieId: 'demo-neo4flix-movie-4', value: 3},
    {userId: 'demo-neo4flix-user-5', movieId: 'demo-neo4flix-movie-4', value: 3},
    {userId: 'demo-neo4flix-user-6', movieId: 'demo-neo4flix-movie-4', value: 3}
] AS rating
MATCH (u:User {id: rating.userId})
MATCH (m:Movie {id: rating.movieId})
MERGE (u)-[r:RATED]->(m)
SET r.value = rating.value;
