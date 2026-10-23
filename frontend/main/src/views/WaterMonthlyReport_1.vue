<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getWaterMonthlyDetail, getWaterMonthlySummary } from '@/api/waterReport.js'
import { downloadXLSX } from '@/utils/export'
import MonthPicker from '@/components/forms/MonthPicker.vue'

const filterStatMonth = ref('')
const filterMerchantName = ref('')
const filterDeviceNo = ref('')

watch(filterStatMonth, (val) => console.log('[WaterMonthlyReport] filterStatMonth changed to:', val))

const loading = ref(false)
const detailList = ref([])
const summaryList = ref([])

const pageNum = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)

const summary = computed(() => {
  let tu = 0, tf = 0, mc = 0
  summaryList.value.forEach(i => {
    tu += Number(i.totalUsage || 0)
    tf += Number(i.totalFee || 0)
    mc = Math.max(mc, Number(i.meterCount || 0))
  })
  return { meterCount: mc, totalUsage: tu, totalFee: tf }
})

async function loadDetail() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value, pageSize: pageSize.value,
      statMonth: filterStatMonth.value || undefined,
      merchantName: filterMerchantName.value || undefined, deviceNo: filterDeviceNo.value || undefined
    }
    console.log('[WaterMonthlyReport] loadDetail params:', JSON.stringify(params))
    const res = await getWaterMonthlyDetail(params)
    console.log('[WaterMonthlyReport] loadDetail raw res:', res)
    const body = res?.data
    console.log('[WaterMonthlyReport] loadDetail body:', body)
    detailList.value = Array.isArray(body?.list) ? body.list : []
    totalCount.value = body?.total || 0
    console.log('[WaterMonthlyReport] loadDetail list length:', detailList.value.length, 'total:', totalCount.value)
  } catch (e) { console.error('[WaterMonthlyReport] loadDetail error:', e); detailList.value = [] }
  finally { loading.value = false }
}

async function loadSummary() {
  try {
    const params = {
      statMonth: filterStatMonth.value || undefined,
      deviceNo: filterDeviceNo.value || undefined
    }
    console.log('[WaterMonthlyReport] loadSummary params:', JSON.stringify(params))
    const res = await getWaterMonthlySummary(params)
    console.log('[WaterMonthlyReport] loadSummary raw res:', res)
    summaryList.value = Array.isArray(res?.data) ? res.data : []
    console.log('[WaterMonthlyReport] loadSummary list length:', summaryList.value.length)
  } catch (e) { console.error('[WaterMonthlyReport] loadSummary error:', e); summaryList.value = [] }
}

async function loadData() { await Promise.all([loadDetail(), loadSummary()]) }

function handleSearch() { pageNum.value = 1; loadData() }
function resetFilters() { filterStatMonth.value = ''; filterMerchantName.value = ''; filterDeviceNo.value = ''; handleSearch() }
function goPage(n) { pageNum.value = n; loadDetail() }
onMounted(() => loadData())

function fmt(v) { if (v == null || v === '') return '—'; const n = Number(v); return isNaN(n) ? v : n.toFixed(2) }

function handleExport() {
  downloadXLSX([
    { key: 'statMonth', label: '统计月份' },
    { key: 'merchantName', label: '商户名称' },
    { key: 'deviceNo', label: '设备编号' },
    { key: 'virtualMeterNo', label: '虚拟表号' },
    { key: 'virtualMerchantName', label: '虚拟表商户名' },
    { key: 'totalUsage', label: '月用水量(m³)' },
    { key: 'totalFee', label: '月水费(元)' },
    { key: 'dailyCount', label: '有效天数' },
  ], detailList.value, '用水量月报明细')
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
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">参与统计水表数</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">个</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总用水量</div><div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div></div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center"><div class="text-[12px] text-[#666] mb-1">总费用</div><div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div></div>
      </div>
    </div>

    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center"><h2 class="text-[15px] font-bold">用水量月报明细</h2><span class="text-[12px] text-[#999]">共 {{ totalCount }} 条记录</span></div>
        <div class="overflow-y-auto max-h-[520px] relative min-h-[120px]">
          <table class="w-full border-collapse text-left">
            <thead class="sticky top-0 z-10"><tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">统计月份</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">设备编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">虚拟表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">虚拟表商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">月用水量(m³)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">月水费(元)</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">有效天数</th>
            </tr></thead>
            <tbody>
              <tr v-if="detailList.length === 0 && !loading"><td colspan="9" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td></tr>
              <tr v-for="(row, idx) in detailList" :key="idx" class="hover:bg-[#f5f6fb] transition-colors">
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (pageNum - 1) * pageSize + idx + 1 }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-medium">{{ row.statMonth }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.merchantName || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.deviceNo || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.virtualMeterNo || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.virtualMerchantName || '—' }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#3a64ab]">{{ fmt(row.totalUsage) }}</td>
                <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#52c41a]">{{ fmt(row.totalFee) }}</td>
                <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.dailyCount ?? '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
          <div class="flex items-center gap-2 text-[12px] text-[#666]">每页<select v-model="pageSize" class="py-1 px-1.5 border border-[#d9d9d9] rounded text-[12px] outline-none" @change="goPage(1)"><option :value="10">10</option><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option></select>条</div>
          <div class="flex gap-1">
            <button class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[12px] bg-white cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="pageNum <= 1" @click="goPage(1)">首页</button>
            <button class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[12px] bg-white cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
            <span class="py-1.5 px-3 text-[12px] text-[#666]">第 {{ pageNum }} 页</span>
            <button class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[12px] bg-white cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="detailList.length < pageSize" @click="goPage(pageNum + 1)">下一页</button>
            <button class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[12px] bg-white cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="detailList.length < pageSize" @click="goPage(Math.ceil(totalCount / pageSize))">末页</button>
          </div>
        </div>
    </div>
  </div>
</template>
