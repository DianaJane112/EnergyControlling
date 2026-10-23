<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWaterCampusMonthlySummary } from '@/api/waterReport.js'
import { downloadXLSX } from '@/utils/export'
import MonthPicker from '@/components/forms/MonthPicker.vue'

const filterStatMonth = ref('')

const loading = ref(false)
const tableData = ref([])

function lastDayOfMonth(y, m) { return new Date(y, m, 0).getDate() }

const summary = computed(() => {
  let tu = 0, tf = 0, mc = 0
  tableData.value.forEach(i => { tu += Number(i.totalUsage || 0); tf += Number(i.totalFee || 0); if (Number(i.meterCount || 0) > mc) mc = Number(i.meterCount) })
  return { monthCount: tableData.value.length, totalUsage: tu, totalFee: tf, maxMeterCount: mc }
})

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (filterStatMonth.value) {
      const [y, m] = filterStatMonth.value.split('-').map(Number)
      params.statYear = String(y)
      params.startDate = `${y}-${String(m).padStart(2, '0')}-01`
      params.endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDayOfMonth(y, m)).padStart(2, '0')}`
    }
    const res = await getWaterCampusMonthlySummary(params)
    tableData.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) { console.error(e); tableData.value = [] }
  finally { loading.value = false }
}

function handleSearch() { loadData() }
function resetFilters() { filterStatMonth.value = ''; loadData() }
onMounted(() => loadData())

function fmt(v) { if (v == null || v === '') return '—'; const n = Number(v); return isNaN(n) ? v : n.toFixed(2) }

function handleExport() {
  downloadXLSX([
    { key: 'statMonth', label: '统计月份' },
    { key: 'totalUsage', label: '月度用水量(m³)' },
    { key: 'totalFee', label: '月度费用(元)' },
    { key: 'meterCount', label: '活跃水表数' },
    { key: 'dailyCount', label: '有效天数' },
  ], tableData.value, '全园区月度用水量汇总')
}

const sortKey = ref(''); const sortDir = ref('asc')
function toggleSort(key) { if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'; else { sortKey.value = key; sortDir.value = 'asc' } }
const sortedData = computed(() => { const list = [...tableData.value]; if (sortKey.value) list.sort((a, b) => { const va = a[sortKey.value] ?? ''; const vb = b[sortKey.value] ?? ''; if (va < vb) return sortDir.value === 'asc' ? -1 : 1; if (va > vb) return sortDir.value === 'asc' ? 1 : -1; return 0 }); return list })
</script>

<template>
  <div class="p-5">
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <MonthPicker v-model="filterStatMonth" placeholder="选择月份" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">统计月份数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.monthCount }} <span class="text-[12px] font-normal text-[#999]">月</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总用水量</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">总费用</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">峰值活跃水表</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.maxMeterCount }} <span class="text-[12px] font-normal text-[#999]">户</span></div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center"><h2 class="text-[15px] font-bold">全园区月度用水量汇总</h2><span class="text-[12px] text-[#999]">共 {{ tableData.length }} 条记录</span></div>
        <div class="overflow-x-auto max-h-[520px] overflow-y-auto">
        <table class="w-full border-collapse text-left">
          <thead class="sticky top-0 z-10"><tr>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap cursor-pointer hover:bg-[#ebedf2] select-none" @click="toggleSort('statMonth')">统计月份<span class="text-[10px] ml-0.5 text-[#bbb]"><span v-if="sortKey==='statMonth'">{{ sortDir==='asc'?'▲':'▼' }}</span><span v-else>⇅</span></span></th>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right cursor-pointer hover:bg-[#ebedf2] select-none" @click="toggleSort('totalUsage')">月度用水量(m³)<span class="text-[10px] ml-0.5 text-[#bbb]"><span v-if="sortKey==='totalUsage'">{{ sortDir==='asc'?'▲':'▼' }}</span><span v-else>⇅</span></span></th>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right cursor-pointer hover:bg-[#ebedf2] select-none" @click="toggleSort('totalFee')">月度费用(元)<span class="text-[10px] ml-0.5 text-[#bbb]"><span v-if="sortKey==='totalFee'">{{ sortDir==='asc'?'▲':'▼' }}</span><span v-else>⇅</span></span></th>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">活跃水表数</th>
            <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">有效天数</th>
          </tr></thead>
          <tbody>
            <tr v-if="sortedData.length === 0 && !loading"><td colspan="6" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td></tr>
            <tr v-for="(row, idx) in sortedData" :key="row.statMonth" class="hover:bg-[#f5f6fb] transition-colors">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-medium">{{ row.statMonth }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#3a64ab]">{{ fmt(row.totalUsage) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ fmt(row.totalFee) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.meterCount ?? '—' }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.dailyCount ?? '—' }}</td>
            </tr>
          </tbody>
        </table>
        </div>
    </div>
  </div>
</template>
