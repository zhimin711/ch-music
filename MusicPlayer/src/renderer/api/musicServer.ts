import axios, { AxiosHeaders } from 'axios';

import type {
  MusicServerAuthResponse,
  MusicServerFavorite,
  MusicServerMusic,
  MusicServerPlaylist,
  MusicServerTranscodeCapabilities,
  MusicServerTranscodeStatus,
  MusicServerUser
} from '@/types/musicServer';

const DEFAULT_BASE_URL = 'http://localhost:8080';
const PACKAGED_BASE_URL = import.meta.env.VITE_MUSIC_SERVER_BASE_URL || DEFAULT_BASE_URL;
const TOKEN_KEY = 'musicServerToken';

const normalizeBaseUrl = (baseUrl: string) => {
  const trimmed = baseUrl.trim() || DEFAULT_BASE_URL;
  return trimmed.replace(/\/+$/, '');
};

export const getMusicServerBaseUrl = () => normalizeBaseUrl(PACKAGED_BASE_URL);

export const getMusicServerToken = () => localStorage.getItem(TOKEN_KEY) || '';

export const setMusicServerToken = (token: string) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
};

const musicServerRequest = axios.create({
  timeout: 30000
});

musicServerRequest.interceptors.request.use((config) => {
  config.baseURL = getMusicServerBaseUrl();
  const token = getMusicServerToken();
  if (token) {
    config.headers = AxiosHeaders.from(config.headers);
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

export function buildMusicServerStreamUrl(musicId: number, profileId?: string) {
  const baseUrl = getMusicServerBaseUrl();
  const token = getMusicServerToken();
  const url = new URL(`/api/music/${musicId}/stream`, baseUrl);
  if (profileId && profileId !== 'original') {
    url.searchParams.set('profile', profileId);
  }
  if (token) {
    url.searchParams.set('access_token', token);
  }
  return url.toString();
}

export function registerMusicServer(payload: {
  username: string;
  password: string;
  displayName?: string;
}) {
  return musicServerRequest.post<MusicServerAuthResponse>('/api/auth/register', payload);
}

export function loginMusicServer(payload: { username: string; password: string }) {
  return musicServerRequest.post<MusicServerAuthResponse>('/api/auth/login', payload);
}

export function logoutMusicServer() {
  return musicServerRequest.post<void>('/api/auth/logout');
}

export function getMusicServerMe() {
  return musicServerRequest.get<MusicServerUser>('/api/auth/me');
}

export function updateMusicServerMe(payload: { displayName: string; avatarUrl?: string | null }) {
  return musicServerRequest.put<MusicServerUser>('/api/auth/me', payload);
}

export function uploadMusicServerAvatar(formData: FormData) {
  return musicServerRequest.post<MusicServerUser>('/api/auth/me/avatar', formData, {
    timeout: 0
  });
}

export function listMusicServerMusic() {
  return musicServerRequest.get<MusicServerMusic[]>('/api/music');
}

export function uploadMusicServerMusic(formData: FormData) {
  return musicServerRequest.post<MusicServerMusic>('/api/music', formData, {
    timeout: 0
  });
}

export function deleteMusicServerMusic(musicId: number) {
  return musicServerRequest.delete<void>(`/api/music/${musicId}`);
}

export function getMusicServerTranscodeCapabilities() {
  return musicServerRequest.get<MusicServerTranscodeCapabilities>('/api/music/transcode-capabilities');
}

export function prepareMusicServerTranscode(musicId: number, profileId: string) {
  return musicServerRequest.post<MusicServerTranscodeStatus>(
    `/api/music/${musicId}/transcodes/${profileId}`
  );
}

export function getMusicServerTranscodeStatus(musicId: number, profileId: string) {
  return musicServerRequest.get<MusicServerTranscodeStatus>(
    `/api/music/${musicId}/transcodes/${profileId}`
  );
}

export function listMusicServerPlaylists() {
  return musicServerRequest.get<MusicServerPlaylist[]>('/api/playlists');
}

export function getMusicServerPlaylist(playlistId: number) {
  return musicServerRequest.get<MusicServerPlaylist>(`/api/playlists/${playlistId}`);
}

export function createMusicServerPlaylist(payload: { name: string; description?: string }) {
  return musicServerRequest.post<MusicServerPlaylist>('/api/playlists', payload);
}

export function updateMusicServerPlaylist(
  playlistId: number,
  payload: { name: string; description?: string }
) {
  return musicServerRequest.put<MusicServerPlaylist>(`/api/playlists/${playlistId}`, payload);
}

export function deleteMusicServerPlaylist(playlistId: number) {
  return musicServerRequest.delete<void>(`/api/playlists/${playlistId}`);
}

export function addMusicServerPlaylistTrack(playlistId: number, musicId: number) {
  return musicServerRequest.post<MusicServerPlaylist>(`/api/playlists/${playlistId}/tracks`, {
    musicId
  });
}

export function addMusicServerPlaylistExternalTrack(
  playlistId: number,
  payload: {
    source: string;
    externalId: string;
    title: string;
    artist?: string | null;
    album?: string | null;
    picUrl?: string | null;
    duration?: number | null;
  }
) {
  return musicServerRequest.post<MusicServerPlaylist>(`/api/playlists/${playlistId}/tracks`, payload);
}

export function removeMusicServerPlaylistTrack(playlistId: number, trackId: number) {
  return musicServerRequest.delete<MusicServerPlaylist>(
    `/api/playlists/${playlistId}/tracks/${trackId}`
  );
}

export function listMusicServerFavorites() {
  return musicServerRequest.get<MusicServerFavorite[]>('/api/favorites');
}

export function addMusicServerFavorite(musicId: number) {
  return musicServerRequest.post<MusicServerFavorite[]>(`/api/favorites/${musicId}`);
}

export function addMusicServerExternalFavorite(payload: {
  source: string;
  externalId: string;
  title: string;
  artist?: string | null;
  album?: string | null;
  picUrl?: string | null;
  duration?: number | null;
}) {
  return musicServerRequest.post<MusicServerFavorite[]>('/api/favorites', payload);
}

export function removeMusicServerFavorite(musicId: number) {
  return musicServerRequest.delete<MusicServerFavorite[]>(`/api/favorites/${musicId}`);
}

export function removeMusicServerExternalFavorite(source: string, externalId: string) {
  return musicServerRequest.delete<MusicServerFavorite[]>('/api/favorites/external', {
    params: { source, externalId }
  });
}
