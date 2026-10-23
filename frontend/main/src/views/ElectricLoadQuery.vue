<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getElectricLoad, getElectricLoadCurve } from '@/api/report'
import { getMeterGroupList } from '@/api/meterGroup'
import { reportColumnsConfig } from '@/config/reportColumns'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'
import MonthPicker from '@/components/forms/MonthPicker.vue'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'

use([BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const PERIOD_COLORS = { 尖: '#faad14', 峰: '#1890ff', 平: '#52c41a', 谷: '#ff4d4f' }

// ── Filters ────────────────────────────────────────────────
const granularity = ref('daily')
const dateStart = ref('')
const dateEnd = ref('')
const monthStart = ref('')  // YYYY-MM for monthly mode
const groupId = ref('')
const groupOptions = ref([{ value: '', label: '全部分组' }])

function initDateRange() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  dateStart.value = `${y}-${m}-01`
  dateEnd.value = `${y}-${m}-${String(now.getDate()).padStart(2, '0')}`
}

function initMonthRange() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  monthStart.value = `${y}-${m}`
}

// 粒度切换时同步日期/月份
function onGranularityChange(val) {
  granularity.value = val
  if (val === 'monthly') {
    monthStart.value = dateStart.value ? dateStart.value.substring(0, 7) : ''
    if (!monthStart.value) initMonthRange()
  } else {
    if (monthStart.value) {
      const [y, m] = monthStart.value.split('-')
      if (y && m) {
        dateStart.value = `${y}-${m}-01`
        dateEnd.value = `${y}-${m}-${String(new Date(+y, +m, 0).getDate()).padStart(2, '0')}`
      }
    } else {
      initDateRange()
    }
  }
  currentPage.value = 1
  fetchData()
}

// 获取实际发给后端的日期参数
function effectiveDateParams() {
  if (granularity.value === 'monthly') {
    const [y, m] = (monthStart.value || '').split('-')
    if (y && m) {
      const start = `${y}-${m}-01`
      const end = `${y}-${m}-${String(new Date(+y, +m, 0).getDate()).padStart(2, '0')}`
      return { startDate: start, endDate: end }
    }
    return { startDate: null, endDate: null }
  }
  return { startDate: dateStart.value || null, endDate: dateEnd.value || null }
}

onMounted(async () => {
  initDateRange()
  initMonthRange()
  try {
    const res = await getMeterGroupList({ pageSize: 500 })
    const records = (res.data?.records || [])
    groupOptions.value = [{ value: '', label: '全部分组' }, ...records.map(g => ({ value: String(g.id), label: g.groupName || g.name || '' }))]
  } catch { /* silent */ }
  fetchData()
})

// ── Data ────────────────────────────────────────────────────
const loading = ref(false)
const loadTableData = ref([])
const curveData = ref([])

const columns = reportColumnsConfig['electric-load'] || []

