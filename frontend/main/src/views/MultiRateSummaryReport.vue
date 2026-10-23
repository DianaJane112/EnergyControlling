<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue'

// 1. 引入真实 API（使用新的标准函数名）
import { getTimeOfUseMonthly } from '@/api/report'
import { downloadXLSX } from '@/utils/export'

// 2. 接收 ReportCenter 传过来的统一参数
const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([]) // 存放真实后端数据

// ── 3. 核心：发起真实请求并翻译字段 ──────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    const apiParams = {
      startDate: props.searchParams.dateStart || null,
      endDate: props.searchParams.dateEnd || null,
      customerId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }

    const res = await getTimeOfUseMonthly(apiParams)

    if (res && res.data) {
      // 翻译后端字段，适配前端的卡片和表格
      rawData.value = res.data.map((item, index) => ({
        id: index,
        // 如果后端返回了月份字段(如 period 或 reportMonth)，就用它，否则用序号兜底
        period: item.period || item.reportMonth || item.reportDate || `汇总周期 ${index + 1}`,
        meterCount: item.meterCount || 0, // 参与统计的设备数

        totalKwh: item.totalConsumed || 0,
        peakKwh: item.peakConsumed || 0,
        highKwh: item.busyConsumed || 0,
        flatKwh: item.normalConsumed || 0,
        valleyKwh: item.idleConsumed || 0,

        totalAmt: item.totalCharge || item.chargeAmount || 0,
        peakAmt: item.peakCharge || 0,
        highAmt: item.busyCharge || 0,
        flatAmt: item.normalCharge || 0,
        valleyAmt: item.idleCharge || 0
      })).sort((a, b) => {
        const aKey = a.period || ''
        const bKey = b.period || ''
        return aKey.localeCompare(bKey, 'zh-CN')
      })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取复费率汇总表失败', error)
  } finally {
    loading.value = false
  }
}

// 监听参数变化，自动触发请求
watch(
  () => props.searchParams,
  () => { loadData() },
  { deep: true, immediate: true }
)

// ── 客户端关键词过滤 ──────────────────────────────────────────
const filteredData = computed(() => {
  let list = rawData.value
  const p = props.searchParams

  if (p.keyword && p.keyword.trim()) {
    const kw = p.keyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.period && String(r.period).toLowerCase().includes(kw))
    )
  }
  return list
})

// ── Summary cards ────────────────────────────────────────────────────
const summaryCards = computed(() => {
  const recs = filteredData.value
  const sum = (key) => recs.reduce((s, r) => s + (r[key] || 0), 0)
  
  // 汇总总表数（如果有返回的话）
  const totalMeters = recs.reduce((s, r) => s + (r.meterCount || 0), 0)

  return {
    usage: [
      { label: '表计总计', value: totalMeters || recs.length, unit: '台', isCount: true },
      { label: '总用电量', value: sum('totalKwh'), unit: 'kWh' },
      { label: '尖期用电量', value: sum('peakKwh'), unit: 'kWh' },
      { label: '峰期用电量', value: sum('highKwh'), unit: 'kWh' },
      { label: '平期用电量', value: sum('flatKwh'), unit: 'kWh' },
      { label: '谷期用电量', value: sum('valleyKwh'), unit: 'kWh' },
    ],
    cost: [
      { label: '总费用', value: sum('totalAmt'), unit: '¥' },
      { label: '尖期费用', value: sum('peakAmt'), unit: '¥' },
      { label: '峰期费用', value: sum('highAmt'), unit: '¥' },
      { label: '平期费用', value: sum('flatAmt'), unit: '¥' },
      { label: '谷期费用', value: sum('valleyAmt'), unit: '¥' },
    ],
  }
})

// ── Table sorting & Pagination ───────────────────────────────────────
const sortKey = ref('')
const sortDir = ref('asc')
const currentPage = ref(1)
const pageSize = 10

function toggleSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
  currentPage.value = 1
}

// 重置时清除排序状态
watch(() => props.tableResetKey, () => {
  sortKey.value = ''
  sortDir.value = 'asc'
  currentPage.value = 1
})

const sortedRecords = computed(() => {
  let list = [...filteredData.value]
  if (sortKey.value) {
    list.sort((a, b) => {
      const va = a[sortKey.value] ?? ''
      const vb = b[sortKey.value] ?? ''
      const na = typeof va === 'number' ? va : String(va)
      const nb = typeof vb === 'number' ? vb : String(vb)
      if (na < nb) return sortDir.value === 'asc' ? -1 : 1
      if (na > nb) return sortDir.value === 'asc' ? 1 : -1
      return 0
    })
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(sortedRecords.value.length / pageSize)))
const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sortedRecords.value.slice(start, start + pageSize)
})

function goPage(n) { if (n >= 1 && n <= totalPages.value) currentPage.value = n }

