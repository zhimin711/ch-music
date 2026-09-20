import { PlayerController, PlayerSnapshot } from '../player/PlayerController';

@Entry
@Component
struct PlayerPage {
  @State title: string = '未播放'
  @State artist: string = ''
  @State playing: boolean = false
  @State position: number = 0
  @State duration: number = 1
  @State queueLabel: string = ''
  @State errorMessage: string = ''
  private timerId: number = -1
  private controller: PlayerController = PlayerController.getInstance();

  aboutToAppear(): void {
    this.sync();
    this.timerId = setInterval(() => this.sync(), 500);
  }

  aboutToDisappear(): void {
    if (this.timerId >= 0) {
      clearInterval(this.timerId);
      this.timerId = -1;
    }
  }

  private sync(): void {
    const snapshot: PlayerSnapshot = this.controller.snapshot();
    this.title = snapshot.track?.title || snapshot.track?.originalFilename || '未播放';
    this.artist = snapshot.track?.artist || '未知歌手';
    this.playing = snapshot.playing;
    this.position = snapshot.position;
    this.duration = Math.max(1, snapshot.duration || snapshot.track?.duration || 1);
    this.queueLabel = snapshot.queueSize > 0
      ? (snapshot.queueIndex + 1) + ' / ' + snapshot.queueSize
      : '';
    this.errorMessage = snapshot.error;
  }

  private formatTime(ms: number): string {
    const total = Math.floor(ms / 1000);
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
  }

  build() {
    Column({ space: 22 }) {
      Blank().height(40)

      Stack() {
        Text('♪')
          .fontSize(90)
          .fontColor('#3867D6')
      }
      .width(220)
      .height(220)
      .backgroundColor('#EEF3FF')
      .borderRadius(28)

      Text(this.title)
        .fontSize(26)
        .fontWeight(FontWeight.Bold)
        .maxLines(2)
        .textAlign(TextAlign.Center)

      Text(this.artist)
        .fontSize(16)
        .fontColor('#777777')

      if (this.queueLabel.length > 0) {
        Text('播放队列 ' + this.queueLabel)
          .fontSize(13)
          .fontColor('#999999')
      }

      Column({ space: 6 }) {
        Slider({
          value: this.position,
          min: 0,
          max: this.duration,
          step: 1000,
          style: SliderStyle.OutSet
        })
          .onChange((value: number, mode: SliderChangeMode) => {
            this.position = value;
            if (mode === SliderChangeMode.End) {
              this.controller.seek(value);
            }
          })

        Row() {
          Text(this.formatTime(this.position))
            .fontSize(12)
            .fontColor('#777777')
          Blank()
          Text(this.formatTime(this.duration))
            .fontSize(12)
            .fontColor('#777777')
        }
        .width('100%')
      }
      .width('86%')

      Row({ space: 24 }) {
        Button('⏮')
          .type(ButtonType.Circle)
          .width(56)
          .height(56)
          .fontSize(22)
          .onClick(() => this.controller.previous())
        Button(this.playing ? 'Ⅱ' : '▶')
          .type(ButtonType.Circle)
          .width(72)
          .height(72)
          .fontSize(26)
          .onClick(() => this.controller.toggle())
        Button('⏭')
          .type(ButtonType.Circle)
          .width(56)
          .height(56)
          .fontSize(22)
          .onClick(() => this.controller.next())
      }

      if (this.errorMessage.length > 0) {
        Text(this.errorMessage)
          .fontColor('#D93025')
          .fontSize(13)
          .padding(16)
      }

      Blank()
    }
    .width('100%')
    .height('100%')
    .padding(20)
    .alignItems(HorizontalAlign.Center)
  }
}
