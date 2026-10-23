<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getWaterBillingDaily } from '@/api/waterReport.js'
import * as XLSX from 'xlsx'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// ═══════════════════════════════════════════
// 筛选条件
// ═══════════════════════════════════════════
const filterStartDate = ref('')
const filterEndDate = ref('')
const filterMerchantName = ref('')
const filterDeviceNo = ref('')

// ═══════════════════════════════════════════
// 数据状态
// ═══════════════════════════════════════════
const loading = ref(false)
const dailyRawList = ref([])
const chartMode = ref('bar')
const metricMode = ref('usage')
const showHelp = ref(false)

// ═══════════════════════════════════════════
// 日期工具函数
// ═══════════════════════════════════════════
function parseLocalDate(str) {
  if (!str) return null
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function formatDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ═══════════════════════════════════════════
// 动态日列：从日期范围生成所有天
// ═══════════════════════════════════════════
const dayColumns = computed(() => {
  let start, end

  if (filterStartDate.value) {
    start = parseLocalDate(filterStartDate.value)
  }
  if (filterEndDate.value) {
    end = parseLocalDate(filterEndDate.value)
  }

  const now = new Date()
  if (!start) {
    start = new Date(now.getFullYear(), now.getMonth(), 1)
  }
  if (!end) {
    end = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  }
  if (!start) return []
  if (!end || end < start) end = new Date(start)

  const cols = []
  const cursor = new Date(start)
  while (cursor <= end) {
    const y = cursor.getFullYear()
    const m = String(cursor.getMonth() + 1).padStart(2, '0')
    const d = String(cursor.getDate()).padStart(2, '0')
    const key = `${m}-${d}`
    cols.push({ key, label: `${y}-${m}-${d}` })
    cursor.setDate(cursor.getDate() + 1)
  }
  return cols
})

// ═══════════════════════════════════════════
// 将日数据按月份分组，构建月度行
// ═══════════════════════════════════════════
const monthlyTableData = computed(() => {
  const list = dailyRawList.value
  if (!list || list.length === 0) return []

  // 先做客户端关键词筛选
  let filtered = list
  if (filterMerchantName.value && filterMerchantName.value.trim()) {
    const kw = filterMerchantName.value.trim().toLowerCase()
    filtered = filtered.filter(r =>
      (r.merchantName && String(r.merchantName).toLowerCase().includes(kw))
    )
  }
  if (filterDeviceNo.value && filterDeviceNo.value.trim()) {
    const dn = filterDeviceNo.value.trim().toLowerCase()
    filtered = filtered.filter(r =>
      (r.deviceNo && String(r.deviceNo).toLowerCase().includes(dn))
    )
  }

  // 按 statDate 的前7位（YYYY-MM）分组
  const monthMap = new Map()
  filtered.forEach(row => {
    const sd = row.statDate || row.statMonth || ''
    if (!sd) return
    const monthKey = sd.substring(0, 7) // "YYYY-MM"
    if (!monthMap.has(monthKey)) {
      monthMap.set(monthKey, { monthKey, dailyMap: new Map() })
    }
    const entry = monthMap.get(monthKey)
    const dayKey = sd.length >= 10 ? sd.substring(8, 10) : sd.substring(5, 10)
    const fullDayKey = sd.length >= 10 ? sd.substring(5, 10) : dayKey // "MM-dd"
    entry.dailyMap.set(fullDayKey, row)
  })

  // 转换为数组并排序
  const months = Array.from(monthMap.values())
  months.sort((a, b) => a.monthKey.localeCompare(b.monthKey))

  // 为每个月份行计算汇总
  return months.map((m, idx) => {
    let totalUsage = 0, totalFee = 0, meterCount = 0, dailyCount = 0
    m.dailyMap.forEach(row => {
      totalUsage += Number(row.totalUsage || 0)
      totalFee += Number(row.totalFee || 0)
      if (row.meterCount) meterCount = Math.max(meterCount, Number(row.meterCount))
      dailyCount++
    })
    const avgPrice = totalUsage > 0 ? totalFee / totalUsage : 0
    return {
      _rowId: idx,
      monthKey: m.monthKey,
      dailyMap: m.dailyMap,
      totalUsage,
      totalFee,
      avgPrice,
      meterCount,
      dailyCount,
    }
  })
})

const totalRows = computed(() => monthlyTableData.value.length)

// 辅助获取某月某日的日数据
function getDayData(monthRow, dayColKey) {
  // dayColKey 是 "MM-dd" 格式
  return monthRow.dailyMap.get(dayColKey) || null
}

// ═══════════════════════════════════════════
// 汇总指标卡片
// ═══════════════════════════════════════════
const summary = computed(() => {
  const rows = monthlyTableData.value
  if (rows.length === 0) {
    return { totalUsage: 0, totalFee: 0, avgPrice: 0, monthCount: 0, meterCount: 0, dailyTotal: 0 }
  }
  let tu = 0, tf = 0, mc = 0, dt = 0
  rows.forEach(r => {
    tu += r.totalUsage
    tf += r.totalFee
    mc = Math.max(mc, r.meterCount)
    dt += r.dailyCount
  })
  return {
    totalUsage: tu,
    totalFee: tf,
    avgPrice: tu > 0 ? tf / tu : 0,
    monthCount: rows.length,
    meterCount: mc,
    dailyTotal: dt,
  }
})

// ═══════════════════════════════════════════
// ECharts 图表（日趋势：所选日期区间每天的数据）
// ═══════════════════════════════════════════
const chartOption = computed(() => {
  const days = dayColumns.value
  if (!days || days.length === 0) return {}

  // 按 statDate 聚合：同一天多表数据求和
  const dayMap = new Map()
  const list = dailyRawList.value
  if (list && list.length > 0) {
    list.forEach(row => {
      const sd = row.statDate || ''
      if (!sd) return
      const use = Number(row.totalUsage || 0)
      const fee = Number(row.totalFee || 0)
      if (dayMap.has(sd)) {
        const cur = dayMap.get(sd)
        cur.usage += use
        cur.fee += fee
      } else {
        dayMap.set(sd, { usage: use, fee })
      }
    })
  }

  // 以 dateColumns 为 x 轴，无数据天补 0
  const dayLabels = days.map(d => d.key) // "MM-DD"
  const isUsage = metricMode.value === 'usage'
  const values = days.map(d => {
    const fullKey = d.label // "YYYY-MM-DD"
    const v = dayMap.get(fullKey)
    if (!v) return 0
    return Number((isUsage ? v.usage : v.fee).toFixed(2))
  })
  const unit = isUsage ? 'm³' : '元'
  const title = isUsage ? '日计费水量趋势' : '日计费水费趋势'
  const nonZero = values.filter(v => v > 0)
  const avg = nonZero.length > 0 ? nonZero.reduce((a, b) => a + b, 0) / nonZero.length : 0

  const baseColor = isUsage ? '#3a64ab' : '#52c41a'
  const rotate = dayLabels.length > 20 ? 45 : 0

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e8e8e8',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: 13 },
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      formatter: (params) => {
        const p = Array.isArray(params) ? params[0] : params
        if (!p) return ''
        const val = Number(p.value).toFixed(2)
        return `<div style="font-weight:600;margin-bottom:4px">${p.axisValue}</div>
                <div style="color:${baseColor}">● ${title}：<b>${val}</b> ${unit}</div>
                <div style="color:#999;font-size:11px">日均参考：${avg.toFixed(2)} ${unit}</div>`
      },
    },
    grid: { left: '3%', right: '4%', top: 40, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category', data: dayLabels,
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisTick: { show: false },
      axisLabel: { color: '#666', fontSize: 11, rotate },
    },
    yAxis: {
      type: 'value', name: unit,
      nameTextStyle: { color: '#999', fontSize: 11 },
      axisLabel: { color: '#666', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [{
      type: chartMode.value,
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      barWidth: '50%',
      lineStyle: { color: baseColor, width: 2 },
      itemStyle: chartMode.value === 'bar' ? {
        borderRadius: [4, 4, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: isUsage
            ? [{ offset: 0, color: '#5b8def' }, { offset: 1, color: '#3a64ab' }]
            : [{ offset: 0, color: '#73d13d' }, { offset: 1, color: '#389e0d' }],
        },
      } : { color: baseColor, borderColor: '#fff', borderWidth: 1 },
      areaStyle: chartMode.value === 'line' ? {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: isUsage ? 'rgba(58,100,171,0.15)' : 'rgba(82,196,26,0.12)' },
            { offset: 1, color: 'rgba(255,255,255,0)' },
          ],
        },
      } : undefined,
    }],
  }
})