async function fetchData() {
  loading.value = true
  try {
    const { startDate, endDate } = effectiveDateParams()
    const baseParams = {
      startDate,
      endDate,
      groupId: groupId.value ? Number(groupId.value) : null,
    }

    const [loadRes, curveRes] = await Promise.all([
      getElectricLoad({ ...baseParams, granularity: granularity.value }),
      getElectricLoadCurve(baseParams),
    ])

    loadTableData.value = Array.isArray(loadRes.data) ? loadRes.data : (loadRes.data?.records || [])
    curveData.value = Array.isArray(curveRes.data) ? curveRes.data : (curveRes.data?.records || [])
  } catch (e) {
    console.error('获取用电负荷数据失败', e)
    loadTableData.value = []
    curveData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { fetchData() }
function handleReset() {
  granularity.value = 'daily'
  groupId.value = ''
  initDateRange()
  initMonthRange()
  currentPage.value = 1
  fetchData()
}

// ── Summary ─────────────────────────────────────────────────
const summary = computed(() => {
  if (loadTableData.value.length === 0) return { max: 0, avg: 0, min: 0, rate: 0, total: 0, meters: 0 }
  let max = 0, totalAvg = 0, min = Infinity, totalConsumed = 0, meterCount = 0
  loadTableData.value.forEach(r => {
    const mx = Number(r.maxLoad || 0)
    const avg = Number(r.averageLoad || 0)
    const mn = Number(r.minLoad || 0)
    if (mx > max) max = mx
    if (mn < min) min = mn
    totalAvg += avg
    totalConsumed += Number(r.totalConsumed || 0)
    meterCount = Math.max(meterCount, Number(r.meterCount || 0))
  })
  const avgLoad = loadTableData.value.length > 0 ? totalAvg / loadTableData.value.length : 0
  return {
    max: max.toFixed(2),
    avg: avgLoad.toFixed(2),
    min: min === Infinity ? 0 : min.toFixed(2),
    rate: max > 0 ? ((avgLoad / max) * 100).toFixed(1) : '0.0',
    total: totalConsumed.toFixed(2),
    meters: meterCount,
  }
})

// ── Table pagination ────────────────────────────────────────
const currentPage = ref(1)
const pageSize = 10
const totalPages = computed(() => Math.max(1, Math.ceil(loadTableData.value.length / pageSize)))
const tableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return loadTableData.value.slice(start, start + pageSize)
})
function goPage(n) { if (n >= 1 && n <= totalPages.value) currentPage.value = n }
watch([dateStart, dateEnd, monthStart, groupId], () => { currentPage.value = 1 })

// ── Curve chart (aggregate by month when granularity=monthly) ──
const curveChartData = computed(() => {
  if (granularity.value !== 'monthly') return curveData.value

  const monthMap = {}
  curveData.value.forEach(d => {
    const month = (d.date || '').substring(0, 7)
    if (!month) return
    if (!monthMap[month]) {
      monthMap[month] = { date: month, totalLoad: 0, peakLoad: 0, busyLoad: 0, normalLoad: 0, idleLoad: 0, meterCount: 0 }
    }
    monthMap[month].totalLoad += Number(d.totalLoad || 0)
    monthMap[month].peakLoad += Number(d.peakLoad || 0)
    monthMap[month].busyLoad += Number(d.busyLoad || 0)
    monthMap[month].normalLoad += Number(d.normalLoad || 0)
    monthMap[month].idleLoad += Number(d.idleLoad || 0)
    monthMap[month].meterCount = Math.max(monthMap[month].meterCount, Number(d.meterCount || 0))
  })
  return Object.values(monthMap).sort((a, b) => a.date.localeCompare(b.date))
})

const curveOption = computed(() => {
  const isMonthly = granularity.value === 'monthly'
  const data = curveChartData.value
  const dates = data.map(d => d.date || '')
  const peakSeries = data.map(d => Number(d.peakLoad || 0))
  const busySeries = data.map(d => Number(d.busyLoad || 0))
  const normalSeries = data.map(d => Number(d.normalLoad || 0))
  const idleSeries = data.map(d => Number(d.idleLoad || 0))

  return {
    title: { text: isMonthly ? '月用电负荷曲线（分时段）' : '日用电负荷曲线（分时段）', left: 'center', textStyle: { fontSize: 14, fontWeight: 'bold' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: 60, right: 20, top: 50, bottom: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, rotate: dates.length > 15 ? 45 : 0 } },
    yAxis: { type: 'value', name: 'kWh', nameTextStyle: { fontSize: 11 }, axisLabel: { fontSize: 10 } },
    series: [
      { name: '尖', type: 'bar', stack: 'total', data: peakSeries, itemStyle: { color: PERIOD_COLORS['尖'] }, barGap: '5%' },
      { name: '峰', type: 'bar', stack: 'total', data: busySeries, itemStyle: { color: PERIOD_COLORS['峰'] } },
      { name: '平', type: 'bar', stack: 'total', data: normalSeries, itemStyle: { color: PERIOD_COLORS['平'] } },
      { name: '谷', type: 'bar', stack: 'total', data: idleSeries, itemStyle: { color: PERIOD_COLORS['谷'] } },
    ],
  }
})

