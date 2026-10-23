<script setup>
import { computed } from 'vue'

const props = defineProps({
  bars: { type: Array, required: true },
  color: { type: String, default: 'bg-secondary-fixed-dim' },
  height: { type: String, default: '240px' },
})
</script>

<template>
  <div class="w-full flex items-end justify-between gap-4" :style="{ height }">
    <div
      v-for="(bar, index) in bars"
      :key="index"
      class="flex flex-col items-center flex-1"
    >
      <div
        class="chart-bar w-full rounded-t-sm"
        :class="bar.color || color"
        :style="{ height: bar.height || '50%' }"
      ></div>
      <span v-if="bar.label" class="text-label-caps text-outline mt-sm">{{ bar.label }}</span>
    </div>
  </div>
</template>
