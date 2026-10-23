<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getMerchantList, exportMerchantList, createMerchant, updateMerchant,
  updateMerchantStatus, deleteMerchant, getWaterMerchantList, getElectricMerchantList,
  exportWaterMerchantList, exportElectricMerchantList,
} from '@/api/merchants.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'

// ── Dropdown options ──────────────────────────────────────────────────
const customerTypeOptions = [
  { value: '居民', label: '居民' },
  { value: '商业', label: '商业' },
  { value: '工业', label: '工业' },
]
const route = useRoute()
const isWaterCustomerPage = computed(() => route.path.includes('/merchant/water'))
const isElectricCustomerPage = computed(() => route.path.includes('/merchant/electric'))
const customerPageTitle = computed(() => isWaterCustomerPage.value ? '水表客户管理' : isElectricCustomerPage.value ? '电表客户管理' : '客户管理')
const bizTypeOptions = [
  { value: '0', label: '电' },
  { value: '1', label: '水' },
  { value: '2', label: '气' },
  { value: '3', label: '热' },
]
const statusOptions = [
  { value: '1', label: '启用' },
  { value: '0', label: '停用' },
]
const useNatureOptions = [
  { value: '居民用水', label: '居民用水' },
  { value: '商业用水', label: '商业用水' },
  { value: '工业用水', label: '工业用水' },
  { value: '居民用电', label: '居民用电' },
  { value: '商业用电', label: '商业用电' },
  { value: '工业用电', label: '工业用电' },
]

// ── Filters ──────────────────────────────────────────────────────────
const filterCustomerName = ref('')
const filterCustomerCode = ref('')
const filterAreaName = ref('')
const filterCustomerType = ref('')
const filterStatus = ref(null)

function resetFilters() {
  filterCustomerName.value = ''
  filterCustomerCode.value = ''
  filterAreaName.value = ''
  filterCustomerType.value = ''
  filterStatus.value = null
  currentPage.value = 1
  fetchData()
}

// ── Data & Pagination ───────────────────────────────────────────────
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    if (filterCustomerName.value.trim()) params.customerName = filterCustomerName.value.trim()
    if (filterCustomerCode.value.trim()) params.customerCode = filterCustomerCode.value.trim()
    if (filterAreaName.value.trim()) params.areaName = filterAreaName.value.trim()
    if (filterCustomerType.value) params.customerType = filterCustomerType.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = Number(filterStatus.value)
    const listApi = isWaterCustomerPage.value ? getWaterMerchantList : isElectricCustomerPage.value ? getElectricMerchantList : getMerchantList
    const res = await listApi(params)
    const data = res.data
    tableData.value = (data.records || []).map(r => ({
      id: r.id,
      customerCode: r.customerCode || '--',
      customerName: r.customerName || '--',
      areaName: r.areaName || '--',
      address: r.address || '--',
      phone: r.phone || '--',
      customerType: r.customerType || '--',
      useNature: r.useNature || '--',
      type: r.type,
      status: r.status,
      createTime: r.createTime || '--',
      operator: r.operator || '--',
      sign: r.sign || '--',
      remark: r.remark || '--',
      raw: r,
    }))
    total.value = data.total || 0
  } catch (e) { console.error('获取商户列表失败', e) } finally { loading.value = false }
}

function goToPage(p) { if (p >= 1 && p <= totalPages.value) { currentPage.value = p; fetchData() } }

// ── Row selection ────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Export ────────────────────────────────────────────────────────────
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try {
    const params = { pageNum: 1, pageSize: 9999 }
    if (filterCustomerName.value.trim()) params.customerName = filterCustomerName.value.trim()
    if (filterCustomerCode.value.trim()) params.customerCode = filterCustomerCode.value.trim()
    if (filterAreaName.value.trim()) params.areaName = filterAreaName.value.trim()
    if (filterCustomerType.value) params.customerType = filterCustomerType.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = Number(filterStatus.value)
    const exportApi = isWaterCustomerPage.value ? exportWaterMerchantList : isElectricCustomerPage.value ? exportElectricMerchantList : exportMerchantList
    await exportApi(params)
  } catch (e) { console.error('导出失败', e) } finally { exporting.value = false }
}

// ── Edit modal ───────────────────────────────────────────────────────
const editModal = ref(false)
const editSaving = ref(false)
const editingId = ref(null)
const editErrors = reactive({})
const editForm = ref({
  customerName: '', areaName: '', address: '', phone: '',
  customerType: '', useNature: '', operator: '', sign: '', remark: '',
})

function validateEditForm() {
  Object.keys(editErrors).forEach(k => delete editErrors[k])
  if (!editForm.value.customerName.trim()) editErrors.customerName = '请输入商户名称'
  if (!editForm.value.phone.trim()) editErrors.phone = '请输入电话'
  return Object.keys(editErrors).length === 0
}