// ═══════════════════════════════════════════
// 数据加载
// ═══════════════════════════════════════════
async function loadData() {
  loading.value = true
  try {
    const params = {
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined,
      merchantName: filterMerchantName.value || undefined,
      deviceNo: filterDeviceNo.value || undefined,
    }
    const dailyRes = await getWaterBillingDaily(params)
    dailyRawList.value = Array.isArray(dailyRes?.data) ? dailyRes.data : []
  } catch (e) {
    console.error('加载计费水量月报失败', e)
    dailyRawList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function resetFilters() {
  filterStartDate.value = ''
  filterEndDate.value = ''
  filterMerchantName.value = ''
  filterDeviceNo.value = ''
  handleSearch()
}

// ═══════════════════════════════════════════
// 分页
// ═══════════════════════════════════════════
const currentPage = ref(1)
const pageSize = 10
const totalPages = computed(() => Math.max(1, Math.ceil(totalRows.value / pageSize)))
const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return monthlyTableData.value.slice(start, start + pageSize)
})

function goPage(n) { if (n >= 1 && n <= totalPages.value) currentPage.value = n }

// ═══════════════════════════════════════════
// 格式化
// ═══════════════════════════════════════════
function fmt(v) {
  if (v == null || v === '' || v === '—') return '—'
  const n = Number(v)
  return isNaN(n) ? v : n.toFixed(2)
}