function formatCell(v, col) {
  if (v == null || v === '' || v === '—') return '—'
  if (typeof v === 'number') return v.toFixed(2)
  if (col.key === 'loadRate') {
    const n = parseFloat(v)
    return !isNaN(n) ? n.toFixed(1) + '%' : v
  }
  return v
}

function formatMoney(n) {
  const num = Number(n)
  return isNaN(num) ? '0.00' : num.toLocaleString('en-US', { minimumFractionDigits: 2 })
}
</script>

<template>
  <div>
    <!-- ═══ Filter Bar ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <div class="flex items-center gap-1 bg-[#f5f5f5] rounded p-0.5">
          <button
            class="py-1 px-3 rounded text-[12px] border-none cursor-pointer transition-colors"
            :class="granularity === 'daily' ? 'bg-[#3a64ab] text-white' : 'bg-transparent text-[#666] hover:bg-[#e8e8e8]'"
            @click="onGranularityChange('daily')"
          >日</button>
          <button
            class="py-1 px-3 rounded text-[12px] border-none cursor-pointer transition-colors"
            :class="granularity === 'monthly' ? 'bg-[#3a64ab] text-white' : 'bg-transparent text-[#666] hover:bg-[#e8e8e8]'"
            @click="onGranularityChange('monthly')"
          >月</button>
        </div>
        <DateRangePicker v-if="granularity === 'daily'" v-model:start-value="dateStart" v-model:end-value="dateEnd" start-placeholder="开始日期" end-placeholder="结束日期" />
        <MonthPicker v-else v-model="monthStart" placeholder="选择月份" />
        <DropdownSelect v-model="groupId" :options="groupOptions" placeholder="分组" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" :disabled="loading" @click="handleSearch">{{ loading ? '加载中...' : '查询' }}</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- ═══ Summary Cards ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">最大负荷</div>
          <div class="text-lg font-bold text-[#ff4d4f]">{{ summary.max }}<span class="text-[12px] font-normal text-[#999] ml-1">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">平均负荷</div>
          <div class="text-lg font-bold text-[#1890ff]">{{ summary.avg }}<span class="text-[12px] font-normal text-[#999] ml-1">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">最小负荷</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.min }}<span class="text-[12px] font-normal text-[#999] ml-1">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">负荷率</div>
          <div class="text-lg font-bold text-[#faad14]">{{ summary.rate }}<span class="text-[12px] font-normal text-[#999] ml-1">%</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总用电量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.total }}<span class="text-[12px] font-normal text-[#999] ml-1">kWh</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">涉及表计</div>
          <div class="text-lg font-bold text-[#666]">{{ summary.meters }}<span class="text-[12px] font-normal text-[#999] ml-1">个</span></div>
        </div>
      </div>
    </div>

    <!-- ═══ Curve Chart ═══ -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <VChart v-if="curveChartData.length > 0" :option="curveOption" autoresize style="height: 380px" />
      <div v-else class="py-16 text-center text-[14px] text-[#999]">暂无负荷曲线数据</div>
    </div>

    <!-- ═══ Data Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">负荷明细</h2>
        <span class="text-[12px] text-[#999]">共 {{ loadTableData.length }} 条记录</span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th
                v-for="col in columns"
                :key="col.key"
                class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap"
                :class="col.align === 'right' ? 'text-right' : ''"
              >{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in tableData" :key="row.period || idx">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td
                v-for="col in columns"
                :key="col.key"
                class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"
                :class="col.align === 'right' ? 'text-right font-mono' : ''"
              >{{ formatCell(row[col.key], col) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="py-16 text-center text-[14px] text-[#999]">暂无数据</div>
      </div>

      <div v-if="loadTableData.length > pageSize" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(totalPages)">末页</button>
        </div>
      </div>
    </div>
  </div>
</template>
