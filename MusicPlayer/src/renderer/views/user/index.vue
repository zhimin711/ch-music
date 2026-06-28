<template>
  <div class="user-page">
    <template v-if="infoLoading">
      <div
        class="left-skeleton flex-1 max-w-[600px] rounded-2xl overflow-hidden p-4 bg-light-200 dark:bg-dark-100"
      >
        <div class="flex flex-col gap-6">
          <div class="flex justify-between">
            <div class="h-8 w-32 skeleton-shimmer rounded-lg" />
            <div class="h-6 w-20 skeleton-shimmer rounded-lg" />
          </div>
          <div class="flex items-center gap-4">
            <div class="h-[50px] w-[50px] skeleton-shimmer rounded-full" />
            <div class="flex w-2/5 justify-around">
              <div v-for="i in 3" :key="i" class="flex flex-col items-center gap-1">
                <div class="h-5 w-8 skeleton-shimmer rounded-lg" />
                <div class="h-4 w-12 skeleton-shimmer rounded-lg" />
              </div>
            </div>
          </div>
          <div class="h-4 w-3/4 skeleton-shimmer rounded-lg" />
          <div class="mt-4 rounded-xl bg-light p-4 dark:bg-black">
            <div class="mb-4 h-8 w-full skeleton-shimmer rounded-xl" />
            <div class="space-y-4">
              <div v-for="i in 5" :key="i" class="flex gap-3">
                <div class="h-[50px] w-[50px] skeleton-shimmer rounded-xl flex-shrink-0" />
                <div class="flex flex-1 flex-col justify-center gap-2">
                  <div class="h-4 w-1/2 skeleton-shimmer rounded-lg" />
                  <div class="h-3 w-1/3 skeleton-shimmer rounded-lg" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!isMobile" class="right">
        <div class="title"><div class="h-8 w-32 skeleton-shimmer rounded-lg" /></div>
        <div class="rounded-2xl bg-light p-4 dark:bg-black">
          <div class="space-y-2">
            <div
              v-for="i in 10"
              :key="i"
              class="flex items-center gap-4 rounded-2xl bg-light-100 p-2 dark:bg-dark-100"
            >
              <div class="h-10 w-10 skeleton-shimmer rounded-full flex-shrink-0" />
              <div class="h-10 w-10 skeleton-shimmer rounded-xl flex-shrink-0" />
              <div class="flex flex-1 flex-col gap-2">
                <div class="h-4 w-1/3 skeleton-shimmer rounded-lg" />
                <div class="h-3 w-1/4 skeleton-shimmer rounded-lg" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <template v-else>
      <div
        v-if="userDetail && user"
        class="left"
        :class="[setAnimationClass('animate__fadeIn'), { 'has-user-background': hasUserBackground }]"
        :style="userBackgroundStyle"
      >
        <div class="page">
          <div class="user-name">
            <span>{{ user.nickname }}</span>
            <span v-if="currentLoginType" class="login-type">{{
              t('login.title.' + currentLoginType)
            }}</span>
          </div>
          <div class="user-info">
            <div class="avatar-editor">
              <n-avatar round :size="50" :src="getImgUrl(user.avatarUrl, '50y50')" />
              <button class="avatar-edit-btn" @click="openProfileEditor">
                <i class="ri-pencil-line"></i>
              </button>
            </div>
            <div class="user-info-list">
              <div class="user-info-item">
                <div class="label">{{ userDetail.profile.followeds }}</div>
                <div>{{ t('user.profile.followers') }}</div>
              </div>
              <div class="user-info-item" @click="showFollowList">
                <div class="label">{{ userDetail.profile.follows }}</div>
                <div>{{ t('user.profile.following') }}</div>
              </div>
              <div class="user-info-item">
                <div class="label">{{ userDetail.level }}</div>
                <div>{{ t('user.profile.level') }}</div>
              </div>
            </div>
          </div>
          <div class="uesr-signature">{{ userDetail.profile.signature }}</div>
          <div class="play-list" :class="setAnimationClass('animate__fadeIn')">
            <div class="tab-container">
              <n-tabs v-model:value="currentTab" type="segment" animated>
                <n-tab v-for="tab in tabs" :key="tab.key" :name="tab.key" :tab="t(tab.label)">
                </n-tab>
              </n-tabs>
            </div>
            <n-scrollbar>
              <div class="mt-4">
                <div
                  v-if="albumLoading && currentTab === 'album'"
                  class="flex h-32 items-center justify-center"
                >
                  <n-spin size="medium" />
                </div>
                <template v-else>
                  <button
                    class="play-list-item"
                    @click="goToImportPlaylist"
                    v-if="false && isElectron && currentTab === 'created'"
                  >
                    <div class="play-list-item-img"><i class="icon iconfont ri-add-line"></i></div>
                    <div class="play-list-item-info">
                      <div class="play-list-item-name">
                        {{ t('comp.playlist.import.button') }}
                      </div>
                    </div>
                  </button>
                  <div
                    v-for="(item, index) in currentList"
                    :key="index"
                    class="play-list-item"
                    @click="handleItemClick(item)"
                  >
                    <n-image
                      :src="getImgUrl(getCoverUrl(item), '50y50')"
                      class="play-list-item-img"
                      lazy
                      preview-disabled
                    />
                    <div class="play-list-item-info">
                      <div class="play-list-item-name">
                        <n-ellipsis :line-clamp="1">{{ item.name }}</n-ellipsis>
                      </div>
                      <div class="play-list-item-count">
                        {{ getItemDescription(item) }}
                      </div>
                    </div>
                  </div>
                  <div class="pb-20"></div>
                  <play-bottom />
                </template>
              </div>
            </n-scrollbar>
          </div>
        </div>
      </div>
      <div v-if="!isMobile" class="right" :class="setAnimationClass('animate__fadeIn')">
        <div class="title">{{ t('user.ranking.title') }}</div>
        <div class="record-list">
          <n-scrollbar>
            <div
              v-for="(item, index) in recordList"
              :key="item.id"
              class="record-item"
              :class="setAnimationClass('animate__bounceInUp')"
              :style="setAnimationDelay(index, 25)"
            >
              <div class="play-score">
                {{ index + 1 }}
              </div>
              <song-item class="song-item" :item="item" mini @play="handlePlay" />
            </div>
            <play-bottom />
          </n-scrollbar>
        </div>
      </div>
    </template>
    <!-- 未登录时显示登录组件 -->
    <div
      v-if="!isLoggedIn && isMobile"
      class="login-container"
      :class="setAnimationClass('animate__fadeIn')"
    >
      <login-component @login-success="handleLoginSuccess" />
    </div>
    <n-modal
      v-model:show="profileModalVisible"
      preset="card"
      :bordered="false"
      style="width: min(520px, calc(100vw - 32px))"
      class="profile-modal"
    >
      <template #header>
        <div class="profile-modal-title">
          <div>
            <div class="profile-modal-title-main">编辑个人资料</div>
            <div class="profile-modal-title-sub">更新昵称和头像</div>
          </div>
        </div>
      </template>
      <div class="profile-form">
        <div class="profile-preview">
          <div class="profile-avatar-wrap">
            <n-avatar
              round
              :size="88"
              :src="getImgUrl(avatarPreviewUrl || profileForm.avatarUrl, '100y100')"
              :img-props="{ class: 'profile-avatar-img' }"
            />
            <button type="button" class="profile-avatar-button" @click="avatarFileInput?.click()">
              <i class="ri-camera-line" />
            </button>
          </div>
          <div class="profile-preview-text">
            <div>{{ profileForm.displayName || user?.nickname }}</div>
            <div>{{ selectedAvatarFile ? '新头像将在保存后上传' : '头像和昵称将同步到云端' }}</div>
            <div v-if="selectedAvatarFile" class="avatar-file-name">{{ selectedAvatarFile.name }}</div>
          </div>
        </div>
        <input
          ref="avatarFileInput"
          class="hidden"
          type="file"
          accept="image/*"
          @change="handleAvatarFileChange"
        />
        <label class="profile-field">
          <span>显示名称</span>
          <n-input v-model:value="profileForm.displayName" placeholder="填写你的昵称" maxlength="120" />
        </label>
        <n-collapse-transition :show="showAvatarUrlInput">
          <label class="profile-field">
            <span>头像链接</span>
            <n-input
              v-model:value="profileForm.avatarUrl"
              placeholder="可选，上传头像后会自动填写"
              clearable
            />
          </label>
        </n-collapse-transition>
        <button
          type="button"
          class="profile-link-toggle"
          @click="showAvatarUrlInput = !showAvatarUrlInput"
        >
          <i :class="showAvatarUrlInput ? 'ri-arrow-up-s-line' : 'ri-link'" />
          {{ showAvatarUrlInput ? '收起头像链接' : '手动填写头像链接' }}
        </button>
        <div class="profile-actions">
          <n-button @click="profileModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="profileSaving" @click="saveProfile">保存</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { useMessage } from 'naive-ui';
