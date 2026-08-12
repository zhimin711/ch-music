# MusicServer Deployment

This guide covers a durable self-hosted MusicServer setup with PostgreSQL, Flyway migrations, persistent upload storage, and the optional NetEase public API sidecar.

## Runtime

- Java 17+
- PostgreSQL 15+
- Optional FFmpeg for transcoding
- Optional Node.js sidecar from `sidecars/netease-api`

Run production mode with:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar MusicServer.jar
```

## Database

Production uses PostgreSQL and Flyway. The `prod` profile enables Flyway and validates the JPA model against the migrated schema.

Required environment variables:

```bash
export MUSIC_DB_URL=jdbc:postgresql://localhost:5432/musicserver
export MUSIC_DB_USERNAME=musicserver
export MUSIC_DB_PASSWORD=change-me
```

Create the database and user before starting the service:

```sql
create database musicserver;
create user musicserver with encrypted password 'change-me';
grant all privileges on database musicserver to musicserver;
```

Flyway migration files live in `src/main/resources/db/migration`. The first production migration creates users, auth tokens, private music, playlists, favorites, and transcode cache metadata.

## Storage

Uploaded music is stored on disk. Point `MUSIC_STORAGE_ROOT` to a persistent directory that is backed up with the database.

```bash
export MUSIC_STORAGE_ROOT=/var/lib/musicserver/music-storage
export MUSIC_UPLOAD_MAX_TOTAL_SIZE=40GB
```

`MUSIC_UPLOAD_MAX_TOTAL_SIZE` is a per-user quota. Set it to `0B` only when you intentionally want unlimited uploads.

## Public NetEase Sidecar

MusicServer can proxy public NetEase endpoints through the internal Node.js sidecar. Clients should only call MusicServer, not the sidecar directly.

Local sidecar:

```bash
docker compose -f docker-compose.netease.yml up -d --build
export MUSIC_NETEASE_BASE_URL=http://127.0.0.1:3000
```

Docker internal network example:

```bash
export MUSIC_NETEASE_BASE_URL=http://netease-api:3000
```

The sidecar does not store NetEase cookies in this phase. Only public endpoints under `/api/netease/public/**` are proxied anonymously.

## Common Settings

```bash
export MUSIC_CORS_ALLOWED_ORIGINS=http://localhost:2389
export MUSIC_AUTH_TOKEN_TTL_DAYS=30
export MUSIC_NETEASE_ENABLED=true
export MUSIC_NETEASE_ANONYMOUS_PER_MINUTE=60
```

Optional transcoding:

```bash
export MUSIC_TRANSCODING_ENABLED=true
export MUSIC_TRANSCODING_FFMPEG_PATH=ffmpeg
```

Keep transcode cache storage separate from the original upload directory.

## Smoke Checks

Anonymous public NetEase proxy:

```bash
curl 'http://localhost:8080/api/netease/public/search?keywords=test'
```

Private APIs should still require login:

```bash
curl -i http://localhost:8080/api/music
```

Register and upload:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo1234","displayName":"Demo"}'
```

Check Flyway:

```sql
select version, description, success from flyway_schema_history order by installed_rank;
```

## Backups

Back up both PostgreSQL and `MUSIC_STORAGE_ROOT`. The database contains metadata and ownership; the storage directory contains original music files and avatars.

Do not restore one without the other unless you are intentionally repairing missing files.
