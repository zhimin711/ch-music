<template>
  <div class="cloud-music-page h-full w-full bg-white dark:bg-black transition-colors duration-500">
    <n-scrollbar class="h-full">
      <div class="pb-32">
        <section class="page-padding-x pt-6 pb-5 border-b border-neutral-100 dark:border-neutral-900">
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div class="flex items-center gap-5 min-w-0">
              <div
                class="w-20 h-20 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 flex items-center justify-center ring-1 ring-emerald-100 dark:ring-emerald-900 shrink-0"
              >
                <i class="ri-cloud-fill text-4xl text-emerald-500" />
              </div>
              <div class="min-w-0">
                <h1 class="text-2xl md:text-3xl font-bold text-neutral-900 dark:text-white">
                  云音乐库
                </h1>
                <p class="mt-1 text-sm text-neutral-500 dark:text-neutral-400 truncate">
                  {{ cloudStore.isLoggedIn ? cloudStore.user?.displayName : '未连接云音乐库' }}
                </p>
              </div>
            </div>

            <div v-if="cloudStore.isLoggedIn" class="flex items-center gap-2">
              <button
                class="w-10 h-10 rounded-full flex items-center justify-center bg-neutral-100 dark:bg-neutral-900 text-neutral-600 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-all"
                :disabled="cloudStore.loading"
                @click="handleRefresh"
              >
                <i class="ri-refresh-line text-lg" :class="{ 'animate-spin': cloudStore.loading }" />
              </button>
              <button
                class="px-4 h-10 rounded-full bg-neutral-100 dark:bg-neutral-900 text-sm text-neutral-700 dark:text-neutral-200 hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-all"
                @click="handleLogout"
              >
                退出
              </button>
            </div>
          </div>
        </section>

        <section v-if="!cloudStore.isLoggedIn" class="page-padding-x mt-8">
          <div
            class="max-w-xl rounded-lg border border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-950 p-6"
          >
            <div class="flex items-start gap-4">
              <div
                class="w-11 h-11 rounded-lg bg-neutral-100 dark:bg-neutral-900 flex items-center justify-center text-neutral-400 shrink-0"
              >
                <i class="ri-cloud-off-line text-xl" />
              </div>
              <div class="min-w-0">
                <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-100">
                  当前未连接云音乐库
                </h2>
                <p class="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
                  云端音乐将在账号状态可用后自动同步。
                </p>
              </div>
            </div>
          </div>
        </section>

        <template v-else>
          <input
            ref="fileInput"
            type="file"
            accept="audio/*"
            multiple
            class="hidden"
            @change="handleFileChange"
          />

          <section class="cloud-drive page-padding-x mt-5">
            <div class="drive-tabs">
              <button
                v-for="tab in driveTabs"
                :key="tab.key"
                class="drive-tab"
                :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>

            <div class="drive-quota">
              <span>网盘容量</span>
              <div class="quota-bar">
                <div class="quota-bar-fill" :style="{ width: quotaPercent + '%' }"></div>
              </div>
              <span>{{ formatBytes(totalCloudSize) }}/40G</span>
            </div>

            <div class="drive-toolbar">
              <div class="drive-actions">
                <n-button
                  type="primary"
                  :disabled="filteredMusic.length === 0"
                  @click="handlePlayAll(filteredMusic)"
                >
                  <template #icon>
                    <i class="ri-play-fill" />
                  </template>
                  播放全部
                </n-button>
                <n-button secondary :loading="cloudStore.uploading" @click="fileInput?.click()">
                  <template #icon>
                    <i class="ri-upload-cloud-2-line" />
                  </template>
                  上传
                </n-button>
                <n-button quaternary circle :disabled="cloudStore.loading" @click="handleRefresh">
                  <template #icon>
                    <i class="ri-refresh-line" :class="{ 'animate-spin': cloudStore.loading }" />
                  </template>
                </n-button>
              </div>

              <n-input
                v-model:value="searchKeyword"
                placeholder="搜索"
                clearable
                class="drive-search"
              >
                <template #prefix>
                  <i class="ri-search-line text-neutral-400" />
                </template>
              </n-input>
            </div>

            <div v-if="cloudStore.uploading || selectedFiles.length" class="upload-status">
              <i class="ri-upload-cloud-2-line" />
              <span>{{ uploadStatusText }}</span>
            </div>

            <div v-if="activeTab === 'music'">
              <div v-if="filteredMusic.length === 0" class="drive-empty">
                <i class="ri-cloud-off-line" />
                暂无云端音乐
              </div>
              <div v-else class="drive-table">
                <div class="drive-table-head">
                  <div>#</div>
                  <div>标题</div>
                  <div>专辑</div>
                  <div>格式</div>
                  <div>上传时间</div>
                  <div>离线</div>
                  <div>大小</div>
                  <div></div>
                </div>
                <div
                  v-for="(music, index) in filteredMusic"
                  :key="music.id"
                  class="drive-row"
                  @contextmenu.prevent="handleCloudMusicContextMenu($event, music)"
                  @dblclick="handlePlayMusic(music)"
                >
                  <div class="drive-index">{{ String(index + 1).padStart(2, '0') }}</div>
                  <div class="drive-title-cell">
                    <img :src="getMusicCover(music)" alt="" class="drive-cover" />
                    <div class="min-w-0">
                      <div class="drive-title">{{ music.title }}</div>
                      <div class="drive-artist">
                        <i class="ri-cloud-line" />
                        {{ music.artist || '未知歌手' }}
                      </div>
                    </div>
                  </div>
                  <div class="drive-muted">{{ music.album || '未知专辑' }}</div>
                  <div class="drive-muted">{{ getMusicFormat(music) }}</div>
                  <div class="drive-muted">{{ formatDate(music.createdAt) }}</div>
                  <div class="drive-cache-cell">
                    <span class="cache-badge" :class="getCacheBadgeClass(music)">
                      <i :class="getCacheStatusIcon(music)" />
                      {{ getCacheStatusText(music) }}
                    </span>
                  </div>
                  <div class="drive-muted">{{ formatBytes(music.fileSize) }}</div>
                  <div class="drive-row-actions">
                    <button type="button" title="播放" @click.stop="handlePlayMusic(music)">
                      <i class="ri-play-fill" />
                    </button>
                    <button
                      type="button"
                      :title="getCacheActionLabel(music)"
                      :disabled="isCacheActionDisabled(music)"
                      @click.stop="handleCacheAction(music)"
                    >
                      <i :class="getCacheActionIcon(music)" />
                    </button>
                    <button type="button" title="删除" @click.stop="handleDeleteMusic(Number(music.id))">
                      <i class="ri-delete-bin-line" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'uploading'">
              <div v-if="!selectedFiles.length && !cloudStore.uploading" class="drive-empty">
                <i class="ri-upload-cloud-line" />
                当前没有正在上传的音乐
              </div>
              <div v-else class="uploading-list">
                <div v-for="file in selectedFiles" :key="`${file.name}-${file.size}`" class="uploading-item">
                  <i class="ri-music-2-line" />
                  <div class="min-w-0 flex-1">
                    <div class="truncate text-sm font-medium text-neutral-800 dark:text-neutral-100">
                      {{ file.name }}
                    </div>
                    <div class="mt-1 text-xs text-neutral-400">{{ formatBytes(file.size) }}</div>
                  </div>
                  <span class="text-xs text-primary">{{ cloudStore.uploading ? '上传中' : '待上传' }}</span>
                </div>
              </div>
            </div>

            <template v-else-if="activeTab === 'favorites'">
              <div v-if="cloudStore.favoriteSongs.length === 0" class="drive-empty">
                <i class="ri-heart-line" />
                暂无收藏
              </div>
              <div v-else class="song-list-container">
                <song-item
                  v-for="(song, index) in favoriteSongResults"
                  :key="song.id"
                  :item="song"
                  :index="index"
                  @play="handlePlaySongResult(song, favoriteSongResults)"
                />
              </div>
            </template>

            <template v-else>
                <div class="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-5 py-3">
                  <div>
                    <div class="flex gap-2 mb-3">
                      <n-input v-model:value="newPlaylistName" placeholder="新建歌单" @keyup.enter="handleCreatePlaylist" />
                      <n-button type="primary" :disabled="!newPlaylistName.trim()" @click="handleCreatePlaylist">
                        <template #icon>
                          <i class="ri-add-line" />
                        </template>
                      </n-button>
                    </div>
                    <div class="space-y-2">
                      <button
                        v-for="playlist in cloudStore.playlists"
                        :key="playlist.id"
                        class="w-full rounded-lg px-3 py-3 text-left transition-all"
                        :class="
                          playlist.id === activePlaylistId
                            ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300'
                            : 'bg-neutral-50 dark:bg-neutral-950 text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-900'
                        "
                        @click="activePlaylistId = playlist.id"
                      >
                        <div class="font-medium truncate">{{ playlist.name }}</div>
                        <div class="text-xs opacity-70 mt-1">{{ playlist.tracks.length }} 首</div>
                      </button>
                    </div>
                  </div>

                  <div v-if="activePlaylist" class="min-w-0">
                    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-3">
                      <div class="min-w-0">
                        <h2 class="text-lg font-semibold text-neutral-900 dark:text-white truncate">
                          {{ activePlaylist.name }}
                        </h2>
                        <p class="text-xs text-neutral-500 dark:text-neutral-400">
                          {{ activePlaylist.tracks.length }} 首音乐
                        </p>
                      </div>
                      <div class="flex items-center gap-2">
                        <n-select
                          v-model:value="playlistTrackMusicId"
                          :options="musicOptions"
                          placeholder="选择音乐"
                          class="w-44"
                          size="small"
                        />
                        <n-button
                          size="small"
                          :disabled="!playlistTrackMusicId"
                          @click="handleAddTrackToActivePlaylist"
                        >
                          添加
                        </n-button>
                        <n-popconfirm @positive-click="handleDeletePlaylist(activePlaylist.id)">
                          <template #trigger>
                            <n-button size="small" quaternary>
                              <template #icon>
                                <i class="ri-delete-bin-line" />
                              </template>
                            </n-button>
                          </template>
                          删除这个云端歌单？
                        </n-popconfirm>
                      </div>
                    </div>

                    <div v-if="activePlaylist.tracks.length === 0" class="py-16 text-center text-neutral-400">
                      暂无歌曲
                    </div>
                    <div v-else class="song-list-container">
                      <song-item
                        v-for="(song, index) in activePlaylistSongResults"
                        :key="song.id"
                        :item="song"
                        :index="index"
                        compact
                        can-remove
                        @play="handlePlaySongResult(song, activePlaylistSongResults)"
                        @remove-song="handleRemoveTrack(activePlaylist.id, song.musicServerTrackId || Number($event))"
                      />
                    </div>
                  </div>

                  <div v-else class="py-20 text-center text-neutral-400">
                    暂无歌单
                  </div>
                </div>
            </template>
          </section>

          <song-item-dropdown
            v-if="isElectron && cloudContextSong"
            :item="cloudContextSong"
            :show="showCloudDropdown"
            :x="cloudDropdownX"
            :y="cloudDropdownY"
            :is-favorite="isCloudContextFavorite"
            :is-dislike="isCloudContextDislike"
            can-remove
            @update:show="showCloudDropdown = $event"
            @play="handleCloudContextPlay"
            @play-next="handleCloudContextPlayNext"
            @download="handleCloudContextDownload"
            @download-lyric="handleCloudContextDownloadLyric"
            @toggle-favorite="toggleCloudContextFavorite"
            @toggle-dislike="toggleCloudContextDislike"
            @remove="handleCloudContextRemove"
          />
        </template>
      </div>
    </n-scrollbar>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios';