// ── 4. Table columns（两级表头，与复费率日报表格式一致）──────────────
// 父级表头（colspan 合并）
const parentHeaders = [
  { label: '序号', colspan: 1, rowspan: 2 },
  { label: '月份/周期', colspan: 1, rowspan: 2 },
  { label: '总用量', colspan: 2, rowspan: 1 },
  { label: '尖用量', colspan: 2, rowspan: 1 },
  { label: '峰用量', colspan: 2, rowspan: 1 },
  { label: '平用量', colspan: 2, rowspan: 1 },
  { label: '谷用量', colspan: 2, rowspan: 1 },
]

// 子级表头（在父级下面）
const childHeaders = [
  { key: 'totalKwh', label: '总电量(kWh)', align: 'right', sortable: true },
  { key: 'totalAmt', label: '总电费(¥)',   align: 'right', sortable: true },
  { key: 'peakKwh',  label: '尖电量(kWh)', align: 'right' },
  { key: 'peakAmt',  label: '尖电费(¥)',   align: 'right' },
  { key: 'highKwh',  label: '峰电量(kWh)', align: 'right' },
  { key: 'highAmt',  label: '峰电费(¥)',   align: 'right' },
  { key: 'flatKwh',  label: '平电量(kWh)', align: 'right' },
  { key: 'flatAmt',  label: '平电费(¥)',   align: 'right' },
  { key: 'valleyKwh',label: '谷电量(kWh)', align: 'right' },
  { key: 'valleyAmt',label: '谷电费(¥)',   align: 'right' },
]

// 数据行渲染用的完整列（固定列 + 子级表头）
const tableColumns = [
  { key: 'period', label: '月份/周期', width: 'w-[120px]', sortable: true },
  ...childHeaders,
]

function formatVal(v) {
  if (typeof v === 'number') return v.toFixed(2)
  return v
}

// ── Custom horizontal scrollbar ──────────────────────────────────────
const tableWrapper = ref(null)
const scrollTrack = ref(null)
const scrollThumb = ref(null)
const thumbLeft = ref(0)
const thumbWidth = ref(80)
const isDragging = ref(false)
let dragStartX = 0
let dragStartLeft = 0

function updateThumb() {
  const wrapper = tableWrapper.value
  if (!wrapper) return
  const ratio = wrapper.clientWidth / wrapper.scrollWidth
  thumbWidth.value = Math.max(ratio * 100, 10)
  const maxLeft = 100 - thumbWidth.value
  const scrollRatio = wrapper.scrollWidth > wrapper.clientWidth
    ? wrapper.scrollLeft / (wrapper.scrollWidth - wrapper.clientWidth)
    : 0
  thumbLeft.value = scrollRatio * maxLeft
}

function onThumbMouseDown(e) {
  isDragging.value = true
  dragStartX = e.clientX
  dragStartLeft = thumbLeft.value
  e.preventDefault()
}

function onTrackClick(e) {
  const track = scrollTrack.value
  const wrapper = tableWrapper.value
  if (!track || !wrapper || e.target !== track) return
  const rect = track.getBoundingClientRect()
  const tw = (thumbWidth.value / 100) * rect.width
  const cx = e.clientX - rect.left
  const ratio = Math.max(0, Math.min(1, (cx - tw / 2) / (rect.width - tw)))
  wrapper.scrollLeft = ratio * (wrapper.scrollWidth - wrapper.clientWidth)
}

function onMouseMove(e) {
  if (!isDragging.value || !scrollTrack.value || !tableWrapper.value) return
  const track = scrollTrack.value
  const trackRect = track.getBoundingClientRect()
  const trackWidth = trackRect.width
  const thumbW = (thumbWidth.value / 100) * trackWidth
  const maxX = trackWidth - thumbW
  const dx = e.clientX - dragStartX
  const newLeft = Math.max(0, Math.min(maxX, dragStartLeft * trackWidth / 100 + dx))
  const percent = maxX > 0 ? newLeft / maxX : 0

  const wrapper = tableWrapper.value
  wrapper.scrollLeft = percent * (wrapper.scrollWidth - wrapper.clientWidth)
}

function onMouseUp() {
  isDragging.value = false
}

function onTableScroll() {
  updateThumb()
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  updateThumb()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})

// 监听真实数据变化，重绘滚动条
watch(rawData, () => {
  setTimeout(updateThumb, 50)
}, { deep: true })

// ── 导出功能 ─────────────────────────────────────────────────
const exportColumns = [
  { key: 'period', label: '月份/周期' },
  { key: 'totalKwh', label: '总电量(kWh)' },
  { key: 'totalAmt', label: '总电费(¥)' },
  { key: 'peakKwh', label: '尖电量(kWh)' },
  { key: 'peakAmt', label: '尖电费(¥)' },
  { key: 'highKwh', label: '峰电量(kWh)' },
  { key: 'highAmt', label: '峰电费(¥)' },
  { key: 'flatKwh', label: '平电量(kWh)' },
  { key: 'flatAmt', label: '平电费(¥)' },
  { key: 'valleyKwh', label: '谷电量(kWh)' },
  { key: 'valleyAmt', label: '谷电费(¥)' },
]

