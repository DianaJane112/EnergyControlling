import http from './request.js'
import axios from 'axios'

// ── API：充值记录分页查询 ──────────────────────────────────────────────
export function getRechargeRecords(params = {}) {
  return http.get('/v1/record/dbRechargePage', { params })
}

// ── API：充值明细（财务管理） ────────────────────────────────────────────
export function getDepositLog(params = {}) {
  return http.get('/v1/financial/deposit-log', { params })
}

// ── API：充值明细收据打印 ──────────────────────────────────────────────
export function getDepositLogPrint(id) {
  return http.get(`/v1/financial/deposit-log/${id}/print`)
}

// ── API：充值明细导出 ──────────────────────────────────────────────────
export function exportDepositLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/deposit-log', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：分账统计汇总 ──────────────────────────────────────────────────
export function getAccountFlowSummary(params = {}) {
  return http.get('/v1/account-flow/summary', { params })
}

// ── API：分账明细分页查询 ──────────────────────────────────────────────
export function getAccountFlowList(params = {}) {
  return http.get('/v1/account-flow/list', { params })
}

// ── API：退费明细分页查询 ──────────────────────────────────────────────
export function getRefundDetailList(params = {}) {
  return http.get('/v1/refund/detail-list', { params })
}

// ── API：退费明细详情 ──────────────────────────────────────────────────
export function getRefundDetail(id) {
  return http.get(`/v1/refund/detail/${id}`)
}

