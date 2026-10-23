<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getCollectorPage, getCollectorStatistics, getCollectorDetail,
  getCollectorMeters, deleteCollector,
  unbindAllMetersFromCollector, bindAllOnlineMeters, unbindMeterFromCollector,
} from '@/api/collector.js'

// ── Stats ──────────────────────────────────────────────────────────
const stats = ref({ totalCount: 0, onlineCount: 0, offlineCount: 0, totalBoundMeters: 0 })

async function fetchStats() {
  try {
    const res = await getCollectorStatistics()
    if (res.data) stats.value = res.data
  } catch { /* ignore */ }
}

const totalStations = computed(() => stats.value.totalCount || 0)
const onlineCount = computed(() => stats.value.onlineCount || 0)
const offlineCount = computed(() => stats.value.offlineCount || 0)
const totalBoundMeters = computed(() => stats.value.totalBoundMeters || 0)

const activeStat = ref('')

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  currentPage.value = 1
  fetchList()
}

// ── Filters ────────────────────────────────────────────────────────
const filterKeyword = ref('')

function resetFilters() {
  filterKeyword.value = ''
  activeStat.value = ''
  currentPage.value = 1
  fetchList()
}

// ── List data ──────────────────────────────────────────────────────
const list = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 8
const loading = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function fetchList() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize }
    if (filterKeyword.value.trim()) params.keyword = filterKeyword.value.trim()
    if (activeStat.value === '在线') params.commStatus = 'online'
    else if (activeStat.value === '离线') params.commStatus = 'offline'
    const res = await getCollectorPage(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { list.value = []; total.value = 0 }
  finally { loading.value = false }
}

function goPage(n) { if (n >= 1 && n <= totalPages.value) { currentPage.value = n; fetchList() } }

// ── Row selection ──────────────────────────────────────────────────
const selectedIds = ref(new Set())
function toggleStation(id) {
  const s = new Set(selectedIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  selectedIds.value = s
}
function toggleSelectAll() {
  if (selectedIds.value.size === list.value.length && list.value.length > 0) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(list.value.map(s => s.id))
  }
}
const selectedCount = computed(() => selectedIds.value.size)

// ── Batch bind ────────────────────────────────────────────────────
const binding = ref(false)
async function handleBatchBind() {
  if (selectedIds.value.size === 0) return
  binding.value = true
  try {
    for (const id of selectedIds.value) {
      await bindAllOnlineMeters(id)
    }
    selectedIds.value = new Set()
    fetchList()
    fetchStats()
  } catch (e) { console.error('批量绑定失败', e) }
  finally { binding.value = false }
}

// ── Context menu ───────────────────────────────────────────────────
const contextMenu = ref({ show: false, x: 0, y: 0, row: null })
function showContext(e, row) { contextMenu.value = { show: true, x: e.clientX, y: e.clientY, row } }
function hideContext() { contextMenu.value.show = false }

// ── Delete ─────────────────────────────────────────────────────────
async function handleDelete(row) {
  hideContext()
  try { await deleteCollector(row.id); fetchList(); fetchStats() } catch { /* ignore */ }
}

// ── Detail modal ───────────────────────────────────────────────────
const detailModal = ref(false)
const detailStationId = ref(null)
const detailData = ref(null)
const detailLoading = ref(false)

async function openDetail(station) {
  detailStationId.value = station.id
  detailModal.value = true
  detailLoading.value = true
  try {
    const res = await getCollectorDetail(station.id)
    detailData.value = res.data || null
  } catch { detailData.value = null }
  finally { detailLoading.value = false }
  detailFilterId.value = ''
  detailCurrentPage.value = 1
  fetchBoundMeters()
}
function closeDetail() { detailModal.value = false }

// ── Bound meters ───────────────────────────────────────────────────
const boundMeters = ref([])
const boundTotal = ref(0)
const detailFilterId = ref('')
const detailCurrentPage = ref(1)
const detailPageSize = ref(10)
const boundLoading = ref(false)
const detailTotalPages = computed(() => Math.max(1, Math.ceil(boundTotal.value / detailPageSize.value)))

async function fetchBoundMeters() {
  boundLoading.value = true
  try {
    const res = await getCollectorMeters(detailStationId.value, {
      pageNum: detailCurrentPage.value,
      pageSize: detailPageSize.value,
      keyword: detailFilterId.value || undefined,
    })
    boundMeters.value = res.data?.records || []
    boundTotal.value = res.data?.total || 0
  } catch { boundMeters.value = []; boundTotal.value = 0 }
  finally { boundLoading.value = false }
}

function detailGoToPage(page) {
  if (page >= 1 && page <= detailTotalPages.value) { detailCurrentPage.value = page; fetchBoundMeters() }
}
function detailResetFilter() {
  detailFilterId.value = ''
  detailCurrentPage.value = 1
  fetchBoundMeters()
}

