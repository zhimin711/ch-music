import { AbilityConstant, common, UIAbility, Want } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';
import { SessionStore } from '../data/SessionStore';
import { PlayerController } from '../player/PlayerController';

export default class EntryAbility extends UIAbility {
  async onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): Promise<void> {
    await SessionStore.getInstance().initialize(this.context as common.UIAbilityContext);
    await PlayerController.getInstance().initialize(this.context as common.UIAbilityContext);
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index');
  }

  async onDestroy(): Promise<void> {
    await PlayerController.getInstance().release();
  }
}
