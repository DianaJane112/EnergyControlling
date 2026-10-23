<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getCollectorPage,
  getCollectorDetail,
  getCollectorMeters,
  getCollectorStatistics,
  exportCollector,
  bindAllOnlineMeters,
  batchImportCollectors,
  deleteCollector,
  unbindAllMetersFromCollector,
  unbindMeterFromCollector,
} from '@/api/collector.js'
import * as XLSX from 'xlsx'

// ── Filters ────────────────────────────────────────────────────────
const filterKeyword = ref('')

// ── Pagination & data ──────────────────────────────────────────────
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(8)
const loading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// ── Stats ──────────────────────────────────────────────────────────
const stats = reactive({ totalCollectors: 0, areaStats: [] })
const activeStat = ref('')

async function fetchStats() {
  try {
    const res = await getCollectorStatistics()
    if (res.data) {
      stats.totalCollectors = res.data.totalCollectors || 0
      stats.areaStats = res.data.areaStats || []
    }
  } catch (e) {
    console.error('获取基站统计失败', e)
  }
}

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  currentPage.value = 1
  fetchPage()
}

// ── Fetch page ─────────────────────────────────────────────────────
async function fetchPage() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    const kw = filterKeyword.value.trim()
    if (kw) params.keyword = kw
    if (activeStat.value) {
      const area = stats.areaStats.find(a => (a.areaName || a.areaCode) === activeStat.value)
      if (area) params.areaCode = area.areaCode
    }
    const res = await getCollectorPage(params)
    const data = res.data
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取基站列表失败', e)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchPage()
}

function resetFilters() {
  filterKeyword.value = ''
  currentPage.value = 1
  fetchPage()
}

function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchPage()
  }
}

// ── Row selection ──────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Context menu ───────────────────────────────────────────────────
const contextMenu = reactive({ show: false, x: 0, y: 0, row: null })
function showContext(e, row) {
  e.preventDefault()
  contextMenu.show = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.row = row
}
function hideContext() { contextMenu.show = false }

async function handleDeleteStation(row) {
  hideContext()
  if (!confirm(`确认删除基站「${row.collName || row.collCode}」？`)) return
  try {
    await deleteCollector(row.id)
    fetchPage()
    fetchStats()
  } catch (e) {
    alert('删除失败：' + (e.message || '未知错误'))
  }
}

// ── Detail modal ───────────────────────────────────────────────────
const detailModal = ref(false)
const detailStation = ref(null)
const boundMeters = ref([])
const detailLoading = ref(false)

// bound-meter filter & pagination
const detailFilterId = ref('')
const detailCurrentPage = ref(1)
const detailPageSize = ref(10)
const boundMetersTotal = ref(0)

const detailTotalPages = computed(() => Math.ceil(boundMetersTotal.value / detailPageSize.value) || 1)

async function openDetail(station) {
  detailStation.value = null
  boundMeters.value = []
  detailFilterId.value = ''
  detailCurrentPage.value = 1
  detailModal.value = true
  detailLoading.value = true
  try {
    const [detailRes, metersRes] = await Promise.all([
      getCollectorDetail(station.id),
      getCollectorMeters(station.id, { pageNum: 1, pageSize: detailPageSize.value }),
    ])
    detailStation.value = detailRes.data
    const metersData = metersRes.data
    boundMeters.value = metersData.records || []
    boundMetersTotal.value = metersData.total || 0
  } catch (e) {
    console.error('获取基站详情失败', e)
  } finally {
    detailLoading.value = false
  }
}

async function fetchBoundMeters() {
  if (!detailStation.value) return
  try {
    const params = { pageNum: detailCurrentPage.value, pageSize: detailPageSize.value }
    const kw = detailFilterId.value.trim()
    if (kw) params.keyword = kw
    const metersRes = await getCollectorMeters(detailStation.value.id, params)
    const metersData = metersRes.data
    boundMeters.value = metersData.records || []
    boundMetersTotal.value = metersData.total || 0
  } catch (e) {
    console.error('获取绑定表计失败', e)
  }
}

function handleDetailSearch() {
  detailCurrentPage.value = 1
  fetchBoundMeters()
}

