import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import {
  addMusicServerFavorite,
  addMusicServerPlaylistExternalTrack,
  addMusicServerPlaylistTrack,
  buildMusicServerStreamUrl,
  createMusicServerPlaylist,
  deleteMusicServerMusic,
  deleteMusicServerPlaylist,
  getMusicServerBaseUrl,
  getMusicServerMe,
  getMusicServerToken,
  listMusicServerFavorites,
  listMusicServerMusic,
  listMusicServerPlaylists,
  loginMusicServer,
  logoutMusicServer,
  registerMusicServer,
  removeMusicServerFavorite,
  removeMusicServerPlaylistTrack,
  setMusicServerToken,
  updateMusicServerMe,
  uploadMusicServerAvatar,
  uploadMusicServerMusic
} from '@/api/musicServer';
import type {
  MusicServerAuthResponse,
  MusicServerFavorite,
  MusicServerMusic,
  MusicServerOfflineCacheEntry,
  MusicServerOfflineCacheRequest,
  MusicServerPlaylist,
  MusicServerUser
} from '@/types/musicServer';
import { toMusicServerSongResult } from '@/utils/musicServerUtils';

const USER_KEY = 'musicServerUser';
const DEFAULT_CACHE_PROFILE_ID = 'original';

type MusicServerOfflineCachePublicEntry = Omit<
  MusicServerOfflineCacheEntry,
  'localPath' | 'streamUrl'
> & {
  playbackUrl?: string;
};

type MusicServerOfflineCacheQuery = {
  serverBaseUrl?: string;
  userId?: number;
  musicId?: number;
  profileId?: string;
  cacheKey?: string;
  checksum?: string;
};

const readStoredUser = (): MusicServerUser | null => {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as MusicServerUser) : null;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
};

const persistUser = (user: MusicServerUser | null) => {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_KEY);
  }
};

