# netease-api sidecar

This sidecar runs `netease_cloud_music_api` as an internal Node.js capability service.
Clients should never call it directly. MusicServer calls it through the configured
`music.netease.base-url`.

## Local Docker

```bash
docker build -t ch-music/netease-api ./sidecars/netease-api
docker run --rm -p 127.0.0.1:3000:3000 ch-music/netease-api
```

Then keep MusicServer configured with:

```properties
music.netease.base-url=http://127.0.0.1:3000
```

## Compose / Internal Network

Expose the sidecar only to the application network and configure MusicServer with:

```properties
music.netease.base-url=http://netease-api:3000
```

Do not publish the sidecar port publicly in production.
