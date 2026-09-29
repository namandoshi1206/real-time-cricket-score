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
- Live score updates
- Match summary generation
- Scorecard generation
- Player statistics
- Role-based user access (future)
- Admin dashboard (future)
- Search and filtering (future)
- Real-time WebSocket broadcasting (future)
- Responsive dashboard and viewer experience

## Technology Stack
### Backend
- Java 25
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- MySQL
- Bean Validation
- Spring Security (future stage)
- JWT (future stage)
- WebSocket + STOMP (future stage)

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
- Real-time layer: WebSocket/STOMP for live score broadcasts
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
- Stage 6 complete: innings and ball-by-ball scoring engine

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

The scoring engine supports batsman runs from 0 through 6, wides, no-balls, byes, leg-byes, penalty extras, wickets, legal-ball counting, standard cricket over notation, target completion, limited-overs completion, and transactional delivery updates. WebSocket broadcasting, statistics, and advanced Test-match rules remain deferred.

## Initial Architecture Notes
This project will use a clean layered structure where controllers coordinate requests, services contain business logic, repositories handle persistence, and entities represent database state. DTOs will be used to prevent leaking persistence models to API consumers.

Authentication, WebSocket, and deployment concerns are intentionally deferred to later stages to preserve a simple, maintainable progression.

## Project Goals
- Build a learning-focused but professional-grade cricket scoring system
- Keep the architecture modular and extensible
- Support future real-time live match updates
- Follow current Spring Boot best practices
- Maintain environment-based configuration and avoid hardcoded secrets

## Notes
This repository has completed Stage 6. Statistics, real-time updates, security, frontend, and deployment remain deferred to later stages.
