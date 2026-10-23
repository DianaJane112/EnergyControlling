<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getAccountFlowSummary, exportAccountFlowSummary } from '@/api/finance.js'
import PeriodPicker from '@/components/forms/PeriodPicker.vue'

// ── 日期选择状态 ──────────────────────────────────────────────────────
const now = new Date()
const pickerYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const selectedYear = ref(now.getFullYear())

// 日数据：选中年 + 月 → 起止时间
const dailyStartEnd = computed(() => {
  const y = pickerYear.value
  const m = selectedMonth.value
  const lastDay = new Date(y, m, 0).getDate()
  return {
    start: `${y}-${String(m).padStart(2, '0')}-01 00:00:00`,
    end: `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padEnd(2, '0')} 23:59:59`,
  }
})

// 月数据：选中年 → 起止时间
const monthlyStartEnd = computed(() => {
  const y = selectedYear.value
  return {
    start: `${y}-01-01 00:00:00`,
    end: `${y}-12-31 23:59:59`,
  }
})

// ── 筛选条件 ──────────────────────────────────────────────────────────
const granularity = ref('daily')

const granularityOptions = [
  { v: 'daily', l: '日数据' },
  { v: 'monthly', l: '月数据' },
  { v: 'yearly', l: '年数据' },
]

function resetFilters() {
  granularity.value = 'daily'
  pickerYear.value = now.getFullYear()
  selectedMonth.value = now.getMonth() + 1
  selectedYear.value = now.getFullYear()
  fetchData()
}

// ── 数据 ──────────────────────────────────────────────────────────────
const tableData = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const params = { granularity: granularity.value }
    if (granularity.value === 'daily') {
      params.startTime = dailyStartEnd.value.start
      params.endTime = dailyStartEnd.value.end
    } else if (granularity.value === 'monthly') {
      params.startTime = monthlyStartEnd.value.start
      params.endTime = monthlyStartEnd.value.end
    }
    const res = await getAccountFlowSummary(params)
    tableData.value = (res && res.data) ? res.data : []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { fetchData() }

async function handleExport() {
  try {
    const params = { granularity: granularity.value }
    if (granularity.value === 'daily') {
      params.startTime = dailyStartEnd.value.start
      params.endTime = dailyStartEnd.value.end
    } else if (granularity.value === 'monthly') {
      params.startTime = monthlyStartEnd.value.start
      params.endTime = monthlyStartEnd.value.end
    }
    const res = await exportAccountFlowSummary(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const now = new Date()
    const ds = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
    a.download = `分账统计_${ds}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* export failed silently */ }
}

onMounted(() => fetchData())

// 切换粒度时自动查询
watch(granularity, () => fetchData())

// ── 统计 ──────────────────────────────────────────────────────────────
const totalRecharge = computed(() => tableData.value.reduce((s, r) => s + (Number(r.totalRecharge) || 0), 0))
const totalDebit = computed(() => tableData.value.reduce((s, r) => s + (Number(r.totalDebit) || 0), 0))
const totalRefund = computed(() => tableData.value.reduce((s, r) => s + (Number(r.totalRefund) || 0), 0))
const totalNetFlow = computed(() => tableData.value.reduce((s, r) => s + (Number(r.netFlow) || 0), 0))
const totalFlowCount = computed(() => tableData.value.reduce((s, r) => s + (Number(r.flowCount) || 0), 0))

function formatMoney(n) {
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<template>
  <div>
    <!-- ═══ 1. 筛选查询区 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4 items-center">

        <!-- ══ 日期选择 ══ -->
        <PeriodPicker
          v-if="granularity !== 'yearly'"
          :granularity="granularity"
          :picker-year="pickerYear"
          :selected-month="selectedMonth"
          :selected-year="selectedYear"
          @update:picker-year="pickerYear = $event"
          @update:selected-month="selectedMonth = $event"
          @update:selected-year="selectedYear = $event"
          @change="handleSearch"
        />

        <!-- ══ 统计维度切换 ══ -->
        <div class="flex items-center gap-1.5">
          <span class="text-[13px] text-[#666] mr-1">统计维度</span>
          <button
            v-for="(opt, idx) in granularityOptions"
            :key="opt.v"
            class="py-1.5 px-4 text-[13px] cursor-pointer border transition-colors"
            :class="granularity === opt.v
              ? 'bg-[#3a64ab] text-white border-[#3a64ab]'
              : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
            :style="idx === 0 ? 'border-radius: 4px 0 0 4px' : idx === granularityOptions.length - 1 ? 'border-radius: 0 4px 4px 0' : 'border-radius: 0'"
            @click="granularity = opt.v"
          >{{ opt.l }}</button>
        </div>

        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <!-- ═══ 2. 统计概览 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">充值总额</div>
          <div class="text-[22px] font-bold text-[#3a64ab]">¥ {{ formatMoney(totalRecharge) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">扣费总额</div>
          <div class="text-[22px] font-bold text-[#fa8c16]">¥ {{ formatMoney(totalDebit) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">退费总额</div>
          <div class="text-[22px] font-bold text-[#ff4d4f]">¥ {{ formatMoney(totalRefund) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">净流水</div>
          <div class="text-[22px] font-bold text-[#52c41a]">¥ {{ formatMoney(totalNetFlow) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">总笔数</div>
          <div class="text-[22px] font-bold">{{ totalFlowCount }} 笔</div>
        </div>
      </div>
    </div>

    <!-- ═══ 3. 数据表格 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h2 class="text-[15px] font-bold">分账统计</h2>
        <span class="text-[12px] text-[#999]">共 {{ tableData.length }} 条记录</span>
      </div>

      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>

        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">周期</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">充值总额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">扣费总额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">退费总额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">调账总额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">分摊总额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">净流水</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">笔数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in tableData" :key="r.period || idx" class="hover:bg-[#f5f6fb]">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.period }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.totalRecharge > 0 ? 'text-[#3a64ab] font-medium' : 'text-[#999]'">¥ {{ formatMoney(r.totalRecharge) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.totalDebit > 0 ? 'text-[#fa8c16] font-medium' : 'text-[#999]'">¥ {{ formatMoney(r.totalDebit) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.totalRefund > 0 ? 'text-[#ff4d4f] font-medium' : 'text-[#999]'">¥ {{ formatMoney(r.totalRefund) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.totalAdjust !== 0 ? 'text-[#722ed1] font-medium' : 'text-[#999]'">¥ {{ formatMoney(r.totalAdjust) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.totalAllocation > 0 ? 'text-[#333]' : 'text-[#999]'">¥ {{ formatMoney(r.totalAllocation) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.netFlow > 0 ? 'text-[#52c41a] font-medium' : r.netFlow < 0 ? 'text-[#ff4d4f] font-medium' : 'text-[#999]'">¥ {{ formatMoney(r.netFlow) }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono">{{ r.flowCount }} 笔</td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">
          暂无匹配数据
        </div>
      </div>
    </div>
  </div>
</template>
