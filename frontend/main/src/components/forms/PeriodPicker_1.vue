<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  granularity: { type: String, required: true },        // 'daily' | 'monthly' | 'yearly'
  pickerYear: { type: Number, required: true },
  selectedMonth: { type: Number, default: 1 },
  selectedYear: { type: Number, required: true },
})

const emit = defineEmits([
  'update:pickerYear',
  'update:selectedMonth',
  'update:selectedYear',
  'change',
])

const show = ref(false)
const triggerRef = ref(null)
const panelTop = ref('0px')
const panelLeft = ref('0px')

const months = Array.from({ length: 12 }, (_, i) => i + 1)

const yearGroupStart = computed(() => Math.floor(props.pickerYear / 10) * 10)
const yearGroupYears = computed(() => {
  const s = yearGroupStart.value
  return Array.from({ length: 10 }, (_, i) => s + i)
})

function displayLabel() {
  if (props.granularity === 'daily') {
    return `${props.pickerYear}年${props.selectedMonth}月`
  }
  if (props.granularity === 'monthly') {
    return `${props.selectedYear}年`
  }
  return ''
}

function prevYear() { emit('update:pickerYear', props.pickerYear - 1) }
function nextYear() { emit('update:pickerYear', props.pickerYear + 1) }
function prevGroup() { emit('update:pickerYear', props.pickerYear - 10) }
function nextGroup() { emit('update:pickerYear', props.pickerYear + 10) }

function selectMonth(m) {
  emit('update:selectedMonth', m)
  emit('change')
  show.value = false
}

function selectYear(y) {
  emit('update:selectedYear', y)
  emit('change')
  show.value = false
}

async function toggle() {
  if (show.value) { show.value = false; return }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value = `${rect.bottom + 6}px`
    panelLeft.value = `${Math.max(0, rect.left - 120)}px`
  }
  show.value = true
}
</script>

<template>
  <div ref="triggerRef" class="relative inline-flex items-center">
    <!-- trigger bar -->
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
      <span class="text-[#333]">{{ displayLabel() }}</span>
      <svg class="w-3 h-3 text-[#999]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </div>

    <!-- dropdown panel -->
    <Teleport to="body">
      <div v-if="show" class="fixed inset-0 z-[80]" @click.self="show = false">
        <div
          class="absolute bg-white rounded-lg p-4"
          style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
          :style="{ top: panelTop, left: panelLeft }"
        >
          <div class="absolute -top-1.5 left-[30px] w-0 h-0 border-l-[6px] border-r-[6px] border-b-[6px] border-l-transparent border-r-transparent border-b-white" />

          <!-- ══ 日数据：年 + 月 ══ -->
          <template v-if="granularity === 'daily'">
            <div class="flex items-center justify-center gap-3 mb-3">
              <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="prevYear">&lt;</button>
              <span class="text-[14px] font-medium text-[#333] min-w-[60px] text-center">{{ pickerYear }} 年</span>
              <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="nextYear">&gt;</button>
            </div>
            <div class="grid grid-cols-4 gap-1.5 w-[240px]">
              <button
                v-for="m in months" :key="m"
                class="py-2 rounded text-[13px] border cursor-pointer transition-colors"
                :class="selectedMonth === m
                  ? 'bg-[#3a64ab] text-white border-[#3a64ab]'
                  : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
                @click="selectMonth(m)"
              >{{ m }}月</button>
            </div>
          </template>

          <!-- ══ 月数据：年组 + 年 ══ -->
          <template v-if="granularity === 'monthly'">
            <div class="flex items-center justify-center gap-3 mb-3">
              <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="prevGroup">&lt;</button>
              <span class="text-[14px] font-medium text-[#333] min-w-[90px] text-center">{{ yearGroupStart }}-{{ yearGroupStart + 9 }}</span>
              <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="nextGroup">&gt;</button>
            </div>
            <div class="grid grid-cols-5 gap-1.5 w-[300px]">
              <button
                v-for="y in yearGroupYears" :key="y"
                class="py-2 rounded text-[13px] border cursor-pointer transition-colors"
                :class="selectedYear === y
                  ? 'bg-[#3a64ab] text-white border-[#3a64ab]'
                  : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
                @click="selectYear(y)"
              >{{ y }}</button>
            </div>
          </template>

          <div class="flex justify-end mt-3 pt-3 border-t border-[#f0f0f0]">
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
