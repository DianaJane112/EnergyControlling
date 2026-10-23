<template>
  <view class="usage-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">用能分析</text>
      </view>
    </view>

    <view class="meter-selector" v-if="meters.length">
      <text class="meter-selector-label">统计表计</text>
      <picker :range="meterLabels" :value="selectedMeterIndex" @change="onMeterChange">
        <view class="meter-selector-value">{{ meterLabels[selectedMeterIndex] || '请选择' }} ▼</view>
      </picker>
    </view>

    <!-- 用能概览 -->
    <view class="section-card" v-if="!loading">
      <view class="section-header">
        <text class="section-title">用能概览</text>
      </view>

      <!-- 用电 -->
      <view class="overview-block">
        <view class="overview-header">
          <text class="overview-dot electric"></text>
          <text class="overview-title">用电</text>
        </view>
        <view class="overview-row">
          <view class="overview-item">
            <text class="overview-num">{{ dashboard.todayElectricUsage || 0 }}</text>
            <text class="overview-unit"> kWh</text>
            <text class="overview-label">今日用电量</text>
          </view>
          <view class="overview-item">
            <text class="overview-num">{{ dashboard.last30DaysElectricUsage || 0 }}</text>
            <text class="overview-unit"> kWh</text>
            <text class="overview-label">近30日用电量</text>
          </view>
        </view>
      </view>

      <!-- 用水 -->
      <view class="overview-block">
        <view class="overview-header">
          <text class="overview-dot water"></text>
          <text class="overview-title">用水</text>
        </view>
        <view class="overview-row">
          <view class="overview-item">
            <text class="overview-num">{{ dashboard.yesterdayWaterUsage || 0 }}</text>
            <text class="overview-unit"> m³</text>
            <text class="overview-label">昨日用水量</text>
          </view>
          <view class="overview-item">
            <text class="overview-num">{{ dashboard.lastMonthWaterUsage || 0 }}</text>
            <text class="overview-unit"> m³</text>
            <text class="overview-label">上月用水量</text>
          </view>
        </view>
      </view>

      <!-- 余额预测 -->
      <view class="forecast-box">
        <text class="forecast-text">
          电表余额 <text class="highlight">{{ dashboard.electricBalance || '0.00' }} 元</text>，预计可用 <text class="highlight">{{ dashboard.electricEstimatedDays || 0 }} 天</text>；水表余额 <text class="highlight">{{ dashboard.waterBalance || '0.00' }} 元</text>，预计可用 <text class="highlight">{{ dashboard.waterEstimatedDays || 0 }} 天</text>
        </text>
      </view>

      <!-- 温馨提示 -->
      <view class="notice-box">
        <image class="notice-icon" src="/static/notice-icon.svg" mode="aspectFit" />
        <text class="notice-text">数据可能因采集装置信号异常延迟 / 缺失，预测信息仅作参考，最终电费以结算账单为准。</text>
      </view>
    </view>

    <!-- 加载中 -->
    <view class="section-card" v-if="loading">
      <view class="section-header">
        <text class="section-title">用能概览</text>
      </view>
      <view class="loading-area">
        <view class="dot-bounce">
          <view class="dot dot-orange"></view>
          <view class="dot dot-blue"></view>
          <view class="dot dot-orange"></view>
        </view>
        <text class="loading-text">数据加载中...</text>
      </view>
    </view>

    <!-- 日用电趋势 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">日用电趋势</text>
        <picker mode="date" fields="month" :value="dayTrendMonth" @change="onDayMonthChange">
          <view class="picker-trigger">
            <text class="picker-label">{{ dayTrendMonth }}</text>
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="chart-area" v-if="!dayLoading && dayTrend.length">
        <scroll-view scroll-y class="chart-scroll-y">
          <view v-for="(item, index) in dayTrend" :key="index" class="hbar-item">
            <text class="hbar-label">{{ (item.date || '').substring(8) }}日</text>
            <view class="hbar-track">
              <view class="hbar-fill" :style="{ width: barWidth(item.usage, dayMax) + '%' }"></view>
            </view>
            <text class="hbar-val">{{ item.usage }} kWh</text>
          </view>
        </scroll-view>
        <view class="chart-summary">
          <text class="summary-text">月累计 <text class="summary-num">{{ dayTotal }} kWh</text>，日均 <text class="summary-num">{{ dayAvg }} kWh</text></text>
        </view>
      </view>
      <view class="placeholder-area" v-if="!dayLoading && !dayTrend.length">
        <text class="placeholder-text">暂无数据</text>
      </view>
      <view class="placeholder-area" v-if="dayLoading">
        <text class="placeholder-text">加载中...</text>
      </view>
    </view>

    <!-- 月用电趋势 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">月用电趋势</text>
        <picker mode="date" fields="year" :value="monthTrendYear" @change="onMonthYearChange">
          <view class="picker-trigger">
            <text class="picker-label">{{ monthTrendYear }}</text>
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="chart-area" v-if="!monthLoading && monthTrend.length">
        <scroll-view scroll-y class="chart-scroll-y">
          <view v-for="(item, index) in monthTrend" :key="index" class="hbar-item">
            <text class="hbar-label">{{ (item.month || '').substring(5) }}月</text>
            <view class="hbar-track">
              <view class="hbar-fill" :style="{ width: barWidth(item.usage, monthMax) + '%' }"></view>
            </view>
            <text class="hbar-val">{{ item.usage }} kWh</text>
          </view>
        </scroll-view>
        <view class="chart-summary">
          <text class="summary-text">年累计 <text class="summary-num">{{ monthTotal }} kWh</text>，月均 <text class="summary-num">{{ monthAvg }} kWh</text></text>
        </view>
      </view>
      <view class="placeholder-area" v-if="!monthLoading && !monthTrend.length">
        <text class="placeholder-text">暂无数据</text>
      </view>
      <view class="placeholder-area" v-if="monthLoading">
        <text class="placeholder-text">加载中...</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getDashboard } from '@/api/dashboard.js'