// ── API：退费明细导出 ──────────────────────────────────────────────────
export function exportRefundLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/refund/export-detail', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：分账统计导出 ──────────────────────────────────────────────────
export function exportAccountFlow(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/account-flow/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：分账统计汇总导出 ────────────────────────────────────────────────
export function exportAccountFlowSummary(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/account-flow/export-summary', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：电表信息分页 ──────────────────────────────────────────────────
export function getMeterPage(params = {}) {
  return http.get('/v1/info/dbPage', { params })
}

// ── API：电表详情 ──────────────────────────────────────────────────────
export function getMeterDetail(dbNo) {
  return http.get('/v1/info/dbDetail', { params: { dbNo } })
}

// ── API：电表用电量分析 ────────────────────────────────────────────────
export function getPowerAnalysis(params = {}) {
  return http.get('/v1/info/getPowerAnalysis', { params })
}

// ── API：电表充值 ──────────────────────────────────────────────────────
export function rechargeMeter(params = {}) {
  return http.post('/v1/info/recharge', null, { params })
}

// ── API：调整余额 ──────────────────────────────────────────────────────
export function updateBalance(params = {}) {
  return http.post('/v1/info/updBalance', null, { params })
}

// ── API：批量修改电价 ──────────────────────────────────────────────────
export function batchUpdatePrice(params = {}) {
  return http.post('/v1/info/batchUpdDj', null, { params })
}

// ── API：获取电表实时数据 ──────────────────────────────────────────────
export function getMeterRealtime(dbNo) {
  return http.get('/v1/info/getDbRealTime', { params: { dbNo } })
}

// ── API：获取虚拟电表信息 ──────────────────────────────────────────────
export function getVirtualDetail(dbNo) {
  return http.post('/v1/info/getVirtualDetail', null, { params: { dbNo } })
}

// ── API：余额退补统一执行 ──────────────────────────────────────────────
export function executeBalanceAdjustment(data = {}) {
  return http.post('/v1/balance-adjustment/execute', data)
}

// ── API：余额调整记录分页查询 ────────────────────────────────────────────
export function getBalanceAdjustmentList(params = {}) {
  return http.get('/v1/balance-adjustment/list', { params })
}

// ── API：余额调整记录详情 ────────────────────────────────────────────────
export function getBalanceAdjustmentDetail(id) {
  return http.get(`/v1/balance-adjustment/${id}`)
}

// ── API：余额调整统计聚合 ────────────────────────────────────────────────
export function getBalanceAdjustmentSummary(params = {}) {
  return http.get('/v1/balance-adjustment/summary', { params })
}

// ── API：余额调整记录导出 ────────────────────────────────────────────────
export function exportBalanceAdjustment(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/balance-adjustment/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：退费记录（财务管理 - 退费日志） ──────────────────────────────────
export function getRefundLogList(params = {}) {
  return http.get('/v1/financial/refund-log', { params })
}

// ── API：退费日志导出 ────────────────────────────────────────────────────
export function exportFinancialRefundLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/refund-log', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：修改虚拟电表余额 ──────────────────────────────────────────────
export function updateVirtualBalance(params = {}) {
  return http.post('/v1/info/updVirtualBalance', null, { params })
}

// ── API：充值汇总统计（按日/月聚合） ────────────────────────────────────
export function getDepositSummary(params = {}) {
  return http.get('/v1/financial/deposit-summary', { params })
}

// ── API：充值汇总导出 ──────────────────────────────────────────────────
export function exportDepositSummary(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/deposit-summary', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── Mock：充值记录分页查询 ─────────────────────────────────────────────
const MOCK_RECORDS = [
  {
    orderNo: 202605150001,
    payamount: 500.00,
    realamount: 498.50,
    balance: 1250.80,
    charegetype: 1,
    passrate: 1.50,
    srvfee: 5.00,
    srvtpe: 1,
    Srvpaid: 1,
    czy: '2026-05-15 14:30:00',
    dbName: '天辰超市',
    dbNo: '202309150001',
    proName: '电子科大科技园天府园',
  },
  {
    orderNo: 202605150002,
    payamount: 1000.00,
    realamount: 997.00,
    balance: 3850.20,
    charegetype: 2,
    passrate: 3.00,
    srvfee: 10.00,
    srvtpe: 1,
    Srvpaid: 0,
    czy: '2026-05-15 16:20:00',
    dbName: '科技园食堂',
    dbNo: '202309150042',
    proName: '电子科大科技园天府园',
  },
  {
    orderNo: 202605160001,
    payamount: 300.00,
    realamount: 299.10,
    balance: 299.10,
    charegetype: 1,
    passrate: 0.90,
    srvfee: 3.00,
    srvtpe: 1,
    Srvpaid: 1,
    czy: '2026-05-16 09:15:00',
    dbName: '咖啡角',
    dbNo: '202309150118',
    proName: '电子科大科技园天府园',
  },
  {
    orderNo: 202605160002,
    payamount: 2000.00,
    realamount: 2000.00,
    balance: 5234.50,
    charegetype: 3,
    passrate: 0.00,
    srvfee: 0.00,
    srvtpe: 1,
    Srvpaid: 1,
    czy: '2026-05-16 11:45:00',
    dbName: '洗衣房',
    dbNo: '202309150203',
    proName: '电子科大科技园天府园',
  },
  {
    orderNo: 202605170001,
    payamount: 150.00,
    realamount: 149.55,
    balance: 856.30,
    charegetype: 1,
    passrate: 0.45,
    srvfee: 1.50,
    srvtpe: 1,
    Srvpaid: 1,
    czy: '2026-05-17 08:50:00',
    dbName: '天辰超市',
    dbNo: '202309150001',
    proName: '电子科大科技园天府园',
  },
]

export function mockGetRechargeRecords(params = {}) {
  const pageNum = params.pageNum || 1
  const pageSzie = params.pageSzie || 10
  const startTime = params.startTime
  const endTime = params.endTime

  let filtered = [...MOCK_RECORDS]

  // 按时间范围过滤
  if (startTime || endTime) {
    filtered = filtered.filter(r => {
      const t = new Date(r.czy).getTime()
      if (startTime && t < startTime) return false
      if (endTime && t > endTime) return false
      return true
    })
  }

  const total = filtered.length
  const start = (pageNum - 1) * pageSzie
  const records = filtered.slice(start, start + pageSzie)

  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        success: true,
        code: 0,
        msg: '操作成功',
        data: {
          records,
          total,
          pageNum,
          pageSzie,
          pages: Math.ceil(total / pageSzie),
        },
        time: Date.now(),
      })
    }, 300)
  })
}