import { storeToRefs } from 'pinia';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { navigateToMusicList } from '@/components/common/MusicListNavigator';
import PlayBottom from '@/components/common/PlayBottom.vue';
import SongItem from '@/components/common/SongItem.vue';
import { useMusicServerStore } from '@/store/modules/musicServer';
import { usePlayerStore } from '@/store/modules/player';
import { useUserStore } from '@/store/modules/user';
import { getImgUrl, isElectron, isMobile, setAnimationClass, setAnimationDelay } from '@/utils';
import { checkLoginStatus as checkAuthStatus } from '@/utils/auth';
import LoginComponent from '@/views/login/index.vue';

defineOptions({
  name: 'User'
});

const { t } = useI18n();
const userStore = useUserStore();
const musicServerStore = useMusicServerStore();
const playerStore = usePlayerStore();
const router = useRouter();
const { userDetail, recordList } = storeToRefs(userStore);
const infoLoading = ref(false);
const albumLoading = ref(false);
const mounted = ref(true);
const message = useMessage();
const profileModalVisible = ref(false);
const profileSaving = ref(false);
const profileForm = ref({
  displayName: '',
  avatarUrl: ''
});
const showAvatarUrlInput = ref(false);
const avatarFileInput = ref<HTMLInputElement | null>(null);
const selectedAvatarFile = ref<File | null>(null);
const avatarPreviewUrl = ref('');

