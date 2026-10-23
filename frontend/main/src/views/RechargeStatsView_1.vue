<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getDepositSummary } from '@/api/finance.js'

// ── Filters ─────────────────────────────────────────────────────────
const mode = ref('daily')
const startDate = ref('')
const endDate = ref('')

function initDateRange() {
  const now = new Date()
  if (mode.value === 'daily') {
    endDate.value = now.toISOString().slice(0, 10)
    const s = new Date(now.getTime() - 7 * 86400000)
    startDate.value = s.toISOString().slice(0, 10)
  } else {
    endDate.value = now.toISOString().slice(0, 10)
    const s = new Date(now.getFullYear(), now.getMonth() - 5, 1)
    startDate.value = s.toISOString().slice(0, 10)
  }
}
initDateRange()

watch(mode, () => { initDateRange(); fetchData() })

// ── Data ────────────────────────────────────────────────────────────
const chartData = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getDepositSummary({
      granularity: mode.value === 'daily' ? 'DAY' : 'MONTH',
      startTime: startDate.value || undefined,
      endTime: endDate.value || undefined,
    })
    chartData.value = res.data || []
  } catch { chartData.value = [] }
  finally { loading.value = false }
}

const totalAmount = computed(() => chartData.value.reduce((sum, d) => sum + (d.totalAmount || 0), 0))

const maxAmount = computed(() => {
  if (chartData.value.length === 0) return 1
  return Math.max(...chartData.value.map(d => d.totalAmount || 0)) || 1
})

onMounted(() => fetchData())
</script>

<template>
  <div>
    <!-- Filter Bar -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <div class="flex items-center gap-2">
          <label class="text-[13px] text-[#666]">日期</label>
          <input
            v-model="startDate"
            type="date"
            class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]"
          />
          <span class="text-[13px] text-[#999]">至</span>
          <input
            v-model="endDate"
            type="date"
            class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]"
          />
        </div>
        <div class="flex gap-2.5">
          <button
            class="py-1.5 px-4 rounded text-[13px] cursor-pointer border-none"
            :class="mode === 'daily' ? 'bg-[#3a64ab] text-white' : 'bg-white border border-[#d9d9d9] text-[#333]'"
            @click="mode = 'daily'"
          >按日统计</button>
          <button
            class="py-1.5 px-4 rounded text-[13px] cursor-pointer border-none"
            :class="mode === 'monthly' ? 'bg-[#3a64ab] text-white' : 'bg-white border border-[#d9d9d9] text-[#333]'"
            @click="mode = 'monthly'"
          >按月统计</button>
        </div>
        <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="fetchData">查询</button>
      </div>
    </div>

    <!-- Chart Panel -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)] relative min-h-[200px]">
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
        <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
      </div>
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-[15px] font-bold">{{ mode === 'daily' ? '每日' : '每月' }}充值金额统计 (¥)</h3>
      </div>
      <div v-if="chartData.length > 0" class="flex items-end gap-3 h-[200px] px-2.5">
        <div v-for="d in chartData" :key="d.period" class="flex-1 flex flex-col items-center gap-1">
          <div class="w-full flex flex-col justify-end rounded-t overflow-hidden" style="height: 180px">
            <div
              class="bg-[#3a64ab] rounded-t w-full transition-all duration-300"
              :style="{ height: ((d.totalAmount || 0) / maxAmount * 100) + '%' }"
            ></div>
          </div>
          <span class="text-[10px] text-[#999] whitespace-nowrap">{{ d.period }}</span>
        </div>
      </div>
      <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无统计数据</div>
    </div>

    <!-- Table Panel -->
    <div class="bg-white rounded p-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex justify-between items-center mb-[15px]">
        <h2 class="text-[15px] font-bold">统计明细</h2>
        <span class="text-[13px] text-[#666]">合计: <b class="text-[#3a64ab]">¥ {{ totalAmount.toLocaleString() }}</b></span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">周期</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb] text-right">充值总额 (¥)</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb] text-right">充值笔数</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb] text-right">充值户数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in chartData" :key="d.period" class="hover:bg-[#f5f6fb]">
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ d.period }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] text-right font-mono font-bold text-[#3a64ab]">¥ {{ (d.totalAmount || 0).toLocaleString() }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] text-right font-mono">{{ d.chargeCount || 0 }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] text-right font-mono">{{ d.customerCount || 0 }}</td>
            </tr>
            <tr v-if="chartData.length === 0 && !loading">
              <td colspan="4" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