// ── Mock：电表信息分页查询 ─────────────────────────────────────────────
const MOCK_METERS = [
  {
    proId: 1,
    proName: '电子科大科技园天府园',
    no: '202309150001',
    name: '天辰超市',
    bei: 1,
    bjlx: 1,
    balance: 1250.80,
    credit: 500,
    warnline: 100,
    creatuser: 1001,
    cttime: '2023-09-15 10:30:00',
    feetype: 1,
    status: 1,
    online: 1,
    signal: 28,
  },
  {
    proId: 1,
    proName: '电子科大科技园天府园',
    no: '202309150042',
    name: '科技园食堂',
    bei: 5,
    bjlx: 1,
    balance: 3850.20,
    credit: 1000,
    warnline: 200,
    creatuser: 1001,
    cttime: '2023-09-15 14:00:00',
    feetype: 1,
    status: 1,
    online: 1,
    signal: 25,
  },
  {
    proId: 1,
    proName: '电子科大科技园天府园',
    no: '202309150118',
    name: '咖啡角',
    bei: 1,
    bjlx: 1,
    balance: 299.10,
    credit: 200,
    warnline: 50,
    creatuser: 1002,
    cttime: '2024-01-10 09:00:00',
    feetype: 2,
    status: 1,
    online: 2,
    signal: 0,
  },
  {
    proId: 1,
    proName: '电子科大科技园天府园',
    no: '202309150203',
    name: '洗衣房',
    bei: 2,
    bjlx: 1,
    balance: 5234.50,
    credit: 800,
    warnline: 150,
    creatuser: 1001,
    cttime: '2023-11-20 11:15:00',
    feetype: 1,
    status: 2,
    online: 1,
    signal: 22,
  },
  {
    proId: 2,
    proName: '电子科大科技园天府园',
    no: '202405010010',
    name: 'B13栋总表',
    bei: 20,
    bjlx: 2,
    balance: 0.00,
    credit: 0,
    warnline: 0,
    creatuser: 1001,
    cttime: '2024-05-01 08:00:00',
    feetype: 2,
    status: 1,
    online: 1,
    signal: 30,
  },
  {
    proId: 3,
    proName: '电子科大科技园天府园',
    no: '202406150005',
    name: '地下车库照明',
    bei: 1,
    bjlx: 3,
    balance: 890.00,
    credit: 300,
    warnline: 80,
    creatuser: 1003,
    cttime: '2024-06-15 16:30:00',
    feetype: 2,
    status: 1,
    online: 2,
    signal: 0,
  },
]

export function mockGetMeterPage(params = {}) {
  const pageNum = params.pageNum || 1
  const pageSize = params.pageSize || 10
  const no = params.no
  const tagId = params.tagId

  let filtered = [...MOCK_METERS]

  if (no) {
    filtered = filtered.filter(r => r.no.includes(no))
  }
  if (tagId != null) {
    // mock: tagId 匹配 proId 做简单过滤
    filtered = filtered.filter(r => r.proId === tagId)
  }

  const total = filtered.length
  const start = (pageNum - 1) * pageSize
  const records = filtered.slice(start, start + pageSize)

  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        success: true,
        code: 0,
        msg: '操作成功',
        data: {
          records,
          total,
          pageNum,
          pageSize,
          pages: Math.ceil(total / pageSize),
        },
        time: Date.now(),
      })
    }, 300)
  })
}

// ── Mock：电表详情 ─────────────────────────────────────────────────────
const MOCK_METER_DETAILS = {
  '202309150001': {
    proId: 1, proName: '电子科大科技园天府园', no: '202309150001', name: '天辰超市',
    bei: 1, bjlx: 1, balance: 1250.80, credit: 500, warnline: 100,
    creatuser: 1001, cttime: '2023-09-15 10:30:00',
    zhyg: 15820.50, zxyg: 15600.30, fxyg: 220.20, plusPrice: 0.00,
    priceType: 1, priceContent: { price: 0.85, unit: '元/kWh' },
    feetype: 1, status: 1, online: 1, signal: 28,
    joinPhone: ['13800138001', '13900139002'],
  },
  '202309150042': {
    proId: 1, proName: '电子科大科技园天府园', no: '202309150042', name: '科技园食堂',
    bei: 5, bjlx: 1, balance: 3850.20, credit: 1000, warnline: 200,
    creatuser: 1001, cttime: '2023-09-15 14:00:00',
    zhyg: 42560.80, zxyg: 42000.00, fxyg: 560.80, plusPrice: 0.00,
    priceType: 3, priceContent: { tiers: [{ limit: 2000, price: 0.65 }, { limit: 5000, price: 0.85 }, { price: 1.05 }], unit: '元/kWh' },
    feetype: 1, status: 1, online: 1, signal: 25,
    joinPhone: ['13800138003'],
  },
  '202309150118': {
    proId: 1, proName: '电子科大科技园天府园', no: '202309150118', name: '咖啡角',
    bei: 1, bjlx: 1, balance: 299.10, credit: 200, warnline: 50,
    creatuser: 1002, cttime: '2024-01-10 09:00:00',
    zhyg: 6820.00, zxyg: 6750.00, fxyg: 70.00, plusPrice: 5.00,
    priceType: 2, priceContent: { peak: 1.20, flat: 0.85, valley: 0.45, unit: '元/kWh' },
    feetype: 2, status: 1, online: 2, signal: 0,
    joinPhone: [],
  },
}

