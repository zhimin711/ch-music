import { createHash, randomUUID } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { pipeline } from 'node:stream/promises';

import axios from 'axios';
import { app, BrowserWindow, ipcMain } from 'electron';
import Store from 'electron-store';
import * as fs from 'fs';
import * as path from 'path';

import { getStore } from './config';

type MusicServerOfflineCacheState =
  | 'queued'
  | 'downloading'
  | 'ready'
  | 'failed'
  | 'stale'
  | 'paused';

type MusicServerOfflineCacheRequest = {
  serverBaseUrl: string;
  userId: number;
  musicId: number;
  profileId?: string;
  title: string;
  checksum: string;
  fileSize: number;
  contentType: string;
  streamUrl: string;
  pinned?: boolean;
  accessToken?: string;
};

type MusicServerOfflineCacheEntry = {
  cacheKey: string;
  serverBaseUrl: string;
  userId: number;
  musicId: number;
  profileId: string;
  title: string;
  checksum: string;
  fileSize: number;
  contentType: string;
  streamUrl: string;
  localPath: string;
  tempPath: string;
  state: MusicServerOfflineCacheState;
  downloadedBytes: number;
  pinned: boolean;
  createdAt: number;
  updatedAt: number;
  lastVerifiedAt?: number;
  error?: string;
  taskId?: string;
};

type MusicServerOfflineCachePublicEntry = Omit<
  MusicServerOfflineCacheEntry,
  'localPath' | 'tempPath' | 'streamUrl'
> & {
  playbackUrl?: string;
};

type MusicServerOfflineCacheStore = {
  entries: Record<string, MusicServerOfflineCacheEntry>;
};

type MusicServerOfflineCacheQuery = {
  serverBaseUrl?: string;
  userId?: number;
  musicId?: number;
  profileId?: string;
  cacheKey?: string;
  checksum?: string;
};

type MusicServerOfflineCacheSyncMusic = {
  id?: number | string;
  musicId?: number | null;
  checksum?: string | null;
};

type MusicServerOfflineCacheSyncResult = {
  activeKeys: string[];
  staleKeys: string[];
  removedKeys: string[];
  entries: MusicServerOfflineCachePublicEntry[];
};

const STORE_NAME = 'music-server-offline-cache';
const DEFAULT_PROFILE_ID = 'original';
const CACHE_DIR_NAME = 'music-server-private';
const TEMP_SUFFIX = '.part';
const MAX_CONCURRENT_DOWNLOADS = 2;

const AUDIO_EXTENSION_BY_CONTENT_TYPE: Record<string, string> = {
  'audio/mpeg': '.mp3',
  'audio/mp3': '.mp3',
  'audio/mp4': '.m4a',
  'audio/x-m4a': '.m4a',
  'audio/aac': '.aac',
  'audio/flac': '.flac',
  'audio/x-flac': '.flac',
  'audio/wav': '.wav',
  'audio/x-wav': '.wav',
  'audio/ogg': '.ogg',
  'audio/webm': '.webm'
};

class MusicServerOfflineCacheManager {
  private metadataStore: Store<MusicServerOfflineCacheStore>;
  private abortControllers = new Map<string, AbortController>();
  private activeDownloads = 0;
  private processTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.metadataStore = new Store<MusicServerOfflineCacheStore>({
      name: STORE_NAME,
      defaults: {
        entries: {}
      }
    });

