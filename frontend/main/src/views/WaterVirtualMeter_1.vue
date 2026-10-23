<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useToast } from '@/composables/useToast.js'
import ToastMessage from '@/components/common/ToastMessage.vue'
import {
  getShadowMeterList, getShadowMeterStatistics, createShadowMeter,
  updateShadowMeter, deleteShadowMeter, getChildBillingTrack,
  getChildOperationLogs, getChildQrcode, getChildRechargeRecords,
} from '@/api/shadowMeter.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import MeterQrCodeModal from '@/components/meters/MeterQrCodeModal.vue'

// ── QR Code modal ─────────────────────────────────────────────────────
const qrModal = reactive({ show: false, meterId: null })

// ── QR Info modal (for context menu QR alert replacement) ──────────
const qrInfoModal = ref(false)
const qrInfo = ref({ meterNo: '', customerName: '' })

const { showToast } = useToast()

// ── Dropdown options ──────────────────────────────────────────────────
const shadowTypeOptions = [
  { value: '', label: '全部类型' },
  { value: 'AUTO', label: '自动创建' },
  { value: 'MANUAL', label: '手动创建' },
]

// ── Filters ──────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterShadowType = ref('')
const activeStat = ref('')
const statChips = [
  { label: '总数', key: null },
  { label: '启用', key: 'status', value: 1 },
  { label: '停用', key: 'status', value: 0 },
]

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  currentPage.value = 1
  fetchData()
}
function resetFilters() {
  filterKeyword.value = ''
  filterShadowType.value = ''
  activeStat.value = ''
  currentPage.value = 1
  fetchData()
}

