<template>
  <div class="local-music-page h-full w-full bg-white dark:bg-black transition-colors duration-500">
    <n-scrollbar class="h-full">
      <div class="local-music-content pb-32">
        <!-- Hero Section -->
        <section class="hero-section relative overflow-hidden rounded-tl-2xl">
          <!-- 背景模糊效果 -->
          <div class="hero-bg absolute inset-0 -top-20">
            <div
              class="absolute inset-0 bg-gradient-to-br from-primary/20 via-transparent to-primary/10 blur-3xl opacity-50 dark:opacity-30"
            ></div>
            <div
              class="absolute inset-0 bg-gradient-to-b from-transparent via-white/80 to-white dark:via-black/80 dark:to-black"
            ></div>
          </div>

          <!-- Hero 内容 -->
          <div class="hero-content relative z-10 page-padding-x pt-6 pb-4">
            <div class="flex items-center gap-5">
              <div
                class="cover-container relative w-20 h-20 rounded-2xl bg-primary/10 flex items-center justify-center shadow-lg ring-2 ring-white/50 dark:ring-neutral-800/50 shrink-0"
              >
                <i class="ri-folder-music-fill text-4xl text-primary opacity-80" />
              </div>

              <div class="info-content min-w-0">
                <h1
                  class="text-2xl md:text-3xl font-bold text-neutral-900 dark:text-white tracking-tight"
                >
                  {{ t('localMusic.title') }}
                </h1>
                <p class="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
                  {{ t('localMusic.songCount', { count: localMusicStore.musicList.length }) }}
                </p>
              </div>
            </div>
          </div>
        </section>

        <!-- Action Bar (Sticky on scroll) -->
        <section
          class="action-bar sticky top-0 z-20 page-padding-x py-3 md:py-4 bg-white/80 dark:bg-black/80 backdrop-blur-xl border-b border-neutral-100 dark:border-neutral-800/50"
        >
          <div class="flex items-center justify-between gap-4">
            <!-- 左侧：搜索框 -->
            <div class="flex-1 max-w-xs">
              <n-input
                v-model:value="searchKeyword"
                :placeholder="t('localMusic.search')"
                clearable
                size="small"
                round
              >
                <template #prefix>
                  <i class="ri-search-line text-neutral-400" />
                </template>
              </n-input>
            </div>

            <!-- 中间：排序方式（仅在有歌曲时显示） -->
            <n-dropdown
              v-if="filteredList.length > 0 || localMusicStore.musicList.length > 0"
              trigger="click"
              :options="sortDropdownOptions"
              @select="(key: string) => handleSortChange(key as SortKey)"
            >
              <button
                class="action-btn-pill flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-all bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-800"
                :title="t('localMusic.sortBy')"
              >
                <i class="ri-sort-asc" v-if="sortAsc" />
                <i class="ri-sort-desc" v-else />
                <span class="hidden md:inline">{{ activeSortLabel }}</span>
              </button>
            </n-dropdown>

            <!-- 右侧：操作按钮 -->
            <div class="flex items-center gap-3">
              <!-- 播放全部按钮 -->
              <button
                v-if="filteredList.length > 0"
                class="action-btn-pill flex items-center gap-2 px-4 py-2 rounded-full font-semibold text-sm transition-all bg-primary text-white hover:bg-primary/90"
                @click="handlePlayAll"
              >
                <i class="ri-play-fill text-lg" />
                <span class="hidden md:inline">{{ t('localMusic.playAll') }}</span>
              </button>

              <!-- 扫描按钮 -->
              <button
                class="action-btn-icon w-10 h-10 rounded-full flex items-center justify-center bg-neutral-100 dark:bg-neutral-900 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-all"
                :disabled="localMusicStore.scanning"
                @click="handleScan"
              >
                <i
                  class="ri-refresh-line text-lg"
                  :class="{ 'animate-spin': localMusicStore.scanning }"
                />
              </button>

              <!-- 添加文件夹按钮 -->
              <button
                class="action-btn-icon w-10 h-10 rounded-full flex items-center justify-center bg-neutral-100 dark:bg-neutral-900 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-all"
                @click="handleAddFolder"
              >
                <i class="ri-folder-add-line text-lg" />
              </button>

              <!-- 文件夹管理按钮 -->
              <button
                v-if="localMusicStore.folderPaths.length > 0"
                class="action-btn-icon w-10 h-10 rounded-full flex items-center justify-center bg-neutral-100 dark:bg-neutral-900 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-neutral-800 transition-all"
                @click="showFolderManager = true"
              >
                <i class="ri-folder-settings-line text-lg" />
              </button>
            </div>
          </div>
        </section>

        <!-- 扫描进度提示 -->
        <section v-if="localMusicStore.scanning" class="page-padding-x mt-6">
          <div
            class="flex items-center gap-4 p-4 rounded-2xl bg-primary/5 dark:bg-primary/10 border border-primary/20"
          >
            <n-spin size="small" />
            <div>
              <p class="text-sm font-medium text-neutral-900 dark:text-white">
                {{ t('localMusic.scanning') }}
              </p>
              <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                {{ t('localMusic.songCount', { count: localMusicStore.scanProgress }) }}
              </p>
            </div>
          </div>
        </section>

        <!-- 歌曲列表 -->
        <section class="list-section page-padding-x mt-6">
          <!-- 空状态 -->
          <div
            v-if="!localMusicStore.scanning && filteredList.length === 0"
            class="empty-state py-20 text-center"
          >
            <i class="ri-folder-music-fill text-5xl mb-4 text-neutral-200 dark:text-neutral-800" />
            <p class="text-neutral-400">{{ t('localMusic.emptyState') }}</p>
            <button
              class="mt-6 px-6 py-2 rounded-full bg-primary text-white text-sm font-medium hover:bg-primary/90 transition-all"
              @click="handleAddFolder"
            >
              <i class="ri-folder-add-line mr-2" />
              {{ t('localMusic.scanFolder') }}
            </button>
          </div>

          <!-- 歌曲列表 -->
          <div v-else-if="filteredList.length > 0" class="song-list-container">
            <song-item
              v-for="(item, index) in filteredSongResults"
              :key="item.id"
              :index="index"
              :item="item"
              manual-play
              @play="handlePlaySong"
            >
              <!-- 自定义副标题：碟号 / 曲目号 / 年份，与 AndroidMusicPlayer 一致 -->
              <template v-if="formatSubtitle(filteredList[index]!)" #subtitle>
                <span class="local-track-subtitle">
                  {{ formatSubtitle(filteredList[index]!) }}
                </span>
              </template>
            </song-item>
          </div>
        </section>
      </div>
    </n-scrollbar>

    <!-- 文件夹管理抽屉 -->
    <n-drawer v-model:show="showFolderManager" :width="400" placement="right">
      <n-drawer-content :title="t('localMusic.removeFolder')" closable>
        <div class="space-y-3 py-4">
          <div
            v-for="folder in localMusicStore.folderPaths"
            :key="folder"
            class="flex items-center justify-between p-3 rounded-xl bg-neutral-50 dark:bg-neutral-900 border border-neutral-100 dark:border-neutral-800"
          >
            <div class="flex items-center gap-3 min-w-0 flex-1">
              <i class="ri-folder-line text-lg text-primary flex-shrink-0" />
              <div class="min-w-0 flex-1">
                <span class="text-sm text-neutral-700 dark:text-neutral-300 truncate block">
                  {{ folder }}
                </span>
                <span
                  v-if="getFolderSongCount(folder) > 0"
                  class="text-[11px] text-neutral-400 dark:text-neutral-500"
                >
                  {{ t('localMusic.songCount', { count: getFolderSongCount(folder) }) }}
                </span>
              </div>
            </div>
            <n-popconfirm
              :positive-text="t('common.confirm')"
              :negative-text="t('common.cancel')"
              @positive-click="handleRemoveFolder(folder)"
            >
              <template #trigger>
                <button
                  class="w-8 h-8 rounded-full flex items-center justify-center text-neutral-400 hover:text-red-500 hover:bg-red-500/10 transition-all flex-shrink-0 ml-2"
                >
                  <i class="ri-delete-bin-line" />
                </button>
              </template>
              {{
                getFolderSongCount(folder) > 0
                  ? t('localMusic.confirmRemoveFolder', { count: getFolderSongCount(folder) })
                  : t('localMusic.confirmRemoveFolderEmpty')
              }}
            </n-popconfirm>
          </div>

          <!-- 空文件夹列表 -->
          <div v-if="localMusicStore.folderPaths.length === 0" class="text-center py-8">
            <i class="ri-folder-line text-4xl text-neutral-200 dark:text-neutral-800" />
            <p class="text-sm text-neutral-400 mt-2">{{ t('localMusic.emptyState') }}</p>
          </div>
        </div>

        <template #footer>
          <n-button type="primary" block @click="handleAddFolder">
            <template #icon>
              <i class="ri-folder-add-line" />
            </template>
            {{ t('localMusic.scanFolder') }}
          </n-button>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { createDiscreteApi } from 'naive-ui';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import SongItem from '@/components/common/SongItem.vue';
