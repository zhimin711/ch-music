import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { User } from '../model/Models';

const STORE_NAME: string = 'ch_music_session';
const KEY_BASE_URL: string = 'base_url';
const KEY_TOKEN: string = 'access_token';
const KEY_USER: string = 'user_json';

export class SessionStore {
  private static instance: SessionStore = new SessionStore();
  private store?: preferences.Preferences;
  private baseUrlValue: string = 'http://127.0.0.1:8080';
  private tokenValue: string = '';
  private userValue?: User;

  static getInstance(): SessionStore {
    return SessionStore.instance;
  }

  async initialize(context: common.UIAbilityContext): Promise<void> {
    this.store = await preferences.getPreferences(context, { name: STORE_NAME });
    this.baseUrlValue = String(await this.store.get(KEY_BASE_URL, this.baseUrlValue));
    this.tokenValue = String(await this.store.get(KEY_TOKEN, ''));
    const userJson: string = String(await this.store.get(KEY_USER, ''));
    if (userJson.length > 0) {
      try {
        this.userValue = JSON.parse(userJson) as User;
      } catch (_) {
        this.userValue = undefined;
      }
    }
  }

  get baseUrl(): string {
    return this.baseUrlValue.replace(/\/+$/, '');
  }

  get token(): string {
    return this.tokenValue;
  }

  get user(): User | undefined {
    return this.userValue;
  }

  get isAuthenticated(): boolean {
    return this.tokenValue.length > 0;
  }

  async saveSession(baseUrl: string, token: string, user: User): Promise<void> {
    this.baseUrlValue = baseUrl.replace(/\/+$/, '');
    this.tokenValue = token;
    this.userValue = user;
    if (!this.store) {
      return;
    }
    await this.store.put(KEY_BASE_URL, this.baseUrlValue);
    await this.store.put(KEY_TOKEN, this.tokenValue);
    await this.store.put(KEY_USER, JSON.stringify(user));
    await this.store.flush();
  }

  async updateBaseUrl(baseUrl: string): Promise<void> {
    this.baseUrlValue = baseUrl.replace(/\/+$/, '');
    if (this.store) {
      await this.store.put(KEY_BASE_URL, this.baseUrlValue);
      await this.store.flush();
    }
  }

  async clear(): Promise<void> {
    this.tokenValue = '';
    this.userValue = undefined;
    if (!this.store) {
      return;
    }
    await this.store.delete(KEY_TOKEN);
    await this.store.delete(KEY_USER);
    await this.store.flush();
  }
}
