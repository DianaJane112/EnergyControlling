<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getInstantBillList, getInstantBillStats, getInstantBillDetail,
  batchCreateBills, batchImportBills, batchDeferBills, batchDeleteBills,
  exportInstantBillList, downloadBillTemplate,
  getBillingTrackList, exportBillingTrackList,
} from '@/api/instantBill.js'
import { getBillList, updateBill, deleteBill, batchDeleteBills as batchDeleteBillRecords, exportBillList } from '@/api/bill.js'
import { createBankAccount, getBankAccountList, deleteBankAccount } from '@/api/bankAccount.js'
import { getEnabledBillCategories, getBillCategoryList, createBillCategory, updateBillCategory, deleteBillCategory } from '@/api/billCategory.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

// ── Dropdown options ──────────────────────────────────────────────────
const billStatusOptions = [
  { value: '', label: '全部状态' },
  { value: 'UNPAID', label: '未缴' },
  { value: 'PAID', label: '已缴' },
  { value: 'OVERDUE', label: '逾期' },
]
const tradeTypeOptions = [
  { value: '', label: '全部类型' },
  { value: '1', label: '充值' },
  { value: '2', label: '消费扣费' },
  { value: '3', label: '退费' },
  { value: '4', label: '冲正' },
  { value: '5', label: '人工缴费' },
  { value: '6', label: '账单缴费' },
  { value: '7', label: '公共分摊' },
]

// ── Tab state ──────────────────────────────────────────────────────────
const activeTab = ref('instant')

// ── Filters ──────────────────────────────────────────────────────────
const filterCustomerName = ref('')
const filterMeterNo = ref('')
const filterCategoryId = ref('')
const filterBillStatus = ref('')
const filterStartTime = ref('')
const filterEndTime = ref('')
const activeStat = ref('')

const categoryOptions = ref([])
async function loadCategories() {
  try {
    const res = await getEnabledBillCategories()
    const list = (res.data || []).map(c => ({ value: c.id, label: c.categoryName }))
    categoryOptions.value = [{ value: '', label: '全部类别' }, ...list]
  } catch (e) { /* silent */ }
}

const statChips = [
  { label: '总数', key: null },
  { label: '未缴', key: 'billStatus', value: 'UNPAID' },
  { label: '已缴', key: 'billStatus', value: 'PAID' },
  { label: '逾期', key: 'billStatus', value: 'OVERDUE' },
]

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  currentPage.value = 1
  fetchData()
}

function buildSearchParams() {
  const p = {}
  if (filterCustomerName.value.trim()) p.customerName = filterCustomerName.value.trim()
  if (filterMeterNo.value.trim()) p.meterNo = filterMeterNo.value.trim()
  if (filterCategoryId.value) p.categoryId = filterCategoryId.value
  if (filterBillStatus.value) p.billStatus = filterBillStatus.value
  if (filterStartTime.value) p.startTime = filterStartTime.value
  if (filterEndTime.value) p.endTime = filterEndTime.value
  if (activeStat.value) {
    const chip = statChips.find(c => c.label === activeStat.value)
    if (chip && chip.key) p[chip.key] = chip.value
  }
  return p
}

function resetFilters() {
  filterCustomerName.value = ''
  filterMeterNo.value = ''
  filterCategoryId.value = ''
  filterBillStatus.value = ''
  filterStartTime.value = ''
  filterEndTime.value = ''
  activeStat.value = ''
  currentPage.value = 1
  fetchData()
}