// Tab 相关
const tabs = [
  { key: 'created', label: 'user.tabs.created' },
  { key: 'favorite', label: 'user.tabs.favorite' }
];
const currentTab = ref('created');

const user = computed(() => userStore.user);
const hasUserBackground = computed(() => Boolean(user.value?.backgroundUrl?.trim()));
const userBackgroundStyle = computed(() => {
  if (!hasUserBackground.value) return {};
  return {
    '--user-background-image': `url(${getImgUrl(user.value?.backgroundUrl)})`
  };
});

// 创建的歌单（当前用户创建的）
const createdPlaylists = computed(() => {
  if (!user.value) return [];
  return userStore.playList;
});

const favoriteSongs = computed(() =>
  musicServerStore.favoriteSongs.map((song) => ({
    ...song,
    name: song.name,
    trackCount: 1,
    playCount: 0,
    type: 'musicServerFavoriteSong'
  }))
);

// 当前显示的列表（根据 tab 切换）
const currentList = computed(() => {
  return currentTab.value === 'created' ? createdPlaylists.value : favoriteSongs.value;
});

// 获取封面图片 URL
const getCoverUrl = (item: any) => {
  return item.coverImgUrl || item.picUrl || '';
};

// 获取列表项描述
const getItemDescription = (item: any) => {
  if (item.type === 'musicServerFavoriteSong') {
    return item.ar?.[0]?.name || '云端音乐';
  } else {
    return `${t('user.playlist.trackCount', { count: item.trackCount })}，${t('user.playlist.playCount', { count: item.playCount })}`;
  }
};

// 统一处理列表项点击
const handleItemClick = (item: any) => {
  if (item.type === 'musicServerFavoriteSong') {
    playerStore.setPlayList(musicServerStore.favoriteSongs);
    playerStore.setPlay(item);
  } else {
    openPlaylist(item);
  }
};

const goToImportPlaylist = () => {
  router.push('/playlist/import');
};

onBeforeUnmount(() => {
  mounted.value = false;
  revokeAvatarPreview();
});

// 检查登录状态
const checkLoginStatus = () => {
  // userStore 的状态已经在 App.vue 中全局初始化，这里只需要检查
  if (userStore.user && userStore.loginType) {
    return true;
  }

  // 如果还是没有登录信息，跳转到登录页
  const loginInfo = checkAuthStatus();
  if (!loginInfo.isLoggedIn) {
    !isMobile.value && router.push('/login');
    return false;
  }

  return true;
};

