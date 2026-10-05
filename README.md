# Real-Time Cricket Score Management System

## Project Objective
The Real-Time Cricket Score Management System is a full-stack application designed to simulate live cricket match scoring for educational and professional learning purposes. The project will allow admins, scorers, and viewers to manage teams, players, matches, innings, deliveries, scores, and live match updates in a structured, scalable, and extensible architecture.

The system is intentionally designed to simulate live scoring without depending on an external cricket data provider. It will be built incrementally and can later be containerized for deployment.

## Features
- Team management
- Player management
- Match management
- Innings tracking
- Ball-by-ball scoring
- Run, extra, and wicket recording
- Live score refresh through REST polling
- External live, upcoming, and recent match feeds through a configurable provider adapter
- Innings scorecards with batting and bowling figures
- Match summary and result calculation
- Role-based access for ADMIN, SCORER, and VIEWER
- Admin dashboard (future)
- Search and filtering (future)
- Real-time WebSocket broadcasting (future)
- Responsive dashboard and viewer experience

## Technology Stack
### Backend
- Java 25
- Spring Boot 3.5.16
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- MySQL
- Bean Validation
- Spring Security HTTP Basic
- BCrypt password hashing

### Frontend
- React
- Vite
- JavaScript
- React Router
- Responsive CSS

### Database
- MySQL

### Testing
- JUnit
- Mockito
- Spring Boot Test
- Postman

### Version Control and Deployment
- Git
- GitHub
- Docker and Docker Compose (future stage)

## High-Level Architecture
The application will follow a layered and modular design:

- Frontend: React UI for dashboard, live score pages, scorecards, and match views
- Backend: Spring Boot service layer for business logic and API orchestration
- Data access: Spring Data JPA repositories over MySQL
- Live updates: REST polling; WebSocket/STOMP remains a future enhancement
- Validation: domain rules, DTO validation, and service-level checks

## Development Stages
1. Stage 1: Project planning and architecture
2. Stage 2: Spring Boot backend setup
3. Stage 3: MySQL database design and configuration
4. Stage 4: Teams and players
5. Stage 5: Match management
6. Stage 6: Ball-by-ball scoring engine
7. Stage 7: Cricket rules and validation
8. Stage 8: Player statistics
9. Stage 9: Scorecard and match summary
10. Stage 10: React frontend
11. Stage 11: Live cricket dashboard
12. Stage 12: WebSocket real-time updates
13. Stage 13: Spring Security + JWT + role-based authorization
14. Stage 14: Admin dashboard
15. Stage 15: Search, filtering and performance optimization
16. Stage 16: Testing
17. Stage 17: Docker and deployment preparation
18. Stage 18: Final documentation and GitHub preparation

## MySQL Database Setup
This project uses MySQL for the persistence layer. The database name is `cricket_score_db`.

Required environment variables:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

Example:
```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "cricket_score_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_secure_password"
```

Create the database using:
```sql
CREATE DATABASE cricket_score_db;
```

Current stage status:
- Stage 1 complete
- Stage 2 complete
- Stage 3 complete: database foundation, entities, repositories, and configuration
- Stage 4 complete: team and player management APIs
- Stage 5 complete: match management APIs
- Stage 6: innings, deliveries, scoring, and scorecards
- Stage 7: basic HTTP Basic role authorization and registration
- Stage 8: React/Vite operations dashboard

## Stage 4 APIs
Team endpoints:
- `GET /api/teams`
- `GET /api/teams/{id}`
- `POST /api/teams`
- `PUT /api/teams/{id}`
- `DELETE /api/teams/{id}`

Player endpoints:
- `GET /api/players`
- `GET /api/players/{id}`
- `GET /api/players/team/{teamId}`
- `POST /api/players`
- `PUT /api/players/{id}`
- `DELETE /api/players/{id}`

Players are associated with teams through the `team_players` relationship entity. Requests use DTOs with Bean Validation, and invalid, missing, or conflicting resources return structured API errors.

## Stage 5 APIs
Match endpoints:
- `GET /api/matches`
- `GET /api/matches/{id}`
- `POST /api/matches`
- `PUT /api/matches/{id}`
- `DELETE /api/matches/{id}`
- `GET /api/matches/status/{status}`
- `GET /api/matches/type/{type}`

Matches reference two different existing teams and use the existing `MatchType` and `MatchStatus` enums. Match status updates follow the basic lifecycle rules implemented for this stage.

## Stage 6 Scoring APIs
Innings endpoints:
- `POST /api/matches/{matchId}/innings`
- `GET /api/matches/{matchId}/innings`
- `GET /api/innings/{inningsId}`

Delivery and score endpoints:
- `POST /api/innings/{inningsId}/deliveries`
- `GET /api/innings/{inningsId}/deliveries`
- `GET /api/innings/{inningsId}/score`

The scoring engine supports batsman runs from 0 through 6, wides, no-balls, byes, leg-byes, penalty extras, wickets, legal-ball counting, strike rotation, standard cricket over notation, target completion, limited-overs completion, and transactional delivery updates. The score endpoint includes run rate, batting figures, bowling figures, and extras. WebSocket broadcasting and advanced Test-match rules remain deferred.

## Running the Application

Requirements: Java 25, Maven 3.9.16, MySQL, and Node.js/npm.