import { useLocalMusicStore } from '@/store/modules/localMusic';
import { usePlayerStore } from '@/store/modules/player';
import type { SongResult } from '@/types/music';
import { filterByKeyword, sortByTrackOrder, toSongResult } from '@/utils/localMusicUtils';

// ==================== Stores ====================
const { t } = useI18n();
const { message } = createDiscreteApi(['message']);
const localMusicStore = useLocalMusicStore();
const playerStore = usePlayerStore();

// ==================== State ====================
/** 搜索关键词 */
const searchKeyword = ref('');
/** 文件夹管理抽屉是否显示 */
const showFolderManager = ref(false);
/** 排序方式：默认按 (碟号, 曲目号) 升序，更贴近 AndroidMusicPlayer 的体验 */
type SortKey = 'track' | 'title' | 'artist' | 'album' | 'duration' | 'year' | 'added';
const sortKey = ref<SortKey>('track');
/** 排序方向 */
const sortAsc = ref(true);

const sortOptions: { key: SortKey; labelKey: string }[] = [
  { key: 'track', labelKey: 'localMusic.sortTrack' },
  { key: 'title', labelKey: 'localMusic.sortTitle' },
  { key: 'artist', labelKey: 'localMusic.sortArtist' },
  { key: 'album', labelKey: 'localMusic.sortAlbum' },
  { key: 'duration', labelKey: 'localMusic.sortDuration' },
  { key: 'year', labelKey: 'localMusic.sortYear' },
  { key: 'added', labelKey: 'localMusic.sortAdded' }
];