const loadPage = async () => {
  if (!mounted.value) return;

  // 检查登录状态
  if (!checkLoginStatus()) return;

  await loadData();
};

const loadData = async () => {
  try {
    // 只有在没有数据时才显示加载状态
    if (!userDetail.value || !recordList.value?.length) {
      infoLoading.value = true;
    }

    if (!user.value) {
      console.warn('用户数据不存在，尝试重新获取');
      // 可以尝试重新获取用户数据
      return;
    }

    await Promise.all([userStore.initializePlaylist(), playerStore.initializeFavoriteList()]);

    if (!mounted.value) return;

    userDetail.value =
      userDetail.value ||
      ({
        level: 0,
        profile: {
          ...user.value,
          followeds: 0,
          follows: 0
        }
      } as any);
    recordList.value = musicServerStore.favoriteSongs;
  } catch (error: any) {
    console.error('加载用户页面失败:', error);
    if (error.response?.status === 401) {
      userStore.handleLogout();
      router.push('/login');
    } else {
      // 添加更多错误处理和重试逻辑
      message.error(t('user.message.loadFailed'));
    }
  } finally {
    if (mounted.value) {
      infoLoading.value = false;
    }
  }
};

// 监听路由变化
watch(
  () => router.currentRoute.value.path,
  (newPath) => {
    console.log('newPath', newPath);
    if (newPath === '/user') {
      checkLoginStatus();
      loadData();
    }
  }
);

// 监听用户状态变化
watch(
  () => userStore.user,
  (newUser) => {
    if (!mounted.value) return;
    if (newUser) {
      checkLoginStatus();
      loadPage();
    }
  }
);

// 监听 tab 切换
watch(currentTab, async (newTab) => {
  if (newTab === 'favorite') {
    await playerStore.initializeFavoriteList();
  }
});

// 页面挂载时检查登录状态
onMounted(() => {
  checkLoginStatus() && loadData();
});

// 替换显示歌单的方法
const openPlaylist = (item: any) => {
  navigateToMusicList(router, {
    id: item.id,
    type: 'musicServerPlaylist',
    name: item.name,
    songList: item.tracks || [],
    listInfo: item,
    canRemove: true // 保留可移除功能
  });
};

const handlePlay = () => {
  const tracks = recordList.value || [];
  playerStore.setPlayList(tracks);
};

// 显示关注列表
const showFollowList = () => {
  if (!user.value) return;
  router.push('/user/follows');
};

// // 显示粉丝列表
// const showFollowerList = () => {
//   if (!user.value) return;
//   router.push('/user/followers');
// };

const handleLoginSuccess = () => {
  // 处理登录成功后的逻辑
  checkLoginStatus();
  loadData();
};

const openProfileEditor = () => {
  clearSelectedAvatarFile();
  showAvatarUrlInput.value = false;
  profileForm.value = {
    displayName: user.value?.nickname || '',
    avatarUrl: user.value?.avatarUrl || ''
  };
  profileModalVisible.value = true;
};

const revokeAvatarPreview = () => {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value);
    avatarPreviewUrl.value = '';
  }
};

const clearSelectedAvatarFile = () => {
  selectedAvatarFile.value = null;
  revokeAvatarPreview();
  if (avatarFileInput.value) avatarFileInput.value.value = '';
};

const handleAvatarFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0] || null;
  if (!file) {
    clearSelectedAvatarFile();
    return;
  }
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件');
    clearSelectedAvatarFile();
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('头像图片不能超过 5MB');
    clearSelectedAvatarFile();
    return;
  }
  revokeAvatarPreview();
  selectedAvatarFile.value = file;
  avatarPreviewUrl.value = URL.createObjectURL(file);
};

const saveProfile = async () => {
  const displayName = profileForm.value.displayName.trim();
  if (!displayName) {
    message.warning('请填写显示名称');
    return;
  }

  profileSaving.value = true;
  try {
    let avatarUrl = profileForm.value.avatarUrl.trim();
    if (selectedAvatarFile.value) {
      const uploadedUser = await userStore.uploadMusicServerProfileAvatar(selectedAvatarFile.value);
      avatarUrl = uploadedUser.avatarUrl || '';
    }
    await userStore.updateMusicServerProfile({
      displayName,
      avatarUrl
    });
    clearSelectedAvatarFile();
    profileModalVisible.value = false;
    message.success('个人资料已更新');
  } catch (error: any) {
    console.error('更新个人资料失败:', error);
    message.error(error?.response?.data?.message || error?.message || '更新失败');
  } finally {
    profileSaving.value = false;
  }
};

