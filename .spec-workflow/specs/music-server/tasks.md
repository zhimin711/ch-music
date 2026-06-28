# Tasks Document

- [x] 1. Initialize Spring Boot backend project
  - File: `MusicServer/pom.xml`
  - Generate Maven-based Spring Boot project with Java 17 and Spring Boot 4.1.0.
  - Purpose: Establish a runnable backend service for MusicPlayer.
  - _Requirements: 5.1_

- [x] 2. Add authentication domain
  - Files: `auth/`, `user/`, `config/SecurityConfig.java`
  - Implement registration, login, logout, current-user lookup, BCrypt password hashing, and opaque Bearer tokens.
  - Purpose: Protect private music resources per user.
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.2_

- [x] 3. Add private music upload and streaming
  - Files: `music/`, `api/MusicController.java`
  - Implement multipart upload, metadata persistence, owner-scoped list/detail/delete, and stream endpoint.
  - Purpose: Provide NetEase-cloud-drive-like personal music storage.
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 4. Add personal playlist management
  - Files: `playlist/`, `api/PlaylistController.java`
  - Implement playlist CRUD and add/remove owned music tracks.
  - Purpose: Let users organize uploaded music.
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 5. Add favorites collection
  - Files: `favorite/`, `api/FavoriteController.java`
  - Implement add/list/remove favorites for owned music.
  - Purpose: Provide a default favorites list for quick playback.
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 6. Add local runtime configuration
  - Files: `application.properties`, `.mvn/local-settings.xml`, `.gitignore`
  - Configure local H2 database, upload limits, storage root, CORS, and project-local Maven cache.
  - Purpose: Make the service easy to run in a restricted local environment.
  - _Requirements: 5.1_

- [ ] 7. Add focused integration tests
  - Files: `src/test/java/...`
  - Cover register/login, upload, playlist add/remove, favorite add/remove, and owner isolation.
  - Purpose: Lock the API contract before MusicPlayer integration.
  - _Requirements: All_

- [ ] 8. Integrate MusicPlayer client
  - Files: `MusicPlayer/src/**`
  - Add server config, login flow, token persistence, upload UI, library sync calls, playlist sync, and favorite sync.
  - Purpose: Connect the desktop client to MusicServer.
  - _Requirements: All_

- [ ] 9. Production hardening
  - Files: `MusicServer/src/main/resources/db/migration/**`, deployment docs
  - Add Flyway migrations, PostgreSQL profile, upload quotas, metadata extraction, range-request tests, and deployment guide.
  - Purpose: Move from local initialization to durable self-hosted operation.
  - _Requirements: All_
