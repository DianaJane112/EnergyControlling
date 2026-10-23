<template>
  <view class="container">
    <!-- Balance card -->
    <view class="balance-card">
      <view class="card-bg"></view>
      <view class="card-content">
        <text class="balance-label">账户余额（元）</text>
        <view class="balance-value">{{ info.balance }}</view>
        <view class="account-info">
          <view class="info-item">
            <text class="info-label">户名</text>
            <text class="info-text">{{ info.customerName || '--' }}</text>
          </view>
        </view>
        <view class="recharge-btn" @click="goRecharge">立即充值</view>
      </view>
    </view>

    <!-- Meter info -->
    <view class="detail-card" v-if="info.meterNo">
      <view class="detail-title">账户信息</view>
      <view class="detail-row">
        <text class="detail-label">表计编号</text>
        <text class="detail-value">{{ info.meterNo }}</text>
      </view>
      <view class="detail-row">
        <text class="detail-label">表计名称</text>
        <text class="detail-value">{{ info.meterName || '--' }}</text>
      </view>
      <view class="detail-row">
        <text class="detail-label">安装地址</text>
        <text class="detail-value">{{ info.address || '--' }}</text>
      </view>
    </view>

    <!-- Recent transactions -->
    <view class="tx-section">
      <view class="section-title">最近充值记录</view>
      <view class="tx-card" v-for="tx in transactions" :key="tx.id" @click="goDetail(tx)">
        <view class="tx-left">
          <view class="tx-icon" :class="statusIcon(tx.auditStatus)">
            {{ statusSymbol(tx.auditStatus) }}
          </view>
          <view class="tx-info">
            <text class="tx-desc">{{ tx.meterName }} · {{ tx.amount }}元</text>
            <text class="tx-time">{{ formatTime(tx.createTime) }}</text>
          </view>
        </view>
        <view class="tx-right">
          <view class="tx-status" :class="statusClass(tx.auditStatus)">
            {{ statusLabel(tx.auditStatus) }}
          </view>
        </view>
      </view>
      <view class="tx-empty" v-if="!transactions.length && !firstLoading">
        <text class="empty-text">暂无充值记录</text>
      </view>
      <view class="tx-loading" v-if="firstLoading">
        <text class="loading-text">加载中...</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getRechargeHistory, getRechargeDetail } from '@/api/recharge.js'

