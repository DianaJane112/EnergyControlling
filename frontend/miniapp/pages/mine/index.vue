<template>
  <view class="mine-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">个人中心</text>
      </view>
    </view>

    <!-- 用户信息区 -->
    <view class="user-card">
      <view class="user-header">
        <view class="avatar">
          <text class="avatar-text">微</text>
        </view>
        <view class="user-info">
          <text class="user-name">{{ userInfo ? userInfo.nickname || '微信用户' : '微信用户' }}</text>
          <text class="user-id">ID: {{ customerCode || '未绑定' }}</text>
        </view>
      </view>
      <view class="user-stats">
        <view class="stat-item">
          <text class="stat-value">{{ balance }}<text class="stat-unit"> 元</text></text>
          <text class="stat-label">当前余额</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ estimatedDays }}<text class="stat-unit"> 天</text></text>
          <text class="stat-label">预计可用</text>
        </view>
      </view>
    </view>

    <!-- 用电数据 -->
    <view class="usage-card">
      <view class="usage-item">
        <view class="usage-info">
          <text class="usage-value">{{ todayUsage }}</text>
          <text class="usage-unit"> kWh</text>
        </view>
        <text class="usage-label">今日用电量</text>
      </view>
      <view class="usage-divider"></view>
      <view class="usage-item">
        <view class="usage-info">
          <text class="usage-value">{{ monthUsage }}</text>
          <text class="usage-unit"> kWh</text>
        </view>
        <text class="usage-label">30日用电量</text>
      </view>
    </view>

    <!-- 功能入口区 -->
    <view class="func-section">
      <!-- 横向三个功能 -->
      <view class="func-row">
        <view class="func-item" @click="goMeter">
          <view class="func-icon orange-bg">
            <image class="icon-img" src="/static/electric-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-name">电表管理</text>
        </view>
        <view class="func-item" @click="goRecharge">
          <view class="func-icon recharge-icon">
            <image class="icon-img" src="/static/recharge-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-name">充值缴费</text>
        </view>
        <view class="func-item" @click="goWaterMeter">
          <view class="func-icon blue-bg">
            <image class="icon-img" src="/static/water-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-name">水表管理</text>
        </view>
      </view>

      <!-- 开票信息 -->
      <view class="func-single" @click="goInvoice">
        <view class="func-single-left">
          <view class="func-icon-small yellow-bg">
            <image class="icon-img-small" src="/static/invoice-icon.svg" mode="aspectFit" />
          </view>
          <text class="func-single-name">开票信息</text>
        </view>
        <text class="func-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

  </view>
</template>

