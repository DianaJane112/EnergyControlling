<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { Lightning } from '@icon-park/vue-next'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardStatistics, getElectricityDailySummary, getWaterDailySummary } from '@/api/dashboard.js'

use([LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const fmt = d => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ── 状态 ───────────────────────────────────────
const dashData = ref(null)
const trendType = ref('dayElec')
const bottomData = ref([])
const bottomLoading = ref(false)

const tabs = [
  { key: 'dayElec', label: '日总用电量' },
  { key: 'dayWater', label: '日总用水量' },
  { key: 'monthElec', label: '月总用电量' },
  { key: 'monthWater', label: '月总用水量' },
]

// ── 下半区：按日/月获取数据 ─────────────────────
async function fetchBottomChartData() {
  const trends = dailyTrends.value
  bottomLoading.value = true
  try {
    const isDay = trendType.value.startsWith('day')
    const isElec = trendType.value.includes('Elec')

    if (isDay) {
      // 日模式：后端若无今日数据则取最近一天，横坐标 0-23 小时
      const today = fmt(new Date())
      let items = []
      try {
        const res = isElec
          ? await getElectricityDailySummary({ startDate: today, endDate: today })
          : await getWaterDailySummary({ startDate: today, endDate: today })
        items = res.data || []
      } catch { /* 接口失败静默 */ }

      let total = isElec
        ? items.reduce((s, i) => s + (i.totalConsumed || 0), 0)
        : items.reduce((s, i) => s + (i.totalUsage || 0), 0)

      // 今日无数据则回退到 dailyTrends 最近一天
      if (total === 0 && trends.length > 0) {
        const last = trends[trends.length - 1]
        total = isElec ? last.totalElectricity : last.totalWater
      }

      const hours = Array.from({ length: 24 }, (_, i) => ({
        label: `${i}`,
        value: 0,
      }))
      if (total > 0) {
        const avg = +(total / 24).toFixed(2)
        hours.forEach(h => { h.value = avg })
      }
      bottomData.value = hours
    } else {
      // 月模式：查本月 daily-summary，无数据则回退 dailyTrends
      const now = new Date()
      const year = now.getFullYear()
      const month = now.getMonth()
      const lastDay = new Date(year, month + 1, 0).getDate()

      const dayMap = {}
      for (let d = 1; d <= lastDay; d++) {
        const key = fmt(new Date(year, month, d))
        dayMap[key] = { label: `${d}`, value: 0 }
      }

      try {
        const startDate = fmt(new Date(year, month, 1))
        const endDate = fmt(new Date(year, month, lastDay))
        const res = isElec
          ? await getElectricityDailySummary({ startDate, endDate })
          : await getWaterDailySummary({ startDate, endDate })

        ;(res.data || []).forEach(i => {
          const dateKey = isElec ? i.period : i.statDate
          if (dayMap[dateKey] != null) {
            dayMap[dateKey].value += (isElec ? (i.totalConsumed || 0) : (i.totalUsage || 0))
          }
        })
      } catch { /* 接口失败静默 */ }

      // 如果本月接口全 0，用 dailyTrends 补
      const totalFromApi = Object.values(dayMap).reduce((s, d) => s + d.value, 0)
      if (totalFromApi === 0 && trends.length > 0) {
        trends.forEach(t => {
          if (dayMap[t.date] != null) {
            dayMap[t.date].value = isElec ? t.totalElectricity : t.totalWater
          }
        })
      }

      bottomData.value = Object.values(dayMap)
    }
  } catch {
    bottomData.value = []
  } finally {
    bottomLoading.value = false
  }
}

watch(trendType, fetchBottomChartData)

// ── 加载核心数据 ────────────────────────────────
const dashLoading = ref(false)
const dashError = ref('')

async function loadDashboard() {
  dashLoading.value = true
  dashError.value = ''
  try {
    const dashRes = await getDashboardStatistics({ _t: Date.now() })
    dashData.value = { ...dashRes.data }

    // 上半区趋势：近 7 天
    try {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      const dateParams = { startDate: fmt(start), endDate: fmt(end) }

      const dateMap = {}
      for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
        const key = fmt(new Date(d))
        dateMap[key] = { date: key, totalElectricity: 0, totalWater: 0 }
      }

      const [elecRes, waterRes] = await Promise.all([
        getElectricityDailySummary(dateParams),
        getWaterDailySummary(dateParams),
      ])

      ;(elecRes.data || []).forEach(i => {
        if (dateMap[i.period]) dateMap[i.period].totalElectricity += (i.totalConsumed || 0)
        else dateMap[i.period] = { date: i.period, totalElectricity: i.totalConsumed || 0, totalWater: 0 }
      })
      ;(waterRes.data || []).forEach(i => {
        if (dateMap[i.statDate]) dateMap[i.statDate].totalWater += (i.totalUsage || 0)
        else dateMap[i.statDate] = { date: i.statDate, totalElectricity: 0, totalWater: i.totalUsage || 0 }
      })

      dashData.value.dailyTrends = Object.values(dateMap).sort((a, b) => a.date.localeCompare(b.date))
    } catch { /* 静默 */ }
  } catch (e) {
    dashError.value = e.message || '加载失败'
  } finally {
    dashLoading.value = false
  }
}

onMounted(async () => {
  await loadDashboard()
  fetchBottomChartData()
})

function switchTrendType(key) {
  trendType.value = key
}

// ── 派生数据 ────────────────────────────────────
const dailyTrends = computed(() => dashData.value?.dailyTrends || [])

const monthTotalElec = computed(() => dailyTrends.value.reduce((s, t) => s + t.totalElectricity, 0))
const monthTotalWater = computed(() => dailyTrends.value.reduce((s, t) => s + t.totalWater, 0))

const todayTrend = computed(() => {
  const arr = dailyTrends.value
  return arr.length ? arr[arr.length - 1] : null
})

const yesterdayTrend = computed(() => {
  const arr = dailyTrends.value
  return arr.length > 1 ? arr[arr.length - 2] : null
})

const overdueAlerts = computed(() => (dashData.value?.riskAlerts || []).filter(a => a.balance < 0))
const lowBalanceAlerts = computed(() => (dashData.value?.riskAlerts || []).filter(a => a.balance > 0 && a.balance < 50))

// ── 左上图：近 7 天用电/用水趋势（双折线）─────────
const dailyChartOption = computed(() => {
  const trends = dailyTrends.value
  const dates = trends.map(t => t.date.slice(5))
  const elec = trends.map(t => t.totalElectricity)
  const water = trends.map(t => t.totalWater)

  return {
    grid: { left: 48, right: 16, top: 36, bottom: 24 },
    legend: {
      data: ['用电量(kWh)', '用水量(t)'],
      right: 0, top: 2,
      textStyle: { fontSize: 12 },
    },
    xAxis: {
      type: 'category', data: dates,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value', min: 0,
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      {
        name: '用电量(kWh)', type: 'line', data: elec,
        symbol: 'circle', symbolSize: 5,
        lineStyle: { color: '#1890ff', width: 2 },
        itemStyle: { color: '#1890ff' },
      },
      {
        name: '用水量(t)', type: 'line', data: water,
        symbol: 'circle', symbolSize: 5,
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' },
      },
    ],
    tooltip: { trigger: 'axis' },
  }
})

// ── 下图：日/月趋势（折线图）─────────────────────
const isDayMode = computed(() => trendType.value.startsWith('day'))
const isElecMode = computed(() => trendType.value.includes('Elec'))

const bottomChartOption = computed(() => {
  const data = bottomData.value
  const labels = data.map(d => d.label)
  const values = data.map(d => d.value)
  const name = isElecMode.value ? '用电量(kWh)' : '用水量(t)'
  const color = isElecMode.value ? '#1d3557' : '#2b6ed7'

  return {
    grid: { left: 52, right: 16, top: 24, bottom: 28 },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: {
        formatter: isDayMode.value ? '{value}时' : '{value}日',
        fontSize: 11,
      },
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value', min: 0,
      name,
      nameTextStyle: { fontSize: 11, color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      name, type: 'line', data: values,
      symbol: 'circle', symbolSize: 5,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: { color: isElecMode.value ? 'rgba(29,53,87,0.06)' : 'rgba(43,110,215,0.06)' },
    }],
    tooltip: { trigger: 'axis' },
  }
})
</script>

