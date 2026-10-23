<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '选择日期' },
})

const emit = defineEmits(['update:modelValue'])

const show = ref(false)
const triggerRef = ref(null)

// panel position
const panelTop  = ref('0px')
const panelLeft = ref('0px')

// internal cursor – always YYYY-MM-DD
const cursor = ref(today())

function today() {
  return fmt(new Date())
}
function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function parse(str) {
  if (!str) return null
  const d = new Date(str)
  return isNaN(d.getTime()) ? null : d
}

const cursorDate  = computed(() => new Date(cursor.value))
const cursorYear  = computed(() => cursorDate.value.getFullYear())
const cursorMonth = computed(() => cursorDate.value.getMonth())

const daysInMonth = computed(() => new Date(cursorYear.value, cursorMonth.value + 1, 0).getDate())
const startDayOfWeek = computed(() => new Date(cursorYear.value, cursorMonth.value, 1).getDay())

const calendarDays = computed(() => {
  const total = daysInMonth.value
  const start = startDayOfWeek.value
  const cells = []
  for (let i = 0; i < start; i++) cells.push(null)
  for (let d = 1; d <= total; d++) cells.push(d)
  return cells
})

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

function prevMonth() {
  const d = cursorDate.value
  d.setMonth(d.getMonth() - 1)
  cursor.value = fmt(d)
}
function nextMonth() {
  const d = cursorDate.value
  d.setMonth(d.getMonth() + 1)
  cursor.value = fmt(d)
}

function selectDay(day) {
  const y = cursorYear.value
  const m = String(cursorMonth.value + 1).padStart(2, '0')
  const d = String(day).padStart(2, '0')
  emit('update:modelValue', `${y}-${m}-${d}`)
  show.value = false
}

async function toggle() {
  if (show.value) { show.value = false; return }
  if (props.modelValue) {
    const d = parse(props.modelValue)
    if (d) cursor.value = fmt(d)
  }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value  = `${rect.bottom + 6}px`
    panelLeft.value = `${rect.left}px`
  }
  show.value = true
}

function isToday(day) {
  const t = new Date()
  return t.getFullYear() === cursorYear.value &&
    t.getMonth() === cursorMonth.value &&
    t.getDate() === day
}
function isSelected(day) {
  if (!props.modelValue) return false
  const s = parse(props.modelValue)
  if (!s) return false
  return s.getFullYear() === cursorYear.value &&
    s.getMonth() === cursorMonth.value &&
    s.getDate() === day
}
</script>

<template>
  <div ref="triggerRef" class="relative inline-block">
    <!-- trigger -->
    <div
      class="flex items-center gap-1.5 py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[160px] cursor-pointer bg-white hover:border-[#3a64ab] transition-colors select-none"
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

    <!-- dropdown -->
    <Teleport to="body">
      <div
        v-if="show"
        class="fixed inset-0 z-[80]"
        @click.self="show = false"
      >
        <div
          class="absolute bg-white rounded-lg p-3 w-[224px]"
          style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
          :style="{ top: panelTop, left: panelLeft }"
        >
          <!-- arrow -->
          <div class="absolute -top-1.5 left-5 w-0 h-0 border-l-[6px] border-r-[6px] border-b-[6px] border-l-transparent border-r-transparent border-b-white" />

          <!-- month nav -->
          <div class="flex items-center justify-between mb-2">
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="prevMonth">&lt;</button>
            <span class="text-[13px] font-medium text-[#333]">{{ cursorYear }} 年 {{ cursorMonth + 1 }} 月</span>
            <button class="w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm" @click="nextMonth">&gt;</button>
          </div>

          <!-- day headers -->
          <div class="grid grid-cols-7 mb-1">
            <div v-for="wd in weekDays" :key="wd" class="text-center text-[11px] text-[#999] py-1">{{ wd }}</div>
          </div>

          <!-- day grid -->
          <div class="grid grid-cols-7">
            <div v-for="(cell, idx) in calendarDays" :key="idx" class="text-center py-0.5">
              <button
                v-if="cell !== null"
                class="w-7 h-7 rounded text-[13px] border-none cursor-pointer transition-colors"
                :class="isSelected(cell)
                  ? 'bg-[#3a64ab] text-white'
                  : isToday(cell)
                    ? 'bg-[#eef3fa] text-[#3a64ab]'
                    : 'bg-transparent text-[#333] hover:bg-[#f5f5f5]'"
                @click="selectDay(cell)"
              >{{ cell }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
