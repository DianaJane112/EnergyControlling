<template>
  <view class="invoice-page">
    <!-- 导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <view class="back-arrow"></view>
        </view>
        <text class="nav-title">我的票据</text>
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
      <view class="invoice-card" v-for="item in list" :key="item.id" @click="showDetail(item)">
        <view class="invoice-left">
          <text class="invoice-no">{{ item.invoiceNo || '--' }}</text>
          <text class="invoice-type">{{ typeName(item.invoiceType) }}</text>
          <text class="invoice-time">{{ formatTime(item.createTime) }}</text>
        </view>
        <view class="invoice-right">
          <text class="invoice-amount">{{ item.amount }} 元</text>
          <text class="invoice-status" :class="item.invoiceStatus === 'ISSUED' ? 'issued' : 'not-issued'">
            {{ item.invoiceStatus === 'ISSUED' ? '已开票' : '可开票' }}
          </text>
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
      <text class="empty-text">暂无票据记录</text>
    </view>

    <!-- 首次加载 -->
    <view class="empty-area" v-if="firstLoading">
      <text class="loading-text">加载中...</text>
    </view>
  </view>
</template>

<script>
import { getInvoiceList, getInvoiceDetail } from '@/api/invoice.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      activeStatus: 'all',
      statusTabs: [
        { key: 'all', label: '全部' },
        { key: 'NOT_ISSUED', label: '可开票' },
        { key: 'ISSUED', label: '已开票' }
      ],
      list: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      firstLoading: true,
      loadingMore: false,
      noMore: false
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
      const params = { pageNum: 1, pageSize: this.pageSize }
      if (this.activeStatus !== 'all') {
        params.invoiceStatus = this.activeStatus
      }
      getInvoiceList(params)
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
      if (this.activeStatus !== 'all') {
        params.invoiceStatus = this.activeStatus
      }
      getInvoiceList(params)
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
    showDetail(item) {
      uni.showLoading({ title: '加载中...' })
      getInvoiceDetail(item.id)
        .then((data) => {
          uni.hideLoading()
          const detail = data || item
          const items = [
            `票据号：${detail.invoiceNo || '--'}`,
            `类型：${this.typeName(detail.invoiceType)}`,
            `金额：${detail.amount != null ? detail.amount + ' 元' : '--'}`,
            `状态：${detail.invoiceStatus === 'ISSUED' ? '已开票' : '可开票'}`,
            `开票时间：${this.formatTime(detail.issueTime)}`,
            `备注：${detail.remark || '--'}`
          ]
          uni.showModal({
            title: '票据详情',
            content: items.join('\n'),
            showCancel: false,
            confirmText: '关闭'
          })
        })
        .catch(() => {
          uni.hideLoading()
        })
    },
    typeName(type) {
      const map = { ELECTRIC: '电费', WATER: '水费' }
      return map[type] || type || '--'
    },
    formatTime(time) {
      if (!time) return '--'
      return time.replace('T', ' ').substring(0, 16)
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.invoice-page {
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

/* 列表区域 */
.list-area {
  flex: 1;
  padding: 20rpx 30rpx;
}
.invoice-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.invoice-left {
  flex: 1;
  overflow: hidden;
}
.invoice-no {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-bottom: 6rpx;
}
.invoice-type {
  font-size: 30rpx;
  color: #1a1a1a;
  font-weight: 500;
  display: block;
  margin-bottom: 6rpx;
}
.invoice-time {
  font-size: 24rpx;
  color: #aaa;
}
.invoice-right {
  text-align: right;
  flex-shrink: 0;
  margin-left: 20rpx;
}
.invoice-amount {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
  display: block;
  margin-bottom: 6rpx;
}
.invoice-status {
  font-size: 24rpx;
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.invoice-status.issued {
  color: #059669;
  background: #ecfdf5;
}
.invoice-status.not-issued {
  color: #f59e0b;
  background: #fffbeb;
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