    app.on('before-quit', () => {
      for (const [cacheKey, controller] of this.abortControllers.entries()) {
        controller.abort();
        const entry = this.getEntry(cacheKey);
        if (entry?.state === 'downloading') {
          this.saveEntry({ ...entry, state: 'paused', updatedAt: Date.now() });
        }
      }
    });
  }

  public initialize(): void {
    this.ensureRootDirectory();
    this.restoreInterruptedDownloads();
    this.registerIpcHandlers();
    this.scheduleQueue();
  }

  public async addItems(items: MusicServerOfflineCacheRequest[]): Promise<string[]> {
    const cacheKeys: string[] = [];
    for (const item of items) {
      const entry = await this.upsertEntry(item);
      cacheKeys.push(entry.cacheKey);
    }
    this.scheduleQueue();
    return cacheKeys;
  }

  public async pause(cacheKey: string): Promise<MusicServerOfflineCachePublicEntry | null> {
    const entry = this.getEntry(cacheKey);
    if (!entry) return null;

    const controller = this.abortControllers.get(cacheKey);
    if (controller) {
      controller.abort();
      this.abortControllers.delete(cacheKey);
    }

    const pausedEntry = { ...entry, state: 'paused' as const, updatedAt: Date.now() };
    this.saveEntry(pausedEntry);
    this.emitStateChange(pausedEntry);
    return this.toPublicEntry(pausedEntry);
  }

  public async resume(cacheKey: string): Promise<MusicServerOfflineCachePublicEntry | null> {
    const entry = this.getEntry(cacheKey);
    if (!entry) return null;
    if (!['paused', 'failed', 'stale'].includes(entry.state)) {
      return this.toPublicEntry(entry);
    }

    const resumedEntry = {
      ...entry,
      state: 'queued' as const,
      error: undefined,
      taskId: randomUUID(),
      updatedAt: Date.now()
    };
    this.saveEntry(resumedEntry);
    this.emitStateChange(resumedEntry);
    this.scheduleQueue();
    return this.toPublicEntry(resumedEntry);
  }

  public async remove(cacheKey: string): Promise<boolean> {
    const entry = this.getEntry(cacheKey);
    if (!entry) return false;

    const controller = this.abortControllers.get(cacheKey);
    if (controller) {
      controller.abort();
      this.abortControllers.delete(cacheKey);
    }

    await this.removeFileIfInsideRoot(entry.localPath);
    await this.removeFileIfInsideRoot(entry.tempPath);
    const entries = this.getEntries();
    delete entries[cacheKey];
    this.metadataStore.set('entries', entries);
    this.emitRemoved(cacheKey);
    return true;
  }

  public async getState(
    query: MusicServerOfflineCacheQuery
  ): Promise<MusicServerOfflineCachePublicEntry | null> {
    const entry = this.findEntry(query);
    if (!entry) return null;
    const validated = await this.validateEntry(entry, query.checksum);
    return this.toPublicEntry(validated);
  }

  public async getAll(
    query: MusicServerOfflineCacheQuery = {}
  ): Promise<MusicServerOfflineCachePublicEntry[]> {
    const matchedEntries = Object.values(this.getEntries()).filter((entry) =>
      this.matchesQuery(entry, query)
    );
    const validatedEntries = await Promise.all(
      matchedEntries.map((entry) => this.validateEntry(entry, query.checksum))
    );
    return validatedEntries.map((entry) => this.toPublicEntry(entry));
  }

  public async resolvePlaybackUrl(query: MusicServerOfflineCacheQuery): Promise<string | null> {
    const entry = this.findEntry(query);
    if (!entry) return null;

    const validated = await this.validateEntry(entry, query.checksum);
    if (validated.state !== 'ready') return null;
    return pathToFileURL(validated.localPath).toString();
  }

  public async syncIndex(payload: {
    serverBaseUrl: string;
    userId: number;
    musicList: MusicServerOfflineCacheSyncMusic[];
  }): Promise<MusicServerOfflineCacheSyncResult> {
    const activeMusic = new Map<number, string>();
    for (const music of payload.musicList) {
      const musicId = this.normalizeMusicId(music);
      if (musicId != null) {
        activeMusic.set(musicId, music.checksum ?? '');
      }
    }

    const activeKeys: string[] = [];
    const staleKeys: string[] = [];
    const removedKeys: string[] = [];
    const entries = this.getEntries();

    for (const entry of Object.values(entries)) {
      if (entry.serverBaseUrl !== payload.serverBaseUrl || entry.userId !== payload.userId) {
        continue;
      }

      const currentChecksum = activeMusic.get(entry.musicId);
      if (currentChecksum === undefined) {
        await this.remove(entry.cacheKey);
        removedKeys.push(entry.cacheKey);
        continue;
      }

      if (currentChecksum && currentChecksum !== entry.checksum) {
        const staleEntry = {
          ...entry,
          checksum: currentChecksum,
          state: 'stale' as const,
          error: 'CHECKSUM_CHANGED',
          updatedAt: Date.now()
        };
        this.saveEntry(staleEntry);
        this.emitStateChange(staleEntry);
        staleKeys.push(entry.cacheKey);
        continue;
      }

      activeKeys.push(entry.cacheKey);
    }

    return {
      activeKeys,
      staleKeys,
      removedKeys,
      entries: await this.getAll({
        serverBaseUrl: payload.serverBaseUrl,
        userId: payload.userId
      })
    };
  }

  private registerIpcHandlers(): void {
    ipcMain.handle('music-server-cache:add', async (_, items: MusicServerOfflineCacheRequest[]) => {
      return await this.addItems(items);
    });
    ipcMain.handle('music-server-cache:pause', async (_, cacheKey: string) => {
      return await this.pause(cacheKey);
    });
    ipcMain.handle('music-server-cache:resume', async (_, cacheKey: string) => {
      return await this.resume(cacheKey);
    });
    ipcMain.handle('music-server-cache:remove', async (_, cacheKey: string) => {
      return await this.remove(cacheKey);
    });
    ipcMain.handle('music-server-cache:get-state', async (_, query: MusicServerOfflineCacheQuery) => {
      return await this.getState(query);
    });
    ipcMain.handle('music-server-cache:get-all', async (_, query: MusicServerOfflineCacheQuery) => {
      return await this.getAll(query);
    });
    ipcMain.handle(
      'music-server-cache:resolve-playback-url',
      async (_, query: MusicServerOfflineCacheQuery) => {
        return await this.resolvePlaybackUrl(query);
      }
    );
    ipcMain.handle(
      'music-server-cache:sync-index',
      async (
        _,
        payload: {
          serverBaseUrl: string;
          userId: number;
          musicList: MusicServerOfflineCacheSyncMusic[];
        }
      ) => {
        return await this.syncIndex(payload);
      }
    );
  }

  private async upsertEntry(
    item: MusicServerOfflineCacheRequest
  ): Promise<MusicServerOfflineCacheEntry> {
    const now = Date.now();
    const request = this.normalizeRequest(item);
    const existing = this.getEntry(request.cacheKey);
    if (existing) {
      const nextEntry = {
        ...existing,
        title: request.title,
        checksum: request.checksum,
        fileSize: request.fileSize,
        contentType: request.contentType,
        streamUrl: request.streamUrl,
        pinned: request.pinned,
        state: existing.state === 'ready' ? existing.state : ('queued' as const),
        error: undefined,
        taskId: existing.state === 'ready' ? existing.taskId : randomUUID(),
        updatedAt: now
      };
      const validatedEntry =
        nextEntry.state === 'ready' ? await this.validateEntry(nextEntry) : nextEntry;
      this.saveEntry(validatedEntry);
      this.emitStateChange(validatedEntry);
      return validatedEntry;
    }

    const entry: MusicServerOfflineCacheEntry = {
      ...request,
      localPath: this.getLocalPath(request),
      tempPath: `${this.getLocalPath(request)}${TEMP_SUFFIX}`,
      state: 'queued',
      downloadedBytes: 0,
      createdAt: now,
      updatedAt: now,
      taskId: randomUUID()
    };
    this.saveEntry(entry);
    this.emitStateChange(entry);
    return entry;
  }

  private normalizeRequest(item: MusicServerOfflineCacheRequest): Omit<
    MusicServerOfflineCacheEntry,
    'localPath' | 'tempPath' | 'state' | 'downloadedBytes' | 'createdAt' | 'updatedAt'
  > {
    const profileId = item.profileId?.trim() || DEFAULT_PROFILE_ID;
    const serverBaseUrl = this.normalizeServerBaseUrl(item.serverBaseUrl);
    const contentType = item.contentType || 'application/octet-stream';
    const checksum = item.checksum || '';
    const cacheKey = this.createCacheKey(serverBaseUrl, item.userId, item.musicId, profileId);

    return {
      cacheKey,
      serverBaseUrl,
      userId: Number(item.userId),
      musicId: Number(item.musicId),
      profileId,
      title: item.title || String(item.musicId),
      checksum,
      fileSize: Number(item.fileSize) || 0,
      contentType,
      streamUrl: item.streamUrl,
      pinned: Boolean(item.pinned),
      taskId: randomUUID()
    };
  }

  private scheduleQueue(): void {
    if (this.processTimer) return;
    this.processTimer = setTimeout(() => {
      this.processTimer = null;
      void this.processQueue();
    }, 0);
  }

  private async processQueue(): Promise<void> {
    if (this.activeDownloads >= MAX_CONCURRENT_DOWNLOADS) return;

    const queuedEntries = Object.values(this.getEntries())
      .filter((entry) => entry.state === 'queued')
      .sort((a, b) => a.updatedAt - b.updatedAt);

    while (this.activeDownloads < MAX_CONCURRENT_DOWNLOADS && queuedEntries.length > 0) {
      const entry = queuedEntries.shift();
      if (!entry) break;
      void this.downloadEntry(entry);
    }
  }

  private async downloadEntry(entry: MusicServerOfflineCacheEntry): Promise<void> {
    if (this.abortControllers.has(entry.cacheKey)) return;

    const controller = new AbortController();
    this.abortControllers.set(entry.cacheKey, controller);
    this.activeDownloads++;

    const downloadingEntry = {
      ...entry,
      state: 'downloading' as const,
      error: undefined,
      updatedAt: Date.now()
    };
    this.saveEntry(downloadingEntry);
    this.emitStateChange(downloadingEntry);

    try {
      await fs.promises.mkdir(path.dirname(downloadingEntry.localPath), { recursive: true });
      let downloadedBytes = await this.getExistingSize(downloadingEntry.tempPath);
      const headers: Record<string, string> = {};
      if (downloadedBytes > 0) {
        headers.Range = `bytes=${downloadedBytes}-`;
      }

      const response = await axios.get(downloadingEntry.streamUrl, {
        responseType: 'stream',
        signal: controller.signal,
        timeout: 30000,
        headers
      });

      const appendToTemp = downloadedBytes > 0 && response.status === 206;
      if (!appendToTemp) {
        downloadedBytes = 0;
      }

      const writer = fs.createWriteStream(downloadingEntry.tempPath, {
        flags: appendToTemp ? 'a' : 'w'
      });
      let lastProgressAt = 0;
      response.data.on('data', (chunk: Buffer) => {
        downloadedBytes += chunk.length;
        const now = Date.now();
        if (now - lastProgressAt > 500) {
          lastProgressAt = now;
          this.updateProgress(downloadingEntry.cacheKey, downloadedBytes);
        }
      });

      await pipeline(response.data, writer);
      this.updateProgress(downloadingEntry.cacheKey, downloadedBytes);
      await this.finalizeDownload(downloadingEntry, downloadedBytes);
    } catch (error) {
      if (controller.signal.aborted) {
        const latest = this.getEntry(entry.cacheKey);
        if (latest && latest.state === 'downloading') {
          const pausedEntry = { ...latest, state: 'paused' as const, updatedAt: Date.now() };
          this.saveEntry(pausedEntry);
          this.emitStateChange(pausedEntry);
        }
      } else {
        this.markFailed(entry.cacheKey, this.sanitizeError(error));
      }
    } finally {
      this.abortControllers.delete(entry.cacheKey);
      this.activeDownloads--;
      this.scheduleQueue();
    }
  }

  private async finalizeDownload(
    entry: MusicServerOfflineCacheEntry,
    downloadedBytes: number
  ): Promise<void> {
    const tempStats = await fs.promises.stat(entry.tempPath);
    if (entry.fileSize > 0 && tempStats.size !== entry.fileSize) {
      this.markFailed(entry.cacheKey, 'CACHE_SIZE_MISMATCH');
      return;
    }

    if (entry.checksum) {
      const checksum = await this.sha256File(entry.tempPath);
      if (checksum !== entry.checksum) {
        this.markFailed(entry.cacheKey, 'CACHE_CHECKSUM_MISMATCH');
        return;
      }
    }

    await this.moveFile(entry.tempPath, entry.localPath);
    const readyEntry = {
      ...entry,
      state: 'ready' as const,
      downloadedBytes,
      error: undefined,
      lastVerifiedAt: Date.now(),
      updatedAt: Date.now()
    };
    this.saveEntry(readyEntry);
    this.emitStateChange(readyEntry);
  }

  private async validateEntry(
    entry: MusicServerOfflineCacheEntry,
    expectedChecksum?: string
  ): Promise<MusicServerOfflineCacheEntry> {
    if (entry.state !== 'ready') return entry;

    const exists = await fs.promises
      .stat(entry.localPath)
      .then((stats) => stats)
      .catch(() => null);
    if (!exists) {
      return this.markStale(entry, 'CACHE_FILE_MISSING');
    }
    if (entry.fileSize > 0 && exists.size !== entry.fileSize) {
      return this.markStale(entry, 'CACHE_SIZE_MISMATCH');
    }

    const checksumToVerify = expectedChecksum || entry.checksum;
    if (checksumToVerify) {
      const checksum = await this.sha256File(entry.localPath);
      if (checksum !== checksumToVerify) {
        return this.markStale(entry, 'CACHE_CHECKSUM_MISMATCH');
      }
    }

    const verifiedEntry = {
      ...entry,
      lastVerifiedAt: Date.now(),
      updatedAt: Date.now()
    };
    this.saveEntry(verifiedEntry);
    return verifiedEntry;
  }

  private markStale(
    entry: MusicServerOfflineCacheEntry,
    error: string
  ): MusicServerOfflineCacheEntry {
    const staleEntry = {
      ...entry,
      state: 'stale' as const,
      error,
      updatedAt: Date.now()
    };
    this.saveEntry(staleEntry);
    this.emitStateChange(staleEntry);
    return staleEntry;
  }

  private markFailed(cacheKey: string, error: string): void {
    const entry = this.getEntry(cacheKey);
    if (!entry) return;

    const failedEntry = {
      ...entry,
      state: 'failed' as const,
      error,
      updatedAt: Date.now()
    };
    this.saveEntry(failedEntry);
    this.emitStateChange(failedEntry);
  }

  private updateProgress(cacheKey: string, downloadedBytes: number): void {
    const entry = this.getEntry(cacheKey);
    if (!entry || entry.state !== 'downloading') return;

    const nextEntry = {
      ...entry,
      downloadedBytes,
      updatedAt: Date.now()
    };
    this.saveEntry(nextEntry);
    this.emitStateChange(nextEntry);
  }

  private restoreInterruptedDownloads(): void {
    const entries = this.getEntries();
    let changed = false;
    for (const [cacheKey, entry] of Object.entries(entries)) {
      if (entry.state === 'downloading') {
        entries[cacheKey] = {
          ...entry,
          state: 'paused',
          updatedAt: Date.now()
        };
        changed = true;
      }
    }
    if (changed) {
      this.metadataStore.set('entries', entries);
    }
  }

  private toPublicEntry(
    entry: MusicServerOfflineCacheEntry
  ): MusicServerOfflineCachePublicEntry {
    const { localPath, tempPath, streamUrl, ...publicEntry } = entry;
    return {
      ...publicEntry,
      playbackUrl: entry.state === 'ready' ? pathToFileURL(localPath).toString() : undefined
    };
  }

  private findEntry(query: MusicServerOfflineCacheQuery): MusicServerOfflineCacheEntry | null {
    if (query.cacheKey) {
      return this.getEntry(query.cacheKey);
    }

    const profileId = query.profileId?.trim() || DEFAULT_PROFILE_ID;
    if (query.serverBaseUrl && query.userId != null && query.musicId != null) {
      const cacheKey = this.createCacheKey(
        this.normalizeServerBaseUrl(query.serverBaseUrl),
        query.userId,
        query.musicId,
        profileId
      );
      return this.getEntry(cacheKey);
    }

    return null;
  }

  private matchesQuery(
    entry: MusicServerOfflineCacheEntry,
    query: MusicServerOfflineCacheQuery
  ): boolean {
    if (query.cacheKey && entry.cacheKey !== query.cacheKey) return false;
    if (query.serverBaseUrl && entry.serverBaseUrl !== this.normalizeServerBaseUrl(query.serverBaseUrl)) {
      return false;
    }
    if (query.userId != null && entry.userId !== query.userId) return false;
    if (query.musicId != null && entry.musicId !== query.musicId) return false;
    if (query.profileId && entry.profileId !== query.profileId) return false;
    return true;
  }

  private normalizeMusicId(music: MusicServerOfflineCacheSyncMusic): number | null {
    const value = music.musicId ?? music.id;
    const musicId = Number(value);
    return Number.isFinite(musicId) ? musicId : null;
  }

  private getEntry(cacheKey: string): MusicServerOfflineCacheEntry | null {
    return this.getEntries()[cacheKey] ?? null;
  }

  private saveEntry(entry: MusicServerOfflineCacheEntry): void {
    const entries = this.getEntries();
    entries[entry.cacheKey] = entry;
    this.metadataStore.set('entries', entries);
  }

  private getEntries(): Record<string, MusicServerOfflineCacheEntry> {
    return this.metadataStore.get('entries') ?? {};
  }

  private getLocalPath(
    entry: Pick<
      MusicServerOfflineCacheEntry,
      'serverBaseUrl' | 'userId' | 'musicId' | 'profileId' | 'contentType'
    >
  ): string {
    const serverKey = this.shortHash(entry.serverBaseUrl);
    const extension = this.extensionFor(entry.contentType);
    return path.join(
      this.getRootDirectory(),
      serverKey,
      String(entry.userId),
      String(entry.musicId),
      `${this.sanitizePathSegment(entry.profileId)}${extension}`
    );
  }

  private getRootDirectory(): string {
    const configStore = getStore() as any;
    const diskCacheDir = configStore?.get?.('set.diskCacheDir');
    const baseDirectory =
      typeof diskCacheDir === 'string' && diskCacheDir.trim()
        ? diskCacheDir
        : path.join(app.getPath('userData'), 'cache');
    return path.join(path.resolve(baseDirectory), CACHE_DIR_NAME);
  }

  private ensureRootDirectory(): void {
    fs.mkdirSync(this.getRootDirectory(), { recursive: true });
  }

  private async removeFileIfInsideRoot(filePath: string): Promise<void> {
    if (!this.isPathInsideRoot(filePath)) return;
    await fs.promises.unlink(filePath).catch(() => undefined);
  }

  private isPathInsideRoot(filePath: string): boolean {
    const rootDirectory = path.resolve(this.getRootDirectory());
    const normalizedPath = path.resolve(filePath);
    const relativePath = path.relative(rootDirectory, normalizedPath);
    return (
      relativePath === '' || (!relativePath.startsWith('..') && !path.isAbsolute(relativePath))
    );
  }

  private async getExistingSize(filePath: string): Promise<number> {
    return await fs.promises
      .stat(filePath)
      .then((stats) => stats.size)
      .catch(() => 0);
  }

  private async moveFile(sourcePath: string, targetPath: string): Promise<void> {
    await fs.promises.mkdir(path.dirname(targetPath), { recursive: true });
    try {
      await fs.promises.rename(sourcePath, targetPath);
    } catch {
      await fs.promises.copyFile(sourcePath, targetPath);
      await fs.promises.unlink(sourcePath);
    }
  }

  private async sha256File(filePath: string): Promise<string> {
    const hash = createHash('sha256');
    const stream = fs.createReadStream(filePath);
    for await (const chunk of stream) {
      hash.update(chunk);
    }
    return hash.digest('hex');
  }

  private createCacheKey(
    serverBaseUrl: string,
    userId: number,
    musicId: number,
    profileId: string
  ): string {
    return this.shortHash([serverBaseUrl, userId, musicId, profileId].join('|'));
  }

  private normalizeServerBaseUrl(serverBaseUrl: string): string {
    return (serverBaseUrl || '').trim().replace(/\/+$/, '');
  }

  private shortHash(value: string): string {
    return createHash('sha256').update(value).digest('hex').slice(0, 32);
  }

  private extensionFor(contentType: string): string {
    return AUDIO_EXTENSION_BY_CONTENT_TYPE[contentType.toLowerCase()] ?? '.audio';
  }

  private sanitizePathSegment(value: string): string {
    return value.replace(/[<>:"/\\|?*\s]+/g, '_').replace(/^\.+$/, '_').slice(0, 80);
  }

  private sanitizeError(error: unknown): string {
    if (axios.isAxiosError(error)) {
      if (error.response?.status) {
        return `HTTP_${error.response.status}`;
      }
      if (error.code === 'ERR_CANCELED') {
        return 'DOWNLOAD_ABORTED';
      }
      return error.code || 'DOWNLOAD_FAILED';
    }
    return error instanceof Error ? error.message.slice(0, 120) : 'DOWNLOAD_FAILED';
  }

  private emitStateChange(entry: MusicServerOfflineCacheEntry): void {
    const publicEntry = this.toPublicEntry(entry);
    for (const window of BrowserWindow.getAllWindows()) {
      window.webContents.send('music-server-cache:state-change', publicEntry);
    }
  }

  private emitRemoved(cacheKey: string): void {
    for (const window of BrowserWindow.getAllWindows()) {
      window.webContents.send('music-server-cache:removed', cacheKey);
    }
  }
}

export const musicServerOfflineCacheManager = new MusicServerOfflineCacheManager();

export function initializeMusicServerOfflineCache(): void {
  musicServerOfflineCacheManager.initialize();
}
