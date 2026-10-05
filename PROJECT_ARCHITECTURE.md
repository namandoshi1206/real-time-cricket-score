# Project Architecture and Development Plan

## 1. Objective
The system is a cricket score management platform for admins, scorers, and viewers. It provides team/player/match management, innings scoring, scorecards, and a REST-backed dashboard.

## 1.1 Implemented Runtime
The current checkout contains a Spring Boot 3.5.16 REST backend, Spring Data JPA/Hibernate persistence, a MySQL datasource, and a React/Vite client. Controllers delegate to services and repositories; DTOs define request and response contracts. Public GET routes expose fixture and score data, while write routes require ADMIN or SCORER through HTTP Basic authentication. New passwords are BCrypt-hashed. The dashboard polls score REST endpoints; WebSocket/STOMP events are not implemented.

The optional external live-data path is `React -> Spring Boot REST API -> live service/cache -> CricketDataProvider adapter -> configured external provider`. Provider calls and credentials stay server-side. The selected provider and its native schema are not included or verified; deployment requires a compatible adapter/configuration. Manual match data continues through the existing Spring Boot/MySQL path.

## 2. Final High-Level Architecture

### Overview
The platform will be split into the following major layers:

1. Frontend Layer
   - React + Vite application
   - Responsive pages for dashboards, match pages, summary views, scorecards, and admin screens
   - User-friendly and mobile-friendly UI

2. API Layer
   - Spring Boot REST APIs
   - DTO-based request and response contracts
   - Structured exception handling and validation

3. Business Service Layer
   - Match orchestration
   - Scoring logic coordination
   - Team and player operations
   - Statistics computation
   - Match summary generation

4. Persistence Layer
   - Spring Data JPA + Hibernate
   - MySQL database
   - Entities for core cricket domain objects

5. Real-Time Layer
  - Current: browser polling of REST score endpoints
  - Future: WebSocket/STOMP event publishing and live match broadcasting

6. Security Layer
  - Current: Spring Security HTTP Basic and BCrypt with ADMIN, SCORER, and VIEWER roles
  - Initial ADMIN provisioning uses APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD
  - Future: JWT and production identity lifecycle


## 3. Backend Package Architecture
The backend should follow a clear layered architecture using package separation.

```text
backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── cricket/
                    ├── config/
                    ├── controller/
                    ├── dto/
                    ├── entity/
                    ├── exception/
                    ├── repository/
                    ├── security/
                    ├── service/
                    ├── websocket/
                    ├── util/
                    ├── validation/
                    └── CricketApplication.java
```

### Package Responsibilities
- config
  - Application configuration, JPA settings, validation, WebSocket configuration, and future security settings
- controller
  - REST endpoints for users, teams, players, matches, innings, deliveries, scorecards, and stats
- dto
  - Request/response models for API contracts
- entity
  - JPA persistence entities
- exception
  - Custom exception classes and centralized error handling
- repository
  - Spring Data repository interfaces
- security
  - JWT and role-based authorization logic (future stage)
- service
  - Core business logic and orchestration
- websocket
  - WebSocket event handlers and message broadcasting logic (future stage)
- util
  - Helper methods, formatters, and common utility classes
- validation
  - Custom validators and rule enforcement logic if needed

### Architectural Principle
Controller -> Service -> Repository -> Database

Business logic should remain in services, not in controllers. DTOs should be preferred for API contracts to avoid exposing persistence entities directly.

## 4. Frontend Architecture
The frontend will be a React application built with Vite and organized around pages and reusable UI structures.

### Recommended Frontend Structure
```text
frontend/
├── src/
│   ├── api/
│   ├── components/
│   │   ├── common/
│   │   ├── dashboard/
│   │   ├── match/
│   │   └── scoreboard/
│   ├── context/
│   ├── hooks/
│   ├── pages/
│   │   ├── HomePage.jsx
│   │   ├── LiveMatchesPage.jsx
│   │   ├── MatchDetailsPage.jsx
│   │   ├── ScorecardPage.jsx
│   │   ├── PlayerStatisticsPage.jsx
│   │   ├── AdminDashboardPage.jsx
│   │   ├── ScorerDashboardPage.jsx
│   │   └── LoginPage.jsx
│   ├── routes/
│   ├── styles/
│   ├── utils/
│   ├── App.jsx
│   └── main.jsx
└── package.json
```

