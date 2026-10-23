<script setup>
defineProps({
  bars: { type: Array, required: true },
  height: { type: String, default: '240px' },
})
</script>

<template>
  <div class="w-full flex items-end justify-between gap-4" :style="{ height }">
    <div
      v-for="(bar, index) in bars"
      :key="index"
      class="flex flex-col items-center flex-1 h-full"
    >
      <div class="w-full flex flex-col justify-end h-full">
        <div
          v-for="(segment, i) in (bar.segments || [bar])"
          :key="i"
          class="w-full"
          :class="segment.color || 'bg-primary'"
          :style="{ height: segment.height || '30%' }"
        ></div>
      </div>
      <span v-if="bar.label" class="text-label-caps text-outline mt-sm">{{ bar.label }}</span>
    </div>
  </div>
</template>