import { createDiscreteApi } from 'naive-ui';
import { computed, onMounted, reactive, ref, watch } from 'vue';

import SongItem from '@/components/common/SongItem.vue';
import SongItemDropdown from '@/components/common/songItemCom/SongItemDropdown.vue';
import { useSongItem } from '@/hooks/useSongItem';
import { useMusicServerStore } from '@/store/modules/musicServer';
import { usePlayerStore } from '@/store/modules/player';
import type { SongResult } from '@/types/music';
import type { MusicServerMusic } from '@/types/musicServer';
import { DEFAULT_COVER_URL, getImgUrl, isElectron } from '@/utils';
import { toMusicServerSongResult } from '@/utils/musicServerUtils';

const { message } = createDiscreteApi(['message']);
const cloudStore = useMusicServerStore();
const playerStore = usePlayerStore();
const CLOUD_CAPACITY_BYTES = 40 * 1024 * 1024 * 1024;
const DEFAULT_CACHE_PROFILE_ID = 'original';

const activeTab = ref<'music' | 'uploading' | 'favorites' | 'playlists'>('music');
const activePlaylistId = ref<number | null>(null);
const searchKeyword = ref('');
const fileInput = ref<HTMLInputElement | null>(null);
const selectedFiles = ref<File[]>([]);
const uploadTitle = ref('');
const uploadArtist = ref('');
const uploadAlbum = ref('');
const newPlaylistName = ref('');
const playlistTrackMusicId = ref<number | null>(null);
const cacheActionLoading = ref<Record<string, boolean>>({});
const cloudContextSong = ref<SongResult | null>(null);
const cloudContextMusic = ref<MusicServerMusic | null>(null);
const driveTabs = [
  { key: 'music' as const, label: '已上传单曲' },
  { key: 'uploading' as const, label: '正在上传' },
  { key: 'favorites' as const, label: '收藏' },
  { key: 'playlists' as const, label: '歌单' }
];

