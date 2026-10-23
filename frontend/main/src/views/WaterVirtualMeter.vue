<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getShadowMeterList, getShadowMeterStatistics, createShadowMeter, getAvailableRealWaterMeters,
  updateShadowMeter, deleteShadowMeter, getChildBillingTrack,
  getChildOperationLogs, getChildQrcode, getChildRechargeRecords,
  getRealMeters, bindRealMeters, unbindRealMeter,
} from '@/api/shadowMeter.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import GroupSelector from '@/components/forms/GroupSelector.vue'
import { getMeterOperationTracePage } from '@/api/instantBill.js'

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
      realMeterCount: r.realMeterCount ?? 0,
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
  projectName: '', parentMeterNo: '', childMeterNo: '', name: '',
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
      clearGroup: editForm.groupId === '' ? true : false,
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
const realMeterOptions = ref([])
const realMeterLoading = ref(false)
const createForm = reactive({
  parentMeterId: null, childMeterId: null,
  projectId: '', parentMeterNo: '商户型', childMeterNo: '商户型', name: '',
  warnline: '', credit: '', feetype: '', shadowType: 'MERCHANT',
  coefficient: '1', relationDesc: '', effectiveDate: '', groupId: '',
  disableWarning: '', status: 1,
})
const createErrors = reactive({})

async function openCreate() {
  Object.assign(createForm, {
    projectName: '', parentMeterId: null, childMeterId: null, parentMeterNo: '', childMeterNo: '', name: '',
    warnline: '', credit: '', feetype: '', shadowType: 'MERCHANT',
    coefficient: '1', relationDesc: '', effectiveDate: '', groupId: '',
    disableWarning: '', status: 1,
  })
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  createModal.value = true
  realMeterOptions.value = []
}

function onRealMeterSelect(kind, id) {
  const meter = realMeterOptions.value.find(m => String(m.id) === String(id))
  if (!meter) return
  const no = meter.meteraddress || meter.dbNo || meter.stealNo || ''
  if (kind === 'parent') { createForm.parentMeterId = Number(id); createForm.parentMeterNo = no }
  else { createForm.childMeterId = Number(id); createForm.childMeterNo = no; if (!createForm.name) createForm.name = meter.name || '' }
}

function onCreateGroupChange(node) {
  createForm.groupId = node?.id ?? ''
  createForm.projectId = node?.projectId ?? node?.project?.id ?? node?.merchantId ?? ''
  createForm.projectName = node?.projectName ?? node?.project?.projectName ?? node?.merchantName ?? ''
}
function closeCreate() { createModal.value = false }
function validateCreate() {
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  if (createForm.groupId === '' || createForm.groupId == null) createErrors.groupId = '请选择分组'
  return Object.keys(createErrors).length === 0
}
async function submitCreate() {
  if (!validateCreate()) return
  createSaving.value = true
  try {
    await createShadowMeter({
      projectName: createForm.projectName.trim() || undefined,
      projectId: createForm.projectId !== '' ? Number(createForm.projectId) : undefined,
      parentMeterId: createForm.parentMeterId || undefined,
      childMeterId: createForm.childMeterId || undefined,
      parentMeterNo: createForm.parentMeterNo || undefined,
      childMeterNo: createForm.childMeterNo || undefined,
      meterType: 0,
      effectiveDate: createForm.effectiveDate || undefined,
      name: createForm.name || undefined,
      warnline: createForm.warnline !== '' ? Number(createForm.warnline) : undefined,
      credit: createForm.credit !== '' ? Number(createForm.credit) : undefined,
      feetype: createForm.feetype !== '' ? Number(createForm.feetype) : undefined,
      shadowType: 'MERCHANT',
      coefficient: createForm.coefficient !== '' ? Number(createForm.coefficient) : undefined,
      relationDesc: createForm.relationDesc || undefined,
      groupId: createForm.groupId !== '' ? Number(createForm.groupId) : undefined,
      disableWarning: createForm.disableWarning !== '' ? Number(createForm.disableWarning) : undefined,
      status: createForm.status,
    })
    createModal.value = false
    fetchData()
    fetchStats()
  } catch (e) { console.error('创建失败', e); alert(e?.response?.data?.msg || e?.response?.data?.message || e?.message || '创建失败，请检查网络或后端日志') } finally { createSaving.value = false }
}

