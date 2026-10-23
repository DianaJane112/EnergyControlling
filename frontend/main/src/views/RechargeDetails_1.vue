<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getDepositLog, getDepositLogPrint, exportDepositLog } from '@/api/finance.js'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

// ── 充值类型映射 ─────────────────────────────────────────────────────
const chargeTypeLabels = {
  1: '在线充值',
  2: '人工充值',
  3: '银行转账',
  4: '其他',
}

// ── 筛选条件 ──────────────────────────────────────────────────────────
const filter = reactive({
  keyword: '',
  dateStart: '',
  dateEnd: '',
})

function resetFilters() {
  filter.keyword = ''
  filter.dateStart = ''
  filter.dateEnd = ''
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

// ── 请求数据 ──────────────────────────────────────────────────────────
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

    const res = await getDepositLog(params)
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

async function handleExport() {
  const params = {}
  if (filter.keyword) params.keyword = filter.keyword
  if (filter.dateStart) params.startTime = filter.dateStart + ' 00:00:00'
  if (filter.dateEnd) params.endTime = filter.dateEnd + ' 23:59:59'

  try {
    const res = await exportDepositLog(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const now = new Date()
    const ds = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
    a.download = `充值明细_${ds}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 导出失败静默处理
  }
}

function goToPage(p) {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p
    fetchData()
  }
}

onMounted(() => fetchData())

// ── 统计 ──────────────────────────────────────────────────────────────
const totalCount = computed(() => total.value)
const totalAmount = computed(() => tableData.value.reduce((s, r) => s + (Number(r.amount) || 0), 0))
const totalPayAmount = computed(() => tableData.value.reduce((s, r) => s + (Number(r.payAmount) || 0), 0))

// ── 行选择 ────────────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── 格式化 ────────────────────────────────────────────────────────────
function formatMoney(n) {
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

function chargeTypeLabel(v) {
  return chargeTypeLabels[v] || '未知'
}

// ── 金额转中文大写 ────────────────────────────────────────────────────
const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
const units = ['', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿']

function toChinese(num) {
  if (num === 0) return '零元整'
  let n = Math.round(num * 100)
  const jiao = Math.floor((n % 100) / 10)
  const fen = n % 10
  n = Math.floor(n / 100)
  let str = ''
  if (n === 0) str = '零'
  else {
    const parts = []
    let unitIdx = 0
    while (n > 0) {
      let seg = n % 10000
      n = Math.floor(n / 10000)
      let segStr = ''
      for (let i = 0; i < 4 && seg > 0; i++) {
        const d = seg % 10
        seg = Math.floor(seg / 10)
        if (d === 0) {
          if (segStr && segStr[0] !== '零') segStr = '零' + segStr
        } else {
          segStr = digits[d] + units[i] + segStr
        }
      }
      if (segStr) parts.push(segStr + (unitIdx > 0 ? units[3 + unitIdx] || '' : ''))
      else if (parts.length === 0) parts.push('零')
      unitIdx++
    }
    str = parts.reverse().join('')
  }
  str = '人民币' + str + '元'
  if (jiao > 0) str += digits[jiao] + '角'
  if (fen > 0) str += digits[fen] + '分'
  if (jiao === 0 && fen === 0) str += '整'
  return str
}

// ── 收据打印 Modal ────────────────────────────────────────────────────
const receiptModal = ref(false)
const receiptRow = ref(null)
const receiptLoading = ref(false)
const printDate = ref('')
const printPerson = ref('管理员')

async function openReceipt(row) {
  receiptModal.value = true
  receiptRow.value = null
  receiptLoading.value = true
  const now = new Date()
  printDate.value = now.getFullYear() + ' 年 ' + (now.getMonth() + 1) + ' 月 ' + now.getDate() + ' 日'
  try {
    const res = await getDepositLogPrint(row.id)
    receiptRow.value = res && res.data ? res.data : row
  } catch {
    receiptRow.value = row
  } finally {
    receiptLoading.value = false
  }
}
function closeReceipt() { receiptModal.value = false }
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- ═══ 1. 筛选查询区 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4 items-center">
        <input v-model="filter.keyword" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="订单号" type="text" @keyup.enter="handleSearch" />
        <DateRangePicker
          v-model:start-value="filter.dateStart"
          v-model:end-value="filter.dateEnd"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <!-- ═══ 2. 统计概览 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4 items-center">
        <div class="flex-1 min-w-[150px]">
          <div class="text-[13px] text-[#666] mb-1">记录总数</div>
          <div class="text-[22px] font-bold">{{ totalCount }} 条</div>
        </div>
        <div class="flex-1 min-w-[150px]">
          <div class="text-[13px] text-[#666] mb-1">充值金额（当前页）</div>
          <div class="text-[22px] font-bold text-[#3a64ab]">¥ {{ formatMoney(totalAmount) }}</div>
        </div>
        <div class="flex-1 min-w-[150px]">
          <div class="text-[13px] text-[#666] mb-1">实收金额（当前页）</div>
          <div class="text-[22px] font-bold text-[#52c41a]">¥ {{ formatMoney(totalPayAmount) }}</div>
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
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">订单号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">充值金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">实付金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">实收金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">余额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">充值类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作员</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">项目</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">备注</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(r, idx) in tableData"
              :key="r.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === r.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(r.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.orderNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.customerName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ r.dbNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-medium">¥ {{ formatMoney(r.amount) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">¥ {{ formatMoney(r.payAmount) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono text-[#52c41a] font-medium">¥ {{ formatMoney(r.realAmount) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">¥ {{ formatMoney(r.balance) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ chargeTypeLabel(r.chargeType) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.operatorName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.proName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] max-w-[150px] truncate" :title="r.remark">{{ r.remark }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ formatTime(r.createTime) }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] whitespace-nowrap" @click.stop="openReceipt(r)">收据打印</span>
              </td>
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

    <!-- ═══ 收据打印 Modal ═══ -->
    <Teleport to="body">
      <div v-if="receiptModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeReceipt">
        <div class="bg-white rounded-lg shadow-2xl w-[680px] max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-4 border-b border-[#f0f0f0] no-print">
            <h3 class="text-base font-medium">收据预览</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeReceipt">&times;</button>
          </div>

          <div class="p-6" v-if="receiptLoading">
            <div class="py-12 text-center text-[#3a64ab] text-[13px] font-bold animate-pulse">加载收据数据...</div>
          </div>

          <div class="p-6" v-else-if="receiptRow">
            <!-- ═══ 收据样式 ═══ -->
            <div class="receipt-wrapper" style="border: 1px solid #000; padding: 20px;">
              <div style="text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 20px;">收 据</div>

              <table style="width: 100%; border-collapse: collapse; margin-bottom: 15px; font-size: 14px;">
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">交易日期</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ formatTime(receiptRow.createTime) }}</td>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">订单编号</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ receiptRow.orderNo }}</td>
                </tr>
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">商户名称</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;" colspan="3">{{ receiptRow.customerName }}</td>
                </tr>
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">表号</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ receiptRow.dbNo }}</td>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">收款项目</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ chargeTypeLabel(receiptRow.chargeType) }}</td>
                </tr>
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">金额（大写）</td>
                  <td style="border: 1px solid #000; padding: 6px 8px; font-weight: bold;">{{ toChinese(receiptRow.amount) }}</td>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">金额（小写）</td>
                  <td style="border: 1px solid #000; padding: 6px 8px; font-weight: bold;">¥ {{ formatMoney(receiptRow.amount) }}</td>
                </tr>
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">收款方式</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ chargeTypeLabel(receiptRow.chargeType) }}</td>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">经手人</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;">{{ receiptRow.operatorName }}</td>
                </tr>
                <tr>
                  <td class="receipt-label" style="border: 1px solid #000; padding: 6px 8px; width: 120px; background: #f9f9f9;">备注</td>
                  <td style="border: 1px solid #000; padding: 6px 8px;" colspan="3">{{ receiptRow.remark }}</td>
                </tr>
              </table>

              <div style="display: flex; justify-content: space-between; margin-top: 30px; font-size: 14px;">
                <div>打印日期：{{ printDate }}</div>
                <div>打印人：{{ printPerson }}</div>
              </div>

            </div>
          </div>

          <div class="flex justify-end gap-2.5 p-4 border-t border-[#f0f0f0] no-print">
            <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="closeReceipt">关闭</button>
            <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="window.print()">打印</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style>
@media print {
  body * {
    visibility: hidden;
  }
  .receipt-wrapper,
  .receipt-wrapper * {
    visibility: visible;
  }
  .receipt-wrapper {
    position: absolute;
    left: 0;
    top: 0;
    width: 210mm;
    padding: 10mm;
    border: 1px solid #000 !important;
  }
  .no-print {
    display: none !important;
  }
}
</style>
