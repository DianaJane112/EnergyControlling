<template>
  <view class="recharge-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <view class="back-arrow"></view>
        </view>
        <text class="nav-title">充值缴费</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 表计类型切换 -->
    <view class="type-tabs">
      <view
        class="type-tab"
        :class="{ active: meterType === 'ELECTRIC' }"
        @click="switchType('ELECTRIC')"
      >
        <view class="type-text"><image class="type-icon" src="/static/lightning-icon.svg" mode="aspectFit" /> 电表</view>
      </view>
      <view
        class="type-tab"
        :class="{ active: meterType === 'WATER' }"
        @click="switchType('WATER')"
      >
        <view class="type-text"><image class="type-icon" src="/static/waterdrop-icon.svg" mode="aspectFit" /> 水表</view>
      </view>
    </view>

    <!-- 查询表计 -->
    <view class="search-card">
      <view class="search-row">
        <input
          class="search-input"
          v-model="meterNo"
          :placeholder="meterType === 'ELECTRIC' ? '输入电表表号，如 010000000001' : '输入水表表号，如 010000000001'"
          placeholder-style="color:#c0c0c0;font-size:28rpx;"
        />
        <view class="scan-btn" @click="scanMeter">
          <image class="scan-icon-img" src="/static/scan-icon.svg" mode="aspectFit" />
        </view>
      </view>
      <view class="search-action">
        <button class="query-btn" @click="queryMeter" :disabled="!meterNo.trim()">查询表计</button>
      </view>
    </view>

    <!-- 表计信息 -->
    <view class="meter-card" v-if="meterInfo">
      <view class="meter-card-header">
        <text class="meter-card-title">{{ meterInfo.meterName || '--' }}</text>
        <text class="meter-status" :class="(meterInfo.status || '').toLowerCase() === 'running' ? 'running' : ''">
          {{ meterInfo.status === 'RUNNING' ? '运行中' : meterInfo.status || '--' }}
        </text>
      </view>
      <view class="meter-card-body">
        <view class="meter-info-row">
          <text class="meter-label">表号</text>
          <text class="meter-value">{{ meterInfo.meterNo || '--' }}</text>
        </view>
        <view class="meter-info-row">
          <text class="meter-label">当前余额</text>
          <text class="meter-value balance">{{ meterInfo.balance != null ? meterInfo.balance + ' 元' : '--' }}</text>
        </view>
        <view class="meter-info-row">
          <text class="meter-label">安装地址</text>
          <text class="meter-value">{{ meterInfo.address || '--' }}</text>
        </view>
      </view>
    </view>

    <!-- 充值表单 -->
    <view class="recharge-card" v-if="meterInfo">
      <view class="recharge-title">充值金额</view>
      <view class="amount-input-row">
        <text class="currency">¥</text>
        <input
          class="amount-input"
          v-model="amount"
          type="digit"
          placeholder="0.00"
          placeholder-style="color:#d0d0d0;font-size:56rpx;"
        />
      </view>
      <view class="quick-amounts">
        <view
          v-for="a in quickAmounts"
          :key="a"
          class="quick-item"
          :class="{ selected: amount === String(a) }"
          @click="amount = String(a)"
        >
          <text>{{ a }}元</text>
        </view>
      </view>
      <button class="submit-btn" :disabled="!canSubmit" :loading="submitting" @click="submitRecharge">
        {{ submitting ? '提交中...' : '确认充值' }}
      </button>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="!meterInfo && !querying">
      <image class="empty-icon-img" src="/static/empty-icon.svg" mode="aspectFit" />
      <text class="empty-text">输入表号或扫码查询表计后充值</text>
    </view>

    <!-- 查询中 -->
    <view class="loading-state" v-if="querying">
      <text class="loading-text">查询中...</text>
    </view>
  </view>
</template>