// ── Sub-table modals ──────────────────────────────────────────────────
const subModal = ref({ show: false, title: '', loading: false, records: [], columns: [], childMeterId: null })

function closeSubModal() { subModal.value.show = false }

async function openRechargeRecords(row) {
  subModal.value = { show: true, title: `充值记录 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'eventType', label: '事件' }, { key: 'operationTerminal', label: '操作端' }, { key: 'balanceBefore', label: '充值前余额' }, { key: 'creditedAmount', label: '到账金额' }, { key: 'changeAmount', label: '变动金额' }, { key: 'balanceAfter', label: '充值后余额' }, { key: 'operatorName', label: '操作人' }, { key: 'operationTime', label: '时间' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getMeterOperationTracePage({ objectId: row.id, eventType: 'RECHARGE', pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询充值记录失败', e) } finally { subModal.value.loading = false }
}

async function openBillingTrack(row) {
  subModal.value = { show: true, title: `计费明细 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'eventType', label: '事件' }, { key: 'rawMeasurement', label: '原始计量' }, { key: 'billedMeasurement', label: '计费计量' }, { key: 'consumption', label: '用量' }, { key: 'changeAmount', label: '变动金额' }, { key: 'balanceAfter', label: '计费后余额' }, { key: 'resultStatus', label: '结果' }, { key: 'operationTime', label: '时间' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getMeterOperationTracePage({ objectId: row.id, eventType: 'BILLING_SUCCESS', pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询计费明细失败', e) } finally { subModal.value.loading = false }
}

async function openChildOpLogs(row) {
  subModal.value = { show: true, title: `操作日志 - ${row.childMeterName}`, loading: true, records: [], columns: [
    { key: 'eventType', label: '事件' }, { key: 'operationTerminal', label: '操作端' }, { key: 'operatorName', label: '操作人' }, { key: 'remark', label: '详情' }, { key: 'resultStatus', label: '结果' }, { key: 'operationTime', label: '时间' },
  ], childMeterId: row.childMeterId }
  hideContext()
  try {
    const res = await getMeterOperationTracePage({ objectId: row.id, pageNum: 1, pageSize: 50 })
    subModal.value.records = res.data?.records || []
  } catch (e) { console.error('查询操作日志失败', e) } finally { subModal.value.loading = false }
}

async function openQrcode(row) {
  try {
    const res = await getChildQrcode(row.childMeterId)
    alert(`分表二维码信息:\n表号: ${res.data?.jicheng?.stealNo || res.data?.chenman?.dbNo || '--'}\n商户: ${res.data?.customerName || '--'}`)
  } catch (e) { console.error('获取二维码信息失败', e) }
}

// ── Helpers ──────────────────────────────────────────────────────────
function formatVal(v) { return typeof v === 'number' ? v.toFixed(2) : (v ?? '--') }
function feetypeLabel(v) { return v === 1 ? '预付费' : v === 2 ? '后付费' : '--' }
function statusLabel(v) { return v === 1 ? '启用' : '停用' }

// ── 关联真实水表（水表数详情弹窗） ─────────────────────────────────────
const realMetersModal = ref({ show: false, loading: false, virtualMeterId: null, virtualMeterName: '', records: [] })

async function openRealMeters(meter) {
  realMetersModal.value = { show: true, loading: true, virtualMeterId: meter.childMeterId, virtualMeterName: meter.childMeterName, records: [] }
  try {
    const res = await getRealMeters(meter.childMeterId)
    realMetersModal.value.records = res.data || []
  } catch (e) { console.error('查询关联真实水表失败', e); realMetersModal.value.records = [] }
  finally { realMetersModal.value.loading = false }
}
function closeRealMeters() { realMetersModal.value.show = false }

async function refreshRealMeters() {
  realMetersModal.value.loading = true
  try {
    const res = await getRealMeters(realMetersModal.value.virtualMeterId)
    realMetersModal.value.records = res.data || []
  } catch (e) { console.error('查询关联真实水表失败', e) }
  finally { realMetersModal.value.loading = false }
}

async function unbindReal(meterRow) {
  if (!window.confirm(`确定解除真实水表 "${meterRow.meterName || meterRow.meterNo}" 的关联吗？`)) return
  try {
    await unbindRealMeter(meterRow.relationId)
    fetchData(); fetchStats()
    await refreshRealMeters()
  } catch (e) { console.error('解除关联失败', e); alert(e?.response?.data?.msg || e?.response?.data?.message || e?.message || '解除关联失败') }
}

// ── 新增关联水表 ────────────────────────────────────────────────────────
const bindModal = ref({
  show: false, loading: false, saving: false,
  keyword: '', groupId: '',
  records: [], total: 0, pageNum: 1, pageSize: 10,
  selectedIds: [],
})

function openBind() {
  bindModal.value = { show: true, loading: false, saving: false, keyword: '', groupId: '', records: [], total: 0, pageNum: 1, pageSize: 10, selectedIds: [] }
  fetchAvailableRealMeters()
}
function closeBind() { bindModal.value.show = false }
function resetBindSearch() {
  bindModal.value.keyword = ''
  bindModal.value.groupId = ''
  bindModal.value.pageNum = 1
  fetchAvailableRealMeters()
}
function searchBind() { bindModal.value.pageNum = 1; fetchAvailableRealMeters() }
async function fetchAvailableRealMeters() {
  bindModal.value.loading = true
  try {
    const params = { pageNum: bindModal.value.pageNum, pageSize: bindModal.value.pageSize }
    if (bindModal.value.keyword.trim()) params.name = bindModal.value.keyword.trim()
    if (bindModal.value.groupId) params.groupId = bindModal.value.groupId
    const res = await getAvailableRealWaterMeters(params)
    const data = res.data || {}
    bindModal.value.records = data.records || []
    bindModal.value.total = data.total || 0
  } catch (e) { console.error('查询可关联水表失败', e); bindModal.value.records = []; bindModal.value.total = 0 }
  finally { bindModal.value.loading = false }
}
function toggleSelectMeter(id) {
  const s = bindModal.value.selectedIds
  const i = s.indexOf(id)
  if (i >= 0) s.splice(i, 1); else s.push(id)
}
function isMeterSelected(id) { return bindModal.value.selectedIds.includes(id) }
async function submitBind() {
  if (!bindModal.value.selectedIds.length) { alert('请先选择要关联的水表'); return }
  bindModal.value.saving = true
  try {
    await bindRealMeters(realMetersModal.value.virtualMeterId, bindModal.value.selectedIds)
    bindModal.value.show = false
    fetchData(); fetchStats()
    await refreshRealMeters()
  } catch (e) { console.error('关联水表失败', e); alert(e?.response?.data?.msg || e?.response?.data?.message || e?.message || '关联失败') }
  finally { bindModal.value.saving = false }
}

function markStatusLabel(v) {
  return { NORMAL: '正常', ATTENTION: '关注', ABNORMAL: '异常', MAINTENANCE: '维护' }[v] || v || '--'
}
function valveStatusLabel(v) {
  return v === 0 ? '开阀' : v === 1 ? '关阀' : '--'
}
function onlineLabel(v) {
  return v === 1 ? '在线' : v === 0 ? '离线' : '--'
}

onMounted(() => { fetchData(); fetchStats() })
</script>

<template>
  <div @click="selectedRowId = null">
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
            <template v-else>{{ stat.label === '启用' || stat.label === '停用' ? '...' : '' }}</template>
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
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">水表数</th>
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
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-center">
                <span class="inline-block min-w-[24px] text-[#3a64ab] cursor-pointer hover:underline" @click.stop="openRealMeters(meter)">{{ meter.realMeterCount }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
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
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 选择分组</label>
            <GroupSelector v-model="createForm.groupId" placeholder="请选择分组" @change="onCreateGroupChange" />
            <p v-if="createErrors.groupId" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.groupId }}</p>
          </div>
          <div v-if="false" class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 总表表号</label>
            <select :value="createForm.parentMeterId" @change="onRealMeterSelect('parent', $event.target.value)" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none"><option value="">{{ realMeterLoading ? '加载中...' : '选择真实总表' }}</option><option v-for="m in realMeterOptions" :key="m.id" :value="m.id">{{ m.meteraddress || m.dbNo || m.stealNo }} - {{ m.name || '未命名商户' }}</option></select>
            <p v-if="createErrors.parentMeterNo" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.parentMeterNo }}</p>
          </div>
          <div v-if="false" class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 分表表号</label>
            <select :value="createForm.childMeterId" @change="onRealMeterSelect('child', $event.target.value)" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none"><option value="">选择真实分表</option><option v-for="m in realMeterOptions" :key="m.id" :value="m.id">{{ m.meteraddress || m.dbNo || m.stealNo }} - {{ m.name || '未命名商户' }}</option></select>
            <p v-if="createErrors.childMeterNo" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.childMeterNo }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1">虚拟水表名称（可选）</label>
            <input v-model="createForm.name" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none" :class="createErrors.name ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入虚拟水表名称" type="text" />
            <p v-if="createErrors.name" class="text-[#ff4d4f] text-[12px] mt-1">{{ createErrors.name }}</p>
          </div>
          <div class="mb-4">
            <label class="text-[13px] text-[#666] block mb-1">生效日期（可选，默认今天）</label>
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
            <label class="text-[13px] text-[#666] block mb-1">付费方式（可选）</label>
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
              <GroupSelector v-model="editForm.groupId" placeholder="请选择分组" />
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

    <!-- 关联真实水表弹窗 -->
    <Teleport to="body">
      <div v-if="realMetersModal.show" class="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center" @click.self="closeRealMeters">
        <div class="bg-white rounded-lg shadow-2xl w-[960px] max-w-[calc(100vw-32px)] max-h-[85vh] flex flex-col">
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">关联真实水表 - {{ realMetersModal.virtualMeterName || '--' }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeRealMeters">&times;</button>
          </div>
          <div class="p-4 border-b border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="openBind">新增关联水表</button>
          </div>
          <div class="p-5 overflow-auto flex-1">
            <div v-if="realMetersModal.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div class="overflow-x-auto" v-else-if="realMetersModal.records.length > 0">
              <table class="w-full border-collapse text-left">
                <thead>
                  <tr>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">序号</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">标记状态</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">阀门状态</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">通讯状态</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">状态更新时间</th>
                    <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(rec, idx) in realMetersModal.records" :key="rec.relationId" class="hover:bg-[#f5f6fb]">
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">{{ idx + 1 }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">{{ rec.groupName || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">{{ rec.meterName || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ rec.meterNo || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">{{ markStatusLabel(rec.markStatus) }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">
                      <span class="status-tag" :class="rec.powerStatus === 0 ? 'status-green' : rec.powerStatus === 1 ? 'status-red' : 'status-gray'">{{ valveStatusLabel(rec.powerStatus) }}</span>
                    </td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">
                      <span class="status-tag" :class="rec.online === 1 ? 'status-green' : 'status-gray'">{{ onlineLabel(rec.online) }}</span>
                    </td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ rec.updateTime || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[13px]">
                      <span class="text-[#ff4d4f] cursor-pointer hover:text-[#ff7875]" @click="unbindReal(rec)">删除</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="text-center text-[#999] text-[13px] py-10">暂无关联真实水表</div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 新增关联水表弹窗 -->
    <Teleport to="body">
      <div v-if="bindModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeBind">
        <div class="bg-white rounded-lg shadow-2xl w-[760px] max-w-[calc(100vw-32px)] max-h-[85vh] flex flex-col">
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">新增关联水表</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeBind">&times;</button>
          </div>
          <div class="p-4 border-b border-[#f0f0f0] shrink-0">
            <div class="flex flex-wrap gap-3 items-center">
              <input v-model="bindModal.keyword" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none" placeholder="商户名称/表号" type="text" @keyup.enter="searchBind" />
              <GroupSelector v-model="bindModal.groupId" placeholder="请选择分组" :show-manage-link="false" />
              <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="searchBind">查询</button>
              <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetBindSearch">重置</button>
            </div>
          </div>
          <div class="p-5 overflow-auto flex-1">
            <div v-if="bindModal.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div class="overflow-x-auto" v-else-if="bindModal.records.length > 0">
              <table class="w-full border-collapse text-left">
                <thead>
                  <tr>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]"></th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
                    <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="meter in bindModal.records" :key="meter.id" class="hover:bg-[#f5f6fb]">
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]"><input type="checkbox" class="styled-checkbox" :checked="isMeterSelected(meter.id)" @change="toggleSelectMeter(meter.id)" /></td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ meter.jiexian?.meteraddress || meter.chenman?.dbNo || meter.jicheng?.stealNo || meter.id }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.customerName || meter.chenman?.name || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[13px]">{{ meter.general?.groupName || '--' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="text-center text-[#999] text-[13px] py-10">暂无可选水表</div>
          </div>
          <div class="flex justify-end gap-2.5 p-4 border-t border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeBind">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="bindModal.saving" @click="submitBind">{{ bindModal.saving ? '关联中...' : '关联' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
