<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useToast } from '@/composables/useToast.js'
import ToastMessage from '@/components/common/ToastMessage.vue'
import {
  getBalanceAdjustmentList,
  getBalanceAdjustmentSummary,
  exportBalanceAdjustment,
  executeBalanceAdjustment,
} from '@/api/finance.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

const { showToast } = useToast()

// ── 调整类型选项 ──────────────────────────────────────────────────────
const adjustTypeOptions = computed(() => [
  { value: '', label: '全部类型' },
  { value: 'INCREASE', label: '余额增加' },
  { value: 'DECREASE', label: '余额扣减' },
])

// ── 筛选条件 ──────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterAdjustType = ref('')
const filterDateStart = ref('')
const filterDateEnd = ref('')

function resetFilters() {
  filterKeyword.value = ''
  filterAdjustType.value = ''
  filterDateStart.value = ''
  filterDateEnd.value = ''
  currentPage.value = 1
  fetchData()
}

// ── 分页 ──────────────────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = ref(10)
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
    console.error('获取水表余额调整列表失败', e)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
  fetchSummary()
}

function goPage(n) {
  if (n >= 1 && n <= totalPages.value) {
    currentPage.value = n
    fetchData()
  }
}

// ── 统计 ──────────────────────────────────────────────────────────────
const summary = reactive({
  totalAmount: 0,
  adjustCount: 0,
  customerCount: 0,
  period: '',
})