export function mockGetMeterDetail(dbNo) {
  return new Promise(resolve => {
    setTimeout(() => {
      const detail = MOCK_METER_DETAILS[dbNo]
      if (detail) {
        resolve({ success: true, code: 0, msg: '操作成功', data: detail, time: Date.now() })
      } else {
        resolve({ success: false, code: 404, msg: '表号不存在', data: null, time: Date.now() })
      }
    }, 300)
  })
}

// ── Mock：电表用电量分析 ───────────────────────────────────────────────
const MOCK_POWER_ANALYSIS = {
  '202309150001': { balance: 1250.80, avgCostMoney: 38.50, toMonthElectricity: 420.00, toDayElectricity: 15.30 },
  '202309150042': { balance: 3850.20, avgCostMoney: 142.00, toMonthElectricity: 1850.50, toDayElectricity: 62.80 },
  '202309150118': { balance: 299.10, avgCostMoney: 22.30, toMonthElectricity: 280.00, toDayElectricity: 9.50 },
  '202309150203': { balance: 5234.50, avgCostMoney: 96.80, toMonthElectricity: 1200.00, toDayElectricity: 45.20 },
  '202405010010': { balance: 0.00, avgCostMoney: 560.00, toMonthElectricity: 8500.00, toDayElectricity: 320.00 },
  '202406150005': { balance: 890.00, avgCostMoney: 55.00, toMonthElectricity: 680.00, toDayElectricity: 22.50 },
}

export function mockGetPowerAnalysis(params = {}) {
  const dbNos = params.dbNos || Object.keys(MOCK_POWER_ANALYSIS)
  return new Promise(resolve => {
    setTimeout(() => {
      const data = {}
      for (const no of dbNos) {
        if (MOCK_POWER_ANALYSIS[no]) {
          data[no] = MOCK_POWER_ANALYSIS[no]
        }
      }
      resolve({ success: true, code: 0, msg: '操作成功', data, time: Date.now() })
    }, 300)
  })
}

// ── Mock：电表充值 ─────────────────────────────────────────────────────
export function mockRechargeMeter(params = {}) {
  const { dbNo, amount } = params
  return new Promise((resolve) => {
    setTimeout(() => {
      if (!dbNo || !/^\d{12}$/.test(dbNo)) {
        resolve({ success: false, code: 400, msg: '表号格式错误，需为12位数字', data: null, time: Date.now() })
        return
      }
      const amt = Number(amount)
      if (!amt || amt < 1 || amt > 999999) {
        resolve({ success: false, code: 400, msg: '充值金额需在1-999999之间', data: null, time: Date.now() })
        return
      }
      const meter = MOCK_METER_DETAILS[dbNo]
      if (!meter) {
        resolve({ success: false, code: 404, msg: '表号不存在', data: null, time: Date.now() })
        return
      }
      const newBalance = meter.balance + amt
      meter.balance = newBalance
      const orderNo = 'R' + Date.now() + String(Math.random() * 1000 | 0).padStart(3, '0')
      resolve({ success: true, code: 0, msg: '充值成功', data: { balance: newBalance, orderNo }, time: Date.now() })
    }, 500)
  })
}

