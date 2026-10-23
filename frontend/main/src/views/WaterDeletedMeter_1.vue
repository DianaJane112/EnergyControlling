<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDeletedWaterList, getDeletedWaterDetail, restoreDeletedWater, getWaterOperationLogs } from '@/api/meters.js'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'

const groupOptions = [
  { value: '计量', label: '计量' },
  { value: '计费', label: '计费' },
  { value: '电子科大科技园天府园', label: '电子科大科技园天府园' },
]

const filterKeyword = ref('')
const filterGroup = ref('')
const activeStat = ref('')

function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
  currentPage.value = 1
  fetchData()
}

function resetFilters() {
  filterKeyword.value = ''
  filterGroup.value = ''
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
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function getVendorType(r) {
  const jx = r.jiexian || {}
  const isJiexian = jx.companycode || jx.meterCaliber ||
    jx.controlling != null || jx.stateValve != null ||
    jx.signalStrength != null || jx.batteryVoltage != null
  if (isJiexian) return '捷先'
  if (r.jicheng?.stealNo || r.jicheng?.vender) return '积成'
  return '--'
}

function getValveLabel(r) {
  if (r.jiexian?.stateValve !== undefined && r.jiexian?.stateValve !== '') {
    return r.jiexian.stateValve === 1 ? '关阀' : '开阀'
  }
  if (r.jicheng?.switchOp !== undefined) {
    return r.jicheng.switchOp === '2' ? '关阀' : '开阀'
  }
  return '--'
}

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    if (filterKeyword.value.trim()) params.keyword = filterKeyword.value.trim()
    if (filterGroup.value) params.groupId = Number(filterGroup.value)
    if (activeStat.value === '在线') params.online = 1
    if (activeStat.value === '离线') params.online = 0
    const res = await getDeletedWaterList(params)
    const data = res.data
    tableData.value = (data.records || []).map(r => {
      const jc = r.jicheng || {}
      const jx = r.jiexian || {}
      const g = r.general || {}
      return {
        id: r.id,
        vendorType: getVendorType(r),
        group: g.groupId || '--',
        customerName: r.customerName || '--',
        meterNo: jc.stealNo || jx.meteraddress || r.id,
        caliber: jc.caliber || jc.meterTypeName || jx.meterCaliber || '--',
        valveStatus: getValveLabel(r),
        online: g.online,
        commStatusDesc: g.commStatusDesc || (g.online === 1 ? '在线' : '离线'),
        status: g.status,
        signal: jc.terminalName || jx.signalStrength || '--',
        battery: jx.batteryVoltage || '--',
        deletedAt: r.updateTime || '--',
        raw: r,
      }
    })
    total.value = data.total || 0
  } catch (e) { console.error('获取已删除水表列表失败', e) } finally { loading.value = false }
}

function goToPage(p) { if (p >= 1 && p <= totalPages.value) { currentPage.value = p; fetchData() } }

// ── Row selection ────────────────────────────────────────────────────
const selectedRowId = ref(null)
function selectRow(id) { selectedRowId.value = id }

// ── Stats ────────────────────────────────────────────────────────────
const totalCount = computed(() => total.value)
const onlineCount = ref(0)
const offlineCount = ref(0)

// ── Detail modal ─────────────────────────────────────────────────────
const detailModal = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
async function openDetail(meter) {
  detailModal.value = true; detailLoading.value = true; detailData.value = null
  try { const res = await getDeletedWaterDetail(meter.id); detailData.value = res.data }
  catch (e) { console.error('获取详情失败', e) } finally { detailLoading.value = false }
}
function closeDetail() { detailModal.value = false }

// ── Restore ──────────────────────────────────────────────────────────
const restoring = ref(false)
async function restoreMeter(row) {
  restoring.value = true
  try { await restoreDeletedWater(row.id); fetchData() } catch (e) { console.error('恢复失败', e) }
  finally { restoring.value = false }
}

