<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getDeletedElectricList,
  getDeletedElectricDetail,
  restoreElectric,
  batchRestoreElectric,
  getElectricOperationLogs,
} from '@/api/meters.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'

// ── Dropdown options ──────────────────────────────────────────────────
const statusOptions = [
  { value: '', label: '运行状态：全部' },
  { value: '0', label: '停用' },
  { value: '1', label: '正常运行' },
  { value: '2', label: '欠费停机' },
]

// ── 筛选条件 ──────────────────────────────────────────────────────────
const filter = reactive({
  keyword: '',
  groupId: '',
  status: '',
  startTime: '',
  endTime: '',
})

function resetFilters() {
  filter.keyword = ''
  filter.groupId = ''
  filter.status = ''
  filter.startTime = ''
  filter.endTime = ''
  currentPage.value = 1
  fetchData()
}

// ── 分页 ──────────────────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

// ── Toast 通知 ──────────────────────────────────────────────────────────
const toastMsg = ref('')
const toastType = ref('success')
function showToast(msg, type = 'error') {
  toastMsg.value = msg
  toastType.value = type
  setTimeout(() => { toastMsg.value = '' }, 4000)
}

// ── 请求数据 ──────────────────────────────────────────────────────────
async function fetchData() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize,
      meterType: 1,
    }
    if (filter.keyword) params.keyword = filter.keyword
    if (filter.groupId) params.groupId = Number(filter.groupId)
    if (filter.status !== '') params.status = Number(filter.status)
    if (filter.startTime) params.startTime = filter.startTime
    if (filter.endTime) params.endTime = filter.endTime

    const res = await getDeletedElectricList(params)
    if (res && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '查询失败'
    showToast(msg, 'error')
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

// ── 行选择 ────────────────────────────────────────────────────────────
const selectedRowId = ref(null)
const selectedIds = ref(new Set())

function selectRow(id) { selectedRowId.value = id }

function toggleSelectAll(e) {
  if (e.target.checked) {
    tableData.value.forEach(m => selectedIds.value.add(m.id))
  } else {
    selectedIds.value.clear()
  }
}

function toggleMeter(id) {
  if (selectedIds.value.has(id)) selectedIds.value.delete(id)
  else selectedIds.value.add(id)
}

// ── 辅助方法 ──────────────────────────────────────────────────────────
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 19)
}

function getMeterNo(meter) {
  return meter.jicheng?.stealNo || meter.chenman?.dbNo || meter.id
}

function getGroupName(meter) {
  return meter.chenman?.proName || '-'
}

function getPowerStatus(meter) {
  const v = meter.jiexian?.stateValve
  return v === 0 ? '合闸' : v === 1 ? '拉闸' : '-'
}

function isOnline(meter) {
  return meter.general?.online === 1
}

function getCtRatio(meter) {
  const v = meter.general?.ctRatio
  return v != null ? String(v) : '-'
}

function getPtRatio(meter) {
  const v = meter.general?.ptRatio
  return v != null ? String(v) : '-'
}

function getOverdraft(meter) {
  const v = meter.chenman?.credit
  return v != null ? String(v) : '-'
}

// ── 详情弹窗 ──────────────────────────────────────────────────────────
const detailModal = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailMeterName = ref('')

