import { router } from '@kit.ArkUI';
import { Favorite, MusicTrack, Playlist } from '../model/Models';
import { MusicRepository } from '../repository/MusicRepository';
import { MusicServerClient } from '../network/MusicServerClient';
import { PlayerController } from '../player/PlayerController';

@Entry
@Component
struct LibraryPage {
  @State tracks: MusicTrack[] = []
  @State favorites: Favorite[] = []
  @State playlists: Playlist[] = []
  @State loading: boolean = false
  @State errorMessage: string = ''
  @State playlistName: string = ''

  private repository: MusicRepository = MusicRepository.getInstance();
  private client: MusicServerClient = MusicServerClient.getInstance();

  aboutToAppear(): void {
    this.refresh();
  }

  private async refresh(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      const values = await Promise.all([
        this.repository.library(),
        this.repository.favorites(),
        this.repository.playlists()
      ]);
      this.tracks = values[0] as MusicTrack[];
      this.favorites = values[1] as Favorite[];
      this.playlists = values[2] as Playlist[];
    } catch (error) {
      this.errorMessage = String(error);
    } finally {
      this.loading = false;
    }
  }

  private isFavorite(track: MusicTrack): boolean {
    return this.favorites.some((favorite) => favorite.music.musicId === track.musicId);
  }

  private async toggleFavorite(track: MusicTrack): Promise<void> {
    try {
      this.favorites = await this.repository.toggleFavorite(track, this.isFavorite(track));
    } catch (error) {
      this.errorMessage = String(error);
    }
  }

  private async play(track: MusicTrack): Promise<void> {
    const index = this.tracks.findIndex((item) => item.id === track.id);
    await PlayerController.getInstance().playQueue(this.tracks, Math.max(0, index));
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  private async createPlaylist(): Promise<void> {
    const name = this.playlistName.trim();
    if (name.length === 0) {
      return;
    }
    try {
      await this.repository.createPlaylist(name);
      this.playlistName = '';
      this.playlists = await this.repository.playlists();
    } catch (error) {
      this.errorMessage = String(error);
    }
  }

  private async logout(): Promise<void> {
    await this.client.logout();
    router.replaceUrl({ url: 'pages/LoginPage' });
  }

  @Builder
  private trackRow(track: MusicTrack) {
    Row({ space: 12 }) {
      Column({ space: 4 }) {
        Text(track.title || track.originalFilename || '未命名音轨')
          .fontSize(17)
          .fontWeight(FontWeight.Medium)
          .maxLines(1)
        Text((track.artist || '未知歌手') + (track.album ? ' · ' + track.album : ''))
          .fontSize(13)
          .fontColor('#777777')
          .maxLines(1)
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)

      Button(this.isFavorite(track) ? '♥' : '♡')
        .fontSize(22)
        .type(ButtonType.Normal)
        .backgroundColor(Color.Transparent)
        .fontColor(this.isFavorite(track) ? '#E74C3C' : '#666666')
        .onClick(() => this.toggleFavorite(track))

      Button('▶')
        .type(ButtonType.Circle)
        .width(42)
        .height(42)
        .onClick(() => this.play(track))
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
  }

  build() {
    Column() {
      Row() {
        Column() {
          Text('我的音乐')
            .fontSize(30)
            .fontWeight(FontWeight.Bold)
          Text(this.tracks.length + ' 首私有音乐 · ' + this.favorites.length + ' 首收藏')
            .fontSize(13)
            .fontColor('#777777')
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Button('刷新')
          .type(ButtonType.Normal)
          .onClick(() => this.refresh())
        Button('退出')
          .type(ButtonType.Normal)
          .onClick(() => this.logout())
      }
      .width('100%')
      .padding(20)

      if (this.errorMessage.length > 0) {
        Text(this.errorMessage)
          .fontColor('#D93025')
          .fontSize(13)
          .padding({ left: 20, right: 20, bottom: 8 })
      }

      if (this.loading) {
        LoadingProgress()
          .width(36)
          .height(36)
          .margin(16)
      }

      Scroll() {
        Column({ space: 20 }) {
          Column() {
            Text('曲库')
              .fontSize(20)
              .fontWeight(FontWeight.Bold)
              .width('100%')
              .textAlign(TextAlign.Start)
            List() {
              ForEach(this.tracks, (track: MusicTrack) => {
                ListItem() {
                  this.trackRow(track)
                }
              }, (track: MusicTrack) => track.id)
            }
            .width('100%')
            .divider({ strokeWidth: 0.5, color: '#E5E5E5' })
          }

          Column({ space: 10 }) {
            Text('歌单')
              .fontSize(20)
              .fontWeight(FontWeight.Bold)
              .width('100%')
              .textAlign(TextAlign.Start)
            Row({ space: 8 }) {
              TextInput({ placeholder: '新建歌单名称', text: this.playlistName })
                .layoutWeight(1)
                .onChange((value: string) => this.playlistName = value)
              Button('创建')
                .onClick(() => this.createPlaylist())
            }
            ForEach(this.playlists, (playlist: Playlist) => {
              Row() {
                Text(playlist.name)
                  .fontSize(16)
                  .layoutWeight(1)
                Text((playlist.tracks?.length || 0) + ' 首')
                  .fontSize(13)
                  .fontColor('#777777')
              }
              .width('100%')
              .padding({ top: 8, bottom: 8 })
            }, (playlist: Playlist) => String(playlist.id))
          }
        }
        .padding({ left: 20, right: 20, bottom: 40 })
      }
      .layoutWeight(1)
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FAFAFA')
  }
}
