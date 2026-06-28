import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import {
  getMusicServerMe,
  getMusicServerToken,
  listMusicServerPlaylists,
  logoutMusicServer,
  updateMusicServerMe
} from '@/api/musicServer';
import type { IUserDetail } from '@/types/user';
import type { MusicServerPlaylist, MusicServerUser } from '@/types/musicServer';
import { clearLoginStatus } from '@/utils/auth';
import { toMusicServerSongResult } from '@/utils/musicServerUtils';

interface UserData {
  userId: number;
  nickname: string;
  avatarUrl: string;
  backgroundUrl: string;
  signature: string;
  vipType: number;
  musicServerUser?: MusicServerUser;
  [key: string]: any;
}

function getLocalStorageItem<T>(key: string, defaultValue: T): T {
  try {
    const item = localStorage.getItem(key);
    return item ? JSON.parse(item) : defaultValue;
  } catch {
    return defaultValue;
  }
}

const toUserData = (musicServerUser: MusicServerUser): UserData => ({
  userId: musicServerUser.id,
  nickname: musicServerUser.displayName || musicServerUser.username,
  avatarUrl: musicServerUser.avatarUrl || '',
  backgroundUrl: '',
  signature: 'MusicServer 私有音乐库',
  vipType: 0,
  musicServerUser
});

const toUserDetail = (userData: UserData): IUserDetail =>
  ({
    level: 0,
    listenSongs: 0,
    profile: {
      userId: userData.userId,
      nickname: userData.nickname,
      avatarUrl: userData.avatarUrl,
      backgroundUrl: userData.backgroundUrl,
      signature: userData.signature,
      followeds: 0,
      follows: 0,
      vipType: 0
    }
  }) as IUserDetail;

const toPlaylistItem = (playlist: MusicServerPlaylist, owner: UserData) => ({
  id: playlist.id,
  name: playlist.name,
  description: playlist.description || '',
  coverImgUrl: '',
  picUrl: '',
  trackCount: playlist.tracks.length,
  playCount: 0,
  source: 'musicServer',
  userId: owner.userId,
  creator: {
    userId: owner.userId,
    nickname: owner.nickname,
    avatarUrl: owner.avatarUrl
  },
  tracks: playlist.tracks.map(toMusicServerSongResult),
  raw: playlist
});

export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<UserData | null>(getLocalStorageItem('user', null));
  const userDetail = ref<IUserDetail | null>(null);
  const recordList = ref<any[]>([]);
  const loginType = ref<'token' | 'cookie' | 'qr' | 'uid' | 'musicServer' | null>(
    getLocalStorageItem('loginType', null)
  );
  const searchValue = ref('');
  const searchType = ref(1);
  // 收藏的专辑 ID 列表
  const collectedAlbumIds = ref<Set<number>>(new Set());
  // 用户的歌单列表
  const playList = ref<any[]>([]);
  // 用户的专辑列表
  const albumList = ref<any[]>([]);

  // 方法
  const setUser = (userData: UserData) => {
    user.value = userData;
    localStorage.setItem('user', JSON.stringify(userData));
  };

  const setLoginType = (type: typeof loginType.value) => {
    loginType.value = type;
    if (type) {
      localStorage.setItem('loginType', type);
    } else {
      localStorage.removeItem('loginType');
    }
  };

  const handleLogout = async () => {
    try {
      if (getMusicServerToken()) {
        await logoutMusicServer();
      }
    } catch (error) {
      console.error('MusicServer 登出失败，继续清理本地状态:', error);
    }
    user.value = null;
    userDetail.value = null;
    recordList.value = [];
    loginType.value = null;
    collectedAlbumIds.value.clear();
    playList.value = [];
    albumList.value = [];
    clearLoginStatus();
    window.location.reload();
  };

  const setSearchValue = (value: string) => {
    searchValue.value = value;
  };

  const setSearchType = (type: number) => {
    searchType.value = type;
  };

  // 初始化歌单列表
  const initializePlaylist = async () => {
    if (!user.value) {
      playList.value = [];
      return;
    }

    try {
      const { data } = await listMusicServerPlaylists();
      playList.value = data.map((playlist) => toPlaylistItem(playlist, user.value!));
      console.log(`已加载 ${playList.value.length} 个歌单`);
    } catch (error) {
      console.error('获取歌单列表失败:', error);
      playList.value = [];
    }
  };

  const updateMusicServerProfile = async (payload: { displayName: string; avatarUrl?: string }) => {
    const { data } = await updateMusicServerMe({
      displayName: payload.displayName,
      avatarUrl: payload.avatarUrl || null
    });
    const nextUser = toUserData(data);
    user.value = nextUser;
    userDetail.value = toUserDetail(nextUser);
    localStorage.setItem('user', JSON.stringify(nextUser));
    localStorage.setItem('musicServerUser', JSON.stringify(data));
    const { useMusicServerStore } = await import('./musicServer');
    useMusicServerStore().user = data;
    return nextUser;
  };

  // 初始化专辑列表
  const initializeAlbumList = async () => {
    albumList.value = [];
  };

  // 初始化收藏的专辑ID列表
  const initializeCollectedAlbums = async () => {
    collectedAlbumIds.value.clear();
  };

  // 添加收藏专辑
  const addCollectedAlbum = (albumId: number) => {
    collectedAlbumIds.value.add(albumId);
  };

  // 移除收藏专辑
  const removeCollectedAlbum = (albumId: number) => {
    collectedAlbumIds.value.delete(albumId);
  };

  // 检查专辑是否已收藏
  const isAlbumCollected = (albumId: number) => {
    return collectedAlbumIds.value.has(albumId);
  };

  // 判断用户是否为VIP
  const isVip = computed(() => {
    if (!user.value) return false;
    // vipType: 0 非VIP, 11 VIP
    return user.value.vipType && user.value.vipType !== 0;
  });

  // 初始化
  const initializeUser = async () => {
    if (!getMusicServerToken()) {
      return [];
    }

    try {
      const { data } = await getMusicServerMe();
      const musicServerUser = toUserData(data);
      user.value = musicServerUser;
      userDetail.value = toUserDetail(musicServerUser);
      loginType.value = 'musicServer';
      localStorage.setItem('user', JSON.stringify(musicServerUser));
      localStorage.setItem('loginType', 'musicServer');
      await initializePlaylist();
    } catch (error) {
      console.error('恢复 MusicServer 登录失败:', error);
      handleLogout();
    }
    return [];
  };

  return {
    // 状态
    user,
    loginType,
    searchValue,
    searchType,
    collectedAlbumIds,
    playList,
    albumList,
    isVip,

    // 方法
    setUser,
    setLoginType,
    handleLogout,
    setSearchValue,
    setSearchType,
    initializeUser,
    updateMusicServerProfile,
    initializePlaylist,
    initializeAlbumList,
    initializeCollectedAlbums,
    addCollectedAlbum,
    removeCollectedAlbum,
    isAlbumCollected,
    userDetail,
    recordList
  };
});
