<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWaterDeletedMeterMonthly } from '@/api/waterReport.js'
import { downloadXLSX } from '@/utils/export'
import MonthPicker from '@/components/forms/MonthPicker.vue'

const filterStatMonth = ref('')
const filterMerchantName = ref('')
const filterDeviceNo = ref('')

const loading = ref(false)
const tableData = ref([])

const summary = computed(() => {
  let tu = 0, tf = 0
  tableData.value.forEach(i => { tu += Number(i.totalUsage || 0); tf += Number(i.totalFee || 0) })
  return { meterCount: tableData.value.length, totalUsage: tu, totalFee: tf }
})

async function loadData() {
  loading.value = true
  try {
    const res = await getWaterDeletedMeterMonthly({
      statMonth: filterStatMonth.value || undefined,
      merchantName: filterMerchantName.value || undefined,
      deviceNo: filterDeviceNo.value || undefined
    })
    tableData.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) { console.error(e); tableData.value = [] }
  finally { loading.value = false }
}

function handleSearch() { loadData() }
function resetFilters() { filterStatMonth.value = ''; filterMerchantName.value = ''; filterDeviceNo.value = ''; loadData() }
onMounted(() => loadData())

function fmt(v) { if (v == null || v === '') return '—'; const n = Number(v); return isNaN(n) ? v : n.toFixed(2) }
function dataSourceLabel(v) { return v === 'READING' ? '原始读数' : v === 'ARCHIVE' ? '历史归档' : v || '—' }
function dataSourceColor(v) { return v === 'ARCHIVE' ? '#faad14' : v === 'READING' ? '#52c41a' : '#999' }

function handleExport() {
  downloadXLSX([
    { key: 'statMonth', label: '统计月份' },
    { key: 'merchantName', label: '商户名称' },
    { key: 'deviceNo', label: '设备编号' },
    { key: 'totalUsage', label: '月用水量(m³)' },
    { key: 'totalFee', label: '月水费(元)' },
    { key: 'dailyCount', label: '有效天数' },
    { key: 'deletedTime', label: '删除时间' },
    { key: 'dataSource', label: '数据来源' },
  ], tableData.value, '已删除水表月报明细')
}
</script>

<template>
  <div class="p-5">
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <MonthPicker v-model="filterStatMonth" placeholder="选择月份" />
        <input v-model="filterMerchantName" class="py-2 px-3 border border-[#d9d9d9] rounded text-[13px] w-[150px] outline-none focus:border-[#3a64ab] transition-colors" type="text" placeholder="商户名称" />
        <input v-model="filterDeviceNo" class="py-2 px-3 border border-[#d9d9d9] rounded text-[13px] w-[150px] outline-none focus:border-[#3a64ab] transition-colors" type="text" placeholder="设备编号" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">已删除水表数</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">个</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总用水量</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总费用</div><div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div></div>
      </div>
    </div>

    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center"><h2 class="text-[15px] font-bold">已删除水表月报明细</h2><span class="text-[12px] text-[#999]">共 {{ tableData.length }} 条记录</span></div>
        <div class="overflow-y-auto max-h-[520px] relative min-h-[120px]">
          <table class="w-full border-collapse text-left">
            <thead class="sticky top-0 z-10"><tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">统计月份</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">设备编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">月用水量(m³)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">月水费(元)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">有效天数</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">删除时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">数据来源</th>
            </tr></thead>
            <tbody>
              <tr v-if="tableData.length === 0 && !loading"><td colspan="9" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td></tr>
              <tr v-for="(row, idx) in tableData" :key="idx" class="hover:bg-[#f5f6fb] transition-colors">
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ idx + 1 }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-medium">{{ row.statMonth }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.merchantName || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.deviceNo || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#3a64ab]">{{ fmt(row.totalUsage) }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#52c41a]">{{ fmt(row.totalFee) }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.dailyCount ?? '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ row.deletedTime || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium" :style="{ color: dataSourceColor(row.dataSource) }">{{ dataSourceLabel(row.dataSource) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
    </div>
  </div>
</template>
