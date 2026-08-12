import { defineStore } from 'pinia';
import { ref } from 'vue';

import type { SongResult } from '@/types/music';
import type { MusicServerFavorite } from '@/types/musicServer';
import { getLocalStorageItem, isBilibiliIdMatch, setLocalStorageItem } from '@/utils/playerUtils';

const toFavoriteKey = (songOrId: number | string | SongResult) => {
  if (typeof songOrId === 'object') {
    return songOrId.source === 'musicServer' ? `musicServer:${songOrId.id}` : songOrId.id;
  }
  return songOrId;
};

const toMusicServerId = (songOrId: number | string | SongResult) => {
  if (typeof songOrId === 'object' && songOrId.source === 'musicServer') {
    return Number(songOrId.id);
  }
  if (typeof songOrId === 'string' && songOrId.startsWith('musicServer:')) {
    return Number(songOrId.replace('musicServer:', ''));
  }
  return null;
};

const isMusicServerFavoriteKey = (id: number | string) =>
  typeof id === 'string' && id.startsWith('musicServer:');

const toExternalFavoriteKey = (externalId: number | string) => {
  return String(externalId);
};

const toMusicServerFavoriteKey = (favorite: MusicServerFavorite) => {
  const music = favorite.music;
  if ((music.source || 'musicServer') === 'musicServer') {
    return `musicServer:${music.musicId ?? music.id}`;
  }
  return toExternalFavoriteKey(music.externalId || music.id);
};

const toArtistText = (song: SongResult) => {
  return (song.ar || song.artists || song.song?.artists || [])
    .map((artist: any) => artist.name)
    .filter(Boolean)
    .join(' / ');
};

const toAlbumText = (song: SongResult) => {
  return song.al?.name || song.album?.name || song.song?.album?.name || '';
};

const isLocalSong = (song: SongResult) => song.playMusicUrl?.startsWith('local://');

const toExternalFavoritePayload = (songOrId: number | string | SongResult) => {
  if (typeof songOrId !== 'object') {
    if (typeof songOrId === 'number' || /^\d+$/.test(String(songOrId))) {
      return {
        source: 'netease',
        externalId: String(songOrId),
        title: '未知歌曲'
      };
    }
    return null;
  }

  if (songOrId.source === 'musicServer' || isLocalSong(songOrId)) return null;

  return {
    source: songOrId.source || 'netease',
    externalId: String(songOrId.id),
    title: songOrId.name || '未知歌曲',
    artist: toArtistText(songOrId) || null,
    album: toAlbumText(songOrId) || null,
    picUrl: songOrId.picUrl || songOrId.al?.picUrl || songOrId.album?.picUrl || null,
    duration: songOrId.duration || songOrId.dt || null
  };
};

const toExternalFavoriteIdentity = (songOrId: number | string | SongResult) => {
  const payload = toExternalFavoritePayload(songOrId);
  return payload ? { source: payload.source, externalId: payload.externalId } : null;
};

const isSameFavoriteKey = (existingId: number | string, key: number | string) => {
  if (typeof key === 'string' && key.includes('--')) {
    return isBilibiliIdMatch(existingId, key);
  }
  return existingId === key || String(existingId) === String(key);
};

/**
 * 收藏管理 Store
 * 负责：收藏列表、不喜欢列表的管理
 */