// ── Operation logs ───────────────────────────────────────────────────
const opRecordModal = ref(false)
const opRecordMeterName = ref('')
const opRecords = ref([])
const opLoading = ref(false)
const opTabs = ['全部', 'QUERY', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT']
const opActiveTab = ref('全部')
const opCurrentPage = ref(1)
const opPageSize = 10

const filteredOpRecords = computed(() => {
  if (opActiveTab.value === '全部') return opRecords.value
  return opRecords.value.filter(r => r.operationType === opActiveTab.value)
})
const opTotalPages = computed(() => Math.max(1, Math.ceil(filteredOpRecords.value.length / opPageSize)))
const paginatedOpRecords = computed(() => {
  const start = (opCurrentPage.value - 1) * opPageSize
  return filteredOpRecords.value.slice(start, start + opPageSize)
})

async function openOpRecords(row) {
  opRecordMeterName.value = row.customerName || row.meterNo
  opActiveTab.value = '全部'; opCurrentPage.value = 1; opLoading.value = true
  opRecordModal.value = true
  try { const res = await getWaterOperationLogs(row.id, { pageNum: 1, pageSize: 100 }); opRecords.value = res.data?.records || [] }
  catch (e) { console.error('操作日志获取失败', e) } finally { opLoading.value = false }
}
function closeOpRecords() { opRecordModal.value = false }
function switchOpTab(tab) { opActiveTab.value = tab; opCurrentPage.value = 1 }
function opGoToPage(p) { if (p >= 1 && p <= opTotalPages.value) opCurrentPage.value = p }

onMounted(() => fetchData())
</script>

<template>
  <div @click="selectedRowId = null">
    <!-- Box 1: Filter Bar -->
    <div class="bg-white rounded p-5 mb-3 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <input
          v-model="filterKeyword"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="商户名或表号"
          type="text"
        />
        <DropdownSelect v-model="filterGroup" :options="groupOptions" placeholder="分组" />
        <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="currentPage = 1; fetchData()">查询</button>
        <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- Box 2: Stat Chips -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-3">
        <span
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat
            ? 'border-[#e2e8f0] text-[#bbb] bg-[#f5f6fb]'
            : 'border-[#e2e8f0] text-[#666] bg-[#f5f6fb] hover:border-[#3a64ab] hover:text-[#3a64ab]'"
          @click="activeStat = ''"
        >总数: <b>{{ totalCount }}</b> 条 &gt;</span>
        <span
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '在线' ? 'border-[#3a64ab] text-[#3a64ab] bg-[#eef3fa] font-medium' : activeStat ? 'border-[#e2e8f0] text-[#bbb] bg-[#f5f6fb]' : 'border-[#e2e8f0] text-[#666] bg-[#f5f6fb] hover:border-[#3a64ab] hover:text-[#3a64ab]'"
          @click="applyStatFilter('在线')"
        >在线: <b>{{ onlineCount }}</b> 只 &gt;</span>
        <span
          class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors"
          :class="activeStat === '离线' ? 'border-[#3a64ab] text-[#3a64ab] bg-[#eef3fa] font-medium' : activeStat ? 'border-[#e2e8f0] text-[#bbb] bg-[#f5f6fb]' : 'border-[#e2e8f0] text-[#666] bg-[#f5f6fb] hover:border-[#3a64ab] hover:text-[#3a64ab]'"
          @click="applyStatFilter('离线')"
        >离线: <b>{{ offlineCount }}</b> 只 &gt;</span>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="mb-4 flex justify-between items-center">
        <h3 class="text-[15px] font-bold text-[#333]">删除水表明细</h3>
        <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
      </div>

      <div class="overflow-x-auto max-h-[520px] overflow-y-auto">
        <table class="w-max min-w-full border-collapse text-left">
          <thead class="sticky top-0 z-10">
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">厂家类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">分组</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">商户名</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">口径 / 型号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">阀门状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">通讯状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">运行状态</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">信号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">电池电压</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">删除时间</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableData.length === 0">
              <td :colspan="13" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
            </tr>
            <tr
              v-for="(meter, idx) in tableData"
              :key="meter.id"
              class="cursor-pointer transition-colors"
              :class="selectedRowId === meter.id ? 'bg-[#eef3fa]' : 'hover:bg-[#f5f6fb]'"
              @click.stop="selectRow(meter.id)"
            >
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="meter.vendorType === '积成' ? 'status-blue' : 'status-orange'">{{ meter.vendorType }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.group }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.customerName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono text-[#3a64ab]">{{ meter.meterNo }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meter.caliber }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="meter.valveStatus === '开阀' ? 'status-green' : 'status-red'">{{ meter.valveStatus }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="meter.commStatusDesc === '在线' ? 'status-green' : 'status-gray'">{{ meter.commStatusDesc }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="meter.status === 1 ? 'status-green' : 'status-red'">{{ meter.status === 1 ? '正常' : '停用' }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ meter.signal }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ meter.battery }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono whitespace-nowrap">{{ meter.deletedAt }}</td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px]">
                <span class="text-[#3a64ab] cursor-pointer mr-2 hover:text-[#264c8d] whitespace-nowrap" @click.stop="openDetail(meter)">详情</span>
                <span class="text-[#52c41a] cursor-pointer mr-2 hover:text-[#73d13d] whitespace-nowrap" @click.stop="restoreMeter(meter)">恢复</span>
                <span class="text-[#3a64ab] cursor-pointer hover:text-[#264c8d] whitespace-nowrap" @click.stop="openOpRecords(meter)">操作记录</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <div class="flex items-center gap-3 text-[13px] text-[#666]">
          <span>共 <b class="text-[#3a64ab] mx-1">{{ total }}</b> 条</span>
          <select v-model.number="pageSize" class="py-1 px-2 border border-[#d9d9d9] rounded text-[13px] outline-none bg-white" @change="currentPage = 1; fetchData()">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
        </div>
        <div class="flex items-center gap-2 text-[13px]">
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
          <span class="text-[#666]">当前页 <b class="text-[#3a64ab]">{{ currentPage }}</b></span>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
          <span class="text-[#666] ml-2">前往</span>
          <input
            class="w-[40px] py-1 px-1 border border-[#d9d9d9] rounded text-[13px] outline-none text-center"
            type="text"
            @keyup.enter="goToPage(Number(($event.target).value))"
          />
          <span class="text-[#666]">页</span>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[60] flex items-start justify-center pt-[40px] pb-[40px]" @click.self="closeDetail">
        <div class="bg-white rounded-lg shadow-2xl w-[860px] max-h-full overflow-y-auto flex flex-col">
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">水表详情</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeDetail">&times;</button>
          </div>

          <div v-if="detailLoading" class="p-10 text-center text-[13px] text-[#999]">加载中...</div>
          <div v-else-if="detailData" class="p-5 overflow-y-auto flex-1">
            <h4 class="text-[14px] font-medium mb-3 text-[#333]">基本信息</h4>
            <div class="grid grid-cols-2 gap-x-8 gap-y-3 mb-6 bg-[#f5f6fb] rounded p-4">
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">商户名称</span><span class="text-[13px] text-[#333]">{{ detailData.customerName || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">表号</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.stealNo || detailData.jiexian?.meteraddress || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">所属分组</span><span class="text-[13px] text-[#333]">{{ detailData.general?.groupId || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">创建时间</span><span class="text-[13px] text-[#333]">{{ detailData.createTime || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">表计类型</span><span class="text-[13px] text-[#333]">水表</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">删除时间</span><span class="text-[13px] text-[#333]">{{ detailData.updateTime || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">口径</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.caliber || detailData.jiexian?.meterCaliber || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">型号</span><span class="text-[13px] text-[#333] font-mono">{{ detailData.jicheng?.meterTypeName || detailData.jiexian?.meterCaliber || '--' }}</span></div>
            </div>

            <h4 class="text-[14px] font-medium mb-3 text-[#333]">状态信息</h4>
            <div class="grid grid-cols-2 gap-x-8 gap-y-3 bg-[#f5f6fb] rounded p-4">
              <div class="flex">
                <span class="text-[13px] text-[#999] w-[90px] shrink-0">阀门状态</span>
                <span class="text-[13px] text-[#333]" :class="detailData.jiexian?.stateValve !== undefined ? (detailData.jiexian.stateValve === 1 ? 'text-[#ff4d4f]' : 'text-[#52c41a]') : detailData.jicheng?.switchOp === '2' ? 'text-[#ff4d4f]' : 'text-[#52c41a]'">
                  {{ detailData.jiexian?.stateValve !== undefined ? (detailData.jiexian.stateValve === 1 ? '关阀' : '开阀') : detailData.jicheng?.switchOp === '2' ? '关阀' : detailData.jicheng?.switchOp ? '开阀' : '--' }}
                </span>
              </div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">通讯状态</span><span class="text-[13px] text-[#333]" :class="detailData.general?.online === 1 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ detailData.general?.online === 1 ? '在线' : '离线' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">运行状态</span><span class="text-[13px] text-[#333]" :class="detailData.general?.status === 1 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ detailData.general?.status === 1 ? '正常' : '停用' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">信号</span><span class="text-[13px] text-[#333]">{{ detailData.jicheng?.terminalName || detailData.jiexian?.signalStrength || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">电池电压</span><span class="text-[13px] text-[#333]">{{ detailData.jiexian?.batteryVoltage || '--' }}</span></div>
              <div class="flex"><span class="text-[13px] text-[#999] w-[90px] shrink-0">通讯描述</span><span class="text-[13px] text-[#333]">{{ detailData.general?.commStatusDesc || '--' }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Operation Log Modal -->
    <Teleport to="body">
      <div v-if="opRecordModal" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="closeOpRecords">
        <div class="bg-white rounded-lg shadow-2xl w-[860px] max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">操作记录 - {{ opRecordMeterName }}</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeOpRecords">&times;</button>
          </div>

          <div class="px-5 pt-4 flex gap-2 flex-wrap shrink-0">
            <span
              v-for="tab in opTabs" :key="tab"
              class="py-1.5 px-3 rounded text-[13px] cursor-pointer transition-colors border"
              :class="opActiveTab === tab ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
              @click="switchOpTab(tab)"
            >{{ tab }}</span>
          </div>

          <div class="p-5 overflow-y-auto flex-1">
            <div class="overflow-x-auto">
              <table class="w-full border-collapse text-left">
                <thead>
                  <tr>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作人</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">模块</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作类型</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作描述</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">请求参数</th>
                    <th class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">IP地址</th>
                    <th class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(rec, idx) in paginatedOpRecords" :key="idx" class="hover:bg-[#f5f6fb]">
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.operatorName || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.moduleName || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.operationType || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px]">{{ rec.operationDesc || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] max-w-[200px] truncate">{{ rec.requestParams || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-r border-[#e8e8e8] text-[12px] font-mono">{{ rec.ipAddress || '--' }}</td>
                    <td class="py-2.5 px-2 border-b border-[#e8e8e8] text-[12px] font-mono whitespace-nowrap">{{ rec.operationTime || '--' }}</td>
                  </tr>
                  <tr v-if="paginatedOpRecords.length === 0">
                    <td :colspan="7" class="py-10 text-center text-[13px] text-[#999]">暂无操作记录</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="flex justify-between items-center p-4 border-t border-[#f0f0f0] shrink-0">
            <span class="text-[12px] text-[#666]">共 {{ filteredOpRecords.length }} 条</span>
            <div class="flex items-center gap-2 text-[12px]">
              <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="opCurrentPage <= 1" @click="opGoToPage(opCurrentPage - 1)">上一页</button>
              <span class="text-[#666]">{{ opCurrentPage }} / {{ opTotalPages || 1 }}</span>
              <button class="py-1 px-3 rounded border border-[#d9d9d9] bg-white text-[#333] cursor-pointer hover:border-[#3a64ab] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="opCurrentPage >= opTotalPages" @click="opGoToPage(opCurrentPage + 1)">下一页</button>
            </div>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="closeOpRecords">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
