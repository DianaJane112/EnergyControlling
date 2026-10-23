<script setup>
defineProps({
  status: { type: String, required: true },
  type: { type: String, default: 'default' }
})
</script>

<template>
  <span
    class="px-sm py-0.5 text-[11px] font-bold rounded inline-flex items-center gap-xs"
    :class="{
      'bg-green-50 text-green-600': type === 'success' || type === 'online',
      'bg-amber-50 text-amber-600': type === 'warning',
      'bg-red-50 text-red-600': type === 'error' || type === 'offline',
      'bg-primary-fixed text-primary': type === 'info',
      'bg-surface-container-high text-on-surface-variant': type === 'default',
    }"
  >
    <span
      v-if="type === 'success' || type === 'online' || type === 'error' || type === 'offline' || type === 'warning'"
      class="w-2 h-2 rounded-full"
      :class="{
        'bg-green-600': type === 'success' || type === 'online',
        'bg-amber-600': type === 'warning',
        'bg-red-600': type === 'error' || type === 'offline',
      }"
    ></span>
    {{ status }}
  </span>
</template>
