<template>
  <div class="login-page">
    <div class="login-shell" :class="setAnimationClass('animate__fadeInUp')">
      <section class="brand-panel">
        <img :src="brandLogo" alt="朝华音乐" class="brand-logo" />
        <div>
          <div class="brand-name">朝华音乐</div>
          <div class="brand-subtitle">欢迎回来</div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="login-tabs">
          <button
            v-for="tab in loginTabs"
            :key="tab.key"
            type="button"
            class="tab-item"
            :class="{ active: activeMode === tab.key }"
            @click="switchToMode(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <transition
          name="login-content"
          mode="out-in"
          enter-active-class="animate__animated animate__fadeIn"
          leave-active-class="animate__animated animate__fadeOut"
        >
          <form
            v-if="!isTransitioning"
            :key="activeMode"
            class="login-form"
            @submit.prevent="submit"
          >
            <div>
              <div class="login-title">{{ activeMode === 'login' ? '账号登录' : '创建账号' }}</div>
              <div class="login-hint">
                {{ activeMode === 'login' ? '输入账号继续使用' : '填写信息完成注册' }}
              </div>
            </div>

            <div class="form-fields">
              <label class="field">
                <i class="ri-user-line"></i>
                <input
                  v-model="username"
                  type="text"
                  autocomplete="username"
                  placeholder="用户名（3-80 位）"
                />
              </label>
              <label class="field">
                <i class="ri-lock-line"></i>
                <input
                  v-model="password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="密码（8-120 位）"
                />
              </label>
              <label v-if="activeMode === 'register'" class="field">
                <i class="ri-id-card-line"></i>
                <input
                  v-model="displayName"
                  type="text"
                  autocomplete="nickname"
                  placeholder="显示名称（可选）"
                />
              </label>
            </div>

            <n-button attr-type="submit" class="btn-login" type="primary" :loading="loading">
              {{ activeMode === 'login' ? t('login.button.login') : '注册并登录' }}
            </n-button>
          </form>
        </transition>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import axios from 'axios';
import { useMessage } from 'naive-ui';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import brandLogo from '@/assets/chaohua-logo.svg';
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
  @apply flex min-h-full items-center justify-center bg-neutral-100 px-6 py-10 transition-colors duration-300 dark:bg-black;
}

.login-shell {
  @apply grid w-full max-w-[820px] grid-cols-[0.9fr_1.1fr] overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-xl shadow-neutral-900/10 dark:border-neutral-800 dark:bg-neutral-950 dark:shadow-black/40;
  min-height: 520px;
}

.brand-panel {
  @apply flex flex-col justify-between bg-neutral-950 p-8 text-white;
  background:
    linear-gradient(135deg, rgba(34, 197, 94, 0.24), transparent 42%),
    linear-gradient(180deg, #111827, #0a0f1a);

  &::after {
    content: '';
    @apply mt-auto block h-1 w-24 rounded-full bg-green-400;
  }
}

.brand-logo {
  @apply h-20 w-20 rounded-2xl shadow-lg shadow-black/30;
}

.brand-name {
  @apply mt-8 text-3xl font-extrabold leading-tight;
}

.brand-subtitle {
  @apply mt-3 text-sm text-neutral-300;
}

.auth-panel {
  @apply flex flex-col justify-center p-8;
}

.login-tabs {
  @apply mb-10 grid grid-cols-2 rounded-lg bg-neutral-100 p-1 dark:bg-neutral-900;
}

.tab-item {
  @apply h-10 rounded-md text-sm font-medium text-neutral-500 transition-all duration-200 dark:text-neutral-400;

  &:hover {
    @apply text-neutral-900 dark:text-white;
  }

  &.active {
    @apply bg-white text-neutral-950 shadow-sm dark:bg-neutral-800 dark:text-white;
  }
}

.login-form {
  @apply flex flex-col gap-7;
}

.login-title {
  @apply text-2xl font-bold text-neutral-950 dark:text-white;
}

.login-hint {
  @apply mt-2 text-sm text-neutral-500 dark:text-neutral-400;
}

.form-fields {
  @apply grid gap-3;
}

.field {
  @apply flex h-12 items-center gap-3 rounded-lg border border-neutral-200 bg-neutral-50 px-4 text-neutral-500 transition-all duration-200 dark:border-neutral-800 dark:bg-neutral-900 dark:text-neutral-400;

  &:focus-within {
    @apply border-green-500 bg-white text-green-600 shadow-sm shadow-green-500/10 dark:bg-neutral-950 dark:text-green-400;
  }

  i {
    @apply text-lg;
  }

  input {
    @apply h-full min-w-0 flex-1 bg-transparent text-sm text-neutral-900 outline-none placeholder:text-neutral-400 dark:text-white dark:placeholder:text-neutral-500;
  }
}

.btn-login {
  @apply h-12 rounded-lg text-sm font-semibold;
}

/* 登录内容切换动画 */
.login-content-enter-active,
.login-content-leave-active {
  animation-duration: 0.3s;
}

.login-content-enter-from {
  opacity: 0;
  transform: translateY(16px);
}

.login-content-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}

.mobile {
  .login-page {
    @apply px-4 py-6;
  }

  .login-shell {
    @apply max-w-[420px] grid-cols-1;
    min-height: auto;
  }

  .brand-panel {
    @apply flex-row items-center justify-start gap-4 p-5;

    &::after {
      display: none;
    }
  }

  .brand-logo {
    @apply h-14 w-14 rounded-xl;
  }

  .brand-name {
    @apply mt-0 text-xl;
  }

  .brand-subtitle {
    @apply mt-1;
  }

  .auth-panel {
    @apply p-5;
  }
}
</style>