// ── Data & Pagination ───────────────────────────────────────────────
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const stats = reactive({
  totalCount: 0, unpaidCount: 0, paidCount: 0, overdueCount: 0,
  totalAmount: 0, paidAmount: 0, householdRate: 0, amountRate: 0,
})
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function fetchData() {
  loading.value = true
  try {
    const res = await getInstantBillList({ pageNum: currentPage.value, pageSize: pageSize.value, ...buildSearchParams() })
    const data = res.data
    tableData.value = (data.records || []).map(r => ({
      meterId: r.meterId,
      meterName: r.meterName || '--',
      meterNo: r.meterNo || '--',
      customerId: r.customerId,
      customerName: r.customerName || '--',
      currentReading: r.currentReading ?? 0,
      lastSettledReading: r.lastSettledReading ?? 0,
      consumption: r.consumption ?? 0,
      multiplier: r.multiplier ?? 1,
      unitPrice: r.unitPrice ?? 0,
      billAmount: r.billAmount ?? 0,
      currentBalance: r.currentBalance ?? 0,
      meterType: r.meterType,
      status: r.status,
      calculateTime: r.calculateTime || '--',
    }))
    total.value = data.total || 0
  } catch (e) { console.error('获取即时账单列表失败', e); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function fetchStats() {
  try {
    const res = await getInstantBillStats(buildSearchParams())
    if (res.data) Object.assign(stats, {
      totalCount: res.data.totalCount || res.data.totalMeters || res.data.totalBills || 0,
      unpaidCount: res.data.unpaidCount || res.data.normalCount || res.data.unpaidBills || 0,
      paidCount: res.data.paidCount || res.data.paidBills || 0,
      overdueCount: res.data.overdueCount || 0,
      totalAmount: res.data.totalAmount || res.data.totalBalance || 0,
      paidAmount: res.data.paidAmount || 0,
      householdRate: res.data.householdRate || res.data.paymentRate || 0,
      amountRate: res.data.amountRate || 0,
    })
  } catch (e) { /* silent */ }
}

function goPage(p) { if (p >= 1 && p <= totalPages.value) { currentPage.value = p; fetchData() } }

// ── Row selection ────────────────────────────────────────────────────
const selectedIds = ref(new Set())
function toggleSelect(id) {
  if (selectedIds.value.has(id)) selectedIds.value.delete(id)
  else selectedIds.value.add(id)
}
function toggleAll() {
  if (selectedIds.value.size === tableData.value.length) selectedIds.value.clear()
  else selectedIds.value = new Set(tableData.value.map(r => r.meterId))
}
const allChecked = computed(() => tableData.value.length > 0 && selectedIds.value.size === tableData.value.length)

// ── Helpers ──────────────────────────────────────────────────────────
function formatMoney(n) { return Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function meterTypeLabel(v) { return v === 0 ? '水表' : v === 1 ? '电表' : '--' }
function statusLabel(v) { return v === 1 ? '运行' : v === 0 ? '停用' : v === 2 ? '欠费停用' : '--' }

// ── Detail modal ─────────────────────────────────────────────────────
const detailModal = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
async function openDetail(row) {
  detailModal.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getInstantBillDetail(row.meterId)
    detailData.value = res.data
  } catch (e) { console.error('获取详情失败', e) } finally { detailLoading.value = false }
}
function closeDetail() { detailModal.value = false; detailData.value = null }

// ── Batch generate bill modal ────────────────────────────────────────
const batchGenModal = ref(false)
const batchGenSaving = ref(false)
const batchGenForm = reactive({ categoryId: '', billDescription: '', fixedAmount: '', remark: '' })
function openBatchGen() {
  batchGenForm.categoryId = ''
  batchGenForm.billDescription = ''
  batchGenForm.fixedAmount = ''
  batchGenForm.remark = ''
  batchGenModal.value = true
}
async function submitBatchGen() {
  if (selectedIds.value.size === 0) return
  batchGenSaving.value = true
  try {
    await batchCreateBills({
      meterIds: [...selectedIds.value],
      categoryId: batchGenForm.categoryId ? Number(batchGenForm.categoryId) : undefined,
      billDescription: batchGenForm.billDescription || undefined,
      fixedAmount: batchGenForm.fixedAmount !== '' ? Number(batchGenForm.fixedAmount) : undefined,
      remark: batchGenForm.remark || undefined,
    })
    batchGenModal.value = false
    selectedIds.value.clear()
    fetchData()
    fetchStats()
  } catch (e) { console.error('批量生成失败', e) } finally { batchGenSaving.value = false }
}

// ── Batch defer modal ────────────────────────────────────────────────
const batchDeferModal = ref(false)
const batchDeferSaving = ref(false)
const batchDeferForm = reactive({ deferTo: '', deferReason: '' })
function openBatchDefer() {
  if (selectedIds.value.size === 0) return
  batchDeferForm.deferTo = ''
  batchDeferForm.deferReason = ''
  batchDeferModal.value = true
}
async function submitBatchDefer() {
  if (!batchDeferForm.deferTo) return
  batchDeferSaving.value = true
  try {
    await batchDeferBills({ meterIds: [...selectedIds.value], deferTo: batchDeferForm.deferTo, deferReason: batchDeferForm.deferReason || undefined })
    batchDeferModal.value = false
    selectedIds.value.clear()
    fetchData()
  } catch (e) { console.error('批量延期失败', e) } finally { batchDeferSaving.value = false }
}

// ── Batch delete ─────────────────────────────────────────────────────
const batchDelConfirm = ref(false)
const batchDeling = ref(false)
function openBatchDelete() {
  if (selectedIds.value.size === 0) return
  batchDelConfirm.value = true
}
async function executeBatchDelete() {
  batchDeling.value = true
  try {
    await batchDeleteBills([...selectedIds.value])
    batchDelConfirm.value = false
    selectedIds.value.clear()
    fetchData()
    fetchStats()
  } catch (e) { console.error('批量删除失败', e) } finally { batchDeling.value = false }
}

// ── Export ────────────────────────────────────────────────────────────
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try { await exportInstantBillList(buildSearchParams()) } catch (e) { console.error('导出失败', e) } finally { exporting.value = false }
}

// ── Template download ────────────────────────────────────────────────
const templating = ref(false)
async function handleTemplateDownload() {
  templating.value = true
  try { await downloadBillTemplate() } catch (e) { console.error('模板下载失败', e) } finally { templating.value = false }
}

// ── Import modal ─────────────────────────────────────────────────────
const importModal = ref(false)
const importFile = ref(null)
const importFileInput = ref(null)
const importForm = reactive({ categoryId: '' })
const importing = ref(false)
function openImport() { importFile.value = null; importForm.categoryId = ''; importModal.value = true }
function closeImport() { importModal.value = false }
function onImportFilePick(e) { importFile.value = e.target.files?.[0] || null }
async function submitImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const text = await importFile.value.text()
    const rows = parseCSV(text)
    if (rows.length === 0) { alert('文件中没有有效数据'); return }
    await batchImportBills({ bills: rows, categoryId: importForm.categoryId ? Number(importForm.categoryId) : undefined })
    importModal.value = false
    fetchData()
    fetchStats()
  } catch (e) { console.error('导入失败', e); alert('导入失败') } finally { importing.value = false }
}
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

// ── Billing track modal ──────────────────────────────────────────────
const trailModal = ref(false)
const trailLoading = ref(false)
const trailRecords = ref([])
const trailTotal = ref(0)
const trailRow = ref(null)
const trailPage = ref(1)
const trailFilterType = ref('')
const trailFilterStart = ref('')
const trailFilterEnd = ref('')
async function openTrail(row) {
  trailRow.value = row
  trailPage.value = 1
  trailFilterType.value = ''
  trailFilterStart.value = ''
  trailFilterEnd.value = ''
  trailModal.value = true
  await fetchTrail()
}
async function fetchTrail() {
  trailLoading.value = true
  try {
    const p = { pageNum: trailPage.value, pageSize: 10, meterId: trailRow.value?.meterId }
    if (trailFilterType.value) p.tradeType = Number(trailFilterType.value)
    if (trailFilterStart.value) p.startTime = trailFilterStart.value
    if (trailFilterEnd.value) p.endTime = trailFilterEnd.value
    const res = await getBillingTrackList(p)
    trailRecords.value = res.data?.records || []
    trailTotal.value = res.data?.total || 0
  } catch (e) { console.error('获取缴费轨迹失败', e) } finally { trailLoading.value = false }
}
function closeTrail() { trailModal.value = false }
async function handleTrailExport() {
  const p = { meterId: trailRow.value?.meterId }
  if (trailFilterType.value) p.tradeType = Number(trailFilterType.value)
  if (trailFilterStart.value) p.startTime = trailFilterStart.value
  if (trailFilterEnd.value) p.endTime = trailFilterEnd.value
  try { await exportBillingTrackList(p) } catch (e) { console.error('导出轨迹失败', e) }
}

// ── Bank account management ───────────────────────────────────────────
const bankAcctModal = ref(false)
const bankAccts = ref([])
const bankAcctSaving = ref(false)
const bankAcctForm = reactive({
  id: null, bankName: '', accountName: '', accountNumber: '',
  bankBranch: '', bankCode: '', accountType: '', contactPerson: '', contactPhone: '', remark: '', status: '1',
})
const bankAcctErrors = reactive({})

