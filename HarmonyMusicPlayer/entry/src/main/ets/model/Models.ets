export interface User {
  id: number
  username: string
  displayName?: string
  avatarUrl?: string
}

export interface AuthResponse {
  tokenType: string
  accessToken: string
  expiresAt: string
  user: User
}

export interface PlaybackVariant {
  profileId: string
  label: string
  contentType: string
  bitrateKbps?: number
  streamUrl: string
  state: string
  fileSize?: number
  checksum?: string
}

export interface PlaybackCapabilities {
  supportsRange: boolean
  supportsOriginal: boolean
  supportsTranscoding: boolean
  supportsOfflineCache: boolean
  variants: PlaybackVariant[]
}

export interface MusicTrack {
  id: string
  musicId?: number
  trackId?: number
  source: string
  externalId?: string
  title: string
  artist?: string
  album?: string
  picUrl?: string
  duration?: number
  originalFilename?: string
  contentType?: string
  fileSize: number
  checksum?: string
  createdAt?: string
  updatedAt?: string
  streamUrl?: string
  playback: PlaybackCapabilities
}

export interface Favorite {
  id: number
  createdAt: string
  music: MusicTrack
}

export interface Playlist {
  id: number
  name: string
  description?: string
  createdAt: string
  tracks: MusicTrack[]
}

export interface ApiError {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
}
