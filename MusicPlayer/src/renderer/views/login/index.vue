<template>
  <div class="login-page">
    <div class="phone-login" :class="setAnimationClass('animate__fadeInDown')">
      <div class="bg"></div>
      <div class="content">
        <div class="login-tabs" :class="setAnimationClass('animate__fadeInUp')">
          <div
            v-for="tab in loginTabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeMode === tab.key }"
            @click="switchToMode(tab.key)"
          >
            {{ tab.label }}
          </div>
        </div>

        <div class="login-content">
          <transition
            name="login-content"
            mode="out-in"
            enter-active-class="animate__animated animate__fadeIn"
            leave-active-class="animate__animated animate__fadeOut"
          >
            <div
              v-if="!isTransitioning"
              :key="activeMode"
              class="phone"
            >
              <div class="login-title">{{ activeMode === 'login' ? 'MusicServer 登录' : '注册 MusicServer' }}</div>
              <div class="phone-page">
                <input
                  v-model="baseUrl"
                  class="phone-input"
                  type="text"
                  placeholder="MusicServer 地址"
                />
                <input
                  v-model="username"
                  class="phone-input"
                  type="text"
                  placeholder="用户名（3-80 位）"
                />
                <input
                  v-model="password"
                  class="phone-input"
                  type="password"
                  placeholder="密码（8-120 位）"
                />
                <input
                  v-if="activeMode === 'register'"
                  v-model="displayName"
                  class="phone-input"
                  type="text"
                  placeholder="显示名称（可选）"
                />
              </div>
              <div class="text">公开搜索、排行继续保留；账号、个人歌单和收藏使用 MusicServer。</div>
              <n-button class="btn-login" :loading="loading" @click="submit">
                {{ activeMode === 'login' ? t('login.button.login') : '注册并登录' }}
              </n-button>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import axios from 'axios';
import { useMessage } from 'naive-ui';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { useMusicServerStore } from '@/store/modules/musicServer';
import { usePlayerStore } from '@/store/modules/player';
import { useUserStore } from '@/store/modules/user';
import { setAnimationClass } from '@/utils';

defineOptions({
  name: 'Login'
});

const { t } = useI18n();
const message = useMessage();
const router = useRouter();
const userStore = useUserStore();
const musicServerStore = useMusicServerStore();
const playerStore = usePlayerStore();

const activeMode = ref<'login' | 'register'>('login');
const isTransitioning = ref(false);
const loginTabs = computed(() => [
  { key: 'login' as const, label: '登录' },
  { key: 'register' as const, label: '注册' }
]);

const loading = ref(false);
const baseUrl = ref(musicServerStore.baseUrl);
const username = ref('');
const password = ref('');
const displayName = ref('');

const switchToMode = (mode: 'login' | 'register') => {
  if (mode === activeMode.value) return;

  isTransitioning.value = true;
  setTimeout(() => {
    activeMode.value = mode;
    setTimeout(() => {
      isTransitioning.value = false;
    }, 50);
  }, 150);
};

const validate = () => {
  const trimmedUsername = username.value.trim();
  if (!trimmedUsername || !password.value) {
    message.error('请填写用户名和密码');
    return false;
  }
  if (trimmedUsername.length < 3 || trimmedUsername.length > 80) {
    message.error('用户名长度需在 3 到 80 位之间');
    return false;
  }
  if (password.value.length < 8 || password.value.length > 120) {
    message.error('密码长度需在 8 到 120 位之间');
    return false;
  }
  if (displayName.value.trim().length > 120) {
    message.error('显示名称不能超过 120 位');
    return false;
  }
  return true;
};

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message || t('login.message.loginFailed');
  }
  return error instanceof Error ? error.message : t('login.message.loginFailed');
};

