<script setup>
defineProps({
  icon: { type: String, default: 'inbox' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="flex flex-col items-center justify-center py-12 text-center">
    <span class="material-symbols-outlined text-[64px] text-outline-variant mb-md">{{ icon }}</span>
    <h3 class="text-title-sm text-on-surface mb-sm">{{ title }}</h3>
    <p v-if="description" class="text-body-md text-on-surface-variant max-w-md">{{ description }}</p>
    <slot />
  </div>
</template>
