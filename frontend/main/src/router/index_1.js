import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'login',
    meta: { title: '登录 - 小鳗管家' },
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', meta: { title: '智慧水电管理系统', subtitle: '' }, component: () => import('@/views/DashboardView.vue') },
      { path: 'meters/physical', name: 'physical-meter-list', meta: { title: '电表管理', subtitle: '电表列表' }, component: () => import('@/views/PhysicalMeterList.vue') },
      { path: 'meters/water', name: 'water-meter-list', meta: { title: '水表管理', subtitle: '水表列表' }, component: () => import('@/views/WaterMeterManagement.vue') },
      { path: 'meters/water-virtual', name: 'water-virtual-meter-list', meta: { title: '水表管理', subtitle: '虚拟水表' }, component: () => import('@/views/WaterVirtualMeter.vue') },
      { path: 'meters/water-deleted', name: 'water-deleted-meter-list', meta: { title: '水表管理', subtitle: '已删除水表' }, component: () => import('@/views/WaterDeletedMeter.vue') },
      { path: 'meters/virtual', name: 'virtual-meter-list', meta: { title: '电表管理', subtitle: '虚拟电表' }, component: () => import('@/views/VirtualMeterList.vue') },
      { path: 'meters/deleted', name: 'deleted-meter-list', meta: { title: '电表管理', subtitle: '已删除电表' }, component: () => import('@/views/DeletedMeterList.vue') },
      { path: 'stations', name: 'station-management', meta: { title: '基站管理', subtitle: '' }, component: () => import('@/views/StationManagement.vue') },
      { path: 'stations/water', name: 'water-station-management', meta: { title: '基站管理', subtitle: '' }, component: () => import('@/views/WaterStationManagement.vue') },
      { path: 'billing/recharge-details', name: 'recharge-details', meta: { title: '财务收费', subtitle: '充值明细' }, component: () => import('@/views/RechargeDetails.vue') },
      { path: 'billing/recharge-stats', name: 'recharge-stats', meta: { title: '财务收费', subtitle: '充值统计' }, component: () => import('@/views/RechargeStatsView.vue') },
      { path: 'billing/split-account-stats', name: 'split-account-stats', meta: { title: '财务收费', subtitle: '分账统计' }, component: () => import('@/views/SplitAccountStats.vue') },
      { path: 'billing/split-account-details', name: 'split-account-details', meta: { title: '财务收费', subtitle: '分账明细' }, component: () => import('@/views/SplitAccountDetails.vue') },
      { path: 'billing/refund-details', name: 'refund-details', meta: { title: '财务收费', subtitle: '退费明细' }, component: () => import('@/views/RefundDetails.vue') },
      { path: 'billing/balance-adjustment', name: 'balance-adjustment', meta: { title: '余额退补', subtitle: '退补记录' }, component: () => import('@/views/BalanceAdjustment.vue') },
      { path: 'billing/water-balance-adjustment', name: 'water-balance-adjustment', meta: { title: '余额退补', subtitle: '退补记录' }, component: () => import('@/views/WaterBalanceAdjustment.vue') },
      { path: 'billing/public-allocation', name: 'public-allocation', meta: { title: '公共分摊', subtitle: '分摊规则' }, component: () => import('@/views/PublicAllocation.vue') },
      { path: 'merchant/management', name: 'merchant-management', meta: { title: '商户管理', subtitle: '' }, component: () => import('@/views/MerchantManagement.vue') },
      { path: 'merchant/water', name: 'merchant-water', meta: { title: '水表客户管理', subtitle: '' }, component: () => import('@/views/MerchantManagement.vue') },
      { path: 'merchant/electric', name: 'merchant-electric', meta: { title: '电表客户管理', subtitle: '' }, component: () => import('@/views/MerchantManagement.vue') },
      { path: 'merchant/instant-bill', name: 'instant-bill', meta: { title: '商户管理', subtitle: '即时账单' }, component: () => import('@/views/InstantBill.vue') },
      { path: 'operations/group-config', name: 'group-config', meta: { title: '运维管理', subtitle: '分组配置' }, component: () => import('@/views/GroupConfig.vue') },
      { path: 'operations/manual-payment', name: 'manual-payment', meta: { title: '运维管理', subtitle: '人工收费' }, component: () => import('@/views/ManualPayment.vue') },
      { path: 'operations/water-manual-payment', name: 'water-manual-payment', meta: { title: '运维管理', subtitle: '人工收费' }, component: () => import('@/views/WaterManualPayment.vue') },
      { path: 'reports/water-daily', name: 'water-daily-report', meta: { title: '统计报表', subtitle: '用水量日报表' }, component: () => import('@/views/WaterDailyReport.vue') },
      { path: 'reports/water-monthly', name: 'water-monthly-report', meta: { title: '统计报表', subtitle: '用水量月报表' }, component: () => import('@/views/WaterMonthlyReport.vue') },
      { path: 'reports/water-monthly-deleted', name: 'water-deleted-meter-report', meta: { title: '统计报表', subtitle: '用水量月报表(删除水表)' }, component: () => import('@/views/WaterDeletedMeterReport.vue') },
      { path: 'reports/water-monthly-summary', name: 'water-monthly-summary', meta: { title: '统计报表', subtitle: '月度用水量汇总表' }, component: () => import('@/views/WaterMonthlySummaryReport.vue') },
      { path: 'reports/water-yearly-summary', name: 'water-yearly-summary', meta: { title: '统计报表', subtitle: '年度用水量汇总表' }, component: () => import('@/views/WaterYearlySummaryReport.vue') },
      { path: 'reports/billing-water-summary', name: 'billing-water-summary', meta: { title: '统计报表', subtitle: '计费水量汇总表' }, component: () => import('@/views/BillingWaterSummaryReport.vue') },
      { path: 'reports/billing-water-monthly', name: 'billing-water-monthly', meta: { title: '统计报表', subtitle: '计费水量月报表' }, component: () => import('@/views/BillingWaterMonthlyReport.vue') },
      { path: 'reports/billing-water-yearly', name: 'billing-water-yearly', meta: { title: '统计报表', subtitle: '计费水量年报表' }, component: () => import('@/views/BillingWaterYearlyReport.vue') },
      { path: 'reports/electric-load', name: 'electric-load-query', meta: { title: '统计报表', subtitle: '用电负荷查询' }, component: () => import('@/views/ElectricLoadQuery.vue') },
      { path: 'reports/energy-trend', name: 'energy-trend', meta: { title: '统计报表', subtitle: '用能趋势' }, component: () => import('@/views/EnergyTrend.vue') },
      { path: 'reports/:reportId', name: 'report-detail', meta: { title: '统计报表（电表）', subtitle: '' }, component: () => import('@/views/ReportCenter.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const { isAuthenticated } = useAuth()

  if (to.name === 'login' && isAuthenticated.value) {
    // Already logged in — go to dashboard
    next({ path: '/dashboard' })
    return
  }

  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)
  if (requiresAuth && !isAuthenticated.value) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