function handleExport() {
  downloadXLSX(exportColumns, filteredData.value, '复费率计费电量汇总表')
}

const registerExport = inject('registerExport', null)
onMounted(() => {
  if (registerExport) registerExport(handleExport)
})
</script>

<template>
  <div>
    
    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex gap-4 overflow-x-auto pb-2">
        <div
          v-for="card in summaryCards.usage"
          :key="card.label"
          class="flex-shrink-0 p-3 bg-[#f5f6fb] rounded text-center"
          style="min-width: 130px;"
        >
          <div class="text-[12px] text-[#666] mb-1">{{ card.label }}</div>
          <div class="text-lg font-bold text-[#3a64ab]">
            {{ card.isCount ? card.value : card.value.toFixed(2) }}
            <span class="text-[12px] font-normal text-[#999]">{{ card.unit }}</span>
          </div>
        </div>
        <div
          v-for="card in summaryCards.cost"
          :key="card.label"
          class="flex-shrink-0 p-3 bg-[#f5f6fb] rounded text-center"
          style="min-width: 130px;"
        >
          <div class="text-[12px] text-[#666] mb-1">{{ card.label }}</div>
          <div class="text-lg font-bold text-[#52c41a]">
            {{ card.value.toFixed(2) }}
            <span class="text-[12px] font-normal text-[#999]">{{ card.unit }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">分时用电汇总</h2>
        <span class="text-[12px] text-[#999]">共 {{ sortedRecords.length }} 条记录</span>
      </div>

        <div
          ref="tableWrapper"
          class="overflow-y-auto overflow-x-hidden max-h-[520px]"
          @scroll="onTableScroll"
        >
          <table class="w-max min-w-full border-collapse text-left">
            <thead class="sticky top-0 z-10">
              <!-- 父级表头：总用量 / 尖用量 / 峰用量 / 平用量 / 谷用量 -->
              <tr>
                <th
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center sticky left-0 z-20"
                  style="min-width: 60px;"
                >序号</th>
                <th
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >月份/周期</th>
                <th
                  colspan="2"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >总用量</th>
                <th
                  colspan="2"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >尖用量</th>
                <th
                  colspan="2"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >峰用量</th>
                <th
                  colspan="2"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >平用量</th>
                <th
                  colspan="2"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                >谷用量</th>
              </tr>
              <!-- 子级表头：各时段电量 / 电费 -->
              <tr>
                <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky left-0 z-20"></th>
                <th
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap cursor-pointer select-none hover:bg-[#ebedf2] transition-colors"
                  @click="toggleSort('period')"
                >
                  月份/周期
                  <span class="text-[10px] ml-0.5 text-[#bbb]">
                    <span v-if="sortKey === 'period'">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
                    <span v-else>⇅</span>
                  </span>
                </th>
                <th
                  v-for="col in childHeaders"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap select-none transition-colors"
                  :class="[
                    col.align === 'right' ? 'text-right' : '',
                    col.sortable ? 'cursor-pointer hover:bg-[#ebedf2]' : '',
                  ]"
                  @click="col.sortable ? toggleSort(col.key) : null"
                >
                  {{ col.label }}
                  <span v-if="col.sortable" class="text-[10px] ml-0.5 text-[#bbb]">
                    <span v-if="sortKey === col.key">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
                    <span v-else>⇅</span>
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedRecords.length === 0 && !loading">
                <td :colspan="tableColumns.length + 1" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
              </tr>
              <tr
                v-for="(row, idx) in paginatedRecords"
                :key="row.id"
              >
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666] sticky left-0 bg-white">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <td
                  v-for="col in tableColumns"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"
                  :class="[
                    col.align === 'right' ? 'text-right font-mono' : '',
                    col.key === 'period' ? 'font-mono' : '',
                  ]"
                >{{ formatVal(row[col.key]) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div
          v-show="sortedRecords.length > 0"
          ref="scrollTrack"
          class="h-2.5 bg-[#f0f0f0] rounded-full relative cursor-pointer select-none mt-3"
          @mousedown="onTrackClick"
        >
          <div
            ref="scrollThumb"
            class="absolute top-0 h-full bg-[#c0c0c0] rounded-full hover:bg-[#a0a0a0] transition-colors"
            :style="{ left: thumbLeft + '%', width: thumbWidth + '%', cursor: isDragging ? 'grabbing' : 'grab' }"
            @mousedown.stop="onThumbMouseDown"
          ></div>
        </div>

      <div v-if="sortedRecords.length > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-2 text-[13px] text-[#666]">
          <span>每页 <b class="text-[#3a64ab]">{{ pageSize }}</b> 条</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[13px] text-[#666] mr-2">{{ currentPage }} / {{ totalPages || 1 }}</span>
          <button
            class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="currentPage <= 1"
            @click="goPage(currentPage - 1)"
          >上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)"
              class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors"
              :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
              @click="goPage(p)"
            >{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button
            class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="currentPage >= totalPages"
            @click="goPage(currentPage + 1)"
          >下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>