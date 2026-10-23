<template>
  <view class="home-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">明思管家</text>
      </view>
    </view>

    <view class="meter-selector" v-if="meters.length">
      <text class="meter-selector-label">当前表计</text>
      <picker :range="meterLabels" :value="selectedMeterIndex" @change="onMeterChange">
        <view class="meter-selector-value">{{ meterLabels[selectedMeterIndex] || '请选择' }} ▼</view>
      </picker>
    </view>

    <!-- 顶部用户信息区 -->
    <view class="user-card">
      <view class="user-top">
        <view class="avatar-sm">
          <text class="avatar-text-sm">微</text>
        </view>
        <text class="user-name">{{ userInfo ? userInfo.nickname || '微信用户' : '微信用户' }}</text>
      </view>
      <view class="balance-row">
        <view class="balance-item">
          <text class="balance-label">电表</text>
          <view class="balance-data">
            <text class="balance-val">{{ dashboard.electricBalance || '0.00' }}</text>
            <text class="balance-unit"> 元</text>
          </view>
          <text class="balance-sub">预计可用 {{ dashboard.electricEstimatedDays || 0 }} 天</text>
        </view>
        <view class="balance-divider"></view>
        <view class="balance-item">
          <text class="balance-label">水表</text>
          <view class="balance-data">
            <text class="balance-val">{{ dashboard.waterBalance || '0.00' }}</text>
            <text class="balance-unit"> 元</text>
          </view>
          <text class="balance-sub">预计可用 {{ dashboard.waterEstimatedDays || 0 }} 天</text>
        </view>
      </view>
    </view>

    <!-- 用电用水数据栏 -->
    <view class="usage-row">
      <view class="usage-block">
        <view class="usage-block-header">
          <text class="usage-dot electric"></text>
          <text class="usage-block-title">用电</text>
        </view>
        <view class="usage-block-body">
          <view class="usage-stat">
            <text class="usage-num">{{ dashboard.todayElectricUsage || 0 }}</text>
            <text class="usage-u"> kWh</text>
            <text class="usage-tag">今日用电量</text>
          </view>
          <view class="usage-stat">
            <text class="usage-num">{{ dashboard.last30DaysElectricUsage || 0 }}</text>
            <text class="usage-u"> kWh</text>
            <text class="usage-tag">30日用电量</text>
          </view>
        </view>
      </view>
      <view class="usage-block">
        <view class="usage-block-header">
          <text class="usage-dot water"></text>
          <text class="usage-block-title">用水</text>
        </view>
        <view class="usage-block-body">
          <view class="usage-stat">
            <text class="usage-num">{{ dashboard.yesterdayWaterUsage || 0 }}</text>
            <text class="usage-u"> m³</text>
            <text class="usage-tag">昨日用水量</text>
          </view>
          <view class="usage-stat">
            <text class="usage-num">{{ dashboard.lastMonthWaterUsage || 0 }}</text>
            <text class="usage-u"> m³</text>
            <text class="usage-tag">上月用水量</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 系统提示语 -->
    <view class="welcome-bar">
      <text class="welcome-text">{{ dashboard.customerName ? dashboard.customerName + '，欢迎使用明思管家' : '欢迎使用明思管家' }}</text>
    </view>

    <!-- 我的电表 -->
    <view class="module-card">
      <view class="module-title">
        <text class="module-title-text">我的电表</text>
      </view>
      <view class="func-grid">
        <view class="func-cell" @click="goPage('电表详情')">
          <view class="func-icon-box orange-bg">
            <image class="icon-img" src="/static/electric-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">我的电表</text>
        </view>
        <view v-if="!isViewerOnly" class="func-cell" @click="goPage('充值缴费')">
          <view class="func-icon-box blue-bg">
            <image class="icon-img" src="/static/recharge-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">充值缴费</text>
        </view>
        <view class="func-cell" @click="goPage('充值明细')">
          <view class="func-icon-box green-bg">
            <image class="icon-img" src="/static/receipt-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">充值明细</text>
        </view>
        <view class="func-cell" @click="goPage('我的票据')">
          <view class="func-icon-box yellow-bg">
            <image class="icon-img" src="/static/invoice-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">我的票据</text>
        </view>
      </view>
    </view>

    <!-- 我的水表 -->
    <view class="module-card">
      <view class="module-title">
        <text class="module-title-text">我的水表</text>
      </view>
      <view class="func-grid">
        <view class="func-cell" @click="goPage('水表详情')">
          <view class="func-icon-box blue-bg">
            <image class="icon-img" src="/static/water-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">我的水表</text>
        </view>
        <view v-if="!isViewerOnly" class="func-cell" @click="goPage('水表充值')">
          <view class="func-icon-box blue-bg">
            <image class="icon-img" src="/static/recharge-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">充值缴费</text>
        </view>
        <view class="func-cell" @click="goPage('水表明细')">
          <view class="func-icon-box green-bg">
            <image class="icon-img" src="/static/receipt-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-label">充值明细</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDashboard } from '@/api/dashboard.js'
