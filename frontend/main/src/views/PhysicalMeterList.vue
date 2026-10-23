<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import {
  getMeterList, getMeterDetail, createMeter, updateMeter, deleteMeter, clearBalance,
  openValve, closeValve, updateMeterStatus, exportMeterList, exportMeterBatch, getElectricOperationLogs,
  batchUpdateMeterInfo, batchUpdateElectricityPrice, batchSetTimeSlot,
  globalUpdateElectricityPrice, batchValveControl, batchCreateMeters, getMeterReadingCurve,
} from '@/api/meters.js'
import { useAuth } from '@/stores/auth.js'
import { getPriceStrategyPage } from '@/api/priceStrategy.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import GroupSelector from '@/components/forms/GroupSelector.vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

// ── Auth ──────────────────────────────────────────────────────────────
const { user } = useAuth()
const isAdmin = computed(() => {
  return user.value?.role?.toUpperCase() === 'ADMIN'
})

// ── Clear balance dialog ──────────────────────────────────────────────
const clearDialog = reactive({ show: false, row: null, reason: '' })
function openClearDialog(row) {
  clearDialog.row = row
  clearDialog.reason = ''
  clearDialog.show = true
}
function closeClearDialog() {
  clearDialog.show = false
}
async function submitClearBalance() {
  if (!clearDialog.reason.trim()) return
  try {
    await clearBalance(clearDialog.row.id, clearDialog.reason.trim())
    closeClearDialog()
    fetchMeters()
  } catch (e) {
    console.error('余额清零失败', e)
  }
}

// ── Dropdown options ──────────────────────────────────────────────────
const typeOptions = [
  { value: '公共表', label: '公共表' },
  { value: '商户', label: '商户' },
  { value: '总表', label: '总表' },
]
const venderOptions = [
  { value: '青岛积成电子', label: '青岛积成电子' },
  { value: '春城水表厂', label: '春城水表厂' },
  { value: '江苏远传', label: '江苏远传' },
  { value: '宁波水表', label: '宁波水表' },
]
const bjlxOptions = [
  { value: '1', label: '租户' },
  { value: '2', label: '总表' },
  { value: '3', label: '公区' },
  { value: '4', label: '楼栋' },
  { value: '5', label: '公摊' },
  { value: '6', label: '虚拟总表' },
]
const feetypeOptions = [
  { value: '1', label: '预付费' },
  { value: '2', label: '后付费' },
]
const priceTypeOptions = [
  { value: '1', label: '固定单价' },
  { value: '2', label: '费率单价' },
  { value: '3', label: '阶梯单价' },
]
const phaseOptions = [
  { value: '1', label: '单相' },
  { value: '3', label: '三相' },
]
// Options used by the edit form. Values follow the backend bjlx contract.
const editMarkOptions = [
  { value: '1', label: '商户' },
  { value: '5', label: '分摊表' },
  { value: '2', label: '总表' },
  { value: '3', label: '公共表' },
  { value: '4', label: '其他' },
  { value: '6', label: '公区总表' },
]

const rateConfigOptions = ref([])
async function loadRateConfigOptions() {
  try {
    const res = await getPriceStrategyPage({ pageNum: 1, pageSize: 100, meterType: 1 })
    const records = res?.data?.records ?? res?.data ?? []
    rateConfigOptions.value = records.map(r => ({ value: String(r.id), label: r.strategyName || r.strategyCode || String(r.id) }))
  } catch (e) { console.error('加载费率策略失败', e) }
}

// ── Stat chips (only those mappable to API params) ────────────────────
const statChips = [
  { label: '总数', key: null },
  { label: '在线', key: 'online', value: 1 },
  { label: '离线', key: 'online', value: 0 },
  { label: '停用', key: 'status', value: 1 },
]

const activeStat = ref('')
const statCounts = reactive({ total: 0, online: 0, offline: 0, disabled: 0 })

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  handleSearch()
}

// ── Form filters ──────────────────────────────────────────────────────
const filterMerchant = ref('')
const filterGroup = ref('')
const filterBjlx = ref('')
const filterVender = ref('')
const filterDateStart = ref('')
const filterDateEnd = ref('')

function resetFilters() {
  filterMerchant.value = ''
  filterGroup.value = ''
  filterBjlx.value = ''
  filterVender.value = ''
  filterDateStart.value = ''
  filterDateEnd.value = ''
  activeStat.value = ''
  handleSearch()
}

// ── Pagination & data ─────────────────────────────────────────────────
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const totalPages = ref(0)

function buildParams() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value, meterType: 1 }

  if (filterMerchant.value.trim()) {
    params.name = filterMerchant.value.trim()
  }
  if (filterVender.value) {
    params.vender = filterVender.value
  }
  if (filterGroup.value) {
    params.groupId = filterGroup.value
  }
  if (filterBjlx.value) {
    params.bjlx = Number(filterBjlx.value)
  }

  // stat quick-filter → API params
  if (activeStat.value) {
    const chip = statChips.find(c => c.label === activeStat.value)
    if (chip && chip.key && chip.value !== undefined) {
      params[chip.key] = chip.value
    }
  }

  return params
}

