<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, default: 1 },
  totalItems: { type: Number, required: true },
  pageSize: { type: Number, default: 10 },
})

const emit = defineEmits(['update:currentPage'])

const totalPages = computed(() => Math.ceil(props.totalItems / props.pageSize))
const startItem = computed(() => (props.currentPage - 1) * props.pageSize + 1)
const endItem = computed(() => Math.min(props.currentPage * props.pageSize, props.totalItems))

function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    emit('update:currentPage', page)
  }
}

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = props.currentPage
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})
</script>

<template>
  <div class="p-lg flex justify-between items-center border-t border-outline-variant bg-surface-container-lowest">
    <span class="text-body-sm text-on-surface-variant">
      显示 {{ startItem }} 到 {{ endItem }} 条，共 {{ totalItems }} 条
    </span>
    <div class="flex items-center gap-xs">
      <button
        class="p-xs hover:bg-surface-container rounded border border-outline-variant text-outline disabled:opacity-50"
        :disabled="currentPage <= 1"
        @click="goToPage(currentPage - 1)"
      >
        <span class="material-symbols-outlined">chevron_left</span>
      </button>
      <template v-for="page in visiblePages" :key="page">
        <span v-if="page === '...'" class="px-xs text-outline">...</span>
        <button
          v-else
          class="w-8 h-8 flex items-center justify-center rounded font-bold text-body-sm"
          :class="currentPage === page ? 'bg-primary text-white' : 'hover:bg-surface-container text-on-surface-variant'"
          @click="goToPage(page)"
        >{{ page }}</button>
      </template>
      <button
        class="p-xs hover:bg-surface-container rounded border border-outline-variant text-outline disabled:opacity-50"
        :disabled="currentPage >= totalPages"
        @click="goToPage(currentPage + 1)"
      >
        <span class="material-symbols-outlined">chevron_right</span>
      </button>
    </div>
  </div>
</template>
