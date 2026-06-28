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
                  {{ cloudStore.isLoggedIn ? cloudStore.user?.displayName : cloudStore.baseUrl }}
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
            class="max-w-xl rounded-lg border border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-950 p-5"
          >
            <div class="grid gap-4">
              <n-input v-model:value="baseUrlForm" placeholder="MusicServer 地址">
                <template #prefix>
                  <i class="ri-server-line text-neutral-400" />
                </template>
              </n-input>
              <n-input v-model:value="username" placeholder="用户名（3-80 位）">
                <template #prefix>
                  <i class="ri-user-line text-neutral-400" />
                </template>
              </n-input>
              <n-input
                v-model:value="password"
                type="password"
                show-password-on="click"
                placeholder="密码（8-120 位）"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <i class="ri-lock-line text-neutral-400" />
                </template>
              </n-input>
              <n-input v-model:value="displayName" placeholder="显示名称（注册时可填，最多 120 位）">
                <template #prefix>
                  <i class="ri-id-card-line text-neutral-400" />
                </template>
              </n-input>
              <div class="flex flex-col sm:flex-row gap-3">
                <n-button type="primary" :loading="authLoading" class="flex-1" @click="handleLogin">
                  登录
                </n-button>
                <n-button :loading="authLoading" class="flex-1" @click="handleRegister">
                  注册并登录
                </n-button>
              </div>
            </div>
          </div>
        </section>

        <template v-else>
          <section class="page-padding-x mt-5">
            <div
              class="rounded-lg border border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-950 p-4"
            >
              <input
                ref="fileInput"
                type="file"
                accept="audio/*"
                class="hidden"
                @change="handleFileChange"
              />
              <div class="grid grid-cols-1 lg:grid-cols-[1fr_1fr_1fr_auto_auto] gap-3 items-center">
                <n-input v-model:value="uploadTitle" placeholder="标题（可选）" clearable />
                <n-input v-model:value="uploadArtist" placeholder="艺术家（可选）" clearable />
                <n-input v-model:value="uploadAlbum" placeholder="专辑（可选）" clearable />
                <n-button @click="fileInput?.click()">
                  <template #icon>
                    <i class="ri-folder-open-line" />
                  </template>
                  {{ selectedFile ? selectedFile.name : '选择文件' }}
                </n-button>
                <n-button
                  type="primary"
                  :loading="cloudStore.uploading"
                  :disabled="!selectedFile"
                  @click="handleUpload"
                >
                  <template #icon>
                    <i class="ri-upload-cloud-line" />
                  </template>
                  上传
                </n-button>
              </div>
            </div>
          </section>

          <section class="page-padding-x mt-5">
            <n-tabs v-model:value="activeTab" type="line" animated>
              <n-tab-pane name="music" tab="音乐">
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 py-3">
                  <n-input
                    v-model:value="searchKeyword"
                    placeholder="搜索云端音乐"
                    clearable
                    class="sm:max-w-xs"
                  >
                    <template #prefix>
                      <i class="ri-search-line text-neutral-400" />
                    </template>
                  </n-input>
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
                </div>

                <div v-if="filteredMusic.length === 0" class="py-20 text-center text-neutral-400">
                  <i class="ri-cloud-off-line text-5xl mb-3 block text-neutral-200 dark:text-neutral-800" />
                  暂无云端音乐
                </div>
                <div v-else class="song-list-container">
                  <song-item
                    v-for="(song, index) in filteredSongResults"
                    :key="song.id"
                    :item="song"
                    :index="index"
                    can-remove
                    @play="handlePlaySongResult(song, filteredSongResults)"
                    @remove-song="handleDeleteMusic(Number($event))"
                  />
                </div>
              </n-tab-pane>

              <n-tab-pane name="favorites" tab="收藏">
                <div v-if="cloudStore.favoriteSongs.length === 0" class="py-20 text-center text-neutral-400">
                  <i class="ri-heart-line text-5xl mb-3 block text-neutral-200 dark:text-neutral-800" />
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
              </n-tab-pane>

              <n-tab-pane name="playlists" tab="歌单">
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
              </n-tab-pane>
            </n-tabs>
          </section>
        </template>
      </div>
    </n-scrollbar>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios';
import { createDiscreteApi } from 'naive-ui';
import { computed, onMounted, ref, watch } from 'vue';

import SongItem from '@/components/common/SongItem.vue';
import { useMusicServerStore } from '@/store/modules/musicServer';
import { usePlayerStore } from '@/store/modules/player';
import type { SongResult } from '@/types/music';
import type { MusicServerMusic } from '@/types/musicServer';
import { toMusicServerSongResult } from '@/utils/musicServerUtils';