async function openDetail(meter) {
  detailMeterName.value = meter.customerName || getMeterNo(meter) || ''
  detailModal.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getDeletedElectricDetail(meter.id)
    detailData.value = res.data
  } catch (e) {
    showToast('获取详情失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() { detailModal.value = false }

// ── 操作记录弹窗 ──────────────────────────────────────────────────────
const opModal = ref(false)
const opMeterId = ref(null)
const opMeterName = ref('')
const opActiveTab = ref('全部')
const opTabs = ['全部', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT', 'OTHER']
const opRecords = ref([])
const opTotal = ref(0)
const opPage = ref(1)
const opPageSize = 5
const opLoading = ref(false)

const opTotalPages = computed(() => Math.max(1, Math.ceil(opTotal.value / opPageSize)))

async function openOpRecords(meter) {
  opMeterId.value = meter.id
  opMeterName.value = meter.customerName || getMeterNo(meter) || ''
  opActiveTab.value = '全部'
  opPage.value = 1
  opModal.value = true
  await fetchOpRecords()
}

async function fetchOpRecords() {
  opLoading.value = true
  try {
    const params = { pageNum: opPage.value, pageSize: opPageSize }
    if (opActiveTab.value !== '全部') params.operationType = opActiveTab.value

    const res = await getElectricOperationLogs(opMeterId.value, params)
    if (res && res.data) {
      opRecords.value = res.data.records || []
      opTotal.value = res.data.total || 0
    }
  } catch {
    opRecords.value = []
    opTotal.value = 0
  } finally {
    opLoading.value = false
  }
}

function closeOpRecords() { opModal.value = false }

function switchOpTab(tab) {
  opActiveTab.value = tab
  opPage.value = 1
  fetchOpRecords()
}

function opGoToPage(p) {
  if (p >= 1 && p <= opTotalPages.value) {
    opPage.value = p
    fetchOpRecords()
  }
}

// ── 恢复电表 ──────────────────────────────────────────────────────────
async function handleRestore(row) {
  const name = row.customerName || getMeterNo(row)
  if (!confirm(`确定恢复电表「${name}」吗？恢复后将回到电表列表中。`)) return
  try {
    await restoreElectric(row.id)
    showToast('恢复成功', 'success')
    fetchData()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '恢复失败'
    showToast(msg, 'error')
  }
}

async function handleBatchRestore() {
  if (selectedIds.value.size === 0) {
    showToast('请先勾选需要恢复的电表', 'error')
    return
  }
  if (!confirm(`确定批量恢复 ${selectedIds.value.size} 台电表吗？`)) return
  try {
    await batchRestoreElectric([...selectedIds.value])
    showToast(`成功恢复 ${selectedIds.value.size} 台电表`, 'success')
    selectedIds.value.clear()
    fetchData()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '批量恢复失败'
    showToast(msg, 'error')
  }
}
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- Toast 通知 -->
    <Teleport to="body">
      <div
        v-if="toastMsg"
        class="fixed top-5 right-5 z-[100] py-3 px-5 rounded text-[13px] text-white shadow-lg transition-all"
        :class="toastType === 'success' ? 'bg-[#52c41a]' : 'bg-[#ff4d4f]'"
      >{{ toastMsg }}</div>
    </Teleport>

    <!-- ═══ 1. 筛选查询区 ═══ -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input
          v-model="filter.keyword"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none"
          placeholder="表号"
          type="text"
          @keyup.enter="handleSearch"
        />
        <input
          v-model="filter.groupId"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[120px] outline-none"
          placeholder="分组ID"
          type="text"
          @keyup.enter="handleSearch"
        />
        <DropdownSelect v-model="filter.status" :options="statusOptions" placeholder="运行状态" />
        <input
          v-model="filter.startTime"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[170px] outline-none"
          placeholder="删除开始时间"
          type="text"
        />
        <input
          v-model="filter.endTime"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[170px] outline-none"
          placeholder="删除结束时间"
          type="text"
        />
        <button
          class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]"
          @click="handleSearch"
        >查询</button>
        <button
          class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]"
          @click="resetFilters"
        >重置</button>
      </div>
    </div>

    <!-- ═══ 2. 统计 + 批量操作 ═══ -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5 items-center">
        <span class="py-1.5 px-3 border border-[#e2e8f0] rounded text-xs bg-[#f5f6fb] text-[#666]">
          总数: <b>{{ total }}</b> 台
        </span>
        <button
          class="py-1.5 px-4 rounded bg-[#52c41a] text-white text-[13px] cursor-pointer border-none hover:bg-[#73d13d] disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="selectedIds.size === 0"
          @click="handleBatchRestore"
        >批量恢复 ({{ selectedIds.size }})</button>
      </div>
    </div>

    <!-- ═══ 3. 数据列表 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>

        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]"><input type="checkbox" class="styled-checkbox" @change="toggleSelectAll" /></th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">供电状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">通讯状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">CT 变比</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">PT 变比</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">透支额度</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">删除时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(meter, idx) in tableData"
              :key="meter.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === meter.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(meter.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"><input type="checkbox" class="styled-checkbox" :checked="selectedIds.has(meter.id)" @change="toggleMeter(meter.id)" /></td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ getGroupName(meter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.customerName || '-' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ getMeterNo(meter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="meter.jiexian?.stateValve === 0 ? 'status-green' : 'status-red'">{{ getPowerStatus(meter) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="isOnline(meter) ? 'status-green' : 'status-red'">{{ isOnline(meter) ? '在线' : '离线' }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ getCtRatio(meter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ getPtRatio(meter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#fa8c16]">{{ getOverdraft(meter) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ formatTime(meter.updateTime) }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d] whitespace-nowrap" @click.stop="openDetail(meter)">详情</span>
                <span class="text-[#52c41a] cursor-pointer mr-2 hover:text-[#73d13d] whitespace-nowrap" @click.stop="handleRestore(meter)">恢复</span>
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] whitespace-nowrap" @click.stop="openOpRecords(meter)">操作记录</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">
          暂无已删除电表数据
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

    <!-- ═══ 4. 详情 Modal ═══ -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[60] flex items-start justify-center pt-[40px] pb-[40px]" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[860px] max-h-full overflow-y-auto flex flex-col">
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">电表详情 - {{ detailMeterName }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>

          <div class="p-5 overflow-y-auto flex-1">
            <div v-if="detailLoading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div v-else-if="!detailData" class="text-center text-[#999] text-[13px] py-8">未找到电表信息</div>

            <template v-else>
              <!-- 基本信息 -->
              <h4 class="text-[14px] font-medium mb-3 text-[#333]">基本信息</h4>
              <div class="grid grid-cols-2 gap-x-8 gap-y-3 mb-6 bg-[#f5f6fb] rounded p-4">
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">商户名称</span><span class="text-[13px] text-[#333]">{{ detailData.customerName || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">创建时间</span><span class="text-[13px] text-[#333]">{{ formatTime(detailData.createTime) }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">所属项目</span><span class="text-[13px] text-[#333]">{{ detailData.chenman?.proName || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">创建人</span><span class="text-[13px] text-[#333]">{{ detailData.createBy || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">表计类型</span><span class="text-[13px] text-[#333]">{{ detailData.jicheng?.meterTypeName || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">初始表底</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.startVal || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">档案编号</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.chenman?.dbNo || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">建档日期</span><span class="text-[13px] text-[#333]">{{ detailData.chenman?.cttime || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">安装地址</span><span class="text-[13px] text-[#333]">{{ detailData.jicheng?.address || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">倍率</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.chenman?.bei ?? '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">生产厂家</span><span class="text-[13px] text-[#333]">{{ detailData.jicheng?.vender || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">口径</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.caliber || '-' }}</span></div>
              </div>

              <!-- 状态信息 -->
              <h4 class="text-[14px] font-medium mb-3 text-[#333]">状态信息</h4>
              <div class="grid grid-cols-2 gap-x-8 gap-y-3 bg-[#f5f6fb] rounded p-4">
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">ICCID</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.iccid || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">更新时间</span><span class="text-[13px] text-[#333]">{{ formatTime(detailData.updateTime) }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">信号强度</span><span class="text-[13px] text-[#333]">{{ detailData.jiexian?.signalStrength ?? detailData.general?.meterSignal ?? '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">IMEI</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.imei || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">联系电话</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.chenman?.joinPhone || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">采集设备号</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.chenman?.collectorDeviceNo || '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">标记状态</span><span class="text-[13px] text-[#333]">{{ detailData.general?.markStatus || 'NORMAL' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">通讯状态</span><span class="text-[13px] text-[#333]">{{ detailData.general?.online === 1 ? '在线' : '离线' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">运行状态</span><span class="text-[13px] text-[#333]">{{ detailData.general?.status === 0 ? '停用' : detailData.general?.status === 1 ? '正常运行' : detailData.general?.status === 2 ? '欠费停机' : '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">CT变比</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.general?.ctRatio ?? '-' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[80px] shrink-0">PT变比</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.general?.ptRatio ?? '-' }}</span></div>
                <div class="flex col-span-2"><span class="text-[13px] text-[#999] w-[80px] shrink-0">表地址</span><span class="text-[13px] text-[#333]">{{ detailData.jiexian?.meteraddress || '-' }}</span></div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 5. 操作记录 Modal ═══ -->
    <Teleport to="body">
      <div v-if="opModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeOpRecords">
        <div class="bg-white rounded-lg shadow-2xl w-[860px] max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">操作记录 - {{ opMeterName }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeOpRecords">&times;</button>
          </div>

          <!-- Tabs -->
          <div class="px-5 pt-4 flex gap-2 flex-wrap shrink-0">
            <span
              v-for="tab in opTabs" :key="tab"
              class="py-1.5 px-3 rounded text-[13px] cursor-pointer transition-colors border"
              :class="opActiveTab === tab ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
              @click="switchOpTab(tab)"
            >{{ tab }}</span>
          </div>

          <!-- Table -->
          <div class="p-5 overflow-y-auto flex-1">
            <div class="overflow-x-auto">
              <table class="w-full border-collapse text-left">
                <thead>
                  <tr>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作人员</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作模块</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作类型</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作描述</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">IP地址</th>
                    <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="opLoading" class="text-center text-[#999] text-[13px]">
                    <td :colspan="6" class="py-6">加载中...</td>
                  </tr>
                  <tr v-else-if="opRecords.length === 0">
                    <td :colspan="6" class="py-10 text-center text-[13px] text-[#999]">暂无操作记录</td>
                  </tr>
                  <tr v-for="rec in opRecords" :key="rec.id" v-else class="hover:bg-[#f5f6fb]">
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.operatorName || '-' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.moduleName || '-' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                      <span :class="rec.operationType === 'DELETE' ? 'text-[#ff4d4f]' : ''">{{ rec.operationType || '-' }}</span>
                    </td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.operationDesc || '-' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono">{{ rec.ipAddress || '-' }}</td>
                    <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-mono whitespace-nowrap">{{ formatTime(rec.operationTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Pagination & close -->
          <div class="flex justify-between items-center p-4 border-t border-[#f0f0f0] shrink-0">
            <span class="text-[12px] text-[#666]">共 {{ opTotal }} 条</span>
            <div class="flex items-center gap-2 text-[12px]">
              <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="opPage <= 1" @click="opGoToPage(opPage - 1)">上一页</button>
              <span class="text-[#666]">{{ opPage }} / {{ opTotalPages }}</span>
              <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="opPage >= opTotalPages" @click="opGoToPage(opPage + 1)">下一页</button>
            </div>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="closeOpRecords">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
