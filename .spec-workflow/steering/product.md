# Product Overview

## Product Purpose
CH Music provides a desktop-first personal music experience. MusicPlayer is the computer client, and MusicServer is the private backend that stores accounts, uploaded private music, personal playlists, and favorites.

## Target Users
Primary users are individuals who want their own music library to follow them across machines without depending on a public music platform.

## Key Features
1. **User Login**: Account registration, login, logout, and current user lookup for MusicPlayer.
2. **Private Music Cloud**: Upload personal audio files and stream them back to the authenticated owner.
3. **Personal Playlists**: Create, update, delete, and manage tracks in user-owned playlists.
4. **Favorites**: Maintain a default favorites collection for quick access.

## Business Objectives
- Make MusicPlayer usable with a self-hosted personal music backend.
- Keep private music data isolated by account.
- Provide a small, understandable API surface before adding sync and multi-device features.

## Success Metrics
- MusicPlayer can register/login and attach a Bearer token to backend requests.
- A user can upload, list, stream, favorite, and place private songs into playlists.
- Backend can start locally with no external database required.

## Product Principles
1. **Private by Default**: User music is visible only to the owner.
2. **Desktop Client Friendly**: APIs favor simple JSON and multipart calls that Electron can consume directly.
3. **Self-hostable First**: Local development uses embedded storage, while production can move to PostgreSQL.

## Monitoring & Visibility
- **Dashboard Type**: None for the initial backend; MusicPlayer is the main user interface.
- **Real-time Updates**: Future versions can add WebSocket or polling-based library sync.
- **Key Metrics Displayed**: Future server health, library size, and upload progress.

## Future Vision
MusicServer can grow into a private music cloud with lyrics, transcoding, device sync, remote access, search, and background metadata extraction.
