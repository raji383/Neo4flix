# 🎬 Neo4flix

**Neo4flix** is a movie discovery and personalized recommendation platform built with Angular, Spring Boot microservices, and Neo4j.

The platform allows users to explore movies, search and filter the catalogue, rate movies, manage personal watchlists, and receive personalized movie recommendations using a hybrid recommendation algorithm.

Administrators can manage the movie catalogue, while users can manage their profiles and secure their accounts with optional Two-Factor Authentication (2FA).

---

## ✨ Features

### 👤 User Management & Authentication
- User registration and login.
- JWT-based authentication.
- User profile management.
- Role-based access control (USER / ADMIN).
- Optional Two-Factor Authentication (2FA).
- Email OTP verification when activating 2FA.
- OTP-based login verification when 2FA is enabled.
- Secure password hashing.

### 🎥 Movie Catalogue
- Browse available movies.
- Search movies by title.
- Filter movies by genre and release year.
- Paginated movie listings.
- View detailed movie information.
- Explore popular movies.
- Admin-only movie creation, updating, and deletion.
- Movie poster uploads using Cloudinary.

### ⭐ Rating System
- Rate movies from 1 to 5.
- Update existing ratings.
- Delete ratings.
- Retrieve personal ratings.
- View rating history.
- Store ratings as relationships between users and movies in Neo4j.

### 📋 Personal Watchlist
- Add movies to a personal watchlist.
- Remove movies from the watchlist.
- Retrieve saved movies.
- Check whether a movie is already in the watchlist.

### 🤖 Personalized Recommendations

Neo4flix implements a hybrid recommendation system combining:

1. **Collaborative Filtering:** Finds users with similar rating patterns and uses their ratings to predict preferences.
2. **Genre-Based Recommendations:** Analyzes the user's rating history to identify genre preferences.
3. **Global Movie Ratings:** Uses the average rating of movies as an additional ranking signal.

The hybrid scoring model uses the following weights:

| Recommendation Signal | Weight |
|---|---:|
| Collaborative Filtering | 45% |
| Genre Preference | 35% |
| Global Average Rating | 20% |

The system combines available signals, normalizes the scores, and returns up to 10 recommended movies.

### 🛠️ Admin Dashboard
- Manage the movie catalogue.
- Add, update, and delete movies.
- Access catalogue statistics.
- Restrict dashboard access to administrators.

---

## 🏗️ Architecture

Neo4flix follows a microservices architecture. Each backend service has its own Spring Boot application and communicates through the API Gateway and Eureka Service Registry.

```text
                    Angular Frontend
                           |
                           |
                           v
                    API Gateway
                     HTTPS :8443
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     User Service     Movie Service    Rating Service
       :8083             :8080            :8081
          |                |                |
          |                |                |
          +----------------+----------------+
                           |
                           v
                      Neo4j Database
                        :7687

                    Recommendation Service
                            :8082
                              |
                              v
                         Neo4j Database

                    Eureka Service Registry
                            :8761
```

### Microservices

| Service | Responsibility |
|---|---|
| API Gateway | Central API entry point, JWT validation, routing, and HTTPS |
| User Service | Registration, authentication, profiles, and 2FA |
| Movie Service | Movie catalogue, genres, search, and movie management |
| Rating Service | Movie ratings and personal watchlists |
| Recommendation Service | Personalized movie recommendations |
| Registry | Service discovery using Netflix Eureka |

All domain services currently connect to a shared Neo4j database.

---

## 🧰 Technology Stack

### Frontend
- Angular 22
- TypeScript 6
- RxJS
- Angular Material
- HTML5 / SCSS

### Backend
- Java 21
- Spring Boot 4
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data Neo4j
- Maven
- JWT (JSON Web Tokens)
- JavaMail / SMTP

### Database
- Neo4j 5 Community Edition
- Cypher Query Language

### External Services
- Cloudinary – Movie poster storage.
- TMDB API – Optional movie catalogue import.
- Gmail SMTP – Email verification and 2FA OTP delivery.

### DevOps & Deployment
- Docker
- Docker Compose
- Maven Wrapper
- NGINX

---

## 📂 Project Structure

```text
Neo4flix/
│
├── backend/
│   ├── api-gateway/
│   ├── registry/
│   ├── user-service/
│   ├── movie-service/
│   ├── rating-service/
│   └── recommendation-service/
│
├── frontend/
│   ├── src/
│   │   └── app/
│   │       ├── core/
│   │       ├── features/
│   │       ├── layout/
│   │       └── shared/
│   └── package.json
│
├── docs/
│   ├── Architecture.md
│   └── todo.md
│
├── docker-compose.yml
├── .env.example
├── run-services.sh
└── README.md
```

---

## ⚙️ Prerequisites

Make sure you have installed:

- Java 21
- Node.js and npm
- Docker
- Docker Compose
- Git

External service credentials are required for features such as Cloudinary uploads and email delivery.

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/raji383/Neo4flix.git

cd Neo4flix
```

### 2. Configure environment variables

Create your local environment file:

```bash
cp .env.example .env
```

Configure the required values:

```env
# JWT
JWT_SECRET=YOUR_BASE64_ENCODED_SECRET
JWT_EXPIRATION=3600000

# Neo4j
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=YOUR_NEO4J_PASSWORD

# API Gateway HTTPS
API_GATEWAY_SSL_PASS=YOUR_KEYSTORE_PASSWORD

# Email / Gmail SMTP
MAIL_USERNAME=YOUR_GMAIL_ADDRESS
MAIL_APP_PASSWORD=YOUR_GMAIL_APP_PASSWORD

# Cloudinary
CLOUDINARY_CLOUD_NAME=YOUR_CLOUD_NAME
CLOUDINARY_API_KEY=YOUR_API_KEY
CLOUDINARY_API_SECRET=YOUR_API_SECRET

