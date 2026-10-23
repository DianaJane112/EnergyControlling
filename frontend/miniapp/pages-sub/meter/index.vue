<template>
  <view class="meter-page">
    <!-- 导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <view class="back-arrow"></view>
        </view>
        <text class="nav-title">{{ pageTitle }}</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 查询输入区 -->
    <view class="search-bar">
      <view class="search-row">
        <input
          class="search-input"
          v-model="meterNo"
          :placeholder="'请扫码或输入表号'"
          placeholder-style="color:#c0c0c0;font-size:28rpx;"
          @confirm="queryMeter"
        />
        <view class="scan-btn" @click="scanMeter">
          <image class="scan-icon-img" src="/static/scan-icon.svg" mode="aspectFit" />
        </view>
      </view>
    </view>

    <!-- 表计详情 -->
    <view class="meter-detail" v-if="meterInfo">
      <view class="detail-card">
        <view class="detail-header">
          <view class="detail-title-row">
            <text class="detail-name">{{ meterInfo.meterName || '--' }}</text>
            <text class="detail-status" :class="(meterInfo.status || '').toLowerCase() === 'running' ? 'running' : ''">
              {{ meterInfo.status === 'RUNNING' ? '运行中' : meterInfo.status || '--' }}
            </text>
          </view>
          <text class="detail-no">表号：{{ meterInfo.meterNo || '--' }}</text>
        </view>
        <view class="detail-body">
          <view class="detail-row">
            <text class="detail-label">当前余额</text>
            <text class="detail-value balance">{{ meterInfo.balance != null ? meterInfo.balance + ' 元' : '--' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">已使用量</text>
            <text class="detail-value">{{ meterInfo.totalUsage != null ? meterInfo.totalUsage + (meterType === 'ELECTRIC' ? ' kWh' : ' m³') : '--' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">上次读数</text>
            <text class="detail-value">{{ meterInfo.lastReading != null ? meterInfo.lastReading : '--' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">上次读数时间</text>
            <text class="detail-value">{{ formatTime(meterInfo.lastReadingTime) }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">安装地址</text>
            <text class="detail-value">{{ meterInfo.address || '--' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">预警线</text>
            <text class="detail-value">{{ meterInfo.warnline != null ? meterInfo.warnline + ' 元' : '--' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">透支限额</text>
            <text class="detail-value">{{ meterInfo.credit != null ? meterInfo.credit + ' 元' : '--' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 未查询状态 -->
    <view class="empty-state" v-if="!meterInfo && !querying && !notFound">
      <view class="empty-box">
        <image class="empty-icon-img" src="/static/empty-icon.svg" mode="aspectFit" />
      </view>
      <text class="empty-text">请扫码或输入表号查询</text>
    </view>

    <!-- 未找到 -->
    <view class="empty-state" v-if="!meterInfo && !querying && notFound">
      <view class="empty-box">
        <image class="empty-box-icon" src="/static/search-icon.svg" mode="aspectFit" />
      </view>
      <text class="empty-text">未找到该{{ meterType === 'ELECTRIC' ? '电表' : '水表' }}</text>
      <text class="empty-sub-text">请检查表号是否正确</text>
    </view>

    <!-- 查询中 -->
    <view class="loading-state" v-if="querying">
      <text class="loading-text">查询中...</text>
    </view>
  </view>
</template>

<script>
import { getElectricMeterByNo, getWaterMeterByNo, getElectricMeterDetail, getWaterMeterDetail, scanElectricMeter, scanWaterMeter } from '@/api/meter.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      meterType: 'ELECTRIC',
      meterNo: '',
      meterInfo: null,
      querying: false,
      notFound: false
    }
  },
  computed: {
    pageTitle() {
      return this.meterType === 'ELECTRIC' ? '我的电表' : '我的水表'
    }
  },
  onLoad(options) {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
    if (options.type && (options.type === 'ELECTRIC' || options.type === 'WATER')) {
      this.meterType = options.type
    }
    if (options.meterId) {
      this.queryMeterById(Number(options.meterId))
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    queryMeter() {
      if (!this.meterNo.trim()) return
      const no = this.meterNo.trim()
      this.querying = true
      this.meterInfo = null
      this.notFound = false
      const fn = this.meterType === 'ELECTRIC' ? getElectricMeterByNo : getWaterMeterByNo
      fn(no)
        .then((data) => {
          this.meterInfo = data
        })
        .catch(() => {
          this.notFound = true
        })
        .finally(() => {
          this.querying = false
        })
    },
    queryMeterById(meterId) {
      this.querying = true
      this.notFound = false
      const fn = this.meterType === 'ELECTRIC' ? getElectricMeterDetail : getWaterMeterDetail
      fn(meterId)
        .then((data) => {
          this.meterInfo = data
          this.meterNo = data.meterNo || ''
        })
        .catch(() => { this.notFound = true })
        .finally(() => { this.querying = false })
    },
    scanMeter() {
      uni.scanCode({
        onlyFromCamera: true,
        success: (res) => {
          const fn = this.meterType === 'ELECTRIC' ? scanElectricMeter : scanWaterMeter
          this.querying = true
          this.meterInfo = null
          this.notFound = false
          fn(res.result)
            .then((data) => {
              this.meterInfo = data
              this.meterNo = data.meterNo || ''
            })
            .catch(() => {
              this.notFound = true
            })
            .finally(() => {
              this.querying = false
            })
        },
        fail: () => {
          uni.showToast({ title: '扫码取消', icon: 'none' })
        }
      })
    },
    formatTime(time) {
      if (!time) return '--'
      return time.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style scoped>
.meter-page {
  min-height: 100vh;
  background: #fafafa;
  padding-bottom: 40rpx;
}

/* 导航栏 */
.nav-bar {
  width: 100%;
  background: #ff7a3d;
}
.nav-content {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}
.nav-back {
  width: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-arrow {
  width: 18rpx;
  height: 18rpx;
  border-left: 4rpx solid #fff;
  border-bottom: 4rpx solid #fff;
  transform: rotate(45deg);
}
.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
}
.nav-placeholder {
  width: 60rpx;
}

/* 查询输入区 */
.search-bar {
  background: #fff;
  padding: 20rpx 30rpx;
}
.search-row {
  display: flex;
  align-items: center;
  background: #f8f8f8;
  border-radius: 12rpx;
  padding: 0 20rpx;
}
.search-input {
  flex: 1;
  height: 80rpx;
  font-size: 28rpx;
  color: #333;
}
.scan-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.scan-icon-img {
  width: 40rpx;
  height: 40rpx;
}

/* 表计详情 */
.meter-detail {
  margin: 20rpx 30rpx 0;
}
.detail-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.detail-header {
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 20rpx;
}
.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}
.detail-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}
.detail-status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: #f0f0f0;
  color: #999;
}
.detail-status.running {
  background: #ecfdf5;
  color: #059669;
}
.detail-no {
  font-size: 24rpx;
  color: #999;
}
.detail-body {}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}
.detail-label {
  font-size: 26rpx;
  color: #999;
}
.detail-value {
  font-size: 26rpx;
  color: #333;
}
.detail-value.balance {
  color: #ff7a3d;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}
.empty-box {
  width: 160rpx;
  height: 160rpx;
  background: #f0f0f0;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.empty-box-icon {
  width: 80rpx;
  height: 80rpx;
  opacity: 0.35;
}
.empty-icon-img {
  width: 100rpx;
  height: 100rpx;
  opacity: 0.4;
}
.empty-text {
  font-size: 26rpx;
  color: #bbb;
}
.empty-sub-text {
  font-size: 24rpx;
  color: #ccc;
  margin-top: 8rpx;
}

/* 加载中 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}
.loading-text {
  font-size: 28rpx;
  color: #999;
}
</style>