export const useFavoriteStore = defineStore('favorite', () => {
  // ==================== 状态 ====================
  const favoriteList = ref<Array<number | string>>(getLocalStorageItem('favoriteList', []));
  const dislikeList = ref<Array<number | string>>(getLocalStorageItem('dislikeList', []));

  // ==================== Actions ====================

  /**
   * 添加到收藏列表
   */
  const addToFavorite = async (songOrId: number | string | SongResult) => {
    const id = toFavoriteKey(songOrId);
    // 检查是否已存在
    const isAlreadyInList = favoriteList.value.some((existingId) => isSameFavoriteKey(existingId, id));

    if (!isAlreadyInList) {
      favoriteList.value.push(id);
      setLocalStorageItem('favoriteList', favoriteList.value);
    }

    const musicServerId = toMusicServerId(songOrId);
    const externalFavoritePayload = toExternalFavoritePayload(songOrId);
    if (localStorage.getItem('musicServerToken') && (musicServerId || externalFavoritePayload)) {
      try {
        const { addMusicServerFavorite, addMusicServerExternalFavorite } = await import(
          '@/api/musicServer'
        );
        const { useMusicServerStore } = await import('./musicServer');
        const musicServerStore = useMusicServerStore();
        const { data } = musicServerId
          ? await addMusicServerFavorite(musicServerId)
          : await addMusicServerExternalFavorite(externalFavoritePayload!);
        musicServerStore.favorites = data;
        const serverList = data.map(toMusicServerFavoriteKey);
        const serverKeySet = new Set(serverList.map(String));
        const nonServerLocal = favoriteList.value.filter(
          (item) => !isMusicServerFavoriteKey(item) && !serverKeySet.has(String(item))
        );
        favoriteList.value = Array.from(new Set([...nonServerLocal, ...serverList]));
        setLocalStorageItem('favoriteList', favoriteList.value);
      } catch (error) {
        console.error('同步 MusicServer 收藏失败:', error);
      }
    }
  };

  /**
   * 从收藏列表移除
   */
  const removeFromFavorite = async (songOrId: number | string | SongResult) => {
    const id = toFavoriteKey(songOrId);
    // 对于B站视频，需要根据bvid和cid来匹配
    if (typeof id === 'string' && id.includes('--')) {
      favoriteList.value = favoriteList.value.filter(
        (existingId) => !isBilibiliIdMatch(existingId, id)
      );
    } else {
      favoriteList.value = favoriteList.value.filter((existingId) => !isSameFavoriteKey(existingId, id));

      const musicServerId = toMusicServerId(songOrId);
      const externalFavoriteIdentity = toExternalFavoriteIdentity(songOrId);
      if (localStorage.getItem('musicServerToken') && (musicServerId || externalFavoriteIdentity)) {
        try {
          const { removeMusicServerFavorite, removeMusicServerExternalFavorite } = await import(
            '@/api/musicServer'
          );
          const { useMusicServerStore } = await import('./musicServer');
          const musicServerStore = useMusicServerStore();
          const { data } = musicServerId
            ? await removeMusicServerFavorite(musicServerId)
            : await removeMusicServerExternalFavorite(
                externalFavoriteIdentity!.source,
                externalFavoriteIdentity!.externalId
              );
          musicServerStore.favorites = data;
          const serverList = data.map(toMusicServerFavoriteKey);
          const serverKeySet = new Set(serverList.map(String));
          const nonServerLocal = favoriteList.value.filter(
            (item) => !isMusicServerFavoriteKey(item) && !serverKeySet.has(String(item))
          );
          favoriteList.value = Array.from(new Set([...nonServerLocal, ...serverList]));
        } catch (error) {
          console.error('取消同步 MusicServer 收藏失败:', error);
        }
      }
    }
    setLocalStorageItem('favoriteList', favoriteList.value);
  };

  /**
   * 添加到不喜欢列表
   */
  const addToDislikeList = (id: number | string) => {
    if (!dislikeList.value.includes(id)) {
      dislikeList.value.push(id);
      setLocalStorageItem('dislikeList', dislikeList.value);
    }
  };

  /**
   * 从不喜欢列表移除
   */
  const removeFromDislikeList = (id: number | string) => {
    dislikeList.value = dislikeList.value.filter((existingId) => existingId !== id);
    setLocalStorageItem('dislikeList', dislikeList.value);
  };

  /**
   * 初始化收藏列表（从服务器同步）
   */
  const initializeFavoriteList = async () => {
    const localFavoriteList = localStorage.getItem('favoriteList');
    const localList: Array<number | string> = localFavoriteList ? JSON.parse(localFavoriteList) : [];

    if (localStorage.getItem('musicServerToken')) {
      try {
        const { useMusicServerStore } = await import('./musicServer');
        const musicServerStore = useMusicServerStore();
        await musicServerStore.loadFavorites();
        const serverList = musicServerStore.favorites.map(toMusicServerFavoriteKey);
        const serverKeySet = new Set(serverList.map(String));
        const nonServerLocal = localList.filter(
          (id) => !isMusicServerFavoriteKey(id) && !serverKeySet.has(String(id))
        );
        favoriteList.value = Array.from(new Set([...nonServerLocal, ...serverList]));
      } catch (error) {
        console.error('获取 MusicServer 收藏列表失败，使用本地数据:', error);
        favoriteList.value = localList;
      }
    } else {
      favoriteList.value = localList;
    }

    setLocalStorageItem('favoriteList', favoriteList.value);
  };

  /**
   * 检查歌曲是否已收藏
   */
  const isFavorite = (id: number | string | SongResult): boolean => {
    const key = toFavoriteKey(id);
    return favoriteList.value.some((existingId) => isSameFavoriteKey(existingId, key));
  };

  /**
   * 检查歌曲是否在不喜欢列表中
   */
  const isDisliked = (id: number | string): boolean => {
    return dislikeList.value.includes(id);
  };

  return {
    // 状态
    favoriteList,
    dislikeList,

    // Actions
    addToFavorite,
    removeFromFavorite,
    addToDislikeList,
    removeFromDislikeList,
    initializeFavoriteList,
    isFavorite,
    isDisliked
  };
});