### UI Direction
- Responsive layout for mobile and desktop
- Clean card-based dashboards
- Live scoreboard panels and summary panels
- Role-aware navigation for admin and scorer flows
- REST polling for live score refresh; WebSocket subscriptions remain future work

### Current Client Structure
The current client keeps the main workflow in `frontend/src/App.jsx`, the REST and Basic authorization helper in `frontend/src/api.js`, the entry point in `frontend/src/main.jsx`, and responsive styling in `frontend/src/styles.css`.

## 5. Database Entity and Relationship Model
The core domain model will be centered on teams, players, matches, innings, and deliveries. A simplified yet extensible schema is preferred.

### Stage 3 actual data model
For the database foundation stage, the project includes the following initial JPA entities and relationships:

- User
  - id
  - username
  - email
  - password
  - enabled
  - createdAt
  - updatedAt
  - roles

- Role
  - id
  - name
  - description

- Team
  - id
  - name
  - shortName
  - country
  - createdAt
  - updatedAt

- Player
  - id
  - firstName
  - lastName
  - role
  - battingStyle
  - bowlingStyle
  - createdAt
  - updatedAt

- TeamPlayer
  - id
  - team
  - player
  - createdAt
  - updatedAt

- Match
  - id
  - title
  - teamA
  - teamB
  - venue
  - matchType
  - scheduledDate
  - status
  - createdAt
  - updatedAt

### Match management
Match CRUD is implemented through DTO-based REST controllers, service-layer validation, Spring Data repositories, and lazy `ManyToOne` relationships. Matches reference two distinct existing teams and support status and type filtering.

- Innings
  - id
  - match
  - battingTeam
  - bowlingTeam
  - inningsNumber
  - targetRuns
  - totalRuns
  - wickets
  - legalBalls
  - striker
  - nonStriker
  - currentBowler
  - status

- Delivery
  - id
  - innings
  - overNumber
  - ballNumber
  - batsman
  - nonStriker
  - bowler
  - runs
  - extraType
  - extraRuns
  - totalRuns
  - legalDelivery
  - wicketType
  - dismissedBatsman
  - createdAt

### Scoring engine
The scoring engine manages innings and transactional deliveries through the service layer. It validates match teams and player membership, calculates runs and extras, distinguishes legal deliveries from wides and no-balls, tracks dismissals and strike, formats overs as legal-ball notation, and completes limited-overs innings on targets, wickets, or configured overs. Innings scorecards derive batting/bowling figures and extras from deliveries. WebSocket events and advanced Test-match rules remain deferred.

### Core Entities
- User
  - id
  - username
  - email
  - passwordHash (future)
  - role
  - createdAt

- Role
  - id
  - name

- Team
  - id
  - name
  - shortName
  - city
  - createdAt

- Player
  - id
  - teamId
  - name
  - battingStyle
  - bowlingStyle
  - role
  - dateOfBirth

- TeamPlayer
  - id
  - teamId
  - playerId
  - jerseyNumber
  - isCaptain
  - isKeeper

- Match
  - id
  - teamAId
  - teamBId
  - venue
  - tossWinnerTeamId
  - tossDecision
  - status
  - startTime
  - endTime
  - winnerTeamId
  - targetRuns
  - targetOvers

- Innings
  - id
  - matchId
  - battingTeamId
  - bowlingTeamId
  - inningsNumber
  - totalRuns
  - wicketsFallen
  - overs
  - isCompleted

- Delivery
  - id
  - inningsId
  - ballNumber
  - overNumber
  - legalDelivery
  - batsmanId
  - bowlerId
  - runsScored
  - extraRuns
  - totalRuns
  - wicketType
  - wicketFielderId
  - notes

- BattingScore
  - id
  - inningsId
  - playerId
  - runs
  - ballsFaced
  - fours
  - sixes
  - strikeRate
  - isOut
  - dismissalType

- BowlingScore
  - id
  - inningsId
  - playerId
  - overs
  - runsConceded
  - wickets
  - maidenOvers
  - economyRate

- MatchEvent
  - id
  - matchId
  - eventType
  - description
  - createdAt

### Key Relationship Concept
- Team has many players
- Match has two teams and multiple innings
- Innings has many deliveries
- Delivery is the primary scoring event record