function openEdit(row) {
  editingId.value = row.id
  editForm.value = {
    customerName: row.customerName,
    areaName: row.areaName,
    address: row.address,
    phone: row.phone,
    customerType: row.customerType,
    useNature: row.useNature,
    operator: row.operator,
    sign: row.sign,
    remark: row.remark,
  }
  Object.keys(editErrors).forEach(k => delete editErrors[k])
  editModal.value = true
}
function closeEdit() { editModal.value = false }
async function saveEdit() {
  if (!validateEditForm()) return
  editSaving.value = true
  try {
    await updateMerchant(editingId.value, editForm.value)
    closeEdit()
    fetchData()
  } catch (e) { console.error('更新商户失败', e) } finally { editSaving.value = false }
}

// ── Create modal ─────────────────────────────────────────────────────
const createModal = ref(false)
const createSaving = ref(false)
const createErrors = reactive({})
const createForm = ref({
  customerCode: '', customerName: '', areaName: '', address: '',
  phone: '', customerType: '', useNature: '', type: '', operator: '', sign: '', remark: '',
})

function validateCreateForm() {
  Object.keys(createErrors).forEach(k => delete createErrors[k])
  if (!createForm.value.customerCode.trim()) createErrors.customerCode = '请输入用户编号'
  if (!createForm.value.customerName.trim()) createErrors.customerName = '请输入商户名称'
  if (!createForm.value.phone.trim()) createErrors.phone = '请输入电话'
  return Object.keys(createErrors).length === 0
}

function openCreate() { createModal.value = true }
function closeCreate() {
  createModal.value = false
  createForm.value = { customerCode: '', customerName: '', areaName: '', address: '', phone: '', customerType: '', useNature: '', type: '', operator: '', sign: '', remark: '' }
  Object.keys(createErrors).forEach(k => delete createErrors[k])
}
async function submitCreate() {
  if (!validateCreateForm()) return
  createSaving.value = true
  try {
    await createMerchant(createForm.value)
    closeCreate()
    fetchData()
  } catch (e) { console.error('创建商户失败', e) } finally { createSaving.value = false }
}

// ── Status toggle ────────────────────────────────────────────────────
const statusChanging = ref(false)
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  statusChanging.value = true
  try { await updateMerchantStatus(row.id, newStatus); fetchData() }
  catch (e) { console.error('状态变更失败', e) } finally { statusChanging.value = false }
}

// ── Delete ───────────────────────────────────────────────────────────
const deleteConfirm = ref({ show: false, row: null })
const deleting = ref(false)
function confirmDelete(row) { deleteConfirm.value = { show: true, row } }
async function executeDelete() {
  const row = deleteConfirm.value.row
  if (!row) return
  deleting.value = true
  try { await deleteMerchant(row.id); deleteConfirm.value = { show: false, row: null }; fetchData() }
  catch (e) { console.error('注销商户失败', e) } finally { deleting.value = false }
}

// ── Helpers ──────────────────────────────────────────────────────────
function statusLabel(v) { return v === 1 ? '启用' : '停用' }
function bizTypeLabel(v) {
  const map = { '0': '电', '1': '水', '2': '气', '3': '热' }
  return map[v] || v || '--'
}