const emptyContextArtist = {
  name: '',
  id: 0,
  picId: 0,
  img1v1Id: 0,
  briefDesc: '',
  picUrl: '',
  img1v1Url: '',
  albumSize: 0,
  alias: [],
  trans: '',
  musicSize: 0,
  topicPerson: 0
};

const emptyContextSong: SongResult = {
  id: 0,
  name: '',
  picUrl: DEFAULT_COVER_URL,
  ar: [emptyContextArtist],
  artists: [emptyContextArtist],
  al: {
    name: '',
    id: 0,
    type: '',
    size: 0,
    picId: 0,
    blurPicUrl: '',
    companyId: 0,
    pic: 0,
    picUrl: DEFAULT_COVER_URL,
    publishTime: 0,
    description: '',
    tags: '',
    company: '',
    briefDesc: '',
    artist: emptyContextArtist,
    songs: [],
    alias: [],
    status: 0,
    copyrightId: 0,
    commentThreadId: '',
    artists: [],
    subType: '',
    transName: null,
    onSale: false,
    mark: 0,
    picId_str: ''
  },
  count: 0,
  source: 'musicServer'
};

const cloudContextProps = reactive({
  get item() {
    return cloudContextSong.value || emptyContextSong;
  },
  canRemove: false
});

const {
  showDropdown: showCloudDropdown,
  dropdownX: cloudDropdownX,
  dropdownY: cloudDropdownY,
  isFavorite: isCloudContextFavorite,
  isDislike: isCloudContextDislike,
  handleContextMenu: openCloudContextMenu,
  handlePlayNext: handleCloudContextPlayNext,
  toggleFavorite: toggleCloudContextFavorite,
  toggleDislike: toggleCloudContextDislike,
  downloadMusic,
  downloadLyric
} = useSongItem(cloudContextProps);

