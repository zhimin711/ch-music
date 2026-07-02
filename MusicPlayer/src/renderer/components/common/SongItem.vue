<template>
  <component
    :is="renderComponent"
    :item="item"
    :favorite="favorite"
    :selectable="selectable"
    :selected="selected"
    :can-remove="canRemove"
    :is-next="isNext"
    :index="index"
    @play="(...args) => $emit('play', ...args)"
    @select="(...args) => $emit('select', ...args)"
    @remove-song="(...args) => $emit('remove-song', ...args)"
  >
    <!-- 自定义副标题插槽（如本地音乐的"碟号/曲目号/年份"） -->
    <template v-if="$slots.subtitle" #subtitle>
      <slot name="subtitle" />
    </template>
  </component>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

import type { SongResult } from '@/types/music';

import CompactSongItem from './songItemCom/CompactSongItem.vue';
import HomeSongItem from './songItemCom/HomeSongItem.vue';
import ListSongItem from './songItemCom/ListSongItem.vue';
import MiniSongItem from './songItemCom/MiniSongItem.vue';
import StandardSongItem from './songItemCom/StandardSongItem.vue';

const props = withDefaults(
  defineProps<{
    item: SongResult;
    mini?: boolean;
    list?: boolean;
    compact?: boolean;
    home?: boolean;
    favorite?: boolean;
    selectable?: boolean;
    selected?: boolean;
    canRemove?: boolean;
    isNext?: boolean;
    index?: number;
  }>(),
  {
    mini: false,
    list: false,
    compact: false,
    home: false,
    favorite: true,
    selectable: false,
    selected: false,
    canRemove: false,
    isNext: false,
    index: undefined
  }
);

defineEmits(['play', 'select', 'remove-song']);

// 根据属性决定渲染哪个组件
const renderComponent = computed(() => {
  if (props.mini) return MiniSongItem;
  if (props.list) return ListSongItem;
  if (props.compact) return CompactSongItem;
  if (props.home) return HomeSongItem;
  return StandardSongItem;
});
</script>
