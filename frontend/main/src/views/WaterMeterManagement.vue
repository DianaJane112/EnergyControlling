<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import {
  getMeterList, getMeterDetail, getMeterReadingCurve, createMeter, updateMeter, deleteMeter, clearBalance,
  openValve, closeValve, updateMeterStatus, exportMeterList, exportMeterBatch, getWaterOperationLogs,
  batchUpdateMeterInfo, batchUpdateWaterPrice, batchSetTimeSlot,
  globalUpdateWaterPrice, batchValveControl, batchCreateMeters,
} from '@/api/meters.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import GroupSelector from '@/components/forms/GroupSelector.vue'

// ── 厂家筛选 ──────────────────────────────────────────────────────────
const vendorOptions = [
  { value: '', label: '全部厂家' },
  { value: '青岛积成电子', label: '积成' },
  { value: '捷先', label: '捷先' },
]

// ── 筛选条件 ──────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterVendor = ref('')

function resetFilters() {
  filterKeyword.value = ''
  filterVendor.value = ''
  activeStat.value = ''
  handleSearch()
}

// ── 快捷统计筛选 ──────────────────────────────────────────────────────
const activeStat = ref('')
const statChips = [
  { label: '总数', key: null },
  { label: '在线', key: 'online', value: 1 },
  { label: '离线', key: 'online', value: 0 },
  { label: '停用', key: 'status', value: 0 },
]

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  handleSearch()
}

// ── 分页 ──────────────────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value, meterType: 0 }

    if (filterKeyword.value.trim()) {
      params.name = filterKeyword.value.trim()
    }
    if (filterVendor.value) {
      params.vender = filterVendor.value
    }
    if (activeStat.value) {
      const chip = statChips.find(c => c.label === activeStat.value)
      if (chip && chip.key && chip.value !== undefined) {
        params[chip.key] = chip.value
      }
    }

    const res = await getMeterList(params)
    const data = res.data
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取水表列表失败', e)
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

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
  fetchData()
}

onMounted(() => fetchData())

function getMeterNo(row) {
  return row.jicheng?.stealNo || row.chenman?.dbNo || row.id
}

function getCustomerName(row) {
  return row.customerName || row.chenman?.name || '--'
}

function isOnline(row) {
  return row.general?.online === 1
}

function getStatusLabel(row) {
  return row.general?.status === 1 ? '正常' : '停用'
}

function getVendorType(row) {
  const jx = row.jiexian || {}
  const isJiexian = jx.companycode || jx.meterCaliber ||
    jx.controlling != null || jx.stateValve != null ||
    jx.signalStrength != null || jx.batteryVoltage != null
  if (isJiexian) return '捷先'
  if (row.jicheng?.stealNo || row.jicheng?.vender) return '积成'
  return '--'
}

function getProName(row) {
  return row.chenman?.proName || '--'
}

function getCaliber(row) {
  return row.jicheng?.caliber || row.jiexian?.meterCaliber || '--'
}

function getValveStatus(row) {
  if (row.jiexian?.stateValve !== undefined && row.jiexian?.stateValve !== '') {
    return row.jiexian.stateValve === 1 ? '关阀' : '开阀'
  }
  if (row.jicheng?.switchOp !== undefined) {
    return row.jicheng.switchOp === '2' ? '关阀' : '开阀'
  }
  return '--'
}

function getSignal(row) {
  return row.jiexian?.signalStrength ?? row.general?.meterSignal ?? '--'
}

function getBattery(row) {
  return row.jiexian?.batteryVoltage ? row.jiexian.batteryVoltage + ' V' : '--'
}

// ── 行选择 ────────────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Checkbox selection ────────────────────────────────────────────────
const selectedIds = ref(new Set())
function toggleSelectAll(e) {
  if (e.target.checked) tableData.value.forEach(m => selectedIds.value.add(m.id))
  else selectedIds.value.clear()
}
function toggleMeter(id) {
  if (selectedIds.value.has(id)) selectedIds.value.delete(id)
  else selectedIds.value.add(id)
}
const allChecked = computed({
  get: () => tableData.value.length > 0 && tableData.value.every(m => selectedIds.value.has(m.id)),
  set: val => { if (val) tableData.value.forEach(m => selectedIds.value.add(m.id)); else selectedIds.value.clear() },
})

// ── Context menu ──────────────────────────────────────────────────────
const contextMenu = ref({ show: false, x: 0, y: 0, row: null })
function showContext(e, row) { contextMenu.value = { show: true, x: e.clientX, y: e.clientY, row } }
function hideContext() { contextMenu.value.show = false }

// ── Detail dialog ─────────────────────────────────────────────────────
const detailDialog = reactive({ show: false, loading: false, data: null })
async function viewDetail(row) {
  hideContext()
  detailDialog.show = true
  detailDialog.loading = true
  detailDialog.data = null
  try {
    const res = await getMeterDetail(row.id)
    detailDialog.data = res.data
    try {
      const curve = await getMeterReadingCurve(row.id)
      const body = curve?.data ?? curve
      const points = Array.isArray(body) ? body : (body?.records || body?.list || [])
      if (points.length) detailDialog.data.latestReading = points[points.length - 1]
    } catch (e) { console.warn('获取水表实时读数失败', e) }
  } finally {
    detailDialog.loading = false
  }
}
function closeDetailDialog() { detailDialog.show = false; detailDialog.data = null }

