<template>
  <view class="login-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">明思管家</text>
      </view>
    </view>

    <!-- 中间背景区域 -->
    <view class="hero-area">
      <image class="hero-bg" src="/static/bg.jpg" mode="aspectFill" />
    </view>

    <!-- 底部操作区 -->
    <view class="action-area">
      <!-- 微信登录 -->
      <button
        class="login-btn wechat-btn"
        hover-class="login-btn-hover"
        :loading="loading"
        :disabled="loading"
        @click="handleWechatLogin"
      >
        <text v-if="!loading">微信一键登录</text>
        <text v-else>登录中...</text>
      </button>

      <!-- 协议勾选 -->
      <view class="agreement-row">
        <view class="checkbox-wrapper" @click="toggleAgree">
          <view class="checkbox" :class="{ checked: agreed }">
            <text v-if="agreed" class="check-icon">✓</text>
          </view>
        </view>
        <text class="agreement-text">
          已阅读并同意
          <text class="agreement-link" @click.stop="showAgreement">用户服务协议</text>
        </text>
      </view>

    </view>

    <!-- 底部系统信息 -->
    <view class="footer">
      <text class="footer-text">智能电表系统</text>
    </view>
  </view>
</template>

<script>
import { wechatLogin } from '@/api/login.js'

export default {
  data() {
    return {
      statusBarHeight: 20,
      agreed: true,
      loading: false
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  methods: {
    toggleAgree() {
      this.agreed = !this.agreed
    },
    showAgreement() {
      uni.showToast({ title: '用户服务协议', icon: 'none' })
    },

    // 微信登录
    handleWechatLogin() {
      if (!this.agreed) {
        uni.showToast({ title: '请先阅读并同意用户服务协议', icon: 'none' })
        return
      }
      this.loading = true

      uni.login({
        provider: 'weixin',
        success: (loginRes) => {
          wechatLogin(loginRes.code)
            .then((data) => {
              this.loginSuccess(data)
            })
            .catch(() => {
              this.loading = false
            })
        },
        fail: () => {
          uni.showToast({ title: '获取微信授权失败', icon: 'none' })
          this.loading = false
        }
      })
    },

    // 登录成功
    loginSuccess(data) {
      this.loading = false
      uni.setStorageSync('token', data.token)
      if (data.userInfo) {
        uni.setStorageSync('userInfo', data.userInfo)
      }
      if (data.customerInfo) {
        uni.setStorageSync('customerInfo', data.customerInfo)
      }

      if (data.needBindCustomer) {
        const userId = data.userInfo && data.userInfo.userId
        uni.showModal({
          title: '微信账号已登记',
          content: `请联系管理员分配虚拟表查看权限${userId ? `\n用户编号：${userId}` : ''}，授权后重新登录即可使用。`,
          showCancel: false,
          confirmText: '我知道了'
        })
        return
      }

      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 800)
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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

/* 中间背景区域 */
.hero-area {
  flex: 1;
  width: 100%;
}

.hero-bg {
  width: 100%;
  height: 100%;
}

/* 底部操作区 */
.action-area {
  width: 100%;
  padding: 40rpx 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 授权登录按钮 */
.login-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  color: #fff;
  font-size: 34rpx;
  font-weight: 600;
  border-radius: 48rpx;
  border: none;
  text-align: center;
}

.login-btn::after {
  border: none;
}

.login-btn-hover {
  opacity: 0.85;
}

.wechat-btn {
  background: #ff7a3d;
  box-shadow: 0 8rpx 24rpx rgba(255, 92, 26, 0.35);
}

/* 协议勾选 */
.agreement-row {
  display: flex;
  align-items: center;
  margin-top: 32rpx;
}

.checkbox-wrapper {
  padding: 4rpx;
}

.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #ff7a3d;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.checkbox.checked {
  background: #ff7a3d;
  border-color: #ff7a3d;
}

.check-icon {
  font-size: 22rpx;
  color: #fff;
  font-weight: bold;
}

.agreement-text {
  font-size: 26rpx;
  color: #999;
}

.agreement-link {
  color: #ff7a3d;
}

/* 底部 */
.footer {
  width: 100%;
  padding: 30rpx 0 50rpx;
  text-align: center;
}

.footer-text {
  font-size: 24rpx;
  color: #ccc;
  letter-spacing: 2rpx;
}
</style>