The datasource URL defaults to `localhost:3306/cricket_score_db`. Provide datasource credentials through Spring environment overrides and never commit real credentials:

```powershell
$env:SPRING_DATASOURCE_USERNAME = "cricket_app"
$env:SPRING_DATASOURCE_PASSWORD = "<set locally>"
$env:APP_ADMIN_USERNAME = "score-admin"
$env:APP_ADMIN_PASSWORD = "<set locally; use a strong password>"
```

The current local `application.yml` still contains inline datasource credentials; replace those with environment placeholders before sharing or deploying this checkout. `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` override those local values. Hibernate uses `ddl-auto: update` for the development database; do not use this setting as a production migration strategy.

Start the backend:

```powershell
cd backend
mvn spring-boot:run
```

Start the frontend in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Set `VITE_API_URL` when the backend is not at `http://localhost:9090/api`.

## Real-World Live Cricket Data

The backend exposes `GET /api/live/matches`, `/api/live/upcoming`, and `/api/live/recent`, plus match detail, score, and scorecard routes below `/api/live/matches/{matchId}`. The React client polls these endpoints every 15 seconds and never calls a provider directly. With no configured provider, the API and UI return/show an explicit unavailable or empty state; they do not substitute sample scores. **Live provider credentials/data source still need to be configured.** No vendor format or account was verified for this checkout.

Configure the provider in the backend process environment (values are not read from `.env` automatically):

```powershell
$env:CRICKET_API_BASE_URL = "https://<provider-host>/<api-root>"
$env:CRICKET_API_KEY = "<set privately>"
$env:CRICKET_API_KEY_HEADER = "Authorization"
$env:CRICKET_API_KEY_PREFIX = "Bearer"
$env:CRICKET_API_LIVE_PATH = "/matches/live"
$env:CRICKET_API_UPCOMING_PATH = "/matches/upcoming"
$env:CRICKET_API_RECENT_PATH = "/matches/recent"
```

The adapter contract is intentionally vendor-neutral and must be mapped to the chosen provider. It requests the configured live/upcoming/recent path using the configured key header and expects JSON shaped as `{ "data": [ ... ] }`. Each match needs `matchId`, `title`, and `status`; teams, score, innings, batters, bowlers, recent deliveries, result, venue, and update time are optional and are only displayed when supplied. A score object may contain `runs`, `wickets`, `overs`, `inningsNumber`, `runRate`, and `targetRuns`. Provider-specific response fields must be normalized by a compatible adapter before enabling credentials. Keys stay in the backend.

Live responses are cached in memory for 15 seconds; upcoming/recent feeds for 5 minutes. Failed refreshes return the most recent cached records as stale when available, otherwise an unavailable response. This cache is per application process and is not persisted.

## Authentication and Roles

`POST /api/auth/register` creates a VIEWER account; passwords are BCrypt-hashed. `GET /api/auth/me` requires HTTP Basic credentials. Read-only API requests are public. Write requests require ADMIN or SCORER. Set `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` before first startup to provision an initial ADMIN account. The browser holds credentials in memory for the current session. HTTP Basic should only be used over HTTPS outside local development. JWT is not implemented.

## API Overview

- Health: `GET /api/health`
- Authentication: `POST /api/auth/register`, `GET /api/auth/me`
- Teams and players: CRUD routes under `/api/teams` and `/api/players`; players can be listed by `/api/players/team/{teamId}`
- Matches: CRUD and status/type filters under `/api/matches`; `GET /api/matches/{id}/summary`
- Scoring: innings routes under `/api/matches/{matchId}/innings`; delivery and score routes under `/api/innings/{inningsId}`
- External live cricket: `/api/live/matches`, `/api/live/matches/{matchId}`, `/api/live/matches/{matchId}/score`, `/api/live/matches/{matchId}/scorecard`, `/api/live/upcoming`, `/api/live/recent`

## Scoring Notes

The scorer supplies the striker, non-striker, bowler, run/extras, and optional dismissed batter for each delivery. The service validates team membership and delivery sequence, rotates strike for completed runs and over changes, and completes T20/ODI innings on target, overs, or ten wickets. Scorecard figures are derived from recorded deliveries. The current model does not implement all Test-match rules or automatic player statistics beyond the innings scorecard.

## Verification

Run backend tests with `cd backend; mvn clean test`. Build the frontend with `cd frontend; npm install; npm run build`. Tests cover health, controller behavior, core scoring, and basic registration/write authorization. No production database migration or WebSocket integration test is included.

## Screenshots

Screenshots can be added here after capturing the running dashboard.

## Initial Architecture Notes
This project will use a clean layered structure where controllers coordinate requests, services contain business logic, repositories handle persistence, and entities represent database state. DTOs will be used to prevent leaking persistence models to API consumers.

JWT, WebSocket broadcasting, and deployment concerns remain future enhancements. Basic HTTP authentication and browser polling are implemented.

## Project Goals
- Build a learning-focused but professional-grade cricket scoring system
- Keep the architecture modular and extensible
- Support future real-time live match updates
- Follow current Spring Boot best practices
- Maintain environment-based configuration and avoid hardcoded secrets

## Notes
The backend and React client cover team/player management, match management, innings scoring, scorecards, and basic role-gated writes. See the setup and limitations above before using this repository outside local development.