watch(profileModalVisible, (visible) => {
  if (!visible) {
    clearSelectedAvatarFile();
  }
});

const isLoggedIn = computed(() => userStore.user);
const currentLoginType = computed(() => userStore.loginType);
</script>

<style lang="scss" scoped>
.user-page {
  @apply flex h-full;
  .left {
    max-width: 600px;
    isolation: isolate;
    box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
    @apply flex-1 rounded-2xl overflow-hidden relative h-full;
    background:
      linear-gradient(135deg, rgba(34, 197, 94, 0.22) 0%, transparent 38%),
      linear-gradient(225deg, rgba(14, 165, 233, 0.18) 0%, transparent 34%),
      linear-gradient(160deg, #111827 0%, #0f172a 58%, #07130f 100%);
    background-position: center;
    background-size: cover;

    &.has-user-background {
      background-image:
        linear-gradient(160deg, rgba(8, 13, 23, 0.84), rgba(8, 13, 23, 0.46)),
        var(--user-background-image);
    }

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      z-index: 0;
      background:
        linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 100%),
        linear-gradient(0deg, rgba(255, 255, 255, 0.05) 0 1px, transparent 1px 100%);
      background-size: 34px 34px;
      mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.65), transparent 82%);
      opacity: 0.42;
      pointer-events: none;
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      z-index: 0;
      background: linear-gradient(180deg, rgba(0, 0, 0, 0.04), rgba(0, 0, 0, 0.28));
      pointer-events: none;
    }

    .page {
      @apply p-5 w-full z-10 flex flex-col h-full relative;
      background: linear-gradient(180deg, rgba(15, 23, 42, 0.72), rgba(15, 23, 42, 0.42));
      backdrop-filter: blur(18px);
      -webkit-backdrop-filter: blur(18px);
    }
    .title {
      @apply text-lg font-bold flex items-center justify-between;
      @apply text-gray-900 dark:text-white;
    }
    .user-name {
      @apply text-2xl font-bold mb-4 flex items-center justify-between gap-3;
      @apply text-white;
      text-shadow: 0 2px 18px rgba(0, 0, 0, 0.28);
    }

    .uesr-signature {
      @apply mt-4 rounded-xl border border-white/10 bg-white/10 px-4 py-3 text-sm text-white/75;
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
    }

  .user-info {
    @apply flex items-center rounded-2xl border border-white/10 bg-white/10 p-3;
    backdrop-filter: blur(14px);
    -webkit-backdrop-filter: blur(14px);
    &-list {
        @apply flex flex-1 justify-around text-center;
        @apply text-white/70;

        .label {
          @apply text-xl font-bold text-white;
        }
      }

      &-item {
        @apply cursor-pointer;
      }
    }
  }
  .avatar-editor {
    @apply relative mr-4 flex h-[58px] w-[58px] flex-shrink-0;

    :deep(.n-avatar) {
      @apply ring-2 ring-white/40 shadow-lg;
    }
  }
  .avatar-edit-btn {
    @apply absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-xs text-white shadow-lg ring-2 ring-white/70 transition hover:bg-primary/90;
  }

  .right {
    @apply flex-1 ml-4 overflow-hidden h-full;

    .record-list {
      @apply rounded-2xl;
      @apply bg-light dark:bg-black;
      height: calc(100% - 60px);

      .record-item {
        @apply flex items-center px-2 mb-2 rounded-2xl bg-light-100 dark:bg-dark-100;
      }

      .song-item {
        @apply flex-1;
      }

      .play-score {
        @apply text-gray-500 dark:text-gray-400 mr-2 text-lg w-10 h-10 rounded-full flex items-center justify-center;
      }
    }

    .title {
      @apply text-xl font-bold m-4;
      @apply text-gray-900 dark:text-white;
    }
  }
}

