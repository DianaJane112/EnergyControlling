<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getAllocationList, executePublicAllocation } from '@/api/publicAllocation.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

// ── Filter options ──────────────────────────────────────────────────
const allocationTypeOptions = [
  { value: 'PER_HOUSEHOLD', label: '按户均摊' },
  { value: 'BY_USAGE_RATIO', label: '按用量比例' },
]

const statusOptions = [
  { value: '0', label: '未执行' },
  { value: '1', label: '已执行' },
  { value: '2', label: '已冲销' },
]

// ── Filters ─────────────────────────────────────────────────────────
const filterName = ref('')
const filterAllocType = ref('')
const filterStatus = ref(null)

function resetFilters() {
  filterName.value = ''
  filterAllocType.value = ''
  filterStatus.value = null
  currentPage.value = 1
  fetchList()
}

// ── Data ────────────────────────────────────────────────────────────
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 8
const loading = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function fetchList() {
  loading.value = true
  try {
    const res = await getAllocationList({
      pageNum: currentPage.value,
      pageSize,
      allocationName: filterName.value || undefined,
      allocationType: filterAllocType.value || undefined,
      status: filterStatus.value !== null && filterStatus.value !== '' ? Number(filterStatus.value) : undefined,
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { records.value = []; total.value = 0 }
  finally { loading.value = false }
}

function goToPage(p) { if (p >= 1 && p <= totalPages.value) { currentPage.value = p; fetchList() } }

// ── Row selection ───────────────────────────────────────────────────
const selectedIds = ref(new Set())

function toggleSelectAll() {
  if (selectedIds.value.size === records.value.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(records.value.map(r => r.allocationId || r.id))
  }
}

function toggleSelect(id) {
  if (selectedIds.value.has(id)) selectedIds.value.delete(id)
  else selectedIds.value.add(id)
}

function clearSelection() { selectedIds.value = new Set() }

const allChecked = computed(() => records.value.length > 0 && selectedIds.value.size === records.value.length)

// ── Row highlight ───────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Batch select toggle ─────────────────────────────────────────────
function toggleBatchSelect() {
  if (selectedIds.value.size > 0) clearSelection()
  else selectedIds.value = new Set(records.value.map(r => r.allocationId || r.id))
}

// ── New allocation modal ────────────────────────────────────────────
const newModalOpen = ref(false)
const saving = ref(false)
const newForm = reactive({
  allocationName: '',
  projectId: '',
  sourceMeterId: '',
  totalAmount: '',
  allocationType: '',
  effectiveDate: '',
  targetMeterIds: '',
  remark: '',
})

function resetNewForm() {
  newForm.allocationName = ''
  newForm.projectId = ''
  newForm.sourceMeterId = ''
  newForm.totalAmount = ''
  newForm.allocationType = ''
  newForm.effectiveDate = ''
  newForm.targetMeterIds = ''
  newForm.remark = ''
}

function parseMeterIds(str) {
  return str.split(/[,，\s]+/).map(s => s.trim()).filter(Boolean).map(Number).filter(n => !isNaN(n))
}

async function submitNew() {
  const targetMeterIds = parseMeterIds(newForm.targetMeterIds)
  if (!newForm.projectId || !newForm.sourceMeterId || !newForm.totalAmount || Number(newForm.totalAmount) <= 0 || !newForm.allocationType || targetMeterIds.length === 0) return
  saving.value = true
  try {
    await executePublicAllocation({
      projectId: Number(newForm.projectId),
      sourceMeterId: Number(newForm.sourceMeterId),
      totalAmount: Number(newForm.totalAmount),
      allocationType: newForm.allocationType,
      targetMeterIds,
      allocationName: newForm.allocationName || undefined,
      effectiveDate: newForm.effectiveDate || undefined,
      remark: newForm.remark || undefined,
    })
    newModalOpen.value = false
    resetNewForm()
    fetchList()
  } catch { /* error handled silently */ }
  finally { saving.value = false }
}

// ── Status helpers ──────────────────────────────────────────────────
function statusLabel(s) {
  if (s === 0) return '未执行'
  if (s === 1) return '已执行'
  if (s === 2) return '已冲销'
  return '--'
}
function statusClass(s) {
  if (s === 1) return 'text-[#52c41a]'
  if (s === 2) return 'text-[#999]'
  return 'text-[#fa8c16]'
}
function allocTypeLabel(t) {
  if (t === 'PER_HOUSEHOLD') return '按户均摊'
  if (t === 'BY_USAGE_RATIO') return '按用量比例'
  return t || '--'
}

onMounted(() => fetchList())
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- Filter Area -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-5 items-center">
        <input
          v-model="filterName"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="分摊名称"
          type="text"
        />
        <DropdownSelect v-model="filterAllocType" :options="allocationTypeOptions" placeholder="分摊方式" />
        <DropdownSelect v-model="filterStatus" :options="statusOptions" placeholder="状态" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="currentPage = 1; fetchList()">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
        <div class="flex-1"></div>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="toggleBatchSelect">批量选择</button>
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="newModalOpen = true">新建分摊</button>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] relative min-h-[150px]">
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
        <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">
                <input type="checkbox" class="styled-checkbox" :checked="allChecked" @change="toggleSelectAll" />
              </th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分摊名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分摊方式</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">总分摊金额 (¥)</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">成功户数</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap text-right">失败户数</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">生效日期</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="records.length === 0 && !loading">
              <td colspan="10" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(row, idx) in records"
              :key="row.allocationId || row.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === (row.allocationId || row.id) ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(row.allocationId || row.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]" @click.stop>
                <input type="checkbox" class="styled-checkbox" :checked="selectedIds.has(row.allocationId || row.id)" @change="toggleSelect(row.allocationId || row.id)" />
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.allocationName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ allocTypeLabel(row.allocationType) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono">{{ (row.totalAmount || 0).toFixed(2) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono text-[#52c41a]">{{ row.successCount || 0 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-right font-mono" :class="(row.failCount || 0) > 0 ? 'text-[#ff4d4f]' : ''">{{ row.failCount || 0 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ row.effectiveDate || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d]">详情</span>
                <span v-if="row.status === 1" class="text-[#ff4d4f] cursor-pointer hover:text-[#ff7875]">冲销</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-3 text-[13px]">
          <span class="text-[#666]">已选择 <b class="text-[#3a64ab]">{{ selectedIds.size }}</b> 条</span>
          <span v-if="selectedIds.size > 0" class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click="clearSelection">取消选择</span>
          <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[12px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="selectedIds.size === 0">批量导出</button>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="text-[13px] text-[#666] mr-2">共 {{ total }} 条，{{ currentPage }} / {{ totalPages || 1 }}</span>
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

    <!-- New Allocation Modal -->
    <Teleport to="body">
      <div v-if="newModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="newModalOpen = false">
        <div class="bg-white rounded-lg shadow-2xl w-[540px]" @click.stop>
          <div class="flex justify-between items-center px-6 py-4 border-b border-[#f0f0f0]">
            <span class="text-[16px] font-medium text-[#333]">新建分摊</span>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="newModalOpen = false">&times;</button>
          </div>

          <div class="px-6 py-5">
            <div class="grid grid-cols-2 gap-x-8 gap-y-5">
              <div>
                <label class="text-[13px] text-[#333] block mb-2">分摊名称</label>
                <input v-model="newForm.allocationName" type="text" placeholder="选填" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">
                  <span class="text-[#ff4d4f]">*</span> 项目ID
                </label>
                <input v-model="newForm.projectId" type="text" placeholder="项目ID" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="!newForm.projectId ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">
                  <span class="text-[#ff4d4f]">*</span> 总表ID
                </label>
                <input v-model="newForm.sourceMeterId" type="text" placeholder="公区表/总表ID" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="!newForm.sourceMeterId ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">
                  <span class="text-[#ff4d4f]">*</span> 总分摊金额
                </label>
                <input v-model="newForm.totalAmount" type="text" placeholder="元" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab]" :class="!newForm.totalAmount || Number(newForm.totalAmount) <= 0 ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">
                  <span class="text-[#ff4d4f]">*</span> 分摊方式
                </label>
                <DropdownSelect v-model="newForm.allocationType" :options="allocationTypeOptions" placeholder="请选择" :z-index="110" />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">生效日期</label>
                <DateRangePicker
                  v-model:start-value="newForm.effectiveDate"
                  start-placeholder="选择日期"
                  :single="true"
                />
              </div>
              <div>
                <label class="text-[13px] text-[#333] block mb-2">备注</label>
                <input v-model="newForm.remark" type="text" placeholder="选填" class="w-full py-2 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" />
              </div>
            </div>
            <div class="mt-5">
              <label class="text-[13px] text-[#333] block mb-2">
                <span class="text-[#ff4d4f]">*</span> 目标电表ID（多个用逗号/空格分隔）
              </label>
              <textarea v-model="newForm.targetMeterIds" placeholder="例如：1001, 1002, 1003" class="w-full py-2 px-3 border rounded text-[13px] outline-none focus:border-[#3a64ab] resize-none" :class="!newForm.targetMeterIds.trim() ? 'border-[#ff4d4f]' : 'border-[#d9d9d9]'" rows="2"></textarea>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-[#f0f0f0] flex justify-end gap-3">
            <button class="py-1.5 px-5 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="newModalOpen = false">取消</button>
            <button
              class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="!newForm.projectId || !newForm.sourceMeterId || !newForm.totalAmount || Number(newForm.totalAmount) <= 0 || !newForm.allocationType || !newForm.targetMeterIds.trim() || saving"
              @click="submitNew"
            >{{ saving ? '提交中...' : '确定' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
