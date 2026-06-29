# MusicServer

MusicServer is the self-hosted private music backend for CH Music desktop and Android clients. It provides account auth, private music upload, owner-scoped playlists/favorites, authenticated Range streaming, optional FFmpeg transcoding, and server-side transcode cache.

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
- `GET /api/music/{musicId}/stream?profile={profileId}`
- `GET /api/music/transcode-capabilities`
- `POST /api/music/{musicId}/transcodes/{profileId}`
- `GET /api/music/{musicId}/transcodes/{profileId}`
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

## Streaming And Transcoding

`GET /api/music/{musicId}/stream` streams the original uploaded file. It supports standard single-range requests when `music.streaming.range.enabled=true`:

- no `Range` header returns `200 OK`
- valid single ranges return `206 Partial Content`
- invalid or out-of-bounds ranges return `416 Range Not Satisfiable`

The `profile` query parameter streams a ready transcode variant, for example:

```http
GET /api/music/12/stream?profile=aac-128
Range: bytes=0-1048575
```

Transcoding is optional and disabled by default. If FFmpeg is not available or no profile is configured, original streaming still works and clients can discover the unavailable state through `GET /api/music/transcode-capabilities`.

Example local configuration:

```properties
music.streaming.range.enabled=true
music.transcoding.enabled=true
music.transcoding.ffmpeg-path=ffmpeg
music.transcoding.max-concurrency=1
music.transcoding.temp-root=./.local/transcode-temp
music.transcoding.cache-root=./.local/transcode-cache
music.transcoding.cache-max-size=20GB
music.transcoding.profiles[0].id=aac-128
music.transcoding.profiles[0].label=AAC 128k
music.transcoding.profiles[0].content-type=audio/aac
music.transcoding.profiles[0].bitrate-kbps=128
music.transcoding.profiles[0].extension=.aac
music.transcoding.profiles[0].args=-vn -c:a aac -b:a 128k
music.transcoding.profiles[0].offline-cacheable=true
```

Transcode workflow:

1. client reads `/api/music/transcode-capabilities` and `music.playback.variants`
2. client calls `POST /api/music/{musicId}/transcodes/{profileId}`
3. client polls `GET /api/music/{musicId}/transcodes/{profileId}`
4. when the state is `READY`, client plays `/api/music/{musicId}/stream?profile={profileId}`

## Local Data

- H2 database: `MusicServer/.local/musicserver-db`
- Uploaded music: `MusicServer/.local/music-storage`
- Transcode temp files: `MusicServer/.local/transcode-temp`
- Transcode cache files: `MusicServer/.local/transcode-cache`

These paths are ignored by Git.

Never point transcode cache cleanup at the original upload directory. Server cleanup only removes transcode variants and temporary files.

## Client Offline Cache Notes

Desktop and Android clients keep their own offline cache indexes. MusicServer does not expose client cache files; it only provides stable stream URLs, `fileSize`, `checksum`, `updatedAt`, and playback capability metadata. Clients should namespace cache entries by server URL, user id, music id, and profile id, then mark entries stale when checksum changes or a song disappears from `/api/music`.

## Troubleshooting

- Seek fails or playback restarts: verify the response includes `Accept-Ranges: bytes` and that proxies do not strip the `Range` header.
- Transcode profile is unavailable: check `music.transcoding.enabled`, `music.transcoding.ffmpeg-path`, and `GET /api/music/transcode-capabilities`.
- Disk usage grows: lower `music.transcoding.cache-max-size` or clear `MusicServer/.local/transcode-cache`.
- Client cache is stale: refresh the private library; clients compare MusicServer checksum metadata and will hide, delete, or mark stale entries.
