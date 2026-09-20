<template>
  <div class="reparse-panel bg-light-100 dark:bg-dark-100 p-4 rounded-xl">
    <div class="text-base font-medium mb-2">{{ t('player.reparse.title') }}</div>
    <div class="text-sm opacity-70 mb-3">{{ t('player.reparse.desc') }}</div>
    <div class="mb-3 max-h-80 overflow-y-auto">
      <div class="flex flex-col space-y-2">
        <template v-for="(group, groupIndex) in groupedSources" :key="group.key">
          <div
            v-if="groupIndex > 0"
            class="border-t border-gray-200 dark:border-gray-700 my-1"
          ></div>
          <div
            v-for="source in group.sources"
            :key="source.id"
            class="source-button flex items-center p-2 rounded-lg transition-all duration-200"
            :class="[
              source.available
                ? 'cursor-pointer bg-light-200 dark:bg-dark-200 hover:bg-light-300 dark:hover:bg-dark-300'
                : 'opacity-40 cursor-not-allowed bg-light-200 dark:bg-dark-200',
              {
                'bg-green-50 dark:bg-green-900/20 text-green-500': isCurrentSource(source.id),
                'opacity-50 cursor-not-allowed': isReparsing && source.available
              }
            ]"
            @click="source.available && handleSourceClick(source)"
          >
            <div
              class="flex items-center justify-center w-6 h-6 mr-3 text-lg"
              :style="{ color: source.color }"
            >
              <i :class="source.icon"></i>
            </div>
            <div class="flex-1 text-sm whitespace-nowrap overflow-hidden text-ellipsis">
              <span>{{ source.label }}</span>
              <n-tooltip v-if="!source.available && source.configHint" trigger="hover">
                <template #trigger>
                  <i class="ri-information-line text-xs ml-1 opacity-60"></i>
                </template>
                {{ t(source.configHint) }}
              </n-tooltip>
            </div>
            <div
              v-if="isReparsing && currentReparsingId === source.id"
              class="w-5 h-5 flex items-center justify-center"
            >
              <i class="ri-loader-4-line animate-spin"></i>
            </div>
            <div
              v-else-if="isCurrentSource(source.id)"
              class="w-5 h-5 flex items-center justify-center"
            >
              <i class="ri-check-line"></i>
            </div>
          </div>
        </template>
      </div>
    </div>
    <div
      class="text-red-500 text-sm flex items-center bg-light-200 dark:bg-dark-200 rounded-lg p-2 cursor-pointer"
      @click="clearCustomSource"
    >
      <div class="flex items-center justify-center w-6 h-6 mr-3 text-lg">
        <i class="ri-close-circle-line"></i>
      </div>
      <div>
        {{ t('player.reparse.clear') }}
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useMessage } from 'naive-ui';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import { CacheManager } from '@/api/musicParser';
import { playMusic } from '@/hooks/MusicHook';
import { initLxMusicRunner, setLxMusicRunner } from '@/services/LxMusicSourceRunner';
import { reparseCurrentSong } from '@/services/playbackController';
import { SongSourceConfigManager } from '@/services/SongSourceConfigManager';
import { useSettingsStore } from '@/store';
import type { LxMusicScriptConfig } from '@/types/lxMusic';
import type { Platform } from '@/types/music';
import { type MusicSourceGroup, useMusicSources } from '@/utils/musicSourceConfig';

type ReparseSourceItem = {
  id: string;
  platform: Platform;
  label: string;
  icon: string;
  color: string;
  group: MusicSourceGroup;
  available: boolean;
  configHint?: string;
  lxScriptId?: string;
};

const settingsStore = useSettingsStore();
const { t } = useI18n();
const message = useMessage();
const { allSources } = useMusicSources();
const isReparsing = ref(false);
const currentReparsingId = ref<string | null>(null);
const selectedSourceId = ref<string | null>(null);

