<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue'
import { getMonthlyElecSummary } from '@/api/report'
import { downloadXLSX } from '@/utils/export'

const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])

// ── 调用真实 API ──────────────────────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    const apiParams = {
      statMonth: props.searchParams.dateStart
        ? props.searchParams.dateStart.substring(0, 7)
        : null,
      customerId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }
    const res = await getMonthlyElecSummary(apiParams)
    if (res && res.data) {
      rawData.value = (Array.isArray(res.data) ? res.data : [])
        .map((item, index) => ({
          ...item,
          _rowId: index,
        }))
        .sort((a, b) => {
          const aKey = (a.deviceNo || '') + '|' + (a.customerName || '')
          const bKey = (b.deviceNo || '') + '|' + (b.customerName || '')
          return aKey.localeCompare(bKey, 'zh-CN')
        })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取月度用电量汇总明细失败', error)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.searchParams, () => { loadData() }, { deep: true, immediate: true })

// ── 客户端关键词过滤 ──────────────────────────────────────────────
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

// ── Summary cards ──────────────────────────────────────────────────
const summary = computed(() => {
  const recs = filteredData.value
  const sum = (key) => recs.reduce((s, r) => s + (Number(r[key]) || 0), 0)
  return {
    meterCount: recs.length,
    totalForwardUsage: sum('forwardUsage'),
    totalReverseUsage: sum('reverseUsage'),
    totalEnergy: sum('totalEnergy'),
  }
})

// ── Table sorting ──────────────────────────────────────────────────
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

// ── Pagination ─────────────────────────────────────────────────────
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

// ── 格式化工具 ────────────────────────────────────────────────────
function formatNum(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toFixed(2)
}

function formatTime(v) {
  if (!v) return '—'
  if (typeof v === 'string') {
    // 后端可能返回 "2026-06-01T12:00:00" 格式
    const t = v.replace('T', ' ').substring(0, 19)
    return t || '—'
  }
  return String(v)
}

function formatRatio(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (isNaN(n)) return String(v)
  return n.toFixed(1) + '%'
}

// ── 子级表头列定义（正向有功5列 + 反向有功5列） ─────────────────
const forwardChildHeaders = [
  { key: 'forwardStartReading', label: '上期示数', align: 'right' },
  { key: 'forwardStartTime',    label: '上期时间', align: 'right' },
  { key: 'forwardEndReading',   label: '本期示数', align: 'right' },
  { key: 'forwardEndTime',      label: '本期时间', align: 'right' },
  { key: 'forwardUsage',        label: '用量',     align: 'right', sortable: true },
]

const reverseChildHeaders = [
  { key: 'reverseStartReading', label: '上期示数', align: 'right' },
  { key: 'reverseStartTime',    label: '上期时间', align: 'right' },
  { key: 'reverseEndReading',   label: '本期示数', align: 'right' },
  { key: 'reverseEndTime',      label: '本期时间', align: 'right' },
  { key: 'reverseUsage',        label: '用量',     align: 'right', sortable: true },
]

// ── 数据行渲染用的完整扁平列 ─────────────────────────────────────
const tableColumns = [
  { key: 'groupName',           label: '分组',           width: 'w-[90px]' },
  { key: 'deviceNo',            label: '表号',           width: 'w-[130px]' },
  { key: 'customerName',        label: '商户名',         width: 'w-[120px]' },
  { key: 'virtualMeterNo',      label: '虚拟表号',       width: 'w-[130px]' },
  { key: 'virtualMerchantName', label: '虚拟表商户名',   width: 'w-[120px]' },
  { key: 'meterType',           label: '表计类型',       width: 'w-[80px]' },
  { key: 'statMonth',           label: '月度',           width: 'w-[90px]', sortable: true },
  ...forwardChildHeaders,
  ...reverseChildHeaders,
  { key: 'reverseRatio',        label: '反向占比(%)',    align: 'right', sortable: true },
  { key: 'totalStartReading',   label: '总起示数',       align: 'right' },
  { key: 'totalEndReading',     label: '总止示数',       align: 'right' },
  { key: 'totalEnergy',         label: '总电量',         align: 'right', sortable: true },
  { key: 'previousBalance',     label: '上期余额',       align: 'right', sortable: true },
  { key: 'currentBalance',      label: '本期余额',       align: 'right', sortable: true },
  { key: 'dataUpdateTime',      label: '数据更新时间' },
]

function getCellClass(col) {
  if (col.align === 'right') return 'text-right font-mono'
  if (['deviceNo', 'virtualMeterNo'].includes(col.key)) return 'font-mono'
  return ''
}

function formatCell(row, col) {
  const v = row[col.key]
  // 时间字段
  if (col.key.endsWith('Time')) return formatTime(v)
  // 反向占比
  if (col.key === 'reverseRatio') return formatRatio(v)
  // 数值字段
  if (col.align === 'right') return formatNum(v)
  // 其他
  if (v === null || v === undefined || v === '') return '—'
  return v
}

// ── Custom horizontal scrollbar ──────────────────────────────────────
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
  thumbWidth.value = Math.max(ratio * 100, 8)
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
const exportColumns = tableColumns

function handleExport() {
  downloadXLSX(exportColumns, filteredData.value, '月度用电量汇总')
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
          <div class="text-[12px] text-[#666] mb-1">正向有功总用量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalForwardUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">反向有功总用量</div>
          <div class="text-lg font-bold text-[#e8a838]">{{ summary.totalReverseUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总电量</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalEnergy.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">月度用电量汇总</h2>
        <span class="text-[12px] text-[#999]">共 {{ sortedRecords.length }} 条记录</span>
      </div>

        <div ref="tableWrapper" class="overflow-y-auto overflow-x-hidden max-h-[520px]" @scroll="onTableScroll">
          <table class="w-max min-w-full border-collapse text-left">
            <!-- ═══ 两级表头 ═══ -->
            <thead class="sticky top-0 z-10">
              <!-- 父级表头 -->
              <tr>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center sticky left-0 z-20" rowspan="2" style="min-width:60px">序号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">分组</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">表号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">商户名</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">虚拟表号</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">虚拟表商户名</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">表计类型</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">月度</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="5">正向有功电量</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="5">反向有功电量</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">反向占比(%)</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="2">总起止示数</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">总电量</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">上期余额</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">本期余额</th>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" rowspan="2">数据更新时间</th>
              </tr>
              <!-- 子级表头 -->
              <tr>
                <!-- 正向有功子列 -->
                <th
                  v-for="col in forwardChildHeaders"
                  :key="'fw_'+col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap select-none transition-colors"
                  :class="[
                    col.align === 'right' ? 'text-right' : '',
                    col.sortable ? 'cursor-pointer hover:bg-[#ebedf2]' : '',
                  ]"
                  @click.stop="col.sortable ? toggleSort(col.key) : null"
                >
                  {{ col.label }}
                  <span v-if="col.sortable" class="text-[10px] ml-0.5 text-[#bbb]">
                    <span v-if="sortKey === col.key">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
                    <span v-else>⇅</span>
                  </span>
                </th>
                <!-- 反向有功子列 -->
                <th
                  v-for="col in reverseChildHeaders"
                  :key="'rv_'+col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap select-none transition-colors"
                  :class="[
                    col.align === 'right' ? 'text-right' : '',
                    col.sortable ? 'cursor-pointer hover:bg-[#ebedf2]' : '',
                  ]"
                  @click.stop="col.sortable ? toggleSort(col.key) : null"
                >
                  {{ col.label }}
                  <span v-if="col.sortable" class="text-[10px] ml-0.5 text-[#bbb]">
                    <span v-if="sortKey === col.key">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
                    <span v-else>⇅</span>
                  </span>
                </th>
                <!-- 总起止示数子列 -->
                <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总起示数</th>
                <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总止示数</th>
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
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666] sticky left-0 bg-white">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <td
                  v-for="col in tableColumns"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"
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