// ── Data & Pagination ───────────────────────────────────────────────
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const stats = reactive({ totalCount: 0, activeCount: 0, inactiveCount: 0 })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value, meterType: 0 }
    if (filterKeyword.value.trim()) params.keyword = filterKeyword.value.trim()
    if (filterShadowType.value) params.shadowType = filterShadowType.value
    if (activeStat.value) {
      const chip = statChips.find(c => c.label === activeStat.value)
      if (chip && chip.key && chip.value !== undefined) params[chip.key] = chip.value
    }
    const res = await getShadowMeterList(params)
    const data = res.data
    tableData.value = (data.records || []).map(r => ({
      id: r.id,
      projectName: r.projectName || '--',
      parentMeterNo: r.parentMeterNo || '--',
      parentMeterName: r.parentMeterName || '--',
      childMeterId: r.childMeterId,
      childMeterNo: r.childMeterNo || '--',
      childMeterName: r.childMeterName || '--',
      childGroupId: r.childGroupId,
      childFeetype: r.childFeetype,
      childCredit: r.childCredit ?? 0,
      childWarnline: r.childWarnline ?? 0,
      childDisableWarning: r.childDisableWarning,
      shadowType: r.shadowType || '--',
      coefficient: r.coefficient ?? 1,
      relationDesc: r.relationDesc || '--',
      status: r.status,
      createTime: r.createTime || '--',
      raw: r,
    }))
    total.value = data.total || 0
  } catch (e) { console.error('获取虚拟水表列表失败', e); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function fetchStats() {
  try {
    const res = await getShadowMeterStatistics()
    if (res.data) Object.assign(stats, res.data)
  } catch (e) { /* silent */ }
}

function goPage(n) { if (n >= 1 && n <= totalPages.value) { currentPage.value = n; fetchData() } }

// ── Row selection ────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Context menu ─────────────────────────────────────────────────────
const contextMenu = ref({ show: false, x: 0, y: 0, row: null })
function showContext(e, row) { contextMenu.value = { show: true, x: e.clientX, y: e.clientY, row } }
function hideContext() { contextMenu.value.show = false }

// ── Delete confirm ───────────────────────────────────────────────────
const deleting = ref(false)
const deleteConfirm = ref({ show: false, row: null })
function confirmDelete(row) { deleteConfirm.value = { show: true, row }; contextMenu.value.show = false }
async function executeDelete() {
  const row = deleteConfirm.value.row
  if (!row) return
  deleting.value = true
  try { await deleteShadowMeter(row.id); deleteConfirm.value = { show: false, row: null }; fetchData(); fetchStats() }
  catch (e) { console.error('删除失败', e) } finally { deleting.value = false }
}

// ── Edit modal ───────────────────────────────────────────────────────
const editModal = ref(false)
const editingId = ref(null)
const editSaving = ref(false)
const editForm = reactive({
  projectId: '', parentMeterId: '', childMeterId: '', name: '',
  warnline: '', credit: '', feetype: '', shadowType: 'MANUAL',
  coefficient: '', relationDesc: '', effectiveDate: '', groupId: '',
  disableWarning: '', status: 1,
})
const formErrors = reactive({})

function openEdit(row) {
  editingId.value = row.id
  const r = row.raw || {}
  editForm.projectId = r.projectId ?? ''
  editForm.parentMeterId = r.parentMeterId ?? ''
  editForm.childMeterId = r.childMeterId ?? ''
  editForm.name = r.childMeterName || ''
  editForm.warnline = r.childWarnline ?? ''
  editForm.credit = r.childCredit ?? ''
  editForm.feetype = r.childFeetype ?? ''
  editForm.shadowType = r.shadowType || 'MANUAL'
  editForm.coefficient = r.coefficient ?? ''
  editForm.relationDesc = r.relationDesc || ''
  editForm.effectiveDate = r.effectiveDate || ''
  editForm.groupId = r.childGroupId ?? ''
  editForm.disableWarning = r.childDisableWarning ?? ''
  editForm.status = r.status ?? 1
  Object.keys(formErrors).forEach(k => delete formErrors[k])
  editModal.value = true
  contextMenu.value.show = false
}
function closeEdit() { editModal.value = false }
function validateForm() {
  Object.keys(formErrors).forEach(k => delete formErrors[k])
  if (!editForm.name.trim()) formErrors.name = '请输入虚拟水表名称'
  if (editForm.feetype === '' || editForm.feetype === undefined) formErrors.feetype = '请选择付费方式'
  return Object.keys(formErrors).length === 0
}
async function submitEdit() {
  if (!validateForm()) return
  editSaving.value = true
  try {
    await updateShadowMeter(editingId.value, {
      projectId: editForm.projectId || undefined,
      parentMeterId: editForm.parentMeterId || undefined,
      childMeterId: editForm.childMeterId || undefined,
      name: editForm.name || undefined,
      warnline: editForm.warnline !== '' ? Number(editForm.warnline) : undefined,
      credit: editForm.credit !== '' ? Number(editForm.credit) : undefined,
      feetype: editForm.feetype !== '' ? Number(editForm.feetype) : undefined,
      shadowType: editForm.shadowType || undefined,
      coefficient: editForm.coefficient !== '' ? Number(editForm.coefficient) : undefined,
      relationDesc: editForm.relationDesc || undefined,
      effectiveDate: editForm.effectiveDate || undefined,
      groupId: editForm.groupId !== '' ? Number(editForm.groupId) : undefined,
      disableWarning: editForm.disableWarning !== '' ? Number(editForm.disableWarning) : undefined,
      status: editForm.status,
    })
    editModal.value = false
    fetchData()
    fetchStats()
  } catch (e) { console.error('更新失败', e) } finally { editSaving.value = false }
}

// ── Create modal ─────────────────────────────────────────────────────
const createModal = ref(false)
const createSaving = ref(false)
const createForm = reactive({
  projectId: '', parentMeterId: '', childMeterId: '', name: '',
  warnline: '', credit: '', feetype: '', shadowType: 'MANUAL',
  coefficient: '1', relationDesc: '', effectiveDate: '', groupId: '',
  disableWarning: '', status: 1,
})
const createErrors = reactive({})

function openCreate() {
  Object.assign(createForm, {
    projectId: '', parentMeterId: '', childMeterId: '', name: '',
    warnline: '', credit: '', feetype: '', shadowType: 'MANUAL',
    coefficient: '1', relationDesc: '', effectiveDate: '', groupId: '',
    disableWarning: '', status: 1,
  })
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  createModal.value = true
}
function closeCreate() { createModal.value = false }
function validateCreate() {
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  if (!createForm.projectId) createErrors.projectId = '请输入项目ID'
  if (!createForm.parentMeterId) createErrors.parentMeterId = '请输入总表ID'
  if (!createForm.childMeterId) createErrors.childMeterId = '请输入分表ID'
  if (!createForm.name.trim()) createErrors.name = '请输入虚拟水表名称'
  if (!createForm.effectiveDate) createErrors.effectiveDate = '请选择生效日期'
  if (createForm.feetype === '' || createForm.feetype === undefined) createErrors.feetype = '请选择付费方式'
  return Object.keys(createErrors).length === 0
}
async function submitCreate() {
  if (!validateCreate()) return
  createSaving.value = true
  try {
    await createShadowMeter({
      projectId: Number(createForm.projectId),
      parentMeterId: Number(createForm.parentMeterId),
      childMeterId: Number(createForm.childMeterId),
      effectiveDate: createForm.effectiveDate,
      name: createForm.name || undefined,
      warnline: createForm.warnline !== '' ? Number(createForm.warnline) : undefined,
      credit: createForm.credit !== '' ? Number(createForm.credit) : undefined,
      feetype: createForm.feetype !== '' ? Number(createForm.feetype) : undefined,
      shadowType: createForm.shadowType || 'MANUAL',
      coefficient: createForm.coefficient !== '' ? Number(createForm.coefficient) : undefined,
      relationDesc: createForm.relationDesc || undefined,
      groupId: createForm.groupId !== '' ? Number(createForm.groupId) : undefined,
      disableWarning: createForm.disableWarning !== '' ? Number(createForm.disableWarning) : undefined,
      status: createForm.status,
    })
    createModal.value = false
    fetchData()
    fetchStats()
  } catch (e) { console.error('创建失败', e) } finally { createSaving.value = false }
}

// ── Sub-table modals ──────────────────────────────────────────────────
const subModal = ref({ show: false, title: '', loading: false, records: [], columns: [], childMeterId: null })

function closeSubModal() { subModal.value.show = false }

async function openRechargeRecords(row) {
  subModal.value = { show: true, title: `充值记录 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'orderNo', label: '订单号' }, { key: 'dbNo', label: '表号' }, { key: 'amount', label: '充值金额' },
    { key: 'payamount', label: '支付金额' }, { key: 'balance', label: '到账后余额' }, { key: 'czy', label: '操作员' },
    { key: 'createTime', label: '创建时间' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getChildRechargeRecords(row.childMeterId, { pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询充值记录失败', e) } finally { subModal.value.loading = false }
}

async function openBillingTrack(row) {
  subModal.value = { show: true, title: `计费明细 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'flowNo', label: '流水号' }, { key: 'tradeTypeName', label: '交易类型' },
    { key: 'balanceBefore', label: '变动前余额' }, { key: 'changeAmount', label: '变动金额' },
    { key: 'balanceAfter', label: '变动后余额' }, { key: 'operatorName', label: '操作人' },
    { key: 'operateTime', label: '操作时间' }, { key: 'remark', label: '备注' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getChildBillingTrack(row.childMeterId, { pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询计费明细失败', e) } finally { subModal.value.loading = false }
}

async function openChildOpLogs(row) {
  subModal.value = { show: true, title: `操作日志 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'operatorName', label: '操作员' }, { key: 'moduleName', label: '操作模块' },
    { key: 'operationType', label: '操作类型' }, { key: 'operationDesc', label: '操作描述' },
    { key: 'operationTime', label: '操作时间' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getChildOperationLogs(row.childMeterId, { pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询操作日志失败', e) } finally { subModal.value.loading = false }
}

async function openQrcode(row) {
  try {
    const res = await getChildQrcode(row.childMeterId)
    qrInfo.value = {
      meterNo: res.data?.jicheng?.stealNo || res.data?.chenman?.dbNo || '--',
      customerName: res.data?.customerName || '--'
    }
    qrInfoModal.value = true
  } catch (e) { console.error('获取二维码信息失败', e) }
}

// ── Helpers ──────────────────────────────────────────────────────────
function formatVal(v) { return typeof v === 'number' ? v.toFixed(2) : (v ?? '--') }
function feetypeLabel(v) { return v === 1 ? '预付费' : v === 2 ? '后付费' : '--' }
function statusLabel(v) { return v === 1 ? '启用' : '停用' }

onMounted(() => { fetchData(); fetchStats() })
</script>

<template>
  <div @click="selectedRowId = null">
    <ToastMessage />
    <!-- Filter Bar -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input v-model="filterKeyword" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none" placeholder="表号 / 商户名" type="text" @keyup.enter="currentPage = 1; fetchData()" />
        <DropdownSelect v-model="filterShadowType" :options="shadowTypeOptions" placeholder="类型" />
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="currentPage = 1; fetchData()">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- Stat Chips -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div v-for="stat in statChips" :key="stat.label" class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors" :class="activeStat === stat.label ? 'bg-[#3a64ab] border-[#3a64ab] text-white' : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'" @click="applyStatFilter(stat.label)">
          {{ stat.label }}:
          <span class="ml-1">
            <template v-if="stat.label === '总数'">{{ stats.totalCount }}</template>
            <template v-else-if="stat.label === '启用'">{{ stats.activeCount }}</template>
            <template v-else-if="stat.label === '停用'">{{ stats.inactiveCount }}</template>
          </span>
          <span v-if="stat.label !== '总数'">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- Action Bar -->
    <div class="flex justify-between items-center mb-4">
      <div class="flex gap-2.5">
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="openCreate">新增虚拟表</button>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h3 class="text-[15px] font-bold text-[#333]">虚拟水表明细</h3>
        <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
      </div>

      <div class="overflow-x-auto relative" style="min-height:150px">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>
        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">项目名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">虚拟水表名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">总表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">付费类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">透支额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">告警额</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">系数</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(meter, idx) in tableData" :key="meter.id" class="cursor-pointer transition-colors" :class="selectedRowId === meter.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'" @click.stop="selectRow(meter.id)">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.projectName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.childMeterName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ meter.childMeterNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ meter.parentMeterNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span :class="meter.childFeetype === 2 ? 'text-[#fa8c16]' : 'text-[#3a64ab]'">{{ feetypeLabel(meter.childFeetype) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#ff4d4f]">{{ formatVal(meter.childCredit) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ formatVal(meter.childWarnline) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.coefficient }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span :class="meter.status === 1 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ statusLabel(meter.status) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ meter.createTime }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d]" @click.stop="qrModal.show = true; qrModal.meterId = meter.childMeterId">二维码</span>
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d]" @click.stop="openEdit(meter)">编辑</span>
                <span class="text-[#ff4d4f] cursor-pointer mr-2 hover:text-[#ff7875]" @click.stop="confirmDelete(meter)">删除</span>
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click.stop="showContext($event, meter)">更多 &#x25BC;</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无数据</div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(1)">首页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(totalPages)">末页</button>
        </div>
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div v-if="contextMenu.show" class="fixed inset-0 z-[70]" @click="hideContext">
        <div class="absolute bg-white border border-[#e8e8e8] rounded-lg shadow-xl py-1.5 min-w-[160px]" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }" @click.stop>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openEdit(contextMenu.row)">编辑信息</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openRechargeRecords(contextMenu.row)">查询充值记录</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openChildOpLogs(contextMenu.row)">操作记录</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openQrcode(contextMenu.row)">用户二维码</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openBillingTrack(contextMenu.row)">计费明细</div>
        </div>
      </div>
    </Teleport>

    <!-- Create Modal -->
    <Teleport to="body">
      <div v-if="createModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeCreate">
        <div class="bg-white rounded-lg shadow-2xl p-6 w-[520px] max-h-[90vh] overflow-y-auto">
          <h3 class="text-base font-medium mb-5">新增虚拟水表</h3>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 项目ID</label>
            <input v-model="createForm.projectId" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.projectId ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入项目ID" type="number" />
            <p v-if="createErrors.projectId" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.projectId }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 总表ID</label>
            <input v-model="createForm.parentMeterId" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.parentMeterId ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入总表ID（上级表计）" type="number" />
            <p v-if="createErrors.parentMeterId" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.parentMeterId }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 分表ID</label>
            <input v-model="createForm.childMeterId" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.childMeterId ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入分表ID（下级表计）" type="number" />
            <p v-if="createErrors.childMeterId" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.childMeterId }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 虚拟水表名称</label>
            <input v-model="createForm.name" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.name ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入虚拟水表名称" type="text" />
            <p v-if="createErrors.name" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.name }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 生效日期</label>
            <input v-model="createForm.effectiveDate" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.effectiveDate ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="date" />
            <p v-if="createErrors.effectiveDate" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.effectiveDate }}</p>
          </div>
          <div class="mb-4 grid grid-cols-2 gap-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">告警额</label>
              <input v-model="createForm.warnline" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="元" type="number" step="0.01" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">透支额</label>
              <input v-model="createForm.credit" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="元" type="number" step="0.01" />
            </div>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 付费方式</label>
            <div class="flex gap-2">
              <button class="py-1.5 px-4 rounded border text-[13px] cursor-pointer transition-colors" :class="createForm.feetype == 1 ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="createForm.feetype = 1">预付费</button>
              <button class="py-1.5 px-4 rounded border text-[13px] cursor-pointer transition-colors" :class="createForm.feetype == 2 ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="createForm.feetype = 2">后付费</button>
            </div>
            <p v-if="createErrors.feetype" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.feetype }}</p>
          </div>
          <div class="flex gap-2.5 justify-end mt-6 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeCreate">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="createSaving" @click="submitCreate">{{ createSaving ? '创建中...' : '提交' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Edit Modal -->
    <Teleport to="body">
      <div v-if="editModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeEdit">
        <div class="bg-white rounded-lg shadow-2xl p-6 w-[520px] max-h-[90vh] overflow-y-auto">
          <h3 class="text-base font-medium mb-5">编辑虚拟水表</h3>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 虚拟水表名称</label>
            <input v-model="editForm.name" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="formErrors.name ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入虚拟水表名称" type="text" />
            <p v-if="formErrors.name" class="text-[#ff4d4f] text-[12px] mt-1">{{ formErrors.name }}</p>
          </div>
          <div class="mb-4 grid grid-cols-2 gap-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">告警额</label>
              <input v-model="editForm.warnline" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="元" type="number" step="0.01" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">透支额</label>
              <input v-model="editForm.credit" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="元" type="number" step="0.01" />
            </div>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 付费方式</label>
            <div class="flex gap-2">
              <button class="py-1.5 px-4 rounded border text-[13px] cursor-pointer transition-colors" :class="editForm.feetype == 1 ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="editForm.feetype = 1">预付费</button>
              <button class="py-1.5 px-4 rounded border text-[13px] cursor-pointer transition-colors" :class="editForm.feetype == 2 ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="editForm.feetype = 2">后付费</button>
            </div>
            <p v-if="formErrors.feetype" class="text-[#ff4d4f] text-[12px] mt-1">{{ formErrors.feetype }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1">关系描述</label>
            <input v-model="editForm.relationDesc" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如：A栋总表→101商户分表" type="text" />
          </div>
          <div class="mb-4 grid grid-cols-2 gap-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">系数</label>
              <input v-model="editForm.coefficient" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="默认1.0" type="number" step="0.01" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">分组ID</label>
              <input v-model="editForm.groupId" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="number" />
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-6 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeEdit">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="editSaving" @click="submitEdit">{{ editSaving ? '保存中...' : '提交' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirm Modal -->
    <Teleport to="body">
      <div v-if="deleteConfirm.show" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="deleteConfirm.show = false">
        <div class="bg-white rounded-lg shadow-2xl p-6 w-[400px]">
          <h3 class="text-base font-medium mb-4">确认删除</h3>
          <p class="text-[13px] text-[#666] mb-5">确定要删除虚拟水表关系 <b>{{ deleteConfirm.row?.childMeterName }}</b> 吗？</p>
          <div class="flex gap-3 justify-end">
            <button class="py-1.5 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="deleteConfirm.show = false">取消</button>
            <button class="py-1.5 px-5 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="deleting" @click="executeDelete">{{ deleting ? '删除中...' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Generic Sub-table Modal (recharge / billing / logs) -->
    <Teleport to="body">
      <div v-if="subModal.show" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeSubModal">
        <div class="bg-white rounded-lg shadow-2xl w-[860px] max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">{{ subModal.title }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeSubModal">&times;</button>
          </div>
          <div class="p-5 overflow-y-auto flex-1">
            <div v-if="subModal.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div class="overflow-x-auto" v-else-if="subModal.records.length > 0">
              <table class="w-full border-collapse text-left">
                <thead>
                  <tr>
                    <th v-for="col in subModal.columns" :key="col.key" class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">{{ col.label }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(rec, i) in subModal.records" :key="i" class="hover:bg-[#f5f6fb]">
                    <td v-for="col in subModal.columns" :key="col.key" class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]" :class="col.key === 'amount' || col.key === 'changeAmount' ? (rec[col.key] < 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]') : ''">{{ rec[col.key] ?? '--' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="text-center text-[#999] text-[13px] py-8">暂无数据</div>
          </div>
          <div class="flex justify-end p-4 border-t border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="closeSubModal">确定</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- QR Code Modal -->
    <MeterQrCodeModal v-model="qrModal.show" :meter-id="qrModal.meterId" meter-type="shadow" />

    <!-- QR Info Modal (context menu) -->
    <Teleport to="body">
      <div v-if="qrInfoModal" class="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center" @click.self="qrInfoModal = false">
        <div class="bg-white rounded-xl shadow-2xl w-[400px] p-6" @click.stop>
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-base font-medium">分表二维码信息</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="qrInfoModal = false">&times;</button>
          </div>
          <div class="space-y-3 text-[13px]">
            <div><span class="text-[#666]">表号：</span><span class="text-[#333] font-mono">{{ qrInfo.meterNo }}</span></div>
            <div><span class="text-[#666]">商户：</span><span class="text-[#333]">{{ qrInfo.customerName }}</span></div>
          </div>
          <div class="flex justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="qrInfoModal = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
