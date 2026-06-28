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
  streamUrl: string;
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