import { getMyMeters } from '@/api/meter.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      dashboard: {
        customerName: '',
        electricBalance: 0,
        electricEstimatedDays: 0,
        waterBalance: 0,
        waterEstimatedDays: 0,
        todayElectricUsage: 0,
        last30DaysElectricUsage: 0,
        yesterdayWaterUsage: 0,
        lastMonthWaterUsage: 0
      },
      userInfo: null,
      meters: [],
      selectedMeterIndex: 0,
      isViewerOnly: false,
      loading: true
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  onShow() {
    this.loadDashboard()
  },
  methods: {
    loadDashboard() {
      const userInfo = uni.getStorageSync('userInfo')
      this.userInfo = userInfo
      this.isViewerOnly = !uni.getStorageSync('customerInfo')
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
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },

    onMeterChange(e) {
      this.selectedMeterIndex = Number(e.detail.value)
      const selected = this.meters[this.selectedMeterIndex]
      if (!selected) return
      uni.setStorageSync('selectedMeterId', selected.meterId)
      this.loadDashboard()
    },

    goPage(name) {
      const selected = this.meters[this.selectedMeterIndex]
      const electricMeter = selected && selected.meterType === 'ELECTRIC'
        ? selected : this.meters.find(item => item.meterType === 'ELECTRIC')
      const waterMeter = selected && selected.meterType === 'WATER'
        ? selected : this.meters.find(item => item.meterType === 'WATER')
      switch (name) {
        case '电表详情':
          uni.navigateTo({ url: `/pages-sub/meter/index?type=ELECTRIC${electricMeter ? `&meterId=${electricMeter.meterId}` : ''}` })
          break
        case '水表详情':
          uni.navigateTo({ url: `/pages-sub/meter/index?type=WATER${waterMeter ? `&meterId=${waterMeter.meterId}` : ''}` })
          break
        case '充值缴费':
          uni.navigateTo({ url: '/pages-sub/recharge/index?type=ELECTRIC' })
          break
        case '水表充值':
          uni.navigateTo({ url: '/pages-sub/recharge/index?type=WATER' })
          break
        case '充值明细':
        case '水表明细':
          uni.navigateTo({ url: '/pages-sub/recharge-history/index' })
          break
        case '我的票据':
          uni.navigateTo({ url: '/pages-sub/invoice/index' })
          break
        default:
          uni.showToast({ title: name, icon: 'none' })
      }
    }
  }
  ,
  computed: {
    meterLabels() {
      return this.meters.map(item => `${item.meterName || item.meterNo || '未命名表计'}（${item.meterNo || '--'}）`)
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fafafa;
  padding-bottom: 40rpx;
}

.meter-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22rpx 30rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
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

/* 用户信息卡 */
.user-card {
  margin: 0;
  background: #ff7a3d url(/static/UserBg.jpg) center/cover no-repeat;
  border-radius: 0;
  padding: 30rpx;
}

.user-top {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.avatar-sm {
  width: 72rpx;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.4);
}

.avatar-text-sm {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.balance-row {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 16rpx;
  padding: 24rpx 0;
}

.balance-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.balance-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6rpx;
}

.balance-data {
  margin-bottom: 4rpx;
}

.balance-val {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}

.balance-unit {
  font-size: 22rpx;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.8);
}

.balance-sub {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.65);
}

.balance-divider {
  width: 2rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.2);
}

/* 用电用水数据栏 */
.usage-row {
  margin: 20rpx 0 0;
  display: flex;
  gap: 20rpx;
}

.usage-block {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.usage-block-header {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
}

.usage-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-right: 10rpx;
}

.usage-dot.electric {
  background: #ff7a3d;
}

.usage-dot.water {
  background: #4b90e2;
}

.usage-block-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.usage-block-body {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.usage-stat {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.usage-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.usage-u {
  font-size: 22rpx;
  color: #999;
  margin-right: 10rpx;
}

.usage-tag {
  font-size: 20rpx;
  color: #bbb;
}

/* 欢迎提示 */
.welcome-bar {
  margin: 20rpx 0 0;
  background: #fff;
  border-radius: 0;
  padding: 20rpx 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.welcome-text {
  font-size: 26rpx;
  color: #999;
}

/* 功能模块卡片 */
.module-card {
  margin: 20rpx 0 0;
  background: #fff;
  border-radius: 0;
  padding: 28rpx 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.module-title {
  margin-bottom: 24rpx;
}

.module-title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a1a1a;
}


/* 功能网格 */
.func-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.func-cell {
  width: calc((100% - 32rpx) / 3);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0 16rpx;
}

.func-icon-box {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.func-emoji {
  font-size: 38rpx;
}
.icon-img {
  width: 56rpx;
  height: 56rpx;
}

.orange-bg { background: #fff5f0; }
.blue-bg   { background: #eff6ff; }
.green-bg  { background: #ecfdf5; }
.yellow-bg { background: #fefce8; }
.purple-bg { background: #f5f3ff; }
.cyan-bg   { background: #ecfeff; }

.func-label {
  font-size: 24rpx;
  color: #333;
  text-align: center;
}

/* 运营资讯链接 */
.info-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding: 20rpx 16rpx;
  background: #fafafa;
  border-radius: 12rpx;
}

.info-link-left {
  display: flex;
  align-items: center;
}

.info-icon-text {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.info-link-text {
  font-size: 26rpx;
  color: #555;
}

.info-arrow {
  font-size: 32rpx;
  color: #ccc;
}
</style>