export const useMusicServerStore = defineStore('musicServer', () => {
  const baseUrl = ref(getMusicServerBaseUrl());
  const token = ref(getMusicServerToken());
  const user = ref<MusicServerUser | null>(readStoredUser());
  const musicList = ref<MusicServerMusic[]>([]);
  const playlists = ref<MusicServerPlaylist[]>([]);
  const favorites = ref<MusicServerFavorite[]>([]);
  const cacheStates = ref<Record<string, MusicServerOfflineCachePublicEntry>>({});
  const loading = ref(false);
  const uploading = ref(false);
  const cacheListenersReady = ref(false);

  const isLoggedIn = computed(() => Boolean(token.value && user.value));
  const songs = computed(() =>
    musicList.value.map((music) =>
      toMusicServerSongResult(music, { playMusicUrl: getCachedPlaybackUrl(music) })
    )
  );
  const favoriteMusicIds = computed(() =>
    favorites.value
      .filter((item) => (item.music.source || 'musicServer') === 'musicServer')
      .map((item) => Number(item.music.musicId ?? item.music.id))
      .filter(Number.isFinite)
  );
  const favoriteSongs = computed(() =>
    favorites.value.map((item) =>
      toMusicServerSongResult(item.music, { playMusicUrl: getCachedPlaybackUrl(item.music) })
    )
  );

  const hasCacheApi = () => {
    return typeof window !== 'undefined' && Boolean(window.api?.musicServerCacheAdd);
  };

  const getMusicId = (music: MusicServerMusic) => {
    const musicId = Number(music.musicId ?? music.id);
    return Number.isFinite(musicId) ? musicId : null;
  };

  const isPrivateMusic = (music: MusicServerMusic) => {
    return (music.source || 'musicServer') === 'musicServer' && getMusicId(music) != null;
  };

  const buildCacheQuery = (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ): MusicServerOfflineCacheQuery | null => {
    const musicId = getMusicId(music);
    if (!user.value || musicId == null || !isPrivateMusic(music)) return null;
    return {
      serverBaseUrl: baseUrl.value,
      userId: user.value.id,
      musicId,
      profileId,
      checksum: music.checksum
    };
  };

  const findCacheState = (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ): MusicServerOfflineCachePublicEntry | null => {
    const query = buildCacheQuery(music, profileId);
    if (!query) return null;
    return (
      Object.values(cacheStates.value).find((entry) => {
        return (
          entry.serverBaseUrl === query.serverBaseUrl &&
          entry.userId === query.userId &&
          entry.musicId === query.musicId &&
          entry.profileId === query.profileId
        );
      }) ?? null
    );
  };

  const isCurrentCacheEntry = (entry: MusicServerOfflineCachePublicEntry | null | undefined) => {
    return Boolean(
      entry &&
        user.value &&
        entry.serverBaseUrl === baseUrl.value &&
        entry.userId === user.value.id
    );
  };

  const getCachedPlaybackUrl = (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ) => {
    const state = findCacheState(music, profileId);
    return state?.state === 'ready' ? state.playbackUrl : undefined;
  };

  const setCacheState = (entry: MusicServerOfflineCachePublicEntry | null | undefined) => {
    if (!entry?.cacheKey) return;
    if (!isCurrentCacheEntry(entry)) return;
    cacheStates.value = {
      ...cacheStates.value,
      [entry.cacheKey]: entry
    };
  };

  const removeCacheState = (cacheKey: string) => {
    const nextStates = { ...cacheStates.value };
    delete nextStates[cacheKey];
    cacheStates.value = nextStates;
  };

  const setupOfflineCacheListeners = () => {
    if (!hasCacheApi() || cacheListenersReady.value) return;
    window.api.onMusicServerCacheStateChange((entry) => {
      setCacheState(entry);
    });
    window.api.onMusicServerCacheRemoved((cacheKey) => {
      removeCacheState(cacheKey);
    });
    cacheListenersReady.value = true;
  };

  const loadCacheStates = async () => {
    if (!hasCacheApi() || !user.value) return;
    setupOfflineCacheListeners();
    try {
      const entries = await window.api.musicServerCacheGetAll({
        serverBaseUrl: baseUrl.value,
        userId: user.value.id
      });
      cacheStates.value = Object.fromEntries(
        entries.filter(isCurrentCacheEntry).map((entry) => [entry.cacheKey, entry])
      );
    } catch (error) {
      console.warn('加载 MusicServer 离线缓存状态失败:', error);
    }
  };

  const syncCacheIndex = async (items: MusicServerMusic[] = musicList.value) => {
    if (!hasCacheApi() || !user.value) {
      cacheStates.value = {};
      return;
    }
    setupOfflineCacheListeners();
    try {
      const result = await window.api.musicServerCacheSyncIndex({
        serverBaseUrl: baseUrl.value,
        userId: user.value.id,
        musicList: items.map((music) => ({
          id: music.id,
          musicId: music.musicId,
          checksum: music.checksum
        }))
      });
      cacheStates.value = Object.fromEntries(
        result.entries.filter(isCurrentCacheEntry).map((entry) => [entry.cacheKey, entry])
      );
    } catch (error) {
      console.warn('同步 MusicServer 离线缓存索引失败:', error);
    }
  };

  const createCacheRequest = (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ): MusicServerOfflineCacheRequest | null => {
    const musicId = getMusicId(music);
    if (!user.value || musicId == null || !isPrivateMusic(music)) return null;
    const variant = music.playback?.variants.find((item) => item.profileId === profileId);
    const streamUrl =
      variant?.streamUrl ||
      buildMusicServerStreamUrl(musicId, profileId === DEFAULT_CACHE_PROFILE_ID ? undefined : profileId);
    return {
      serverBaseUrl: baseUrl.value,
      userId: user.value.id,
      musicId,
      profileId,
      title: music.title,
      checksum: variant?.checksum || music.checksum,
      fileSize: variant?.fileSize || music.fileSize,
      contentType: variant?.contentType || music.contentType,
      streamUrl,
      pinned: true
    };
  };

  const persistAuth = (auth: MusicServerAuthResponse) => {
    const shouldResetCache = user.value?.id !== auth.user.id;
    token.value = auth.accessToken;
    user.value = auth.user;
    setMusicServerToken(auth.accessToken);
    persistUser(auth.user);
    if (shouldResetCache) {
      cacheStates.value = {};
    }
  };

  const clearAuth = () => {
    token.value = '';
    user.value = null;
    musicList.value = [];
    playlists.value = [];
    favorites.value = [];
    cacheStates.value = {};
    setMusicServerToken('');
    persistUser(null);
  };

  const login = async (payload: { username: string; password: string }) => {
    const { data } = await loginMusicServer(payload);
    persistAuth(data);
    await loadAll();
  };

  const register = async (payload: { username: string; password: string; displayName?: string }) => {
    const { data } = await registerMusicServer(payload);
    persistAuth(data);
    await loadAll();
  };

  const logout = async () => {
    try {
      if (token.value) {
        await logoutMusicServer();
      }
    } finally {
      clearAuth();
    }
  };

  const restoreSession = async () => {
    if (!token.value) {
      clearAuth();
      return;
    }

    try {
      const { data } = await getMusicServerMe();
      if (user.value?.id !== data.id) {
        cacheStates.value = {};
      }
      user.value = data;
      persistUser(data);
      await loadAll();
    } catch (error) {
      clearAuth();
      throw error;
    }
  };

  const updateProfile = async (payload: { displayName: string; avatarUrl?: string | null }) => {
    const { data } = await updateMusicServerMe(payload);
    user.value = data;
    persistUser(data);
    return data;
  };

  const uploadAvatar = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await uploadMusicServerAvatar(formData);
    user.value = data;
    persistUser(data);
    return data;
  };

  const loadMusic = async () => {
    const { data } = await listMusicServerMusic();
    musicList.value = data;
    await syncCacheIndex(data);
  };

  const loadPlaylists = async () => {
    const { data } = await listMusicServerPlaylists();
    playlists.value = data;
  };

  const loadFavorites = async () => {
    const { data } = await listMusicServerFavorites();
    favorites.value = data;
  };

  const loadAll = async () => {
    if (!token.value) return;
    loading.value = true;
    try {
      setupOfflineCacheListeners();
      await Promise.all([loadMusic(), loadPlaylists(), loadFavorites()]);
      await loadCacheStates();
    } finally {
      loading.value = false;
    }
  };

  const uploadRequest = async (
    file: File,
    metadata: { title?: string; artist?: string; album?: string } = {}
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (metadata.title?.trim()) formData.append('title', metadata.title.trim());
    if (metadata.artist?.trim()) formData.append('artist', metadata.artist.trim());
    if (metadata.album?.trim()) formData.append('album', metadata.album.trim());
    await uploadMusicServerMusic(formData);
  };

  const upload = async (
    file: File,
    metadata: { title?: string; artist?: string; album?: string } = {}
  ) => {
    uploading.value = true;
    try {
      await uploadRequest(file, metadata);
      await loadMusic();
    } finally {
      uploading.value = false;
    }
  };

  const uploadMany = async (
    files: File[],
    getMetadata: (file: File, index: number) => { title?: string; artist?: string; album?: string } = () => ({})
  ) => {
    if (!files.length) return;

    uploading.value = true;
    try {
      for (const [index, file] of files.entries()) {
        await uploadRequest(file, getMetadata(file, index));
      }
      await loadMusic();
    } finally {
      uploading.value = false;
    }
  };

  const deleteMusic = async (musicId: number) => {
    await deleteMusicServerMusic(musicId);
    await loadAll();
  };

  const createPlaylist = async (payload: { name: string; description?: string }) => {
    await createMusicServerPlaylist(payload);
    await loadPlaylists();
  };

  const deletePlaylist = async (playlistId: number) => {
    await deleteMusicServerPlaylist(playlistId);
    await loadPlaylists();
  };

  const addTrackToPlaylist = async (playlistId: number, musicId: number) => {
    await addMusicServerPlaylistTrack(playlistId, musicId);
    await loadPlaylists();
  };

  const addExternalTrackToPlaylist = async (
    playlistId: number,
    payload: {
      source: string;
      externalId: string;
      title: string;
      artist?: string | null;
      album?: string | null;
      picUrl?: string | null;
      duration?: number | null;
    }
  ) => {
    await addMusicServerPlaylistExternalTrack(playlistId, payload);
    await loadPlaylists();
  };

  const removeTrackFromPlaylist = async (playlistId: number, trackId: number) => {
    await removeMusicServerPlaylistTrack(playlistId, trackId);
    await loadPlaylists();
  };

  const isFavorite = (musicId: number) => favoriteMusicIds.value.includes(musicId);

  const toggleFavorite = async (musicId: number) => {
    const { data } = isFavorite(musicId)
      ? await removeMusicServerFavorite(musicId)
      : await addMusicServerFavorite(musicId);
    favorites.value = data;
  };

  const cacheMusic = async (music: MusicServerMusic, profileId = DEFAULT_CACHE_PROFILE_ID) => {
    if (!hasCacheApi()) return [];
    const request = createCacheRequest(music, profileId);
    if (!request) return [];
    setupOfflineCacheListeners();
    const cacheKeys = await window.api.musicServerCacheAdd([request]);
    await loadCacheStates();
    return cacheKeys;
  };

  const removeCachedMusic = async (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ) => {
    if (!hasCacheApi()) return false;
    const state = findCacheState(music, profileId);
    if (!state) return false;
    const removed = await window.api.musicServerCacheRemove(state.cacheKey);
    if (removed) {
      removeCacheState(state.cacheKey);
    }
    return removed;
  };

  const retryCachedMusic = async (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ) => {
    if (!hasCacheApi()) return null;
    const state = findCacheState(music, profileId);
    if (!state) {
      await cacheMusic(music, profileId);
      return findCacheState(music, profileId);
    }
    const nextState = await window.api.musicServerCacheResume(state.cacheKey);
    setCacheState(nextState);
    return nextState;
  };

  const resolveMusicServerPlaybackUrl = async (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ) => {
    const musicId = getMusicId(music);
    if (musicId == null || !isPrivateMusic(music)) return null;

    if (hasCacheApi()) {
      const query = buildCacheQuery(music, profileId);
      if (query) {
        try {
          const cachedUrl = await window.api.musicServerCacheResolvePlaybackUrl(query);
          if (cachedUrl) return cachedUrl;
          const state = await window.api.musicServerCacheGetState(query);
          setCacheState(state);
        } catch (error) {
          console.warn('解析 MusicServer 离线缓存播放地址失败:', error);
        }
      }
    }

    return buildMusicServerStreamUrl(
      musicId,
      profileId === DEFAULT_CACHE_PROFILE_ID ? undefined : profileId
    );
  };

  const toCachedSongResult = async (
    music: MusicServerMusic,
    profileId = DEFAULT_CACHE_PROFILE_ID
  ) => {
    const playMusicUrl = await resolveMusicServerPlaybackUrl(music, profileId);
    return toMusicServerSongResult(music, { playMusicUrl: playMusicUrl || undefined, profileId });
  };

  return {
    baseUrl,
    token,
    user,
    musicList,
    playlists,
    favorites,
    cacheStates,
    loading,
    uploading,
    isLoggedIn,
    songs,
    favoriteMusicIds,
    favoriteSongs,
    login,
    register,
    logout,
    restoreSession,
    updateProfile,
    uploadAvatar,
    loadMusic,
    loadPlaylists,
    loadFavorites,
    loadCacheStates,
    syncCacheIndex,
    loadAll,
    upload,
    uploadMany,
    deleteMusic,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    addExternalTrackToPlaylist,
    removeTrackFromPlaylist,
    cacheMusic,
    removeCachedMusic,
    retryCachedMusic,
    resolveMusicServerPlaybackUrl,
    toCachedSongResult,
    isFavorite,
    toggleFavorite
  };
});