.play-list {
  @apply mt-4 py-4 px-2 rounded-2xl flex-1 overflow-hidden;
  @apply bg-white/95 dark:bg-black/75;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);

  &-title {
    @apply text-lg;
    @apply text-gray-900 dark:text-white;
  }

  &-item {
    @apply flex items-center px-2 py-2 rounded-xl cursor-pointer w-full;
    @apply transition-all duration-200;
    @apply hover:bg-light-200 dark:hover:bg-dark-200;

    &-img {
      @apply flex items-center justify-center rounded-xl text-[40px] w-[60px] h-[60px] bg-light-300 dark:bg-dark-300;
      .iconfont {
        @apply text-[40px];
      }
    }

    &-info {
      @apply ml-2 flex-1;
    }

    &-name {
      @apply text-gray-900 dark:text-white text-base flex items-center gap-2;

      .playlist-creator-tag {
        @apply inline-flex items-center justify-center px-2 rounded-full text-xs;
        @apply bg-light-300 text-primary dark:bg-dark-300 dark:text-white;
        @apply border border-primary/20 dark:border-primary/30;
        height: 18px;
        font-size: 10px;
        font-weight: 500;
        min-width: 60px;
        backdrop-filter: blur(4px);
        -webkit-backdrop-filter: blur(4px);
      }
    }

    &-count {
      @apply text-gray-500 dark:text-gray-400;
    }
  }
}

.login-type {
  @apply text-sm text-green-500 dark:text-green-400;
}

.mobile {
  .user-page {
    padding-left: var(--page-pl);
    padding-right: var(--page-pr);
  }

  .login-container {
    @apply flex justify-center items-center h-full w-full;
  }
}

:deep(.n-tabs-rail) {
  @apply rounded-xl overflow-hidden !important;
  .n-tabs-capsule {
    @apply rounded-xl !important;
  }
}

.profile-modal {
  :deep(.n-card) {
    @apply overflow-hidden rounded-2xl;
  }

  :deep(.n-card-header) {
    @apply px-6 pt-6 pb-3;
  }

  :deep(.n-card__content) {
    @apply px-6 pb-6;
  }

  :deep(.n-card-header__close) {
    @apply mt-1;
  }
}

.profile-modal-title {
  @apply flex items-center gap-3;
}

.profile-modal-title-main {
  @apply text-xl font-bold text-neutral-950 dark:text-white;
}

.profile-modal-title-sub {
  @apply mt-1 text-sm text-neutral-500 dark:text-neutral-400;
}

.profile-form {
  @apply flex flex-col gap-5;
}

.profile-preview {
  @apply relative flex items-center gap-4 overflow-hidden rounded-2xl border border-neutral-200 bg-neutral-50 p-4 dark:border-neutral-800 dark:bg-neutral-900;

  &::before {
    content: '';
    @apply absolute inset-0 opacity-80;
    background:
      linear-gradient(135deg, rgba(34, 197, 94, 0.18), transparent 42%),
      linear-gradient(225deg, rgba(14, 165, 233, 0.14), transparent 36%);
    pointer-events: none;
  }

  > * {
    @apply relative z-10;
  }
}

.profile-avatar-wrap {
  @apply relative flex h-[78px] w-[78px] flex-shrink-0 items-center justify-center;

  :deep(.n-avatar) {
    @apply ring-4 ring-white shadow-xl dark:ring-neutral-950;
  }

  :deep(.profile-avatar-img) {
    @apply h-full w-full object-cover;
  }
}

.profile-avatar-button {
  @apply absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-primary text-sm text-white shadow-lg ring-4 ring-neutral-50 transition hover:bg-primary/90 dark:ring-neutral-900;
}

.profile-preview-text {
  @apply min-w-0 flex-1 text-sm text-gray-700 dark:text-gray-200;

  > div:first-child {
    @apply truncate text-base font-bold text-neutral-950 dark:text-white;
  }

  > div:last-child {
    @apply mt-1 text-xs text-gray-400;
  }
}

.avatar-file-name {
  @apply mt-2 max-w-full truncate rounded-full bg-white/75 px-3 py-1 text-xs text-neutral-500 dark:bg-black/30 dark:text-neutral-400;
}

.profile-field {
  @apply flex flex-col gap-2;

  > span {
    @apply text-sm font-medium text-neutral-700 dark:text-neutral-200;
  }
}

.profile-link-toggle {
  @apply -mt-2 flex w-fit items-center gap-1 text-sm text-neutral-500 transition hover:text-primary dark:text-neutral-400 dark:hover:text-primary;
}

.profile-actions {
  @apply flex justify-end gap-3 border-t border-neutral-100 pt-5 dark:border-neutral-800;
}
</style>
