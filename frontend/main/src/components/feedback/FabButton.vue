<script setup>
defineProps({
  icon: { type: String, default: 'add' },
  label: { type: String, default: '' },
})

defineEmits(['click'])
</script>

<template>
  <button
    class="fixed bottom-6 right-6 bg-primary-container text-on-primary-container shadow-lg rounded-full p-4 hover:shadow-xl transition-all z-50 flex items-center gap-sm"
    @click="$emit('click')"
  >
    <span class="material-symbols-outlined text-[24px]">{{ icon }}</span>
    <span v-if="label" class="text-body-md font-bold">{{ label }}</span>
  </button>
</template>