<script>
import { getElectricMeterByNo, getWaterMeterByNo, scanElectricMeter, scanWaterMeter } from '@/api/meter.js'
import { submitRecharge as submitRechargeApi } from '@/api/recharge.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      meterType: 'ELECTRIC',
      meterNo: '',
      amount: '',
      meterInfo: null,
      querying: false,
      submitting: false,
      quickAmounts: [50, 100, 200, 500]
    }
  },
  onLoad(options) {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
    if (options.type && (options.type === 'ELECTRIC' || options.type === 'WATER')) {
      this.meterType = options.type
    }
  },
  computed: {
    canSubmit() {
      const val = parseFloat(this.amount)
      return !isNaN(val) && val > 0 && this.meterInfo
    }
  },
  methods: {
    switchType(type) {
      if (this.meterType === type) return
      this.meterType = type
      this.meterNo = ''
      this.amount = ''
      this.meterInfo = null
    },
    goBack() {
      uni.navigateBack()
    },
    queryMeter() {
      if (!this.meterNo.trim()) return
      const no = this.meterNo.trim()
      this.querying = true
      this.meterInfo = null
      this.amount = ''
      const fn = this.meterType === 'ELECTRIC' ? getElectricMeterByNo : getWaterMeterByNo
      fn(no)
        .then((data) => {
          this.meterInfo = data
        })
        .catch(() => {})
        .finally(() => {
          this.querying = false
        })
    },
    scanMeter() {
      uni.scanCode({
        onlyFromCamera: true,
        success: (res) => {
          const fn = this.meterType === 'ELECTRIC' ? scanElectricMeter : scanWaterMeter
          this.querying = true
          this.meterInfo = null
          this.amount = ''
          fn(res.result)
            .then((data) => {
              this.meterInfo = data
              this.meterNo = data.meterNo || ''
            })
            .catch(() => {})
            .finally(() => {
              this.querying = false
            })
        },
        fail: () => {
          uni.showToast({ title: '扫码取消', icon: 'none' })
        }
      })
    },
    submitRecharge() {
      if (!this.canSubmit || !this.meterInfo) return
      const amount = parseFloat(this.amount)
      if (isNaN(amount) || amount <= 0) {
        uni.showToast({ title: '请输入有效金额', icon: 'none' })
        return
      }
      this.submitting = true
      submitRechargeApi(this.meterInfo.meterId, amount)
        .then((data) => {
          uni.showToast({ title: data.message || '提交成功', icon: 'success' })
          this.amount = ''
        })
        .catch(() => {})
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style scoped>
.recharge-page {
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

/* 类型切换 */
.type-tabs {
  display: flex;
  background: #fff;
  padding: 20rpx 30rpx;
  gap: 20rpx;
}
.type-tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: 12rpx;
  background: #fafafa;
}
.type-tab.active {
  background: #fff5f0;
}
.type-text {
  font-size: 28rpx;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
}
.type-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 6rpx;
}
.type-tab.active .type-text {
  color: #ff7a3d;
  font-weight: 600;
}

/* 查询区 */
.search-card {
  margin: 20rpx 30rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.search-row {
  display: flex;
  align-items: center;
}
.search-input {
  flex: 1;
  height: 88rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
}
.scan-btn {
  width: 88rpx;
  height: 88rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 16rpx;
}
.scan-icon-img {
  width: 40rpx;
  height: 40rpx;
}
.search-action {
  margin-top: 20rpx;
}
.query-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #ff7a3d;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
  text-align: center;
}
.query-btn::after { border: none; }
.query-btn[disabled] {
  background: #f0d0bf;
  color: #fff;
}

/* 表计信息 */
.meter-card {
  margin: 20rpx 30rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.meter-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.meter-card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}
.meter-status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: #f0f0f0;
  color: #999;
}
.meter-status.running {
  background: #ecfdf5;
  color: #059669;
}
.meter-card-body {
  border-top: 1rpx solid #f0f0f0;
  padding-top: 20rpx;
}
.meter-info-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}
.meter-label {
  font-size: 26rpx;
  color: #999;
}
.meter-value {
  font-size: 26rpx;
  color: #333;
}
.meter-value.balance {
  color: #ff7a3d;
  font-weight: 600;
}

/* 充值表单 */
.recharge-card {
  margin: 20rpx 30rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.recharge-title {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 16rpx;
}
.amount-input-row {
  display: flex;
  align-items: baseline;
  padding: 16rpx 0 24rpx;
  border-bottom: 3rpx solid #ff7a3d;
  margin-bottom: 24rpx;
}
.currency {
  font-size: 48rpx;
  font-weight: 600;
  color: #1a1a1a;
  margin-right: 12rpx;
}
.amount-input {
  flex: 1;
  font-size: 56rpx;
  font-weight: 700;
  color: #1a1a1a;
  height: 80rpx;
}
.quick-amounts {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}
.quick-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 10rpx;
  background: #fafafa;
  font-size: 26rpx;
  color: #666;
}
.quick-item.selected {
  background: #fff5f0;
  color: #ff7a3d;
  font-weight: 600;
}
.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #ff7a3d;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  text-align: center;
}
.submit-btn::after { border: none; }
.submit-btn[disabled] {
  background: #f0d0bf;
  color: #fff;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
  opacity: 0.5;
}
.empty-icon-img {
  width: 100rpx;
  height: 100rpx;
  opacity: 0.4;
  margin-bottom: 24rpx;
}
.empty-text {
  font-size: 26rpx;
  color: #bbb;
}
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 160rpx;
}
.loading-text {
  font-size: 28rpx;
  color: #999;
}
</style>
