<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '选择年份' },
})

const emit = defineEmits(['update:modelValue'])

const show = ref(false)
const triggerRef = ref(null)
const panelTop = ref('0px')
const panelLeft = ref('0px')

const currentYear = new Date().getFullYear()
function decadeStartOf(y) { return Math.floor(y / 10) * 10 }
const decadeStart = ref(decadeStartOf(Number(props.modelValue) || currentYear))
const decadeYears = computed(() => Array.from({ length: 10 }, (_, i) => decadeStart.value + i))
const decadeLabel = computed(() => `${decadeStart.value} - ${decadeStart.value + 9}`)

async function toggle() {
  if (show.value) { show.value = false; return }
  if (props.modelValue) {
    decadeStart.value = decadeStartOf(Number(props.modelValue))
  } else {
    decadeStart.value = decadeStartOf(currentYear)
  }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value = `${rect.bottom + 6}px`
    panelLeft.value = `${Math.max(0, rect.left)}px`
  }
  show.value = true
}

function prevDecade() { decadeStart.value -= 10 }
function nextDecade() { decadeStart.value += 10 }

function selectYear(y) {
  emit('update:modelValue', String(y))
  show.value = false
}

function isSelected(y) { return String(y) === String(props.modelValue) }
function isCurrentYear(y) { return y === currentYear }
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
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="prevDecade">&lt;</button>
            <span class="text-[14px] font-medium text-[#333]">{{ decadeLabel }}</span>
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="nextDecade">&gt;</button>
          </div>

          <div class="grid grid-cols-5 gap-2 w-[280px]">
            <button
              v-for="y in decadeYears"
              :key="y"
              class="py-2 rounded text-[13px] border-none cursor-pointer transition-colors"
              :class="isSelected(y)
                ? 'bg-[#3a64ab] text-white'
                : isCurrentYear(y)
                  ? 'bg-[#eef3fa] text-[#3a64ab] hover:bg-[#dce8f7]'
                  : 'bg-transparent text-[#333] hover:bg-[#f5f5f5]'"
              @click="selectYear(y)"
            >{{ y }}</button>
          </div>

          <div class="flex items-center justify-between mt-3 pt-3 border-t border-[#f0f0f0]">
            <span class="text-[12px] text-[#999]">请选择年份</span>
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
