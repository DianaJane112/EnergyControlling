<script setup>
import { ref } from 'vue'

defineProps({
  filters: { type: Array, default: () => [] },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
})

const emit = defineEmits(['search', 'filter', 'reset'])

const searchQuery = ref('')

function onSearch() {
  emit('search', searchQuery.value)
}

function onReset() {
  searchQuery.value = ''
  emit('reset')
}
</script>

<template>
  <div class="bg-surface border border-outline-variant rounded-lg p-md flex flex-wrap items-end gap-md">
    <div v-if="showSearch" class="flex-1 min-w-[200px]">
      <label class="text-label-caps text-outline block mb-xs">搜索</label>
      <div class="relative">
        <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-outline">
          <span class="material-symbols-outlined text-[20px]">search</span>
        </span>
        <input
          v-model="searchQuery"
          class="w-full h-10 pl-10 pr-4 border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary text-body-md outline-none"
          :placeholder="searchPlaceholder"
          type="text"
          @keyup.enter="onSearch"
        />
      </div>
    </div>

    <div v-for="filter in filters" :key="filter.key" class="min-w-[160px]">
      <label class="text-label-caps text-outline block mb-xs">{{ filter.label }}</label>
      <select
        class="w-full h-10 px-md border border-outline-variant rounded-lg focus:ring-2 focus:ring-primary focus:border-primary text-body-md outline-none bg-white"
        @change="emit('filter', { key: filter.key, value: $event.target.value })"
      >
        <option v-for="opt in filter.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>

    <div class="flex gap-md">
      <button
        class="h-10 bg-primary text-white rounded-lg font-bold flex items-center justify-center gap-sm hover:opacity-90 transition-opacity px-md"
        @click="onSearch"
      >
        <span class="material-symbols-outlined text-[20px]">search</span>
        查询
      </button>
      <button
        class="px-md h-10 border border-primary text-primary rounded-lg font-bold hover:bg-primary-container/10 transition-colors"
        @click="onReset"
      >
        重置
      </button>
    </div>
  </div>
</template>
