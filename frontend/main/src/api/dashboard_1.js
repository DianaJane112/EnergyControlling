import http from './request.js'

// ════════════════════════════════════════════════════════════════════════
//  符合后端规范的真实 Mock 数据（字段与真接口完全一致）
// ════════════════════════════════════════════════════════════════════════

function buildDailyTrends() {
  const DAYS = ['05-20', '05-21', '05-22', '05-23', '05-24', '05-25', '05-26']
  const elecMap = [1020, 1340, 890, 1100, 950, 1230, 1250.5]
  const waterMap = [380, 450, 290, 340, 410, 370, 340.2]

  return DAYS.map((day, i) => ({
    date: `2026-${day}`,
    totalElectricity: elecMap[i],
    totalWater: waterMap[i]
  }))
}

const MOCK_DASHBOARD = {
  code: 1073741824,
  msg: '操作成功',
  data: {
    assetOverview: {
      totalMeters: 128,
      activeMeters: 110,
      overdueMeters: 8,
      onlineRate: 85.94
    },
    financeOverview: {
      totalBalance: 152300.5,
      todayRechargeTotal: 5200,
      todayDebitTotal: 1340.82
    },
    dailyTrends: buildDailyTrends(),
    riskAlerts: [
      { customerId: 1, customerName: '天辰超市', balance: -1250.80 },
      { customerId: 2, customerName: '科技园食堂', balance: -380.20 },
      { customerId: 5, customerName: '电子科大3号商铺', balance: 23.5 }
    ]
  },
  timestamp: Date.now(),
  success: true
}

// ════════════════════════════════════════════════════════════════════════
//  API 接口
// ════════════════════════════════════════════════════════════════════════

// 1. 真实的后端请求接口
export function getDashboardStatistics(params) {
  return http.get('/v1/dashboard/statistics', { params })
}

// 2. 用电量日报汇总（按日聚合总用电量）
export function getElectricityDailySummary(params = {}) {
  return http.get('/v1/report/daily-summary', { params })
}

// 3. 用水量日报汇总（按日聚合总用水量）
export function getWaterDailySummary(params = {}) {
  return http.get('/v1/water-meter-report/daily-summary', { params })
}

// 4. 本地测试用的 Mock 接口
export function mockGetDashboardStatistics() {
  return new Promise(resolve => {
    setTimeout(() => {
      const snap = JSON.parse(JSON.stringify(MOCK_DASHBOARD))
      snap.timestamp = Date.now()
      resolve(snap)
    }, 300)
  })
}