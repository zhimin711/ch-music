export interface MusicServerUser {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
}

export interface MusicServerAuthResponse {
  tokenType: 'Bearer';
  accessToken: string;
  expiresAt: string;
  user: MusicServerUser;
}

export type MusicServerTranscodeState =
  | 'UNAVAILABLE'
  | 'NOT_REQUESTED'
  | 'QUEUED'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED'
  | 'STALE';

export type MusicServerTranscodeCapabilityStatus =
  | 'AVAILABLE'
  | 'DISABLED'
  | 'TOOL_UNAVAILABLE'
  | 'PROFILE_NOT_FOUND';

export type MusicServerOfflineCacheState =
  | 'queued'
  | 'downloading'
  | 'ready'
  | 'failed'
  | 'stale'
  | 'paused';

export interface MusicServerPlaybackVariant {
  profileId: string;
  label: string;
  contentType: string | null;
  bitrateKbps?: number | null;
  streamUrl: string | null;
  state: MusicServerTranscodeState;
  fileSize?: number | null;
  checksum?: string | null;
}

export interface MusicServerPlaybackCapabilities {
  supportsRange: boolean;
  supportsOriginal: boolean;
  supportsTranscoding: boolean;
  supportsOfflineCache: boolean;
  variants: MusicServerPlaybackVariant[];
}

export interface MusicServerTranscodeProfile {
  profileId: string;
  label: string;
  contentType: string | null;
  bitrateKbps?: number | null;
  extension: string | null;
  offlineCacheable: boolean;
}

export interface MusicServerTranscodeCapabilities {
  enabled: boolean;
  toolAvailable: boolean;
  status: MusicServerTranscodeCapabilityStatus;
  reason: string | null;
  profiles: MusicServerTranscodeProfile[];
}

export interface MusicServerTranscodeStatus {
  musicId: number;
  profileId: string;
  state: MusicServerTranscodeState;
  reason?: string | null;
  streamUrl?: string | null;
  retryAfterSeconds?: number | null;
  fileSize?: number | null;
  checksum?: string | null;
}

export interface MusicServerMusic {
  id: number | string;
  musicId?: number | null;
  trackId?: number | null;
  source?: 'musicServer' | 'netease' | string | null;
  externalId?: string | null;
  title: string;
  artist?: string | null;
  album?: string | null;
  picUrl?: string | null;
  duration?: number | null;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  checksum: string;
  createdAt: string;
  updatedAt?: string;
  streamUrl: string;
  playback?: MusicServerPlaybackCapabilities;
}

export interface MusicServerPlaylist {
  id: number;
  name: string;
  description?: string | null;
  createdAt: string;
  tracks: MusicServerMusic[];
}

export interface MusicServerFavorite {
  id: number;
  createdAt: string;
  music: MusicServerMusic;
}

export interface MusicServerOfflineCacheRequest {
  serverBaseUrl: string;
  userId: number;
  musicId: number;
  profileId?: string;
  title: string;
  checksum: string;
  fileSize: number;
  contentType: string;
  streamUrl: string;
  pinned?: boolean;
}

export interface MusicServerOfflineCacheEntry extends MusicServerOfflineCacheRequest {
  cacheKey: string;
  profileId: string;
  localPath: string;
  state: MusicServerOfflineCacheState;
  downloadedBytes: number;
  lastVerifiedAt?: number;
  pinned: boolean;
  error?: string;
}

export interface MusicServerOfflineCacheAddResponse {
  cacheKeys: string[];
  entries?: MusicServerOfflineCacheEntry[];
}

export interface MusicServerOfflineCacheSyncResult {
  activeKeys: string[];
  staleKeys: string[];
  removedKeys: string[];
  entries: MusicServerOfflineCacheEntry[];
}
