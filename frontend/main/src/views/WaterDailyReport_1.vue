<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getWaterDailyDetail, getWaterDailySummary } from '@/api/waterReport.js'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'
import * as XLSX from 'xlsx'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

// ── Filters ──────────────────────────────────────────────────────────
const filterStartDate = ref('')
const filterEndDate = ref('')
const filterMerchantName = ref('')
const filterDeviceNo = ref('')
// ── Data ─────────────────────────────────────────────────────────────
const loading = ref(false)
const errorMsg = ref('')
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const summaryList = ref([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function formatDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function buildParams() {
  const p = { pageNum: currentPage.value, pageSize: pageSize.value }
  if (filterStartDate.value) p.startDate = filterStartDate.value
  if (filterEndDate.value) p.endDate = filterEndDate.value
  if (filterMerchantName.value.trim()) p.merchantName = filterMerchantName.value.trim()
  if (filterDeviceNo.value.trim()) p.deviceNo = filterDeviceNo.value.trim()
  return p
}

async function fetchDetail() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getWaterDailyDetail(buildParams())
    const body = res.data
    if (body && body.list) {
      tableData.value = body.list
      total.value = body.total || 0
    } else if (Array.isArray(body)) {
      tableData.value = body
      total.value = body.length
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.msg || e.message || '加载失败'
    tableData.value = []
    total.value = 0
  } finally { loading.value = false }
}

async function fetchSummary() {
  try {
    const p = {}
    if (filterStartDate.value) p.startDate = filterStartDate.value
    if (filterEndDate.value) p.endDate = filterEndDate.value
    if (filterDeviceNo.value.trim()) p.deviceNo = filterDeviceNo.value.trim()
    const res = await getWaterDailySummary(p)
    summaryList.value = Array.isArray(res.data) ? res.data : []
  } catch { summaryList.value = [] }
}

async function loadData() {
  await Promise.all([fetchDetail(), fetchSummary()])
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function resetFilters() {
  filterStartDate.value = ''
  filterEndDate.value = ''
  filterMerchantName.value = ''
  filterDeviceNo.value = ''
  handleSearch()
}

function goPage(p) {
  if (p >= 1 && p <= totalPages.value) { currentPage.value = p; fetchDetail() }
}

// ── Chart ────────────────────────────────────────────────────────────
const chartOption = computed(() => {
  const list = summaryList.value
  if (!list || list.length === 0) return {}
  const dates = list.map(i => i.statDate || '')
  const values = list.map(i => Number(i.totalUsage || 0))
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${p.axisValue}<br/>用水量：<b>${Number(p.value).toFixed(2)}</b> m³`
      },
    },
    grid: { left: '3%', right: '4%', top: 20, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category', data: dates,
      axisLabel: { color: '#666', fontSize: 11, rotate: dates.length > 15 ? 45 : 0 },
    },
    yAxis: {
      type: 'value', name: 'm³',
      axisLabel: { color: '#666', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [{
      type: 'line', data: values, smooth: true,
      symbol: 'circle', symbolSize: 5,
      lineStyle: { color: '#3a64ab', width: 2 },
      itemStyle: { color: '#3a64ab' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(58,100,171,0.15)' }, { offset: 1, color: 'rgba(58,100,171,0.01)' }] } },
    }],
  }
})

// ── Export ───────────────────────────────────────────────────────────
function handleExport() {
  if (tableData.value.length === 0) return
  const rows = tableData.value.map((r, i) => ({
    '序号': i + 1,
    '统计日期': r.statDate || '',
    '商户名称': r.merchantName || '',
    '设备编号': r.deviceNo || '',
    '虚拟表号': r.virtualMeterNo || '',
    '虚拟表商户名': r.virtualMerchantName || '',
    '日用水量(m³)': Number(r.totalUsage || 0).toFixed(2),
    '日水费(元)': Number(r.totalFee || 0).toFixed(2),
  }))
  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '用水量日报表')
  const d = new Date()
  XLSX.writeFile(wb, `用水量日报表_${formatDate(d)}.xlsx`)
}

// ── Summary ──────────────────────────────────────────────────────────
const summary = computed(() => {
  const list = summaryList.value
  if (!list.length) return { totalUsage: 0, totalFee: 0, meterCount: 0, dayCount: 0 }
  let totalUsage = 0, totalFee = 0, meterCount = 0
  list.forEach(r => {
    totalUsage += Number(r.totalUsage || 0)
    totalFee += Number(r.totalFee || 0)
    if (r.meterCount > meterCount) meterCount = r.meterCount
  })
  return {
    totalUsage,
    totalFee,
    meterCount: meterCount || list.length,
    dayCount: list.length,
  }
})

function fmt(v) {
  if (v == null || v === '') return '—'
  const n = Number(v)
  return isNaN(n) ? v : n.toFixed(2)
}

// ── Init ─────────────────────────────────────────────────────────────
onMounted(() => {
  const now = new Date()
  const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  filterStartDate.value = formatDate(weekAgo)
  filterEndDate.value = formatDate(now)
  loadData()
})
</script>

<template>
  <div>
    <!-- Filter Bar -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <DateRangePicker
          v-model:start-value="filterStartDate"
          v-model:end-value="filterEndDate"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <input v-model="filterMerchantName" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="商户名称" type="text" @keyup.enter="handleSearch" />
        <input v-model="filterDeviceNo" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="设备编号" type="text" @keyup.enter="handleSearch" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总用水量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总水费</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">活跃水表数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">个</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">统计天数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.dayCount }} <span class="text-[12px] font-normal text-[#999]">天</span></div>
        </div>
      </div>
    </div>

    <!-- Trend Chart -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">日用水量趋势</h2>
      </div>
      <div v-if="summaryList.length > 0" style="height:280px">
        <VChart :option="chartOption" autoresize class="w-full h-full" />
      </div>
      <div v-else class="flex items-center justify-center h-[280px] text-[14px] text-[#999]">暂无趋势数据</div>
    </div>

    <!-- Detail Table -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">用水量日报明细</h2>
        <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
      </div>

      <!-- Error message -->
      <p v-if="errorMsg" class="text-[13px] text-[#ff4d4f] bg-red-50 px-4 py-2.5 rounded-lg mb-3">{{ errorMsg }}</p>

      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>
        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">统计日期</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">设备编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">虚拟表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">日用水量(m³)</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">日水费(元)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in tableData" :key="`${row.meterId || ''}-${row.statDate || ''}-${idx}`" class="hover:bg-[#f5f6fb]">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.statDate || '—' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.merchantName || '—' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ row.deviceNo || '—' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.virtualMeterNo || '—' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#3a64ab]">{{ fmt(row.totalUsage) }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#52c41a]">{{ fmt(row.totalFee) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无匹配数据</div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(totalPages)">末页</button>
        </div>
      </div>
    </div>
  </div>
</template>
