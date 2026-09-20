import { router } from '@kit.ArkUI';
import { SessionStore } from '../data/SessionStore';

@Entry
@Component
struct Index {
  aboutToAppear(): void {
    const target: string = SessionStore.getInstance().isAuthenticated
      ? 'pages/LibraryPage'
      : 'pages/LoginPage';
    router.replaceUrl({ url: target });
  }

  build() {
    Column({ space: 16 }) {
      Text('CH Music')
        .fontSize(30)
        .fontWeight(FontWeight.Bold)
      LoadingProgress()
        .width(42)
        .height(42)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