const { message } = createDiscreteApi(['message']);
const cloudStore = useMusicServerStore();
const playerStore = usePlayerStore();

const activeTab = ref('music');
const activePlaylistId = ref<number | null>(null);
const authLoading = ref(false);
const baseUrlForm = ref(cloudStore.baseUrl);
const username = ref('');
const password = ref('');
const displayName = ref('');
const searchKeyword = ref('');
const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const uploadTitle = ref('');
const uploadArtist = ref('');
const uploadAlbum = ref('');
const newPlaylistName = ref('');
const playlistTrackMusicId = ref<number | null>(null);

const filteredMusic = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return cloudStore.musicList;
  return cloudStore.musicList.filter((music) =>
    [music.title, music.artist, music.album, music.originalFilename]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(keyword))
  );
});

const favoriteMusics = computed(() => cloudStore.favorites.map((item) => item.music));

const filteredSongResults = computed(() => filteredMusic.value.map(toMusicServerSongResult));

const favoriteSongResults = computed(() => favoriteMusics.value.map(toMusicServerSongResult));

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

function applyBaseUrl() {
  cloudStore.setBaseUrl(baseUrlForm.value);
  baseUrlForm.value = cloudStore.baseUrl;
}

function validateAuthForm() {
  applyBaseUrl();
  const trimmedUsername = username.value.trim();
  const trimmedDisplayName = displayName.value.trim();
  if (!trimmedUsername || !password.value) {
    message.warning('请填写用户名和密码');
    return false;
  }
  if (trimmedUsername.length < 3 || trimmedUsername.length > 80) {
    message.warning('用户名长度需在 3 到 80 位之间');
    return false;
  }
  if (password.value.length < 8 || password.value.length > 120) {
    message.warning('密码长度需在 8 到 120 位之间');
    return false;
  }
  if (trimmedDisplayName.length > 120) {
    message.warning('显示名称不能超过 120 位');
    return false;
  }
  return true;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

async function handleLogin() {
  if (!validateAuthForm()) return;
  authLoading.value = true;
  try {
    await cloudStore.login({
      username: username.value.trim(),
      password: password.value
    });
    await playerStore.initializeFavoriteList();
    message.success('登录成功');
  } catch (error) {
    console.error('MusicServer 登录失败:', error);
    message.error(getErrorMessage(error, '登录失败'));
  } finally {
    authLoading.value = false;
  }
}

async function handleRegister() {
  if (!validateAuthForm()) return;
  authLoading.value = true;
  try {
    await cloudStore.register({
      username: username.value.trim(),
      password: password.value,
      displayName: displayName.value.trim() || undefined
    });
    await playerStore.initializeFavoriteList();
    message.success('注册成功');
  } catch (error) {
    console.error('MusicServer 注册失败:', error);
    message.error(getErrorMessage(error, '注册失败'));
  } finally {
    authLoading.value = false;
  }
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
  const file = target.files?.[0] || null;
  selectedFile.value = file;
  if (file && !uploadTitle.value.trim()) {
    uploadTitle.value = file.name.replace(/\.[^.]+$/, '');
  }
}

async function handleUpload() {
  if (!selectedFile.value) return;

  try {
    await cloudStore.upload(selectedFile.value, {
      title: uploadTitle.value,
      artist: uploadArtist.value,
      album: uploadAlbum.value
    });
    selectedFile.value = null;
    uploadTitle.value = '';
    uploadArtist.value = '';
    uploadAlbum.value = '';
    if (fileInput.value) fileInput.value.value = '';
    message.success('上传成功');
  } catch (error) {
    console.error('上传云音乐失败:', error);
    message.error(getErrorMessage(error, '上传失败'));
  }
}

function toSongs(musicList: MusicServerMusic[]) {
  return musicList.map(toMusicServerSongResult);
}

async function handlePlayAll(musicList: MusicServerMusic[]) {
  if (!musicList.length) return;
  const songs = toSongs(musicList);
  playerStore.setPlayList(songs);
  await playerStore.setPlay(songs[0]);
}

async function handlePlaySongResult(song: SongResult, context: SongResult[]) {
  playerStore.setPlayList(context);
  await playerStore.setPlay(song);
}

async function handleDeleteMusic(musicId: number) {
  try {
    await cloudStore.deleteMusic(musicId);
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
    message.warning('云音乐库登录已失效');
  }
});
</script>