const submit = async () => {
  if (!validate()) return;
  loading.value = true;
  try {
    musicServerStore.setBaseUrl(baseUrl.value);
    if (activeMode.value === 'login') {
      await musicServerStore.login({
        username: username.value.trim(),
        password: password.value
      });
    } else {
      await musicServerStore.register({
        username: username.value.trim(),
        password: password.value,
        displayName: displayName.value.trim() || undefined
      });
    }
    await userStore.initializeUser();
    await musicServerStore.loadFavorites();
    await playerStore.initializeFavoriteList();
    message.success(t('login.message.loginSuccess'));
    router.push('/user');
  } catch (error) {
    console.error(t('login.message.loginFailed') + ':', error);
    message.error(getErrorMessage(error));
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.login-page {
  @apply flex flex-col items-center justify-center;
  @apply bg-light dark:bg-black;
}

.login-title {
  @apply text-2xl font-bold mb-6 text-white;
}

.text {
  @apply mt-4 text-white text-xs;
}

.phone-login {
  width: 350px;
  height: 550px; /* 恢复原来的高度 */
  @apply rounded-2xl rounded-b-none bg-cover bg-no-repeat relative overflow-hidden;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' version='1.1' xmlns:xlink='http://www.w3.org/1999/xlink' xmlns:svgjs='http://svgjs.dev/svgjs' width='400' height='560' preserveAspectRatio='none' viewBox='0 0 400 560'%3e%3cg mask='url(%26quot%3b%23SvgjsMask1066%26quot%3b)' fill='none'%3e%3crect width='400' height='560' x='0' y='0' fill='rgba(24%2c 106%2c 59%2c 1)'%3e%3c/rect%3e%3cpath d='M0%2c234.738C43.535%2c236.921%2c80.103%2c205.252%2c116.272%2c180.923C151.738%2c157.067%2c188.295%2c132.929%2c207.855%2c94.924C227.898%2c55.979%2c233.386%2c10.682%2c226.119%2c-32.511C218.952%2c-75.107%2c199.189%2c-115.793%2c167.469%2c-145.113C137.399%2c-172.909%2c92.499%2c-171.842%2c55.779%2c-189.967C8.719%2c-213.196%2c-28.344%2c-282.721%2c-78.217%2c-266.382C-128.725%2c-249.834%2c-111.35%2c-166.696%2c-143.781%2c-124.587C-173.232%2c-86.348%2c-244.72%2c-83.812%2c-255.129%2c-36.682C-265.368%2c9.678%2c-217.952%2c48.26%2c-190.512%2c87.004C-167.691%2c119.226%2c-140.216%2c145.431%2c-109.013%2c169.627C-74.874%2c196.1%2c-43.147%2c232.575%2c0%2c234.738' fill='%23114b2a'%3e%3c/path%3e%3cpath d='M400 800.9010000000001C443.973 795.023 480.102 765.6 513.011 735.848 541.923 709.71 561.585 676.6320000000001 577.037 640.85 592.211 605.712 606.958 568.912 601.458 531.035 595.962 493.182 568.394 464.36400000000003 546.825 432.775 522.317 396.88300000000004 507.656 347.475 466.528 333.426 425.366 319.366 384.338 352.414 342.111 362.847 297.497 373.869 242.385 362.645 211.294 396.486 180.212 430.318 192.333 483.83299999999997 188.872 529.644 185.656 572.218 178.696 614.453 191.757 655.101 205.885 699.068 227.92 742.4110000000001 265.75 768.898 304.214 795.829 353.459 807.1220000000001 400 800.9010000000001' fill='%231f894c'%3e%3c/path%3e%3c/g%3e%3cdefs%3e%3cmask id='SvgjsMask1066'%3e%3crect width='400' height='560' fill='white'%3e%3c/rect%3e%3c/mask%3e%3c/defs%3e%3c/svg%3e");
  box-shadow: inset 0px 0px 20px 5px rgba(0, 0, 0, 0.37);
  animation-duration: 0.8s;

  .bg {
    @apply absolute w-full h-full bg-light-100 dark:bg-dark-100 opacity-20;
  }

  .content {
    @apply absolute w-full h-full p-4 flex flex-col items-center justify-center text-center;

    .login-tabs {
      @apply flex mb-6 bg-black bg-opacity-20 rounded-xl p-1;
      width: 320px;
      animation-duration: 0.6s;
      animation-delay: 0.2s;

      .tab-item {
        @apply flex-1 py-2 px-3 text-sm text-white text-center cursor-pointer rounded-lg transition-all duration-300;
        @apply hover:bg-white hover:bg-opacity-10;
        transform: translateY(0);

        &:hover {
          transform: translateY(-2px);
        }

        &.active {
          @apply bg-green-600 text-white font-medium;
          transform: translateY(-1px);
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
        }
      }
    }

    .login-content {
      @apply flex-1 flex items-center justify-center;
      min-height: 300px;
    }

    .phone {
      animation-duration: 0.5s;
      width: 100%;
      max-width: 300px;

      &-page {
        @apply bg-light dark:bg-gray-800 bg-opacity-90 dark:bg-opacity-90;
        width: 250px;
        @apply rounded-2xl overflow-hidden;
        margin: 0 auto;
      }

      &-input {
        height: 40px;
        @apply w-full px-4 outline-none;
        @apply text-gray-900 dark:text-white bg-transparent;
        @apply border-b border-gray-200 dark:border-gray-700;
        @apply placeholder-gray-500 dark:placeholder-gray-400;
        transition: all 0.3s ease;

        &:focus {
          @apply border-green-500;
          transform: translateY(-1px);
        }
      }
    }

    .btn-login {
      width: 250px;
      height: 40px;
      @apply mt-10 text-white rounded-xl;
      @apply bg-green-600 hover:bg-green-700 transition-all duration-300;
      transform: translateY(0);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 12px rgba(34, 197, 94, 0.3);
      }
    }
  }
}

/* 登录内容切换动画 */
.login-content-enter-active,
.login-content-leave-active {
  animation-duration: 0.3s;
}

.login-content-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.login-content-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.mobile {
  .login-page {
    @apply pt-0;
  }

  .phone-login {
    width: 90vw;
    max-width: 350px;
    height: 500px;
  }
}
</style>
