<template>
  <div v-if="isComponent ? favoriteSongs.length : true" class="favorite-page h-full flex flex-col">
    <!-- Header Section -->
    <div
      class="flex items-center justify-between px-6 py-4 flex-shrink-0"
      :class="setAnimationClass('animate__fadeInLeft')"
    >
      <div class="flex items-center gap-4">
        <div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white">
            {{ t('favorite.title') }}
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            {{ t('favorite.count', { count: favoriteList.length }) }}
          </p>
        </div>
      </div>

      <div v-if="!isComponent && isElectron" class="flex items-center gap-3">
        <template v-if="!isSelecting">
          <!-- Sort Controls -->
          <div class="flex items-center bg-gray-100 dark:bg-neutral-800 rounded-full p-1 h-9">
            <button
              v-for="isDesc in [true, false]"
              :key="String(isDesc)"
              class="px-3 h-full rounded-full text-xs font-medium transition-all duration-300 flex items-center gap-1"
              :class="
                isDescending === isDesc
                  ? 'bg-white dark:bg-neutral-700 text-gray-900 dark:text-white shadow-sm'
                  : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              "
              @click="toggleSort(isDesc)"
            >
              <i class="text-sm" :class="isDesc ? 'ri-sort-desc' : 'ri-sort-asc'"></i>
              {{ isDesc ? t('favorite.descending') : t('favorite.ascending') }}
            </button>
          </div>

          <button
            class="h-9 px-4 rounded-full bg-primary/10 hover:bg-primary text-primary hover:text-white text-xs font-medium transition-all duration-300 flex items-center gap-1.5"
            @click="startSelect"
          >
            <i class="ri-checkbox-multiple-line text-sm"></i>
            {{ t('favorite.batchDownload') }}
          </button>
        </template>

        <!-- Selection Controls -->
        <div
          v-else
          class="flex items-center gap-3 bg-white dark:bg-neutral-800 shadow-sm rounded-full px-4 py-1.5 border border-gray-100 dark:border-neutral-700 h-9"
        >
          <n-checkbox
            :checked="isAllSelected"
            :indeterminate="isIndeterminate"
            size="small"
            @update:checked="handleSelectAll"
          >
            <span class="text-xs">{{ t('common.selectAll') }}</span>
          </n-checkbox>
          <div class="h-3 w-px bg-gray-200 dark:bg-neutral-700 mx-1"></div>
          <div class="flex items-center gap-2">
            <button
              class="h-6 px-3 rounded-full bg-primary text-white text-xs font-medium hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
              :disabled="selectedSongs.length === 0"
              @click="handleBatchDownload"
            >
              <i class="ri-download-line"></i>
              {{ t('favorite.download', { count: selectedSongs.length }) }}
            </button>
            <button
              class="h-6 px-3 rounded-full bg-gray-100 dark:bg-neutral-700 text-gray-600 dark:text-gray-300 text-xs font-medium hover:bg-gray-200 dark:hover:bg-neutral-600 transition-colors"
              @click="cancelSelect"
            >
              {{ t('common.cancel') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-grow min-h-0 px-2" :class="setAnimationClass('animate__bounceInRight')">
      <n-scrollbar ref="scrollbarRef" class="h-full pr-4" @scroll="handleScroll">
        <div
          v-if="favoriteList.length === 0"
          class="h-full flex flex-col items-center justify-center text-gray-400"
        >
          <div
            class="w-24 h-24 rounded-full bg-gray-100 dark:bg-neutral-800 flex items-center justify-center mb-4"
          >
            <i class="ri-heart-line text-4xl text-gray-300 dark:text-gray-600"></i>
          </div>
          <p>{{ t('favorite.emptyTip') }}</p>
        </div>

        <div
          v-else
          class="space-y-1"
          :class="{ 'max-w-[400px]': isComponent }"
          :style="{ paddingBottom: contentPaddingBottom }"
        >
          <div class="favorite-list-section">
            <song-item
              v-for="(song, index) in renderedItems"
              :key="song.id"
              :item="song"
              :favorite="false"
              class="rounded-xl hover:bg-gray-100 dark:hover:bg-neutral-800 transition-colors"
              :class="[
                index < 20 ? setAnimationClass('animate__bounceInLeft') : '',
                { '!bg-primary/10': selectedSongs.includes(song.id) }
              ]"
              :style="index < 20 ? getItemAnimationDelay(index) : undefined"
              :selectable="isSelecting"
              :selected="selectedSongs.includes(song.id)"
              @play="handlePlay"
              @select="handleSelect"
            />
          </div>

          <!-- 未渲染项占位 -->
          <div v-if="placeholderHeight > 0" :style="{ height: placeholderHeight + 'px' }" />

          <div v-if="isComponent" class="pt-4 text-center">
            <n-button text type="primary" @click="handleMore">
              {{ t('common.viewMore') }} <i class="ri-arrow-right-s-line ml-1"></i>
            </n-button>
          </div>

          <!-- Loading Skeletons -->
          <div v-if="loading" class="space-y-2 pt-2">
            <div
              v-for="i in 5"
              :key="i"
              class="flex items-center gap-4 rounded-xl p-2 animate-pulse"
            >
              <div class="h-12 w-12 rounded-xl bg-gray-200 dark:bg-neutral-800"></div>
              <div class="flex-1 space-y-2">
                <div class="h-4 w-1/3 rounded bg-gray-200 dark:bg-neutral-800"></div>
                <div class="h-3 w-1/4 rounded bg-gray-200 dark:bg-neutral-800"></div>
              </div>
            </div>
          </div>

          <div v-if="noMore" class="text-center py-8 text-sm text-gray-400 dark:text-gray-500">
            {{ t('common.noMore') }}
          </div>
        </div>
      </n-scrollbar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { getMusicDetail } from '@/api/music';
import SongItem from '@/components/common/SongItem.vue';
import { useDownload } from '@/hooks/useDownload';
import { useProgressiveRender } from '@/hooks/useProgressiveRender';
import { useLocalMusicStore, usePlayerStore } from '@/store';
import { useMusicServerStore } from '@/store/modules/musicServer';
import type { SongResult } from '@/types/music';
import { DEFAULT_COVER_URL, isElectron, setAnimationClass, setAnimationDelay } from '@/utils';
import { toSongResult as toLocalSongResult } from '@/utils/localMusicUtils';

const { t } = useI18n();
const playerStore = usePlayerStore();
const musicServerStore = useMusicServerStore();
const localMusicStore = useLocalMusicStore();
const favoriteList = computed(() => playerStore.favoriteList);
const favoriteSongs = ref<SongResult[]>([]);
const loading = ref(false);
const noMore = ref(false);
const scrollbarRef = ref();

// 手工虚拟化
const {
  renderedItems,
  placeholderHeight,
  contentPaddingBottom,
  handleScroll: progressiveScroll,
  resetRenderLimit
} = useProgressiveRender({
  items: favoriteSongs,
  itemHeight: 64,
  listSelector: '.favorite-list-section',
  initialCount: 40,
  onReachEnd: () => {
    if (!loading.value && !noMore.value) {
      currentPage.value++;
      getFavoriteSongs();
    }
  }
});

const handleScroll = (e: Event) => {
  progressiveScroll(e);
};

// 多选相关
const isSelecting = ref(false);
const selectedSongs = ref<Array<number | string>>([]);
const { batchDownloadMusic } = useDownload();

// 开始多选
const startSelect = () => {
  isSelecting.value = true;
  selectedSongs.value = [];
};

// 取消多选
const cancelSelect = () => {
  isSelecting.value = false;
  selectedSongs.value = [];
};

// 处理选择
const handleSelect = (songId: number | string, selected: boolean) => {
  if (selected) {
    selectedSongs.value.push(songId);
  } else {
    selectedSongs.value = selectedSongs.value.filter((id) => id !== songId);
  }
};

// 批量下载
const handleBatchDownload = async () => {
  const selectedSongsList = selectedSongs.value
    .map((songId) => favoriteSongs.value.find((s) => s.id === songId))
    .filter((song) => song) as SongResult[];

  await batchDownloadMusic(selectedSongsList);
  cancelSelect();
};

// 排序相关
const isDescending = ref(true);

const toggleSort = (descending: boolean) => {
  if (isDescending.value === descending) return;
  isDescending.value = descending;
  currentPage.value = 1;
  favoriteSongs.value = [];
  noMore.value = false;
  resetRenderLimit();
  getFavoriteSongs();
};

// 无限滚动相关
const pageSize = 100;
const currentPage = ref(1);

const props = defineProps({
  isComponent: {
    type: Boolean,
    default: false
  }
});

const isMusicServerFavoriteKey = (id: number | string) =>
  typeof id === 'string' && id.startsWith('musicServer:');

const getMusicServerFavoriteId = (id: number | string) => {
  if (!isMusicServerFavoriteKey(id)) return null;
  const musicId = Number(String(id).replace('musicServer:', ''));
  return Number.isFinite(musicId) ? musicId : null;
};

const isNumericFavoriteId = (id: number | string) => {
  if (typeof id === 'number') return Number.isFinite(id);
  return /^\d+$/.test(id);
};

const getOrderedFavoriteKeys = () => {
  let ids = [...favoriteList.value];
  if (isDescending.value) {
    ids = ids.reverse();
  }
  return ids;
};

const getCurrentPageIds = () => {
  const ids = getOrderedFavoriteKeys();
  const startIndex = (currentPage.value - 1) * pageSize;
  return ids.slice(startIndex, startIndex + pageSize);
};

const ensureLocalMusicLoaded = async () => {
  if (localMusicStore.musicList.length === 0) {
    await localMusicStore.loadFromCache();
  }
};

const resolveFavoriteSongs = async (ids: Array<number | string>) => {
  const localSongMap = new Map(
    localMusicStore.musicList.map((entry) => [entry.id, toLocalSongResult(entry)])
  );

  const neteaseIds = ids
    .filter((id) => !isMusicServerFavoriteKey(id))
    .filter((id) => !localSongMap.has(String(id)))
    .filter(isNumericFavoriteId)
    .map((id) => Number(id));

  let neteaseSongs: SongResult[] = [];
  if (neteaseIds.length > 0) {
    const res = await getMusicDetail(neteaseIds);
    if (res.data.songs) {
      neteaseSongs = res.data.songs.map((song: SongResult) => ({
        ...song,
        picUrl: song.al?.picUrl || DEFAULT_COVER_URL,
        source: 'netease'
      }));
    }
  }

  return ids
    .map((id) => {
      const musicServerId = getMusicServerFavoriteId(id);
      if (musicServerId) {
        return musicServerStore.favoriteSongs.find((song) => Number(song.id) === musicServerId);
      }

      const localSong = localSongMap.get(String(id));
      if (localSong) {
        return localSong;
      }

      return neteaseSongs.find((song) => String(song.id) === String(id));
    })
    .filter((song): song is SongResult => !!song);
};

// 获取收藏歌曲详情，本地收藏从本地缓存/公开歌曲详情还原，云端收藏从 MusicServer 获取
const getFavoriteSongs = async () => {
  if (favoriteList.value.length === 0) {
    favoriteSongs.value = [];
    return;
  }

  if (props.isComponent && favoriteSongs.value.length >= 16) {
    return;
  }

  loading.value = true;
  try {
    await ensureLocalMusicLoaded();
    if (localStorage.getItem('musicServerToken')) {
      await musicServerStore.loadFavorites();
    }

    const currentIds = getCurrentPageIds();
    const newSongs = await resolveFavoriteSongs(currentIds);

    if (currentPage.value === 1) {
      favoriteSongs.value = newSongs;
    } else {
      favoriteSongs.value = [...favoriteSongs.value, ...newSongs];
    }

    noMore.value = currentPage.value * pageSize >= getOrderedFavoriteKeys().length;
  } catch (error) {
    console.error('获取收藏歌曲失败:', error);
  } finally {
    loading.value = false;
  }
};

const hasLoaded = ref(false);

onMounted(async () => {
  if (!hasLoaded.value) {
    await playerStore.initializeFavoriteList();
    await getFavoriteSongs();
    hasLoaded.value = true;
  }
});

watch(
  favoriteList,
  async () => {
    hasLoaded.value = false;
    currentPage.value = 1;
    noMore.value = false;
    resetRenderLimit();
    await getFavoriteSongs();
    hasLoaded.value = true;
  },
  { deep: true }
);

const handlePlay = () => {
  playerStore.setPlayList(favoriteSongs.value);
};

const getItemAnimationDelay = (index: number) => {
  return setAnimationDelay(index, 30);
};

const router = useRouter();
const handleMore = () => {
  router.push('/history');
};

// 全选相关
const isAllSelected = computed(() => {
  return (
    favoriteSongs.value.length > 0 && selectedSongs.value.length === favoriteSongs.value.length
  );
});

const isIndeterminate = computed(() => {
  return selectedSongs.value.length > 0 && selectedSongs.value.length < favoriteSongs.value.length;
});

const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedSongs.value = favoriteSongs.value.map((song) => song.id);
  } else {
    selectedSongs.value = [];
  }
};
</script>

<style lang="scss" scoped>
/* Scoped styles kept minimal as we use Tailwind classes */
</style>
