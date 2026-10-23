<script setup>
import { ref } from 'vue'

defineProps({
  placeholder: { type: String, default: '搜索...' },
})

const emit = defineEmits(['search'])

const query = ref('')

function onSearch() {
  emit('search', query.value)
}
</script>

<template>
  <div class="relative">
    <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-outline">
      <span class="material-symbols-outlined text-[20px]">search</span>
    </span>
    <input
      v-model="query"
      class="w-full h-10 pl-10 pr-4 border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary text-body-md outline-none"
      :placeholder="placeholder"
      type="text"
      @keyup.enter="onSearch"
    />
  </div>
</template>
