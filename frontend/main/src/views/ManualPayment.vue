<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { chargeDeposit } from '@/api/manualPayment.js'
import { getMeterList } from '@/api/meters.js'
import { getInstantBillDetail } from '@/api/instantBill.js'
import { getBillingTrackList } from '@/api/instantBill.js'

// ── 商户名下拉 / 搜索 ─────────────────────────────────────────────
const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref([])
const selectedCustomer = ref(null)
const selectedBalance = ref(null)
const searchNotFound = ref(false)
const loadingDetail = ref(false)

const meterOptions = ref([])
const selectedMeterId = ref('')
const loadingMeters = ref(false)

function merchantName(m) { return m?.chenman?.name || m?.customerName || '' }
function meterNo(m) { return m?.chenman?.dbNo || m?.jicheng?.stealNo || m?.id || '' }
function meterTypeName(m) { return m?.jicheng?.meterTypeName || '电表' }

async function loadMeterOptions() {
  loadingMeters.value = true
  try {
    const res = await getMeterList({ meterType: 1, pageNum: 1, pageSize: 500 })
    meterOptions.value = res.data?.records || []
  } catch {
    meterOptions.value = []
  } finally { loadingMeters.value = false }
}

async function onMerchantSelect() {
  const meter = meterOptions.value.find(m => String(m.id) === String(selectedMeterId.value))
  if (meter) await selectCustomer(meter)
}

async function searchCustomer() {
  const kw = searchQuery.value.trim()
  if (!kw) return
  searching.value = true
  searchNotFound.value = false
  try {
    // 按表计「商户名」（name 字段）模糊搜索，与表计管理里的商户名保持一致
    const res = await getMeterList({ name: kw, meterType: 1, pageSize: 5 })
    const records = res.data?.records || []
    if (records.length === 1) {
      // Auto-select if single result
      await selectCustomer(records[0])
    } else {
      searchResults.value = records
      searchNotFound.value = records.length === 0
    }
  } catch (e) {
    searchResults.value = []
    searchNotFound.value = true
  } finally { searching.value = false }
}

async function selectCustomer(meter) {
  selectedCustomer.value = meter
  selectedMeterId.value = meter?.id != null ? String(meter.id) : ''
  searchResults.value = []
  loadingDetail.value = true
  try {
    const res = await getInstantBillDetail(meter.id)
    selectedBalance.value = res.data?.currentBalance ?? null
  } catch { selectedBalance.value = null }
  finally { loadingDetail.value = false }
}

// ── Form ──────────────────────────────────────────────────────────
const amount = ref('')
const paymentMethod = ref('CASH')
const remark = ref('')
const submitted = ref(false)
const saving = ref(false)
const submitError = ref('')
const submitSuccess = ref('')

const presetAmounts = ['50', '100', '200', '300', '500', '1000']
const paymentMethods = [
  { value: 'CASH', label: '现金支付' },
  { value: 'TRANSFER', label: '对公转账' },
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
]

function setAmount(val) { amount.value = val }

function resetForm() {
  searchQuery.value = ''
  selectedCustomer.value = null
  selectedMeterId.value = ''
  selectedBalance.value = null
  searchNotFound.value = false
  searchResults.value = []
  amount.value = ''
  paymentMethod.value = 'CASH'
  remark.value = ''
  submitted.value = false
  submitError.value = ''
  submitSuccess.value = ''
}

async function submitPayment() {
  if (!selectedCustomer.value || !amount.value.trim() || Number(amount.value) <= 0) return
  submitError.value = ''
  submitSuccess.value = ''
  saving.value = true
  try {
    await chargeDeposit({
      meterId: selectedCustomer.value.id,
      customerId: selectedCustomer.value.customerId,
      amount: Number(amount.value),
      paymentMethod: paymentMethod.value,
      remark: remark.value || undefined,
    })
    submitSuccess.value = `缴费成功！已为电表 ${meterNo(selectedCustomer.value)} 充值 ¥${amount.value}，收款方式：${paymentMethods.find(m => m.value === paymentMethod.value)?.label}`
    submitted.value = true
    fetchRecords()
  } catch (e) {
    submitError.value = e?.response?.data?.msg || e?.message || '缴费失败，请重试'
  } finally { saving.value = false }
}

// ── Payment Records ────────────────────────────────────────────────
const records = ref([])
const recTotal = ref(0)
const recPage = ref(1)
const recLoading = ref(false)
const recPageSize = 10
const recTotalPages = computed(() => Math.max(1, Math.ceil(recTotal.value / recPageSize)))