// ═══════════════════════════════════════════
// 自定义水平滚动条
// ═══════════════════════════════════════════
const tableWrapper = ref(null)
const scrollTrack = ref(null)
const thumbLeft = ref(0)
const thumbWidth = ref(80)
const isDragging = ref(false)
let dragStartX = 0
let dragStartLeft = 0

function updateThumb() {
  const wrapper = tableWrapper.value
  if (!wrapper) return
  const ratio = wrapper.clientWidth / wrapper.scrollWidth
  thumbWidth.value = Math.max(ratio * 100, 8)
  const maxLeft = 100 - thumbWidth.value
  const scrollRatio = wrapper.scrollWidth > wrapper.clientWidth
    ? wrapper.scrollLeft / (wrapper.scrollWidth - wrapper.clientWidth)
    : 0
  thumbLeft.value = scrollRatio * maxLeft
}

function onThumbMouseDown(e) {
  isDragging.value = true
  dragStartX = e.clientX
  dragStartLeft = thumbLeft.value
  e.preventDefault()
}

function onMouseMove(e) {
  if (!isDragging.value || !scrollTrack.value || !tableWrapper.value) return
  const track = scrollTrack.value
  const trackRect = track.getBoundingClientRect()
  const trackWidth = trackRect.width
  const thumbW = (thumbWidth.value / 100) * trackWidth
  const maxX = trackWidth - thumbW
  const dx = e.clientX - dragStartX
  const newLeft = Math.max(0, Math.min(maxX, dragStartLeft * trackWidth / 100 + dx))
  const percent = maxX > 0 ? newLeft / maxX : 0
  tableWrapper.value.scrollLeft = percent * (tableWrapper.value.scrollWidth - tableWrapper.value.clientWidth)
}