const filteredMusic = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return cloudStore.musicList;
  return cloudStore.musicList.filter((music) =>
    [music.title, music.artist, music.album, music.originalFilename]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(keyword))
  );
});

const favoriteSongResults = computed(() => cloudStore.favoriteSongs);

const musicOptions = computed(() =>
  cloudStore.musicList.map((music) => ({
    label: `${music.title}${music.artist ? ` - ${music.artist}` : ''}`,
    value: music.id
  }))
);

const activePlaylist = computed(
  () => cloudStore.playlists.find((playlist) => playlist.id === activePlaylistId.value) || null
);

const activePlaylistSongResults = computed(() =>
  activePlaylist.value ? activePlaylist.value.tracks.map(toMusicServerSongResult) : []
);
const totalCloudSize = computed(() =>
  cloudStore.musicList.reduce((total, music) => total + (Number(music.fileSize) || 0), 0)
);
const quotaPercent = computed(() =>
  Math.min(100, Math.round((totalCloudSize.value / CLOUD_CAPACITY_BYTES) * 1000) / 10)
);
const uploadStatusText = computed(() => {
  if (!selectedFiles.value.length) return cloudStore.uploading ? '正在上传音乐' : '';
  return `${cloudStore.uploading ? '正在上传' : '待上传'} ${selectedFiles.value.length} 个文件`;
});

