import { http } from '@kit.NetworkKit';
import { AuthResponse, Favorite, MusicTrack, Playlist, User, ApiError } from '../model/Models';
import { SessionStore } from '../data/SessionStore';

export class MusicServerError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export class MusicServerClient {
  private static instance: MusicServerClient = new MusicServerClient();
  private session: SessionStore = SessionStore.getInstance();

  static getInstance(): MusicServerClient {
    return MusicServerClient.instance;
  }

  absoluteUrl(path?: string): string {
    if (!path) {
      return '';
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    return this.session.baseUrl + (path.startsWith('/') ? path : '/' + path);
  }

  streamUrl(track: MusicTrack): string {
    const variant = track.playback?.variants?.find((item) => item.profileId === 'original');
    return this.absoluteUrl(variant?.streamUrl || track.streamUrl);
  }

  authorizationHeaders(): Record<string, string> {
    const headers: Record<string, string> = {};
    if (this.session.token.length > 0) {
      headers['Authorization'] = 'Bearer ' + this.session.token;
    }
    return headers;
  }

  async login(baseUrl: string, username: string, password: string): Promise<AuthResponse> {
    await this.session.updateBaseUrl(baseUrl);
    const response = await this.request<AuthResponse>(
      '/api/auth/login',
      http.RequestMethod.POST,
      { username, password, displayName: null },
      false
    );
    await this.session.saveSession(baseUrl, response.accessToken, response.user);
    return response;
  }

  async register(baseUrl: string, username: string, password: string, displayName: string): Promise<AuthResponse> {
    await this.session.updateBaseUrl(baseUrl);
    const response = await this.request<AuthResponse>(
      '/api/auth/register',
      http.RequestMethod.POST,
      { username, password, displayName: displayName.length > 0 ? displayName : null },
      false
    );
    await this.session.saveSession(baseUrl, response.accessToken, response.user);
    return response;
  }

  async logout(): Promise<void> {
    try {
      await this.request<void>('/api/auth/logout', http.RequestMethod.POST);
    } finally {
      await this.session.clear();
    }
  }

  async me(): Promise<User> {
    return this.request<User>('/api/auth/me', http.RequestMethod.GET);
  }

  async listMusic(): Promise<MusicTrack[]> {
    return this.request<MusicTrack[]>('/api/music', http.RequestMethod.GET);
  }

  async listFavorites(): Promise<Favorite[]> {
    return this.request<Favorite[]>('/api/favorites', http.RequestMethod.GET);
  }

  async addFavorite(musicId: number): Promise<Favorite[]> {
    return this.request<Favorite[]>('/api/favorites/' + musicId, http.RequestMethod.POST);
  }

  async removeFavorite(musicId: number): Promise<Favorite[]> {
    return this.request<Favorite[]>('/api/favorites/' + musicId, http.RequestMethod.DELETE);
  }

  async listPlaylists(): Promise<Playlist[]> {
    return this.request<Playlist[]>('/api/playlists', http.RequestMethod.GET);
  }

  async playlistDetails(id: number): Promise<Playlist> {
    return this.request<Playlist>('/api/playlists/' + id, http.RequestMethod.GET);
  }

  async createPlaylist(name: string, description: string = ''): Promise<Playlist> {
    return this.request<Playlist>(
      '/api/playlists',
      http.RequestMethod.POST,
      { name, description: description.length > 0 ? description : null }
    );
  }

  async addTrackToPlaylist(playlistId: number, track: MusicTrack): Promise<Playlist> {
    return this.request<Playlist>(
      '/api/playlists/' + playlistId + '/tracks',
      http.RequestMethod.POST,
      {
        musicId: track.musicId || null,
        source: track.source,
        externalId: track.externalId || null,
        title: track.title,
        artist: track.artist || null,
        album: track.album || null,
        picUrl: track.picUrl || null,
        duration: track.duration || null
      }
    );
  }

  private async request<T>(
    path: string,
    method: http.RequestMethod,
    body?: Object,
    authenticated: boolean = true
  ): Promise<T> {
    const request = http.createHttp();
    const headers: Record<string, string> = { 'Accept': 'application/json' };
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    if (authenticated && this.session.token.length > 0) {
      headers['Authorization'] = 'Bearer ' + this.session.token;
    }

    try {
      const response = await request.request(this.absoluteUrl(path), {
        method,
        header: headers,
        extraData: body === undefined ? undefined : JSON.stringify(body),
        expectDataType: http.HttpDataType.STRING,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      const text: string = String(response.result ?? '');
      if (response.responseCode < 200 || response.responseCode >= 300) {
        let message: string = 'MusicServer request failed (' + response.responseCode + ')';
        if (text.length > 0) {
          try {
            const apiError = JSON.parse(text) as ApiError;
            message = apiError.message || apiError.error || message;
          } catch (_) {
            message = text;
          }
        }
        if (response.responseCode === 401 && authenticated) {
          await this.session.clear();
        }
        throw new MusicServerError(response.responseCode, message);
      }
      if (text.length === 0) {
        return undefined as T;
      }
      return JSON.parse(text) as T;
    } finally {
      request.destroy();
    }
  }
}
