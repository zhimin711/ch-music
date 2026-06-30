<template>
  <div class="equalizer-settings">
    <eq-control />
  </div>
</template>

<script setup lang="ts">
import EqControl from '@/components/EQControl.vue';
</script>

<style scoped>
.equalizer-settings {
  display: flex;
  justify-content: center;
}

:deep(.eq-control) {
  max-width: 860px;
}
</style>