function openBankAcctModal() {
  Object.assign(bankAcctForm, {
    id: null, bankName: '', accountName: '', accountNumber: '',
    bankBranch: '', bankCode: '', accountType: '', contactPerson: '', contactPhone: '', remark: '', status: '1',
  })
  Object.keys(bankAcctErrors).forEach(k => delete bankAcctErrors[k])
  bankAcctModal.value = true
  loadBankAccts()
}
function closeBankAcctModal() { bankAcctModal.value = false }
async function loadBankAccts() {
  try {
    const res = await getBankAccountList({ pageSize: 100 })
    bankAccts.value = res.data?.records || []
  } catch (e) { console.error('加载银行账户失败', e) }
}
function editBankAcct(row) {
  bankAcctForm.id = row.id
  bankAcctForm.bankName = row.bankName || ''
  bankAcctForm.accountName = row.accountName || ''
  bankAcctForm.accountNumber = row.accountNumber || ''
  bankAcctForm.bankBranch = row.bankBranch || ''
  bankAcctForm.bankCode = row.bankCode || ''
  bankAcctForm.accountType = row.accountType || ''
  bankAcctForm.contactPerson = row.contactPerson || ''
  bankAcctForm.contactPhone = row.contactPhone || ''
  bankAcctForm.remark = row.remark || ''
  bankAcctForm.status = String(row.status ?? 1)
}
function resetBankAcctForm() {
  bankAcctForm.id = null
  bankAcctForm.bankName = ''
  bankAcctForm.accountName = ''
  bankAcctForm.accountNumber = ''
  bankAcctForm.bankBranch = ''
  bankAcctForm.bankCode = ''
  bankAcctForm.accountType = ''
  bankAcctForm.contactPerson = ''
  bankAcctForm.contactPhone = ''
  bankAcctForm.remark = ''
  bankAcctForm.status = '1'
}
async function saveBankAcct() {
  Object.keys(bankAcctErrors).forEach(k => delete bankAcctErrors[k])
  if (!bankAcctForm.bankName.trim()) { bankAcctErrors.bankName = '请输入银行名称'; return }
  if (!bankAcctForm.accountName.trim()) { bankAcctErrors.accountName = '请输入账户名称'; return }
  if (!bankAcctForm.accountNumber.trim()) { bankAcctErrors.accountNumber = '请输入银行账号'; return }
  bankAcctSaving.value = true
  try {
    await createBankAccount({
      bankName: bankAcctForm.bankName,
      accountName: bankAcctForm.accountName,
      accountNumber: bankAcctForm.accountNumber,
      bankBranch: bankAcctForm.bankBranch || undefined,
      bankCode: bankAcctForm.bankCode || undefined,
      accountType: bankAcctForm.accountType || undefined,
      contactPerson: bankAcctForm.contactPerson || undefined,
      contactPhone: bankAcctForm.contactPhone || undefined,
      remark: bankAcctForm.remark || undefined,
      status: Number(bankAcctForm.status),
    })
    resetBankAcctForm()
    loadBankAccts()
  } catch (e) { console.error('保存银行账户失败', e) } finally { bankAcctSaving.value = false }
}
async function delBankAcct(row) {
  if (!confirm(`确定删除银行账户 "${row.accountName}" 吗？`)) return
  try { await deleteBankAccount(row.id); loadBankAccts() } catch (e) { console.error('删除失败', e) }
}

// ── Bill category management ─────────────────────────────────────────
const billCatModal = ref(false)
const billCats = ref([])
const billCatSaving = ref(false)
const billCatForm = reactive({ id: null, categoryName: '', categoryCode: '', description: '', sortOrder: '', status: '1' })
const billCatErrors = reactive({})

function openBillCatModal() {
  billCatForm.id = null
  billCatForm.categoryName = ''
  billCatForm.categoryCode = ''
  billCatForm.description = ''
  billCatForm.sortOrder = ''
  billCatForm.status = '1'
  Object.keys(billCatErrors).forEach(k => delete billCatErrors[k])
  billCatModal.value = true
  loadBillCats()
}
function closeBillCatModal() { billCatModal.value = false }
async function loadBillCats() {
  try {
    const res = await getBillCategoryList({ pageSize: 100 })
    billCats.value = res.data?.records || []
  } catch (e) { console.error('加载账单分类失败', e) }
}
function editBillCat(row) {
  billCatForm.id = row.id
  billCatForm.categoryName = row.categoryName || ''
  billCatForm.categoryCode = row.categoryCode || ''
  billCatForm.description = row.description || ''
  billCatForm.sortOrder = row.sortOrder ?? ''
  billCatForm.status = String(row.status ?? 1)
}
function resetBillCatForm() {
  billCatForm.id = null
  billCatForm.categoryName = ''
  billCatForm.categoryCode = ''
  billCatForm.description = ''
  billCatForm.sortOrder = ''
  billCatForm.status = '1'
}
async function saveBillCat() {
  Object.keys(billCatErrors).forEach(k => delete billCatErrors[k])
  if (!billCatForm.categoryName.trim()) { billCatErrors.categoryName = '请输入分类名称'; return }
  if (!billCatForm.categoryCode.trim()) { billCatErrors.categoryCode = '请输入分类编码'; return }
  billCatSaving.value = true
  try {
    if (billCatForm.id) {
      await updateBillCategory(billCatForm.id, {
        categoryName: billCatForm.categoryName,
        categoryCode: billCatForm.categoryCode,
        description: billCatForm.description || undefined,
        sortOrder: billCatForm.sortOrder !== '' ? Number(billCatForm.sortOrder) : undefined,
        status: Number(billCatForm.status),
      })
    } else {
      await createBillCategory({
        categoryName: billCatForm.categoryName,
        categoryCode: billCatForm.categoryCode,
        description: billCatForm.description || undefined,
        sortOrder: billCatForm.sortOrder !== '' ? Number(billCatForm.sortOrder) : undefined,
        status: Number(billCatForm.status),
      })
    }
    resetBillCatForm()
    loadBillCats()
    loadCategories()
  } catch (e) { console.error('保存账单分类失败', e) } finally { billCatSaving.value = false }
}
async function delBillCat(row) {
  if (!confirm(`确定删除账单分类 "${row.categoryName}" 吗？`)) return
  try { await deleteBillCategory(row.id); loadBillCats(); loadCategories() } catch (e) { console.error('删除失败', e) }
}

// ── Bill records (账单记录) ────────────────────────────────────────────
const billRecords = ref([])
const billLoading = ref(false)
const billTotal = ref(0)
const billPage = ref(1)
const billPageSize = ref(20)
const billPages = computed(() => Math.max(1, Math.ceil(billTotal.value / billPageSize.value)))

const billFilterMerchantName = ref('')
const billFilterMeterNo = ref('')
const billFilterPaymentStatus = ref('')
const billFilterCreateStart = ref('')
const billFilterCreateEnd = ref('')

const paymentStatusOptions = [
  { value: '', label: '全部状态' },
  { value: '待审核', label: '待审核' },
  { value: '已支付', label: '已支付' },
  { value: '未支付', label: '未支付' },
]

