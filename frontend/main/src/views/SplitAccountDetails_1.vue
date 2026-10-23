<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getAccountFlowList, exportAccountFlow } from '@/api/finance.js'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

// ── 筛选条件 ──────────────────────────────────────────────────────────
const filter = reactive({
  keyword: '',
  dateStart: '',
  dateEnd: '',
  tradeType: '',
})

function resetFilters() {
  filter.keyword = ''
  filter.dateStart = ''
  filter.dateEnd = ''
  filter.tradeType = ''
  currentPage.value = 1
  fetchData()
}

// ── 分页 ──────────────────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function fetchData() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize,
    }
    if (filter.keyword) params.keyword = filter.keyword
    if (filter.dateStart) params.startTime = filter.dateStart + ' 00:00:00'
    if (filter.dateEnd) params.endTime = filter.dateEnd + ' 23:59:59'
    if (filter.tradeType) params.tradeType = filter.tradeType

    const res = await getAccountFlowList(params)
    if (res && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function goToPage(p) {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p
    fetchData()
  }
}

onMounted(() => fetchData())

// ── 导出 ──────────────────────────────────────────────────────────────
async function handleExport() {
  const params = {}
  if (filter.dateStart) params.startTime = filter.dateStart + ' 00:00:00'
  if (filter.dateEnd) params.endTime = filter.dateEnd + ' 23:59:59'
  if (filter.tradeType) params.tradeType = filter.tradeType
  if (filter.keyword) params.keyword = filter.keyword

  try {
    const res = await exportAccountFlow(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const now = new Date()
    const ds = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
    a.download = `分账明细_${ds}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 导出失败静默处理
  }
}

// ── 统计 ──────────────────────────────────────────────────────────────
const totalRecharge = computed(() => tableData.value
  .filter(r => Number(r.changeAmount) > 0)
  .reduce((s, r) => s + (Number(r.changeAmount) || 0), 0))
const totalDeduct = computed(() => tableData.value
  .filter(r => Number(r.changeAmount) < 0)
  .reduce((s, r) => s + (Number(r.changeAmount) || 0), 0))
const netChange = computed(() => tableData.value.reduce((s, r) => s + (Number(r.changeAmount) || 0), 0))

function formatMoney(n) {
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}
</script>

<template>
  <div>
    <!-- ═══ 1. 筛选查询区 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4 items-center">
        <input v-model="filter.keyword" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="流水号" type="text" @keyup.enter="handleSearch" />
        <DateRangePicker v-model:start-value="filter.dateStart" v-model:end-value="filter.dateEnd" start-placeholder="开始日期" end-placeholder="结束日期" />
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
          <div class="text-[13px] text-[#666] mb-1">总记录数</div>
          <div class="text-[22px] font-bold">{{ total }} 条</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">收入（当前页）</div>
          <div class="text-[22px] font-bold text-[#52c41a]">¥ {{ formatMoney(totalRecharge) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">支出（当前页）</div>
          <div class="text-[22px] font-bold text-[#ff4d4f]">¥ {{ formatMoney(Math.abs(totalDeduct)) }}</div>
        </div>
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">净变动（当前页）</div>
          <div class="text-[22px] font-bold" :class="netChange >= 0 ? 'text-[#3a64ab]' : 'text-[#ff4d4f]'">¥ {{ formatMoney(netChange) }}</div>
        </div>
      </div>
    </div>

    <!-- ═══ 3. 数据表格 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>

        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">流水号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">交易类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">业务单号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动前余额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动后余额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作员</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">备注</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in tableData" :key="r.id" class="hover:bg-[#f5f6fb]">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.flowNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.customerName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ r.meterId }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.tradeTypeLabel }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.bizNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">¥ {{ formatMoney(r.balanceBefore) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="r.changeAmount >= 0 ? 'text-[#52c41a] font-medium' : 'text-[#ff4d4f] font-medium'">¥ {{ formatMoney(r.changeAmount) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">¥ {{ formatMoney(r.balanceAfter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.operatorName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] max-w-[120px] truncate" :title="r.remark">{{ r.remark }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ formatTime(r.operateTime) }}</td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">
          暂无匹配数据
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="flex items-center gap-1.5">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goToPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(totalPages)">末页</button>
        </div>
      </div>
    </div>
  </div>
</template>
