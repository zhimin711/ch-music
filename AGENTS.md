# Repository Guidelines

CH Music is a multi-client personal music system. Keep changes scoped to the relevant client or backend and preserve the MusicServer REST API contract across clients.

## Project Structure & Module Organization

- `MusicPlayer/`: Electron + Vue desktop app. Main, preload, and renderer code live under `src/`; static resources are in `resources/`.
- `AndroidMusicPlayer/`: Android app. Kotlin/Java code is in `app/src/main/`; flavor-specific code belongs in `app/src/normal/` or `app/src/fdroid/`; resources are under `res/`.
- `MusicServer/`: Spring Boot backend. Production code is in `src/main/java/com/chmusic/musicserver`; JUnit tests are in `src/test/java`.

## Build, Test, and Development Commands

```bash
cd MusicPlayer && npm install && npm run dev
cd MusicPlayer && npm run typecheck && npm run lint
cd MusicServer && MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml test
cd MusicServer && MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml spring-boot:run
cd AndroidMusicPlayer && ./gradlew assembleNormalDebug
cd AndroidMusicPlayer && ./gradlew lint
```

Use `npm run dev:web` for browser-only desktop UI work. Build Android's `fdroid` flavor separately when changing flavor-dependent code. The server starts locally on port 8080 by default.

## Coding Style & Naming Conventions

- Use TypeScript/Vue conventions in `MusicPlayer`: PascalCase components, camelCase variables, and the repository ESLint/Prettier configuration. Run `npm run format` only on intentional formatting changes.
- Use standard Spring Java formatting: four spaces, PascalCase classes, camelCase methods, and package names under `com.chmusic.musicserver`.
- Use Kotlin idioms in Android: four spaces, PascalCase types, camelCase members, immutable `val` by default, and Android resources named in `snake_case`.
- Keep API DTO field names synchronized across MusicServer and both clients; do not expose one user's music, cache, or profile data to another.

## Testing Guidelines

- Add backend tests beside the affected feature under `MusicServer/src/test/java`; name them `*Tests.java`.
- Run the focused Maven test class during backend work, then the full Maven test suite before handoff.
- For desktop and Android changes, run the available typecheck/lint or Gradle task. Include manual verification steps when platform tests are unavailable.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit-style subjects, for example `feat(musicserver): add cover URL handling` and `fix(netease): update API base URL`. Use `feat`, `fix`, `refactor`, or `docs` with a concise scope. Keep commits focused.

PRs should state the affected module, behavior change, validation commands, and any API/configuration impact. Link related issues when available; include screenshots or recordings for visible desktop or Android changes. Never commit tokens, passwords, local `.env` values, keystores, or generated runtime data.
