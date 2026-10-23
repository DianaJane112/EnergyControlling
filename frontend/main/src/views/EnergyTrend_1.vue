<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getElectricLoadCurve } from '@/api/report'
import { getMeterGroupList } from '@/api/meterGroup'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'

use([BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const PERIOD_COLORS = { '尖': '#faad14', '峰': '#1890ff', '平': '#52c41a', '谷': '#ff4d4f' }
const PERIODS = ['尖', '峰', '平', '谷']

// ═══ Filter State ══════════════════════════════════════════════
const startDate = ref('')
const endDate = ref('')
const groupId = ref('')
const groupOptions = ref([{ value: '', label: '全部分组' }])

onMounted(async () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  startDate.value = `${y}-${m}-01`
  endDate.value = `${y}-${m}-${String(now.getDate()).padStart(2, '0')}`
  try {
    const res = await getMeterGroupList({ pageSize: 500 })
    const records = (res.data?.records || [])
    groupOptions.value = [{ value: '', label: '全部分组' }, ...records.map(g => ({ value: String(g.id), label: g.groupName || g.name || '' }))]
  } catch { /* silent */ }
  fetchData()
})

// ═══ Data ══════════════════════════════════════════════════════
const loading = ref(false)
const rawData = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getElectricLoadCurve({
      startDate: startDate.value || null,
      endDate: endDate.value || null,
      groupId: groupId.value ? Number(groupId.value) : null,
    })
    rawData.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
  } catch (e) {
    console.error('获取用能趋势数据失败', e)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { fetchData() }
function handleReset() {
  groupId.value = ''
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  startDate.value = `${y}-${m}-01`
  endDate.value = `${y}-${m}-${String(now.getDate()).padStart(2, '0')}`
  fetchData()
}

// ═══ Computed: normalize API data for charts ══════════════════
const chartData = computed(() => rawData.value.map(d => ({
  date: d.date || '',
  tip: Number(d.peakLoad || 0),
  peak: Number(d.busyLoad || 0),
  flat: Number(d.normalLoad || 0),
  valley: Number(d.idleLoad || 0),
  total: Number(d.peakLoad || 0) + Number(d.busyLoad || 0) + Number(d.normalLoad || 0) + Number(d.idleLoad || 0),
  meterCount: d.meterCount || 0,
})))

const periodTotals = computed(() => {
  const totals = { '尖': 0, '峰': 0, '平': 0, '谷': 0, total: 0 }
  for (const d of chartData.value) {
    totals['尖'] += d.tip
    totals['峰'] += d.peak
    totals['平'] += d.flat
    totals['谷'] += d.valley
  }
  totals.total = totals['尖'] + totals['峰'] + totals['平'] + totals['谷']
  return totals
})

// ═══ Bar Chart ═════════════════════════════════════════════════
const barOption = computed(() => {
  const days = chartData.value.map(d => d.date)
  return {
    title: { text: '每日用电量（分时段）', left: 'center', textStyle: { fontSize: 14, fontWeight: 'bold' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: 60, right: 20, top: 50, bottom: 40 },
    xAxis: { type: 'category', data: days, axisLabel: { fontSize: 10, rotate: days.length > 15 ? 45 : 0 } },
    yAxis: { type: 'value', name: 'kWh', nameTextStyle: { fontSize: 11 }, axisLabel: { fontSize: 10 } },
    series: [
      { name: '尖', type: 'bar', stack: 'total', data: chartData.value.map(d => d.tip), itemStyle: { color: PERIOD_COLORS['尖'] }, barGap: '5%' },
      { name: '峰', type: 'bar', stack: 'total', data: chartData.value.map(d => d.peak), itemStyle: { color: PERIOD_COLORS['峰'] } },
      { name: '平', type: 'bar', stack: 'total', data: chartData.value.map(d => d.flat), itemStyle: { color: PERIOD_COLORS['平'] } },
      { name: '谷', type: 'bar', stack: 'total', data: chartData.value.map(d => d.valley), itemStyle: { color: PERIOD_COLORS['谷'] } },
    ],
  }
})

// ═══ Pie Chart ═════════════════════════════════════════════════
const pieOption = computed(() => {
  const totals = periodTotals.value
  const data = PERIODS.filter(p => totals[p] > 0).map(p => ({
    name: p + '期',
    value: +totals[p].toFixed(2),
    itemStyle: { color: PERIOD_COLORS[p] },
  }))
  return {
    title: { text: '能耗占比', left: 'center', textStyle: { fontSize: 14, fontWeight: 'bold' } },
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}: ${p.value} kWh (${p.percent}%)` },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '48%'], data,
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
    }],
    grid: { left: 0, right: 0, top: 45, bottom: 35 },
  }
})

// ═══ Table ═════════════════════════════════════════════════════
const tablePage = ref(1)
const tablePageSize = 12
const pagedTableData = computed(() => {
  const start = (tablePage.value - 1) * tablePageSize
  return chartData.value.slice(start, start + tablePageSize)
})
const tableTotalPages = computed(() => Math.max(1, Math.ceil(chartData.value.length / tablePageSize)))
function goTablePage(n) { if (n >= 1 && n <= tableTotalPages.value) tablePage.value = n }

const tableSummary = computed(() => {
  const totals = periodTotals.value
  const peakTotal = totals['峰']
  const valleyTotal = totals['谷']
  return {
    total: +totals.total.toFixed(2),
    tip: +totals['尖'].toFixed(2),
    peak: +peakTotal.toFixed(2),
    flat: +totals['平'].toFixed(2),
    valley: +valleyTotal.toFixed(2),
    ratio: peakTotal > 0 ? +((valleyTotal / peakTotal) * 100).toFixed(2) : 0,
  }
})
</script>

<template>
  <div>
    <!-- ═══ Filter Bar ═══ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <DateRangePicker v-model:start-value="startDate" v-model:end-value="endDate" start-placeholder="开始日期" end-placeholder="结束日期" />
        <DropdownSelect v-model="groupId" :options="groupOptions" placeholder="分组" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" :disabled="loading" @click="handleSearch">{{ loading ? '加载中...' : '查询' }}</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleReset">重置</button>
        <div class="flex-1"></div>
        <span class="text-[12px] text-[#999]">共 {{ rawData.length }} 天数据</span>
      </div>
    </div>

    <!-- ═══ Charts Row ═══ -->
    <div class="flex gap-4 mb-4">
      <div class="flex-1 bg-white rounded p-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
        <VChart v-if="chartData.length > 0" :option="barOption" autoresize style="height: 400px" />
        <div v-else class="py-20 text-center text-[14px] text-[#999]">暂无趋势数据</div>
      </div>
      <div class="w-[340px] shrink-0 bg-white rounded p-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
        <VChart v-if="periodTotals.total > 0" :option="pieOption" autoresize style="height: 400px" />
        <div v-else class="py-20 text-center text-[14px] text-[#999]">暂无占比数据</div>
      </div>
    </div>

    <!-- ═══ Detail Table ═══ -->
    <div class="bg-white rounded shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex items-center justify-between px-4 py-3 border-b border-[#f0f0f0]">
        <span class="text-[14px] font-semibold text-[#333]">能耗示值明细</span>
        <span class="text-[12px] text-[#999]">单位：kWh</span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-[13px]" v-if="pagedTableData.length > 0">
          <thead>
            <tr class="bg-[#fafafa]">
              <th class="px-3 py-2 text-left text-[#666] font-medium border-b border-[#f0f0f0]">日期</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">总计电量</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">尖期电量</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">峰期电量</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">平期电量</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">谷期电量</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">峰谷比</th>
              <th class="px-3 py-2 text-right text-[#666] font-medium border-b border-[#f0f0f0]">表计数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedTableData" :key="row.date" class="hover:bg-[#fafafa] transition-colors">
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-[#333]">{{ row.date }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#333] font-medium">{{ row.total.toFixed(2) }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right" :class="row.tip === 0 ? 'text-[#ccc]' : 'text-[#faad14]'">{{ row.tip.toFixed(2) }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#1890ff]">{{ row.peak.toFixed(2) }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#52c41a]">{{ row.flat.toFixed(2) }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#ff4d4f]">{{ row.valley.toFixed(2) }}</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#666]">{{ row.peak > 0 ? ((row.valley / row.peak) * 100).toFixed(2) : '0.00' }}%</td>
              <td class="px-3 py-2 border-b border-[#f5f5f5] text-right text-[#666]">{{ row.meterCount }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="bg-[#fafafa] font-medium">
              <td class="px-3 py-2 text-[#333]">合计</td>
              <td class="px-3 py-2 text-right text-[#333]">{{ tableSummary.total }}</td>
              <td class="px-3 py-2 text-right" :class="tableSummary.tip === 0 ? 'text-[#ccc]' : 'text-[#faad14]'">{{ tableSummary.tip }}</td>
              <td class="px-3 py-2 text-right text-[#1890ff]">{{ tableSummary.peak }}</td>
              <td class="px-3 py-2 text-right text-[#52c41a]">{{ tableSummary.flat }}</td>
              <td class="px-3 py-2 text-right text-[#ff4d4f]">{{ tableSummary.valley }}</td>
              <td class="px-3 py-2 text-right text-[#666]">{{ tableSummary.ratio }}%</td>
              <td class="px-3 py-2 text-right text-[#666]">--</td>
            </tr>
          </tfoot>
        </table>
        <div v-else class="py-16 text-center text-[14px] text-[#999]">暂无明细数据</div>
      </div>

      <!-- table pagination -->
      <div v-if="chartData.length > tablePageSize" class="flex justify-between items-center px-4 py-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">第 {{ tablePage }} / {{ tableTotalPages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="tablePage <= 1" @click="goTablePage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="tablePage <= 1" @click="goTablePage(tablePage - 1)">上一页</button>
          <template v-for="p in tableTotalPages" :key="p">
            <button v-if="tableTotalPages <= 7 || p === 1 || p === tableTotalPages || (p >= tablePage - 1 && p <= tablePage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer" :class="tablePage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goTablePage(p)">{{ p }}</button>
            <span v-else-if="p === tablePage - 2 || p === tablePage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="tablePage >= tableTotalPages" @click="goTablePage(tablePage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="tablePage >= tableTotalPages" @click="goTablePage(tableTotalPages)">末页</button>
        </div>
      </div>
    </div>
  </div>
</template>
