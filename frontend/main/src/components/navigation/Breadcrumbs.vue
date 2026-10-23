<script setup>
defineProps({
  items: { type: Array, required: true }
})
</script>

<template>
  <div class="flex items-center gap-sm">
    <template v-for="(item, index) in items" :key="index">
      <span v-if="index > 0" class="text-outline-variant">/</span>
      <a
        v-if="item.route && index < items.length - 1"
        class="text-body-sm text-on-surface-variant hover:text-primary transition-colors"
        :href="item.route"
      >{{ item.label }}</a>
      <span
        v-else
        class="text-body-sm"
        :class="index === items.length - 1 ? 'text-primary font-bold' : 'text-on-surface-variant'"
      >{{ item.label }}</span>
    </template>
  </div>
</template>