// ── Edit drawer ───────────────────────────────────────────────────────
const drawerOpen = ref(false)
const editForm = reactive({
  id: null,
  customerName: '',
  address: '',
  vender: '',
  caliber: '',
  stateValve: undefined,
  status: 0,
})
function openEdit(row) {
  hideContext()
  editForm.id = row.id
  editForm.customerName = getCustomerName(row)
  editForm.address = row.jiexian?.meteraddress || row.jicheng?.address || ''
  editForm.vender = row.jicheng?.vender || ''
  editForm.caliber = row.jicheng?.caliber || row.jiexian?.meterCaliber || ''
  editForm.stateValve = row.jiexian?.stateValve ?? (row.jicheng?.switchOp === '2' ? 1 : 0)
  editForm.status = row.general?.status ?? 0
  drawerOpen.value = true
}
function closeDrawer() { drawerOpen.value = false }

// ── Action bubbles ────────────────────────────────────────────────────
const bubbles = ref({ batch: false, group: false })
const bubblePositions = ref({ batch: { top: '0px', left: '0px' }, group: { top: '0px', left: '0px' } })

function toggleBubble(e, name) {
  if (bubbles.value[name]) { bubbles.value[name] = false; return }
  const rect = e.currentTarget.getBoundingClientRect()
  bubblePositions.value[name] = { top: `${rect.bottom + 8}px`, left: `${rect.left}px` }
  bubbles.value[name] = true
}

function handleClickOutside(e) {
  for (const name of ['batch', 'group']) {
    if (!bubbles.value[name]) continue
    const el = document.getElementById(`wm-${name}Bubble`)
    const btn = document.getElementById(`wm-${name}Btn`)
    if (el && !el.contains(e.target) && btn && !btn.contains(e.target)) bubbles.value[name] = false
  }
}

// ── Delete confirm ────────────────────────────────────────────────────
const deleting = ref(false)
const deleteConfirm = reactive({ show: false, row: null })
function confirmDelete(row) { hideContext(); deleteConfirm.show = true; deleteConfirm.row = row }
async function executeDelete() {
  if (!deleteConfirm.row) return
  deleting.value = true
  try { await deleteMeter(deleteConfirm.row.id); deleteConfirm.show = false; fetchData() }
  catch (e) { console.error('删除失败', e) } finally { deleting.value = false }
}

// ── Valve & status actions ────────────────────────────────────────────
const valveForm = reactive({ show: false, row: null, action: 'OPEN', reason: '' })
const valving = ref(false)
function openValveDialog(row, action) {
  hideContext(); valveForm.show = true; valveForm.row = row; valveForm.action = action; valveForm.reason = ''
}
async function executeValve() {
  if (!valveForm.reason.trim() || !valveForm.row) return
  valving.value = true
  try { const fn = valveForm.action === 'OPEN' ? openValve : closeValve; await fn(valveForm.row.id, valveForm.reason.trim()); valveForm.show = false; fetchData() }
  catch (e) { console.error('阀门操作失败', e) } finally { valving.value = false }
}
async function toggleStatus(row) {
  hideContext()
  const newStatus = row.general?.status === 1 ? 0 : 1
  try { await updateMeterStatus(row.id, newStatus); fetchData() } catch (e) { console.error('状态变更失败', e) }
}

// ── Export ────────────────────────────────────────────────────────────
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try { await exportMeterList(buildSearchParams()) } catch (e) { console.error('导出失败', e) } finally { exporting.value = false }
}

async function handleBatchExport() {
  if (selectedIds.value.size === 0) return
  exporting.value = true
  try { await exportMeterBatch([...selectedIds.value]) }
  catch (e) { console.error('导出失败', e) } finally { exporting.value = false }
}

// ── Import ────────────────────────────────────────────────────────────
const importFileInput = ref(null)
const importing = ref(false)
function handleImport() { importFileInput.value?.click() }
function parseCSV(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim())
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map(h => h.trim())
  return lines.slice(1).map(line => {
    const vals = line.split(',').map(v => v.trim())
    const row = {}
    headers.forEach((h, i) => { row[h] = vals[i] || '' })
    return row
  })
}
async function onImportFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  importing.value = true
  try {
    const text = await file.text()
    const rows = parseCSV(text)
    if (rows.length === 0) { alert('文件中没有有效数据'); return }
    const meters = rows.map(r => ({
      meterType: 0,
      stealNo: r['表号'] || r['stealNo'] || undefined,
      vender: r['生产厂家'] || r['vender'] || undefined,
      caliber: r['口径'] || r['caliber'] || undefined,
      meteraddress: r['表地址'] || r['meteraddress'] || undefined,
      meterCaliber: r['表口径'] || r['meterCaliber'] || undefined,
      address: r['安装地址'] || r['address'] || undefined,
      name: r['商户名'] || r['name'] || r['商户名称'] || undefined,
      customerName: r['商户名'] || r['商户名称'] || r['name'] || undefined,
      groupId: r['分组ID'] || r['groupId'] || undefined,
      status: r['运行状态'] || r['status'] || undefined,
    }))
    await batchCreateMeters(meters)
    alert(`成功导入 ${rows.length} 条记录`)
    fetchData()
    e.target.value = ''
  } catch (err) { console.error('导入失败', err); alert('导入失败: ' + (err.message || '请检查文件格式')) }
  finally { importing.value = false }
}
function buildSearchParams() {
  const params = { pageNum: currentPage.value, pageSize: pageSize.value, meterType: 0 }
  if (filterKeyword.value.trim()) params.name = filterKeyword.value.trim()
  if (filterVendor.value) params.vender = filterVendor.value
  if (activeStat.value) { const chip = statChips.find(c => c.label === activeStat.value); if (chip && chip.key && chip.value !== undefined) params[chip.key] = chip.value }
  return params
}

// ── Operation logs ────────────────────────────────────────────────────
const opLogDialog = reactive({ show: false, loading: false, row: null, records: [], total: 0 })
async function openOpLogs(row) {
  hideContext(); opLogDialog.show = true; opLogDialog.row = row; opLogDialog.records = []
  opLogDialog.loading = true
  try { const res = await getWaterOperationLogs(row.id, { pageNum: 1, pageSize: 50 }); opLogDialog.records = res.data?.records || [] }
  catch (e) { console.error('操作日志查询失败', e) } finally { opLogDialog.loading = false }
}

