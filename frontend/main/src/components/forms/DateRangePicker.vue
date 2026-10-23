<script setup>
import { ref, reactive, computed, nextTick, onBeforeUnmount } from 'vue'

const props = defineProps({
  startValue: { type: String, default: '' },
  endValue:   { type: String, default: '' },
  startPlaceholder: { type: String, default: '开始日期' },
  endPlaceholder:   { type: String, default: '结束日期' },
  single: { type: Boolean, default: false },
})

const emit = defineEmits(['update:startValue', 'update:endValue'])

const show = ref(false)
const triggerRef = ref(null)
const panelTop  = ref('0px')
const panelLeft = ref('0px')

// which side is the user picking now
const picking = ref('start') // 'start' | 'end'

// base month for the left calendar
const base = ref(today())

function today() { return fmt(new Date()) }
function fmt(d) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
function parse(s) { if (!s) return null; const d = new Date(s); return isNaN(d.getTime()) ? null : d }
function ymd(y,m,d) { return `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}` }

// ── calendar helpers ──────────────────────────────────────────────
function calData(year, month) {
  const days = new Date(year, month + 1, 0).getDate()
  const startDow = new Date(year, month, 1).getDay()
  const cells = []
  for (let i = 0; i < startDow; i++) cells.push(null)
  for (let d = 1; d <= days; d++) cells.push(d)
  return { year, month, cells }
}

const leftCal = computed(() => {
  const d = new Date(base.value)
  return calData(d.getFullYear(), d.getMonth())
})
const rightCal = computed(() => {
  const d = new Date(base.value)
  d.setMonth(d.getMonth() + 1)
  return calData(d.getFullYear(), d.getMonth())
})

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// ── navigation ────────────────────────────────────────────────────
function prevPair() {
  const d = new Date(base.value)
  d.setMonth(d.getMonth() - 1)
  base.value = fmt(d)
}
function nextPair() {
  const d = new Date(base.value)
  d.setMonth(d.getMonth() + 1)
  base.value = fmt(d)
}

// ── open / close ──────────────────────────────────────────────────
async function toggle() {
  if (show.value) { show.value = false; return }
  // start picking from whichever side is empty, default to start
  if (!props.startValue) {
    picking.value = 'start'
  } else if (!props.endValue) {
    picking.value = 'end'
  } else {
    // both filled – re-pick start
    picking.value = 'start'
  }
  // set base month from existing values
  if (props.startValue) {
    const d = parse(props.startValue)
    if (d) base.value = fmt(new Date(d.getFullYear(), d.getMonth(), 1))
  }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value  = `${rect.bottom + 6}px`
    panelLeft.value = `${Math.max(0, rect.left - 150)}px`
  }
  show.value = true
}

function selectDay(year, month, day) {
  const val = ymd(year, month, day)
  if (props.single) {
    emit('update:startValue', val)
    show.value = false
    return
  }
  if (picking.value === 'start') {
    emit('update:startValue', val)
    if (props.endValue && val > props.endValue) {
      emit('update:endValue', '')
    }
    picking.value = 'end'
  } else {
    if (props.startValue && val < props.startValue) {
      emit('update:endValue', '')
      emit('update:startValue', val)
      picking.value = 'end'
    } else {
      emit('update:endValue', val)
      show.value = false
    }
  }
}

// ── cell classes ──────────────────────────────────────────────────
function cellClass(year, month, day) {
  const d = ymd(year, month, day)
  const s = props.startValue
  const e = props.endValue
  if (d === s || d === e) return 'bg-[#3a64ab] text-white'
  if (s && e && d > s && d < e) return 'bg-[#eef3fa] rounded-none text-[#3a64ab]'
  const t = today()
  if (d === t) return 'bg-[#eef3fa] text-[#3a64ab]'
  return 'bg-transparent text-[#333] hover:bg-[#f5f5f5]'
}

// current "active" date to show as focused ring
const hoverDate = ref('')

onBeforeUnmount(() => { show.value = false })
</script>