### Design Decisions
- Keep the model simple and scalable
- Use foreign keys and indexes for match lookup, innings lookup, and player stats
- Add constraints for valid status values and wicket events
- Keep ball-by-ball data as the source of truth for calculations

## 6. Major REST API Groups
These are the eventual API groups the application will need.

### Core Domain APIs
- /api/users
- /api/roles
- /api/teams
- /api/players
- /api/team-players
- /api/matches
- /api/innings
- /api/deliveries
- /api/scorecards
- /api/statistics
- /api/admin

### Representative Endpoints
- GET /api/matches
- GET /api/matches/{id}
- POST /api/matches
- GET /api/teams
- POST /api/teams
- GET /api/players
- POST /api/players
- GET /api/matches/{id}/scorecard
- GET /api/matches/{id}/summary
- GET /api/statistics/players/{id}
- GET /api/admin/dashboard

### API Design Notes
- Keep entity controllers slim
- Use DTOs for request/response payloads
- Return meaningful HTTP status codes
- Validate payloads before service execution

## 7. WebSocket Architecture for Live Updates
WebSocket/STOMP will be used later for broadcasting live scoring events.

### Object Flow
SCORER -> REST API -> Spring Boot Service -> Database -> WebSocket Topic -> Connected Viewers

### Recommended Architecture
- STOMP endpoint for clients to subscribe to live match topics
- Topic naming pattern such as:
  - /topic/match.{matchId}.score
  - /topic/match.{matchId}.summary
  - /topic/match.{matchId}.stats
- Delivery recording triggers a score update event
- Connected viewers receive changes without manual browser refresh

### Future WebSocket Responsibilities
- Push score updates
- Push match summary updates
- Push wicket and milestone events
- Push scoreboard state changes
- Maintain connection lifecycle and error handling

## 8. Important Cricket Scoring Business Rules to Implement Later
These are critical rules that must be captured in the scoring engine stage.

### Core Rules
- An over consists of 6 legal deliveries
- A wide adds runs but is not a legal delivery
- A no-ball adds runs and is not a legal delivery
- The batting side can lose wickets
- A scorecard must reflect current batsmen, bowlers, extras, and total runs
- Partnership values must be computed from runs scored together
- Strike rate and economy rate are derived values
- Batsman and bowler statistics should be cumulative across innings

### Valid Run Types
- Normal runs: 0, 1, 2, 3, 4, 6
- Extras: wide, no ball, bye, leg bye, penalty runs
- Wicket types: bowled, caught, lbw, run out, stumped, hit wicket

### Calculated Metrics
- Total runs
- Total wickets
- Overs and legal deliveries
- Current batsman score
- Balls faced
- Fours and sixes
- Strike rate
- Bowler overs
- Bowler runs conceded
- Bowler wickets
- Economy rate
- Current run rate
- Required run rate
- Target
- Partnership

### Validation Principles
- Only legal deliveries advance the over count
- Extras must not be counted as legal deliveries
- A wicket can affect batting dispatch and player stats
- Match progression must preserve valid inning state transitions

## 9. Assumptions and Constraints
- Authentication and authorization are intentionally deferred
- WebSocket support is intentionally deferred
- Docker and deployment preparation are intentionally deferred
- This is a learning-oriented project with strong emphasis on maintainability and expansion
- Configuration must stay environment-based and secrets must never be committed

## 10. Development Plan Summary
### Stage 1 (Current)
- Requirements analysis
- Architecture proposal
- Documentation setup
- Project readiness for next development stage

### Stage 2
- Initialize Spring Boot backend structure
- Create Maven project and base configuration
- Add base package structure
- Prepare for entity and database work

### Later stages
- MySQL configuration and tables
- Team and player modules
- Match and innings management
- Ball-by-ball scoring engine
- Rules and validation logic
- Statistics and scorecards
- React frontend
- Real-time dashboards
- Security and admin access
- Testing and deployment preparation

## 11. Implementation Guidance
The project should remain simple, beginner-friendly, and maintainable. Avoid unnecessary libraries and avoid over-engineering the domain. Build features incrementally and validate each stage before moving to the next.

The most important architectural priorities for this project are:
- clear separation of concerns
- domain-driven and service-oriented backend design
- scalable and future-friendly data model
- extensibility for real-time match updates
- strict validation and good error handling