function onMouseUp() { isDragging.value = false }
function onTableScroll() { updateThumb() }

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  // 默认查询当月
  const now = new Date()
  filterStartDate.value = formatDateStr(new Date(now.getFullYear(), now.getMonth(), 1))
  filterEndDate.value = formatDateStr(new Date(now.getFullYear(), now.getMonth(), now.getDate()))
  loadData().then(() => { setTimeout(updateThumb, 100) })
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})

watch(monthlyTableData, () => { setTimeout(updateThumb, 50) })

// ═══════════════════════════════════════════
// Toast
// ═══════════════════════════════════════════
const toastMessage = ref('')
const toastVisible = ref(false)
let toastTimer = null
function showToast(msg) {
  toastMessage.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2500)
}

// ═══════════════════════════════════════════
// 导出 Excel
// ═══════════════════════════════════════════
function handleExport() {
  const data = monthlyTableData.value
  if (data.length === 0) {
    showToast('当前没有可导出的数据')
    return
  }

  const rows = data.map((r, i) => {
    const row = {
      '序号': i + 1,
      '统计月份': r.monthKey,
      '月用水量(m³)': Number(r.totalUsage).toFixed(2),
      '月水费(元)': Number(r.totalFee).toFixed(2),
      '月均价(元/m³)': r.avgPrice.toFixed(2),
      '活跃水表数': r.meterCount,
      '有效天数': r.dailyCount,
    }
    // 附加每日详情
    dayColumns.value.forEach(dc => {
      const dayData = getDayData(r, dc.key)
      row[`${dc.key} 用水量(m³)`] = dayData ? Number(dayData.totalUsage || 0).toFixed(2) : '—'
      row[`${dc.key} 水费(元)`] = dayData ? Number(dayData.totalFee || 0).toFixed(2) : '—'
    })
    return row
  })

  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '计费水量月报表')
  const d = new Date()
  const filename = `计费水量月报表_${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}.xlsx`
  XLSX.writeFile(wb, filename)
}

// ═══════════════════════════════════════════
// colspan 计算：序号 + 统计月份 + 日列×2 + 汇总(2=水费+均价)
// ═══════════════════════════════════════════
const totalColspan = computed(() => 2 + dayColumns.value.length * 2 + 2)
</script>