/** n-dropdown 用的选项格式（key + label） */
const sortDropdownOptions = computed(() =>
  sortOptions.map((opt) => ({
    key: opt.key,
    label: t(opt.labelKey)
  }))
);

/** 当前排序方式的可读文本（用于按钮显示） */
const activeSortLabel = computed(() => {
  const opt = sortOptions.find((o) => o.key === sortKey.value);
  return opt ? t(opt.labelKey) : '';
});

// ==================== Computed ====================
/**
 * 本地音乐按 (碟号, 曲目号) 升序的"自然顺序"列表。
 * 搜索框为空时直接使用；非空时也用这个顺序作为基础，再做关键词过滤，
 * 这样可以避免"搜一次就乱序"的问题。
 */
const trackOrderedList = computed(() => sortByTrackOrder(localMusicStore.musicList));

/** 根据当前排序方式生成最终列表 */
const sortedList = computed(() => {
  const list = [...trackOrderedList.value];
  const dir = sortAsc.value ? 1 : -1;
  switch (sortKey.value) {
    case 'title':
      list.sort((a, b) => dir * a.title.localeCompare(b.title));
      break;
    case 'artist':
      list.sort((a, b) => dir * a.artist.localeCompare(b.artist) || a.title.localeCompare(b.title));
      break;
    case 'album':
      list.sort(
        (a, b) =>
          dir *
          (a.album.localeCompare(b.album) ||
            // 同一专辑内按 (碟号, 曲目号) 排，符合 Android 体验
            (a.discNumber || 1) - (b.discNumber || 1) ||
            (a.trackNumber || 0) - (b.trackNumber || 0))
      );
      break;
    case 'duration':
      list.sort((a, b) => dir * (a.duration - b.duration));
      break;
    case 'year':
      list.sort((a, b) => dir * ((a.year || 0) - (b.year || 0)));
      break;
    case 'added':
      list.sort((a, b) => dir * (a.modifiedTime - b.modifiedTime));
      break;
    case 'track':
    default:
      // 已是 trackOrderedList
      if (!sortAsc.value) list.reverse();
      break;
  }
  return list;
});

