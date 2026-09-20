import { router } from '@kit.ArkUI';
import { MusicServerClient } from '../network/MusicServerClient';
import { SessionStore } from '../data/SessionStore';

@Entry
@Component
struct LoginPage {
  @State baseUrl: string = SessionStore.getInstance().baseUrl
  @State username: string = ''
  @State password: string = ''
  @State displayName: string = ''
  @State registerMode: boolean = false
  @State loading: boolean = false
  @State errorMessage: string = ''

  private client: MusicServerClient = MusicServerClient.getInstance();

  private async submit(): Promise<void> {
    if (this.baseUrl.trim().length === 0 || this.username.trim().length < 3 || this.password.length < 8) {
      this.errorMessage = '请填写服务器地址；用户名至少 3 位，密码至少 8 位。';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    try {
      if (this.registerMode) {
        await this.client.register(this.baseUrl.trim(), this.username.trim(), this.password, this.displayName.trim());
      } else {
        await this.client.login(this.baseUrl.trim(), this.username.trim(), this.password);
      }
      router.replaceUrl({ url: 'pages/LibraryPage' });
    } catch (error) {
      this.errorMessage = String(error);
    } finally {
      this.loading = false;
    }
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        Text('CH Music')
          .fontSize(34)
          .fontWeight(FontWeight.Bold)
        Text(this.registerMode ? '创建 MusicServer 账号' : '登录 MusicServer')
          .fontSize(20)
          .fontColor('#666666')

        TextInput({ placeholder: 'http://192.168.1.10:8080', text: this.baseUrl })
          .onChange((value: string) => this.baseUrl = value)
        TextInput({ placeholder: '用户名', text: this.username })
          .onChange((value: string) => this.username = value)
        TextInput({ placeholder: '密码', text: this.password })
          .type(InputType.Password)
          .onChange((value: string) => this.password = value)

        if (this.registerMode) {
          TextInput({ placeholder: '显示名称（可选）', text: this.displayName })
            .onChange((value: string) => this.displayName = value)
        }

        if (this.errorMessage.length > 0) {
          Text(this.errorMessage)
            .fontColor('#D93025')
            .fontSize(14)
        }

        Button(this.loading ? '处理中…' : (this.registerMode ? '注册并登录' : '登录'))
          .width('100%')
          .enabled(!this.loading)
          .onClick(() => this.submit())

        Button(this.registerMode ? '已有账号？返回登录' : '没有账号？注册')
          .type(ButtonType.Normal)
          .backgroundColor(Color.Transparent)
          .fontColor('#3867D6')
          .onClick(() => {
            this.registerMode = !this.registerMode;
            this.errorMessage = '';
          })
      }
      .padding(28)
      .width('100%')
      .constraintSize({ maxWidth: 560 })
    }
    .width('100%')
    .height('100%')
  }
}
