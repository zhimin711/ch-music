import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import homeRouter from '@/router/home';
import { useSettingsStore } from '@/store/modules/settings';
import { useUserStore } from '@/store/modules/user';
import { isElectron } from '@/utils';

export const useMenuStore = defineStore('menu', () => {
  const allMenus = ref(homeRouter);
  const settingsStore = useSettingsStore();
  const userStore = useUserStore();

  const menus = computed(() => {
    return allMenus.value.filter((item) => {
      if (item.meta?.electronOnly && !isElectron) {
        return false;
      }
      if (item.meta?.hideInSidebar) {
        return false;
      }
      if (item.meta?.requireAuth && !userStore.user) {
        return false;
      }
      if (settingsStore.isMobile) {
        return item.meta?.isMobile !== false;
      }
      return true;
    });
  });

  const setMenus = (newMenus: any[]) => {
    allMenus.value = newMenus;
  };

  return {
    menus,
    setMenus
  };
});