import { getDailyElectricTrend, getMonthlyElectricTrend } from '@/api/energy.js'
import { getMyMeters } from '@/api/meter.js'

export default {
  data() {
    const now = new Date()
    return {
      statusBarHeight: 20,
      loading: true,
      dayTrendMonth: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
      monthTrendYear: String(now.getFullYear()),
      dayTrend: [],
      monthTrend: [],
      meters: [],
      selectedMeterIndex: 0,
      dayLoading: false,
      monthLoading: false,
      dashboard: {
        electricBalance: 0,
        electricEstimatedDays: 0,
        waterBalance: 0,
        waterEstimatedDays: 0,
        todayElectricUsage: 0,
        last30DaysElectricUsage: 0,
        yesterdayWaterUsage: 0,
        lastMonthWaterUsage: 0
      }
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  computed: {
    meterLabels() {
      return this.meters.map(item => `${item.meterName || item.meterNo || '未命名表计'}（${item.meterNo || '--'}）`)
    },
    dayMax() { return this.trendMax(this.dayTrend) },
    monthMax() { return this.trendMax(this.monthTrend) },
    dayTotal() { return this.trendSum(this.dayTrend) },
    monthTotal() { return this.trendSum(this.monthTrend) },
    dayAvg() {
      return this.dayTrend.length ? (this.trendSum(this.dayTrend) / this.dayTrend.length).toFixed(2) : '0'
    },
    monthAvg() {
      return this.monthTrend.length ? (this.trendSum(this.monthTrend) / this.monthTrend.length).toFixed(2) : '0'
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      getMyMeters().then((meters) => {
        this.meters = Array.isArray(meters) ? meters : []
        const savedId = uni.getStorageSync('selectedMeterId')
        const savedIndex = this.meters.findIndex(item => item.meterId === savedId)
        this.selectedMeterIndex = savedIndex >= 0 ? savedIndex : 0
        const selected = this.meters[this.selectedMeterIndex]
        if (selected) uni.setStorageSync('selectedMeterId', selected.meterId)
        return getDashboard(selected ? selected.meterId : null)
      })
        .then((data) => {
          this.dashboard = data
          this.loadDayTrend()
          this.loadMonthTrend()
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    loadDayTrend() {
      this.dayLoading = true
      const selected = this.meters[this.selectedMeterIndex]
      getDailyElectricTrend(this.dayTrendMonth, selected ? selected.meterId : null)
        .then((data) => {
          this.dayTrend = Array.isArray(data) ? data : []
        })
        .catch(() => {})
        .finally(() => { this.dayLoading = false })
    },
    loadMonthTrend() {
      this.monthLoading = true
      const selected = this.meters[this.selectedMeterIndex]
      getMonthlyElectricTrend(this.monthTrendYear, selected ? selected.meterId : null)
        .then((data) => {
          this.monthTrend = Array.isArray(data) ? data : []
        })
        .catch(() => {})
        .finally(() => { this.monthLoading = false })
    },
    onDayMonthChange(e) {
      this.dayTrendMonth = e.detail.value
      this.loadDayTrend()
    },
    onMonthYearChange(e) {
      this.monthTrendYear = e.detail.value
      this.loadMonthTrend()
    },
    onMeterChange(e) {
      this.selectedMeterIndex = Number(e.detail.value)
      const selected = this.meters[this.selectedMeterIndex]
      if (selected) uni.setStorageSync('selectedMeterId', selected.meterId)
      this.loadData()
    },
    barWidth(usage, max) {
      if (!max) return 5
      return Math.max((usage / max) * 100, 5)
    },
    trendMax(arr) {
      if (!arr.length) return 0
      return Math.max(...arr.map(item => item.usage || 0))
    },
    trendSum(arr) {
      return arr.reduce((sum, item) => sum + (item.usage || 0), 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.usage-page {
  min-height: 100vh;
  background: #fafafa;
  padding-bottom: 40rpx;
}
.meter-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20rpx 16rpx 0;
  padding: 22rpx 30rpx;
  background: #fff;
  border-radius: 16rpx;
}
.meter-selector-label { font-size: 26rpx; color: #666; }
.meter-selector-value { font-size: 26rpx; color: #ff7a3d; }

/* 自定义导航栏 */
.nav-bar {
  width: 100%;
  background: #ff7a3d;
}

.nav-content {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2rpx;
}

/* 卡片通用 */
.section-card {
  background: #fff;
  margin: 20rpx 16rpx 0;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a1a;
}


/* 用能概览 */
.overview-block {
  margin-bottom: 28rpx;
}

.overview-header {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
}

.overview-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-right: 10rpx;
}

.overview-dot.electric { background: #ff7a3d; }
.overview-dot.water { background: #4b90e2; }

.overview-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.overview-row {
  display: flex;
  gap: 40rpx;
}

.overview-item {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.overview-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.overview-unit {
  font-size: 22rpx;
  color: #999;
  margin-right: 10rpx;
}

.overview-label {
  font-size: 22rpx;
  color: #bbb;
}

/* 余额预测 */
.forecast-box {
  background: #fafafa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
}

.forecast-text {
  font-size: 26rpx;
  color: #666;
  line-height: 44rpx;
}

.highlight {
  color: #ff7a3d;
  font-weight: 600;
}

/* 加载动画 */
.loading-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
}

.dot-bounce {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite both;
}

.dot-orange { background: #ff7a3d; }
.dot-blue   { background: #4b90e2; }

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.loading-text {
  font-size: 24rpx;
  color: #bbb;
}

/* 月份/年份选择器 */
.picker-trigger {
  display: flex;
  align-items: center;
  background: #fff5f0;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
}

.picker-label {
  font-size: 24rpx;
  color: #ff7a3d;
  font-weight: 500;
  margin-right: 8rpx;
}

.picker-arrow {
  font-size: 18rpx;
  color: #ff7a3d;
}

/* 温馨提示 */
.notice-box {
  background: #fafafa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  display: flex;
}

.notice-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.notice-text {
  font-size: 22rpx;
  color: #999;
  line-height: 36rpx;
}

/* 图表占位 */
.placeholder-area {
  padding: 80rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  font-size: 26rpx;
  color: #bbb;
}

/* 趋势图表 */
.chart-area {
  margin-top: 10rpx;
}
.chart-scroll-y {
  max-height: 440rpx;
}
.hbar-item {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
}
.hbar-label {
  width: 72rpx;
  font-size: 24rpx;
  color: #666;
  text-align: right;
  flex-shrink: 0;
}
.hbar-track {
  flex: 1;
  height: 36rpx;
  background: #f0f4f8;
  border-radius: 8rpx;
  overflow: hidden;
  margin: 0 16rpx;
}
.hbar-fill {
  height: 100%;
  border-radius: 8rpx;
  background: #4b90e2;
  min-width: 10rpx;
}
.hbar-val {
  width: 130rpx;
  font-size: 24rpx;
  color: #333;
  flex-shrink: 0;
}
.chart-summary {
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
  text-align: center;
}
.summary-text {
  font-size: 24rpx;
  color: #999;
}
.summary-num {
  color: #ff7a3d;
  font-weight: 600;
}
</style>