type CacheState = (typeof cloudStore.cacheStates)[string];

watch(
  () => cloudStore.playlists,
  (playlists) => {
    if (!playlists.length) {
      activePlaylistId.value = null;
      return;
    }
    if (!activePlaylistId.value || !playlists.some((item) => item.id === activePlaylistId.value)) {
      activePlaylistId.value = playlists[0].id;
    }
  },
  { immediate: true }
);

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

async function handleLogout() {
  await cloudStore.logout();
  message.success('已退出云音乐库');
}

async function handleRefresh() {
  try {
    await cloudStore.loadAll();
    await playerStore.initializeFavoriteList();
    message.success('已刷新');
  } catch (error) {
    console.error('刷新云音乐库失败:', error);
    message.error(getErrorMessage(error, '刷新失败'));
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const files = Array.from(target.files || []);
  selectedFiles.value = files;
  if (files.length === 1 && !uploadTitle.value.trim()) {
    uploadTitle.value = files[0].name.replace(/\.[^.]+$/, '');
  }
  if (files.length > 1) {
    uploadTitle.value = '';
  }
  if (files.length) {
    activeTab.value = 'uploading';
    void handleUpload();
  }
}

async function handleUpload() {
  if (!selectedFiles.value.length) return;

  try {
    const files = [...selectedFiles.value];
    if (files.length === 1) {
      await cloudStore.upload(files[0], {
        title: uploadTitle.value,
        artist: uploadArtist.value,
        album: uploadAlbum.value
      });
    } else {
      await cloudStore.uploadMany(files, () => ({
        artist: uploadArtist.value,
        album: uploadAlbum.value
      }));
    }
    selectedFiles.value = [];
    uploadTitle.value = '';
    uploadArtist.value = '';
    uploadAlbum.value = '';
    if (fileInput.value) fileInput.value.value = '';
    message.success(files.length === 1 ? '上传成功' : `已上传 ${files.length} 首音乐`);
    activeTab.value = 'music';
  } catch (error) {
    console.error('上传云音乐失败:', error);
    message.error(getErrorMessage(error, '上传失败'));
  }
}

function getMusicCover(music: MusicServerMusic) {
  return getImgUrl(music.picUrl || DEFAULT_COVER_URL, '100y100');
}

function getMusicFormat(music: MusicServerMusic) {
  const fromName = music.originalFilename?.split('.').pop();
  const fromType = music.contentType?.split('/').pop();
  return (fromName || fromType || 'audio').toLowerCase();
}

function formatDate(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toISOString().slice(0, 10);
}

function formatBytes(value?: number | null) {
  const bytes = Number(value) || 0;
  if (bytes <= 0) return '0B';
  const units = ['B', 'K', 'M', 'G'];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)}${units[unitIndex]}`;
}

function getMusicId(music: MusicServerMusic) {
  const musicId = Number(music.musicId ?? music.id);
  return Number.isFinite(musicId) ? musicId : null;
}

function getMusicCacheState(music: MusicServerMusic): CacheState | null {
  const musicId = getMusicId(music);
  if (!cloudStore.user || musicId == null) return null;
  return (
    Object.values(cloudStore.cacheStates).find(
      (entry) =>
        entry.serverBaseUrl === cloudStore.baseUrl &&
        entry.userId === cloudStore.user?.id &&
        entry.musicId === musicId &&
        entry.profileId === DEFAULT_CACHE_PROFILE_ID
    ) || null
  );
}

function getCacheStatusText(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  if (state === 'ready') return '已缓存';
  if (state === 'downloading') return '缓存中';
  if (state === 'queued') return '等待中';
  if (state === 'paused') return '已暂停';
  if (state === 'failed') return '失败';
  if (state === 'stale') return '需更新';
  return '未缓存';
}

function getCacheStatusIcon(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  if (state === 'ready') return 'ri-checkbox-circle-fill';
  if (state === 'downloading' || state === 'queued') return 'ri-loader-4-line animate-spin';
  if (state === 'paused') return 'ri-pause-circle-line';
  if (state === 'failed' || state === 'stale') return 'ri-error-warning-line';
  return 'ri-download-cloud-line';
}

function getCacheBadgeClass(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  return {
    ready: state === 'ready',
    active: state === 'downloading' || state === 'queued',
    warning: state === 'failed' || state === 'stale',
    paused: state === 'paused'
  };
}

function getCacheActionLabel(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  if (state === 'ready') return '移除缓存';
  if (state === 'failed' || state === 'stale' || state === 'paused') return '重试缓存';
  if (state === 'downloading' || state === 'queued') return '缓存中';
  return '缓存';
}

function getCacheActionIcon(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  if (cacheActionLoading.value[String(music.id)]) return 'ri-loader-4-line animate-spin';
  if (state === 'ready') return 'ri-delete-back-2-line';
  if (state === 'failed' || state === 'stale' || state === 'paused') return 'ri-restart-line';
  if (state === 'downloading' || state === 'queued') return 'ri-loader-4-line animate-spin';
  return 'ri-download-cloud-2-line';
}

function isCacheActionDisabled(music: MusicServerMusic) {
  const state = getMusicCacheState(music)?.state;
  return (
    Boolean(cacheActionLoading.value[String(music.id)]) ||
    state === 'downloading' ||
    state === 'queued'
  );
}

async function toSongs(musicList: MusicServerMusic[]) {
  return await Promise.all(musicList.map((music) => cloudStore.toCachedSongResult(music)));
}

async function handlePlayAll(musicList: MusicServerMusic[]) {
  if (!musicList.length) return;
  const songs = await toSongs(musicList);
  playerStore.setPlayList(songs);
  await playerStore.setPlay(songs[0]);
}

async function handlePlaySongResult(song: SongResult, context: SongResult[]) {
  playerStore.setPlayList(context);
  await playerStore.setPlay(song);
}

async function handlePlayMusic(music: MusicServerMusic) {
  const songs = await toSongs(filteredMusic.value);
  const song = await cloudStore.toCachedSongResult(music);
  playerStore.setPlayList(songs);
  await playerStore.setPlay(song);
}

async function handleCloudMusicContextMenu(event: MouseEvent, music: MusicServerMusic) {
  cloudContextMusic.value = music;
  cloudContextSong.value = await cloudStore.toCachedSongResult(music);
  openCloudContextMenu(event);
}

async function handleCloudContextPlay() {
  if (!cloudContextMusic.value) return;
  const song = await cloudStore.toCachedSongResult(cloudContextMusic.value);
  cloudContextSong.value = song;
  playerStore.setPlayList(await toSongs(filteredMusic.value));
  await playerStore.setPlay(song);
}

async function handleCacheAction(music: MusicServerMusic) {
  const key = String(music.id);
  if (isCacheActionDisabled(music)) return;

  cacheActionLoading.value = { ...cacheActionLoading.value, [key]: true };
  try {
    const state = getMusicCacheState(music);
    if (state?.state === 'ready') {
      await cloudStore.removeCachedMusic(music);
      message.success('已移除缓存');
    } else if (state && ['failed', 'stale', 'paused'].includes(state.state)) {
      await cloudStore.retryCachedMusic(music);
      message.success('已重新加入缓存队列');
    } else {
      await cloudStore.cacheMusic(music);
      message.success('已加入缓存队列');
    }
  } catch (error) {
    console.error('处理离线缓存失败:', error);
    message.error(getErrorMessage(error, '缓存操作失败'));
  } finally {
    const nextLoading = { ...cacheActionLoading.value };
    delete nextLoading[key];
    cacheActionLoading.value = nextLoading;
  }
}

function handleCloudContextDownload() {
  if (!cloudContextSong.value) return;
  downloadMusic(cloudContextSong.value);
}

function handleCloudContextDownloadLyric() {
  if (!cloudContextSong.value) return;
  downloadLyric(cloudContextSong.value);
}

function handleCloudContextRemove() {
  if (!cloudContextMusic.value) return;
  void handleDeleteMusic(Number(cloudContextMusic.value.id));
}

async function handleDeleteMusic(musicId: number) {
  try {
    await cloudStore.deleteMusic(musicId);
    showCloudDropdown.value = false;
    message.success('已删除');
  } catch (error) {
    console.error('删除云端音乐失败:', error);
    message.error(getErrorMessage(error, '删除失败'));
  }
}

async function handleCreatePlaylist() {
  const name = newPlaylistName.value.trim();
  if (!name) return;

  try {
    await cloudStore.createPlaylist({ name });
    newPlaylistName.value = '';
    message.success('歌单已创建');
  } catch (error) {
    console.error('创建云端歌单失败:', error);
    message.error(getErrorMessage(error, '创建失败'));
  }
}

async function handleAddTrackToActivePlaylist() {
  if (!activePlaylistId.value || !playlistTrackMusicId.value) return;

  try {
    await cloudStore.addTrackToPlaylist(activePlaylistId.value, playlistTrackMusicId.value);
    playlistTrackMusicId.value = null;
    message.success('已添加');
  } catch (error) {
    console.error('添加云端歌单歌曲失败:', error);
    message.error(getErrorMessage(error, '添加失败'));
  }
}

async function handleRemoveTrack(playlistId: number, trackId: number) {
  try {
    await cloudStore.removeTrackFromPlaylist(playlistId, trackId);
    message.success('已移除');
  } catch (error) {
    console.error('移除云端歌单歌曲失败:', error);
    message.error(getErrorMessage(error, '移除失败'));
  }
}

async function handleDeletePlaylist(playlistId: number) {
  try {
    await cloudStore.deletePlaylist(playlistId);
    message.success('歌单已删除');
  } catch (error) {
    console.error('删除云端歌单失败:', error);
    message.error(getErrorMessage(error, '删除失败'));
  }
}

onMounted(async () => {
  if (!cloudStore.token) return;
  try {
    await cloudStore.restoreSession();
    await playerStore.initializeFavoriteList();
  } catch {
    message.warning('云音乐库连接已失效');
  }
});
</script>

<style lang="scss" scoped>
.cloud-drive {
  @apply rounded-2xl bg-white dark:bg-black;
}

.drive-tabs {
  @apply flex items-center gap-8 border-b border-neutral-100 pb-4 dark:border-neutral-900;
}

.drive-tab {
  @apply relative py-2 text-lg font-semibold text-neutral-400 transition hover:text-neutral-800 dark:hover:text-neutral-100;

  &.active {
    @apply text-neutral-950 dark:text-white;

    &::after {
      content: '';
      @apply absolute bottom-0 left-1/2 h-1 w-7 -translate-x-1/2 rounded-full bg-primary;
    }
  }
}

.drive-quota {
  @apply mt-5 flex flex-wrap items-center gap-4 text-sm text-neutral-500 dark:text-neutral-400;
}

.quota-bar {
  @apply h-1.5 w-56 overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-800;
}

.quota-bar-fill {
  @apply h-full rounded-full bg-primary transition-all duration-300;
}

.drive-toolbar {
  @apply mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between;
}

.drive-actions {
  @apply flex items-center gap-3;
}

.drive-search {
  @apply sm:w-48;
}

.upload-status {
  @apply mt-4 flex items-center gap-2 rounded-xl bg-primary/10 px-4 py-3 text-sm text-primary;
}

.drive-empty {
  @apply flex flex-col items-center justify-center gap-3 py-24 text-center text-neutral-400;

  i {
    @apply text-5xl text-neutral-200 dark:text-neutral-800;
  }
}

.drive-table {
  @apply mt-6 overflow-hidden rounded-2xl;
}

.drive-table-head,
.drive-row {
  display: grid;
  grid-template-columns: 52px minmax(240px, 1.5fr) minmax(120px, 0.7fr) 84px 120px 108px 88px 120px;
  align-items: center;
  column-gap: 12px;
}

.drive-table-head {
  @apply border-b border-neutral-100 px-4 py-3 text-sm font-medium text-neutral-400 dark:border-neutral-900;
}

.drive-row {
  @apply min-h-[76px] cursor-default rounded-2xl px-4 text-left transition hover:bg-neutral-50 dark:hover:bg-neutral-900/70;
}

.drive-row:nth-child(2n + 1) {
  @apply bg-neutral-50/60 dark:bg-neutral-950/70;
}

.drive-index {
  @apply text-sm text-neutral-400;
}

.drive-title-cell {
  @apply flex min-w-0 items-center gap-3;
}

.drive-cover {
  @apply h-12 w-12 flex-shrink-0 rounded-lg object-cover bg-neutral-200 dark:bg-neutral-800;
}

.drive-title {
  @apply truncate text-sm font-medium text-neutral-900 dark:text-neutral-50;
}

.drive-artist {
  @apply mt-1 flex items-center gap-1 truncate text-xs text-neutral-400;
}

.drive-muted {
  @apply truncate text-sm text-neutral-400;
}

.drive-cache-cell {
  @apply min-w-0;
}

.cache-badge {
  @apply inline-flex h-7 max-w-full items-center gap-1.5 rounded-full bg-neutral-100 px-2.5 text-xs font-medium text-neutral-500 dark:bg-neutral-900 dark:text-neutral-400;

  i {
    @apply text-sm;
  }

  &.ready {
    @apply bg-emerald-50 text-emerald-600 dark:bg-emerald-950/50 dark:text-emerald-300;
  }

  &.active {
    @apply bg-blue-50 text-blue-600 dark:bg-blue-950/50 dark:text-blue-300;
  }

  &.warning {
    @apply bg-amber-50 text-amber-600 dark:bg-amber-950/50 dark:text-amber-300;
  }

  &.paused {
    @apply bg-neutral-100 text-neutral-500 dark:bg-neutral-900 dark:text-neutral-300;
  }
}

.drive-row-actions {
  @apply flex justify-end gap-1 opacity-0 transition-opacity;

  button {
    @apply flex h-8 w-8 items-center justify-center rounded-full text-neutral-400 transition hover:bg-white hover:text-primary hover:shadow-sm dark:hover:bg-neutral-800;

    &:disabled {
      @apply cursor-not-allowed opacity-50 hover:bg-transparent hover:text-neutral-400 hover:shadow-none dark:hover:bg-transparent;
    }
  }
}

.drive-row:hover .drive-row-actions {
  @apply opacity-100;
}

.uploading-list {
  @apply mt-6 space-y-2;
}

.uploading-item {
  @apply flex items-center gap-3 rounded-2xl bg-neutral-50 px-4 py-3 dark:bg-neutral-950;

  > i {
    @apply flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-lg text-primary;
  }
}

@media (max-width: 900px) {
  .drive-table {
    @apply overflow-x-auto;
  }

  .drive-table-head,
  .drive-row {
    min-width: 1040px;
  }
}
</style>
