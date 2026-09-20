import { Favorite, MusicTrack, Playlist } from '../model/Models';
import { MusicServerClient } from '../network/MusicServerClient';

export class MusicRepository {
  private static instance: MusicRepository = new MusicRepository();
  private client: MusicServerClient = MusicServerClient.getInstance();

  static getInstance(): MusicRepository {
    return MusicRepository.instance;
  }

  async library(): Promise<MusicTrack[]> {
    return this.client.listMusic();
  }

  async favorites(): Promise<Favorite[]> {
    return this.client.listFavorites();
  }

  async playlists(): Promise<Playlist[]> {
    return this.client.listPlaylists();
  }

  async toggleFavorite(track: MusicTrack, isFavorite: boolean): Promise<Favorite[]> {
    if (!track.musicId) {
      return this.client.listFavorites();
    }
    return isFavorite
      ? this.client.removeFavorite(track.musicId)
      : this.client.addFavorite(track.musicId);
  }

  async createPlaylist(name: string): Promise<Playlist> {
    return this.client.createPlaylist(name);
  }

  async addToPlaylist(playlistId: number, track: MusicTrack): Promise<Playlist> {
    return this.client.addTrackToPlaylist(playlistId, track);
  }
}