// ── Mock：调整余额 ─────────────────────────────────────────────────────
export function mockUpdateBalance(params = {}) {
  const { dbNo, balance, bz } = params
  return new Promise(resolve => {
    setTimeout(() => {
      if (!dbNo || !/^\d{12}$/.test(dbNo)) {
        resolve({ success: false, code: 400, msg: '表号格式错误，需为12位数字', data: null, time: Date.now() })
        return
      }
      const bal = Number(balance)
      if (isNaN(bal) || bal < -999999 || bal > 999999) {
        resolve({ success: false, code: 400, msg: '调整金额需在-999999至999999之间', data: null, time: Date.now() })
        return
      }
      const meter = MOCK_METER_DETAILS[dbNo]
      if (!meter) {
        resolve({ success: false, code: 404, msg: '表号不存在', data: null, time: Date.now() })
        return
      }
      const prevBalance = meter.balance
      meter.balance = bal
      resolve({
        success: true, code: 0, msg: '余额调整成功',
        data: { dbNo, prevBalance, newBalance: bal, bz: bz || '' },
        time: Date.now(),
      })
    }, 500)
  })
}

// ── Mock：批量修改电价 ────────────────────────────────────────────────
export function mockBatchUpdatePrice(params = {}) {
  const { dbNos, priceType, priceContent, plusPrice } = params
  return new Promise(resolve => {
    setTimeout(() => {
      if (!Array.isArray(dbNos) || dbNos.length === 0) {
        resolve({ success: false, code: 400, msg: '表号列表不能为空', data: null, time: Date.now() })
        return
      }
      const pt = Number(priceType)
      if (![1, 2, 3].includes(pt)) {
        resolve({ success: false, code: 400, msg: '电价类型需为1(固定)、2(分时)或3(阶梯)', data: null, time: Date.now() })
        return
      }
      let content
      try {
        content = typeof priceContent === 'string' ? JSON.parse(priceContent) : priceContent
      } catch {
        resolve({ success: false, code: 400, msg: '电价内容格式错误', data: null, time: Date.now() })
        return
      }
      const pp = Number(plusPrice)
      if (isNaN(pp) || pp < 0 || pp > 10) {
        resolve({ success: false, code: 400, msg: '附加费需在0-10之间', data: null, time: Date.now() })
        return
      }
      let count = 0
      for (const no of dbNos) {
        const meter = MOCK_METER_DETAILS[no]
        if (meter) {
          meter.priceType = pt
          meter.priceContent = content
          meter.plusPrice = pp
          count++
        }
      }
      resolve({
        success: true, code: 0, msg: `成功更新${count}个电表电价`,
        data: { updatedCount: count, totalCount: dbNos.length },
        time: Date.now(),
      })
    }, 500)
  })
}

// ── Mock：获取电表实时数据 ─────────────────────────────────────────────
const MOCK_REALTIME = {
  '202309150001': {
    collectorDeviceNo: 'CD202309150001',
    no: '202309150001', online: 1, status: 1,
    iccid: '89860620120012345678', phase: 3,
    poweryg: 4.52, poweryga: 1.58, powerygb: 1.45, powerygc: 1.49,
    powerwg: 0.85, powerwga: 0.30, powerwgb: 0.28, powerwgc: 0.27,
    powerrate: 0.96, powerratea: 0.97, powerrateb: 0.95, powerratec: 0.96,
    frequency: 50.02,
    zxyg: 15600.30, fxyg: 220.20, zhyg: 15820.50,
    dl: 6.80, dlb: 6.55, dlc: 6.72,
    dy: 221.5, dyb: 220.8, dyc: 221.2,
    zxwg: 350.10, fxwg: 42.50,
    signal: 28, interval: 15, clientIp: '10.0.1.101',
    gxsj: '2026-05-20 10:30:15',
  },
  '202309150042': {
    collectorDeviceNo: 'CD202309150042',
    no: '202309150042', online: 1, status: 1,
    iccid: '89860620120012345679', phase: 3,
    poweryg: 12.80, poweryga: 4.30, powerygb: 4.25, powerygc: 4.25,
    powerwg: 2.10, powerwga: 0.70, powerwgb: 0.70, powerwgc: 0.70,
    powerrate: 0.98, powerratea: 0.98, powerrateb: 0.97, powerratec: 0.99,
    frequency: 49.98,
    zxyg: 42000.00, fxyg: 560.80, zhyg: 42560.80,
    dl: 19.50, dlb: 19.20, dlc: 19.35,
    dy: 220.0, dyb: 219.5, dyc: 220.3,
    zxwg: 890.00, fxwg: 120.00,
    signal: 25, interval: 15, clientIp: '10.0.1.102',
    gxsj: '2026-05-20 10:30:20',
  },
  '202309150118': {
    collectorDeviceNo: 'CD202309150118',
    no: '202309150118', online: 0, status: 1,
    iccid: '89860620120012345680', phase: 1,
    poweryg: null, poweryga: null, powerygb: null, powerygc: null,
    powerwg: null, powerwga: null, powerwgb: null, powerwgc: null,
    powerrate: null, powerratea: null, powerrateb: null, powerratec: null,
    frequency: null,
    zxyg: null, fxyg: null, zhyg: null,
    dl: null, dlb: null, dlc: null,
    dy: null, dyb: null, dyc: null,
    zxwg: null, fxwg: null,
    signal: 0, interval: 15, clientIp: null,
    gxsj: '2026-05-20 09:15:00',
  },
}

