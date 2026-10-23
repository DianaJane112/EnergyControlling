<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWaterBillingDaily } from '@/api/waterReport.js'
import { downloadXLSX } from '@/utils/export'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

const filterStartDate = ref('')
const filterEndDate = ref('')

const loading = ref(false)
const tableData = ref([])

const summary = computed(() => {
  let tu = 0, tf = 0
  tableData.value.forEach(i => { tu += Number(i.totalUsage || 0); tf += Number(i.totalFee || 0) })
  const avg = tu > 0 ? tf / tu : 0
  return { dayCount: tableData.value.length, totalUsage: tu, totalFee: tf, avgPrice: avg }
})

async function loadData() {
  loading.value = true
  try {
    const res = await getWaterBillingDaily({
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined
    })
    tableData.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) { console.error(e); tableData.value = [] }
  finally { loading.value = false }
}

function handleSearch() { loadData() }
function resetFilters() { filterStartDate.value = ''; filterEndDate.value = ''; loadData() }
onMounted(() => loadData())

function fmt(v) { if (v == null || v === '') return '—'; const n = Number(v); return isNaN(n) ? v : n.toFixed(2) }

function handleExport() {
  downloadXLSX([
    { key: 'statDate', label: '统计日期' },
    { key: 'totalUsage', label: '日用水量(m³)' },
    { key: 'totalFee', label: '日水费(元)' },
    { key: 'averagePrice', label: '平均单价(元/吨)' },
    { key: 'meterCount', label: '活跃水表数' },
  ], tableData.value, '计费水量日报明细')
}
</script>

<template>
  <div class="p-5">
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <DateRangePicker
          v-model:start-value="filterStartDate"
          v-model:end-value="filterEndDate"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总计费水量</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总计费金额</div><div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">平均单价</div><div class="text-lg font-bold text-[#faad14]">{{ summary.avgPrice.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元/吨</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">统计天数</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.dayCount }} <span class="text-[12px] font-normal text-[#999]">天</span></div></div>
      </div>
    </div>

    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <div class="bg-white rounded-lg border border-[#e8e8e8] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="p-5 pb-0 flex justify-between items-center"><h3 class="text-[15px] font-bold text-[#333]">计费水量日报明细</h3><span class="text-[12px] text-[#999]">共 {{ tableData.length }} 条记录</span></div>
      <div class="p-5 pt-3">
        <div class="overflow-y-auto max-h-[520px] relative min-h-[120px]">
          <table class="w-full border-collapse text-left">
            <thead class="sticky top-0 z-10"><tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">统计日期</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">日用水量(m³)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">日水费(元)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">平均单价(元/吨)</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">活跃水表数</th>
            </tr></thead>
            <tbody>
              <tr v-if="tableData.length === 0 && !loading"><td colspan="6" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td></tr>
              <tr v-for="(row, idx) in tableData" :key="row.statDate" class="hover:bg-[#f5f6fb] transition-colors">
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ idx + 1 }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ row.statDate }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold">{{ fmt(row.totalUsage) }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#52c41a]">{{ fmt(row.totalFee) }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono text-[#faad14]">{{ fmt(row.averagePrice) }}</td>
                <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.meterCount ?? '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