<template>
  <div ref="triggerRef" class="relative inline-flex items-center gap-1.5">
    <!-- trigger: two date slots in one bar -->
    <div
      class="flex items-center gap-1.5 py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] cursor-pointer bg-white hover:border-[#3a64ab] transition-colors select-none"
      :class="single ? 'w-[180px]' : ''"
      @click="toggle"
    >
      <svg class="w-3.5 h-3.5 text-[#999] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
        <line x1="16" y1="2" x2="16" y2="6" />
        <line x1="8" y1="2" x2="8" y2="6" />
        <line x1="3" y1="10" x2="21" y2="10" />
      </svg>
      <template v-if="single">
        <span :class="startValue ? 'text-[#333]' : 'text-[#999]'">{{ startValue || startPlaceholder }}</span>
      </template>
      <template v-else>
        <span :class="startValue ? 'text-[#333]' : 'text-[#999]'">{{ startValue || startPlaceholder }}</span>
        <span class="text-[#999]">-</span>
        <span :class="endValue ? 'text-[#333]' : 'text-[#999]'">{{ endValue || endPlaceholder }}</span>
      </template>
    </div>

    <!-- dual-calendar dropdown -->
    <Teleport to="body">
      <div v-if="show" class="fixed inset-0 z-[110]" @click.self="show = false">
        <div
          class="absolute bg-white rounded-lg p-4"
          style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
          :style="{ top: panelTop, left: panelLeft }"
        >
          <div class="absolute -top-1.5 left-[30px] w-0 h-0 border-l-[6px] border-r-[6px] border-b-[6px] border-l-transparent border-r-transparent border-b-white" />

          <!-- nav arrows -->
          <button class="absolute top-4 left-3 w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm z-10" @click="prevPair">&lt;</button>
          <button class="absolute top-4 right-3 w-6 h-6 flex items-center justify-center rounded border-none bg-transparent cursor-pointer text-[#666] hover:bg-[#f5f5f5] text-sm z-10" @click="nextPair">&gt;</button>

          <div class="flex gap-6">
            <!-- left calendar -->
            <div class="w-[224px]">
              <div class="text-center text-[13px] font-medium text-[#333] mb-2">{{ leftCal.year }} 年 {{ leftCal.month + 1 }} 月</div>
              <div class="grid grid-cols-7 mb-1">
                <div v-for="wd in weekDays" :key="'l'+wd" class="text-center text-[11px] text-[#999] py-1">{{ wd }}</div>
              </div>
              <div class="grid grid-cols-7">
                <div v-for="(cell, idx) in leftCal.cells" :key="'lc'+idx" class="text-center py-0.5">
                  <button
                    v-if="cell !== null"
                    class="w-7 h-7 rounded text-[13px] border-none cursor-pointer transition-colors"
                    :class="cellClass(leftCal.year, leftCal.month, cell)"
                    @click="selectDay(leftCal.year, leftCal.month, cell)"
                  >{{ cell }}</button>
                </div>
              </div>
            </div>

            <!-- right calendar (hidden in single mode) -->
            <div v-if="!single" class="w-[224px]">
              <div class="text-center text-[13px] font-medium text-[#333] mb-2">{{ rightCal.year }} 年 {{ rightCal.month + 1 }} 月</div>
              <div class="grid grid-cols-7 mb-1">
                <div v-for="wd in weekDays" :key="'r'+wd" class="text-center text-[11px] text-[#999] py-1">{{ wd }}</div>
              </div>
              <div class="grid grid-cols-7">
                <div v-for="(cell, idx) in rightCal.cells" :key="'rc'+idx" class="text-center py-0.5">
                  <button
                    v-if="cell !== null"
                    class="w-7 h-7 rounded text-[13px] border-none cursor-pointer transition-colors"
                    :class="cellClass(rightCal.year, rightCal.month, cell)"
                    @click="selectDay(rightCal.year, rightCal.month, cell)"
                  >{{ cell }}</button>
                </div>
              </div>
            </div>
          </div>

          <!-- picking hint -->
          <div class="flex items-center justify-between mt-3 pt-3 border-t border-[#f0f0f0]">
            <span class="text-[12px] text-[#999]">
              {{ single ? '请选择日期' : picking === 'start' ? '请选择开始日期' : '请选择结束日期' }}
            </span>
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
