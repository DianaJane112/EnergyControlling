<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '选择月份' },
})

const emit = defineEmits(['update:modelValue'])

const show = ref(false)
const triggerRef = ref(null)
const panelTop = ref('0px')
const panelLeft = ref('0px')

const currentYear = ref(new Date().getFullYear())

function fmtYearMonth(y, m) {
  return `${y}-${String(m + 1).padStart(2, '0')}`
}

async function toggle() {
  if (show.value) { show.value = false; return }
  if (props.modelValue) {
    const [y] = props.modelValue.split('-')
    if (y) currentYear.value = +y
  } else {
    currentYear.value = new Date().getFullYear()
  }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value = `${rect.bottom + 6}px`
    panelLeft.value = `${Math.max(0, rect.left)}px`
  }
  show.value = true
}

function prevYear() { currentYear.value-- }
function nextYear() { currentYear.value++ }

function selectMonth(monthIdx) {
  emit('update:modelValue', fmtYearMonth(currentYear.value, monthIdx))
  show.value = false
}

function isSelected(monthIdx) {
  return fmtYearMonth(currentYear.value, monthIdx) === props.modelValue
}

function isCurrentMonth(monthIdx) {
  const now = new Date()
  return currentYear.value === now.getFullYear() && monthIdx === now.getMonth()
}
</script>

<template>
  <div ref="triggerRef" class="relative inline-flex items-center gap-1.5">
    <div
      class="flex items-center gap-1.5 py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] cursor-pointer bg-white hover:border-[#3a64ab] transition-colors select-none"
      @click="toggle"
    >
      <svg class="w-3.5 h-3.5 text-[#999] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
        <line x1="16" y1="2" x2="16" y2="6" />
        <line x1="8" y1="2" x2="8" y2="6" />
        <line x1="3" y1="10" x2="21" y2="10" />
      </svg>
      <span :class="modelValue ? 'text-[#333]' : 'text-[#999]'">{{ modelValue || placeholder }}</span>
    </div>

    <Teleport to="body">
      <div v-if="show" class="fixed inset-0 z-[110]" @click.self="show = false">
        <div
          class="absolute bg-white rounded-lg p-4"
          style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
          :style="{ top: panelTop, left: panelLeft }"
        >
          <div class="absolute -top-1.5 left-[30px] w-0 h-0 border-l-[6px] border-r-[6px] border-b-[6px] border-l-transparent border-r-transparent border-b-white" />

          <div class="flex items-center justify-between mb-3">
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="prevYear">&lt;</button>
            <span class="text-[14px] font-medium text-[#333]">{{ currentYear }} 年</span>
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="nextYear">&gt;</button>
          </div>

          <div class="grid grid-cols-4 gap-2 w-[224px]">
            <button
              v-for="m in 12"
              :key="m"
              class="py-2 rounded text-[13px] border-none cursor-pointer transition-colors"
              :class="isSelected(m - 1)
                ? 'bg-[#3a64ab] text-white'
                : isCurrentMonth(m - 1)
                  ? 'bg-[#eef3fa] text-[#3a64ab] hover:bg-[#dce8f7]'
                  : 'bg-transparent text-[#333] hover:bg-[#f5f5f5]'"
              @click="selectMonth(m - 1)"
            >{{ m }}月</button>
          </div>

          <div class="flex items-center justify-between mt-3 pt-3 border-t border-[#f0f0f0]">
            <span class="text-[12px] text-[#999]">请选择月份</span>
            <button
              class="py-1 px-3 rounded bg-white border border-[#d9d9d9] text-[#333] text-[12px] cursor-pointer hover:bg-[#f5f5f5]"
              @click="show = false"
            >关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
