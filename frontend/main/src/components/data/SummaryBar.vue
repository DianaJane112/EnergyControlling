<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="bg-surface border border-outline-variant rounded-lg p-md flex flex-wrap gap-lg">
    <div
      v-for="item in items"
      :key="item.label"
      class="flex items-center gap-sm"
    >
      <span class="text-body-sm text-on-surface-variant">{{ item.label }}</span>
      <span class="text-body-md font-bold" :class="item.valueClass || 'text-on-surface'">{{ item.value }}</span>
    </div>
  </div>
</template>