# TMDB (Optional)
TMDB_ACCESS_TOKEN=YOUR_TMDB_ACCESS_TOKEN
MOVIE_SEED_ENABLED=false
MOVIE_SEED_COUNT=100
```

Generate a Base64-encoded JWT secret:

```bash
openssl rand -base64 32
```

**Important:**
- Use a Gmail App Password for SMTP, not your regular Gmail password.
- Never commit your `.env` file or real credentials.
- Configure the required environment variables before starting the services.

### 3. Start the complete application with Docker Compose

```bash
docker compose up --build
```

Docker Compose starts:
- Neo4j
- Eureka Registry
- All backend microservices
- API Gateway
- Angular frontend served through NGINX

The services use health checks to coordinate startup.

### 4. Access the application

| Component | URL |
|---|---|
| Frontend (HTTPS) | https://localhost:8444 |
| API Gateway | https://localhost:8443 |
| Eureka Dashboard | http://localhost:8761 |
| Neo4j Browser | http://localhost:7474 |

The included HTTPS certificates are intended for local development. Your browser may display a certificate warning.

---

## 💻 Running Services Individually

You can also run the backend services without Docker Compose.

First, start Neo4j separately.

Then start all backend services:

```bash
./run-services.sh start all
```

Check service status:

```bash
./run-services.sh status
```

Start an individual service:

```bash
./run-services.sh start movie-service
```

Stop an individual service:

```bash
./run-services.sh stop movie-service
```

Stop all backend services:

```bash
./run-services.sh stop all
```

The script manages the six Spring Boot applications. It does not start Neo4j or the frontend.

### Running the Angular frontend

```bash
cd frontend

npm ci

npm start
```

Open:

```text
http://localhost:4200
```

---

## 🔌 API Overview

All API requests are routed through the API Gateway.

Base URL:

```text
https://localhost:8443
```

Protected endpoints require a JWT:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Authenticate a user |
| POST | `/api/auth/2fa/enable` | Start 2FA activation |
| POST | `/api/auth/2fa/email/verify` | Verify email OTP for 2FA activation |
| POST | `/api/auth/2fa/verify` | Verify login OTP |
| POST | `/api/auth/2fa/disable` | Disable 2FA |

### User Profile

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Retrieve current user profile |
| PUT | `/api/users/me` | Update current user profile |

### Movies & Genres

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/movies` | Browse, search, and filter movies |
| GET | `/api/movies/popular` | Retrieve popular movies |
| GET | `/api/movies/{id}` | Retrieve movie details |
| POST | `/api/movies/add` | Add a movie (Admin) |
| PUT | `/api/movies/{id}` | Update a movie (Admin) |
| DELETE | `/api/movies/{id}` | Delete a movie (Admin) |
| GET | `/api/genres` | Retrieve available genres |

### Ratings

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/ratings/{movieId}` | Create a rating |
| PUT | `/api/ratings/{movieId}` | Update a rating |
| DELETE | `/api/ratings/{movieId}` | Delete a rating |
| GET | `/api/ratings/{movieId}` | Retrieve personal movie rating |
| GET | `/api/ratings/me` | Retrieve personal rating history |

### Watchlist

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/watchlist/{movieId}` | Add a movie |
| DELETE | `/api/watchlist/{movieId}` | Remove a movie |
| GET | `/api/watchlist` | Retrieve watchlist |
| GET | `/api/watchlist/{movieId}` | Check watchlist membership |

### Recommendations

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/recommendations` | Retrieve personalized recommendations |

---

## 🧪 Testing

### Backend tests

Each backend microservice is an independent Maven project.

For example:

```bash
cd backend/user-service

./mvnw test
```

Run the same command inside each service directory to execute its tests.

### Frontend tests

```bash
cd frontend

npm test
```

### Frontend development build

```bash
npm run build -- --configuration development
```

---

## 🎞️ Optional TMDB Movie Import

Neo4flix supports importing movie information from TMDB.

To enable it:

1. Configure your TMDB access token.
2. Configure Cloudinary credentials.
3. Configure the existing administrator ID as the movie owner.
4. Set the import configuration:

```env
MOVIE_SEED_ENABLED=true
MOVIE_SEED_COUNT=100
MOVIE_SEED_OWNER=YOUR_ADMIN_USER_ID
```

5. Start or restart the Movie Service.

Imported movie information includes supported metadata such as titles, descriptions, genres, directors, posters, and TMDB identifiers.

TMDB popularity can be used to order the Popular section. TMDB vote averages and vote counts are not used as Neo4flix user ratings.

Disable the importer when you do not want it to run on subsequent startups:

```env
MOVIE_SEED_ENABLED=false
```

---

## 🔐 Security Notes

- JWT-based authentication.
- Password hashing.
- API Gateway token validation.
- Role-aware access control.
- Optional email-based Two-Factor Authentication.
- Environment-based secret configuration.
- HTTPS for local development.

The bundled certificates are development certificates. Production deployments should use trusted TLS certificates and production-grade secret management.

---

## 🙌 Acknowledgements

- [Neo4j](https://neo4j.com/) – Graph database.
- [Spring Boot](https://spring.io/projects/spring-boot) – Backend framework.
- [Angular](https://angular.dev/) – Frontend framework.
- [TMDB](https://www.themoviedb.org/) – Movie metadata.
- [Cloudinary](https://cloudinary.com/) – Media storage.

This product uses the TMDB API but is not endorsed or certified by TMDB.

---

## 👨‍💻 Author

**azraji**
**ranniz**

- GitHub: [raji383](https://github.com/raji383)
- Project: [Neo4flix](https://github.com/raji383/Neo4flix)
