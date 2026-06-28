import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import {
  addMusicServerFavorite,
  addMusicServerPlaylistExternalTrack,
  addMusicServerPlaylistTrack,
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
  setMusicServerBaseUrl,
  setMusicServerToken,
  updateMusicServerMe,
  uploadMusicServerAvatar,
  uploadMusicServerMusic
} from '@/api/musicServer';
import type {
  MusicServerAuthResponse,
  MusicServerFavorite,
  MusicServerMusic,
  MusicServerPlaylist,
  MusicServerUser
} from '@/types/musicServer';
import { toMusicServerSongResult } from '@/utils/musicServerUtils';

const USER_KEY = 'musicServerUser';

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
  const loading = ref(false);
  const uploading = ref(false);

  const isLoggedIn = computed(() => Boolean(token.value && user.value));
  const songs = computed(() => musicList.value.map(toMusicServerSongResult));
  const favoriteMusicIds = computed(() => favorites.value.map((item) => item.music.id));
  const favoriteSongs = computed(() => favorites.value.map((item) => toMusicServerSongResult(item.music)));

  const persistAuth = (auth: MusicServerAuthResponse) => {
    token.value = auth.accessToken;
    user.value = auth.user;
    setMusicServerToken(auth.accessToken);
    persistUser(auth.user);
  };

  const clearAuth = () => {
    token.value = '';
    user.value = null;
    musicList.value = [];
    playlists.value = [];
    favorites.value = [];
    setMusicServerToken('');
    persistUser(null);
  };

  const setBaseUrl = (value: string) => {
    setMusicServerBaseUrl(value);
    baseUrl.value = getMusicServerBaseUrl();
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
    if (!token.value) return;

    try {
      const { data } = await getMusicServerMe();
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
      await Promise.all([loadMusic(), loadPlaylists(), loadFavorites()]);
    } finally {
      loading.value = false;
    }
  };

  const upload = async (
    file: File,
    metadata: { title?: string; artist?: string; album?: string } = {}
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (metadata.title?.trim()) formData.append('title', metadata.title.trim());
    if (metadata.artist?.trim()) formData.append('artist', metadata.artist.trim());
    if (metadata.album?.trim()) formData.append('album', metadata.album.trim());

    uploading.value = true;
    try {
      await uploadMusicServerMusic(formData);
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

  return {
    baseUrl,
    token,
    user,
    musicList,
    playlists,
    favorites,
    loading,
    uploading,
    isLoggedIn,
    songs,
    favoriteMusicIds,
    favoriteSongs,
    setBaseUrl,
    login,
    register,
    logout,
    restoreSession,
    updateProfile,
    uploadAvatar,
    loadMusic,
    loadPlaylists,
    loadFavorites,
    loadAll,
    upload,
    deleteMusic,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    addExternalTrackToPlaylist,
    removeTrackFromPlaylist,
    isFavorite,
    toggleFavorite
  };
});
