<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getBalanceAdjustmentList,
  getBalanceAdjustmentDetail,
  getBalanceAdjustmentSummary,
  exportBalanceAdjustment,
  executeBalanceAdjustment,
} from '@/api/finance.js'
import { getMeterList } from '@/api/meters.js'
import { useToast } from '@/composables/useToast.js'
import ToastMessage from '@/components/common/ToastMessage.vue'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

const { showToast } = useToast()

// ── 筛选选项 ──────────────────────────────────────────────────────────
const typeOptions = [
  { value: '', label: '全部类型' },
  { value: 'INCREASE', label: '余额增加' },
  { value: 'DECREASE', label: '余额扣减' },
]

// ── Filters ──────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterAdjustType = ref('')
const filterDateStart = ref('')
const filterDateEnd = ref('')
const helpModalOpen = ref(false)

async function resetFilters() {
  filterKeyword.value = ''
  filterAdjustType.value = ''
  filterDateStart.value = ''
  filterDateEnd.value = ''
  currentPage.value = 1
  await fetchData()
  await fetchSummary()
}

// ── Pagination & data ────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    if (filterKeyword.value) params.keyword = filterKeyword.value.trim()
    if (filterAdjustType.value) params.adjustmentType = filterAdjustType.value
    if (filterDateStart.value) params.startTime = filterDateStart.value + ' 00:00:00'
    if (filterDateEnd.value) params.endTime = filterDateEnd.value + ' 23:59:59'

    const res = await getBalanceAdjustmentList(params)
    if (res && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('获取余额调整列表失败', e)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  currentPage.value = 1
  await fetchData()
  await fetchSummary()
}

async function goToPage(p) {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p
    await fetchData()
    fetchSummary()
  }
}

// ── Row selection ────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Summary ──────────────────────────────────────────────────────────
const summary = reactive({
  totalAmount: 0,
  adjustCount: 0,
  customerCount: 0,
  refundCount: 0,
  refundAmount: 0,
  rechargeCount: 0,
  rechargeAmount: 0,
  netChange: 0,
})

