# Track Cals Server

Spring Boot API for Track Cals.

## Local Setup

1. Copy `.env.example` to `.env`.
2. Provide a reachable `spring.mongodb.uri`.
3. Set a long random `jwt.secret` before production deploys.
4. Run `./mvnw spring-boot:run`.

## API

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/profile`
- `PUT /api/profile`
- `DELETE /api/profile` - deletes the authenticated account and related profile, meal, and analytics records
- `GET /api/meals?date=YYYY-MM-DD`
- `POST /api/meals`
- `DELETE /api/meals/{id}`
- `GET /api/analytics`
- `POST /api/analytics`

All non-auth endpoints require `Authorization: Bearer <token>`.