const reparseSourceList = computed<ReparseSourceItem[]>(() => {
  const result: ReparseSourceItem[] = [];
  for (const source of allSources.value) {
    if (source.key === 'lxMusic') {
      const scripts: LxMusicScriptConfig[] = settingsStore.setData.lxMusicScripts || [];
      for (const script of scripts) {
        result.push({
          id: `lxMusic:${script.id}`,
          platform: 'lxMusic',
          label: script.name,
          icon: source.icon,
          color: source.color,
          group: source.group,
          available: true,
          lxScriptId: script.id
        });
      }
      if (scripts.length === 0) {
        result.push({
          id: 'lxMusic',
          platform: 'lxMusic',
          label: 'lxMusic',
          icon: source.icon,
          color: source.color,
          group: source.group,
          available: false,
          configHint: 'settings.playback.lxMusic.scripts.notConfigured'
        });
      }
    } else {
      result.push({
        id: source.key,
        platform: source.key,
        label: source.key,
        icon: source.icon,
        color: source.color,
        group: source.group,
        available: source.available,
        configHint: source.configHint
      });
    }
  }
  return result;
});

const GROUP_ORDER: MusicSourceGroup[] = ['unblock', 'extended', 'plugin'];

const groupedSources = computed(() => {
  return GROUP_ORDER.map((groupKey) => ({
    key: groupKey,
    sources: reparseSourceList.value.filter((source) => source.group === groupKey)
  })).filter((group) => group.sources.length > 0);
});

const isCurrentSource = (sourceId: string) => selectedSourceId.value === sourceId;

const initSelectedSources = () => {
  const songId = playMusic.value.id;
  const config = SongSourceConfigManager.getConfig(songId);

  if (config && config.sources.length > 0) {
    const platform = config.sources[0];
    if (platform === 'lxMusic') {
      const activeId = settingsStore.setData.activeLxMusicApiId;
      selectedSourceId.value = activeId ? `lxMusic:${activeId}` : null;
    } else {
      selectedSourceId.value = platform;
    }
  } else {
    selectedSourceId.value = null;
  }
};

const clearCustomSource = () => {
  SongSourceConfigManager.clearConfig(playMusic.value.id);
  selectedSourceId.value = null;
};

const handleSourceClick = async (source: ReparseSourceItem) => {
  if (source.lxScriptId) {
    await reparseWithLxScript(source);
  } else {
    await directReparseMusic(source);
  }
};

const reparseWithLxScript = async (source: ReparseSourceItem) => {
  if (isReparsing.value || !source.lxScriptId) return;

  const scripts: LxMusicScriptConfig[] = settingsStore.setData.lxMusicScripts || [];
  const script = scripts.find((item) => item.id === source.lxScriptId);
  if (!script) return;

  try {
    isReparsing.value = true;
    currentReparsingId.value = source.id;
    setLxMusicRunner(null);
    await initLxMusicRunner(script.script);
    settingsStore.setSetData({ activeLxMusicApiId: script.id });

    const songId = Number(playMusic.value.id);
    await CacheManager.clearMusicCache(songId);

    selectedSourceId.value = source.id;
    SongSourceConfigManager.setConfig(songId, ['lxMusic'], 'manual');

    const success = await reparseCurrentSong('lxMusic', false);
    message[success ? 'success' : 'error'](t(success ? 'player.reparse.success' : 'player.reparse.failed'));
  } catch (error) {
    console.error('解析失败:', error);
    message.error(t('player.reparse.failed'));
  } finally {
    isReparsing.value = false;
    currentReparsingId.value = null;
  }
};

const directReparseMusic = async (source: ReparseSourceItem) => {
  if (isReparsing.value) return;

  try {
    isReparsing.value = true;
    currentReparsingId.value = source.id;

    const songId = Number(playMusic.value.id);
    await CacheManager.clearMusicCache(songId);

    selectedSourceId.value = source.id;
    SongSourceConfigManager.setConfig(songId, [source.platform], 'manual');

    const success = await reparseCurrentSong(source.platform, false);
    message[success ? 'success' : 'error'](t(success ? 'player.reparse.success' : 'player.reparse.failed'));
  } catch (error) {
    console.error('解析失败:', error);
    message.error(t('player.reparse.failed'));
  } finally {
    isReparsing.value = false;
    currentReparsingId.value = null;
  }
};

watch(
  () => playMusic.value.id,
  () => {
    if (playMusic.value.id) {
      initSelectedSources();
    }
  },
  { immediate: true }
);
</script>

<style lang="scss" scoped>
.reparse-panel {
  width: 260px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

.source-button {
  &:hover:not(.opacity-50):not(.opacity-40) {
    @apply transform -translate-y-0.5 shadow-sm;
  }
}
</style>