<template>
  <div v-if="dashLoading" class="loading">加载中...</div>
  <div v-else-if="dashError" class="loading">
    <span>{{ dashError }}</span>
    <button class="retry-btn" @click="loadDashboard">重试</button>
  </div>
  <div v-else-if="dashData" class="dashboard-root">
    <!-- ═════════ 上半区 ═════════ -->
    <div class="top-row">
      <!-- 左：近 7 天趋势 -->
      <div class="card recharge-chart-card">
        <h3 class="card-title">近 7 天用量趋势</h3>
        <VChart v-if="dailyTrends.length" :option="dailyChartOption" autoresize class="chart" />
        <div v-else class="chart-empty">暂无用量数据</div>
      </div>

      <!-- 右：三张统计卡片 -->
      <div class="right-cards">
        <!-- 财务概览 -->
        <div class="stat-card stat-card--dark">
          <div class="stat-card-header">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
            <span>财务概览</span>
          </div>
          <div class="stat-card-body">
            <div class="stat-list">
              <div class="stat-item"><span class="stat-label">总余额</span><span class="stat-val">{{ dashData.financeOverview.totalBalance.toLocaleString() }}元</span></div>
              <div class="stat-item"><span class="stat-label">今日充值</span><span class="stat-val">{{ dashData.financeOverview.todayRechargeTotal.toLocaleString() }}元</span></div>
              <div class="stat-item"><span class="stat-label">今日扣款</span><span class="stat-val">{{ dashData.financeOverview.todayDebitTotal.toLocaleString() }}元</span></div>
              <div class="stat-item"><span class="stat-label">活跃设备</span><span class="stat-val">{{ dashData.assetOverview.activeMeters }}台</span></div>
            </div>
          </div>
        </div>

        <!-- 资产概览 -->
        <div class="stat-card stat-card--light">
          <div class="stat-card-header">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor"><rect x="1" y="6" width="3" height="9" rx="0.5"/><rect x="5" y="2" width="3" height="13" rx="0.5"/><rect x="9" y="4" width="3" height="11" rx="0.5"/><rect x="13" y="7" width="3" height="8" rx="0.5"/></svg>
            <span>资产概览</span>
          </div>
          <div class="stat-card-body">
            <div class="stat-list">
              <div class="stat-item"><span class="stat-label">设备总数</span><span class="stat-val">{{ dashData.assetOverview.totalMeters }}台</span></div>
              <div class="stat-item"><span class="stat-label">在线率</span><span class="stat-val">{{ dashData.assetOverview.onlineRate }}%</span></div>
              <div class="stat-item"><span class="stat-label">当月总电量</span><span class="stat-val">{{ monthTotalElec.toLocaleString() }} kWh</span></div>
              <div class="stat-item"><span class="stat-label">当月总水量</span><span class="stat-val">{{ monthTotalWater.toLocaleString() }} t</span></div>
            </div>
          </div>
        </div>

        <!-- 风险提醒 -->
        <div class="stat-card stat-card--soft">
          <div class="stat-card-header">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
            <span>风险提醒</span>
          </div>
          <div class="todo-list">
            <div class="todo-item">
              <span class="todo-icon todo-icon--red">!</span>
              <span class="todo-label">欠费商户</span>
              <span class="todo-num">{{ overdueAlerts.length }}个</span>
              <span class="todo-tag">欠费</span>
            </div>
            <div class="todo-item">
              <span class="todo-icon todo-icon--yellow">◎</span>
              <span class="todo-label">低余额商户</span>
              <span class="todo-num">{{ lowBalanceAlerts.length }}个</span>
              <span class="todo-tag">余额低</span>
            </div>
            <div class="todo-item" v-if="overdueAlerts.length">
              <span class="todo-icon todo-icon--red">!</span>
              <span class="todo-label">{{ overdueAlerts[0]?.customerName }}</span>
              <span class="todo-num">{{ Math.abs(overdueAlerts[0]?.balance).toLocaleString() }}元</span>
              <span class="todo-tag">最高欠费</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═════════ 中区：设备状态卡片 ═════════ -->
    <div class="mid-row">
      <div class="device-card">
        <div class="device-icon device-icon--orange">
          <Lightning theme="outline" size="24" fill="#eed319" :strokeWidth="3"/>
        </div>
        <div class="device-data">
          <div class="device-item">设备总数 <span class="device-num">{{ dashData.assetOverview.totalMeters }}</span></div>
          <div class="device-item">在线数 <span class="device-num">{{ dashData.assetOverview.activeMeters }}</span></div>
          <div class="device-item">离线数 <span class="device-num">{{ dashData.assetOverview.totalMeters - dashData.assetOverview.activeMeters }}</span></div>
        </div>
      </div>
      <div class="device-card">
        <div class="device-icon device-icon--red">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
        </div>
        <div class="device-data">
          <div class="device-item">欠费商户 <span class="device-num device-num--red">{{ overdueAlerts.length }}</span></div>
          <div class="device-item">逾期设备 <span class="device-num">{{ dashData.assetOverview.overdueMeters }}</span></div>
          <div class="device-item">欠费率 <span class="device-num">{{ dashData.assetOverview.onlineRate }}%</span></div>
        </div>
      </div>
      <div class="device-card">
        <div class="device-icon device-icon--blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <div class="device-data">
          <div class="device-item">今日用电 <span class="device-num">{{ todayTrend?.totalElectricity || 0 }} kWh</span></div>
          <div class="device-item">昨日用电 <span class="device-num">{{ yesterdayTrend?.totalElectricity || 0 }} kWh</span></div>
          <div class="device-item">今日用水 <span class="device-num">{{ todayTrend?.totalWater || 0 }} t</span></div>
        </div>
      </div>
    </div>

    <!-- ═════════ 下半区：日/月趋势 ═════════ -->
    <div class="card elec-chart-card">
      <div class="elec-header">
        <div class="elec-tabs">
          <button
            v-for="tab in tabs" :key="tab.key"
            :class="['elec-tab', { active: trendType === tab.key }]"
            @click="switchTrendType(tab.key)"
          >{{ tab.label }}</button>
        </div>
      </div>
      <div v-if="bottomLoading" class="chart-loading">加载中...</div>
      <VChart v-else-if="bottomData.length" :option="bottomChartOption" autoresize class="chart" />
      <div v-else class="chart-empty">暂无数据</div>
    </div>
  </div>

  <div v-else class="loading">加载中...</div>