// ── Batch edit info modal ─────────────────────────────────────────────
const batchEditModal = reactive({ show: false, form: { bjlx: '', ctRatio: '', ptRatio: '', markStatus: '', disableWarning: '', commStatusDesc: '', status: '', address: '' } })
const batchEditing = ref(false)
function openBatchEdit() {
  bubbles.value.batch = false
  if (selectedIds.value.size === 0) return
  batchEditModal.show = true
  Object.assign(batchEditModal.form, { bjlx: '', ctRatio: '', ptRatio: '', markStatus: '', disableWarning: '', commStatusDesc: '', status: '', address: '' })
}
async function submitBatchEdit() {
  batchEditing.value = true
  try {
    const f = batchEditModal.form; const body = { meterIds: [...selectedIds.value] }
    if (f.bjlx !== '') body.bjlx = Number(f.bjlx)
    if (f.ctRatio !== '') body.ctRatio = Number(f.ctRatio)
    if (f.ptRatio !== '') body.ptRatio = Number(f.ptRatio)
    if (f.markStatus) body.markStatus = f.markStatus
    if (f.disableWarning !== '') body.disableWarning = Number(f.disableWarning)
    if (f.commStatusDesc) body.commStatusDesc = f.commStatusDesc
    if (f.status !== '') body.status = Number(f.status)
    if (f.address) body.address = f.address
    await batchUpdateMeterInfo(body); batchEditModal.show = false; selectedIds.value.clear(); fetchData()
  } catch (e) { console.error('批量修改失败', e) } finally { batchEditing.value = false }
}

