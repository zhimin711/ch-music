import { common } from '@kit.AbilityKit';
import { media } from '@kit.MediaKit';
import { avSession } from '@kit.AVSessionKit';
import { MusicTrack } from '../model/Models';
import { MusicServerClient } from '../network/MusicServerClient';

export class PlayerSnapshot {
  track?: MusicTrack;
  playing: boolean = false;
  position: number = 0;
  duration: number = 0;
  queueIndex: number = -1;
  queueSize: number = 0;
  error: string = '';
}

export class PlayerController {
  private static instance: PlayerController = new PlayerController();
  private client: MusicServerClient = MusicServerClient.getInstance();
  private player?: media.AVPlayer;
  private session?: avSession.AVSession;
  private queue: MusicTrack[] = [];
  private index: number = -1;
  private current?: MusicTrack;
  private playingValue: boolean = false;
  private positionValue: number = 0;
  private durationValue: number = 0;
  private errorValue: string = '';

  static getInstance(): PlayerController {
    return PlayerController.instance;
  }

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (!this.player) {
      this.player = await media.createAVPlayer();
      this.bindPlayerEvents();
    }
    if (!this.session) {
      this.session = await avSession.createAVSession(context, 'CH Music', 'audio');
      this.bindSessionEvents();
      await this.session.activate();
    }
  }

  snapshot(): PlayerSnapshot {
    const value = new PlayerSnapshot();
    value.track = this.current;
    value.playing = this.playingValue;
    value.position = this.positionValue;
    value.duration = this.durationValue;
    value.queueIndex = this.index;
    value.queueSize = this.queue.length;
    value.error = this.errorValue;
    return value;
  }

  async playQueue(queue: MusicTrack[], startIndex: number): Promise<void> {
    if (queue.length === 0) {
      return;
    }
    this.queue = queue.filter((item) => this.client.streamUrl(item).length > 0);
    if (this.queue.length === 0) {
      this.errorValue = '当前列表没有可播放的 MusicServer 音轨';
      return;
    }
    const requested = queue[startIndex];
    const mappedIndex = this.queue.findIndex((item) => item.id === requested.id);
    this.index = mappedIndex >= 0 ? mappedIndex : 0;
    await this.loadCurrent(true);
  }

  async play(): Promise<void> {
    if (this.player) {
      await this.player.play();
    }
  }

  async pause(): Promise<void> {
    if (this.player) {
      await this.player.pause();
    }
  }

  async toggle(): Promise<void> {
    if (this.playingValue) {
      await this.pause();
    } else {
      await this.play();
    }
  }

  seek(positionMs: number): void {
    if (this.player) {
      this.player.seek(Math.max(0, Math.floor(positionMs)));
    }
  }

  async next(): Promise<void> {
    if (this.queue.length === 0) {
      return;
    }
    this.index = (this.index + 1) % this.queue.length;
    await this.loadCurrent(true);
  }

  async previous(): Promise<void> {
    if (this.queue.length === 0) {
      return;
    }
    this.index = (this.index - 1 + this.queue.length) % this.queue.length;
    await this.loadCurrent(true);
  }

  async release(): Promise<void> {
    if (this.player) {
      await this.player.release();
      this.player = undefined;
    }
    if (this.session) {
      await this.session.destroy();
      this.session = undefined;
    }
  }

  private async loadCurrent(autoPlay: boolean): Promise<void> {
    if (!this.player || this.index < 0 || this.index >= this.queue.length) {
      return;
    }
    this.current = this.queue[this.index];
    this.positionValue = 0;
    this.durationValue = this.current.duration || 0;
    this.errorValue = '';

    try {
      if (this.player.state !== 'idle') {
        await this.player.reset();
      }
      const url = this.client.streamUrl(this.current);
      const source = media.createMediaSourceWithUrl(url, this.client.authorizationHeaders());
      const strategy: media.PlaybackStrategy = { preferredBufferDuration: 12 };
      this.player.setMediaSource(source, strategy);
      await this.player.prepare();
      await this.updateSessionMetadata();
      if (autoPlay) {
        await this.player.play();
      }
    } catch (error) {
      this.errorValue = '播放失败：' + String(error);
      await this.reportPlaybackState();
    }
  }

  private bindPlayerEvents(): void {
    if (!this.player) {
      return;
    }
    this.player.on('stateChange', (state) => {
      this.playingValue = state === 'playing';
      if (state === 'completed') {
        this.next();
      }
      this.reportPlaybackState();
    });
    this.player.on('timeUpdate', (time: number) => {
      this.positionValue = time;
      this.reportPlaybackState();
    });
    this.player.on('durationUpdate', (duration: number) => {
      this.durationValue = duration;
      this.updateSessionMetadata();
    });
    this.player.on('error', (error) => {
      this.errorValue = '播放器错误：' + String(error);
      this.playingValue = false;
      this.reportPlaybackState();
    });
  }

  private bindSessionEvents(): void {
    if (!this.session) {
      return;
    }
    this.session.on('play', () => this.play());
    this.session.on('pause', () => this.pause());
    this.session.on('playNext', () => this.next());
    this.session.on('playPrevious', () => this.previous());
    this.session.on('seek', (time: number) => this.seek(time));
  }

  private async updateSessionMetadata(): Promise<void> {
    if (!this.session || !this.current) {
      return;
    }
    const metadata: avSession.AVMetadata = {
      assetId: this.current.id,
      title: this.current.title,
      artist: this.current.artist || '',
      album: this.current.album || '',
      duration: this.durationValue || this.current.duration || 0,
      mediaImage: this.client.absoluteUrl(this.current.picUrl)
    };
    await this.session.setAVMetadata(metadata);
    await this.reportPlaybackState();
  }

  private async reportPlaybackState(): Promise<void> {
    if (!this.session) {
      return;
    }
    try {
      await this.session.setAVPlaybackState({
        state: this.playingValue
          ? avSession.PlaybackState.PLAYBACK_STATE_PLAY
          : avSession.PlaybackState.PLAYBACK_STATE_PAUSE,
        position: {
          elapsedTime: this.positionValue,
          updateTime: new Date().getTime()
        }
      });
    } catch (_) {
    }
  }
}