function buildBillParams() {
  const p = { pageNum: billPage.value, pageSize: billPageSize.value }
  if (billFilterMerchantName.value.trim()) p.merchantName = billFilterMerchantName.value.trim()
  if (billFilterMeterNo.value.trim()) p.meterNo = billFilterMeterNo.value.trim()
  if (billFilterPaymentStatus.value) p.paymentStatus = billFilterPaymentStatus.value
  if (billFilterCreateStart.value) p.billCreateStart = billFilterCreateStart.value
  if (billFilterCreateEnd.value) p.billCreateEnd = billFilterCreateEnd.value
  return p
}

async function fetchBillRecords() {
  billLoading.value = true
  try {
    const res = await getBillList(buildBillParams())
    billRecords.value = res.data?.records || []
    billTotal.value = res.data?.total || 0
  } catch (e) { console.error('获取账单记录失败', e); billRecords.value = []; billTotal.value = 0 }
  finally { billLoading.value = false }
}

function resetBillFilters() {
  billFilterMerchantName.value = ''
  billFilterMeterNo.value = ''
  billFilterPaymentStatus.value = ''
  billFilterCreateStart.value = ''
  billFilterCreateEnd.value = ''
  billPage.value = 1
  fetchBillRecords()
}

function goBillPage(p) { if (p >= 1 && p <= billPages.value) { billPage.value = p; fetchBillRecords() } }

// Bill row selection
const billSelectedIds = ref(new Set())
function toggleBillSelect(id) {
  if (billSelectedIds.value.has(id)) billSelectedIds.value.delete(id)
  else billSelectedIds.value.add(id)
}
function toggleBillAll() {
  if (billSelectedIds.value.size === billRecords.value.length) billSelectedIds.value.clear()
  else billSelectedIds.value = new Set(billRecords.value.map(r => r.id))
}
const billAllChecked = computed(() => billRecords.value.length > 0 && billSelectedIds.value.size === billRecords.value.length)

// Edit modal
const billEditModal = ref(false)
const billEditSaving = ref(false)
const billEditForm = reactive({ id: null, paymentStatus: '', billName: '', remark: '' })
function openBillEdit(row) {
  billEditForm.id = row.id
  billEditForm.paymentStatus = row.paymentStatus || ''
  billEditForm.billName = row.billName || ''
  billEditForm.remark = row.remark || ''
  billEditModal.value = true
}
function closeBillEdit() { billEditModal.value = false }
async function submitBillEdit() {
  billEditSaving.value = true
  try {
    await updateBill(billEditForm.id, {
      paymentStatus: billEditForm.paymentStatus || undefined,
      billName: billEditForm.billName || undefined,
      remark: billEditForm.remark || undefined,
    })
    billEditModal.value = false
    fetchBillRecords()
  } catch (e) { console.error('更新账单失败', e) } finally { billEditSaving.value = false }
}

// Delete
const billDelConfirm = ref(false)
const billDeleting = ref(false)
async function doDeleteBill(id) {
  if (!confirm('确定删除该账单吗？')) return
  try { await deleteBill(id); fetchBillRecords() } catch (e) { console.error('删除失败', e) }
}
async function doBatchDeleteBills() {
  if (billSelectedIds.value.size === 0) return
  billDeleting.value = true
  try {
    await batchDeleteBillRecords([...billSelectedIds.value])
    billDelConfirm.value = false
    billSelectedIds.value.clear()
    fetchBillRecords()
  } catch (e) { console.error('批量删除失败', e) } finally { billDeleting.value = false }
}

// Export
const billExporting = ref(false)
async function handleBillExport() {
  billExporting.value = true
  try { await exportBillList(buildBillParams()) } catch (e) { console.error('导出失败', e) } finally { billExporting.value = false }
}

// Switch tab
function switchTab(tab) { activeTab.value = tab; if (tab === 'records') fetchBillRecords() }

onMounted(() => { fetchData(); fetchStats(); loadCategories() })
</script>

