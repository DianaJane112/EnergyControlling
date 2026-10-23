<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue'
import { getBillingMeterSummary } from '@/api/meterReport'
import { downloadXLSX } from '@/utils/export'

const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])

// ── 调用 API ──────────────────────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    const apiParams = {
      startDate: props.searchParams.dateStart || null,
      endDate: props.searchParams.dateEnd || null,
      merchantId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }
    const res = await getBillingMeterSummary(apiParams)
    if (res && res.data) {
      rawData.value = (Array.isArray(res.data) ? res.data : [])
        .map((item, index) => ({
          id: index,
          customerName: item.customerName || '',
          averagePrice: item.averagePrice || 0,
          deviceNo: item.deviceNo || '',
          virtualMeterNo: item.virtualMeterNo || '',
          virtualMerchantName: item.virtualMerchantName || '',
          totalChargeAmount: item.totalChargeAmount || 0,
          totalConsumed: item.totalConsumed || 0,
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
    console.error('获取计费电量汇总表失败', error)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

watch(
  [() => props.searchParams.dateStart, () => props.searchParams.dateEnd,
   () => props.searchParams.customerId, () => props.searchParams.groupId],
  () => { loadData() },
  { immediate: true }
)

// ── 客户端过滤 ────────────────────────────────────────────────
const tableData = computed(() => {
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

// ── Summary cards ─────────────────────────────────────────────
const summary = computed(() => {
  const recs = tableData.value
  const totalAmt = recs.reduce((s, r) => s + (Number(r.totalChargeAmount) || 0), 0)
  const totalKwh = recs.reduce((s, r) => s + (Number(r.totalConsumed) || 0), 0)
  const avgPrice = totalKwh > 0 ? totalAmt / totalKwh : 0
  return { totalAmt, totalKwh, avgPrice, meterCount: recs.length }
})

// ── Pagination ─────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = 10
const totalPages = computed(() => Math.max(1, Math.ceil(tableData.value.length / pageSize)))
const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})

watch(() => ({ ...props.searchParams }), () => {
  currentPage.value = 1
})

// 重置时回到第1页
watch(() => props.tableResetKey, () => {
  currentPage.value = 1
})

function goPage(n) { if (n >= 1 && n <= totalPages.value) currentPage.value = n }

const tableColumns = [
  { key: 'customerName',      label: '商户名',           width: 'w-[120px]' },
  { key: 'deviceNo',          label: '表号',             width: 'w-[130px]' },
  { key: 'totalChargeAmount', label: '计费金额(元)',     align: 'right' },
  { key: 'totalConsumed',     label: '计费电量(kWh)',    align: 'right' },
  { key: 'averagePrice',      label: '计费均价(元/度)',   align: 'right' },
]

function formatVal(v) {
  if (v == null || v === '') return '—'
  if (typeof v === 'number') return v.toFixed(2)
  const n = Number(v)
  if (!isNaN(n)) return n.toFixed(2)
  return v
}

// ── Custom scrollbar ───────────────────────────────────────────
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
  downloadXLSX(tableColumns, tableData.value, '计费电量汇总')
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

    <!-- ═══ Indicator cards ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总计费金额</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalAmt.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总计费电量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalKwh.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">平均电价</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.avgPrice.toFixed(4) }} <span class="text-[12px] font-normal text-[#999]">元/度</span></div>
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
        <h2 class="text-[15px] font-bold">计费电量汇总</h2>
        <span class="text-[12px] text-[#999]">共 {{ tableData.length }} 条记录</span>
      </div>

        <div ref="tableWrapper" class="overflow-y-auto overflow-x-hidden max-h-[520px]" @scroll="onTableScroll">
          <table class="w-max min-w-full border-collapse text-left">
            <thead class="sticky top-0 z-10">
              <tr>
                <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky left-0 z-20 w-[60px]">序号</th>
                <th
                  v-for="col in tableColumns"
                  :key="col.key"
                  class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap"
                  :class="[col.width || '', col.align === 'right' ? 'text-right' : '']"
                >{{ col.label }}</th>
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
                    col.key === 'deviceNo' || col.key === 'virtualMeterNo' ? 'font-mono' : '',
                  ]"
                >{{ formatVal(row[col.key]) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

      <!-- Pagination -->
      <div v-if="tableData.length > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-2 text-[13px] text-[#666]"><span>每页 <b class="text-[#3a64ab]">{{ pageSize }}</b> 条</span></div>
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
