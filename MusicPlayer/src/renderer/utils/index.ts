import { computed } from 'vue';

import defaultCoverUrl from '@/assets/chaohua-logo.svg';
import { useSettingsStore } from '@/store/modules/settings';

export const DEFAULT_COVER_URL = defaultCoverUrl;

const LEGACY_DEFAULT_COVER = '/images/default_cover.png';

export const normalizeCoverUrl = (url?: string | null) => {
  const trimmed = url?.trim();
  if (!trimmed || trimmed === LEGACY_DEFAULT_COVER) {
    return DEFAULT_COVER_URL;
  }
  return trimmed;
};

// 设置歌手背景图片
export const setBackgroundImg = (url: String) => {
  return `background-image:url(${url})`;
};
// 设置动画类型
export const setAnimationClass = (type: String) => {
  const settingsStore = useSettingsStore();
  if (settingsStore.setData && settingsStore.setData.noAnimate) {
    return '';
  }
  const speed = settingsStore.setData?.animationSpeed || 1;

  let speedClass = '';
  if (speed <= 0.3) speedClass = 'animate__slower';
  else if (speed <= 0.8) speedClass = 'animate__slow';
  else if (speed >= 2.5) speedClass = 'animate__faster';
  else if (speed >= 1.5) speedClass = 'animate__fast';

  return `animate__animated ${type}${speedClass ? ` ${speedClass}` : ''}`;
};
// 设置动画延时
export const setAnimationDelay = (index: number = 6, time: number = 50) => {
  const settingsStore = useSettingsStore();
  if (settingsStore.setData?.noAnimate) {
    return '';
  }
  const speed = settingsStore.setData?.animationSpeed || 1;
  return `animation-delay:${(index * time) / (speed * 2)}ms`;
};

// 计算动画延迟(秒) - 用于新的动画效果
// 根据动画速度配置自动调整延迟时间
export const calculateAnimationDelay = (index: any, baseDelay: number = 0.03): string => {
  const settingsStore = useSettingsStore();
  if (settingsStore.setData?.noAnimate) {
    return '0s';
  }
  const speed = settingsStore.setData?.animationSpeed || 1;
  // 速度越快，延迟应该越短，所以除以 speed
  const delay = (index * baseDelay) / speed;
  return `${delay.toFixed(3)}s`;
};

// 将秒转换为分钟和秒
export const secondToMinute = (s: number) => {
  if (!s) {
    return '00:00';
  }
  const minute: number = Math.floor(s / 60);
  const second: number = Math.floor(s % 60);
  const minuteStr: string = minute > 9 ? minute.toString() : `0${minute.toString()}`;
  const secondStr: string = second > 9 ? second.toString() : `0${second.toString()}`;
  return `${minuteStr}:${secondStr}`;
};

// 格式化数字 千,万, 百万, 千万,亿
const units = [
  { value: 1e8, symbol: '亿' },
  { value: 1e4, symbol: '万' }
];

export const formatNumber = (num: string | number) => {
  num = Number(num);
  for (let i = 0; i < units.length; i++) {
    if (num >= units[i].value) {
      return `${(num / units[i].value).toFixed(1)}${units[i].symbol}`;
    }
  }
  return num.toString();
};

export const getImgUrl = (url: string | null | undefined, size: string = '') => {
  const normalizedUrl = normalizeCoverUrl(url);

  // base64 Data URL 和本地文件路径不需要添加尺寸参数
  if (
    normalizedUrl.startsWith('data:') ||
    normalizedUrl.startsWith('local://') ||
    normalizedUrl.startsWith('blob:') ||
    normalizedUrl === DEFAULT_COVER_URL
  ) {
    return normalizedUrl;
  }

  if (normalizedUrl.startsWith('/') && !normalizedUrl.startsWith('//')) {
    return normalizedUrl;
  }

  if (normalizedUrl.includes('thumbnail')) {
    // 只替换最后一个 thumbnail 参数的尺寸
    return normalizedUrl.replace(/thumbnail=\d+y\d+(?!.*thumbnail)/, `thumbnail=${size}`);
  }

  const imgUrl = `${normalizedUrl}?param=${size}`;
  return imgUrl;
};

export const isMobile = computed(() => {
  const settingsStore = useSettingsStore();
  return settingsStore.isMobile;
});

export const isElectron = (window as any).electron !== undefined;

export const isLyricWindow = computed(() => {
  return window.location.hash.includes('lyric');
});

export const getSetData = (): any => {
  let setData = null;
  if (window.electron) {
    setData = window.electron.ipcRenderer.sendSync('get-store-value', 'set');
  } else {
    const settingsStore = useSettingsStore();
    setData = settingsStore.setData;
  }
  return setData;
};