export function mockGetMeterRealtime(dbNo) {
  return new Promise(resolve => {
    setTimeout(() => {
      const data = MOCK_REALTIME[dbNo]
      if (data) {
        resolve({ success: true, code: 0, msg: '操作成功', data, time: Date.now() })
      } else {
        resolve({ success: false, code: 404, msg: '表号不存在', data: null, time: Date.now() })
      }
    }, 300)
  })
}

// ── Mock：获取虚拟电表信息 ─────────────────────────────────────────────
const MOCK_VIRTUAL_METERS = {
  '202309150301': {
    proId: 1, proName: '电子科大科技园天府园', no: '202309150301', name: 'B16栋分层虚拟表',
    balance: 2850.00, credit: 500, warnline: 100,
    createTime: 1694755200000, createBy: '管理员张三',
    joinPhone: ['13800138005'],
  },
  '202309150302': {
    proId: 1, proName: '电子科大科技园天府园', no: '202309150302', name: '负1层照明虚拟表',
    balance: 450.50, credit: 200, warnline: 50,
    createTime: 1701234567000, createBy: '管理员李四',
    joinPhone: [],
  },
  '202405010201': {
    proId: 2, proName: '电子科大科技园天府园', no: '202405010201', name: 'B13栋公共虚拟表',
    balance: 1200.00, credit: 300, warnline: 80,
    createTime: 1714521600000, createBy: '管理员张三',
    joinPhone: ['13900139009', '13800138010'],
  },
}

export function mockGetVirtualDetail(dbNo) {
  return new Promise(resolve => {
    setTimeout(() => {
      const data = MOCK_VIRTUAL_METERS[dbNo]
      if (data) {
        resolve({ success: true, code: 0, msg: '操作成功', data, time: Date.now() })
      } else {
        resolve({ success: false, code: 404, msg: '虚拟表号不存在', data: null, time: Date.now() })
      }
    }, 300)
  })
}

// ── Mock：修改虚拟电表余额 ─────────────────────────────────────────────
export function mockUpdateVirtualBalance(params = {}) {
  const { dbNo, balance, bz } = params
  return new Promise(resolve => {
    setTimeout(() => {
      if (!dbNo || !/^\d{12}$/.test(dbNo)) {
        resolve({ success: false, code: 400, msg: '表号格式错误，需为12位数字', data: null, time: Date.now() })
        return
      }
      const bal = Number(balance)
      if (isNaN(bal) || bal < -999999 || bal > 999999) {
        resolve({ success: false, code: 400, msg: '调整金额需在-999999至999999之间', data: null, time: Date.now() })
        return
      }
      const meter = MOCK_VIRTUAL_METERS[dbNo]
      if (!meter) {
        resolve({ success: false, code: 404, msg: '虚拟表号不存在', data: null, time: Date.now() })
        return
      }
      const prevBalance = meter.balance
      meter.balance = bal
      resolve({
        success: true, code: 0, msg: '余额调整成功',
        data: { dbNo, prevBalance, newBalance: bal, bz: bz || '' },
        time: Date.now(),
      })
    }, 500)
  })
}
