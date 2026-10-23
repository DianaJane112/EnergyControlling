<script setup>
import { ref, computed, watch, inject, onMounted } from 'vue'
import { getDailyElectricity, getMonthlyDetail } from '@/api/report'
import { getDeletedMeterMonthly } from '@/api/meterReport'
import { reportColumnsConfig } from '@/config/ReportColumns.js'
import { downloadXLSX } from '@/utils/export'

const props = defineProps({
  reportId: { type: String, required: true },
  searchParams: { type: Object, default: () => ({}) },
  tableResetKey: { type: Number, default: 0 },
})

const loading = ref(false)
const rawData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10

// ── 安全解析 yyyy-MM-dd 字符串为本地日期 ──
function parseLocalDate(str) {
  if (!str) return null
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

// ── 为用电量月报表（含已删除电表月报）动态生成日列 ──
const dayColumns = computed(() => {
  if (props.reportId !== 'elec-monthly' && props.reportId !== 'elec-monthly-deleted') return []

  const p = props.searchParams
  let start, end

  if (p.dateStart) {
    start = parseLocalDate(p.dateStart)
  } else {
    const now = new Date()
    start = new Date(now.getFullYear(), now.getMonth(), 1)
  }

  if (p.dateEnd) {
    end = parseLocalDate(p.dateEnd)
  } else {
    end = new Date()
    end = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  }

  if (!start) return []

  // 修正：确保 end 不早于 start
  if (!end || end < start) end = new Date(start)

  const cols = []
  const cursor = new Date(start)
  while (cursor <= end) {
    const month = String(cursor.getMonth() + 1).padStart(2, '0')
    const day = String(cursor.getDate()).padStart(2, '0')
    const key = `${month}-${day}`
    cols.push({
      key: `day:${key}`,
      label: `${cursor.getMonth() + 1}/${String(cursor.getDate()).padStart(2, '0')}`,
      align: 'right',
    })
    cursor.setDate(cursor.getDate() + 1)
  }
  return cols
})

// ── 获取表头配置（月报 / 已删除电表月报动态拼接日列 + 总用量） ──
const currentData = computed(() => {
  if (props.reportId === 'elec-monthly' || props.reportId === 'elec-monthly-deleted') {
    const staticCols = reportColumnsConfig[props.reportId] || []
    return {
      columns: [
        ...staticCols,
        ...dayColumns.value,
        { key: 'totalUsage', label: '总用量(kWh)', align: 'right' },
      ]
    }
  }
  return { columns: reportColumnsConfig[props.reportId] || [] }
})

// ── 客户端过滤（keyword=商户名/表号, virtualKeyword=虚拟表号/商户名）──
const tableData = computed(() => {
  let list = rawData.value
  const p = props.searchParams

  // 商户名 / 表号 模糊过滤（兼容 customerName / merchantName 两种字段名）
  if (p.keyword && p.keyword.trim()) {
    const kw = p.keyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.customerName && String(r.customerName).toLowerCase().includes(kw)) ||
      (r.merchantName && String(r.merchantName).toLowerCase().includes(kw)) ||
      (r.deviceNo && String(r.deviceNo).toLowerCase().includes(kw))
    )
  }

  // 虚拟表号 / 虚拟商户名 模糊过滤
  if (p.virtualKeyword && p.virtualKeyword.trim()) {
    const vk = p.virtualKeyword.trim().toLowerCase()
    list = list.filter(r =>
      (r.virtualMerchantName && String(r.virtualMerchantName).toLowerCase().includes(vk)) ||
      (r.virtualMeterNo && String(r.virtualMeterNo).toLowerCase().includes(vk))
    )
  }

  total.value = list.length
  const start = (currentPage.value - 1) * pageSize
  return list.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

// ── 构建与后端对齐的请求参数 ──
function buildApiParams() {
  const p = props.searchParams
  return {
    pageNum: 1,
    pageSize: 500,
    startDate: p.dateStart || null,
    endDate: p.dateEnd || null,
    customerId: p.customerId || null,
    merchantId: p.customerId || null,
    groupId: p.groupId || null,
    statMonth: p.dateStart ? p.dateStart.substring(0, 7) : null,
  }
}

// ── 核心：发起真实网络请求 ──
const loadData = async () => {
  loading.value = true
  try {
    const apiParams = buildApiParams()
    let res = null

    if (props.reportId === 'elec-daily') {
      res = await getDailyElectricity(apiParams)
    } else if (props.reportId === 'elec-monthly') {
      res = await getMonthlyDetail(apiParams)
    } else if (props.reportId === 'elec-monthly-deleted') {
      res = await getDeletedMeterMonthly(apiParams)
    }

    if (res && res.data) {
      let arr = []
      if (res.data.records) {
        arr = res.data.records
      } else if (Array.isArray(res.data)) {
        arr = res.data
      }
      // 客户端排序，确保每次查询/重置后数据顺序一致
      rawData.value = arr.sort((a, b) => {
        const aKey = (a.deviceNo || '') + '|' + (a.customerName || a.merchantName || '')
        const bKey = (b.deviceNo || '') + '|' + (b.customerName || b.merchantName || '')
        return aKey.localeCompare(bKey, 'zh-CN')
      })
    } else {
      rawData.value = []
    }
  } catch (error) {
    console.error('获取报表数据失败', error)
    rawData.value = []
  } finally {
    loading.value = false
  }
}

// ── 只监听需要重新请求 API 的条件（日期/商户/分组/报表ID） ──
watch(
  [() => props.searchParams.dateStart, () => props.searchParams.dateEnd,
   () => props.searchParams.customerId, () => props.searchParams.groupId,
   () => props.reportId],
  () => { loadData() },
  { immediate: true }
)

// ── 任何搜索条件变化都重置到第1页 ──
watch(() => ({ ...props.searchParams }), () => {
  currentPage.value = 1
})

// 重置时回到第1页
watch(() => props.tableResetKey, () => {
  currentPage.value = 1
})

// ── 辅助函数 ──
function goPage(n) { currentPage.value = n }

function getCellValue(record, colKey) {
  // 日列通过 record.dailyUsage[MM-dd] 取值
  if (colKey.startsWith('day:')) {
    const day = colKey.substring(4)
    return record.dailyUsage?.[day] ?? null
  }
  return record[colKey]
}

function formatCell(v, col) {
  if (v == null || v === '' || v === '—') return '—'
  if (typeof v === 'number') return v.toFixed(2)
  // 百分比类字段
  if (col.key === 'loadRate' || col.key === 'reverseRatio') {
    const n = parseFloat(v)
    if (!isNaN(n)) return n.toFixed(1) + '%'
    return v
  }
  return v
}

// ── 报表标题映射 ──
const reportTitle = computed(() => {
  const map = {
    'elec-daily': '用电量日报表',
    'elec-monthly': '用电量月报表',
    'elec-monthly-deleted': '用电量月报表（已删除电表）',
  }
  return map[props.reportId] || '报表详情'
})

// ── 导出功能 ─────────────────────────────────────────────────
function handleExport() {
  downloadXLSX(currentData.value.columns, tableData.value, reportTitle.value)
}

const registerExport = inject('registerExport', null)
onMounted(() => {
  if (registerExport) registerExport(handleExport)
})
</script>

<template>
  <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
    <div class="mb-4 flex justify-between items-center">
      <h2 class="text-[15px] font-bold">{{ reportTitle }}</h2>
      <span class="text-[12px] text-[#999]">共 {{ total }} 条记录</span>
    </div>

    <div class="overflow-x-auto relative min-h-[150px]">
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
        <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
      </div>

      <table class="w-full border-collapse text-left" v-if="tableData.length > 0">
        <thead>
          <tr>
            <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
            <th
              v-for="col in currentData.columns"
              :key="col.key"
              class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap"
              :class="[col.width || '', col.align === 'right' ? 'text-right' : '']"
            >{{ col.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(record, idx) in tableData"
            :key="record.meterId || record.period || idx"
          >
            <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] text-[#666]">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
            <td
              v-for="col in currentData.columns"
              :key="col.key"
              class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]"
              :class="[
                col.align === 'right' ? 'text-right font-mono' : '',
                col.key === 'deviceNo' || col.key === 'virtualMeterNo' ? 'font-mono' : '',
              ]"
            >{{ formatCell(getCellValue(record, col.key), col) }}</td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">
        暂无匹配数据
      </div>
    </div>

    <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
      <span class="text-[13px] text-[#666]">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <div class="flex items-center gap-2">
        <button
          class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="currentPage <= 1"
          @click="goPage(1)"
        >首页</button>
        <button
          class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="currentPage <= 1"
          @click="goPage(currentPage - 1)"
        >上一页</button>
        <template v-for="p in totalPages" :key="p">
          <button
            v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)"
            class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors"
            :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'"
            @click="goPage(p)"
          >{{ p }}</button>
          <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
        </template>
        <button
          class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="currentPage >= totalPages"
          @click="goPage(currentPage + 1)"
        >下一页</button>
        <button
          class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          :disabled="currentPage >= totalPages"
          @click="goPage(totalPages)"
        >末页</button>
      </div>
    </div>
  </div>
</template>
