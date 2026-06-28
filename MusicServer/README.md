# MusicServer

Personal music resource backend for the CH Music desktop client.

## Run Locally

```bash
MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml spring-boot:run
```

The service starts on `http://localhost:8080`.

## API Surface

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/music`
- `POST /api/music`
- `GET /api/music/{musicId}`
- `GET /api/music/{musicId}/stream`
- `DELETE /api/music/{musicId}`
- `GET /api/playlists`
- `POST /api/playlists`
- `GET /api/playlists/{playlistId}`
- `PUT /api/playlists/{playlistId}`
- `DELETE /api/playlists/{playlistId}`
- `POST /api/playlists/{playlistId}/tracks`
- `DELETE /api/playlists/{playlistId}/tracks/{musicId}`
- `GET /api/favorites`
- `POST /api/favorites/{musicId}`
- `DELETE /api/favorites/{musicId}`

Protected endpoints use:

```http
Authorization: Bearer <accessToken>
```

## Local Data

- H2 database: `MusicServer/.local/musicserver-db`
- Uploaded music: `MusicServer/.local/music-storage`

Both paths are ignored by Git.