onMounted(() => fetchData())
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- Filter bar -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-4 items-center">
        <input v-model="filterCustomerName" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="商户名称" type="text" />
        <input v-model="filterCustomerCode" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="用户编号" type="text" />
        <input v-model="filterAreaName" class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[150px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="区域名称" type="text" />
        <DropdownSelect v-model="filterCustomerType" :options="customerTypeOptions" placeholder="用户类型" />
        <DropdownSelect v-model="filterStatus" :options="statusOptions" placeholder="状态" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="currentPage = 1; fetchData()">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="openCreate">新增商户</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleExport" :disabled="exporting">{{ exporting ? '导出中...' : '导出' }}</button>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">用户编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">电话</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">区域名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">用户类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">用能性质</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">业务类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableData.length === 0">
              <td :colspan="12" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(row, idx) in tableData"
              :key="row.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === row.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(row.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.customerCode }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.customerName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ row.phone }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.areaName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.customerType }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.useNature }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ bizTypeLabel(row.type) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag cursor-pointer" :class="row.status === 1 ? 'status-green' : 'status-red'" @click.stop="toggleStatus(row)">{{ statusLabel(row.status) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ row.createTime }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d] whitespace-nowrap" @click.stop="openEdit(row)">编辑</span>
                <span class="text-[#ff4d4f] cursor-pointer hover:text-[#ff7875] whitespace-nowrap" @click.stop="confirmDelete(row)">归档客户</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录</span>
        <div class="flex items-center gap-2 text-[13px]">
          <select v-model.number="pageSize" class="py-1.5 px-2 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white" @change="currentPage = 1; fetchData()">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goToPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
          <span class="text-[#666] ml-1">跳至</span>
          <input class="w-[40px] py-1.5 px-1 border border-[#d9d9d9] rounded text-[13px] outline-none text-center" type="text" @keyup.enter="goToPage(Number($event.target.value))" />
          <span class="text-[#666]">页</span>
        </div>
      </div>
    </div>

    <!-- Edit Modal -->
    <Teleport to="body">
      <div v-if="editModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeEdit">
        <div class="bg-white rounded-lg shadow-2xl w-[520px] max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <h3 class="text-base font-medium">编辑商户信息</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeEdit">&times;</button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5"><span class="text-[#ff4d4f]">*</span> 商户名称</label>
              <input v-model="editForm.customerName" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors" :class="editErrors.customerName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="text" />
              <p v-if="editErrors.customerName" class="text-[#ff4d4f] text-[12px] mt-0.5">{{ editErrors.customerName }}</p>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5"><span class="text-[#ff4d4f]">*</span> 电话</label>
                <input v-model="editForm.phone" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors" :class="editErrors.phone ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="text" />
                <p v-if="editErrors.phone" class="text-[#ff4d4f] text-[12px] mt-0.5">{{ editErrors.phone }}</p>
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">区域名称</label>
                <input v-model="editForm.areaName" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
              </div>
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">地址</label>
              <input v-model="editForm.address" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">用户类型</label>
                <DropdownSelect v-model="editForm.customerType" :options="customerTypeOptions" placeholder="请选择" />
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">用能性质</label>
                <DropdownSelect v-model="editForm.useNature" :options="useNatureOptions" placeholder="请选择" />
              </div>
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">运营商</label>
              <input v-model="editForm.operator" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">签订</label>
              <input v-model="editForm.sign" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">备注</label>
              <textarea v-model="editForm.remark" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none" rows="2"></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-2.5 p-5 border-t border-[#f0f0f0]">
            <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab]" @click="closeEdit">取消</button>
            <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="editSaving" @click="saveEdit">{{ editSaving ? '保存中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create Modal -->
    <Teleport to="body">
      <div v-if="createModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeCreate">
        <div class="bg-white rounded-lg shadow-2xl w-[520px] max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0]">
            <h3 class="text-base font-medium">新增商户</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeCreate">&times;</button>
          </div>
          <div class="p-5 space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5"><span class="text-[#ff4d4f]">*</span> 用户编号</label>
                <input v-model="createForm.customerCode" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors" :class="createErrors.customerCode ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="text" />
                <p v-if="createErrors.customerCode" class="text-[#ff4d4f] text-[12px] mt-0.5">{{ createErrors.customerCode }}</p>
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5"><span class="text-[#ff4d4f]">*</span> 商户名称</label>
                <input v-model="createForm.customerName" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors" :class="createErrors.customerName ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="text" />
                <p v-if="createErrors.customerName" class="text-[#ff4d4f] text-[12px] mt-0.5">{{ createErrors.customerName }}</p>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5"><span class="text-[#ff4d4f]">*</span> 电话</label>
                <input v-model="createForm.phone" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] transition-colors" :class="createErrors.phone ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" type="text" />
                <p v-if="createErrors.phone" class="text-[#ff4d4f] text-[12px] mt-0.5">{{ createErrors.phone }}</p>
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">区域名称</label>
                <input v-model="createForm.areaName" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
              </div>
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">地址</label>
              <input v-model="createForm.address" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">用户类型</label>
                <DropdownSelect v-model="createForm.customerType" :options="customerTypeOptions" placeholder="请选择" />
              </div>
              <div>
                <label class="text-[13px] text-[#666] block mb-1.5">用能性质</label>
                <DropdownSelect v-model="createForm.useNature" :options="useNatureOptions" placeholder="请选择" />
              </div>
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">业务类型</label>
              <DropdownSelect v-model="createForm.type" :options="bizTypeOptions" placeholder="请选择" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">运营商</label>
              <input v-model="createForm.operator" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">签订</label>
              <input v-model="createForm.sign" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" type="text" />
            </div>
            <div>
              <label class="text-[13px] text-[#666] block mb-1.5">备注</label>
              <textarea v-model="createForm.remark" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none" rows="2"></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-2.5 p-5 border-t border-[#f0f0f0]">
            <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab]" @click="closeCreate">取消</button>
            <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="createSaving" @click="submitCreate">{{ createSaving ? '创建中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirm Modal -->
    <Teleport to="body">
      <div v-if="deleteConfirm.show" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="deleteConfirm.show = false">
        <div class="bg-white rounded-lg shadow-2xl p-6 w-[400px]">
          <h3 class="text-base font-medium mb-4">确认归档客户</h3>
          <p class="text-[13px] text-[#666] mb-4">确定要归档客户 "{{ deleteConfirm.row?.customerName }}" 吗？归档后将不再出现在正常客户列表，但关联表计、虚拟表关系、历史读数和账单都会保留。</p>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="deleteConfirm.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="deleting" @click="executeDelete">{{ deleting ? '归档中...' : '确认归档' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