<template>
  <div>
    <!-- ═══ 0. Tab Switching ═══ -->
    <div class="flex gap-0 mb-4">
      <button :class="['py-2 px-5 text-[14px] font-medium rounded-t border-none cursor-pointer transition-colors', activeTab === 'instant' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666] border border-[#d9d9d9] hover:text-[#3a64ab]']" @click="switchTab('instant')">即时账单</button>
      <button :class="['py-2 px-5 text-[14px] font-medium rounded-t border-none cursor-pointer transition-colors ml-1', activeTab === 'records' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666] border border-[#d9d9d9] hover:text-[#3a64ab]']" @click="switchTab('records')">账单记录</button>
    </div>

    <!-- ═══════════════ 即时账单 Tab ═══════════════ -->
    <div v-show="activeTab === 'instant'">
    <!-- ═══ 1. Filter & Actions ═══ -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center mb-3">
        <input v-model="filterCustomerName" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="商户名" type="text" @keyup.enter="currentPage=1;fetchData()" />
        <input v-model="filterMeterNo" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="表号" type="text" @keyup.enter="currentPage=1;fetchData()" />
        <DropdownSelect v-model="filterCategoryId" :options="categoryOptions" placeholder="账单类别" />
        <DropdownSelect v-model="filterBillStatus" :options="billStatusOptions" placeholder="支付状态" />
        <DateRangePicker v-model:start-value="filterStartTime" v-model:end-value="filterEndTime" start-placeholder="时间起" end-placeholder="时间止" />
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="currentPage=1;fetchData();fetchStats()">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetFilters">重置</button>
      </div>
      <div class="flex flex-wrap gap-2 items-center pt-2 border-t border-[#f0f0f0]">
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="handleExport" :disabled="exporting">{{ exporting ? '导出中...' : '导出' }}</button>
        <button class="py-1.5 px-4 rounded bg-[#52c41a] text-white text-[13px] cursor-pointer border-none hover:bg-[#73d13d]" @click="openBatchGen">批量生成账单</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="handleTemplateDownload" :disabled="templating">{{ templating ? '下载中...' : '模板下载' }}</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="openImport">批量导入</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="openBatchDefer">批量延期</button>
        <div class="w-px h-5 bg-[#e8e8e8] mx-1"></div>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="openBankAcctModal">银行账户设置</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="openBillCatModal">账单分类</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#ff4d4f] text-[#ff4d4f] text-[13px] cursor-pointer hover:bg-[#fff2f0]" @click="openBatchDelete">批量删除</button>
      </div>
    </div>

    <!-- ═══ 2. Stat Chips ═══ -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div v-for="stat in statChips" :key="stat.label" class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors" :class="activeStat === stat.label ? 'bg-[#3a64ab] border-[#3a64ab] text-white' : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'" @click="applyStatFilter(stat.label)">
          {{ stat.label }}:
          <span class="ml-1">
            <template v-if="stat.label === '总数'">{{ stats.totalCount }}</template>
            <template v-else-if="stat.label === '未缴'">{{ stats.unpaidCount }}</template>
            <template v-else-if="stat.label === '已缴'">{{ stats.paidCount }}</template>
            <template v-else-if="stat.label === '逾期'">{{ stats.overdueCount }}</template>
          </span>
          <span v-if="stat.label !== '总数'">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- ═══ 3. Summary Cards ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[100px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">账单总数</div>
          <div class="text-xl font-bold">{{ stats.totalCount }}</div>
        </div>
        <div class="flex-1 min-w-[100px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">未缴</div>
          <div class="text-xl font-bold text-[#ff4d4f]">{{ stats.unpaidCount }}</div>
        </div>
        <div class="flex-1 min-w-[100px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">已缴</div>
          <div class="text-xl font-bold text-[#52c41a]">{{ stats.paidCount }}</div>
        </div>
        <div class="flex-1 min-w-[100px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">逾期</div>
          <div class="text-xl font-bold text-[#faad14]">{{ stats.overdueCount }}</div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">总金额</div>
          <div class="text-lg font-bold text-[#3a64ab]">¥ {{ formatMoney(stats.totalAmount) }}</div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[13px] text-[#666] mb-1">已缴金额</div>
          <div class="text-lg font-bold text-[#52c41a]">¥ {{ formatMoney(stats.paidAmount) }}</div>
        </div>
      </div>
    </div>

    <!-- ═══ 4. Table ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="overflow-x-auto relative" style="min-height:150px">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>
        <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
          <thead>
            <tr>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] w-[42px] text-center">
                <input type="checkbox" class="styled-checkbox" :checked="allChecked" @change="toggleAll" />
              </th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] w-[60px]">序号</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">表计名称</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">表号</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">商户名</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">当前读数</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">用量</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">倍率</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">单价</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">账单金额</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">当前余额</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">类型</th>
              <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">状态</th>
              <th class="py-3 px-2.5 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in tableData" :key="r.meterId" class="transition-colors" :class="selectedIds.has(r.meterId) ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'">
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-center">
                <input type="checkbox" class="styled-checkbox" :checked="selectedIds.has(r.meterId)" @click.stop="toggleSelect(r.meterId)" />
              </td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.meterName }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ r.meterNo }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.customerName }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ r.currentReading }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ r.consumption }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ r.multiplier }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ r.unitPrice }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-medium" :class="r.billAmount > 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">¥ {{ formatMoney(r.billAmount) }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono text-[#3a64ab]">¥ {{ formatMoney(r.currentBalance) }}</td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="r.meterType === 1 ? 'status-blue' : 'status-cyan'">{{ meterTypeLabel(r.meterType) }}</span>
              </td>
              <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="r.status === 1 ? 'status-green' : r.status === 2 ? 'status-yellow' : 'status-red'">{{ statusLabel(r.status) }}</span>
              </td>
              <td class="py-2.5 px-2.5 border-b border-[#e8e8e8] text-[13px] whitespace-nowrap">
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] mr-2" @click.stop="openTrail(r)">缴费轨迹</span>
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click.stop="openDetail(r)">详情</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无数据</div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages || 1 }} 页</span>
        <div class="flex items-center gap-1.5">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- ═══ 缴费轨迹 Modal ═══ -->
    <Teleport to="body">
      <div v-if="trailModal" class="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center" @click.self="closeTrail">
        <div class="bg-white rounded-lg shadow-2xl w-[760px] max-h-[85vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <h3 class="text-base font-medium">缴费轨迹 - {{ trailRow?.customerName }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeTrail">&times;</button>
          </div>
          <div class="p-5" v-if="trailRow">
            <div class="flex gap-6 mb-4 text-[13px]">
              <div><span class="text-[#666]">商户名：</span><span class="text-[#333]">{{ trailRow.customerName }}</span></div>
              <div><span class="text-[#666]">表号：</span><span class="text-[#333] font-mono">{{ trailRow.meterNo }}</span></div>
            </div>
            <div class="flex gap-3 items-center mb-4">
              <DropdownSelect v-model="trailFilterType" :options="tradeTypeOptions" placeholder="交易类型" />
              <DateRangePicker v-model:start-value="trailFilterStart" v-model:end-value="trailFilterEnd" start-placeholder="开始日期" end-placeholder="结束日期" />
              <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="trailPage=1;fetchTrail()">查询</button>
            </div>
            <div v-if="trailLoading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <template v-else>
              <table class="w-full border-collapse text-left mb-4" v-if="trailRecords.length > 0">
                <thead>
                  <tr>
                    <th class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">流水号</th>
                    <th class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">交易类型</th>
                    <th class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] text-right">变动前余额</th>
                    <th class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] text-right">变动金额</th>
                    <th class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] text-right">变动后余额</th>
                    <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">操作时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="t in trailRecords" :key="t.id" class="hover:bg-[#f5f6fb]">
                    <td class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] font-mono text-[#3a64ab]">{{ t.flowNo || '--' }}</td>
                    <td class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px]">{{ t.tradeTypeName || '--' }}</td>
                    <td class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">¥ {{ formatMoney(t.balanceBefore) }}</td>
                    <td class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono" :class="t.changeAmount < 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">¥ {{ formatMoney(t.changeAmount) }}</td>
                    <td class="py-2.5 px-3 border-b border-r border-[#e8e8e8] text-[12px] text-right font-mono">¥ {{ formatMoney(t.balanceAfter) }}</td>
                    <td class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-mono whitespace-nowrap">{{ t.operateTime || '--' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="text-center text-[#999] text-[13px] py-8">暂无缴费记录</div>
            </template>
            <div class="flex justify-between items-center text-[13px]">
              <span class="text-[#666]">共 {{ trailTotal }} 条</span>
              <div class="flex items-center gap-1.5">
                <button class="py-1 px-2.5 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer disabled:opacity-40" :disabled="trailPage <= 1" @click="trailPage--;fetchTrail()">上一页</button>
                <span class="text-[#3a64ab]">{{ trailPage }}</span>
                <button class="py-1 px-2.5 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer disabled:opacity-40" :disabled="trailRecords.length < 10" @click="trailPage++;fetchTrail()">下一页</button>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-2.5 p-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeTrail">关闭</button>
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="handleTrailExport">导出</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 详情 Modal ═══ -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[520px] max-h-[80vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <h3 class="text-base font-medium">账单详情</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>
          <div class="p-6">
            <div v-if="detailLoading" class="text-center text-[#999] text-[13px] py-8">加载中...</div>
            <div v-else-if="detailData" class="grid grid-cols-2 gap-x-6 gap-y-4 text-[13px]">
              <div><span class="text-[#666]">表计ID：</span><span class="text-[#333]">{{ detailData.meterId }}</span></div>
              <div><span class="text-[#666]">表号：</span><span class="text-[#333] font-mono">{{ detailData.meterNo || '--' }}</span></div>
              <div><span class="text-[#666]">商户名：</span><span class="text-[#333]">{{ detailData.customerName || '--' }}</span></div>
              <div><span class="text-[#666]">当前读数：</span><span class="text-[#333] font-mono">{{ detailData.currentReading ?? '--' }}</span></div>
              <div><span class="text-[#666]">上次结算读数：</span><span class="text-[#333] font-mono">{{ detailData.lastSettledReading ?? '--' }}</span></div>
              <div><span class="text-[#666]">用量：</span><span class="text-[#333] font-mono">{{ detailData.consumption ?? '--' }}</span></div>
              <div><span class="text-[#666]">倍率：</span><span class="text-[#333]">{{ detailData.multiplier ?? '--' }}</span></div>
              <div><span class="text-[#666]">单价：</span><span class="text-[#333]">¥ {{ formatMoney(detailData.unitPrice) }}</span></div>
              <div><span class="text-[#666]">账单金额：</span><span class="text-[#333] font-bold">¥ {{ formatMoney(detailData.billAmount) }}</span></div>
              <div><span class="text-[#666]">当前余额：</span><span class="text-[#333]">¥ {{ formatMoney(detailData.currentBalance) }}</span></div>
              <div class="col-span-2"><span class="text-[#666]">计算时间：</span><span class="text-[#333] font-mono">{{ detailData.calculateTime || '--' }}</span></div>
            </div>
          </div>
          <div class="flex justify-end p-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeDetail">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 批量生成账单 Modal ═══ -->
    <Teleport to="body">
      <div v-if="batchGenModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchGenModal = false">
        <div class="bg-white rounded-lg shadow-2xl w-[480px] p-6" @click.stop>
          <h3 class="text-base font-medium mb-4">批量生成账单（{{ selectedIds.size }} 个表计）</h3>
          <div class="space-y-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">账单分类</label>
              <DropdownSelect v-model="batchGenForm.categoryId" :options="categoryOptions.filter(c => c.value)" placeholder="请选择" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">账单说明</label>
              <input v-model="batchGenForm.billDescription" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如：2026年6月账单" type="text" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">固定金额（元，留空则实时计算）</label>
              <input v-model="batchGenForm.fixedAmount" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="0.00" type="number" step="0.01" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">备注</label>
              <input v-model="batchGenForm.remark" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="备注" type="text" />
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchGenModal = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="batchGenSaving || selectedIds.size === 0" @click="submitBatchGen">{{ batchGenSaving ? '生成中...' : selectedIds.size === 0 ? '请先勾选表计' : '确定生成' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 批量延期 Modal ═══ -->
    <Teleport to="body">
      <div v-if="batchDeferModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchDeferModal = false">
        <div class="bg-white rounded-lg shadow-2xl w-[420px] p-6" @click.stop>
          <h3 class="text-base font-medium mb-4">批量延期（{{ selectedIds.size }} 条）</h3>
          <div class="space-y-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 延期至</label>
              <input v-model="batchDeferForm.deferTo" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="datetime-local" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1">延期原因</label>
              <input v-model="batchDeferForm.deferReason" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如：商户申请延期缴费" type="text" />
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchDeferModal = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!batchDeferForm.deferTo || batchDeferSaving" @click="submitBatchDefer">{{ batchDeferSaving ? '提交中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 批量删除确认 ═══ -->
    <Teleport to="body">
      <div v-if="batchDelConfirm" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="batchDelConfirm = false">
        <div class="bg-white rounded-lg shadow-xl w-[400px] p-6" @click.stop>
          <h3 class="text-base font-medium mb-4">确认删除</h3>
          <p class="text-[13px] text-[#666] mb-5">确定要删除选中的 {{ selectedIds.size }} 条账单吗？</p>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="batchDelConfirm = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="batchDeling" @click="executeBatchDelete">{{ batchDeling ? '删除中...' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 批量导入 Modal ═══ -->
    <Teleport to="body">
      <div v-if="importModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeImport">
        <div class="bg-white rounded-lg shadow-2xl w-[480px] p-6" @click.stop>
          <h3 class="text-base font-medium mb-4">批量导入账单</h3>
          <div class="space-y-3">
            <div>
              <label class="text-[13px] text-[#666] block mb-1">账单分类</label>
              <DropdownSelect v-model="importForm.categoryId" :options="categoryOptions.filter(c => c.value)" placeholder="请选择" />
            </div>
            <div class="flex flex-col items-center pt-3">
              <input ref="importFileInput" type="file" accept=".csv,.xlsx" class="hidden" @change="onImportFilePick" />
              <button class="py-2 px-10 rounded bg-[#3a64ab] text-white text-[14px] cursor-pointer border-none mb-3" @click="importFileInput?.click()">选取文件</button>
              <p class="text-[12px] text-[#ff4d4f]">请确认上传的文件是批量模板！支持 .csv 和 .xlsx 文件</p>
              <p v-if="importFile" class="mt-3 text-[13px] text-[#3a64ab]">{{ importFile.name }}</p>
            </div>
          </div>
          <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeImport">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="!importFile || importing" @click="submitImport">{{ importing ? '导入中...' : '上传' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 银行账户设置 Modal ═══ -->
    <Teleport to="body">
      <div v-if="bankAcctModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeBankAcctModal">
        <div class="bg-white rounded-lg shadow-2xl w-[720px] max-h-[85vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">银行账户设置</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeBankAcctModal">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <!-- Account list -->
            <table class="w-full border-collapse text-left mb-4" v-if="bankAccts.length > 0">
              <thead>
                <tr>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">银行名称</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">账户名</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">账号</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">支行</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">状态</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="acct in bankAccts" :key="acct.id" class="hover:bg-[#f5f6fb]">
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">{{ acct.bankName }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">{{ acct.accountName }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px] font-mono">{{ acct.accountNumber }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">{{ acct.bankBranch || '--' }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">
                    <span class="status-tag" :class="acct.status === 1 ? 'status-green' : 'status-red'">{{ acct.status === 1 ? '启用' : '停用' }}</span>
                  </td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">
                    <span class="text-[#3a64ab] cursor-pointer mr-2" @click="editBankAcct(acct)">编辑</span>
                    <span class="text-[#ff4d4f] cursor-pointer" @click="delBankAcct(acct)">删除</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="text-center text-[#999] text-[13px] py-4 mb-4">暂无银行账户</div>

            <!-- Add form -->
            <fieldset class="border border-[#e8e8e8] rounded p-4">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">{{ bankAcctForm.id ? '编辑账户' : '新增账户' }}</legend>
              <div class="grid grid-cols-3 gap-3">
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">银行名称 <span class="text-[#ff4d4f]">*</span></label>
                  <input v-model="bankAcctForm.bankName" class="w-full py-1.5 px-2 border rounded text-[12px] outline-none" :class="bankAcctErrors.bankName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="如：中国工商银行" />
                  <p v-if="bankAcctErrors.bankName" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ bankAcctErrors.bankName }}</p>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">账户名称 <span class="text-[#ff4d4f]">*</span></label>
                  <input v-model="bankAcctForm.accountName" class="w-full py-1.5 px-2 border rounded text-[12px] outline-none" :class="bankAcctErrors.accountName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="如：XX有限公司" />
                  <p v-if="bankAcctErrors.accountName" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ bankAcctErrors.accountName }}</p>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">账号 <span class="text-[#ff4d4f]">*</span></label>
                  <input v-model="bankAcctForm.accountNumber" class="w-full py-1.5 px-2 border rounded text-[12px] outline-none" :class="bankAcctErrors.accountNumber ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="如：6222021234567890" />
                  <p v-if="bankAcctErrors.accountNumber" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ bankAcctErrors.accountNumber }}</p>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">开户支行</label>
                  <input v-model="bankAcctForm.bankBranch" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="如：XX支行" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">联行号</label>
                  <input v-model="bankAcctForm.bankCode" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="如：102100000001" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">账户类型</label>
                  <DropdownSelect v-model="bankAcctForm.accountType" :options="[{ value: '', label: '--' }, { value: 'COMPANY', label: '对公账户' }, { value: 'PERSONAL', label: '个人账户' }]" placeholder="--" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">联系人</label>
                  <input v-model="bankAcctForm.contactPerson" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="如：张三" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">联系电话</label>
                  <input v-model="bankAcctForm.contactPhone" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="如：13800000000" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">状态</label>
                  <DropdownSelect v-model="bankAcctForm.status" :options="[{ value: '1', label: '启用' }, { value: '0', label: '停用' }]" placeholder="状态" />
                </div>
              </div>
              <div class="mt-3">
                <label class="text-[12px] text-[#999] block mb-0.5">备注</label>
                <input v-model="bankAcctForm.remark" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="备注" />
              </div>
              <div class="mt-3 flex gap-2">
                <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[12px] cursor-pointer border-none disabled:opacity-50" :disabled="bankAcctSaving" @click="saveBankAcct">{{ bankAcctSaving ? '保存中...' : (bankAcctForm.id ? '更新' : '添加') }}</button>
                <button v-if="bankAcctForm.id" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[12px] cursor-pointer" @click="resetBankAcctForm">取消编辑</button>
              </div>
            </fieldset>
          </div>
          <div class="flex justify-end p-4 border-t border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeBankAcctModal">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 账单分类 Modal ═══ -->
    <Teleport to="body">
      <div v-if="billCatModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeBillCatModal">
        <div class="bg-white rounded-lg shadow-2xl w-[640px] max-h-[85vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">账单分类管理</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeBillCatModal">&times;</button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <table class="w-full border-collapse text-left mb-4" v-if="billCats.length > 0">
              <thead>
                <tr>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">分类名称</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">编码</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">排序</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">状态</th>
                  <th class="py-2.5 px-3 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb]">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="cat in billCats" :key="cat.id" class="hover:bg-[#f5f6fb]">
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">{{ cat.categoryName }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px] font-mono">{{ cat.categoryCode }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">{{ cat.sortOrder ?? '--' }}</td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">
                    <span class="status-tag" :class="cat.status === 1 ? 'status-green' : 'status-red'">{{ cat.status === 1 ? '启用' : '停用' }}</span>
                  </td>
                  <td class="py-2 px-3 border-b border-[#e8e8e8] text-[12px]">
                    <span class="text-[#3a64ab] cursor-pointer mr-2" @click="editBillCat(cat)">编辑</span>
                    <span class="text-[#ff4d4f] cursor-pointer" @click="delBillCat(cat)">删除</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="text-center text-[#999] text-[13px] py-4 mb-4">暂无账单分类</div>

            <fieldset class="border border-[#e8e8e8] rounded p-4">
              <legend class="text-[13px] font-medium text-[#3a64ab] px-1">{{ billCatForm.id ? '编辑分类' : '新增分类' }}</legend>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">分类名称 <span class="text-[#ff4d4f]">*</span></label>
                  <input v-model="billCatForm.categoryName" class="w-full py-1.5 px-2 border rounded text-[12px] outline-none" :class="billCatErrors.categoryName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="如：物业费" />
                  <p v-if="billCatErrors.categoryName" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ billCatErrors.categoryName }}</p>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">分类编码 <span class="text-[#ff4d4f]">*</span></label>
                  <input v-model="billCatForm.categoryCode" class="w-full py-1.5 px-2 border rounded text-[12px] outline-none" :class="billCatErrors.categoryCode ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" placeholder="如：PROPERTY_FEE" />
                  <p v-if="billCatErrors.categoryCode" class="text-[#ff4d4f] text-[11px] mt-0.5">{{ billCatErrors.categoryCode }}</p>
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">排序号</label>
                  <input v-model="billCatForm.sortOrder" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" type="number" placeholder="1" />
                </div>
                <div>
                  <label class="text-[12px] text-[#999] block mb-0.5">状态</label>
                  <DropdownSelect v-model="billCatForm.status" :options="[{ value: '1', label: '启用' }, { value: '0', label: '停用' }]" placeholder="状态" />
                </div>
                <div class="col-span-2">
                  <label class="text-[12px] text-[#999] block mb-0.5">描述</label>
                  <input v-model="billCatForm.description" class="w-full py-1.5 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none" placeholder="分类描述" />
                </div>
              </div>
              <div class="mt-3 flex gap-2">
                <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[12px] cursor-pointer border-none disabled:opacity-50" :disabled="billCatSaving" @click="saveBillCat">{{ billCatSaving ? '保存中...' : (billCatForm.id ? '更新' : '添加') }}</button>
                <button v-if="billCatForm.id" class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[12px] cursor-pointer" @click="resetBillCatForm">取消编辑</button>
              </div>
            </fieldset>
          </div>
          <div class="flex justify-end p-4 border-t border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeBillCatModal">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
    </div><!-- end v-show instant -->

    <!-- ═══════════════ 账单记录 Tab ═══════════════ -->
    <div v-show="activeTab === 'records'">
      <!-- Filters -->
      <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
        <div class="flex flex-wrap gap-[15px] items-center mb-3">
          <input v-model="billFilterMerchantName" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab]" placeholder="商户名" type="text" @keyup.enter="billPage=1;fetchBillRecords()" />
          <input v-model="billFilterMeterNo" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab]" placeholder="表号" type="text" @keyup.enter="billPage=1;fetchBillRecords()" />
          <DropdownSelect v-model="billFilterPaymentStatus" :options="paymentStatusOptions" placeholder="支付状态" />
          <DateRangePicker v-model:start-value="billFilterCreateStart" v-model:end-value="billFilterCreateEnd" start-placeholder="生成时间起" end-placeholder="生成时间止" />
          <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="billPage=1;fetchBillRecords()">查询</button>
          <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetBillFilters">重置</button>
        </div>
        <div class="flex flex-wrap gap-2 items-center pt-2 border-t border-[#f0f0f0]">
          <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" :disabled="billExporting" @click="handleBillExport">{{ billExporting ? '导出中...' : '导出' }}</button>
          <button class="py-1.5 px-4 rounded bg-white border border-[#ff4d4f] text-[#ff4d4f] text-[13px] cursor-pointer hover:bg-[#fff2f0]" @click="billDelConfirm = billSelectedIds.size > 0 ? true : null" :disabled="billSelectedIds.size === 0">批量删除</button>
        </div>
      </div>

      <!-- Table -->
      <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
        <div class="overflow-x-auto relative" style="min-height:150px">
          <div v-if="billLoading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
            <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
          </div>
          <table class="w-full border-collapse text-left" v-if="billRecords.length > 0">
            <thead>
              <tr>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] w-[42px] text-center">
                  <input type="checkbox" class="styled-checkbox" :checked="billAllChecked" @change="toggleBillAll" />
                </th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] w-[60px]">序号</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">账单编号</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">账单名称</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">商户名</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">表号</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-right">收款金额</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] text-center w-[90px]">支付状态</th>
                <th class="py-3 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb]">账单生成时间</th>
                <th class="py-3 px-2.5 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] w-[120px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, idx) in billRecords" :key="r.id" class="transition-colors" :class="billSelectedIds.has(r.id) ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'">
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-center">
                  <input type="checkbox" class="styled-checkbox" :checked="billSelectedIds.has(r.id)" @click.stop="toggleBillSelect(r.id)" />
                </td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (billPage - 1) * billPageSize + idx + 1 }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ r.billNo || '--' }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.billName || '--' }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.merchantName || '--' }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.meterNo || '--' }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono font-medium" :class="r.receivedAmount > 0 ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">¥ {{ formatMoney(r.receivedAmount || 0) }}</td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] text-center">
                  <span class="status-tag" :class="r.paymentStatus === '已支付' ? 'status-green' : r.paymentStatus === '待审核' ? 'status-yellow' : 'status-red'">{{ r.paymentStatus || '--' }}</span>
                </td>
                <td class="py-2.5 px-2.5 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ r.billCreateTime || '--' }}</td>
                <td class="py-2.5 px-2.5 border-b border-[#e8e8e8] text-[13px] whitespace-nowrap">
                  <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] mr-2" @click.stop="openBillEdit(r)">编辑</span>
                  <span class="text-[#ff4d4f] cursor-pointer hover:text-[#e04343]" @click.stop="doDeleteBill(r.id)">删除</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else-if="!billLoading" class="py-16 text-center text-[14px] text-[#999]">暂无账单记录</div>
        </div>

        <!-- Pagination -->
        <div v-if="billTotal > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
          <span class="text-[13px] text-[#666]">共 {{ billTotal }} 条记录，第 {{ billPage }} / {{ billPages }} 页</span>
          <div class="flex items-center gap-1.5">
            <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="billPage <= 1" @click="goBillPage(billPage - 1)">上一页</button>
            <template v-for="p in billPages" :key="p">
              <button v-if="billPages <= 7 || p === 1 || p === billPages || (p >= billPage - 1 && p <= billPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer" :class="billPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goBillPage(p)">{{ p }}</button>
              <span v-else-if="p === billPage - 2 || p === billPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
            </template>
            <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="billPage >= billPages" @click="goBillPage(billPage + 1)">下一页</button>
          </div>
        </div>
      </div>

      <!-- Edit bill modal -->
      <Teleport to="body">
        <div v-if="billEditModal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="closeBillEdit">
          <div class="bg-white rounded-lg shadow-2xl w-[440px] p-6" @click.stop>
            <h3 class="text-base font-medium mb-4">编辑账单</h3>
            <div class="space-y-3">
              <div>
                <label class="text-[13px] text-[#666] block mb-1">账单名称</label>
                <input v-model="billEditForm.billName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="text" />
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1">支付状态 <span class="text-[#ff4d4f]">*</span></label>
                <DropdownSelect v-model="billEditForm.paymentStatus" :options="paymentStatusOptions.filter(o => o.value)" placeholder="请选择支付状态" />
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1">备注</label>
                <input v-model="billEditForm.remark" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="text" placeholder="备注信息" />
              </div>
            </div>
            <div class="flex gap-2.5 justify-end mt-5 pt-4 border-t border-[#f0f0f0]">
              <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="closeBillEdit">取消</button>
              <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="billEditSaving" @click="submitBillEdit">{{ billEditSaving ? '保存中...' : '确定' }}</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- Batch delete confirm -->
      <Teleport to="body">
        <div v-if="billDelConfirm" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center" @click.self="billDelConfirm = false">
          <div class="bg-white rounded-lg shadow-xl w-[400px] p-6" @click.stop>
            <h3 class="text-base font-medium mb-4">确认删除</h3>
            <p class="text-[13px] text-[#666] mb-5">确定要删除选中的 {{ billSelectedIds.size }} 条账单记录吗？</p>
            <div class="flex gap-2.5 justify-end">
              <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="billDelConfirm = false">取消</button>
              <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50" :disabled="billDeleting" @click="doBatchDeleteBills">{{ billDeleting ? '删除中...' : '确认删除' }}</button>
            </div>
          </div>
        </div>
      </Teleport>
    </div><!-- end v-show records -->

  </div>
</template>
