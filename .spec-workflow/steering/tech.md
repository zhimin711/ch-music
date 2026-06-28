# Technology Stack

## Project Type
MusicServer is a Java REST API service for the CH Music desktop client.

## Core Technologies

### Primary Language(s)
- **Language**: Java 17
- **Runtime/Compiler**: JVM with Maven Wrapper
- **Language-specific tools**: Maven, Spring Boot Maven Plugin

### Key Dependencies/Libraries
- **Spring Boot 4.1.0**: Application runtime and dependency management.
- **Spring Web MVC**: REST endpoints and multipart uploads.
- **Spring Security**: Bearer-token request authentication.
- **Spring Data JPA / Hibernate**: Relational persistence.
- **H2**: Local embedded database for development.
- **PostgreSQL Driver**: Production database option.
- **Flyway**: Migration-ready database lifecycle.
- **Jakarta Validation**: Request validation.

### Application Architecture
MusicServer is a modular monolith organized by domain: auth, user, music, playlist, favorite, config, and api.

### Data Storage
- **Primary storage**: H2 file database by default; PostgreSQL-ready through configuration.
- **File storage**: Local filesystem under `music.storage.root`.
- **Data formats**: JSON for API payloads, multipart form data for uploads.

### External Integrations
- **APIs**: MusicPlayer consumes HTTP REST endpoints under `/api`.
- **Protocols**: HTTP/REST with Bearer tokens.
- **Authentication**: Opaque server-issued tokens stored hashed in the database.

## Development Environment

### Build & Development Tools
- **Build System**: Maven Wrapper
- **Package Management**: Maven
- **Development workflow**: `./mvnw spring-boot:run`

### Code Quality Tools
- **Static Analysis**: Java compiler and Spring context tests.
- **Formatting**: Standard Java formatting.
- **Testing Framework**: JUnit through Spring Boot test starters.

## Deployment & Distribution
- **Target Platform(s)**: Local machine, NAS, home server, or cloud VM.
- **Distribution Method**: Runnable Spring Boot jar.
- **Installation Requirements**: Java 17+.

## Technical Requirements & Constraints

### Performance Requirements
- Uploads must support large personal audio files up to 500 MB by default.
- Streaming endpoints should return file resources without loading the whole file into memory.

### Compatibility Requirements
- **Platform Support**: macOS, Windows, and Linux server runtimes with Java 17+.
- **Client Support**: Electron-based MusicPlayer using JSON, multipart upload, and Bearer token auth.

### Security & Compliance
- Passwords are BCrypt hashed.
- Access tokens are stored as SHA-256 hashes.
- Every music, playlist, and favorite query is scoped to the authenticated owner.

### Scalability & Reliability
- Initial scope targets personal use and small household deployments.
- Future production deployments should move from H2 to PostgreSQL and add migrations.

## Technical Decisions & Rationale
1. **Opaque tokens over JWT**: Easier revocation and less key management for a personal backend.
2. **Filesystem audio storage**: Keeps large files out of the relational database.
3. **H2 default with PostgreSQL driver**: Fast local startup while leaving a production path open.

## Known Limitations
- No audio metadata extraction yet; title, artist, and album are passed by the client or inferred from filename.
- No transcoding or Range-specific response logic yet; initial streaming relies on Spring resource handling.
- No multi-device sync protocol yet beyond normal REST reads and writes.
