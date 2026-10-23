<script setup>
import { ref, computed, watch, inject, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 引入真实 API
import { getTimeOfUseDailyDetail, getTimeOfUseDaily } from '@/api/report'
import { downloadXLSX } from '@/utils/export'

use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// 接收父组件 ReportCenter 传过来的搜索参数
const props = defineProps({
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])       // 明细数据（per meter × date）
const chartRawData = ref([])  // 图表数据（按日聚合）

// ── 加载明细数据（表格用） ──
const loadDetailData = async () => {
  loading.value = true
  try {
    const apiParams = {
      startDate: props.searchParams.dateStart || null,
      endDate: props.searchParams.dateEnd || null,
      customerId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }

    const res = await getTimeOfUseDailyDetail(apiParams)

    if (res && res.data) {
      rawData.value = res.data.map((item, index) => ({
        id: index,
        customerCode: item.customerCode || '',
        deviceNo: item.deviceNo || '',
        date: item.reportDate || '',
        // 总用量
        totalKwh: item.totalConsumed || 0,
        totalAmt: item.totalCharge || 0,
        // 尖
        peakKwh: item.peakConsumed || 0,
        peakAmt: item.peakCharge || 0,
        // 峰
        highKwh: item.busyConsumed || 0,
        highAmt: item.busyCharge || 0,
        // 平
        flatKwh: item.normalConsumed || 0,
        flatAmt: item.normalCharge || 0,
        // 谷
        valleyKwh: item.idleConsumed || 0,
        valleyAmt: item.idleCharge || 0,
      })).sort((a, b) => {
        const aKey = (a.deviceNo || '') + '|' + (a.date || '')
        const bKey = (b.deviceNo || '') + '|' + (b.date || '')
        return aKey.localeCompare(bKey, 'zh-CN')
      })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取复费率日报表明细失败', error)
  } finally {
    loading.value = false
  }
}

// ── 加载聚合数据（图表用） ──
const loadChartData = async () => {
  try {
    const apiParams = {
      startDate: props.searchParams.dateStart || null,
      endDate: props.searchParams.dateEnd || null,
      customerId: props.searchParams.customerId || null,
      groupId: props.searchParams.groupId || null,
    }

    const res = await getTimeOfUseDaily(apiParams)

    if (res && res.data) {
      chartRawData.value = res.data.map(item => ({
        date: item.reportDate || item.period || '',
        totalKwh: item.totalConsumed || 0,
        totalAmt: item.totalCharge || 0,
        peakKwh: item.peakConsumed || 0,
        peakAmt: item.peakCharge || 0,
        highKwh: item.busyConsumed || 0,
        highAmt: item.busyCharge || 0,
        flatKwh: item.normalConsumed || 0,
        flatAmt: item.normalCharge || 0,
        valleyKwh: item.idleConsumed || 0,
        valleyAmt: item.idleCharge || 0,
      }))
    } else {
      chartRawData.value = []
    }
  } catch (error) {
    console.error('获取复费率日报表聚合数据失败', error)
  }
}

// 监听参数变化，自动触发请求
watch(
  () => props.searchParams,
  () => {
    loadDetailData()
    loadChartData()
  },
  { deep: true, immediate: true }
)

// ── Chart mode & Icons ──────────────────────────────────────────────
const chartMode = ref('usage')
const chartType = ref('line')

const modeOptions = [
  { value: 'usage', label: '总用量' },
  { value: 'cost', label: '总费用' },
]

const chartTypeIcons = [
  { type: 'line', label: '折线图', icon: '📈' },
  { type: 'bar', label: '柱状图', icon: '📊' },
  { type: 'stack', label: '堆叠图', icon: '📉' },
]

// ── Statistics cards ────────────────────────────────────────────────
const summaryCards = computed(() => {
  const recs = filteredData.value
  const sum = (key) => recs.reduce((s, r) => s + (r[key] || 0), 0)

  return {
    usage: [
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

// ── Chart data ───────────────────────────────────────────────────────
const chartData = computed(() => {
  const recs = chartRawData.value.length > 0 ? chartRawData.value : rawData.value
  const dates = [...new Set(recs.map(r => r.date))].sort()

  const seriesKeys = chartMode.value === 'usage'
    ? ['totalKwh', 'peakKwh', 'highKwh', 'flatKwh', 'valleyKwh']
    : ['totalAmt', 'peakAmt', 'highAmt', 'flatAmt', 'valleyAmt']

  const seriesNames = chartMode.value === 'usage'
    ? ['总用量', '尖用量', '峰用量', '平用量', '谷用量']
    : ['总费用', '尖费用', '峰费用', '平费用', '谷费用']

  const colors = ['#1890ff', '#f5222d', '#fa8c16', '#52c41a', '#722ed1']

  const series = seriesKeys.map((key, i) => ({
    name: seriesNames[i],
    type: chartType.value === 'stack' ? 'bar' : chartType.value,
    stack: chartType.value === 'stack' ? 'total' : undefined,
    data: dates.map(d => {
      const dayRecs = recs.filter(r => r.date === d)
      return dayRecs.reduce((s, r) => s + (r[key] || 0), 0)
    }),
    itemStyle: { color: colors[i] },
    smooth: chartType.value === 'line',
    symbol: chartType.value === 'line' ? 'circle' : undefined,
    symbolSize: 6,
  }))

  return { dates, series, colors, seriesNames }
})

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e8e8e8', textStyle: { color: '#333', fontSize: 12 } },
  legend: { data: chartData.value.seriesNames, bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12, color: '#666' } },
  grid: { left: 10, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: chartData.value.dates, axisLine: { lineStyle: { color: '#e8e8e8' } }, axisTick: { show: false }, axisLabel: { color: '#999', fontSize: 11 }, splitLine: { show: false } },
  yAxis: { type: 'value', name: chartMode.value === 'usage' ? 'kWh' : '¥', nameTextStyle: { color: '#999', fontSize: 11 }, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#999', fontSize: 11 }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
  series: chartData.value.series,
}))

// ── 客户端关键词过滤 ──────────────────────────────────────────
const filteredData = computed(() => {
  let list = rawData.value
  const p = props.searchParams

  if (p.keyword && p.keyword.trim()) {
    const kw = p.keyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.customerCode && String(r.customerCode).toLowerCase().includes(kw)) ||
      (r.deviceNo && String(r.deviceNo).toLowerCase().includes(kw))
    )
  }
  return list
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

// ── 二级表头结构 ─────────────────────────────────────────────────────
// 父级表头（colspan 合并）
const parentHeaders = [
  { label: '序号', colspan: 1, rowspan: 2 },
  { label: '商户号', colspan: 1, rowspan: 2 },
  { label: '表号', colspan: 1, rowspan: 2 },
  { label: '日期', colspan: 1, rowspan: 2 },
  { label: '总用量', colspan: 2, rowspan: 1 },
  { label: '尖用量', colspan: 2, rowspan: 1 },
  { label: '峰用量', colspan: 2, rowspan: 1 },
  { label: '平用量', colspan: 2, rowspan: 1 },
  { label: '谷用量', colspan: 2, rowspan: 1 },
]

// 子级表头（在父级下面）
const childHeaders = [
  { label: '总电量(kWh)', key: 'totalKwh', sortable: true, align: 'right' },
  { label: '总电费(¥)', key: 'totalAmt', sortable: true, align: 'right' },
  { label: '尖电量(kWh)', key: 'peakKwh', align: 'right' },
  { label: '尖电费(¥)', key: 'peakAmt', align: 'right' },
  { label: '峰电量(kWh)', key: 'highKwh', align: 'right' },
  { label: '峰电费(¥)', key: 'highAmt', align: 'right' },
  { label: '平电量(kWh)', key: 'flatKwh', align: 'right' },
  { label: '平电费(¥)', key: 'flatAmt', align: 'right' },
  { label: '谷电量(kWh)', key: 'valleyKwh', align: 'right' },
  { label: '谷电费(¥)', key: 'valleyAmt', align: 'right' },
]

// 固定前4列 + 子级表头（供数据行渲染使用）
const allColumns = [
  { key: 'rowIndex', label: '序号', width: 'w-[60px]', fixed: true },
  { key: 'customerCode', label: '商户号', width: 'w-[100px]', fixed: true },
  { key: 'deviceNo', label: '表号', width: 'w-[130px]', fixed: true },
  { key: 'date', label: '日期', width: 'w-[100px]', sortable: true, fixed: true },
  ...childHeaders,
]

function formatVal(v) {
  if (typeof v === 'number') return v.toFixed(2)
  return v
}

// ── 导出功能 ─────────────────────────────────────────────────
const exportColumns = [
  { key: 'customerCode', label: '商户号' },
  { key: 'deviceNo', label: '表号' },
  { key: 'date', label: '日期' },
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
  downloadXLSX(exportColumns, filteredData.value, '复费率计费电量日报表明细')
}

const registerExport = inject('registerExport', null)
onMounted(() => {
  if (registerExport) registerExport(handleExport)
})
</script>

<template>
  <div>
    <!-- ═══ Statistics Cards ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex gap-4 overflow-x-auto pb-2">
        <div
          v-for="card in summaryCards.usage"
          :key="card.label"
          class="flex-shrink-0 p-3 bg-[#f5f6fb] rounded text-center"
          style="min-width: 130px;"
        >
          <div class="text-[12px] text-[#666] mb-1">{{ card.label }}</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ card.value.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">{{ card.unit }}</span></div>
        </div>
        <div
          v-for="card in summaryCards.cost"
          :key="card.label"
          class="flex-shrink-0 p-3 bg-[#f5f6fb] rounded text-center"
          style="min-width: 130px;"
        >
          <div class="text-[12px] text-[#666] mb-1">{{ card.label }}</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ card.value.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">{{ card.unit }}</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ Chart Area ═══ -->
    <div class="bg-white rounded-lg border border-[#e8e8e8] p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <!-- Chart controls -->
      <div class="flex justify-between items-center mb-4">
        <div class="flex items-center gap-2">
          <button
            v-for="opt in modeOptions"
            :key="opt.value"
            class="py-1.5 px-5 rounded text-[13px] border cursor-pointer transition-colors"
            :class="chartMode === opt.value ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
            @click="chartMode = opt.value"
          >{{ opt.label }}</button>
        </div>
        <div class="flex items-center gap-1.5">
          <button
            v-for="icon in chartTypeIcons"
            :key="icon.type"
            class="w-8 h-8 flex items-center justify-center rounded text-[14px] border cursor-pointer transition-colors"
            :class="chartType === icon.type ? 'bg-[#eef3fa] border-[#3a64ab]' : 'bg-white border-[#d9d9d9] hover:border-[#3a64ab]'"
            :title="icon.label"
            @click="chartType = icon.type"
          >{{ icon.icon }}</button>
        </div>
      </div>

      <!-- Chart -->
      <div class="w-full" style="height: 340px;">
        <VChart :option="chartOption" autoresize style="width: 100%; height: 100%;" />
      </div>
    </div>

    <!-- ═══ Detail Table ═══ -->
    <div class="bg-white rounded-lg border border-[#e8e8e8] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="p-5 pb-0 flex justify-between items-center">
        <h3 class="text-[15px] font-bold text-[#333]">明细数据</h3>
        <span class="text-[12px] text-[#999]">共 {{ sortedRecords.length }} 条记录</span>
      </div>

      <div class="p-5 pt-3 overflow-x-auto">
        <table class="w-max min-w-full border-collapse text-left">
          <!-- ═══ 二级表头 ═══ -->
          <thead>
            <!-- 父级表头：总用量 / 尖用量 / 峰用量 / 平用量 / 谷用量 -->
            <tr>
              <th
                class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
                style="min-width: 60px;"
              >序号</th>
              <th
                class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
              >商户号</th>
              <th
                class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
              >表号</th>
              <th
                class="py-2 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center"
              >日期</th>
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
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap"></th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap"></th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap"></th>
              <th
                class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap cursor-pointer select-none hover:bg-[#ebedf2] transition-colors"
                @click="toggleSort('date')"
              >
                日期
                <span class="text-[10px] ml-0.5 text-[#bbb]">
                  <span v-if="sortKey === 'date'">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
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
              <td :colspan="allColumns.length" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(row, idx) in paginatedRecords"
              :key="row.id"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666] text-center">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ row.customerCode }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.deviceNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ row.date }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.totalKwh) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.totalAmt) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.peakKwh) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.peakAmt) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.highKwh) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.highAmt) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.flatKwh) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.flatAmt) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.valleyKwh) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatVal(row.valleyAmt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="sortedRecords.length > 0" class="flex justify-between items-center px-5 pb-5 pt-0">
        <div class="flex items-center gap-2 text-[13px] text-[#666]">
          <span>每页</span>
          <span class="text-[#3a64ab]">{{ pageSize }}</span>
          <span>条</span>
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