<template>
  <div class="p-5">
    <!-- ═══════════════════════════════════════════ -->
    <!-- 筛选栏 -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4 items-center">
        <DateRangePicker
          v-model:start-value="filterStartDate"
          v-model:end-value="filterEndDate"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />

        <input
          v-model="filterMerchantName"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors"
          type="text"
          placeholder="商户名称"
        />
        <input
          v-model="filterDeviceNo"
          class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[160px] outline-none focus:border-[#3a64ab] transition-colors"
          type="text"
          placeholder="设备编号"
        />

        <button
          class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors"
          @click="handleSearch"
        >查询</button>
        <button
          class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          @click="resetFilters"
        >重置</button>

        <div class="flex-1"></div>

        <button
          class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors"
          @click="handleExport"
        >导出</button>
        <button
          class="py-2 px-3 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors"
          @click="showHelp = true"
        >说明</button>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- Loading overlay -->
    <!-- ═══════════════════════════════════════════ -->
    <div v-if="loading" class="fixed inset-0 z-50 bg-white/60 flex items-center justify-center">
      <span class="text-[#3a64ab] text-[14px] font-bold animate-pulse">数据加载中...</span>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- ECharts 日趋势图 -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="bg-white rounded-lg border border-[#e8e8e8] shadow-[0_1px_3px_rgba(0,0,0,0.05)] mb-4">
      <div class="flex justify-between items-center p-5 pb-2">
        <h3 class="text-[15px] font-bold text-[#333]">
          {{ metricMode === 'usage' ? '月计费水量趋势' : '月计费水费趋势' }}
        </h3>
        <div class="flex items-center gap-2">
          <div class="flex border border-[#d9d9d9] rounded overflow-hidden">
            <button
              class="py-1 px-3 text-[12px] cursor-pointer border-none transition-colors"
              :class="metricMode === 'usage' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666]'"
              @click="metricMode = 'usage'"
            >用量</button>
            <button
              class="py-1 px-3 text-[12px] cursor-pointer border-none transition-colors"
              :class="metricMode === 'fee' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666]'"
              @click="metricMode = 'fee'"
            >费用</button>
          </div>
          <div class="flex border border-[#d9d9d9] rounded overflow-hidden">
            <button
              class="py-1 px-3 text-[12px] cursor-pointer border-none transition-colors"
              :class="chartMode === 'bar' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666]'"
              @click="chartMode = 'bar'"
            >柱状</button>
            <button
              class="py-1 px-3 text-[12px] cursor-pointer border-none transition-colors"
              :class="chartMode === 'line' ? 'bg-[#3a64ab] text-white' : 'bg-white text-[#666]'"
              @click="chartMode = 'line'"
            >折线</button>
          </div>
        </div>
      </div>
      <div class="px-5 pb-5">
        <div v-if="dayColumns.length > 0" class="w-full" style="height:260px">
          <VChart :option="chartOption" autoresize class="w-full h-full" />
        </div>
        <div v-else class="flex items-center justify-center h-[260px] text-[14px] text-[#999]">
          暂无图表数据
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- 汇总指标卡片 -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="bg-white rounded p-4 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">月计费水量合计</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.totalUsage.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">m³</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">月计费金额合计</div>
          <div class="text-lg font-bold text-[#52c41a]">{{ summary.totalFee.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">平均单价</div>
          <div class="text-lg font-bold text-[#faad14]">{{ summary.avgPrice.toFixed(2) }} <span class="text-[12px] font-normal text-[#999]">元/m³</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">统计月数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.monthCount }} <span class="text-[12px] font-normal text-[#999]">月</span></div>
        </div>
        <div class="flex-1 min-w-[120px] p-3 bg-[#f5f6fb] rounded text-center">
          <div class="text-[12px] text-[#666] mb-1">活跃水表数</div>
          <div class="text-lg font-bold text-[#3a64ab]">{{ summary.meterCount }} <span class="text-[12px] font-normal text-[#999]">台</span></div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- 计费水量月报表格 -->
    <!-- ═══════════════════════════════════════════ -->
    <div class="bg-white rounded-lg border border-[#e8e8e8] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="p-5 pb-0 flex justify-between items-center">
        <h3 class="text-[15px] font-bold text-[#333]">计费水量月报</h3>
        <span class="text-[12px] text-[#999]">共 {{ totalRows }} 条记录</span>
      </div>

      <div class="p-5 pt-3">
        <div class="overflow-x-auto">
          <table class="w-full border-collapse text-left">
            <thead class="sticky top-0 z-10">
              <tr>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center" style="min-width:50px">序号</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center">统计月份</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-right">月用水量 (m³)</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-right">月水费 (元)</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-right">均价 (元/m³)</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center">活跃水表数</th>
                <th class="py-3 px-3 border-b border-[#e8e8e8] text-[12px] font-semibold bg-[#eef3fa] whitespace-nowrap text-center">有效天数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedRecords.length === 0 && !loading">
                <td colspan="7" class="py-16 text-center text-[14px] text-[#999]">暂无数据</td>
              </tr>
              <tr
                v-for="(row, idx) in paginatedRecords"
                :key="row._rowId"
                class="hover:bg-[#f5f6fb] transition-colors"
              >
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-[#666] text-center">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] whitespace-nowrap font-medium">{{ row.monthKey }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono">{{ row.totalUsage.toFixed(2) }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono font-bold text-[#52c41a]">{{ row.totalFee.toFixed(2) }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-right font-mono text-[#faad14]">{{ row.avgPrice.toFixed(2) }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-center">{{ row.meterCount }}</td>
                <td class="py-3 px-3 border-b border-[#e8e8e8] text-[13px] text-center">{{ row.dailyCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalRows > 0" class="flex justify-between items-center px-5 pb-5">
        <div class="flex items-center gap-2 text-[13px] text-[#666]">
          <span>每页 <b class="text-[#3a64ab]">{{ pageSize }}</b> 条</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[13px] text-[#666] mr-2">{{ currentPage }} / {{ totalPages || 1 }}</span>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <template v-for="p in totalPages" :key="p">
            <button v-if="totalPages <= 7 || p === 1 || p === totalPages || (p >= currentPage - 1 && p <= currentPage + 1)" class="w-8 h-8 flex items-center justify-center rounded text-[13px] border cursor-pointer transition-colors" :class="currentPage === p ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="goPage(p)">{{ p }}</button>
            <span v-else-if="p === currentPage - 2 || p === currentPage + 2" class="px-0.5 text-[#999] text-[13px]">...</span>
          </template>
          <button class="py-1.5 px-3 rounded border border-[#d9d9d9] bg-white text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors disabled:opacity-40 disabled:cursor-not-allowed" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════ -->
    <!-- 帮助弹窗 (Teleported) -->
    <!-- ═══════════════════════════════════════════ -->
    <Teleport to="body">
      <div
        v-if="showHelp"
        class="fixed inset-0 z-50 flex items-center justify-center"
        @click.self="showHelp = false"
      >
        <div class="bg-black/40 absolute inset-0"></div>
        <div class="relative bg-white rounded-lg shadow-xl w-[560px] max-h-[80vh] overflow-y-auto z-10">
          <div class="p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-[16px] font-bold text-[#333]">计费水量月报表 — 使用说明</h3>
              <button
                class="w-8 h-8 flex items-center justify-center rounded-full border-none bg-transparent cursor-pointer text-[#999] hover:bg-[#f5f5f5] text-[18px]"
                @click="showHelp = false"
              >✕</button>
            </div>
            <div class="text-[13px] text-[#666] leading-relaxed space-y-3">
              <div>
                <h4 class="font-bold text-[#333] mb-1">报表概述</h4>
                <p>计费水量月报表按<strong>按月+每日明细</strong>展示用水量与水费，自动计算月度汇总和平均单价。数据来源：水表读数流水表实时聚合计算。</p>
              </div>
              <div>
                <h4 class="font-bold text-[#333] mb-1">筛选条件</h4>
                <ul class="list-disc pl-5 space-y-1">
                  <li><strong>日期范围</strong>：选择统计起止日期，默认当月1日~今天</li>
                  <li><strong>商户名称</strong>：模糊搜索商户名称</li>
                  <li><strong>设备编号</strong>：筛选指定水表</li>
                </ul>
              </div>
              <div>
                <h4 class="font-bold text-[#333] mb-1">报表结构</h4>
                <ul class="list-disc pl-5 space-y-1">
                  <li><strong>固定列</strong>：统计月份、活跃水表数、有效天数</li>
                  <li><strong>动态日列</strong>：每天一列（用水量 + 水费），按日期范围自动生成</li>
                  <li><strong>汇总列</strong>：月总用水量、月总水费、月均单价</li>
                  <li><strong>趋势图</strong>：按月展示计费水量/水费走势，支持柱状/折线切换</li>
                </ul>
              </div>
              <div>
                <h4 class="font-bold text-[#333] mb-1">指标说明</h4>
                <ul class="list-disc pl-5 space-y-1">
                  <li><strong>平均单价</strong>：总水费 ÷ 总用水量，用于核验费率一致性</li>
                  <li><strong>活跃水表数</strong>：当月有读数记录的水表数量</li>
                  <li><strong>有效天数</strong>：当月有读数记录的天数</li>
                </ul>
              </div>
              <div>
                <h4 class="font-bold text-[#333] mb-1">数据导出</h4>
                <p>点击<strong>「导出」</strong>按钮可将全部明细数据导出为 Excel（.xlsx）文件，包含每日用水量和费用详情。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Toast -->
    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="toastVisible" class="fixed top-6 left-1/2 -translate-x-1/2 z-[200] bg-white rounded-lg shadow-lg border border-[#e8e8e8] px-5 py-3 flex items-center gap-2">
          <svg class="w-4 h-4 text-[#faad14] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span class="text-[13px] text-[#333]">{{ toastMessage }}</span>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
