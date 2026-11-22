# Neo4flix Audit Checklist

## Functional
- [ ] Download the project from its dedicated repository, run Docker to initiate the web application, and navigate via browser or Postman.
- [ ] Verify the application works properly and core functionalities can be navigated.

## Data and Design
- [ ] Ask the auditee to show a graph representation of the database.
- [ ] Verify nodes and relationships effectively represent movies, users, and ratings.
- [ ] Verify relationships between nodes are well represented with appropriate data attributes.

## Movie Microservice
- [x] Verify the Movie microservice effectively reads and writes movie data to/from the database.
- [ ] Check whether the Movie microservice generates accurate movie recommendations based on user ratings.

## User Microservice
- [x] Verify the User microservice handles user data operations (read and write) correctly.
- [ ] Verify the User microservice stores user ratings for movies as intended.

## Rating Microservice
- [x] Verify the Rating microservice correctly handles data operations related to movie ratings.

## Recommendation Microservice
- [x] Check if Spring Data Neo4j (or another library) is used for Spring Boot and Neo4j interaction.
- [x] Check if Neo4j OGM is used to map Java objects to Neo4j nodes and relationships.
- [x] Check if a Neo4j Graph Algorithms Library approach (content-based, collaborative filtering, etc.) is used for accurate recommendations.
- [ ] Assess whether the auditee understands Cypher Query Language used to express complex recommendation logic.
- [ ] Verify the Recommendation microservice generates accurate recommendations based on user ratings and related movies.

## Search Functionality
- [x] Test searching for movies by criteria such as title, genre, and release date.

## Movie Details
- [x] Verify movie details (title, release date, genre, and rating) are accurately displayed on the movie details page.

## Rating Movies
- [x] Verify the page for rating movies works as intended.

## Viewing Recommendations
- [x] Verify users can view movie recommendations based on their ratings.

## Saving Movies
- [x] Check if users can save movies to a watchlist.

## Sharing Recommendations
- [x] Check if users can easily share movie recommendations with friends.

## User-Friendly Interface
- [ ] Evaluate whether the Neo4flix web application interface is user-friendly and intuitive.

## Web Pages
- [x] Verify essential web pages (login, registration, home, movie details, movie rating, and recommendations) work as expected.

## Security
### Authentication and Authorization
- [x] Verify OAuth 2.0 or JWT is properly implemented for user authentication.
- [x] Check whether two-factor authentication (2FA) is implemented for enhanced security.

### Password Security
- [x] Verify strong password policies, including complexity requirements, are enforced.

### Data Encryption
- [ ] Verify sensitive data is protected, HTTPS is used, and a valid SSL certificate is provided.

## Testing
- [ ] Test thoroughly against flaws and edge cases with as many test scenarios as possible.
- [x] Verify graceful error handling for unexpected input or malicious use of functionalities.
- [ ] Run stress tests and verify the application functions gracefully under load.