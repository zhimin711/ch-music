# Project Structure

## Directory Organization

```
ch-music/
├── MusicPlayer/                       # Existing desktop client
├── MusicServer/                       # Spring Boot backend
│   ├── src/main/java/com/chmusic/musicserver/
│   │   ├── api/                       # REST controllers and DTOs
│   │   ├── auth/                      # Login and token issuing
│   │   ├── config/                    # Spring configuration
│   │   ├── favorite/                  # Favorites domain
│   │   ├── music/                     # Upload, storage, streaming
│   │   ├── playlist/                  # Playlists and playlist tracks
│   │   └── user/                      # User entity and repository
│   └── src/main/resources/            # Application configuration
└── .spec-workflow/
    ├── steering/                      # Product, tech, and structure guidance
    └── specs/music-server/            # MusicServer feature specification
```

## Naming Conventions

### Files
- **Controllers**: `*Controller.java`
- **Services**: `*Service.java`
- **Repositories**: `*Repository.java`
- **DTOs**: `*Request.java`, `*Response.java`
- **Tests**: `*Tests.java`

### Code
- **Classes/Types**: PascalCase
- **Methods/Variables**: camelCase
- **Constants**: UPPER_SNAKE_CASE

## Import Patterns
1. Java and Jakarta APIs
2. Spring APIs
3. Project packages

## Code Structure Patterns
- Domain modules own entities, repositories, and business services.
- `api` owns HTTP controllers and DTO records.
- `config` owns cross-cutting Spring setup.
- Controllers stay thin and delegate domain rules to services.

## Code Organization Principles
1. **Single Responsibility**: Each class has one domain or framework role.
2. **Owner Scoping**: Services must verify authenticated ownership before returning resources.
3. **Client Stability**: DTOs are separate from JPA entities to keep API responses stable.
4. **Filesystem Safety**: Stored paths are normalized and constrained to the configured storage root.

## Module Boundaries
- `api` can depend on services and DTOs.
- Domain services can depend on repositories and other domain services.
- Repositories should not depend on controllers.
- MusicPlayer integration should call `/api` only, not backend internals.

## Code Size Guidelines
- Keep controllers focused on request/response mapping.
- Keep service methods small enough to test individual user flows.
- Add migrations once schema changes need stability across releases.

## Documentation Standards
- Public API changes should update `.spec-workflow/specs/music-server/`.
- Runtime setup details should live in `MusicServer/README.md`.