async function fetchSummary() {
  try {
    const params = {}
    if (filterDateStart.value) params.startTime = filterDateStart.value + ' 00:00:00'
    if (filterDateEnd.value) params.endTime = filterDateEnd.value + ' 23:59:59'
    if (filterAdjustType.value) params.adjustmentType = filterAdjustType.value

    // 后端统计（总笔数/总金额/商户数）
    const res = await getBalanceAdjustmentSummary(params)
    if (res && res.data && res.data.length > 0) {
      let totalAmt = 0, totalCnt = 0, totalCust = 0
      res.data.forEach(item => {
        totalAmt += Number(item.totalAmount || 0)
        totalCnt += Number(item.adjustCount || 0)
        totalCust = Math.max(totalCust, Number(item.customerCount || 0))
      })
      summary.totalAmount = totalAmt
      summary.adjustCount = totalCnt
      summary.customerCount = totalCust
    } else {
      summary.totalAmount = 0
      summary.adjustCount = 0
      summary.customerCount = 0
    }

    // 从当前页数据计算退费/补收 —— 无论如何都要执行
    let refundAmt = 0, refundCnt = 0, rechargeAmt = 0, rechargeCnt = 0
    tableData.value.forEach(r => {
      const bal = Number(r.balance || 0)
      if (bal < 0) { refundCnt++; refundAmt += bal }
      else { rechargeCnt++; rechargeAmt += bal }
    })
    summary.refundCount = refundCnt
    summary.refundAmount = refundAmt
    summary.rechargeCount = rechargeCnt
    summary.rechargeAmount = rechargeAmt
    summary.netChange = rechargeAmt + refundAmt
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

// ── Helpers ──────────────────────────────────────────────────────────
function formatMoney(n) {
  const num = Number(n)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatPrice(n) {
  const num = Number(n)
  if (isNaN(num) || num === 0) return '--'
  return num.toFixed(4)
}

function formatTime(t) {
  if (!t) return '--'
  return String(t).replace('T', ' ').substring(0, 19)
}

// ── 导出 ─────────────────────────────────────────────────────────────
async function handleExport() {
  try {
    const params = {}
    if (filterKeyword.value) params.keyword = filterKeyword.value.trim()
    if (filterAdjustType.value) params.adjustmentType = filterAdjustType.value
    if (filterDateStart.value) params.startTime = filterDateStart.value + ' 00:00:00'
    if (filterDateEnd.value) params.endTime = filterDateEnd.value + ' 23:59:59'

    const res = await exportBalanceAdjustment(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const disposition = res.headers['content-disposition']
    let fileName = '余额调整记录.xlsx'
    if (disposition) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
    }
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出失败', e)
    showToast('导出失败：' + (e.message || '未知错误'), 'error')
  }
}

// ── Modal 1: Batch adjustment（余额退补）─────────────────────────────
const batchModalOpen = ref(false)
const batchForm = reactive({
  startDate: '',
  endDate: '',
  deductMethod: 'immediate',
  priceType: 'multi',
  unifiedPrice: false,
  peakPrice: '',
  highPrice: '',
  flatPrice: '',
  valleyPrice: '',
  surchargePrice: '',
  updatePrice: false,
  remark: '',
})
const batchList = ref([])
const batchSubmitting = ref(false)
const batchDbNoInput = ref('')
const batchAdding = ref(false)

function toggleBatchModal() { batchModalOpen.value = !batchModalOpen.value }

async function addBatchItem() {
  const dbNo = batchDbNoInput.value.trim()
  if (!dbNo) {
    showToast('请输入表号', 'error')
    return
  }
  if (!/^\d{12}$/.test(dbNo)) {
    showToast('表号格式错误，需为12位数字', 'error')
    return
  }
  // 检查是否已添加
  if (batchList.value.some(i => i.meterId === dbNo)) {
    showToast('该表号已添加', 'error')
    return
  }
  batchAdding.value = true
  try {
    const res = await getMeterList({ dbNo, pageNum: 1, pageSize: 1 })
    if (res && res.data && res.data.records && res.data.records.length > 0) {
      const meter = res.data.records[0]
      const name = meter.chenman?.name || meter.customerName || dbNo
      const group = meter.chenman?.proName || meter.jicheng?.meterTypeName || '—'
      const plusPrice = meter.chenman?.plusPrice
      batchList.value.push({
        id: Date.now(),
        groupName: group,
        meterId: dbNo,
        merchantName: name,
        dateRange: batchForm.startDate && batchForm.endDate ? batchForm.startDate + ' ~ ' + batchForm.endDate : '—',
        consumption: '—',
        originalPrice: plusPrice ? Number(plusPrice).toFixed(4) : '0.8500',
        originalSurcharge: '0.0200',
        adjustPrice: batchForm.unifiedPrice ? (batchForm.peakPrice || '0.8200') : '0.8200',
        adjustSurcharge: batchForm.surchargePrice || '0.0100',
        adjustAmount: '0.00',
      })
      batchDbNoInput.value = ''
    } else {
      showToast('未找到该表号的电表：' + dbNo, 'error')
    }
  } catch (e) {
    console.error('查询电表失败', e)
    showToast('查询电表失败：' + (e.message || '未知错误'), 'error')
  } finally {
    batchAdding.value = false
  }
}

function removeBatchItem(id) {
  batchList.value = batchList.value.filter(i => i.id !== id)
}

const batchStats = computed(() => {
  let refundCount = 0, refundAmt = 0, rechargeCount = 0, rechargeAmt = 0
  batchList.value.forEach(i => {
    const amt = parseFloat(i.adjustAmount)
    if (amt < 0) { refundCount++; refundAmt += amt }
    else { rechargeCount++; rechargeAmt += amt }
  })
  return {
    meterCount: batchList.value.length,
    refundCount, refundAmt: refundAmt.toFixed(2),
    rechargeCount, rechargeAmt: rechargeAmt.toFixed(2),
    netChange: (rechargeAmt + refundAmt).toFixed(2),
  }
})

async function submitBatch() {
  if (batchList.value.length === 0) {
    showToast('请添加退补表计', 'error')
    return
  }
  batchSubmitting.value = true
  let successCount = 0, failCount = 0
  for (const item of batchList.value) {
    const price = batchForm.unifiedPrice
      ? batchForm.peakPrice
      : batchForm.peakPrice || batchForm.flatPrice || item.adjustPrice || '0'
    try {
      await executeBalanceAdjustment({
        dbNo: item.meterId,
        adjustedPrice: Number(price),
        amountChange: Number(item.adjustAmount),
        reason: batchForm.remark || '电价调整退补',
        remark: batchForm.remark || '',
        executeType: 'PRICE_ADJUST',
        deductMethod: batchForm.deductMethod,
        previewOnly: false,
      })
      successCount++
    } catch (e) {
      failCount++
      console.error('退补执行失败', item.meterId, e)
    }
  }
  batchSubmitting.value = false
  batchModalOpen.value = false
  batchList.value = []
  showToast(`退补执行完成：成功 ${successCount} 条${failCount > 0 ? '，失败 ' + failCount + ' 条' : ''}`, failCount > 0 ? 'error' : 'success')
  await fetchData()
  await fetchSummary()
}

// ── Modal 2: Single adjustment（余额调整）───────────────────────────
const singleModalOpen = ref(false)
const singleForm = reactive({
  meterType: '',
  dbNo: '',
  currentBalance: '',
  adjustMode: 'change',
  targetValue: '',
  remark: '',
})
const singleSubmitting = ref(false)

function toggleSingleModal() {
  singleForm.meterType = ''
  singleForm.dbNo = ''
  singleForm.currentBalance = ''
  singleForm.adjustMode = 'change'
  singleForm.targetValue = ''
  singleForm.remark = ''
  singleModalOpen.value = !singleModalOpen.value
}

async function onSingleDbNoInput() {
  if (singleForm.dbNo.length === 12) {
    singleForm.currentBalance = '查询中...'
    try {
      const res = await getMeterList({ dbNo: singleForm.dbNo, pageNum: 1, pageSize: 1 })
      if (res && res.data && res.data.records && res.data.records.length > 0) {
        const meter = res.data.records[0]
        singleForm._meterName = meter.customerName || meter.chenman?.name || ''
        if (meter.customerBalance !== undefined && meter.customerBalance !== null) {
          singleForm.currentBalance = Number(meter.customerBalance).toFixed(2)
        } else {
          singleForm.currentBalance = '无余额数据'
        }
      } else {
        singleForm.currentBalance = '表号不存在'
      }
    } catch (e) {
      console.error('查询电表失败', e)
      singleForm.currentBalance = '查询失败'
    }
  } else {
    singleForm.currentBalance = ''
    singleForm._meterName = ''
  }
}

async function submitSingle() {
  if (!singleForm.dbNo || !singleForm.targetValue) {
    showToast('请填写表号和金额', 'error')
    return
  }
  singleSubmitting.value = true
  try {
    const amount = singleForm.adjustMode === 'set'
      ? Number(singleForm.targetValue) - Number(singleForm.currentBalance)
      : Number(singleForm.targetValue)
    await executeBalanceAdjustment({
      dbNo: singleForm.dbNo,
      amountChange: amount,
      reason: singleForm.remark || '手动余额调整',
      remark: singleForm.remark || '',
      executeType: 'BALANCE_ADJUST',
      previewOnly: false,
    })
    showToast('余额调整执行成功', 'success')
    singleModalOpen.value = false
    await fetchData()
    await fetchSummary()
  } catch (e) {
    console.error('余额调整执行失败', e)
    showToast('执行失败：' + ((e.response && e.response.data && e.response.data.msg) || e.message || '未知错误'), 'error')
  } finally {
    singleSubmitting.value = false
  }
}

// ── 详情弹窗 ────────────────────────────────────────────────────────
const detailModal = ref(false)
const detailRow = ref(null)
const detailLoading = ref(false)

async function openDetail(row) {
  detailModal.value = true
  detailRow.value = null
  detailLoading.value = true
  try {
    const res = await getBalanceAdjustmentDetail(row.id)
    if (res && res.data) {
      detailRow.value = res.data
    } else {
      detailRow.value = row
    }
  } catch {
    detailRow.value = row
  } finally {
    detailLoading.value = false
  }
}
function closeDetail() { detailModal.value = false }

// ── 生命周期 ────────────────────────────────────────────────────────
onMounted(async () => {
  await fetchData()
  await fetchSummary()
})
</script>

<template>
  <div @click="selectedRowId = null">
    <ToastMessage />
    <!-- ═══ Filter Area ═══ -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-5 items-center">
        <input
          v-model="filterKeyword"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="商户名称或表号"
          type="text"
          @keyup.enter="handleSearch"
        />
        <DropdownSelect v-model="filterAdjustType" :options="typeOptions" placeholder="退补类型" />
        <DateRangePicker
          v-model:start-value="filterDateStart"
          v-model:end-value="filterDateEnd"
          start-placeholder="创建日期起"
          end-placeholder="创建日期止"
        />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleExport">导出</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="toggleBatchModal">新建余额退补</button>
        <button class="py-2 px-5 rounded bg-[#52c41a] text-white text-[13px] cursor-pointer border-none hover:bg-[#45b016] transition-colors" @click="toggleSingleModal">新建余额调整</button>
        <button class="w-7 h-7 rounded-full border border-[#d9d9d9] bg-white text-[#999] text-[13px] cursor-pointer flex items-center justify-center hover:border-[#3a64ab] hover:text-[#3a64ab] transition-colors font-bold" @click="helpModalOpen = true">?</button>
      </div>
    </div>

    <!-- ═══ Summary Cards ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[140px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">总笔数</div>
          <div class="text-[22px] font-bold text-[#3a64ab]">{{ summary.adjustCount }}<span class="text-[14px] font-normal text-[#999] ml-1">笔</span></div>
        </div>
        <div class="flex-1 min-w-[180px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">退费</div>
          <div class="text-[22px] font-bold text-[#ff4d4f]">{{ summary.refundCount }}<span class="text-[14px] font-normal text-[#999] ml-1">条</span></div>
          <div class="text-[13px] text-[#ff4d4f] mt-0.5">¥ {{ formatMoney(Math.abs(summary.refundAmount)) }}</div>
        </div>
        <div class="flex-1 min-w-[180px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">补收</div>
          <div class="text-[22px] font-bold text-[#52c41a]">{{ summary.rechargeCount }}<span class="text-[14px] font-normal text-[#999] ml-1">条</span></div>
          <div class="text-[13px] text-[#52c41a] mt-0.5">¥ {{ formatMoney(summary.rechargeAmount) }}</div>
        </div>
        <div class="flex-1 min-w-[180px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">变动金额</div>
          <div class="text-[22px] font-bold" :class="summary.netChange >= 0 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">¥ {{ formatMoney(summary.netChange) }}</div>
          <div class="text-[12px] text-[#999] mt-0.5">补收与退费净额</div>
        </div>
      </div>
    </div>

    <!-- ═══ Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <!-- Loading -->
      <div v-if="loading" class="text-center py-10 text-[13px] text-[#999]">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">原电价</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">原附加电价</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">退补电价</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">退补附加电价</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">退补金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">退补类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">支付状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">扣除方式</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">扣除时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建人</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">备注</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableData.length === 0">
              <td colspan="17" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(row, idx) in tableData"
              :key="row.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === row.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(row.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.groupName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ row.dbNo || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.customerName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatPrice(row.originalPrice) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatPrice(row.originalSurcharge) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatPrice(row.adjustPrice) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ formatPrice(row.adjustSurcharge) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-medium" :class="Number(row.balance) < 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">
                {{ Number(row.balance) >= 0 ? '+' : '' }}{{ formatMoney(row.balance) }}
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.adjustmentTypeLabel || row.adjustmentType || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span :class="row.paymentStatus === '已完成' ? 'text-[#52c41a]' : 'text-[#faad14]'">{{ row.paymentStatus || '--' }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.deductMethod || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-mono">{{ formatTime(row.deductTime) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-mono">{{ formatTime(row.createTime) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.operatorName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] max-w-[160px]">
                <div class="relative group inline-block max-w-full">
                  <span class="block truncate">{{ row.remark || '--' }}</span>
                  <div v-if="row.remark && row.remark.length > 10" class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50 pointer-events-none">
                    <div class="bg-white text-[#333] text-[12px] px-2.5 py-2 rounded shadow-[0_2px_8px_rgba(0,0,0,0.15)] border border-[#e8e8e8] w-[160px] leading-[1.6]">{{ row.remark }}</div>
                  </div>
                </div>
              </td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] whitespace-nowrap" @click.stop="openDetail(row)">详情</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages || 1 }} 页</span>
        <div class="flex items-center gap-1.5">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goToPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- ═══ Modal 1: Batch Adjustment ═══ -->
    <Teleport to="body">
      <div v-if="batchModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="batchModalOpen = false">
        <div class="bg-white rounded-lg shadow-2xl w-[900px] max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center px-6 py-4 border-b border-[#f0f0f0] shrink-0">
            <span class="text-[16px] font-medium text-[#333]">新建余额退补</span>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="batchModalOpen = false">&times;</button>
          </div>

          <div class="flex flex-1 overflow-hidden">
            <!-- Left: Config form -->
            <div class="w-[360px] shrink-0 px-6 py-5 border-r border-[#f0f0f0] overflow-y-auto space-y-4">
              <div>
                <label class="text-[13px] text-[#333] block mb-2">退补时间</label>
                <DateRangePicker v-model:start-value="batchForm.startDate" v-model:end-value="batchForm.endDate" start-placeholder="开始日期" end-placeholder="结束日期" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">扣除方式</label>
                <div class="flex gap-6">
                  <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="batchForm.deductMethod" type="radio" value="immediate" class="styled-checkbox" />立即扣除</label>
                  <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="batchForm.deductMethod" type="radio" value="delayed" class="styled-checkbox" />延期扣除</label>
                </div>
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">电价类型</label>
                <div class="flex gap-6">
                  <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="batchForm.priceType" type="radio" value="single" class="styled-checkbox" />单一制电价</label>
                  <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="batchForm.priceType" type="radio" value="multi" class="styled-checkbox" />复费率电价</label>
                </div>
              </div>
              <template v-if="batchForm.priceType === 'multi'">
                <div class="bg-[#f5f6fb] rounded p-3 space-y-3">
                  <label class="flex items-center gap-2 text-[13px] cursor-pointer"><input v-model="batchForm.unifiedPrice" type="checkbox" class="styled-checkbox" />尖峰平谷电价统一</label>
                  <div v-if="batchForm.unifiedPrice">
                    <input v-model="batchForm.peakPrice" type="number" step="0.0001" placeholder="输入统一电价" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div v-else class="grid grid-cols-2 gap-2">
                    <div><span class="text-[11px] text-[#999] block mb-1">尖峰</span><input v-model="batchForm.peakPrice" type="number" step="0.0001" placeholder="元/kWh" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" /></div>
                    <div><span class="text-[11px] text-[#999] block mb-1">高峰</span><input v-model="batchForm.highPrice" type="number" step="0.0001" placeholder="元/kWh" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" /></div>
                    <div><span class="text-[11px] text-[#999] block mb-1">平段</span><input v-model="batchForm.flatPrice" type="number" step="0.0001" placeholder="元/kWh" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" /></div>
                    <div><span class="text-[11px] text-[#999] block mb-1">谷段</span><input v-model="batchForm.valleyPrice" type="number" step="0.0001" placeholder="元/kWh" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" /></div>
                  </div>
                </div>
              </template>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">附加电价 (元/kWh)</label>
                <input v-model="batchForm.surchargePrice" type="number" step="0.0001" placeholder="输入附加电价" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              </div>
              <label class="flex items-center gap-2 text-[13px] cursor-pointer"><input v-model="batchForm.updatePrice" type="checkbox" class="styled-checkbox" />是否更新电价</label>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">备注</label>
                <textarea v-model="batchForm.remark" maxlength="500" rows="3" placeholder="填写退补原因说明" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none"></textarea>
                <span class="text-[11px] text-[#999]">{{ batchForm.remark.length }}/500</span>
              </div>
            </div>

            <!-- Right: Table list -->
            <div class="flex-1 flex flex-col overflow-hidden min-w-0">
              <div class="px-5 py-3 border-b border-[#f0f0f0] flex items-center gap-2 shrink-0">
                <span class="text-[13px] font-medium text-[#333] shrink-0">退补表计</span>
                <input
                  v-model="batchDbNoInput"
                  type="text"
                  placeholder="输入12位表号"
                  maxlength="12"
                  class="flex-1 min-w-0 py-1 px-3 border border-[#d9d9d9] rounded text-[12px] outline-none focus:border-[#3a64ab] transition-colors"
                  @keyup.enter="addBatchItem"
                />
                <button
                  class="py-1 px-3 rounded text-[12px] bg-white border border-[#d9d9d9] text-[#333] cursor-pointer hover:border-[#3a64ab] transition-colors shrink-0 disabled:opacity-40 disabled:cursor-not-allowed"
                  :disabled="batchAdding"
                  @click="addBatchItem"
                >{{ batchAdding ? '查询中...' : '+ 添加' }}</button>
              </div>
              <div class="flex-1 overflow-y-auto px-5 py-3">
                <table class="w-full border-collapse text-left">
                  <thead>
                    <tr>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0">分组</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0">表号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0">商户名</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0">时间区间</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">电量</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">原电价</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">原附加电价</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">退补电价</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">退补附加电价</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 text-right">退补金额</th>
                      <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap sticky top-0 w-[50px]">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="batchList.length === 0">
                      <td colspan="11" class="py-20 text-center text-[13px] text-[#999]">暂无数据，点击"+ 添加"添加待退补电表</td>
                    </tr>
                    <tr v-for="item in batchList" :key="item.id" class="hover:bg-[#f5f6fb]">
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ item.groupName }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono">{{ item.meterId }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ item.merchantName }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] whitespace-nowrap">{{ item.dateRange }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">{{ item.consumption }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">{{ item.originalPrice }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">{{ item.originalSurcharge }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">{{ item.adjustPrice }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">{{ item.adjustSurcharge }}</td>
                      <td class="py-2 px-2 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono font-medium" :class="parseFloat(item.adjustAmount) < 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">{{ parseFloat(item.adjustAmount) >= 0 ? '+' : '' }}{{ item.adjustAmount }}</td>
                      <td class="py-2 px-2 border-b border-[#e8e8e8] text-[12px]"><span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click="removeBatchItem(item.id)">删除</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="px-5 py-3 border-t border-[#f0f0f0] flex items-center gap-6 shrink-0">
                <div class="flex items-center gap-4 text-[12px] text-[#666]">
                  <span>共 <b class="text-[#333]">{{ batchStats.meterCount }}</b> 条</span>
                  <span>退费 <b class="text-[#ff4d4f]">{{ batchStats.refundCount }}</b> 条 <b class="text-[#ff4d4f]">{{ batchStats.refundAmt }}</b> 元</span>
                  <span>补收 <b class="text-[#52c41a]">{{ batchStats.rechargeCount }}</b> 条 <b class="text-[#52c41a]">{{ batchStats.rechargeAmt }}</b> 元</span>
                  <span>净变动 <b :class="parseFloat(batchStats.netChange) >= 0 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ parseFloat(batchStats.netChange) >= 0 ? '+' : '' }}{{ batchStats.netChange }}</b> 元</span>
                </div>
                <span class="flex-1"></span>
                <button class="py-1.5 px-4 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="batchModalOpen = false">取消</button>
                <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="batchList.length === 0 || batchSubmitting" @click="submitBatch">{{ batchSubmitting ? '执行中...' : '确定' }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Help Modal ═══ -->
    <Teleport to="body">
      <div v-if="helpModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="helpModalOpen = false">
        <div class="bg-white rounded-lg shadow-2xl w-[680px] max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center px-6 py-4 border-b border-[#f0f0f0] shrink-0">
            <span class="text-[16px] font-medium text-[#333]">余额退补说明</span>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="helpModalOpen = false">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-4 text-[13px] text-[#333] leading-[1.8] space-y-4">
            <p>余额退补菜单下包含 <b>2个</b> 功能：</p>
            <div>
              <p class="font-bold mb-2">1. 余额退补</p>
              <p>用于对历史已收取电费按新电价进行退款或补收。点击「新建余额退补」→ 配置电价 → 添加表计 → 确定执行。</p>
              <p class="mt-2"><span class="text-[#ff4d4f] font-bold">退补金额为正，代表商户要补交钱给物业，电表余额扣减；退补金额为负，代表物业要给商户退钱，电表余额增加。</span></p>
            </div>
            <div>
              <p class="font-bold mb-2">2. 余额调整</p>
              <p>用于退租时余额清零或误充电费等情况时的电表余额修正。</p>
            </div>
          </div>
          <div class="px-6 py-3 border-t border-[#f0f0f0] flex justify-end shrink-0">
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="helpModalOpen = false">知道了</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Modal 2: Single Adjustment ═══ -->
    <Teleport to="body">
      <div v-if="singleModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="singleModalOpen = false">
        <div class="bg-white rounded-xl shadow-2xl w-[520px]" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <span class="text-[16px] font-bold text-[#333]">新建余额调整</span>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="singleModalOpen = false">&times;</button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">电表类型 <span class="text-[#ff4d4f]">*</span></label>
              <select v-model="singleForm.meterType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] text-[#999]">
                <option value="">请选择电表类型</option>
                <option value="physical">物理电表</option>
                <option value="virtual">虚拟电表</option>
              </select>
            </div>
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">表号 <span class="text-[#ff4d4f]">*</span></label>
              <input v-model="singleForm.dbNo" type="text" placeholder="输入12位表号，如 202309150001" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" @input="onSingleDbNoInput" />
            </div>
            <div v-if="singleForm.currentBalance" class="bg-[#f6f8fa] rounded p-3">
              <span class="text-[13px] text-[#666]">当前余额：</span>
              <span class="text-[16px] font-bold text-[#333]">{{ singleForm.currentBalance }} 元</span>
            </div>
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">调整选项</label>
              <div class="flex gap-4">
                <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="singleForm.adjustMode" type="radio" value="set" class="styled-checkbox" /><span>余额调整</span></label>
                <label class="flex items-center gap-1.5 text-[13px] cursor-pointer"><input v-model="singleForm.adjustMode" type="radio" value="change" class="styled-checkbox" /><span>金额变动</span></label>
              </div>
            </div>
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">{{ singleForm.adjustMode === 'set' ? '请输入余额' : '请输入金额' }} <span class="text-[#ff4d4f]">*</span></label>
              <input v-model="singleForm.targetValue" type="number" step="0.01" :placeholder="singleForm.adjustMode === 'set' ? '输入目标余额' : '正数为充值，负数为扣费'" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              <span class="text-[11px] text-[#999] mt-1">{{ singleForm.adjustMode === 'change' ? '正数为充值，负数为扣费' : '直接设置电表的目标余额' }}</span>
            </div>
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">备注</label>
              <textarea v-model="singleForm.remark" maxlength="500" rows="2" placeholder="填写调整原因说明" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none"></textarea>
              <span class="text-[11px] text-[#999]">{{ singleForm.remark.length }}/500</span>
            </div>
          </div>
          <div class="p-5 border-t border-[#f0f0f0] flex justify-end gap-3">
            <button class="py-1.5 px-5 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer" @click="singleModalOpen = false">取消</button>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="singleSubmitting" @click="submitSingle">{{ singleSubmitting ? '执行中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Modal: Detail ═══ -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[680px] max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-4 border-b border-[#f0f0f0]">
            <h3 class="text-base font-medium">余额退补详情</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>
          <div v-if="detailLoading" class="p-10 text-center text-[13px] text-[#999]">加载中...</div>
          <div v-else-if="detailRow" class="p-6">
            <table class="w-full border-collapse text-[13px]">
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium w-[110px]">调整编号</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ detailRow.adjustmentNo || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium w-[110px]">退补金额</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono font-medium" :class="Number(detailRow.balance) < 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">
                  {{ Number(detailRow.balance) >= 0 ? '+' : '' }}{{ formatMoney(detailRow.balance) }}
                </td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">分组</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.groupName || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">表号</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono text-[#3a64ab]">{{ detailRow.dbNo || '--' }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">商户名</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.customerName || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">商户ID</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ detailRow.customerId || '--' }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">原电价</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatPrice(detailRow.originalPrice) }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">退补电价</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatPrice(detailRow.adjustPrice) }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">原附加</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatPrice(detailRow.originalSurcharge) }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">退补附加</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatPrice(detailRow.adjustSurcharge) }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">退补类型</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.adjustmentTypeLabel || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">支付状态</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.paymentStatus || '--' }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">扣除方式</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.deductMethod || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">扣除时间</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatTime(detailRow.deductTime) }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">操作员</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.operatorName || '--' }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">调整时间</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatTime(detailRow.adjustmentTime) }}</td>
              </tr>
              <tr>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">创建时间</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] font-mono">{{ formatTime(detailRow.createTime) }}</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8] bg-[#f5f6fb] font-medium">备注</td>
                <td class="py-2.5 px-3 border border-[#e8e8e8]">{{ detailRow.remark || detailRow.bz || '--' }}</td>
              </tr>
            </table>
          </div>
          <div class="flex justify-end gap-2.5 p-4 border-t border-[#f0f0f0]">
            <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="closeDetail">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