async function fetchMeters() {
  loading.value = true
  try {
    const res = await getMeterList(buildParams())
    const data = res.data
    tableData.value = data.records || []
    total.value = data.total || 0
    totalPages.value = Math.ceil(total.value / pageSize.value) || 1

    // update stat counts from total
    statCounts.total = total.value
    // approximate: these would ideally come from a separate stats endpoint
    statCounts.online = tableData.value.filter(r => r.general?.online === 1).length
    statCounts.offline = tableData.value.filter(r => r.general?.online === 0).length
    statCounts.disabled = tableData.value.filter(r => r.general?.status === 1).length
  } catch (e) {
    console.error('获取表计列表失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchMeters()
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  pageNum.value = p
  fetchMeters()
}

// ── Context menu ──────────────────────────────────────────────────────
const contextMenu = ref({ show: false, x: 0, y: 0, row: null })
function showContext(e, row) {
  contextMenu.value = { show: true, x: e.clientX, y: e.clientY, row }
}
function hideContext() {
  contextMenu.value.show = false
}
// ── Detail dialog ────────────────────────────────────────────────────
const detailDialog = reactive({ show: false, loading: false, data: null, curve: [] })
async function viewDetail(row) {
  hideContext()
  detailDialog.show = true
  detailDialog.loading = true
  detailDialog.data = null
  detailDialog.curve = []
  try {
    const res = await getMeterDetail(row.id)
    detailDialog.data = res.data
    // 补充读取实时曲线，用于「实时电表读数」栏（失败不影响详情展示）
    try {
      const curveRes = await getMeterReadingCurve(row.id)
      const curveBody = curveRes?.data ?? curveRes
      const points = Array.isArray(curveBody) ? curveBody : (curveBody?.records || curveBody?.list || [])
      detailDialog.curve = Array.isArray(points) ? points : []
      // 详情接口未返回最新读数时，用曲线最后一个点兜底功率/电量
      if (detailDialog.curve.length && detailDialog.data && !detailDialog.data.latestReading) {
        const p = detailDialog.curve[detailDialog.curve.length - 1]
        detailDialog.data.latestReading = { power: p.power, kwh: p.kwh, timestamp: p.timestamp }
      }
    } catch (e) {
      console.warn('读取实时曲线失败', e)
    }
  } finally {
    detailDialog.loading = false
  }
}
function closeDetailDialog() {
  detailDialog.show = false
  detailDialog.data = null
  detailDialog.curve = []
}

// ── Detail dialog helpers ────────────────────────────────────────────
function rootGroupName() {
  const path = detailDialog.data?.general?.groupPath
  if (!path) return '-'
  return path.split(' / ')[0] || '-'
}
function bjlxLabel(value) {
  if (value == null || value === '') return '-'
  const opt = editMarkOptions.find(o => o.value === String(value))
  return opt ? opt.label : String(value)
}
function meterTypeLabel() {
  return detailDialog.data?.chenman?.phase === 1 ? '单相电表' : '三相电表'
}
function fmtNum(v) {
  return v == null || v === '' ? '-' : v
}
function formatCurveTime(ms) {
  if (ms == null || ms === '') return '-'
  const d = new Date(Number(ms))
  if (Number.isNaN(d.getTime())) return '-'
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function moreAction(action, row) {
  hideContext()
  if (action === 'edit') {
    openEditModal(row)
  }
}

// ── Edit modal ────────────────────────────────────────────────────────
const editModal = reactive({
  show: false,
  id: null,
  meterNo: '',       // 表号（只读）
  groupId: '',       // 分组
  customerName: '',  // 商户名（写入 name 字段）
  bjlx: '',          // 表计类型
  status: 0,         // 表计状态：0-正常 1-停用
  ctRatio: 1,        // CT变比
  ptRatio: 1,        // PT变比
})
const saving = ref(false)

function openEditModal(row) {
  editModal.id = row.id
  editModal.meterNo = getMeterNo(row)
  editModal.groupId = row.general?.groupId ?? ''
  editModal.customerName = row.chenman?.name || row.customerName || ''
  editModal.bjlx = row.chenman?.bjlx ?? ''
  editModal.status = row.general?.status ?? 0
  editModal.ctRatio = row.general?.ctRatio ?? 1
  editModal.ptRatio = row.general?.ptRatio ?? 1
  editModal.show = true
}

function closeEditModal() {
  editModal.show = false
}

// ── Create meter modal ──────────────────────────────────────────────────
const createModalOpen = ref(false)
const creating = ref(false)
const createForm = reactive({
  customerName: '',
  dbNo: '',
  name: '',
  bei: '',
  bjlx: '',
  credit: 0,
  warnline: 0,
  creatuser: '',
  cttime: '',
  feetype: '',
  proId: '',
  proName: '',
  plusPrice: 0,
  priceType: '',
  priceContent: '',
  joinPhone: '',
  collectorDeviceNo: '',
  phase: '',
  status: 0,
  online: 1,
  meterSignal: '',
  groupId: '',
  rateConfigId: '',
  ctRatio: 1,
  ptRatio: 1,
  markStatus: '',
  enabledTime: '',
  disableWarning: 0,
  commStatusDesc: '',
  collectorCode: '',
  bpsType: '',
  bitType: '',
  protoType: '',
  serial: '',
  deviceType: '',
})

function resetCreateForm() {
  Object.assign(createForm, {
    customerName: '', dbNo: '', name: '', bei: '', bjlx: '', credit: 0, warnline: 0,
    creatuser: '', cttime: '', feetype: '', proId: '', proName: '', plusPrice: 0,
    priceType: '', priceContent: '', joinPhone: '', collectorDeviceNo: '', phase: '',
    status: 0, online: 1, meterSignal: '', groupId: '', rateConfigId: '',
    ctRatio: 1, ptRatio: 1, markStatus: '', enabledTime: '', disableWarning: 0,
    commStatusDesc: '', collectorCode: '', bpsType: '', bitType: '', protoType: '',
    serial: '', deviceType: '',
  })
  Object.keys(createErrors).forEach(k => delete createErrors[k])
}

// ── Create form validation ───────────────────────────────────────────
const createErrors = reactive({})

function validateCreateForm() {
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  if (!createForm.dbNo.trim()) createErrors.dbNo = '请输入档案编号'
  if (!createForm.name.trim()) createErrors.name = '请输入名称'
  if (!createForm.customerName.trim()) createErrors.customerName = '请输入商户名称'
  if (!Number.isFinite(Number(createForm.ctRatio)) || Number(createForm.ctRatio) <= 0) createErrors.ctRatio = 'CT变比必须大于0'
  if (!Number.isFinite(Number(createForm.ptRatio)) || Number(createForm.ptRatio) <= 0) createErrors.ptRatio = 'PT变比必须大于0'
  return Object.keys(createErrors).length === 0
}

function openCreateModal() {
  resetCreateForm()
  createModalOpen.value = true
  if (rateConfigOptions.value.length === 0) loadRateConfigOptions()
}

async function submitCreate() {
  if (!validateCreateForm()) return
  creating.value = true
  try {
    const body = {
      customerName: createForm.customerName || undefined,
      dbNo: createForm.dbNo || undefined,
      name: createForm.name || undefined,
      bei: createForm.bei !== '' ? Number(createForm.bei) : undefined,
      bjlx: createForm.bjlx !== '' ? Number(createForm.bjlx) : undefined,
      credit: Number(createForm.credit) || 0,
      warnline: Number(createForm.warnline) || 0,
      creatuser: createForm.creatuser || undefined,
      cttime: createForm.cttime || undefined,
      feetype: createForm.feetype !== '' ? Number(createForm.feetype) : undefined,
      proId: createForm.proId ? Number(createForm.proId) : undefined,
      proName: createForm.proName || undefined,
      plusPrice: Number(createForm.plusPrice) || 0,
      priceType: createForm.priceType !== '' ? Number(createForm.priceType) : undefined,
      priceContent: createForm.priceContent || undefined,
      joinPhone: createForm.joinPhone || undefined,
      collectorDeviceNo: createForm.collectorDeviceNo || undefined,
      phase: createForm.phase !== '' ? Number(createForm.phase) : undefined,
      status: Number(createForm.status) || 0,
      online: Number(createForm.online) ?? 1,
      meterSignal: createForm.meterSignal !== '' ? Number(createForm.meterSignal) : undefined,
      meterType: 1,
      groupId: createForm.groupId ? Number(createForm.groupId) : undefined,
      rateConfigId: createForm.rateConfigId ? Number(createForm.rateConfigId) : undefined,
      ctRatio: Number(createForm.ctRatio),
      ptRatio: Number(createForm.ptRatio),
      markStatus: createForm.markStatus || undefined,
      enabledTime: createForm.enabledTime || undefined,
      disableWarning: Number(createForm.disableWarning) || 0,
      commStatusDesc: createForm.commStatusDesc || undefined,
      collectorCode: createForm.collectorCode || undefined,
      bpsType: createForm.bpsType || undefined,
      bitType: createForm.bitType || undefined,
      protoType: createForm.protoType || undefined,
      serial: createForm.serial !== '' ? Number(createForm.serial) : undefined,
      deviceType: createForm.deviceType || undefined,
    }
    await createMeter(body)
    createModalOpen.value = false
    fetchMeters()
  } catch (e) {
    console.error('新增表计失败', e)
  } finally {
    creating.value = false
  }
}

async function submitEdit() {
  if (!editModal.id) return
  saving.value = true
  try {
    const body = {
      meterType: 1,
      name: editModal.customerName || undefined,
      groupId: editModal.groupId !== '' ? Number(editModal.groupId) : undefined,
      bjlx: editModal.bjlx !== '' ? Number(editModal.bjlx) : undefined,
      status: Number(editModal.status),
      ctRatio: editModal.ctRatio === '' || editModal.ctRatio == null ? undefined : Number(editModal.ctRatio),
      ptRatio: editModal.ptRatio === '' || editModal.ptRatio == null ? undefined : Number(editModal.ptRatio),
    }
    await updateMeter(editModal.id, body)
    closeEditModal()
    fetchMeters()
  } catch (e) {
    console.error('保存失败', e)
    alert(e?.response?.data?.msg || e?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ── Bubble state ──────────────────────────────────────────────────────
const bubbles = reactive({ device: false, batch: false, global: false })
const bubblePositions = reactive({
  device: { top: '0px', left: '0px' },
  batch: { top: '0px', left: '0px' },
  global: { top: '0px', left: '0px' },
})

function toggleBubble(e, name) {
  if (bubbles[name]) { bubbles[name] = false; return }
  const rect = e.currentTarget.getBoundingClientRect()
  bubblePositions[name] = { top: `${rect.bottom + 8}px`, left: `${rect.left}px` }
  bubbles[name] = true
}

function handleClickOutside(e) {
  for (const name of ['device', 'batch', 'global']) {
    if (!bubbles[name]) continue
    const el = document.getElementById(`${name}Bubble`)
    const btn = document.getElementById(`${name}Btn`)
    if (el && !el.contains(e.target) && btn && !btn.contains(e.target)) {
      bubbles[name] = false
    }
  }
}

// ── Selection ─────────────────────────────────────────────────────────
const selectedMeters = ref(new Set())

const selectedMeterRow = computed(() => {
  if (selectedMeters.value.size === 0) return null
  const firstId = [...selectedMeters.value][0]
  return tableData.value.find(m => m.id === firstId) || null
})

function toggleSelectAll(e) {
  if (e.target.checked) {
    tableData.value.forEach(m => selectedMeters.value.add(m.id))
  } else {
    selectedMeters.value.clear()
  }
}

function toggleMeter(id) {
  if (selectedMeters.value.has(id)) selectedMeters.value.delete(id)
  else selectedMeters.value.add(id)
}

// ── Helpers ───────────────────────────────────────────────────────────
function getMeterNo(row) {
  return row.chenman?.dbNo || row.id
}
function isOnline(row) {
  return row.general?.online === 1
}
function getStatusLabel(row) {
  return row.general?.status === 0 ? '正常' : '停用'
}

// ── Delete confirm ────────────────────────────────────────────────────
const deleting = ref(false)
const deleteConfirm = reactive({ show: false, row: null })
function confirmDelete(row) { hideContext(); deleteConfirm.show = true; deleteConfirm.row = row }
async function executeDelete() {
  if (!deleteConfirm.row) return
  deleting.value = true
  try {
    await deleteMeter(deleteConfirm.row.id)
    deleteConfirm.show = false
    fetchMeters()
  } catch (e) { console.error('删除失败', e) } finally { deleting.value = false }
}

// ── Valve control ─────────────────────────────────────────────────────
const valveForm = reactive({ show: false, row: null, action: 'OPEN', reason: '' })
const valving = ref(false)
function openValveDialog(row, action) {
  hideContext()
  valveForm.show = true; valveForm.row = row; valveForm.action = action; valveForm.reason = ''
}
async function executeValve() {
  if (!valveForm.reason.trim() || !valveForm.row) return
  valving.value = true
  try {
    const fn = valveForm.action === 'OPEN' ? openValve : closeValve
    await fn(valveForm.row.id, valveForm.reason.trim())
    valveForm.show = false
    fetchMeters()
  } catch (e) { console.error('阀门操作失败', e) } finally { valving.value = false }
}

// ── Status toggle ─────────────────────────────────────────────────────
async function toggleStatus(row) {
  hideContext()
  const newStatus = row.general?.status === 1 ? 0 : 1
  try {
    await updateMeterStatus(row.id, newStatus)
    fetchMeters()
  } catch (e) { console.error('状态变更失败', e) }
}

// ── Export ────────────────────────────────────────────────────────────
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try { await exportMeterList(buildParams()) } catch (e) { console.error('导出失败', e) }
  finally { exporting.value = false }
}

async function handleBatchExport() {
  if (selectedMeters.value.size === 0) return
  exporting.value = true
  try { await exportMeterBatch([...selectedMeters.value]) }
  catch (e) { console.error('导出失败', e) } finally { exporting.value = false }
}

// ── Import ────────────────────────────────────────────────────────────
const importFileInput = ref(null)
const importing = ref(false)
function handleImport() { importFileInput.value?.click() }
async function onImportFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  importing.value = true
  try {
    const text = await file.text()
    const meters = parseCSV(text)
    if (meters.length === 0) { alert('文件中没有有效数据') ; return }
    await batchCreateMeters(meters)
    alert(`成功导入 ${meters.length} 条记录`)
    fetchMeters()
    e.target.value = ''
  } catch (err) { console.error('导入失败', err); alert('导入失败: ' + (err.message || '请检查文件格式')) }
  finally { importing.value = false }
}
function parseCSV(text) {
  const lines = text.trim().split(/\r?\n/)
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''))
  return lines.slice(1).map(line => {
    const values = line.split(',').map(v => v.trim().replace(/^"|"$/g, ''))
    const row = {}
    headers.forEach((h, i) => { if (values[i] !== undefined) row[h] = values[i] })
    return {
      meterType: Number(row['meterType'] ?? row['表计类型'] ?? 1),
      name: row['商户名'] || row['name'] || row['商户名称'] || row['customerName'] || row['customer_name'] || undefined,
      dbNo: row['dbNo'] || row['表号'] || undefined,
      groupId: row['groupId'] || row['分组'] || undefined,
      status: Number(row['status'] ?? row['状态'] ?? 1),
      ctRatio: Number(row['ctRatio'] ?? row['CT变比'] ?? 1),
      ptRatio: Number(row['ptRatio'] ?? row['PT变比'] ?? 1),
      credit: Number(row['credit'] ?? row['透支额度'] ?? 0),
    }
  })
}

// ── Operation logs ────────────────────────────────────────────────────
const opLogDialog = reactive({ show: false, loading: false, row: null, records: [], total: 0, pageNum: 1, pageSize: 10 })
async function openOpLogs(row) {
  hideContext()
  opLogDialog.show = true; opLogDialog.row = row; opLogDialog.records = []; opLogDialog.pageNum = 1
  await fetchOpLogs()
}
async function fetchOpLogs() {
  opLogDialog.loading = true
  try {
    const res = await getElectricOperationLogs(opLogDialog.row.id, {
      pageNum: opLogDialog.pageNum, pageSize: opLogDialog.pageSize,
    })
    const data = res.data
    opLogDialog.records = data?.records || []
    opLogDialog.total = data?.total || 0
  } catch (e) { console.error('操作日志查询失败', e) } finally { opLogDialog.loading = false }
}

// ── Batch edit info modal ─────────────────────────────────────────────
const batchEditModal = reactive({ show: false, form: { bjlx: '', ctRatio: '', ptRatio: '', markStatus: '', disableWarning: '', commStatusDesc: '', status: '', address: '' } })
const batchEditing = ref(false)
function openBatchEdit() {
  bubbles.batch = false
  if (selectedMeters.value.size === 0) return
  batchEditModal.show = true
  Object.assign(batchEditModal.form, { bjlx: '', ctRatio: '', ptRatio: '', markStatus: '', disableWarning: '', commStatusDesc: '', status: '', address: '' })
}
async function submitBatchEdit() {
  batchEditing.value = true
  try {
    const f = batchEditModal.form
    const body = { meterIds: [...selectedMeters.value] }
    if (f.bjlx !== '') body.bjlx = Number(f.bjlx)
    if (f.ctRatio !== '') body.ctRatio = Number(f.ctRatio)
    if (f.ptRatio !== '') body.ptRatio = Number(f.ptRatio)
    if (f.markStatus) body.markStatus = f.markStatus
    if (f.disableWarning !== '') body.disableWarning = Number(f.disableWarning)
    if (f.commStatusDesc) body.commStatusDesc = f.commStatusDesc
    if (f.status !== '') body.status = Number(f.status)
    if (f.address) body.address = f.address
    await batchUpdateMeterInfo(body)
    batchEditModal.show = false
    selectedMeters.value.clear()
    fetchMeters()
  } catch (e) { console.error('批量修改失败', e) } finally { batchEditing.value = false }
}

// ── Batch update price modal ──────────────────────────────────────────
const batchPriceModal = reactive({ show: false, form: { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' } })
const batchPricing = ref(false)
function openBatchPrice() {
  bubbles.batch = false
  if (selectedMeters.value.size === 0) return
  batchPriceModal.show = true
  if (rateConfigOptions.value.length === 0) loadRateConfigOptions()
  Object.assign(batchPriceModal.form, { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' })
}
async function submitBatchPrice() {
  batchPricing.value = true
  try {
    const f = batchPriceModal.form
    await batchUpdateElectricityPrice({
      meterIds: [...selectedMeters.value],
      priceType: Number(f.priceType),
      priceContent: f.priceContent || undefined,
      plusPrice: f.plusPrice !== '' ? Number(f.plusPrice) : undefined,
      rateConfigId: f.rateConfigId ? Number(f.rateConfigId) : undefined,
    })
    batchPriceModal.show = false
    selectedMeters.value.clear()
    fetchMeters()
  } catch (e) { console.error('批量修改电价失败', e) } finally { batchPricing.value = false }
}

// ── Batch time slot modal ─────────────────────────────────────────────
const batchSlotModal = reactive({ show: false, form: { rateConfigId: '' } })
const batchSlotting = ref(false)
function openBatchSlot() {
  bubbles.batch = false
  if (selectedMeters.value.size === 0) return
  batchSlotModal.show = true
  if (rateConfigOptions.value.length === 0) loadRateConfigOptions()
  batchSlotModal.form.rateConfigId = ''
}
async function submitBatchSlot() {
  batchSlotting.value = true
  try {
    await batchSetTimeSlot({
      meterIds: [...selectedMeters.value],
      rateConfigId: Number(batchSlotModal.form.rateConfigId),
      isGlobal: false,
    })
    batchSlotModal.show = false
    selectedMeters.value.clear()
    fetchMeters()
  } catch (e) { console.error('批量设置时段失败', e) } finally { batchSlotting.value = false }
}

// ── Global price modal ────────────────────────────────────────────────
const globalPriceModal = reactive({ show: false, form: { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' } })
const globalPricing = ref(false)
function openGlobalPrice() {
  bubbles.global = false
  globalPriceModal.show = true
  if (rateConfigOptions.value.length === 0) loadRateConfigOptions()
  Object.assign(globalPriceModal.form, { priceType: '', priceContent: '', plusPrice: '', rateConfigId: '' })
}
async function submitGlobalPrice() {
  globalPricing.value = true
  try {
    const f = globalPriceModal.form
    await globalUpdateElectricityPrice({
      priceType: Number(f.priceType),
      priceContent: f.priceContent || undefined,
      plusPrice: f.plusPrice !== '' ? Number(f.plusPrice) : undefined,
      rateConfigId: f.rateConfigId ? Number(f.rateConfigId) : undefined,
    })
    globalPriceModal.show = false
    fetchMeters()
  } catch (e) { console.error('全局修改电价失败', e) } finally { globalPricing.value = false }
}

// ── Global time slot modal ────────────────────────────────────────────
const globalSlotModal = reactive({ show: false, form: { rateConfigId: '' } })
const globalSlotting = ref(false)
function openGlobalSlot() {
  bubbles.global = false
  globalSlotModal.show = true
  if (rateConfigOptions.value.length === 0) loadRateConfigOptions()
  globalSlotModal.form.rateConfigId = ''
}
async function submitGlobalSlot() {
  globalSlotting.value = true
  try {
    await batchSetTimeSlot({ isGlobal: true, meterType: 1, rateConfigId: Number(globalSlotModal.form.rateConfigId) })
    globalSlotModal.show = false
    fetchMeters()
  } catch (e) { console.error('全局设置时段失败', e) } finally { globalSlotting.value = false }
}

// ── Global valve control modal ────────────────────────────────────────
const globalValveModal = reactive({ show: false, form: { action: 'OPEN', reason: '' } })
const globalValving = ref(false)
function openGlobalValve() {
  bubbles.global = false
  globalValveModal.show = true
  globalValveModal.form = { action: 'OPEN', reason: '' }
}
async function submitGlobalValve() {
  globalValving.value = true
  try {
    await batchValveControl({ isGlobal: true, action: globalValveModal.form.action, reason: globalValveModal.form.reason.trim() })
    globalValveModal.show = false
    fetchMeters()
  } catch (e) { console.error('全局阀门操作失败', e) } finally { globalValving.value = false }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  fetchMeters()
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div>
    <!-- Box 1: Filter inputs + buttons -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input
          v-model="filterMerchant"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none"
          placeholder="商户名或表号"
          type="text"
          @keyup.enter="handleSearch"
        />
        <GroupSelector v-model="filterGroup" placeholder="分组" />
        <DropdownSelect v-model="filterBjlx" :options="bjlxOptions" placeholder="报警类型" />
        <DropdownSelect v-model="filterVender" :options="venderOptions" placeholder="厂家" />
        <DateRangePicker
          v-model:start-value="filterDateStart"
          v-model:end-value="filterDateEnd"
          start-placeholder="创建日期起"
          end-placeholder="创建日期止"
        />
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="handleSearch">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- Box 2: Category stat chips -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div
          v-for="stat in statChips"
          :key="stat.label"
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === stat.label
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter(stat.label)"
        >
          {{ stat.label }}:
          <span class="ml-1">
            <template v-if="stat.label === '总数'">{{ total }}</template>
            <template v-else-if="stat.label === '在线'">{{ statCounts.online }}</template>
            <template v-else-if="stat.label === '离线'">{{ statCounts.offline }}</template>
            <template v-else>{{ statCounts.disabled }}</template>
          </span>
          <span v-if="stat.label !== '总数'">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="bg-white rounded p-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <!-- Action Bar -->
      <div class="relative flex gap-2.5 mb-[15px]">
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="openCreateModal">新增表计</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="handleImport" :disabled="importing">{{ importing ? '导入中...' : '批量导入' }}</button>
        <input ref="importFileInput" type="file" accept=".csv,.xls,.xlsx" class="hidden" @change="onImportFile" />
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="handleExport" :disabled="exporting">{{ exporting ? '导出中...' : '导出' }}</button>
        <button id="deviceBtn" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="toggleBubble($event, 'device')">设备操作</button>
        <button id="batchBtn" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="toggleBubble($event, 'batch')">批量操作</button>
        <button id="globalBtn" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="toggleBubble($event, 'global')">全局操作</button>
      </div>

      <!-- Data Table -->
      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]"><input type="checkbox" class="styled-checkbox" @change="toggleSelectAll" /></th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">分组</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">商户名</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">表号</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">表计名称</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">厂家</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">倍率</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">透支额度</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">报警线</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">通讯状态</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">今日有功电量</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">运行状态</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">CT变比</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">PT变比</th>
              <th class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-medium bg-[#f5f6fb]">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading" class="text-center text-[#999] text-[13px]">
              <td :colspan="11" class="py-6">加载中...</td>
            </tr>
            <tr v-else-if="!tableData.length" class="text-center text-[#999] text-[13px]">
              <td :colspan="11" class="py-6">暂无数据</td>
            </tr>
            <tr v-for="meter in tableData" :key="meter.id" class="hover:bg-[#f5f6fb]">
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]"><input type="checkbox" class="styled-checkbox" :checked="selectedMeters.has(meter.id)" @change="toggleMeter(meter.id)" /></td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ meter.groupName || meter.general?.groupName || '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ meter.chenman?.name || meter.customerName || '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono text-[#3a64ab]">{{ getMeterNo(meter) }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ meter.chenman?.name || '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">{{ meter.jicheng?.vender || '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ meter.chenman?.bei ?? '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ meter.chenman?.credit ?? '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ meter.chenman?.warnline ?? '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="isOnline(meter) ? 'status-green' : 'status-gray'">{{ isOnline(meter) ? '在线' : '离线' }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono text-right">{{ meter.todayActiveEnergy == null ? '--' : `${Number(meter.todayActiveEnergy).toFixed(4)} kWh` }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="status-tag" :class="getStatusLabel(meter) === '正常' ? 'status-green' : 'status-red'">{{ getStatusLabel(meter) }}</span>
              </td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ meter.general?.ctRatio ?? '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px] font-mono">{{ meter.general?.ptRatio ?? '--' }}</td>
              <td class="py-3 px-2.5 border-b border-[#f0f0f0] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2">二维码</span>
                <span class="text-[#3a64ab] cursor-pointer mr-2" @click="moreAction('edit', meter)">编辑</span>
                <span class="text-[#3a64ab] cursor-pointer" @click="showContext($event, meter)">更多 &#x25BC;</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex items-center justify-between mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ pageNum }}/{{ totalPages }} 页</span>
        <div class="flex gap-1">
          <button
            class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="pageNum <= 1"
            @click="goPage(1)"
          >首页</button>
          <button
            class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="pageNum <= 1"
            @click="goPage(pageNum - 1)"
          >上一页</button>
          <button
            class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="pageNum >= totalPages"
            @click="goPage(pageNum + 1)"
          >下一页</button>
          <button
            class="py-1 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white cursor-pointer hover:bg-[#f5f6fb] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="pageNum >= totalPages"
            @click="goPage(totalPages)"
          >末页</button>
        </div>
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div v-if="contextMenu.show" class="fixed inset-0 z-[70]" @click="hideContext">
        <div
          class="absolute bg-white border border-[#e8e8e8] rounded-lg shadow-xl py-1.5 min-w-[140px]"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
          @click.stop
        >

          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="viewDetail(contextMenu.row)">查看详情</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openValveDialog(contextMenu.row, 'OPEN')">远程开阀</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openValveDialog(contextMenu.row, 'CLOSE')">远程关阀</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="toggleStatus(contextMenu.row)">
            {{ contextMenu.row?.general?.status === 1 ? '恢复运行' : '停用表计' }}
          </div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openOpLogs(contextMenu.row)">操作日志</div>
          <div class="border-t border-[#f0f0f0]"></div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer text-[#ff4d4f] transition-colors" @click="confirmDelete(contextMenu.row)">删除表计</div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirm Dialog -->
    <Teleport to="body">
      <div v-if="deleteConfirm.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="deleteConfirm.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6">
          <h3 class="text-base font-medium mb-4">确认删除</h3>
          <p class="text-[13px] text-[#666] mb-5">确定要删除表计 <b>{{ getMeterNo(deleteConfirm.row) }}</b> 吗？此操作为逻辑删除，可在已删除列表中恢复。</p>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="deleteConfirm.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="deleting" @click="executeDelete">{{ deleting ? '删除中...' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Valve Control Dialog -->
    <Teleport to="body">
      <div v-if="valveForm.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="valveForm.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[420px] p-6">
          <h3 class="text-base font-medium mb-4">{{ valveForm.action === 'OPEN' ? '远程开阀' : '远程关阀' }}</h3>
          <div class="mb-1 text-[13px] text-[#666]">
            表计：<span class="text-[#333]">{{ getMeterNo(valveForm.row) }}</span>
          </div>
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
              <thead>
                <tr>
                  <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作员</th>
                  <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作模块</th>
                  <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作类型</th>
                  <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作描述</th>
                  <th class="py-2 px-2 border-b border-[#f0f0f0] text-[#666]">操作时间</th>
                </tr>
              </thead>
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

    <!-- Edit Dialog -->
    <Teleport to="body">
      <div v-if="editModal.show" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeEditModal">
        <div class="bg-white rounded-lg shadow-2xl w-[480px] max-h-[85vh] flex flex-col">
          <div class="px-5 py-3 border-b border-[#f0f0f0] flex justify-between items-center shrink-0">
            <h3 class="text-base font-medium">编辑电表信息</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="closeEditModal">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5 space-y-3.5">
            <div>
              <label class="text-[12px] text-[#999] block mb-0.5">表号</label>
              <input :value="editModal.meterNo" readonly class="w-full py-1.5 px-3 border border-[#d9d9d9] bg-[#f5f5f5] rounded text-[13px]" />
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-0.5">分组</label>
              <GroupSelector v-model="editModal.groupId" placeholder="请选择分组" />
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-0.5">商户名</label>
              <input v-model="editModal.customerName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" placeholder="支持中文、空格及特殊符号" />
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-0.5">表计类型</label>
              <DropdownSelect :model-value="String(editModal.bjlx ?? '')" :options="editMarkOptions" placeholder="请选择表计类型" @update:model-value="value => editModal.bjlx = Number(value)" />
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-0.5">表计状态</label>
              <select v-model.number="editModal.status" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white focus:border-[#3a64ab]">
                <option :value="0">正常</option>
                <option :value="1">停用</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-2.5">
              <div>
                <label class="text-[12px] text-[#999] block mb-0.5">CT变比</label>
                <input v-model.number="editModal.ctRatio" type="number" min="0.0001" step="0.0001" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-0.5">PT变比</label>
                <input v-model.number="editModal.ptRatio" type="number" min="0.0001" step="0.0001" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-[#f0f0f0] flex gap-2.5 justify-end shrink-0">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeEditModal">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="saving" @click="submitEdit">{{ saving ? '提交中...' : '提交' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Device Bubble -->
    <Teleport to="body">
      <div
        v-if="bubbles.device" id="deviceBubble" class="fixed z-[70]"
        :style="{ top: bubblePositions.device.top, left: bubblePositions.device.left }"
        style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
      >
        <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent border-b-white ml-5"></div>
        <div class="bg-white rounded-lg p-4 min-w-[180px]">
          <div class="space-y-2">
            <button
              v-if="isAdmin"
              class="w-full py-2 px-4 rounded bg-white border border-[#ff4d4f] text-[#ff4d4f] text-[13px] cursor-pointer text-left hover:bg-[#fff2f0] disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="!selectedMeterRow"
              @click="openClearDialog(selectedMeterRow)"
            >余额清零</button>
            <p v-else class="text-[12px] text-[#999]">仅管理员可操作</p>
            <p v-if="isAdmin && !selectedMeterRow" class="text-[11px] text-[#999]">请先勾选一个表计</p>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Clear Balance Dialog -->
    <Teleport to="body">
      <div v-if="clearDialog.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeClearDialog">
        <div class="bg-white rounded-lg shadow-xl w-[420px] p-6">
          <h3 class="text-base font-medium mb-4">余额清零</h3>
          <div class="mb-1 text-[13px] text-[#666]">
            表计：<span class="text-[#333]">{{ clearDialog.row?.jicheng?.stealNo || clearDialog.row?.id }}</span>
          </div>
          <div class="mb-3">
            <label class="text-[13px] text-[#666] block mb-1">清零原因 <span class="text-[#ff4d4f]">*</span></label>
            <textarea
              v-model="clearDialog.reason"
              class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none"
              rows="3"
              placeholder="请输入清零原因，如：商户退租，退还全部余额后清零"
            ></textarea>
          </div>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeClearDialog">取消</button>
            <button
              class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!clearDialog.reason.trim()"
              @click="submitClearBalance"
            >确认清零</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Meter Detail Dialog -->
    <Teleport to="body">
      <div v-if="detailDialog.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeDetailDialog">
        <div class="bg-white rounded-lg shadow-xl w-[min(1280px,calc(100vw-2rem))] max-h-[85vh] flex flex-col">
          <div class="px-5 py-3 border-b border-[#f0f0f0] flex justify-between items-center shrink-0">
            <h3 class="text-base font-medium">表计详情</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="closeDetailDialog">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <div v-if="detailDialog.loading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div v-else-if="!detailDialog.data" class="text-center text-[#999] text-[13px] py-8">未找到表计信息</div>
            <template v-else>
              <!-- 详情按甲方要求分为竖三栏：基本信息 / 状态信息 / 实时电表读数 -->
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-[13px]">
                <!-- 第一栏：基本信息 -->
                <div class="rounded-lg border border-[#e8edf5] shadow-sm overflow-hidden self-start">
                  <h4 class="text-[14px] font-semibold px-4 py-3 border-b border-[#f0f0f0]">基本信息</h4>
                  <div class="space-y-2.5 p-4">
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">商户名称：</span><span class="text-[#333] truncate">{{ detailDialog.data.chenman?.name || detailDialog.data.customerName || '-' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">所属项目：</span><span class="text-[#333] truncate">{{ rootGroupName() }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">表计类型：</span><span class="text-[#333] truncate">{{ bjlxLabel(detailDialog.data.chenman?.bjlx) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">初始表底：</span><span class="text-[#333] truncate">{{ detailDialog.data.jicheng?.startVal ?? '0' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">初始时间：</span><span class="text-[#333] truncate">{{ detailDialog.data.chenman?.cttime || '平台部署时间' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">计费表底：</span><span class="text-[#333] truncate">0</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">尖时段当前计费表底：</span><span class="text-[#333] truncate">{{ detailDialog.data.latestReading?.kwhPeak ?? '0' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">峰时段当前计费表底：</span><span class="text-[#333] truncate">{{ detailDialog.data.latestReading?.kwhBusy ?? '0' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">平时段当前计费表底：</span><span class="text-[#333] truncate">{{ detailDialog.data.latestReading?.kwhNormal ?? '0' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">谷时段当前计费表底：</span><span class="text-[#333] truncate">{{ detailDialog.data.latestReading?.kwhIdle ?? '0' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">创建时间：</span><span class="text-[#333] truncate">{{ detailDialog.data.createTime || '-' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">故障时间：</span><span class="text-[#333] truncate">-</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[118px]">故障备注：</span><span class="text-[#333] truncate">-</span></div>
                  </div>
                </div>
                <!-- 第二栏：状态信息 -->
                <div class="rounded-lg border border-[#e8edf5] shadow-sm overflow-hidden self-start">
                  <h4 class="text-[14px] font-semibold px-4 py-3 border-b border-[#f0f0f0]">状态信息</h4>
                  <div class="space-y-2.5 p-4">
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">ICCID：</span><span class="text-[#333] truncate">{{ detailDialog.data.jicheng?.iccid || '-' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">电表类型：</span><span class="text-[#333] truncate">{{ meterTypeLabel() }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">有功功率：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.power) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">功率因素：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.fact) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">正向有功：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.todayActiveEnergy) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">反向有功：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.negativeActive) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">组合有功：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.combinedActive) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">尖时段：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.kwhPeak) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">峰时段：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.kwhBusy) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">谷时段：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.kwhIdle) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">A相电流：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.ampA) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">B相电流：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.ampB) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">C相电流：</span><span class="text-[#333] truncate">{{ fmtNum(detailDialog.data.latestReading?.ampC) }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">信号：</span><span class="text-[#333] truncate">{{ detailDialog.data.general?.meterSignal ?? '-' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">上报间隔（分）：</span><span class="text-[#333] truncate">{{ detailDialog.data.latestReading?.timeInterval ?? 15 }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">更新时间：</span><span class="text-[#333] truncate">{{ detailDialog.data.updateTime || '-' }}</span></div>
                    <div class="flex"><span class="text-[#999] shrink-0 w-[104px]">初始示数：</span><span class="text-[#333] truncate">{{ detailDialog.data.jicheng?.startVal ?? '0' }}</span></div>
                  </div>
                </div>
                <!-- 第三栏：实时电表读数（抄表时间 / 电表读数） -->
                <div class="rounded-lg border border-[#e8edf5] shadow-sm overflow-hidden flex flex-col">
                  <h4 class="text-[14px] font-semibold px-4 py-3 border-b border-[#f0f0f0]">实时电表读数</h4>
                  <div class="flex-1 overflow-y-auto max-h-[520px]">
                    <table class="w-full text-[13px]">
                      <thead>
                        <tr class="bg-[#f5f6fb] sticky top-0">
                          <th class="py-2 px-3 text-left text-[12px] font-medium text-[#666] border-b border-[#e8e8e8] whitespace-nowrap">抄表时间</th>
                          <th class="py-2 px-3 text-left text-[12px] font-medium text-[#666] border-b border-[#e8e8e8] whitespace-nowrap">电表读数</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(p, i) in detailDialog.curve" :key="i">
                          <td class="py-1.5 px-3 border-b border-[#f0f0f0] text-[#666] whitespace-nowrap">{{ formatCurveTime(p.timestamp) }}</td>
                          <td class="py-1.5 px-3 border-b border-[#f0f0f0] text-[#333]">{{ p.kwh ?? '-' }}</td>
                        </tr>
                        <tr v-if="!detailDialog.curve.length">
                          <td colspan="2" class="py-6 text-center text-[#999]">暂无读数</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Batch Bubble -->
    <Teleport to="body">
      <div
        v-if="bubbles.batch" id="batchBubble" class="fixed z-[70]"
        :style="{ top: bubblePositions.batch.top, left: bubblePositions.batch.left }"
        style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
      >
        <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent border-b-white ml-5"></div>
        <div class="bg-white rounded-lg p-4 min-w-[180px]">
          <p class="text-[13px] text-[#666] mb-3">已选择 {{ selectedMeters.size }} 个电表</p>
          <div class="space-y-2">
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="handleBatchExport()">批量导出</button>
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openBatchEdit">批量修改信息</button>
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openBatchPrice">批量修改电价</button>
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openBatchSlot">时段设置</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Bubble -->
    <Teleport to="body">
      <div
        v-if="bubbles.global" id="globalBubble" class="fixed z-[70]"
        :style="{ top: bubblePositions.global.top, left: bubblePositions.global.left }"
        style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
      >
        <div class="w-0 h-0 border-l-[8px] border-r-[8px] border-b-[8px] border-l-transparent border-r-transparent border-b-white ml-5"></div>
        <div class="bg-white rounded-lg p-4 min-w-[200px]">
          <p class="text-[13px] text-[#ff4d4f] mb-3">全局操作将影响所有电表，请谨慎执行</p>
          <div class="space-y-2">
            <button class="w-full py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer text-left hover:bg-[#f5f6fb]" @click="openGlobalPrice">修改所有电价</button>
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
            <h3 class="text-base font-medium">新增电表</h3>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="createModalOpen = false">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5 space-y-4">
            <!-- 辰鳗 -->
            <fieldset class="border border-[#e8e8e8] rounded p-3">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">辰鳗</legend>
              <div class="space-y-2.5">
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5"><span class="text-[#ff4d4f]">*</span> 档案编号</label>
                    <input v-model="createForm.dbNo" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="createErrors.dbNo ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" />
                    <p v-if="createErrors.dbNo" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ createErrors.dbNo }}</p>
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5"><span class="text-[#ff4d4f]">*</span> 名称</label>
                    <input v-model="createForm.name" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="createErrors.name ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" />
                    <p v-if="createErrors.name" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ createErrors.name }}</p>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5"><span class="text-[#ff4d4f]">*</span> 商户名称</label>
                    <input v-model="createForm.customerName" class="w-full py-1.5 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="createErrors.customerName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="请输入商户名称" />
                    <p v-if="createErrors.customerName" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ createErrors.customerName }}</p>
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">倍率</label>
                    <input v-model.number="createForm.bei" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">透支额度</label>
                    <input v-model.number="createForm.credit" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">报警线</label>
                    <input v-model.number="createForm.warnline" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">报警类型</label>
                    <DropdownSelect v-model="createForm.bjlx" :options="bjlxOptions" placeholder="请选择" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">费用类型</label>
                    <DropdownSelect v-model="createForm.feetype" :options="feetypeOptions" placeholder="请选择" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">项目ID</label>
                    <input v-model="createForm.proId" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">项目名称</label>
                    <input v-model="createForm.proName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">联系电话</label>
                    <input v-model="createForm.joinPhone" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">采集设备号</label>
                    <input v-model="createForm.collectorDeviceNo" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">电价类型</label>
                    <DropdownSelect v-model="createForm.priceType" :options="priceTypeOptions" placeholder="请选择" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">附加电价</label>
                    <input v-model.number="createForm.plusPrice" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2.5">
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">相数</label>
                    <DropdownSelect v-model="createForm.phase" :options="phaseOptions" placeholder="请选择" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">创建人</label>
                    <input v-model="createForm.creatuser" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
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
                    <label class="text-[12px] text-[#999] block mb-0.5">分组</label>
                    <GroupSelector v-model="createForm.groupId" placeholder="请选择分组" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">费率配置</label>
                    <DropdownSelect v-model="createForm.rateConfigId" :options="rateConfigOptions" placeholder="请选择费率策略" />
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
                    <label class="text-[12px] text-[#999] block mb-0.5">信号强度</label>
                    <input v-model.number="createForm.meterSignal" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
                  </div>
                  <div>
                    <label class="text-[12px] text-[#999] block mb-0.5">禁用报警</label>
                    <input v-model.number="createForm.disableWarning" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
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

    <!-- Batch Edit Info Modal -->
    <Teleport to="body">
      <div v-if="batchEditModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchEditModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[520px] max-h-[80vh] overflow-y-auto p-6">
          <h3 class="text-base font-medium mb-4">批量修改信息（{{ selectedMeters.size }} 个表计）</h3>
          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-[12px] text-[#999] block mb-1">业务类型</label>
                <select v-model="batchEditModal.form.bjlx" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                  <option value="">不修改</option>
                  <option :value="1">租户</option>
                  <option :value="2">总表</option>
                  <option :value="3">公区</option>
                  <option :value="4">楼栋</option>
                  <option :value="5">公摊</option>
                  <option :value="6">虚拟总表</option>
                </select>
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">运行状态</label>
                <select v-model="batchEditModal.form.status" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                  <option value="">不修改</option>
                  <option :value="0">停用</option>
                  <option :value="1">正常运行</option>
                </select>
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">CT变比</label>
                <input v-model="batchEditModal.form.ctRatio" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="留空不修改" />
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">PT变比</label>
                <input v-model="batchEditModal.form.ptRatio" type="number" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="留空不修改" />
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">标记状态</label>
                <select v-model="batchEditModal.form.markStatus" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                  <option value="">不修改</option>
                  <option value="NORMAL">正常</option>
                  <option value="ATTENTION">关注</option>
                  <option value="ABNORMAL">异常</option>
                  <option value="MAINTENANCE">维护</option>
                </select>
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">停用告警</label>
                <select v-model="batchEditModal.form.disableWarning" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                  <option value="">不修改</option>
                  <option :value="0">告警</option>
                  <option :value="1">停用</option>
                </select>
              </div>
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-1">通讯状态描述</label>
              <input v-model="batchEditModal.form.commStatusDesc" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="留空不修改" />
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-1">安装地址</label>
              <input v-model="batchEditModal.form.address" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="留空不修改" />
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchEditModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="batchEditing" @click="submitBatchEdit">{{ batchEditing ? '提交中...' : '确认修改' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Batch Update Price Modal -->
    <Teleport to="body">
      <div v-if="batchPriceModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchPriceModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[480px] p-6">
          <h3 class="text-base font-medium mb-4">批量修改电价（{{ selectedMeters.size }} 个表计）</h3>
          <div class="space-y-3">
            <div>
              <label class="text-[12px] text-[#999] block mb-1">电价类型 <span class="text-[#ff4d4f]">*</span></label>
              <select v-model="batchPriceModal.form.priceType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                <option value="">请选择</option>
                <option :value="1">固定单价</option>
                <option :value="2">费率单价</option>
                <option :value="3">阶梯单价</option>
              </select>
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-1">价格配置JSON</label>
              <textarea v-model="batchPriceModal.form.priceContent" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="费率/阶梯单价时使用"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-[12px] text-[#999] block mb-1">附加费（元/kWh）</label>
                <input v-model="batchPriceModal.form.plusPrice" type="number" step="0.01" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="0" />
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">费率策略</label>
                <DropdownSelect v-model="batchPriceModal.form.rateConfigId" :options="rateConfigOptions" placeholder="可选" />
              </div>
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchPriceModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!batchPriceModal.form.priceType || batchPricing" @click="submitBatchPrice">{{ batchPricing ? '提交中...' : '确认修改' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Batch Time Slot Modal -->
    <Teleport to="body">
      <div v-if="batchSlotModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchSlotModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6">
          <h3 class="text-base font-medium mb-4">批量设置时段参数（{{ selectedMeters.size }} 个表计）</h3>
          <div>
            <label class="text-[12px] text-[#999] block mb-1">费率策略 <span class="text-[#ff4d4f]">*</span></label>
            <DropdownSelect v-model="batchSlotModal.form.rateConfigId" :options="rateConfigOptions" placeholder="请选择费率策略" />
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchSlotModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!batchSlotModal.form.rateConfigId || batchSlotting" @click="submitBatchSlot">{{ batchSlotting ? '提交中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Price Modal -->
    <Teleport to="body">
      <div v-if="globalPriceModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalPriceModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[480px] p-6">
          <h3 class="text-base font-medium mb-1">全局修改电价</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有电表，请谨慎执行！</p>
          <div class="space-y-3">
            <div>
              <label class="text-[12px] text-[#999] block mb-1">电价类型 <span class="text-[#ff4d4f]">*</span></label>
              <select v-model="globalPriceModal.form.priceType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white">
                <option value="">请选择</option>
                <option :value="1">固定单价</option>
                <option :value="2">费率单价</option>
                <option :value="3">阶梯单价</option>
              </select>
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-1">价格配置JSON</label>
              <textarea v-model="globalPriceModal.form.priceContent" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="费率/阶梯单价时使用"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-[12px] text-[#999] block mb-1">附加费（元/kWh）</label>
                <input v-model="globalPriceModal.form.plusPrice" type="number" step="0.01" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="0" />
              </div>
              <div>
                <label class="text-[12px] text-[#999] block mb-1">费率策略</label>
                <DropdownSelect v-model="globalPriceModal.form.rateConfigId" :options="rateConfigOptions" placeholder="可选" />
              </div>
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalPriceModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalPriceModal.form.priceType || globalPricing" @click="submitGlobalPrice">{{ globalPricing ? '提交中...' : '确认修改' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Time Slot Modal -->
    <Teleport to="body">
      <div v-if="globalSlotModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalSlotModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6">
          <h3 class="text-base font-medium mb-1">全局设置时段参数</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有电表，请谨慎执行！</p>
          <div>
            <label class="text-[12px] text-[#999] block mb-1">费率策略 <span class="text-[#ff4d4f]">*</span></label>
            <DropdownSelect v-model="globalSlotModal.form.rateConfigId" :options="rateConfigOptions" placeholder="请选择费率策略" />
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalSlotModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalSlotModal.form.rateConfigId || globalSlotting" @click="submitGlobalSlot">{{ globalSlotting ? '提交中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Global Valve Control Modal -->
    <Teleport to="body">
      <div v-if="globalValveModal.show" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="globalValveModal.show = false">
        <div class="bg-white rounded-lg shadow-xl w-[420px] p-6">
          <h3 class="text-base font-medium mb-1">全局自动分合闸设置</h3>
          <p class="text-[12px] text-[#ff4d4f] mb-4">此操作将影响所有电表，请谨慎执行！</p>
          <div class="space-y-3">
            <div>
              <label class="text-[12px] text-[#999] block mb-1">操作类型 <span class="text-[#ff4d4f]">*</span></label>
              <div class="flex gap-3">
                <label class="flex items-center gap-1 text-[13px] cursor-pointer">
                  <input v-model="globalValveModal.form.action" type="radio" value="OPEN" class="accent-[#3a64ab]" /> 合闸
                </label>
                <label class="flex items-center gap-1 text-[13px] cursor-pointer">
                  <input v-model="globalValveModal.form.action" type="radio" value="CLOSE" class="accent-[#3a64ab]" /> 拉闸
                </label>
              </div>
            </div>
            <div>
              <label class="text-[12px] text-[#999] block mb-1">操作原因 <span class="text-[#ff4d4f]">*</span></label>
              <textarea v-model="globalValveModal.form.reason" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none resize-none" rows="2" placeholder="请输入操作原因"></textarea>
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="globalValveModal.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!globalValveModal.form.reason.trim() || globalValving" @click="submitGlobalValve">{{ globalValving ? '执行中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
