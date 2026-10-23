<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue'
import { getBillingMonthlyMeterDetail } from '@/api/meterReport'
import { downloadXLSX } from '@/utils/export'

const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])

// ── 安全解析 yyyy-MM-dd 字符串为本地日期 ──
function parseLocalDate(str) {
  if (!str) return null
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

// ── 格式化日期为 YYYY-MM-DD ──
function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ── 动态生成日列（从当月1日到当前日期） ──
const dayColumns = computed(() => {
  const p = props.searchParams
  let start, end

  if (p.dateStart) {
    start = parseLocalDate(p.dateStart)
  }
  if (p.dateEnd) {
    end = parseLocalDate(p.dateEnd)
  }

  // 默认：当月1日 → 今天
  const now = new Date()
  if (!start) {
    start = new Date(now.getFullYear(), now.getMonth(), 1)
  }
  if (!end) {
    end = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  }

  if (!start) return []

  // 确保 end 不早于 start
  if (!end || end < start) end = new Date(start)

  const cols = []
  const cursor = new Date(start)
  while (cursor <= end) {
    const y = cursor.getFullYear()
    const m = String(cursor.getMonth() + 1).padStart(2, '0')
    const d = String(cursor.getDate()).padStart(2, '0')
    const key = `${m}-${d}`
    cols.push({
      key,
      label: `${y}-${m}-${d}`,
    })
    cursor.setDate(cursor.getDate() + 1)
  }
  return cols
})

// ── 调用 API ──────────────────────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    // 确定查询的日期范围：默认当月1日到今天
    const now = new Date()
    const defaultStart = formatDate(new Date(now.getFullYear(), now.getMonth(), 1))
    const defaultEnd = formatDate(new Date(now.getFullYear(), now.getMonth(), now.getDate()))

    const apiParams = {
      startDate: props.searchParams.dateStart || defaultStart,
      endDate: props.searchParams.dateEnd || defaultEnd,
      merchantId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }
    const res = await getBillingMonthlyMeterDetail(apiParams)
    if (res && res.data) {
      rawData.value = (Array.isArray(res.data) ? res.data : []).map((item, index) => ({
        _rowId: index,
        meterId: item.meterId,
        deviceType: item.deviceType || '',
        groupName: item.groupName || '',
        merchantName: item.merchantName || '',
        deviceNo: item.deviceNo || '',
        virtualMerchantName: item.virtualMerchantName || '',
        virtualMeterNo: item.virtualMeterNo || '',
        dailyKwh: item.dailyKwh || {},
        dailyAmt: item.dailyAmt || {},
        totalKwh: item.totalKwh || 0,
        totalAmt: item.totalAmt || 0,
      })).sort((a, b) => {
        const aKey = (a.deviceNo || '') + '|' + (a.merchantName || '')
        const bKey = (b.deviceNo || '') + '|' + (b.merchantName || '')
        return aKey.localeCompare(bKey, 'zh-CN')
      })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取计费电量月报表明细失败', error)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

// ── Pagination (must be before the watch that references it) ──
const currentPage = ref(1)
const pageSize = 10

watch(
  [() => props.searchParams.dateStart, () => props.searchParams.dateEnd,
   () => props.searchParams.customerId, () => props.searchParams.groupId],
  () => { currentPage.value = 1; loadData() },
  { immediate: true }
)

// ── 客户端关键词过滤 ──────────────────────────────────────────
const filteredData = computed(() => {
  let list = rawData.value
  const p = props.searchParams

  if (p.keyword && p.keyword.trim()) {
    const kw = p.keyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.merchantName && String(r.merchantName).toLowerCase().includes(kw)) ||
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

// ── Summary cards ─────────────────────────────────────────────
const summary = computed(() => {
  const recs = filteredData.value
  const totalAmt = recs.reduce((s, r) => s + (Number(r.totalAmt) || 0), 0)
  const totalKwh = recs.reduce((s, r) => s + (Number(r.totalKwh) || 0), 0)
  return { totalAmt, totalKwh, meterCount: recs.length }
})

// ── Pagination computed ──────────────────────────────────────

const totalPages = computed(() => Math.max(1, Math.ceil(filteredData.value.length / pageSize)))
const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredData.value.slice(start, start + pageSize)
})

// 重置时回到第1页
watch(() => props.tableResetKey, () => {
  currentPage.value = 1
})

function goPage(n) { if (n >= 1 && n <= totalPages.value) currentPage.value = n }

// ── 格式化工具 ────────────────────────────────────────────────
function formatVal(v) {
  if (v == null || v === '') return '—'
  const n = Number(v)
  if (!isNaN(n)) return n.toFixed(2)
  return v
}

// ── 获取单元格值 ──────────────────────────────────────────────
function getCellKwh(row, dayKey) {
  return row.dailyKwh?.[dayKey] ?? null
}
function getCellAmt(row, dayKey) {
  return row.dailyAmt?.[dayKey] ?? null
}

// ── Custom horizontal scrollbar ─────────────────────────────────
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

// ── 固定列配置 ────────────────────────────────────────────────
const fixedColumns = [
  { key: 'deviceType',         label: '设备类型',       width: 'min-w-[72px]' },
  { key: 'groupName',          label: '分组',           width: 'min-w-[90px]' },
  { key: 'merchantName',       label: '商户名',         width: 'min-w-[120px]' },
  { key: 'deviceNo',           label: '表号',           width: 'min-w-[120px]' },
  { key: 'virtualMerchantName',label: '虚拟表商户名',   width: 'min-w-[110px]' },
  { key: 'virtualMeterNo',     label: '虚拟表号',       width: 'min-w-[120px]' },
]

// ── colspan 计算：固定列(7+1序号) + 日列数×2 + 汇总2列 ──
const totalColspan = computed(() => 1 + fixedColumns.length + dayColumns.value.length * 2 + 2)

// ── 导出功能 ─────────────────────────────────────────────────
function handleExport() {
  const cols = [
    ...fixedColumns,
    ...dayColumns.value.flatMap(dc => [
      { key: '__kwh_' + dc.key, label: dc.label + ' 总电量' },
      { key: '__amt_' + dc.key, label: dc.label + ' 总费用' },
    ]),
    { key: 'totalKwh', label: '汇总总电量' },
    { key: 'totalAmt', label: '汇总总费用' },
  ]
  const data = filteredData.value.map(row => {
    const obj = { ...row }
    dayColumns.value.forEach(dc => {
      obj['__kwh_' + dc.key] = row.dailyKwh?.[dc.key] ?? 0
      obj['__amt_' + dc.key] = row.dailyAmt?.[dc.key] ?? 0
    })
    return obj
  })
  downloadXLSX(cols, data, '计费电量月报')
}

const registerExport = inject('registerExport', null)
onMounted(() => {
  if (registerExport) registerExport(handleExport)
})
</script>

<template>
  <div>
    <!-- ═══ Loading overlay ═══ -->
    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <!-- ═══ Summary cards ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">月计费金额合计</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalAmt.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">月计费电量合计</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalKwh.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">表计数量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">台</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">计费电量月报</h2>
        <span class="text-[12px] text-[#999]">共 {{ filteredData.length }} 条记录</span>
      </div>

        <div ref="tableWrapper" class="overflow-y-auto overflow-x-hidden max-h-[520px]" @scroll="onTableScroll">
          <table class="w-max min-w-full border-collapse text-left">
            <!-- ═══ 两级表头 ═══ -->
            <thead class="sticky top-0 z-10">
              <!-- 父级表头（Level 1） -->
              <tr>
                <th class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center sticky left-0 z-20" rowspan="2" style="min-width:50px">序号</th>
                <th
                  v-for="col in fixedColumns"
                  :key="col.key"
                  class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                  :class="[col.width || '']"
                  rowspan="2"
                >{{ col.label }}</th>
                <th
                  v-for="dc in dayColumns"
                  :key="'hdr_' + dc.key"
                  class="py-2 px-1 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                  colspan="2"
                >{{ dc.label }}</th>
                <th class="py-2 px-1 border-b border-r border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" colspan="2">汇总</th>
              </tr>
              <!-- 子级表头（Level 2）：日期列下的 总电量/总费用（交错排列）+ 汇总列下的 总电量/总费用 -->
              <tr>
                <template v-for="dc in dayColumns" :key="'sub_' + dc.key">
                  <th class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[11px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总电量</th>
                  <th class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[11px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总费用</th>
                </template>
                <th class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[11px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总电量</th>
                <th class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[11px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总费用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedRecords.length === 0 && !loading">
                <td :colspan="totalColspan" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
              </tr>
              <tr
                v-for="(row, idx) in paginatedRecords"
                :key="row._rowId"
              >
                <!-- 序号 -->
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666] sticky left-0 bg-white"
                >{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <!-- 固定列 -->
                <td
                  v-for="col in fixedColumns"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap"
                  :class="[
                    col.key === 'deviceNo' || col.key === 'virtualMeterNo' ? 'font-mono' : '',
                  ]"
                >{{ row[col.key] || '—' }}</td>
                <!-- 每日列：总电量 + 总费用（交错排列，与父级表头的 colspan=2 对齐） -->
                <template v-for="dc in dayColumns" :key="'day_' + dc.key">
                  <td
                    class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono"
                  >{{ formatVal(getCellKwh(row, dc.key)) }}</td>
                  <td
                    class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono"
                  >{{ formatVal(getCellAmt(row, dc.key)) }}</td>
                </template>
                <!-- 汇总列 -->
                <td class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono"
                >{{ formatVal(row.totalKwh) }}</td>
                <td class="py-3.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono"
                >{{ formatVal(row.totalAmt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

      <!-- Custom scrollbar -->
      <div
        v-show="filteredData.length > 0"
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
      <div v-if="filteredData.length > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
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
