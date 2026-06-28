# Requirements Document

## Introduction
MusicServer is the personal music resource backend for the MusicPlayer desktop client. It supports account login, private music uploads, personal playlists, and a favorites collection similar to a private cloud music drive.

## Alignment with Product Vision
This feature makes CH Music self-hostable and account-aware. MusicPlayer can evolve from a local-only client into a private music library client backed by a user's own server.

## Requirements

### Requirement 1
**User Story:** As a MusicPlayer user, I want to register, log in, and keep an authenticated session, so that my private music library is protected.

#### Acceptance Criteria
1. WHEN a user registers with a unique username and password THEN the system SHALL create the account and return a Bearer token.
2. WHEN a user logs in with valid credentials THEN the system SHALL return a Bearer token and current user profile.
3. IF credentials are invalid THEN the system SHALL reject the request with HTTP 401.
4. WHEN a user logs out THEN the system SHALL revoke the presented token.

### Requirement 2
**User Story:** As a user, I want to upload personal private music files, so that MusicPlayer can play my own cloud library.

#### Acceptance Criteria
1. WHEN an authenticated user uploads an audio file THEN the system SHALL store the file on disk and persist metadata in the database.
2. IF the file is empty or not a supported audio type THEN the system SHALL reject the upload with HTTP 400.
3. WHEN a user lists music THEN the system SHALL return only files owned by that user.
4. WHEN a user streams or deletes music THEN the system SHALL enforce ownership.

### Requirement 3
**User Story:** As a user, I want to manage personal playlists, so that I can organize my uploaded songs.

#### Acceptance Criteria
1. WHEN a user creates a playlist THEN the system SHALL store it under the authenticated owner.
2. WHEN a user updates or deletes a playlist THEN the system SHALL modify only that user's playlist.
3. WHEN a user adds a song to a playlist THEN the system SHALL require that the song is owned by the same user.
4. IF a song is already in the playlist THEN the system SHALL keep the operation idempotent.

### Requirement 4
**User Story:** As a user, I want a favorites collection, so that I can quickly access liked songs.

#### Acceptance Criteria
1. WHEN a user favorites a song THEN the system SHALL add that owned song to the user's favorites.
2. WHEN a user removes a favorite THEN the system SHALL remove only that user's favorite entry.
3. WHEN a user lists favorites THEN the system SHALL return favorite songs ordered by newest first.

### Requirement 5
**User Story:** As a MusicPlayer developer, I want a stable REST API, so that the desktop client can integrate without depending on backend internals.

#### Acceptance Criteria
1. WHEN MusicPlayer calls the backend THEN the system SHALL expose all endpoints under `/api`.
2. WHEN protected endpoints are called without a valid token THEN the system SHALL reject them.
3. WHEN errors occur THEN the system SHALL return structured JSON error responses.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Controllers, services, repositories, and entities are separated.
- **Modular Design**: Auth, user, music, playlist, and favorite domains are isolated.
- **Dependency Management**: Spring Boot manages dependency versions.
- **Clear Interfaces**: MusicPlayer integrates through HTTP DTOs, not JPA entities.

### Performance
- Upload size defaults to 500 MB per file.
- Streaming returns a file resource instead of reading the entire song into memory.

### Security
- Passwords must be BCrypt hashed.
- Tokens must be stored hashed and revocable.
- All library data must be scoped by authenticated owner.

### Reliability
- Local development must run without an external database.
- File paths must remain inside the configured storage root.

### Usability
- Initial setup must work with Maven Wrapper and Java 17+.
- API responses must be simple for the Electron client to consume.
