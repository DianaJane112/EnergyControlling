<template>
  <view class="app-root">
    <!-- 网络异常顶部提示条 -->
    <view class="network-bar" v-if="!isOnline">
      <text class="network-bar-text">当前无网络连接，请检查网络设置</text>
    </view>
    <router-view />
  </view>
</template>

<script>
export default {
  data() {
    return {
      isOnline: true
    }
  },
  onLaunch() {
    this.checkNetwork()
    uni.onNetworkStatusChange((res) => {
      this.isOnline = res.isConnected
      if (res.isConnected) {
        uni.showToast({ title: '网络已恢复', icon: 'success', duration: 1500 })
      }
    })
  },
  methods: {
    checkNetwork() {
      uni.getNetworkType({
        success: (res) => {
          this.isOnline = res.networkType !== 'none'
        }
      })
    }
  }
}
</script>

<style>
/* Global styles */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 28rpx;
  color: #333;
}
view, text, scroll-view {
  box-sizing: border-box;
}

/* 网络异常提示条 */
.network-bar {
  width: 100%;
  background: #e74c3c;
  padding: 16rpx 30rpx;
  text-align: center;
  position: sticky;
  top: 0;
  z-index: 9999;
}
.network-bar-text {
  font-size: 26rpx;
  color: #fff;
}
</style>