// ── Unbind / batch operations ──────────────────────────────────────
async function handleUnbindAll() {
  try { await unbindAllMetersFromCollector(detailStationId.value); fetchBoundMeters(); fetchStats() } catch { /* ignore */ }
}
async function handleBindOnline() {
  try { await bindAllOnlineMeters(detailStationId.value); fetchBoundMeters(); fetchStats() } catch { /* ignore */ }
}
async function handleUnbindOne(meterId) {
  try { await unbindMeterFromCollector(detailStationId.value, meterId); fetchBoundMeters(); fetchStats() } catch { /* ignore */ }
}

// ── Helpers ────────────────────────────────────────────────────────
function statusClass(s) {
  if (s === '在线' || s === 'online') return 'text-[#52c41a]'
  return 'text-[#ff4d4f]'
}

onMounted(() => { fetchList(); fetchStats() })
</script>

<template>
  <div>
    <!-- Filter Bar -->
    <div class="bg-white rounded p-5 mb-3 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <input
          v-model="filterKeyword"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[260px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="基站名称或基站编号"
          type="text"
          @keyup.enter="currentPage = 1; fetchList()"
        />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="currentPage = 1; fetchList()">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- Stat Chips -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '总数'
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter('总数')"
        >
          总数: <span class="ml-1 font-semibold">{{ totalStations }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
        <div
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '绑定水表数'
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter('绑定水表数')"
        >
          绑定水表数: <span class="ml-1 font-semibold">{{ totalBoundMeters }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
        <div
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '在线'
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter('在线')"
        >
          在线: <span class="ml-1 font-semibold">{{ onlineCount }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
        <div
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '离线'
            ? 'bg-[#3a64ab] border-[#3a64ab] text-white'
            : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'"
          @click="applyStatFilter('离线')"
        >
          离线: <span class="ml-1 font-semibold">{{ offlineCount }}</span>
          <span class="ml-1">只 &gt;</span>
        </div>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] relative min-h-[150px]">
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
        <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
      </div>

      <div class="mb-4 flex justify-between items-center">
        <div class="flex items-center gap-3">
          <h3 class="text-[15px] font-bold text-[#333]">基站明细</h3>
          <button
            v-if="selectedCount > 0"
            class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors disabled:opacity-50"
            :disabled="binding"
            @click="handleBatchBind"
          >{{ binding ? '绑定中...' : `一键绑定 (${selectedCount})` }}</button>
        </div>
        <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]"><input type="checkbox" class="styled-checkbox" @change="toggleSelectAll" /></th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">基站名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">基站编号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">基站型号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">在线状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">绑定水表数</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建时间</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">创建人</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="list.length === 0 && !loading">
              <td colspan="10" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(station, idx) in list"
              :key="station.id"
              class="cursor-pointer transition-colors hover:bg-[#f5f6fb]"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"><input type="checkbox" class="styled-checkbox" :checked="selectedIds.has(station.id)" @change="toggleStation(station.id)" @click.stop /></td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium">{{ station.name || station.collectorName || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#3a64ab] font-mono">{{ station.collectorNo || station.stationNo || station.id }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ station.model || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span :class="statusClass(station.commStatus || station.status)">{{ station.commStatus === 'online' || station.commStatus === '在线' ? '在线' : '离线' }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ station.meterCount ?? station.meters ?? 0 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ station.createTime || station.createdAt || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ station.creator || station.createBy || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d]" @click.stop="openDetail(station)">详情</span>
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] ml-2" @click.stop="showContext($event, station)">更多 &#x25BC;</span>
              </td>
            </tr>
          </tbody>
        </table>
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
        <div class="absolute bg-white border border-[#e8e8e8] rounded-lg shadow-xl py-1.5 min-w-[150px]" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }" @click.stop>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors" @click="openDetail(contextMenu.row); hideContext()">查看详情</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors">诊断信息</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer transition-colors">信号记录</div>
          <div class="px-4 py-2 text-[13px] hover:bg-[#f5f6fb] cursor-pointer text-[#ff4d4f] transition-colors" @click="handleDelete(contextMenu.row)">删除基站</div>
        </div>
      </div>
    </Teleport>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[60] flex items-start justify-center pt-[40px] pb-[40px]" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[960px] max-h-full overflow-y-auto flex flex-col">
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">基站详情 - {{ detailData?.name || detailData?.collectorName || '' }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>

          <div class="p-5 overflow-y-auto flex-1" v-if="detailData && !detailLoading">
            <div class="flex gap-5 mb-5">
              <div class="flex-1 grid grid-cols-2 gap-x-8 gap-y-3">
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">基站名称</span><span class="text-[13px] text-[#333]">{{ detailData.name || detailData.collectorName || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">基站型号</span><span class="text-[13px] text-[#333]">{{ detailData.model || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">基站编号</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.collectorNo || detailData.stationNo || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">通讯状态</span><span class="text-[13px]" :class="statusClass(detailData.commStatus)">{{ detailData.commStatus === 'online' || detailData.commStatus === '在线' ? '在线' : '离线' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">ICCID</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.iccid || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">版本</span><span class="text-[13px] text-[#333]">{{ detailData.version || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">创建时间</span><span class="text-[13px] text-[#333]">{{ detailData.createTime || detailData.createdAt || '--' }}</span></div>
                <div class="flex"><span class="text-[13px] text-[#999] w-[70px] shrink-0">创建人</span><span class="text-[13px] text-[#333]">{{ detailData.creator || detailData.createBy || '--' }}</span></div>
              </div>

              <div class="flex flex-col gap-2.5 shrink-0">
                <button class="py-2 px-5 rounded bg-white border border-[#ff4d4f] text-[#ff4d4f] text-[13px] cursor-pointer hover:bg-[#fff2f0] transition-colors whitespace-nowrap" @click="handleUnbindAll">解绑所有水表</button>
                <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors whitespace-nowrap" @click="handleBindOnline">绑定离线水表</button>
              </div>
            </div>

            <!-- Bound Water Meter List -->
            <div class="border-t border-[#f0f0f0] pt-5">
              <h4 class="text-[14px] font-medium mb-3">已绑定水表</h4>

              <div class="flex flex-wrap gap-3 items-center mb-3">
                <input v-model="detailFilterId" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors" placeholder="请输入表号" type="text" @keyup.enter="detailCurrentPage = 1; fetchBoundMeters()" />
                <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none" @click="detailCurrentPage = 1; fetchBoundMeters()">查询</button>
                <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="detailResetFilter">重置</button>
              </div>

              <div class="overflow-x-auto relative min-h-[80px]">
                <div v-if="boundLoading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
                  <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
                </div>
                <table class="w-full border-collapse text-left">
                  <thead>
                    <tr>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[50px]">序号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">水表状态</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">水表型号</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">余额</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">今日用量</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">付费类型</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">通讯状态</th>
                      <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">阀门状态</th>
                      <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(meter, idx) in boundMeters" :key="meter.id || meter.meterId" class="hover:bg-[#f5f6fb]">
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ (detailCurrentPage - 1) * detailPageSize + idx + 1 }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.groupName || meter.group || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.customerName || meter.merchant || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono text-[#3a64ab]">{{ meter.dbNo || meter.id }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.meterStatus === '正常' || meter.meterStatus === 'normal' ? 'text-[#52c41a]' : meter.meterStatus === '告警' || meter.meterStatus === 'alarm' ? 'text-[#fa8c16]' : 'text-[#ff4d4f]'">{{ meter.meterStatus || '--' }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.model || '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono" :class="(meter.balance || 0) < 0 ? 'text-[#ff4d4f]' : ''">{{ typeof meter.balance === 'number' ? meter.balance.toFixed(2) : (meter.balance || '0.00') }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ meter.todayUsage ?? '--' }}</td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.paymentType === '后付费' || meter.paymentType === 'postpaid' ? 'text-[#fa8c16]' : 'text-[#3a64ab]'">{{ meter.paymentType || '--' }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.commStatus === '在线' || meter.commStatus === 'online' ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ meter.commStatus || '--' }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">
                        <span :class="meter.valveStatus === '开阀' || meter.valveStatus === 'open' ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ meter.valveStatus || '--' }}</span>
                      </td>
                      <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px]">
                        <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d]" @click="handleUnbindOne(meter.meterId || meter.id)">解绑</span>
                      </td>
                    </tr>
                    <tr v-if="boundMeters.length === 0 && !boundLoading">
                      <td :colspan="12" class="py-10 text-center text-[13px] text-[#999]">暂无绑定水表</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="flex justify-between items-center mt-3 pt-2.5 border-t border-[#f0f0f0]">
                <span class="text-[12px] text-[#666]">共 {{ boundTotal }} 条</span>
                <div class="flex items-center gap-2 text-[12px]">
                  <select class="py-1 px-2 border border-[#d9d9d9] rounded text-[12px] outline-none bg-white" v-model.number="detailPageSize" @change="detailCurrentPage = 1; fetchBoundMeters()">
                    <option :value="10">10 条/页</option>
                    <option :value="20">20 条/页</option>
                    <option :value="50">50 条/页</option>
                  </select>
                  <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="detailCurrentPage <= 1" @click="detailGoToPage(detailCurrentPage - 1)">上一页</button>
                  <span class="text-[#666]">{{ detailCurrentPage }} / {{ detailTotalPages || 1 }}</span>
                  <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="detailCurrentPage >= detailTotalPages" @click="detailGoToPage(detailCurrentPage + 1)">下一页</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="detailLoading" class="p-10 text-center text-[13px] text-[#999]">加载中...</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