<script>
import { getElectricMeterByNo, getWaterMeterByNo, scanElectricMeter, scanWaterMeter } from '@/api/meter.js'
import { getInvoiceList, getInvoiceDetail } from '@/api/invoice.js'
import { getUserInfo } from '@/api/login.js'
import { submitRecharge } from '@/api/recharge.js'
import { getDashboard } from '@/api/dashboard.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      userInfo: null,
      customerCode: '',
      balance: 0,
      estimatedDays: 0,
      todayUsage: 0,
      monthUsage: 0,
      invoiceList: []
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  onShow() {
    this.loadUserInfo()
    this.loadDashboard()
  },
  methods: {
    loadUserInfo() {
      getUserInfo()
        .then((data) => {
          if (data.userInfo) {
            this.userInfo = data.userInfo
            uni.setStorageSync('userInfo', data.userInfo)
          }
          if (data.customerInfo) {
            const c = data.customerInfo
            this.customerCode = c.customerCode || ''
            this.balance = c.balance || 0
            uni.setStorageSync('customerInfo', c)
          }
        })
        .catch(() => {})
    },
    loadDashboard() {
      getDashboard()
        .then((data) => {
          this.balance = data.electricBalance || 0
          this.estimatedDays = data.electricEstimatedDays || 0
          this.todayUsage = data.todayElectricUsage || 0
          this.monthUsage = data.last30DaysElectricUsage || 0
        })
        .catch(() => {})
    },

    // ----- 电表 -----
    goMeter() {
      uni.showActionSheet({
        itemList: ['输入表号查询', '扫码查询'],
        success: (res) => {
          if (res.tapIndex === 0) this.inputMeter('ELECTRIC')
          else this.scanMeter('ELECTRIC')
        }
      })
    },
    // ----- 水表 -----
    goWaterMeter() {
      uni.showActionSheet({
        itemList: ['输入表号查询', '扫码查询'],
        success: (res) => {
          if (res.tapIndex === 0) this.inputMeter('WATER')
          else this.scanMeter('WATER')
        }
      })
    },

    inputMeter(type) {
      const title = type === 'ELECTRIC' ? '查询电表' : '查询水表'
      uni.showModal({
        title,
        editable: true,
        placeholderText: `请输入${type === 'ELECTRIC' ? '电表' : '水表'}表号`,
        success: (res) => {
          if (res.confirm && res.content) {
            const fn = type === 'ELECTRIC' ? getElectricMeterByNo : getWaterMeterByNo
            fn(res.content)
              .then((data) => { this.showMeterDetail(data, type === 'ELECTRIC' ? '电表' : '水表') })
              .catch(() => {})
          }
        }
      })
    },

    scanMeter(type) {
      uni.scanCode({
        onlyFromCamera: true,
        success: (res) => {
          const fn = type === 'ELECTRIC' ? scanElectricMeter : scanWaterMeter
          fn(res.result)
            .then((data) => { this.showMeterDetail(data, type === 'ELECTRIC' ? '电表' : '水表') })
            .catch(() => {})
        },
        fail: () => {
          uni.showToast({ title: '扫码取消', icon: 'none' })
        }
      })
    },

    showMeterDetail(data, type) {
      const items = [
        `表号：${data.meterNo || '--'}`,
        `名称：${data.meterName || '--'}`,
        `余额：${data.balance != null ? data.balance + ' 元' : '--'}`,
        `用量：${data.totalUsage != null ? data.totalUsage : '--'}`,
        `状态：${data.status || '--'}`,
        `地址：${data.address || '--'}`
      ]
      const meterId = data.meterId
      uni.showModal({
        title: `${type}详情`,
        content: items.join('\n'),
        confirmText: '充值',
        cancelText: '关闭',
        success: (res) => {
          if (res.confirm && meterId) {
            this.doRecharge(meterId, data.meterName || type)
          }
        }
      })
    },

    doRecharge(meterId, meterName) {
      uni.showModal({
        title: '充值缴费',
        editable: true,
        placeholderText: '请输入充值金额',
        success: (res) => {
          if (res.confirm && res.content) {
            const amount = parseFloat(res.content)
            if (isNaN(amount) || amount <= 0) {
              uni.showToast({ title: '请输入有效金额', icon: 'none' })
              this.doRecharge(meterId, meterName)
              return
            }
            uni.showLoading({ title: '提交中...' })
            submitRecharge(meterId, amount)
              .then((data) => {
                uni.hideLoading()
                uni.showToast({ title: data.message || '提交成功', icon: 'success' })
              })
              .catch(() => { uni.hideLoading() })
          }
        }
      })
    },

    goRecharge() {
      uni.navigateTo({ url: '/pages-sub/recharge/index?type=ELECTRIC' })
    },

    goInvoice() {
      uni.showLoading({ title: '加载中...' })
      getInvoiceList({ pageNum: 1, pageSize: 10 })
        .then((data) => {
          uni.hideLoading()
          const records = data.records || []
          if (!records.length) {
            uni.showToast({ title: '暂无票据记录', icon: 'none' })
            return
          }
          this.invoiceList = records
          const items = records.map((item, i) =>
            `${i + 1}. ${item.invoiceType || '--'} · ${item.amount}元 [${item.invoiceStatus === 'ISSUED' ? '已开票' : '未开票'}]`
          )
          uni.showActionSheet({
            itemList: items,
            success: (res) => {
              const item = this.invoiceList[res.tapIndex]
              if (item) this.showInvoiceDetail(item.id)
            }
          })
        })
        .catch(() => { uni.hideLoading() })
    },

    showInvoiceDetail(id) {
      uni.showLoading({ title: '加载中...' })
      getInvoiceDetail(id)
        .then((data) => {
          uni.hideLoading()
          const items = [
            `票据号：${data.invoiceNo || '--'}`,
            `类型：${data.invoiceType || '--'}`,
            `金额：${data.amount != null ? data.amount + ' 元' : '--'}`,
            `状态：${data.invoiceStatus === 'ISSUED' ? '已开票' : '未开票'}`,
            `备注：${data.remark || '--'}`
          ]
          uni.showModal({
            title: '票据详情',
            content: items.join('\n'),
            showCancel: false,
            confirmText: '关闭'
          })
        })
        .catch(() => { uni.hideLoading() })
    },

    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/login/index' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #fafafa;
  padding-bottom: 40rpx;
}

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
  background: url(/static/UserBg.jpg) center/cover no-repeat;
  border-radius: 0;
  padding: 40rpx 36rpx;
}

.user-header {
  display: flex;
  align-items: center;
  margin-bottom: 36rpx;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.4);
}

.avatar-text {
  font-size: 40rpx;
  font-weight: 600;
  color: #fff;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
  margin-bottom: 6rpx;
}

.user-id {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

.user-stats {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16rpx;
  padding: 24rpx 0;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6rpx;
}

.stat-unit {
  font-size: 24rpx;
  font-weight: 400;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.75);
}

.stat-divider {
  width: 2rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.25);
}

/* 用电数据 */
.usage-card {
  margin: 20rpx 0 0;
  background: #fff;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.usage-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.usage-divider {
  width: 2rpx;
  height: 56rpx;
  background: #eee;
}

.usage-info {
  margin-bottom: 10rpx;
}

.usage-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.usage-unit {
  font-size: 26rpx;
  font-weight: 400;
  color: #999;
}

.usage-label {
  font-size: 24rpx;
  color: #999;
}

/* 功能入口区 */
.func-section {
  margin: 20rpx 0 0;
}

.func-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.func-item {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.func-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.icon-img {
  width: 56rpx;
  height: 56rpx;
}
.icon-img-small {
  width: 40rpx;
  height: 40rpx;
}

.orange-bg { background: #fff5f0; }
.blue-bg   { background: #eff6ff; }
.yellow-bg { background: #fefce8; }

.recharge-icon {
  background: #eff6ff;
}

.func-name {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

/* 开票信息 */
.func-single {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.func-single-left {
  display: flex;
  align-items: center;
}

.func-icon-small {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.func-single-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.func-arrow {
  font-size: 36rpx;
  color: #ccc;
}

/* 退出登录 */
.logout-section {
  margin: 40rpx 0 0;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #fff;
  color: #e74c3c;
  font-size: 30rpx;
  border-radius: 20rpx;
  border: none;
  text-align: center;
}

.logout-btn::after {
  border: none;
}

</style>