/** 根据搜索关键词过滤后的本地音乐列表（保持当前排序） */
const filteredList = computed(() => {
  return filterByKeyword(sortedList.value, searchKeyword.value);
});

/** 将过滤后的列表转换为 SongResult[] 供 SongItem 使用 */
const filteredSongResults = computed(() => {
  return filteredList.value.map(toSongResult);
});

/** 用于渲染"次要信息行"：碟号、曲目号、年份 */
function formatSubtitle(entry: {
  discNumber: number;
  trackNumber: number;
  trackTotal: number;
  year: number;
}): string {
  const parts: string[] = [];
  if (entry.discNumber > 1) {
    // 单碟时一般不写 discNumber 标签，避免噪音
    parts.push(t('localMusic.disc', { no: entry.discNumber }));
  }
  if (entry.trackNumber > 0) {
    if (entry.trackTotal > 0) {
      parts.push(
        t('localMusic.trackInfoWithTotal', { no: entry.trackNumber, total: entry.trackTotal })
      );
    } else {
      parts.push(t('localMusic.trackInfo', { no: entry.trackNumber }));
    }
  }
  if (entry.year > 0) {
    parts.push(t('localMusic.yearLabel', { year: entry.year }));
  }
  return parts.join(' · ');
}

// ==================== Methods ====================

/**
 * 选择并添加文件夹
 * 调用系统文件夹选择对话框
 * dialog.showOpenDialog 返回 { canceled: boolean, filePaths: string[] }
 */
async function handleAddFolder(): Promise<void> {
  try {
    const result = await window.electron.ipcRenderer.invoke('select-directory');
    if (result && !result.canceled && result.filePaths?.length > 0) {
      localMusicStore.addFolder(result.filePaths[0]);
      // 添加文件夹后自动触发扫描
      await localMusicStore.scanFolders();
    }
  } catch (error) {
    console.error('选择文件夹失败:', error);
    message.error(String(error));
  }
}

/**
 * 移除文件夹（并删除该目录下的所有已缓存歌曲）
 * @param folder 要移除的文件夹路径
 */
async function handleRemoveFolder(folder: string): Promise<void> {
  const removed = await localMusicStore.removeFolder(folder);
  if (removed > 0) {
    message.success(t('localMusic.removedFolderWithCount', { count: removed }));
  } else {
    message.success(t('localMusic.removedFolder'));
  }
}

/**
 * 计算某个目录下已缓存的歌曲数
 * 用于在删除前向用户展示"将删除 N 首歌"
 */