// ── Global price modal ────────────────────────────────────────────────
const globalPriceModal = reactive({ show: false, form: { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' } })
const globalPricing = ref(false)
function openGlobalPrice() { bubbles.value.group = false; globalPriceModal.show = true; Object.assign(globalPriceModal.form, { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' }) }
async function submitGlobalPrice() {
  globalPricing.value = true
  try {
    const f = globalPriceModal.form
    await globalUpdateWaterPrice({ priceType: Number(f.priceType), priceContent: f.priceContent || undefined, plusPrice: f.plusPrice !== '' ? Number(f.plusPrice) : undefined, rateConfigId: f.rateConfigId ? Number(f.rateConfigId) : undefined })
    globalPriceModal.show = false; fetchData()
  } catch (e) { console.error('全局修改水价失败', e) } finally { globalPricing.value = false }
}

// ── Global valve control modal ────────────────────────────────────────
const globalValveModal = reactive({ show: false, form: { action: 'OPEN', reason: '' } })
const globalValving = ref(false)
function openGlobalValve() { bubbles.value.group = false; globalValveModal.show = true; globalValveModal.form = { action: 'OPEN', reason: '' } }
async function submitGlobalValve() {
  globalValving.value = true
  try { await batchValveControl({ isGlobal: true, action: globalValveModal.form.action, reason: globalValveModal.form.reason.trim() }); globalValveModal.show = false; fetchData() }
  catch (e) { console.error('全局阀门操作失败', e) } finally { globalValving.value = false }
}

// ── Global time slot modal ────────────────────────────────────────────
const globalSlotModal = reactive({ show: false, form: { rateConfigId: '' } })
const globalSlotting = ref(false)
function openGlobalSlot() { bubbles.value.group = false; globalSlotModal.show = true; globalSlotModal.form.rateConfigId = '' }
async function submitGlobalSlot() {
  globalSlotting.value = true
  try { await batchSetTimeSlot({ isGlobal: true, meterType: 0, rateConfigId: Number(globalSlotModal.form.rateConfigId) }); globalSlotModal.show = false; fetchData() }
  catch (e) { console.error('全局时段设置失败', e) } finally { globalSlotting.value = false }
}

// ── Edit drawer save ──────────────────────────────────────────────────
const saving = ref(false)
async function saveEdit() {
  if (!editForm.id) return
  saving.value = true
  try {
    await updateMeter(editForm.id, {
      name: editForm.customerName || undefined,
      address: editForm.address || undefined,
      vender: editForm.vender || undefined,
      caliber: editForm.caliber || undefined,
      stateValve: editForm.stateValve !== undefined ? Number(editForm.stateValve) : undefined,
      status: editForm.status !== undefined ? Number(editForm.status) : undefined,
    })
    closeDrawer(); fetchData()
  } catch (e) { console.error('编辑水表失败', e) } finally { saving.value = false }
}

onMounted(() => { document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })

// ── 格式化 ────────────────────────────────────────────────────────────
function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

// ── Create meter modal ──────────────────────────────────────────────────
const createModalOpen = ref(false)
const creating = ref(false)
const createForm = reactive({
  customerName: '',
  // 积成
  stealNo: '', vender: '', meterTypeName: '', caliber: '', switchOp: '',
  startVal: '', terminalName: '', channelType: '', meterPhone: '',
  telecommSn: '', imei: '', iccid: '', address: '', type: '',
  // 捷先
  meteraddress: '', companycode: '', customerNo: '', meterCaliber: '',
  controlling: '', stateValve: '', signalStrength: '', batteryVoltage: '',
  // 通用
  groupId: '', status: 0, online: 1, meterSignal: '', meterType: 0,
  rateConfigId: '', ctRatio: 0, ptRatio: 0, markStatus: '',
  enabledTime: '', disableWarning: 0, commStatusDesc: '',
  // 通讯
  collectorCode: '', bpsType: '', bitType: '', protoType: '', serial: '', deviceType: '',
})

function resetCreateForm() {
  Object.assign(createForm, {
    customerName: '', stealNo: '', vender: '', meterTypeName: '', caliber: '',
    switchOp: '', startVal: '', terminalName: '', channelType: '', meterPhone: '',
    telecommSn: '', imei: '', iccid: '', address: '', type: '',
    meteraddress: '', companycode: '', customerNo: '', meterCaliber: '',
    controlling: '', stateValve: '', signalStrength: '', batteryVoltage: '',
    groupId: '', status: 0, online: 1, meterSignal: '', meterType: 0,
    rateConfigId: '', ctRatio: 0, ptRatio: 0, markStatus: '',
    enabledTime: '', disableWarning: 0, commStatusDesc: '',
    collectorCode: '', bpsType: '', bitType: '', protoType: '', serial: '', deviceType: '',
  })
}

function openCreateModal() {
  resetCreateForm()
  createModalOpen.value = true
}

async function submitCreate() {
  creating.value = true
  try {
    const n = (v) => v !== '' ? Number(v) : undefined
    const body = {
      customerName: createForm.customerName || undefined,
      stealNo: createForm.stealNo || undefined,
      vender: createForm.vender || undefined,
      meterTypeName: createForm.meterTypeName || undefined,
      caliber: createForm.caliber || undefined,
      switchOp: createForm.switchOp || undefined,
      startVal: createForm.startVal || undefined,
      terminalName: createForm.terminalName || undefined,
      channelType: createForm.channelType || undefined,
      meterPhone: createForm.meterPhone || undefined,
      telecommSn: createForm.telecommSn || undefined,
      imei: createForm.imei || undefined,
      iccid: createForm.iccid || undefined,
      address: createForm.address || undefined,
      type: createForm.type || undefined,
      meteraddress: createForm.meteraddress || undefined,
      companycode: createForm.companycode || undefined,
      customerNo: createForm.customerNo || undefined,
      meterCaliber: createForm.meterCaliber || undefined,
      controlling: n(createForm.controlling),
      stateValve: n(createForm.stateValve),
      signalStrength: n(createForm.signalStrength),
      batteryVoltage: createForm.batteryVoltage !== '' ? Number(createForm.batteryVoltage) : undefined,
      status: Number(createForm.status) || 0,
      online: Number(createForm.online) ?? 1,
      meterSignal: n(createForm.meterSignal),
      meterType: Number(createForm.meterType) ?? 0,
      groupId: createForm.groupId ? Number(createForm.groupId) : undefined,
      rateConfigId: createForm.rateConfigId ? Number(createForm.rateConfigId) : undefined,
      ctRatio: Number(createForm.ctRatio) || 0,
      ptRatio: Number(createForm.ptRatio) || 0,
      markStatus: createForm.markStatus || undefined,
      enabledTime: createForm.enabledTime || undefined,
      disableWarning: Number(createForm.disableWarning) || 0,
      commStatusDesc: createForm.commStatusDesc || undefined,
      collectorCode: createForm.collectorCode || undefined,
      bpsType: createForm.bpsType || undefined,
      bitType: createForm.bitType || undefined,
      protoType: createForm.protoType || undefined,
      serial: n(createForm.serial),
      deviceType: createForm.deviceType || undefined,
    }
    await createMeter(body)
    createModalOpen.value = false
    fetchData()
  } catch (e) {
    console.error('新增水表失败', e)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- ═══ 1. 筛选查询区 ═══ -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input v-model="filterKeyword" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none focus:border-[#3a64ab]" placeholder="商户名或表号" type="text" @keyup.enter="handleSearch" />
        <DropdownSelect v-model="filterVendor" :options="vendorOptions" placeholder="厂家" />
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="handleSearch">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- ═══ 2. 分类统计 ═══ -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div v-for="stat in statChips" :key="stat.label" class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors" :class="activeStat === stat.label ? 'bg-[#3a64ab] border-[#3a64ab] text-white' : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'" @click="applyStatFilter(stat.label)">
          {{ stat.label }}:
          <span class="ml-1">
            <template v-if="stat.label === '总数'">{{ total }}</template>
            <template v-else>{{ stat.label === '在线' || stat.label === '离线' || stat.label === '停用' ? '...' : '' }}</template>
          </span>
          <span v-if="stat.label !== '总数'">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- ═══ 3. 数据表格 ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <!-- Action Bar -->
      <div class="flex gap-2.5 mb-[15px]">
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="openCreateModal">新增表计</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="handleExport" :disabled="exporting">{{ exporting ? '导出中...' : '导出' }}</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="handleImport" :disabled="importing">{{ importing ? '导入中...' : '导入' }}</button>
        <input ref="importFileInput" type="file" accept=".csv,.txt" style="display:none" @change="onImportFile" />
        <button id="wm-batchBtn" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="toggleBubble($event, 'batch')">批量操作</button>
        <button id="wm-groupBtn" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="toggleBubble($event, 'group')">全局操作</button>
      </div>

      <div class="overflow-x-auto relative min-h-[150px]">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>

        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb] w-[50px]"><input type="checkbox" class="styled-checkbox" :checked="allChecked" @change="toggleSelectAll" /></th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">厂家类型</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">分组</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">商户名</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">表号</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">口径 / 型号</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">阀门状态</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">通讯状态</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">运行状态</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">信号强度</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">电池电压</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="meter in tableData" :key="meter.id" class="cursor-pointer transition-colors" :class="selectedRowId === meter.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'" @click.stop="selectRow(meter.id)">
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]"><input type="checkbox" class="styled-checkbox" :checked="selectedIds.has(meter.id)" @change="toggleMeter(meter.id)" /></td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="getVendorType(meter) === '积成' ? 'status-blue' : 'status-orange'">{{ getVendorType(meter) }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ getProName(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ getCustomerName(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono text-[#3a64ab]">{{ getMeterNo(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ getCaliber(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="getValveStatus(meter) === '开阀' ? 'status-green' : 'status-red'">{{ getValveStatus(meter) }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="isOnline(meter) ? 'status-green' : 'status-gray'">{{ isOnline(meter) ? '在线' : '离线' }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="getStatusLabel(meter) === '正常' ? 'status-green' : 'status-red'">{{ getStatusLabel(meter) }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ getSignal(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ getBattery(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2" @click.stop="openEdit(meter)">编辑</span>
                <span class="text-[#3a64ab] cursor-pointer" @click.stop="showContext($event, meter)">更多 &#x25BC;</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无数据</div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex items-center justify-between mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ currentPage }}/{{ totalPages }} 页</span>
        <div class="flex gap-1">
          <button class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(1)">首页</button>
          <button class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          <button class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(totalPages)">末页</button>
        </div>
      </div>
    </div>

    <!-- ═══ Context Menu ═══ -->
    <Teleport to="body">
      <div v-if="contextMenu.show" class="fixed inset-0 z-[70]" @click="hideContext">
        <div class="absolute bg-white border border-[#e8e8e8] rounded-lg shadow-xl py-1.5 min-w-[140px]" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }" @click.stop>

          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="viewDetail(contextMenu.row)">查看详情</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openValveDialog(contextMenu.row, 'OPEN')">远程开阀</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openValveDialog(contextMenu.row, 'CLOSE')">远程关阀</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="toggleStatus(contextMenu.row)">
            {{ contextMenu.row?.general?.status === 1 ? '停用表计' : '恢复运行' }}
          </div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openOpLogs(contextMenu.row)">操作日志</div>
          <div class="border-t border-[#f0f0f0]"></div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer text-[#ff4d4f] transition-colors" @click="confirmDelete(contextMenu.row)">删除表计</div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Delete Confirm ═══ -->
    <Teleport to="body">
      <div v-if="deleteConfirm.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="deleteConfirm.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6">
          <h3 class="text-base font-medium mb-4">确认删除</h3>
          <p class="text-[13px] text-[#666] mb-5">确定要删除水表 <b>{{ getMeterNo(deleteConfirm.row) }}</b> 吗？此操作为逻辑删除，可在已删除列表中恢复。</p>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="deleteConfirm.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="deleting" @click="executeDelete">{{ deleting ? '删除中...' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Edit Drawer ═══ -->
    <Teleport to="body">
      <div v-if="drawerOpen" class="fixed inset-0 bg-black/40 z-[60]" @click.self="closeDrawer">
        <div class="absolute right-0 top-0 h-full w-[400px] bg-white shadow-2xl flex flex-col">
          <div class="p-4 border-b border-[#f0f0f0] flex justify-between items-center">
            <h3 class="text-base font-medium">编辑水表信息</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999]" @click="closeDrawer">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-4 space-y-4">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">表号</label>
              <input class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] bg-[#f5f5f5] outline-none" disabled :value="editForm.id || ''" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">商户名</label>
              <input v-model="editForm.customerName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">安装地址</label>
              <input v-model="editForm.address" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">生产厂家</label>
              <input v-model="editForm.vender" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">口径</label>
              <input v-model="editForm.caliber" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">阀门状态</label>
              <select v-model.number="editForm.stateValve" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                <option :value="0">开阀</option>
                <option :value="1">关阀</option>
              </select>
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">运行状态</label>
              <select v-model.number="editForm.status" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                <option :value="0">停用</option>
                <option :value="1">正常</option>
              </select>
            </div>
          </div>
          <div class="p-4 border-t border-[#f0f0f0] flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeDrawer">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="saving" @click="saveEdit">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Detail Dialog ═══ -->
    <Teleport to="body">
      <div v-if="detailDialog.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeDetailDialog">
        <div class="bg-white rounded-lg shadow-xl w-[680px] max-h-[80vh] flex flex-col">
          <div class="px-5 py-3 border-b border-[#f0f0f0] flex justify-between items-center shrink-0">
            <h3 class="text-base font-medium">水表详情</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="closeDetailDialog">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <div v-if="detailDialog.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div v-else-if="!detailDialog.data" class="text-center text-[#999] text-[13px] py-8">未找到表计信息</div>
            <template v-else>
              <!-- 积成 -->
              <div class="mb-5">
                <h4 class="text-[14px] font-medium mb-3 pb-2 border-b border-[#f0f0f0]">积成</h4>
                <div class="grid grid-cols-2 gap-x-8 gap-y-2.5 text-[13px]">
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">厂家类型：</span><span class="status-tag" :class="getVendorType(detailDialog.data) === '积成' ? 'status-blue' : 'status-orange'">{{ getVendorType(detailDialog.data) }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">商户名称：</span><span>{{ getCustomerName(detailDialog.data) }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">表号：</span><span>{{ detailDialog.data.jicheng?.stealNo || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">生产厂家：</span><span>{{ detailDialog.data.jicheng?.vender || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">表计类型：</span><span>{{ detailDialog.data.jicheng?.meterTypeName || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">口径：</span><span>{{ detailDialog.data.jicheng?.caliber || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">开关操作：</span><span>{{ detailDialog.data.jicheng?.switchOp || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">初始值：</span><span>{{ detailDialog.data.jicheng?.startVal || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">终端名称：</span><span>{{ detailDialog.data.jicheng?.terminalName || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">通道类型：</span><span>{{ detailDialog.data.jicheng?.channelType || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">表计电话：</span><span>{{ detailDialog.data.jicheng?.meterPhone || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">IMEI：</span><span>{{ detailDialog.data.jicheng?.imei || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">ICCID：</span><span>{{ detailDialog.data.jicheng?.iccid || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">建档日期：</span><span>{{ detailDialog.data.jicheng?.createDate || '-' }}</span></div>
                  <div class="flex col-span-2"><span class="text-[#999] shrink-0 w-[100px]">安装地址：</span><span>{{ detailDialog.data.jicheng?.address || '-' }}</span></div>
                </div>
              </div>
              <!-- 捷先 -->
              <div class="mb-5">
                <h4 class="text-[14px] font-medium mb-3 pb-2 border-b border-[#f0f0f0]">捷先</h4>
                <div class="grid grid-cols-2 gap-x-8 gap-y-2.5 text-[13px]">
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">表地址：</span><span>{{ detailDialog.data.jiexian?.meteraddress || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">口径：</span><span>{{ detailDialog.data.jiexian?.meterCaliber || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">阀门状态：</span><span class="status-tag" :class="detailDialog.data.jiexian?.stateValve === 0 ? 'status-green' : 'status-red'">{{ detailDialog.data.jiexian?.stateValve === 1 ? '关阀' : detailDialog.data.jiexian?.stateValve === 0 ? '开阀' : '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">控阀状态：</span><span>{{ detailDialog.data.jiexian?.controlling ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">信号强度：</span><span>{{ detailDialog.data.jiexian?.signalStrength ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">电池电压：</span><span>{{ detailDialog.data.jiexian?.batteryVoltage ? detailDialog.data.jiexian.batteryVoltage + ' V' : '-' }}</span></div>
                </div>
              </div>
              <!-- 通用 -->
              <div class="mb-5 rounded-lg border border-[#e8edf5] p-4">
                <h4 class="text-[14px] font-medium mb-3 pb-2 border-b border-[#f0f0f0]">当前水表读数</h4>
                <div class="grid grid-cols-2 gap-3 text-[13px]">
                  <div><span class="text-[#999]">抄表时间：</span>{{ detailDialog.data.latestReading?.time || detailDialog.data.latestReading?.readTime || detailDialog.data.latestReading?.timestamp || detailDialog.data.updateTime || '-' }}</div>
                  <div><span class="text-[#999]">当前读数：</span><span class="font-mono text-[#3a64ab]">{{ detailDialog.data.latestReading?.reading ?? detailDialog.data.latestReading?.value ?? detailDialog.data.latestReading?.total ?? detailDialog.data.jicheng?.startVal ?? '-' }}</span></div>
                </div>
              </div>
              <div class="mb-5">
                <h4 class="text-[14px] font-medium mb-3 pb-2 border-b border-[#f0f0f0]">通用</h4>
                <div class="grid grid-cols-2 gap-x-8 gap-y-2.5 text-[13px]">
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">在线状态：</span><span class="status-tag" :class="detailDialog.data.general?.online === 1 ? 'status-green' : 'status-red'">{{ detailDialog.data.general?.online === 1 ? '在线' : '离线' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">运行状态：</span><span class="status-tag" :class="detailDialog.data.general?.status === 0 ? 'status-green' : 'status-red'">{{ detailDialog.data.general?.status === 1 ? '正常' : '停用' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">信号强度：</span><span>{{ detailDialog.data.general?.meterSignal ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">分组ID：</span><span>{{ detailDialog.data.general?.groupId ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">费率配置ID：</span><span>{{ detailDialog.data.general?.rateConfigId ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">禁用报警：</span><span>{{ detailDialog.data.general?.disableWarning ?? '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">创建时间：</span><span>{{ detailDialog.data.createTime || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">更新时间：</span><span>{{ formatTime(detailDialog.data.updateTime) }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">创建人：</span><span>{{ detailDialog.data.createBy || '-' }}</span></div>
                </div>
              </div>
              <!-- 通讯参数 -->
              <div>
                <h4 class="text-[14px] font-medium mb-3 pb-2 border-b border-[#f0f0f0]">通讯参数</h4>
                <div class="grid grid-cols-2 gap-x-8 gap-y-2.5 text-[13px]">
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">集中器编号：</span><span>{{ detailDialog.data.commParams?.collectorCode || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">波特率：</span><span>{{ detailDialog.data.commParams?.bpsType || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">数据位：</span><span>{{ detailDialog.data.commParams?.bitType || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[120px]">协议类型：</span><span>{{ detailDialog.data.commParams?.protoType || '-' }}</span></div>
                  <div class="flex"><span class="text-[#999] shrink-0 w-[100px]">设备类型：</span><span>{{ detailDialog.data.commParams?.deviceType || '-' }}</span></div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Batch Bubble ═══ -->
    <Teleport to="body">
      <div v-if="bubbles.batch" id="wm-batchBubble" class="fixed z-[70]" :style="{ top: bubblePositions.batch.top, left: bubblePositions.batch.left }" style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))">
        <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent border-b-white ml-5"></div>
        <div class="bg-white rounded-lg p-4 min-w-[180px]">
          <p class="text-[13px] text-[#666] mb-3">已选择 {{ selectedIds.size }} 个水表</p>
          <div class="space-y-2">
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="handleBatchExport()">批量导出</button>
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openBatchEdit">批量修改信息</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Global Bubble ═══ -->
    <Teleport to="body">
      <div v-if="bubbles.group" id="wm-groupBubble" class="fixed z-[70]" :style="{ top: bubblePositions.group.top, left: bubblePositions.group.left }" style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))">
        <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent border-b-white ml-5"></div>
        <div class="bg-white rounded-lg p-4 min-w-[200px]">
          <p class="text-[13px] text-[#ff4d4f] mb-3">全局操作将影响所有水表，请谨慎执行</p>
          <div class="space-y-2">
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openGlobalValve">自动分合闸设置</button>
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openGlobalSlot">时段设置</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create Meter Modal -->
    <Teleport to="body">
      <div v-if="createModalOpen" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="createModalOpen = false">
        <div class="bg-white rounded-lg shadow-2xl w-[720px] max-h-[85vh] flex flex-col">
          <div class="px-5 py-3 border-b border-[#f0f0f0] flex justify-between items-center shrink-0">
            <h3 class="text-base font-medium">新增水表</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="createModalOpen = false">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5 space-y-4">
            <!-- 积成 -->
            <fieldset class="border border-[#e8e8e8] rounded p-3">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">积成</legend>
              <div class="space-y-2.5">
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">表号</label>
                    <input v-model="createForm.stealNo" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">生产厂家</label>
                    <input v-model="createForm.vender" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">表计类型</label>
                    <input v-model="createForm.meterTypeName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">口径</label>
                    <input v-model="createForm.caliber" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">开关操作</label>
                    <input v-model="createForm.switchOp" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">初始值</label>
                    <input v-model="createForm.startVal" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">终端名称</label>
                    <input v-model="createForm.terminalName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">通道类型</label>
                    <input v-model="createForm.channelType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">表计电话</label>
                    <input v-model="createForm.meterPhone" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">IMEI</label>
                    <input v-model="createForm.imei" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">ICCID</label>
                    <input v-model="createForm.iccid" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">商户名称</label>
                    <input v-model="createForm.customerName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" placeholder="请输入商户名称" />
                  </div>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">安装地址</label>
                  <input v-model="createForm.address" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                </div>
              </div>
            </fieldset>

            <!-- 捷先 -->
            <fieldset class="border border-[#e8e8e8] rounded p-3">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">捷先</legend>
              <div class="space-y-2.5">
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">表地址</label>
                  <input v-model="createForm.meteraddress" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">口径</label>
                    <input v-model="createForm.meterCaliber" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">阀门状态</label>
                    <select v-model.number="createForm.stateValve" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab]">
                      <option :value="undefined">--</option>
                      <option :value="0">开阀</option>
                      <option :value="1">关阀</option>
                    </select>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">控阀状态</label>
                    <input v-model.number="createForm.controlling" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">信号强度</label>
                    <input v-model.number="createForm.signalStrength" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">电池电压</label>
                    <input v-model.number="createForm.batteryVoltage" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
              </div>
            </fieldset>

            <!-- 通用 -->
            <fieldset class="border border-[#e8e8e8] rounded p-3">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">通用</legend>
              <div class="space-y-2.5">
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">分组ID</label>
                    <GroupSelector v-model="createForm.groupId" placeholder="请选择分组" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">费率配置ID</label>
                    <input v-model="createForm.rateConfigId" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">运行状态</label>
                    <select v-model.number="createForm.status" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab]">
                      <option :value="0">正常</option>
                      <option :value="1">停用</option>
                    </select>
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">在线状态</label>
                    <select v-model.number="createForm.online" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab]">
                      <option :value="1">在线</option>
                      <option :value="0">离线</option>
                    </select>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">水表类型</label>
                    <select v-model.number="createForm.meterType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab]">
                      <option :value="0">水表</option>
                      <option :value="1">电表</option>
                    </select>
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">信号强度</label>
                    <input v-model.number="createForm.meterSignal" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
              </div>
            </fieldset>

            <!-- 通讯参数 -->
            <fieldset class="border border-[#e8e8e8] rounded p-3">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">通讯参数</legend>
              <div class="space-y-2.5">
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">集中器编号</label>
                    <input v-model="createForm.collectorCode" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">波特率</label>
                    <input v-model="createForm.bpsType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">数据位</label>
                    <input v-model="createForm.bitType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">协议类型</label>
                    <input v-model="createForm.protoType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">设备类型</label>
                    <input v-model="createForm.deviceType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
              </div>
            </fieldset>
          </div>
          <div class="p-4 border-t border-[#f0f0f0] flex gap-2.5 justify-end shrink-0">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="createModalOpen = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="creating" @click="submitCreate">{{ creating ? '创建中...' : '确认新增' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Valve Control Dialog -->
    <Teleport to="body">
      <div v-if="valveForm.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="valveForm.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[420px] p-6">
          <h3 class="text-base font-medium mb-4">{{ valveForm.action === 'OPEN' ? '远程开阀' : '远程关阀' }}</h3>
          <div class="mb-1 text-[13px] text-[#666]">表计：<span class="text-[#333]">{{ getMeterNo(valveForm.row) }}</span></div>
          <div class="mb-3">
            <label class="text-[13px] text-[#666] block mb-1">操作原因 <span class="text-[#ff4d4f]">*</span></label>
            <textarea v-model="valveForm.reason" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="请输入操作原因"></textarea>
          </div>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="valveForm.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!valveForm.reason.trim() || valving" @click="executeValve">{{ valving ? '执行中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Operation Log Dialog -->
    <Teleport to="body">
      <div v-if="opLogDialog.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="opLogDialog.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[720px] max-h-[80vh] flex flex-col">
          <div class="px-5 py-3 border-b border-[#f0f0f0] flex justify-between items-center shrink-0">
            <h3 class="text-base font-medium">操作日志 - {{ getMeterNo(opLogDialog.row) }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="opLogDialog.show = false">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <div v-if="opLogDialog.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <table v-else-if="opLogDialog.records.length > 0" class="w-full border-collapse text-left text-[13px]">
              <thead><tr>
                <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作员</th>
                <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作模块</th>
                <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作类型</th>
                <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作描述</th>
                <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作时间</th>
              </tr></thead>
              <tbody>
                <tr v-for="log in opLogDialog.records" :key="log.id" class="hover:bg-[#f5f6fb]">
                  <td class="py-2 px-2 border-b border-[#f0f0f0]">{{ log.operatorName || '-' }}</td>
                  <td class="py-2 px-2 border-b border-[#f0f0f0]">{{ log.moduleName || '-' }}</td>
                  <td class="py-2 px-2 border-b border-[#f0f0f0]">{{ log.operationType || '-' }}</td>
                  <td class="py-2 px-2 border-b border-[#f0f0f0] max-w-[200px] truncate">{{ log.operationDesc || '-' }}</td>
                  <td class="py-2 px-2 border-b border-[#f0f0f0] whitespace-nowrap">{{ log.operationTime || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="text-center text-[#999] text-[13px] py-8">暂无操作记录</div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Batch Edit Info Modal -->
    <Teleport to="body">
      <div v-if="batchEditModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchEditModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[520px] max-h-[80vh] overflow-y-auto p-6">
          <h3 class="text-base font-medium mb-4">批量修改信息（{{ selectedIds.size }} 个表计）</h3>
          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-3">
              <div><label class="text-[12px] text-[#999] block mb-1">业务类型</label>
                <select v-model="batchEditModal.form.bjlx" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white"><option value="">不修改</option><option :value="1">租户</option><option :value="2">总表</option><option :value="3">公区</option><option :value="4">楼栋</option><option :value="5">公摊</option><option :value="6">虚拟总表</option></select>
              </div>
              <div><label class="text-[12px] text-[#999] block mb-1">运行状态</label>
                <select v-model="batchEditModal.form.status" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white"><option value="">不修改</option><option :value="0">停用</option><option :value="1">正常运行</option></select>
              </div>
              <div><label class="text-[12px] text-[#999] block mb-1">标记状态</label>
                <select v-model="batchEditModal.form.markStatus" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white"><option value="">不修改</option><option value="NORMAL">正常</option><option value="ATTENTION">关注</option><option value="ABNORMAL">异常</option><option value="MAINTENANCE">维护</option></select>
              </div>
              <div><label class="text-[12px] text-[#999] block mb-1">停用告警</label>
                <select v-model="batchEditModal.form.disableWarning" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white"><option value="">不修改</option><option :value="0">告警</option><option :value="1">停用</option></select>
              </div>
            </div>
            <div><label class="text-[12px] text-[#999] block mb-1">安装地址</label><input v-model="batchEditModal.form.address" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="留空不修改" /></div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchEditModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="batchEditing" @click="submitBatchEdit">{{ batchEditing ? '提交中...' : '确认修改' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Price Modal -->
    <Teleport to="body">
      <div v-if="globalPriceModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalPriceModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[480px] p-6">
          <h3 class="text-base font-medium mb-1">全局修改水价</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有水表，请谨慎执行！</p>
          <div class="space-y-3">
            <div><label class="text-[12px] text-[#999] block mb-1">水价类型 <span class="text-[#ff4d4f]">*</span></label>
              <select v-model="globalPriceModal.form.priceType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white"><option value="">请选择</option><option :value="1">固定单价</option><option :value="2">费率单价</option><option :value="3">阶梯单价</option></select>
            </div>
            <div><label class="text-[12px] text-[#999] block mb-1">价格配置JSON</label><textarea v-model="globalPriceModal.form.priceContent" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="费率/阶梯单价时使用"></textarea></div>
            <div class="grid grid-cols-2 gap-3">
              <div><label class="text-[12px] text-[#999] block mb-1">附加费（元/吨）</label><input v-model="globalPriceModal.form.plusPrice" type="number" step="0.01" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="0" /></div>
              <div><label class="text-[12px] text-[#999] block mb-1">费率策略ID</label><input v-model="globalPriceModal.form.rateConfigId" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="可选" /></div>
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalPriceModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalPriceModal.form.priceType || globalPricing" @click="submitGlobalPrice">{{ globalPricing ? '提交中...' : '确认修改' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Valve Modal -->
    <Teleport to="body">
      <div v-if="globalValveModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalValveModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[420px] p-6">
          <h3 class="text-base font-medium mb-1">全局自动分合闸设置</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有水表，请谨慎执行！</p>
          <div class="space-y-3">
            <div><label class="text-[12px] text-[#999] block mb-1">操作类型 <span class="text-[#ff4d4f]">*</span></label>
              <div class="flex gap-3"><label class="flex items-center gap-1 text-[13px] cursor-pointer"><input v-model="globalValveModal.form.action" type="radio" value="OPEN" class="accent-[#3a64ab]" /> 开阀</label><label class="flex items-center gap-1 text-[13px] cursor-pointer"><input v-model="globalValveModal.form.action" type="radio" value="CLOSE" class="accent-[#3a64ab]" /> 关阀</label></div>
            </div>
            <div><label class="text-[12px] text-[#999] block mb-1">操作原因 <span class="text-[#ff4d4f]">*</span></label><textarea v-model="globalValveModal.form.reason" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="请输入操作原因"></textarea></div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalValveModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalValveModal.form.reason.trim() || globalValving" @click="submitGlobalValve">{{ globalValving ? '执行中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Time Slot Modal -->
    <Teleport to="body">
      <div v-if="globalSlotModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalSlotModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6">
          <h3 class="text-base font-medium mb-1">全局设置时段参数</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有水表，请谨慎执行！</p>
          <div><label class="text-[12px] text-[#999] block mb-1">费率策略ID <span class="text-[#ff4d4f]">*</span></label><input v-model="globalSlotModal.form.rateConfigId" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="请输入费率策略ID" /></div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalSlotModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalSlotModal.form.rateConfigId || globalSlotting" @click="submitGlobalSlot">{{ globalSlotting ? '提交中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