async function fetchRecords() {
  recLoading.value = true
  try {
    const res = await getBillingTrackList({ pageNum: recPage.value, pageSize: recPageSize, tradeType: 5 })
    records.value = res.data?.records || []
    recTotal.value = res.data?.total || 0
  } catch { records.value = []; recTotal.value = 0 }
  finally { recLoading.value = false }
}

function recGoPage(p) {
  if (p >= 1 && p <= recTotalPages.value) { recPage.value = p; fetchRecords() }
}

onMounted(() => { fetchRecords(); loadMeterOptions() })
</script>

<template>
  <div>
    <!-- Toast -->
    <Teleport to="body">
      <div v-if="submitError" class="fixed top-5 right-5 z-[100] py-3 px-5 rounded text-[13px] text-white shadow-lg bg-[#ff4d4f]">{{ submitError }}</div>
    </Teleport>

    <!-- ═══ Recharge Form ═══ -->
    <div class="bg-white rounded p-6 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <h2 class="text-[16px] font-bold mb-5">人工充值 / 缴费</h2>

      <div class="grid grid-cols-2 gap-x-8 gap-y-4 max-w-[800px]">
        <!-- 表号 / 商户号 (search) -->
        <div class="col-span-2">
          <label class="text-[13px] text-[#666] block mb-1.5">表号 / 商户号</label>
          <div class="flex gap-2">
            <input
              v-model="searchQuery"
              class="flex-1 py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors"
              placeholder="输入电表编号或商户名搜索"
              type="text"
              @keyup.enter="searchCustomer"
            />
            <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="searchCustomer" :disabled="searching">{{ searching ? '搜索中...' : '查询' }}</button>
          </div>
          <p v-if="searchNotFound" class="text-[12px] text-[#ff4d4f] mt-1">未找到匹配的账户</p>
          <!-- search results dropdown -->
          <div v-if="searchResults.length > 0" class="mt-1 border border-[#d9d9d9] rounded bg-white shadow max-h-[200px] overflow-y-auto">
            <div
              v-for="meter in searchResults" :key="meter.id"
              class="py-2 px-3 text-[13px] cursor-pointer hover:bg-[#eef3fa] border-b border-[#f0f0f0] last:border-b-0"
              @click="selectCustomer(meter)"
            >{{ merchantName(meter) || '--' }} - {{ meterNo(meter) }} ({{ meterTypeName(meter) }})</div>
          </div>
        </div>

        <!-- 商户名（下拉选择，关联表计里写的商户名） -->
        <div>
          <label class="text-[13px] text-[#666] block mb-1.5">商户名</label>
          <select
            v-model="selectedMeterId"
            class="w-full py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab] transition-colors"
            @change="onMerchantSelect"
          >
            <option value="" disabled>{{ loadingMeters ? '加载中...' : '请选择商户名' }}</option>
            <option v-for="m in meterOptions" :key="m.id" :value="String(m.id)">
              {{ merchantName(m) || meterNo(m) }}（{{ meterNo(m) }}）
            </option>
          </select>
        </div>

        <!-- 电表号 -->
        <div>
          <label class="text-[13px] text-[#666] block mb-1.5">电表号</label>
          <input
            class="w-full py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none bg-[#f5f6fb] font-mono"
            :value="meterNo(selectedCustomer)"
            placeholder="自动填充"
            readonly type="text"
          />
        </div>

        <!-- 余额 -->
        <div>
          <label class="text-[13px] text-[#666] block mb-1.5">余额</label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-[#999]">¥</span>
            <input
              class="w-full py-2 pl-7 pr-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none bg-[#f5f6fb] font-mono"
              :class="selectedBalance !== null && selectedBalance <= 0 ? 'text-[#ff4d4f]' : 'text-[#333]'"
              :value="loadingDetail ? '加载中...' : selectedBalance !== null ? selectedBalance.toFixed(2) : '0.00'"
              placeholder="0.00"
              readonly type="text"
            />
          </div>
        </div>

        <!-- 表计类型 -->
        <div>
          <label class="text-[13px] text-[#666] block mb-1.5">表计类型</label>
          <input
            class="w-full py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none bg-[#f5f6fb]"
            :value="meterTypeName(selectedCustomer)"
            placeholder="自动填充"
            readonly type="text"
          />
        </div>

        <!-- 充值金额 -->
        <div class="col-span-2">
          <label class="text-[13px] text-[#666] block mb-1.5">
            <span class="text-[#ff4d4f]">*</span> 充值金额
          </label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[#999] text-[18px]">¥</span>
            <input
              v-model="amount"
              class="w-full py-2.5 pl-8 pr-3.5 border border-[#d9d9d9] rounded text-[20px] font-mono text-right outline-none focus:border-[#3a64ab] transition-colors"
              placeholder="0.00"
              type="text"
            />
          </div>
          <div class="flex gap-2 mt-2">
            <button
              v-for="v in presetAmounts" :key="v"
              class="py-1.5 px-3 bg-[#f5f5f5] rounded text-[13px] font-mono cursor-pointer border-none hover:bg-[#eef3fa] hover:text-[#3a64ab] transition-colors"
              @click="setAmount(v)"
            >¥{{ v }}</button>
          </div>
        </div>

        <!-- 收款方式 -->
        <div class="col-span-2">
          <label class="text-[13px] text-[#666] block mb-1.5">收款方式</label>
          <div class="flex flex-wrap gap-3">
            <label
              v-for="m in paymentMethods" :key="m.value"
              class="flex items-center gap-1.5 text-[13px] text-[#333] cursor-pointer select-none"
            >
              <input v-model="paymentMethod" :value="m.value" class="styled-checkbox" type="radio" name="paymentMethod" />
              {{ m.label }}
            </label>
          </div>
        </div>

        <!-- 备注 -->
        <div class="col-span-2">
          <label class="text-[13px] text-[#666] block mb-1.5">备注</label>
          <textarea
            v-model="remark"
            class="w-full py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors resize-none"
            rows="3" maxlength="200"
            placeholder="可填写本次充值备注信息（200 字符内）"
          ></textarea>
          <p class="text-[12px] text-[#999] text-right mt-0.5">{{ remark.length }} / 200</p>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="flex gap-3 mt-6 pt-5 border-t border-[#f0f0f0]">
        <button
          class="py-2 px-8 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!selectedCustomer || !amount.trim() || Number(amount) <= 0 || saving"
          @click="submitPayment"
        >{{ saving ? '提交中...' : '提交' }}</button>
        <button
          class="py-2 px-8 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          :disabled="saving"
          @click="resetForm"
        >重置</button>
      </div>

      <!-- success hint -->
      <div v-if="submitted && submitSuccess" class="mt-4 p-3 bg-[#f6ffed] border border-[#b7eb8f] rounded text-[13px] text-[#389e0d]">
        {{ submitSuccess }}
      </div>
    </div>

    <!-- ═══ Payment Records ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-[15px] font-bold">充值记录</h3>
        <span class="text-[12px] text-[#666]">共 {{ recTotal }} 条</span>
      </div>

      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="recLoading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>

        <table class="w-full border-collapse text-left" v-if="records.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">流水号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">交易类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动金额 (¥)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动前余额 (¥)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">变动后余额 (¥)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作员</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rec in records" :key="rec.id" class="hover:bg-[#f5f6fb]">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#3a64ab] font-mono">{{ rec.flowNo || rec.id }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ rec.operateTime || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ rec.tradeTypeName || '人工缴费' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="rec.changeAmount > 0 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ (rec.changeAmount ?? 0).toFixed(2) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ (rec.balanceBefore ?? 0).toFixed(2) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ (rec.balanceAfter ?? 0).toFixed(2) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ rec.operatorName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">{{ rec.remark || '--' }}</td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!recLoading" class="py-16 text-center text-[14px] text-[#999]">暂无充值记录</div>
      </div>

      <!-- Pagination -->
      <div v-if="recTotal > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ recTotal }} 条记录，第 {{ recPage }} / {{ recTotalPages }} 页</span>
        <div class="flex items-center gap-1.5">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="recPage <= 1" @click="recGoPage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="recPage <= 1" @click="recGoPage(recPage - 1)">上一页</button>
          <template v-for="p in recTotalPages" :key="p">
            <button v-if="recTotalPages <= 7 || p === 1 || p === recTotalPages || (p >= recPage - 1 && p <= recPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="recPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="recGoPage(p)">{{ p }}</button>
            <span v-else-if="p === recPage - 2 || p === recPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="recPage >= recTotalPages" @click="recGoPage(recPage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="recPage >= recTotalPages" @click="recGoPage(recTotalPages)">末页</button>
        </div>
      </div>
    </div>
  </div>
</template>