</template>

<style scoped>
.dashboard-root {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.chart { width: 100%; flex: 1; min-height: 260px; }
.chart-empty,
.chart-loading {
  width: 100%; flex: 1; min-height: 260px;
  display: flex; align-items: center; justify-content: center;
  color: #999; font-size: 14px;
}
.card-title {
  font-size: 15px; font-weight: 600;
  color: #1e293b; margin: 0 0 4px;
  flex-shrink: 0;
}

/* ── 上半区 ─────────────────────── */
.top-row {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr;
  gap: 16px;
}
.recharge-chart-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.right-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ── 统计卡片 ───────────────────── */
.stat-card {
  border-radius: 10px;
  padding: 14px 16px;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.stat-card--dark {
  background: linear-gradient(135deg, #1a3a6b 0%, #265195 100%);
}
.stat-card--light {
  background: linear-gradient(135deg, #2b6ed7 0%, #4d8ff0 100%);
}
.stat-card--soft {
  background: linear-gradient(135deg, #e8f0fe 0%, #dce8fb 100%);
  color: #333;
}
.stat-card-header {
  display: flex; align-items: center; gap: 6px;
  font-weight: 600; font-size: 13px; margin-bottom: 10px;
  opacity: 0.9;
}
.stat-card--soft .stat-card-header {
  opacity: 1;
}
.stat-card-body {
  display: flex; flex-direction: column;
}
.stat-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  line-height: 1.8;
  gap: 12px;
}
.stat-label { opacity: 0.78; }
.stat-val { font-weight: 600; white-space: nowrap; }

/* ── 风险提醒 ───────────────────── */
.todo-list { display: flex; flex-direction: column; gap: 6px; }
.todo-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px;
}
.todo-icon {
  width: 22px; height: 22px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
  flex-shrink: 0;
}
.todo-icon--red { background: #ff4d4f; color: #fff; }
.todo-icon--yellow { background: #faad14; color: #fff; }
.todo-label { flex: 1; font-weight: 500; }
.todo-num { color: #666; margin-right: 4px; }
.todo-tag {
  font-size: 11px;
  background: #fff;
  padding: 1px 6px;
  border-radius: 3px;
}

/* ── 中区设备卡片 ────────────────── */
.mid-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.device-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.device-icon {
  width: 48px; height: 48px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.device-icon--orange { background: #fff7e6; color: #fa8c16; }
.device-icon--red    { background: #fff1f0; color: #ff4d4f; }
.device-icon--blue   { background: #e6f7ff; color: #1890ff; }
.device-data { flex: 1; min-width: 0; }
.device-item {
  display: flex; justify-content: space-between;
  font-size: 13px; line-height: 2;
  color: #555;
  gap: 8px;
}
.device-num {
  font-weight: 700; color: #fa8c16;
  min-width: 40px; text-align: right;
  white-space: nowrap;
}
.device-num--red { color: #ff4d4f; }

/* ── 下半区趋势图 ────────────────── */
.elec-chart-card { min-height: 360px; }
.elec-header { display: flex; align-items: center; margin-bottom: 8px; }
.elec-tabs { display: flex; gap: 0; }
.elec-tab {
  padding: 6px 16px;
  font-size: 13px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.elec-tab:first-child { border-radius: 4px 0 0 4px; }
.elec-tab:last-child  { border-radius: 0 4px 4px 0; }
.elec-tab.active {
  background: #3a64ab;
  color: #fff;
  border-color: #3a64ab;
}
.elec-tab:not(.active):hover { color: #3a64ab; border-color: #3a64ab; }

.loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 400px; color: #999; gap: 12px;
}
.retry-btn {
  padding: 6px 20px;
  border: 1px solid #3a64ab;
  border-radius: 4px;
  background: #fff;
  color: #3a64ab;
  font-size: 13px;
  cursor: pointer;
}
.retry-btn:hover { background: #3a64ab; color: #fff; }

@media (max-width: 1024px) {
  .top-row {
    grid-template-columns: 1fr;
  }
}
</style>