function detailResetFilter() {
  detailFilterId.value = ''
  detailCurrentPage.value = 1
  fetchBoundMeters()
}

function detailGoToPage(page) {
  if (page >= 1 && page <= detailTotalPages.value) {
    detailCurrentPage.value = page
    fetchBoundMeters()
  }
}

function closeDetail() { detailModal.value = false }

// ── Detail modal: unbind / bind actions ────────────────────────────
async function handleUnbindAll() {
  if (!detailStation.value) return
  if (!confirm('确认解绑该基站下所有电表/水表？')) return
  try {
    const res = await unbindAllMetersFromCollector(detailStation.value.id)
    alert(res.msg || '解绑成功')
    fetchBoundMeters()
    fetchStats()
  } catch (e) {
    alert('解绑失败：' + (e.message || '未知错误'))
  }
}

async function handleUnbindMeter(meterId) {
  if (!detailStation.value) return
  if (!confirm('确认解除该表计的绑定？')) return
  try {
    await unbindMeterFromCollector(detailStation.value.id, meterId)
    fetchBoundMeters()
    fetchStats()
  } catch (e) {
    alert('解绑失败：' + (e.message || '未知错误'))
  }
}

// ── Export ─────────────────────────────────────────────────────────
async function handleExport() {
  try {
    const params = {}
    const kw = filterKeyword.value.trim()
    if (kw) params.keyword = kw
    const res = await exportCollector(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const disposition = res.headers['content-disposition']
    let fileName = '基站列表.xlsx'
    if (disposition) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
      }
    }
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出失败', e)
    alert('导出失败：' + (e.message || '未知错误'))
  }
}

// ── One-click bind ─────────────────────────────────────────────────
async function handleBindAll() {
  if (!selectedRowId.value) {
    alert('请先在表格中点击选中一个基站')
    return
  }
  if (!confirm('确认对该基站执行一键扫描绑定在线表计？')) return
  try {
    const res = await bindAllOnlineMeters(selectedRowId.value)
    alert(res.msg || '绑定完成')
    fetchPage()
    fetchStats()
  } catch (e) {
    alert('一键绑定失败：' + (e.message || '未知错误'))
  }
}

// ── Batch import ───────────────────────────────────────────────────
function handleBatchImport() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    try {
      const data = await readExcelFile(file)
      if (!data || data.length === 0) {
        alert('文件中没有数据')
        return
      }
      if (data.length > 500) {
        alert('单次最多导入 500 条记录')
        return
      }
      // Map Excel columns to CollectorRequest fields
      const requests = data.map(row => ({
        collCode: String(row['集中器编号'] || row['collCode'] || row['编号'] || ''),
        collName: row['集中器名称'] || row['collName'] || row['名称'] || '',
        factCode: row['厂商代码'] || row['factCode'] || '',
        location: row['安装位置'] || row['location'] || '',
        areaCode: row['区域代码'] || row['areaCode'] || '',
        memo: row['备注'] || row['memo'] || '',
        sign: row['公司签名'] || row['sign'] || '',
      })).filter(r => r.collCode)
      if (requests.length === 0) {
        alert('未识别到有效数据，请检查 Excel 列名')
        return
      }
      if (!confirm(`即将导入 ${requests.length} 条基站记录，确认？`)) return
      const res = await batchImportCollectors(requests)
      alert(res.msg || `成功导入 ${requests.length} 条记录`)
      fetchPage()
      fetchStats()
    } catch (err) {
      console.error('批量导入失败', err)
      const backendMsg = err.response?.data?.msg || err.message || '未知错误'
      alert('批量导入失败：' + backendMsg)
    }
  }
  input.click()
}

function readExcelFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const workbook = XLSX.read(e.target.result, { type: 'array' })
        const sheetName = workbook.SheetNames[0]
        const sheet = workbook.Sheets[sheetName]
        const data = XLSX.utils.sheet_to_json(sheet)
        resolve(data)
      } catch (err) {
        reject(err)
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

// ── Helpers ────────────────────────────────────────────────────────
function getMeterStatusLabel(status) {
  if (status === 1) return '正常'
  if (status === 2) return '欠费停机'
  return '停用'
}

function getOnlineLabel(online) {
  return online === 1 ? '在线' : '离线'
}

// ── Lifecycle ──────────────────────────────────────────────────────
onMounted(() => {
  fetchPage()
  fetchStats()
})
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- ═══ Box 1: Filter Bar ═══ -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input
          v-model="filterKeyword"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[260px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="基站名称或基站编号"
          type="text"
          @keyup.enter="handleSearch"
        />
        <button
          class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors"
          @click="handleSearch"
        >查询</button>
        <button
          class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          @click="resetFilters"
        >重置</button>
      </div>
    </div>

    <!-- ═══ Box 2: Stat Chips ═══ -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '总数'
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter('总数')"
        >
          总数: <span class="ml-1">{{ stats.totalCollectors }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
        <div
          v-for="area in stats.areaStats"
          :key="area.areaCode"
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === (area.areaName || area.areaCode)
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter(area.areaName || area.areaCode)"
        >
          {{ area.areaName || area.areaCode }}: <span class="ml-1">{{ area.count }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- ═══ Box 3: Batch Operations ═══ -->
    <div class="bg-white rounded p-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex flex-wrap gap-[15px] items-center">
        <button
          class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          @click="handleExport"
        >导出</button>
        <button
          class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors"
          @click="handleBindAll"
        >一键绑定</button>
        <button
          class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          @click="handleBatchImport"
        >批量导入</button>
      </div>
    </div>

    <!-- ═══ Table Panel ═══ -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <!-- Loading overlay -->
      <div v-if="loading" class="text-center py-10 text-[13px] text-[#999]">加载中...</div>

      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">基站名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">基站编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">厂商代码</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">安装位置</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">区域代码</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">上报时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建人</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(station, idx) in tableData"
              :key="station.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === station.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(station.id)"
              @contextmenu="showContext($event, station)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium">{{ station.collName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#3a64ab] font-mono">{{ station.collCode || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ station.factCode || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ station.location || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ station.areaCode || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ station.updateTime || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ station.createBy || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click.stop="openDetail(station)">详情</span>
              </td>
            </tr>
            <tr v-if="tableData.length === 0">
              <td :colspan="9" class="py-10 text-center text-[13px] text-[#999]">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages || 1 }} 页</span>
        <div class="flex items-center gap-1.5">
          <button
            class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
          >上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)"
              class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors"
              :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
              @click="goToPage(p)"
            >{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button
            class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
          >下一页</button>
        </div>
      </div>
    </div>

    <!-- ═══ Context Menu ═══ -->
    <Teleport to="body">
      <div v-if="contextMenu.show" class="fixed inset-0 z-[70]" @click="hideContext">
        <div
          class="absolute bg-white border border-[#d9d9d9] rounded shadow-lg py-1 min-w-[150px]"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
          @click.stop
        >
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f5f5] cursor-pointer" @click="openDetail(contextMenu.row); hideContext()">查看详情</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f5f5] cursor-pointer text-[#ff4d4f]" @click="handleDeleteStation(contextMenu.row)">删除基站</div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ Detail Modal ═══ -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[60] flex items-start justify-center pt-[40px] pb-[40px]" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[960px] max-h-full overflow-y-auto flex flex-col">
          <!-- Header -->
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">基站详情 - {{ detailStation?.collName || '--' }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>

          <!-- Loading -->
          <div v-if="detailLoading" class="p-10 text-center text-[13px] text-[#999]">加载中...</div>

          <div v-else-if="detailStation" class="p-5 overflow-y-auto flex-1">
            <!-- ═══ Station Info Card ═══ -->
            <div class="flex gap-5 mb-5">
              <div class="flex-1 grid grid-cols-2 gap-x-8 gap-y-3">
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">基站名称</span><span class="text-[13px] text-[#333]">{{ detailStation.collName || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">安装位置</span><span class="text-[13px] text-[#333]">{{ detailStation.location || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">基站编号</span><span class="text-[13px] text-[#333] font-mono">{{ detailStation.collCode || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">区域代码</span><span class="text-[13px] text-[#333] font-mono">{{ detailStation.areaCode || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">厂商代码</span><span class="text-[13px] text-[#333] font-mono">{{ detailStation.factCode || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">商户名称</span><span class="text-[13px] text-[#333]">{{ detailStation.customerName || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">备注</span><span class="text-[13px] text-[#333]">{{ detailStation.memo || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">创建时间</span><span class="text-[13px] text-[#333]">{{ detailStation.createTime || '--' }}</span></div>
                <div class="flex"></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">创建人</span><span class="text-[13px] text-[#333]">{{ detailStation.createBy || '--' }}</span></div>
              </div>

              <div class="flex flex-col gap-2.5 shrink-0">
                <button
                  class="py-2 px-5 rounded bg-white border border-[#ff4d4f] text-[#ff4d4f] text-[13px] cursor-pointer hover:bg-[#fff2f0] transition-colors whitespace-nowrap"
                  @click="handleUnbindAll"
                >解绑所有电表</button>
              </div>
            </div>

            <!-- ═══ Bound Meter List ═══ -->
            <div class="border-t border-[#f0f0f0] pt-5">
              <h4 class="text-[14px] font-medium mb-3">已绑定电表/水表</h4>

              <!-- Filter -->
              <div class="flex flex-wrap gap-3 items-center mb-3">
                <input
                  v-model="detailFilterId"
                  class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors"
                  placeholder="请输入表号或名称"
                  type="text"
                  @keyup.enter="handleDetailSearch"
                />
                <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="handleDetailSearch">查询</button>
                <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="detailResetFilter">重置</button>
              </div>

              <div class="overflow-x-auto">
                <table class="w-full border-collapse text-left">
                  <thead>
                    <tr>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表计名称</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">钢印号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表计类型</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">运行状态</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">通讯状态</th>
                      <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(meter, idx) in boundMeters" :key="meter.id" class="hover:bg-[#f5f6fb]">
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ (detailCurrentPage - 1) * detailPageSize + idx + 1 }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.name || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono">{{ meter.stealNo || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono text-[#3a64ab]">{{ meter.dbNo || meter.meteraddress || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.meterTypeName || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.status === 1 ? 'text-[#52c41a]' : meter.status === 2 ? 'text-[#fa8c16]' : 'text-[#ff4d4f]'">{{ getMeterStatusLabel(meter.status) }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.online === 1 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ getOnlineLabel(meter.online) }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px]">
                        <span class="text-[#ff4d4f] cursor-pointer hover:text-[#ff7875]" @click="handleUnbindMeter(meter.id)">解除绑定</span>
                      </td>
                    </tr>
                    <tr v-if="boundMeters.length === 0">
                      <td :colspan="8" class="py-10 text-center text-[13px] text-[#999]">暂无绑定电表/水表</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Pagination -->
              <div class="flex justify-between items-center mt-3 pt-2.5 border-t border-[#f0f0f0]">
                <span class="text-[12px] text-[#666]">共 {{ boundMetersTotal }} 条</span>
                <div class="flex items-center gap-2 text-[12px]">
                  <select
                    class="py-1 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none bg-white"
                    v-model.number="detailPageSize"
                    @change="detailCurrentPage = 1; fetchBoundMeters()"
                  >
                    <option :value="10">10 条/页</option>
                    <option :value="20">20 条/页</option>
                    <option :value="50">50 条/页</option>
                  </select>
                  <button
                    class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed"
                    :disabled="detailCurrentPage <= 1"
                    @click="detailGoToPage(detailCurrentPage - 1)"
                  >上一页</button>
                  <span class="text-[#666]">{{ detailCurrentPage }} / {{ detailTotalPages || 1 }}</span>
                  <button
                    class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed"
                    :disabled="detailCurrentPage >= detailTotalPages"
                    @click="detailGoToPage(detailCurrentPage + 1)"
                  >下一页</button>
                  <span class="text-[#666]">跳至</span>
                  <input
                    class="w-[40px] py-1 px-1 border border-[#d9d9d9] rounded text-[12px] outline-none text-center"
                    type="text"
                    @keyup.enter="detailGoToPage(Number($event.target.value))"
                  />
                  <span class="text-[#666]">页</span>
                </div>
              </div>
            </div>
          </div>

          <!-- No data fallback in modal -->
          <div v-else class="p-10 text-center text-[13px] text-[#999]">未找到基站信息</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
