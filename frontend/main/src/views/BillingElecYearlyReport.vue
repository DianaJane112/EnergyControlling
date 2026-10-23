<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue'
import { getBillingYearlyDetail } from '@/api/report'
import { downloadXLSX } from '@/utils/export'

const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])

// ── 动态年度/月度标签 ───────────────────────────────────────────
const now = new Date()
const currentYear = String(now.getFullYear())
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')
const yearLabel = computed(() => currentYear + '年度')
const monthLabel = computed(() => (now.getMonth() + 1) + '月')
const statMonth = computed(() => currentYear + '-' + currentMonth)

// ── 调用真实 API ──────────────────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    const apiParams = {
      year: currentYear,
      statMonth: statMonth.value,
      merchantId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }
    const res = await getBillingYearlyDetail(apiParams)
    if (res && res.data) {
      rawData.value = (Array.isArray(res.data) ? res.data : []).map((item, idx) => ({
        ...item,
        _rowId: idx,
      })).sort((a, b) => {
        const aKey = (a.deviceNo || '') + '|' + (a.customerName || '')
        const bKey = (b.deviceNo || '') + '|' + (b.customerName || '')
        return aKey.localeCompare(bKey, 'zh-CN')
      })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取计费电量年报表明细失败', error)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.searchParams, () => { loadData() }, { deep: true, immediate: true })

// ── 客户端关键词过滤 ──────────────────────────────────────────
const filteredData = computed(() => {
  let list = rawData.value
  const p = props.searchParams

  if (p.keyword && p.keyword.trim()) {
    const kw = p.keyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.customerName && String(r.customerName).toLowerCase().includes(kw)) ||
      (r.deviceNo && String(r.deviceNo).toLowerCase().includes(kw))
    )
  }
  if (p.virtualKeyword && p.virtualKeyword.trim()) {
    const vk = p.virtualKeyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.virtualMerchantName && String(r.virtualMerchantName).toLowerCase().includes(vk)) ||
      (r.virtualMeterNo && String(r.virtualMeterNo).toLowerCase().includes(vk))
    )
  }
  return list
})

// ── Summary cards ──────────────────────────────────────────────
const summary = computed(() => {
  const recs = filteredData.value
  const sum = (key) => recs.reduce((s, r) => s + (Number(r[key]) || 0), 0)
  return {
    meterCount: recs.length,
    totalYearAmount: sum('yearAmount'),
    totalYearUsage: sum('yearUsage'),
    totalMonthAmount: sum('monthAmount'),
    totalMonthUsage: sum('monthUsage'),
  }
})

// ── Table sorting ──────────────────────────────────────────────
const sortKey = ref('')
const sortDir = ref('asc')

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

// ── Pagination ─────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = 10

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

// ── 子级表头列定义 ─────────────────────────────────────────────
const yearChildHeaders = [
  { key: 'yearAmount', label: '金额(元)',  align: 'right', sortable: true },
  { key: 'yearUsage',  label: '用量(kWh)', align: 'right', sortable: true },
]

const monthChildHeaders = [
  { key: 'monthAmount',     label: '金额(元)',     align: 'right', sortable: true },
  { key: 'monthUsage',      label: '用量(kWh)',    align: 'right', sortable: true },
  { key: 'currentBalance',  label: '本期余额(元)', align: 'right', sortable: true },
  { key: 'previousBalance', label: '上期余额(元)', align: 'right', sortable: true },
]

// ── 扁平列（数据行渲染用） ────────────────────────────────────
const tableColumns = [
  { key: 'customerName',        label: '商户名',         width: 'w-[120px]' },
  { key: 'deviceNo',            label: '表号',           width: 'w-[130px]' },
  { key: 'virtualMeterNo',      label: '虚拟表号',       width: 'w-[130px]' },
  { key: 'virtualMerchantName', label: '虚拟表商户名',   width: 'w-[120px]' },
  ...yearChildHeaders,
  ...monthChildHeaders,
]

// ── 格式化工具 ────────────────────────────────────────────────
function formatNum(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toFixed(2)
}