export default {
  data() {
    return {
      info: {
        balance: '--',
        customerName: '',
        meterNo: '',
        meterName: '',
        address: '',
        meterId: null
      },
      transactions: [],
      firstLoading: true
    }
  },
  onLoad(options) {
    // 如果从账单列表点进来，直接展示详情
    if (options.id) {
      this.loadDetail(options.id)
      return
    }
    // 尝试从 Storage 读用户信息
    const userInfo = uni.getStorageSync('userInfo')
    const customerInfo = uni.getStorageSync('customerInfo')
    if (customerInfo) {
      this.info = { ...this.info, ...customerInfo }
    }
    if (userInfo) {
      this.info.customerName = this.info.customerName || userInfo.nickname || ''
    }
    this.loadHistory()
  },
  onShow() {
    if (!this.$mp.page.$getAppWebview) {
      this.loadHistory()
    }
  },
  methods: {
    loadHistory() {
      this.firstLoading = true
      getRechargeHistory({ pageNum: 1, pageSize: 5 })
        .then((data) => {
          this.transactions = data.records || []
        })
        .catch(() => {})
        .finally(() => {
          this.firstLoading = false
        })
    },
    loadDetail(id) {
      uni.showLoading({ title: '加载中...' })
      getRechargeDetail(parseInt(id))
        .then((data) => {
          uni.hideLoading()
          if (data.meterId) this.info.meterId = data.meterId
          if (data.meterNo) this.info.meterNo = data.meterNo
          if (data.meterName) this.info.meterName = data.meterName
          const items = [
            `申请编号：${data.requestNo || '--'}`,
            `表计名称：${data.meterName || '--'}`,
            `表号：${data.meterNo || '--'}`,
            `金额：${data.amount != null ? data.amount + ' 元' : '--'}`,
            `支付方式：${data.payMethod || '--'}`,
            `审核状态：${this.statusLabel(data.auditStatus)}`,
            `审核备注：${data.auditRemark || '无'}`,
            `提交时间：${this.formatTime(data.createTime)}`,
            `审核时间：${this.formatTime(data.auditTime) || '--'}`
          ]
          uni.showModal({
            title: '充值详情',
            content: items.join('\n'),
            showCancel: false,
            confirmText: '关闭'
          })
        })
        .catch(() => { uni.hideLoading() })
    },
    goRecharge() {
      uni.navigateTo({ url: '/pages-sub/recharge/index?type=ELECTRIC' })
    },
    goDetail(tx) {
      this.loadDetail(tx.id)
    },
    statusLabel(status) {
      const map = { PENDING: '待审核', APPROVED: '充值成功', REJECTED: '未通过' }
      return map[status] || '未知'
    },
    statusClass(status) {
      return (status || '').toLowerCase()
    },
    statusIcon(status) {
      return (status || '').toLowerCase()
    },
    statusSymbol(status) {
      const map = { PENDING: '○', APPROVED: '●', REJECTED: '✕' }
      return map[status] || '?'
    },
    formatTime(time) {
      if (!time) return ''
      return time.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style scoped>
.container { min-height: 100vh; background: #fafafa; padding-bottom: 40rpx; }

/* Balance card */
.balance-card {
  margin: 30rpx; border-radius: 20rpx; overflow: hidden;
  position: relative; background: #2563EB;
  box-shadow: 0 8rpx 30rpx rgba(37,99,235,0.3);
}
.card-content { padding: 40rpx; position: relative; z-index: 1; }
.balance-label { font-size: 26rpx; color: rgba(255,255,255,0.8); }
.balance-value {
  font-size: 72rpx; font-weight: 700; color: #fff; margin: 12rpx 0 24rpx;
  font-family: "Helvetica Neue", sans-serif;
}
.account-info { display: flex; gap: 40rpx; margin-bottom: 24rpx; }
.info-label { font-size: 22rpx; color: rgba(255,255,255,0.7); display: block; margin-bottom: 4rpx; }
.info-text { font-size: 24rpx; color: rgba(255,255,255,0.95); }
.recharge-btn {
  display: inline-block; background: #fff; color: #2563EB;
  font-size: 28rpx; font-weight: 600; padding: 16rpx 48rpx;
  border-radius: 40rpx; text-align: center;
}

/* Account details */
.detail-card {
  background: #fff; margin: 0 30rpx 20rpx; border-radius: 16rpx;
  padding: 30rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.detail-title { font-size: 28rpx; font-weight: 600; color: #1a1a1a; margin-bottom: 20rpx; }
.detail-row { display: flex; justify-content: space-between; padding: 12rpx 0; }
.detail-label { font-size: 26rpx; color: #999; }
.detail-value { font-size: 26rpx; color: #333; }

/* Transactions */
.tx-section { margin: 0 30rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; margin-bottom: 16rpx; }
.tx-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx;
  margin-bottom: 12rpx; display: flex; justify-content: space-between;
  align-items: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.tx-left { display: flex; align-items: center; gap: 16rpx; flex: 1; overflow: hidden; }
.tx-icon {
  width: 56rpx; height: 56rpx; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 28rpx; font-weight: 700; flex-shrink: 0;
}
.tx-icon.pending { background: #fffbeb; color: #f59e0b; }
.tx-icon.approved { background: #ecfdf5; color: #059669; }
.tx-icon.rejected { background: #fff5f5; color: #e74c3c; }
.tx-info { overflow: hidden; }
.tx-desc { font-size: 26rpx; color: #1a1a1a; display: block; }
.tx-time { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; }
.tx-right { text-align: right; flex-shrink: 0; }
.tx-status { font-size: 24rpx; padding: 4rpx 14rpx; border-radius: 8rpx; }
.tx-status.pending { color: #f59e0b; background: #fffbeb; }
.tx-status.approved { color: #059669; background: #ecfdf5; }
.tx-status.rejected { color: #e74c3c; background: #fff5f5; }

.tx-empty, .tx-loading { text-align: center; padding: 60rpx 0; }
.empty-text { font-size: 26rpx; color: #bbb; }
.loading-text { font-size: 28rpx; color: #999; }
</style>