function getFolderSongCount(folder: string): number {
  // 统一用 '/' 比对，避免 Windows 上 \ 与 / 混用漏判
  const normalize = (p: string) => p.replace(/\\/g, '/').toLowerCase();
  const normalizedFolder = normalize(folder).replace(/\/$/, '');
  return localMusicStore.musicList.filter((entry) => {
    const fp = normalize(entry.filePath);
    return fp === normalizedFolder || fp.startsWith(normalizedFolder + '/');
  }).length;
}

/**
 * 触发扫描
 */
async function handleScan(): Promise<void> {
  if (localMusicStore.folderPaths.length === 0) {
    // 没有配置文件夹时，引导用户先添加文件夹
    await handleAddFolder();
    return;
  }
  await localMusicStore.scanFolders();
}

/**
 * 切换排序方式：相同 key 反向，不同 key 设为正向
 */
function handleSortChange(key: SortKey): void {
  if (sortKey.value === key) {
    sortAsc.value = !sortAsc.value;
  } else {
    sortKey.value = key;
    sortAsc.value = true;
  }
}

/**
 * 播放单曲
 * 本地音乐需要先重建 local:// 播放地址，再由播放列表 Store 统一触发播放。
 * @param song SongItem 组件 emit 的 SongResult 对象
 */
async function handlePlaySong(song: SongResult): Promise<void> {
  try {
    const entryIndex = filteredList.value.findIndex((entry) => entry.id === song.id);
    if (entryIndex === -1) return;

    const entry = filteredList.value[entryIndex];
    const exists = await window.electron.ipcRenderer.invoke('check-file-exists', entry.filePath);
    if (!exists) {
      message.error(t('localMusic.fileNotFound'));
      return;
    }

    const songs = filteredList.value.map(toSongResult);
    playerStore.setPlayList(songs);
    await playerStore.setPlay(songs[entryIndex]);
  } catch (error) {
    console.error('播放本地音乐失败:', error);
  }
}

/**
 * 播放全部
 * 将完整列表转换为 SongResult[] 后设置为播放列表并从第一首开始播放
 */
async function handlePlayAll(): Promise<void> {
  if (filteredSongResults.value.length === 0) return;

  try {
    const firstSong = filteredSongResults.value[0];
    const entry = filteredList.value[0];

    // 检查第一首歌文件是否存在
    const exists = await window.electron.ipcRenderer.invoke('check-file-exists', entry.filePath);
    if (!exists) {
      message.error(t('localMusic.fileNotFound'));
      return;
    }

    // 设置播放列表并播放第一首
    playerStore.setPlayList(filteredSongResults.value);
    await playerStore.setPlay(firstSong);
  } catch (error) {
    console.error('播放全部失败:', error);
  }
}

// ==================== Lifecycle ====================
onMounted(async () => {
  // 进入页面时从 IndexedDB 缓存加载音乐列表
  await localMusicStore.loadFromCache();
});
</script>

<style scoped>
/* 虚拟列表样式 */
.song-virtual-list {
  @apply w-full;
}

.song-virtual-list :deep(.n-virtual-list__scroll) {
  scrollbar-width: thin;
}

.song-virtual-list :deep(.n-virtual-list__scroll)::-webkit-scrollbar {
  width: 6px;
}

.song-virtual-list :deep(.n-virtual-list__scroll)::-webkit-scrollbar-thumb {
  @apply bg-neutral-300 dark:bg-neutral-700 rounded-full;
}

.song-virtual-list :deep(.n-virtual-list__scroll)::-webkit-scrollbar-track {
  @apply bg-transparent;
}

/* 本地音乐扩展副标题：碟号 / 曲目号 / 年份 */
.local-track-subtitle {
  display: inline-block;
  font-size: 11px;
  color: rgb(156 163 175); /* text-gray-400 */
  line-height: 1.2;
}

.dark .local-track-subtitle {
  color: rgb(107 114 128); /* dark:text-gray-500 */
}
</style>