function getCellClass(col) {
  if (col.align === 'right') return 'text-right font-mono'
  if (col.key === 'deviceNo' || col.key === 'virtualMeterNo') return 'font-mono'
  return ''
}

function formatCell(row, col) {
  const v = row[col.key]
  if (col.align === 'right') return formatNum(v)
  if (v === null || v === undefined || v === '') return '—'
  return v
}

// ── Custom horizontal scrollbar ──────────────────────────────
const tableWrapper = ref(null)
const scrollTrack = ref(null)
const thumbLeft = ref(0)
const thumbWidth = ref(80)
const isDragging = ref(false)
let dragStartX = 0
let dragStartLeft = 0

function updateThumb() {
  const wrapper = tableWrapper.value
  if (!wrapper) return
  const ratio = wrapper.clientWidth / wrapper.scrollWidth
  thumbWidth.value = Math.max(ratio * 100, 5)
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
  tableWrapper.value.scrollLeft = percent * (tableWrapper.value.scrollWidth - tableWrapper.value.clientWidth)
}

function onMouseUp() { isDragging.value = false }
function onTableScroll() { updateThumb() }

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  updateThumb()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})

watch(rawData, () => { setTimeout(updateThumb, 50) })

// ── 导出功能 ─────────────────────────────────────────────────
function handleExport() {
  downloadXLSX(tableColumns, filteredData.value, '计费电量年报')
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

    <!-- ═══ Summary cards ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">涉及表计数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">台</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">年度计费金额</div>
          <div class="text-lg font-bold text-[#e8a838]">{{ summary.totalYearAmount.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">年度计费电量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalYearUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">当月计费金额</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalMonthAmount.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">当月计费电量</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalMonthUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">计费电量年报</h2>
        <span class="text-[12px] text-[#999]">共 {{ sortedRecords.length }} 条记录</span>
      </div>

        <div ref="tableWrapper" class="overflow-y-auto overflow-x-hidden max-h-[520px]" @scroll="onTableScroll">
          <table class="w-max min-w-full border-collapse text-left">
            <!-- ═══ 两级表头 ═══ -->
            <thead class="sticky top-0 z-10">
              <!-- 父级表头 -->
              <tr>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center sticky left-0 z-20" rowspan="2" style="min-width:60px">序号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">商户名</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">表号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">虚拟表号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">虚拟表商户名</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="2">{{ yearLabel }}</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="4">{{ monthLabel }}</th>
              </tr>
              <!-- 子级表头 -->
              <tr>
                <th
                  v-for="col in yearChildHeaders"
                  :key="'y_' + col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap select-none transition-colors"
                  :class="[col.align === 'right' ? 'text-right' : '', col.sortable ? 'cursor-pointer hover:bg-[#ebedf2]' : '']"
                  @click.stop="col.sortable ? toggleSort(col.key) : null"
                >
                  {{ col.label }}
                  <span v-if="col.sortable" class="text-[10px] ml-0.5 text-[#bbb]">
                    <span v-if="sortKey === col.key">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
                    <span v-else>⇅</span>
                  </span>
                </th>
                <th
                  v-for="col in monthChildHeaders"
                  :key="'m_' + col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap select-none transition-colors"
                  :class="[col.align === 'right' ? 'text-right' : '', col.sortable ? 'cursor-pointer hover:bg-[#ebedf2]' : '']"
                  @click.stop="col.sortable ? toggleSort(col.key) : null"
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
                :key="row._rowId"
              >
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666] text-center sticky left-0 bg-white">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <td
                  v-for="col in tableColumns"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap"
                  :class="[getCellClass(col)]"
                >{{ formatCell(row, col) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

      <!-- Custom scrollbar -->
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

      <!-- Pagination -->
      <div v-if="sortedRecords.length > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-2 text-[13px] text-[#666]">
          <span>每页 <b class="text-[#3a64ab]">{{ pageSize }}</b> 条</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[13px] text-[#666] mr-2">{{ currentPage }} / {{ totalPages || 1 }}</span>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>
