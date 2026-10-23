<template>
  <view class="bill-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">我的账单</text>
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

    <!-- 类型筛选（圆角框） -->
    <view class="type-row">
      <view
        class="type-pill"
        :class="{ active: activeType === 'all' }"
        @click="switchType('all')"
      >
        <text class="type-pill-text">全部</text>
      </view>
      <view
        class="type-pill"
        :class="{ active: activeType === 'ELECTRIC' }"
        @click="switchType('ELECTRIC')"
      >
        <text class="type-pill-text">电表</text>
      </view>
      <view
        class="type-pill"
        :class="{ active: activeType === 'WATER' }"
        @click="switchType('WATER')"
      >
        <text class="type-pill-text">水表</text>
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view class="list-area" scroll-y @scrolltolower="loadMore" v-if="filteredList.length">
      <view class="bill-card" v-for="item in filteredList" :key="item.id" @click="goDetail(item)">
        <view class="bill-left">
          <text class="bill-no">{{ item.requestNo || '--' }}</text>
          <text class="bill-name">{{ item.meterName }}</text>
          <text class="bill-meta">{{ item.meterNo }} · {{ typeName(item.meterType) }}</text>
        </view>
        <view class="bill-right">
          <text class="bill-amount">{{ item.amount }} 元</text>
          <text class="bill-status" :class="statusClass(item.auditStatus)">
            {{ statusLabel(item.auditStatus) }}
          </text>
          <text class="bill-time">{{ formatTime(item.createTime) }}</text>
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
      <text class="empty-text">暂无账单记录</text>
    </view>

    <!-- 首次加载 -->
    <view class="empty-area" v-if="firstLoading">
      <text class="loading-text">加载中...</text>
    </view>
  </view>
</template>

<script>
import { getRechargeHistory } from '@/api/recharge.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      activeStatus: '',
      statusTabs: [
        { key: '', label: '待交付' },
        { key: 'PENDING', label: '待审核' },
        { key: 'APPROVED', label: '已支付' }
      ],
      activeType: 'all',
      list: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      firstLoading: true,
      loadingMore: false,
      noMore: false
    }
  },
  computed: {
    filteredList() {
      if (this.activeType === 'all') return this.list
      return this.list.filter(item => item.meterType === this.activeType)
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
    switchType(key) {
      this.activeType = key
    },
    fetchData() {
      this.firstLoading = true
      this.pageNum = 1
      this.noMore = false
      const params = { pageNum: 1, pageSize: this.pageSize }
      if (this.activeStatus) {
        params.auditStatus = this.activeStatus
      }
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
      const params = { pageNum: this.pageNum, pageSize: this.pageSize }
      if (this.activeStatus) {
        params.auditStatus = this.activeStatus
      }
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
      uni.navigateTo({ url: `/pages-sub/balance/index?id=${item.id}` })
    },
    statusLabel(status) {
      const map = { PENDING: '待审核', APPROVED: '已支付', REJECTED: '未通过' }
      return map[status] || status
    },
    statusClass(status) {
      return (status || '').toLowerCase()
    },
    typeName(type) {
      const map = { ELECTRIC: '电表', WATER: '水表' }
      return map[type] || type
    },
    formatTime(time) {
      if (!time) return ''
      return time.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style scoped>
.bill-page {
  min-height: 100vh;
  background: #fafafa;
  display: flex;
  flex-direction: column;
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
  justify-content: center;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2rpx;
}

/* 状态标签栏 */
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
.status-text {
  font-size: 28rpx;
  color: #666;
  font-weight: 400;
}
.status-tab.active .status-text {
  color: #ff7a3d;
  font-weight: 600;
}
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

/* 类型筛选圆角框 */
.type-row {
  display: flex;
  padding: 20rpx 30rpx;
  gap: 20rpx;
  background: #fff;
  margin-bottom: 16rpx;
}
.type-pill {
  padding: 16rpx 36rpx;
  border-radius: 32rpx;
  background: #fafafa;
}
.type-pill.active {
  background: #fff5f0;
}
.type-pill-text {
  font-size: 26rpx;
  color: #666;
}
.type-pill.active .type-pill-text {
  color: #ff7a3d;
  font-weight: 600;
}

/* 列表区域 */
.list-area {
  flex: 1;
  padding: 0 30rpx;
}
.bill-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.bill-left {
  flex: 1;
  overflow: hidden;
}
.bill-no {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-bottom: 6rpx;
}
.bill-name {
  font-size: 30rpx;
  color: #1a1a1a;
  font-weight: 500;
  display: block;
  margin-bottom: 6rpx;
}
.bill-meta {
  font-size: 24rpx;
  color: #aaa;
}
.bill-right {
  text-align: right;
  flex-shrink: 0;
  margin-left: 20rpx;
}
.bill-amount {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
  display: block;
  margin-bottom: 6rpx;
}
.bill-status {
  font-size: 24rpx;
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  margin-bottom: 6rpx;
}
.bill-status.pending {
  color: #f59e0b;
  background: #fffbeb;
}
.bill-status.approved {
  color: #059669;
  background: #ecfdf5;
}
.bill-status.rejected {
  color: #e74c3c;
  background: #fff5f5;
}
.bill-time {
  font-size: 22rpx;
  color: #bbb;
  display: block;
}
.load-more {
  padding: 30rpx 0;
  text-align: center;
}
.load-text {
  font-size: 24rpx;
  color: #bbb;
}

/* 空数据 */
.empty-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 100rpx;
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
.empty-icon {
  font-size: 72rpx;
  opacity: 0.5;
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
.loading-text {
  font-size: 28rpx;
  color: #999;
}
</style>
