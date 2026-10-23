<template>
  <view class="page">
    <!-- 导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <view class="back-arrow"></view>
        </view>
        <text class="nav-title">充值明细</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 状态标签栏 -->
    <view class="status-tabs">
      <view
        v-for="item in statusTabs"
        :key="item.key"
        class="status-tab"
        :class="{ active: activeStatus === item.key }"
        @click="switchStatus(item.key)"
      >
        <text class="status-text">{{ item.label }}</text>
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view class="list-area" scroll-y @scrolltolower="loadMore" v-if="list.length">
      <view class="card" v-for="item in list" :key="item.id" @click="goDetail(item)">
        <view class="card-left">
          <text class="card-name">{{ item.meterName }}</text>
          <text class="card-meta">{{ item.meterNo }} · {{ typeName(item.meterType) }}</text>
          <text class="card-no">{{ item.requestNo || '--' }}</text>
        </view>
        <view class="card-right">
          <text class="card-amount">{{ item.amount }} 元</text>
          <text class="card-status" :class="statusClass(item.auditStatus)">
            {{ statusLabel(item.auditStatus) }}
          </text>
          <text class="card-time">{{ formatTime(item.createTime) }}</text>
        </view>
      </view>

      <view class="load-more" v-if="loadingMore">
        <text class="load-text">加载中...</text>
      </view>
      <view class="load-more" v-if="noMore && list.length">
        <text class="load-text">没有更多了</text>
      </view>
    </scroll-view>

    <!-- 空数据 -->
    <view class="empty-area" v-else-if="!firstLoading">
      <view class="empty-box">
        <image class="empty-icon-img" src="/static/empty-icon.svg" mode="aspectFit" />
      </view>
      <text class="empty-text">暂无充值记录</text>
    </view>

    <!-- 首次加载 -->
    <view class="empty-area" v-if="firstLoading">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 详情弹层 -->
    <view class="detail-overlay" v-if="detailVisible" @click="closeDetail">
      <view class="detail-sheet" @click.stop>
        <view class="detail-header">
          <text class="detail-title">充值详情</text>
          <view class="detail-close" @click="closeDetail">
            <text class="close-icon">✕</text>
          </view>
        </view>

        <view class="detail-body" v-if="!detailLoading">
          <!-- 金额醒目 -->
          <view class="detail-amount-row">
            <text class="detail-amount">{{ detail.amount }}<text class="detail-unit"> 元</text></text>
            <text class="detail-status-badge" :class="statusClass(detail.auditStatus)">
              {{ statusLabel(detail.auditStatus) }}
            </text>
          </view>

          <!-- 信息行 -->
          <view class="detail-rows">
            <view class="d-row">
              <text class="d-label">申请编号</text>
              <text class="d-value">{{ detail.requestNo || '--' }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">表计名称</text>
              <text class="d-value">{{ detail.meterName || '--' }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">表号</text>
              <text class="d-value">{{ detail.meterNo || '--' }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">表计类型</text>
              <text class="d-value">{{ typeName(detail.meterType) }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">支付方式</text>
              <text class="d-value">{{ detail.payMethod || '--' }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">审核备注</text>
              <text class="d-value">{{ detail.auditRemark || '无' }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">提交时间</text>
              <text class="d-value">{{ formatTime(detail.createTime) }}</text>
            </view>
            <view class="d-row">
              <text class="d-label">审核时间</text>
              <text class="d-value">{{ formatTime(detail.auditTime) || '--' }}</text>
            </view>
            <view class="d-row" v-if="detail.relatedOrderNo">
              <text class="d-label">关联订单</text>
              <text class="d-value">{{ detail.relatedOrderNo }}</text>
            </view>
          </view>
        </view>

        <view class="detail-loading" v-else>
          <text class="loading-text">加载中...</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getRechargeHistory, getRechargeDetail } from '@/api/recharge.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      activeStatus: 'APPROVED',
      statusTabs: [
        { key: 'APPROVED', label: '充值成功' },
        { key: 'PENDING', label: '待审核' },
        { key: 'REJECTED', label: '未通过' }
      ],
      list: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      firstLoading: true,
      loadingMore: false,
      noMore: false,
      detailVisible: false,
      detailLoading: false,
      detail: {}
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  onShow() {
    this.fetchData()
  },
  methods: {
    switchStatus(key) {
      this.activeStatus = key
      this.fetchData()
    },
    fetchData() {
      this.firstLoading = true
      this.pageNum = 1
      this.noMore = false
      const params = { pageNum: 1, pageSize: this.pageSize, auditStatus: this.activeStatus }
      getRechargeHistory(params)
        .then((data) => {
          this.list = data.records || []
          this.total = data.total || 0
          if (this.list.length >= this.total) this.noMore = true
        })
        .catch(() => {})
        .finally(() => {
          this.firstLoading = false
        })
    },
    loadMore() {
      if (this.loadingMore || this.noMore) return
      this.loadingMore = true
      this.pageNum++
      const params = { pageNum: this.pageNum, pageSize: this.pageSize, auditStatus: this.activeStatus }
      getRechargeHistory(params)
        .then((data) => {
          const records = data.records || []
          this.list = this.list.concat(records)
          if (this.list.length >= (data.total || 0)) this.noMore = true
        })
        .catch(() => {})
        .finally(() => {
          this.loadingMore = false
        })
    },
    goDetail(item) {
      this.detailVisible = true
      this.detailLoading = true
      this.detail = {}
      getRechargeDetail(item.id)
        .then((data) => {
          this.detail = data || item
        })
        .catch(() => {
          this.detail = item
        })
        .finally(() => {
          this.detailLoading = false
        })
    },
    closeDetail() {
      this.detailVisible = false
    },
    statusLabel(status) {
      const map = { PENDING: '待审核', APPROVED: '充值成功', REJECTED: '未通过' }
      return map[status] || status
    },
    statusClass(status) {
      return (status || '').toLowerCase()
    },
    typeName(type) {
      const map = { ELECTRIC: '电表', WATER: '水表' }
      return map[type] || type || '--'
    },
    formatTime(time) {
      if (!time) return ''
      return time.replace('T', ' ').substring(0, 16)
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fafafa;
  display: flex;
  flex-direction: column;
}
.nav-bar { width: 100%; background: #ff7a3d; }
.nav-content {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}
.nav-back { width: 60rpx; display: flex; align-items: center; justify-content: center; }
.back-arrow { width: 18rpx; height: 18rpx; border-left: 4rpx solid #fff; border-bottom: 4rpx solid #fff; transform: rotate(45deg); }
.nav-title { font-size: 34rpx; font-weight: 600; color: #fff; }
.nav-placeholder { width: 60rpx; }

.status-tabs {
  display: flex;
  background: #fff;
  padding: 0 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.status-tab {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  position: relative;
}
.status-text { font-size: 28rpx; color: #666; }
.status-tab.active .status-text { color: #ff7a3d; font-weight: 600; }
.status-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  background: #ff7a3d;
  border-radius: 3rpx;
}

.list-area { flex: 1; padding: 20rpx 30rpx; }
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.card-left { flex: 1; overflow: hidden; }
.card-name { font-size: 30rpx; color: #1a1a1a; font-weight: 500; display: block; margin-bottom: 6rpx; }
.card-meta { font-size: 24rpx; color: #aaa; }
.card-no { font-size: 22rpx; color: #bbb; display: block; margin-top: 4rpx; }
.card-right { text-align: right; flex-shrink: 0; margin-left: 20rpx; }
.card-amount { font-size: 32rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 6rpx; }
.card-status {
  font-size: 24rpx;
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  margin-bottom: 6rpx;
}
.card-status.pending { color: #f59e0b; background: #fffbeb; }
.card-status.approved { color: #059669; background: #ecfdf5; }
.card-status.rejected { color: #e74c3c; background: #fff5f5; }
.card-time { font-size: 22rpx; color: #bbb; display: block; }
.load-more { padding: 30rpx 0; text-align: center; }
.load-text { font-size: 24rpx; color: #bbb; }

.empty-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 100rpx;
}
.empty-box {
  width: 160rpx; height: 160rpx;
  background: #f0f0f0;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.empty-icon-img { width: 100rpx; height: 100rpx; opacity: 0.4; }
.empty-text { font-size: 26rpx; color: #bbb; }
.loading-text { font-size: 28rpx; color: #999; }

/* 详情弹层 */
.detail-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.detail-sheet {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideUp 0.25s ease;
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 30rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.detail-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1a1a1a;
}
.detail-close {
  width: 52rpx; height: 52rpx;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-icon {
  font-size: 24rpx;
  color: #999;
}
.detail-body {
  padding: 0 30rpx 50rpx;
}
.detail-loading {
  padding: 80rpx 0;
  text-align: center;
}

/* 金额区 */
.detail-amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0 24rpx;
}
.detail-amount {
  font-size: 56rpx;
  font-weight: 700;
  color: #1a1a1a;
}
.detail-unit {
  font-size: 30rpx;
  font-weight: 400;
  color: #999;
}
.detail-status-badge {
  font-size: 26rpx;
  padding: 8rpx 24rpx;
  border-radius: 20rpx;
}
.detail-status-badge.pending { color: #f59e0b; background: #fffbeb; }
.detail-status-badge.approved { color: #059669; background: #ecfdf5; }
.detail-status-badge.rejected { color: #e74c3c; background: #fff5f5; }

/* 信息行 */
.detail-rows {
  border-top: 1rpx solid #f5f5f5;
  padding-top: 16rpx;
}
.d-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #fafafa;
}
.d-label {
  font-size: 28rpx;
  color: #999;
  flex-shrink: 0;
  width: 140rpx;
}
.d-value {
  font-size: 28rpx;
  color: #333;
  text-align: right;
  flex: 1;
}
</style>