async function fetchSummary() {
  try {
    const params = {}
    if (filterDateStart.value) params.startTime = filterDateStart.value + ' 00:00:00'
    if (filterDateEnd.value) params.endTime = filterDateEnd.value + ' 23:59:59'
    if (filterAdjustType.value) params.adjustmentType = filterAdjustType.value

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
      summary.period = res.data.length > 0
        ? `${res.data[0].period} ~ ${res.data[res.data.length - 1].period}`
        : ''
    } else {
      summary.totalAmount = 0
      summary.adjustCount = 0
      summary.customerCount = 0
      summary.period = ''
    }
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

// ── Row selection ────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── 格式化 ────────────────────────────────────────────────────────────
function formatMoney(n) {
  const num = Number(n)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
    let fileName = '水表余额调整记录.xlsx'
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

// ── 新建余额调整 ─────────────────────────────────────────────────────
const adjustModalOpen = ref(false)
const adjustForm = reactive({
  meterId: '',
  amount: '',
  remark: '',
})
const adjustSubmitting = ref(false)

function toggleAdjustModal() {
  adjustForm.meterId = ''
  adjustForm.amount = ''
  adjustForm.remark = ''
  adjustModalOpen.value = !adjustModalOpen.value
}

async function submitAdjust() {
  if (!adjustForm.meterId || !adjustForm.amount) {
    showToast('请填写表计ID和金额', 'error')
    return
  }
  adjustSubmitting.value = true
  try {
    await executeBalanceAdjustment({
      meterId: Number(adjustForm.meterId),
      amountChange: Number(adjustForm.amount),
      reason: adjustForm.remark || '水表余额手动调整',
      remark: adjustForm.remark || '',
      executeType: 'BALANCE_ADJUST',
      previewOnly: false,
    })
    adjustModalOpen.value = false
    showToast('余额调整执行成功', 'success')
    fetchData()
    fetchSummary()
  } catch (e) {
    console.error('余额调整执行失败', e)
    showToast('执行失败：' + (e.message || '未知错误'), 'error')
  } finally {
    adjustSubmitting.value = false
  }
}

// ── 生命周期 ────────────────────────────────────────────────────────
onMounted(() => {
  fetchData()
  fetchSummary()
})
</script>

<template>
  <div @click="selectedRowId = null">
    <ToastMessage />
    <!-- ═══ Filter Bar ═══ -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <input
          v-model="filterKeyword"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="商户名/表号"
          type="text"
          @keyup.enter="handleSearch"
        />
        <DropdownSelect v-model="filterAdjustType" :options="adjustTypeOptions" placeholder="调整类型" />
        <DateRangePicker
          v-model:start-value="filterDateStart"
          v-model:end-value="filterDateEnd"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="handleSearch">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="toggleAdjustModal">新建余额调整</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleExport">导出</button>
      </div>
    </div>

    <!-- ═══ Stats Bar ═══ -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[150px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">总笔数</div>
          <div class="text-[22px] font-bold text-[#3a64ab]">{{ summary.adjustCount }}<span class="text-[14px] font-normal text-[#999] ml-1">笔</span></div>
        </div>
        <div class="flex-1 min-w-[160px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">总调整金额</div>
          <div class="text-[22px] font-bold" :class="summary.totalAmount >= 0 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">¥ {{ formatMoney(summary.totalAmount) }}</div>
        </div>
        <div class="flex-1 min-w-[160px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">涉及商户</div>
          <div class="text-[22px] font-bold text-[#3a64ab]">{{ summary.customerCount }}<span class="text-[14px] font-normal text-[#999] ml-1">户</span></div>
        </div>
        <div v-if="summary.period" class="flex-1 min-w-[160px] p-3 bg-[#f5f6fb] rounded">
          <div class="text-[13px] text-[#666] mb-1">统计时段</div>
          <div class="text-[13px] font-medium text-[#333]">{{ summary.period }}</div>
        </div>
      </div>
    </div>

    <!-- ═══ Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h3 class="text-[15px] font-bold text-[#333]">退补明细</h3>
        <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-10 text-[13px] text-[#999]">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">调整编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">调整金额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">调整类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">调账原因</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作员</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">调整时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableData.length === 0">
              <td :colspan="11" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(row, idx) in tableData"
              :key="row.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === row.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(row.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.adjustmentNo || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.customerName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ row.dbNo || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-medium" :class="Number(row.balance) >= 0 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">
                {{ Number(row.balance) >= 0 ? '+' : '' }}{{ formatMoney(row.balance) }}
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.adjustmentTypeLabel || row.adjustmentType || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] max-w-[120px] truncate" :title="row.bz">{{ row.bz || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.operatorName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-mono">{{ formatTime(row.adjustmentTime) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap font-mono">{{ formatTime(row.createTime) }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] max-w-[160px]">
                <div class="relative group inline-block max-w-full">
                  <span class="block truncate">{{ row.remark || '--' }}</span>
                  <div v-if="row.remark && row.remark.length > 10" class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50 pointer-events-none">
                    <div class="bg-white text-[#333] text-[12px] px-2.5 py-2 rounded shadow-[0_2px_8px_rgba(0,0,0,0.15)] border border-[#e8e8e8] w-[160px] leading-[1.6]">{{ row.remark }}</div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ═══ Pagination ═══ -->
      <div class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-3 text-[13px] text-[#666]">
          <span>共<b class="text-[#3a64ab] mx-1">{{ total }}</b>条</span>
          <select v-model.number="pageSize" @change="currentPage = 1; fetchData()" class="py-1 px-2 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
        </div>
        <div class="flex items-center gap-2 text-[13px]">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <span class="text-[#666]">当前页 <b class="text-[#3a64ab]">{{ currentPage }}</b></span>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          <span class="text-[#666] ml-2">前往</span>
          <input
            class="w-[40px] py-1 px-1 border border-[#d9d9d9] rounded text-[13px] outline-none text-center"
            type="text"
            @keyup.enter="goPage(Number(($event.target).value))"
          />
          <span class="text-[#666]">页</span>
        </div>
      </div>
    </div>

    <!-- ═══ Modal: New Adjustment ═══ -->
    <Teleport to="body">
      <div v-if="adjustModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="adjustModalOpen = false">
        <div class="bg-white rounded-xl shadow-2xl w-[520px]" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <span class="text-[16px] font-bold text-[#333]">新建水表余额调整</span>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="adjustModalOpen = false">&times;</button>
          </div>

          <div class="p-5 space-y-4">
            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">水表ID <span class="text-[#ff4d4f]">*</span></label>
              <input
                v-model="adjustForm.meterId"
                type="number"
                placeholder="输入需要调整的水表ID"
                class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]"
              />
            </div>

            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">
                变动金额 <span class="text-[#ff4d4f]">*</span>
              </label>
              <input
                v-model="adjustForm.amount"
                type="number"
                step="0.01"
                placeholder="正数为充值，负数为扣费"
                class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]"
              />
              <span class="text-[11px] text-[#999] mt-1">正数为充值（增加余额），负数为扣费（减少余额）</span>
            </div>

            <div>
              <label class="text-[13px] text-[#333] block mb-1.5">备注</label>
              <textarea v-model="adjustForm.remark" maxlength="500" rows="2" placeholder="填写调整原因说明" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none"></textarea>
              <span class="text-[11px] text-[#999]">{{ adjustForm.remark.length }}/500</span>
            </div>
          </div>

          <div class="p-5 border-t border-[#f0f0f0] flex justify-end gap-3">
            <button class="py-1.5 px-5 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer" @click="adjustModalOpen = false">取消</button>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="adjustSubmitting" @click="submitAdjust">{{ adjustSubmitting ? '执行中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>
