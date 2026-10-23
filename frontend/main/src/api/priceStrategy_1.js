import http from './request.js'

// ═══════════════════════════════════════════════════════════════════════════
//  费率策略 CRUD（对应后端 PriceStrategyController）
//  base: /api/v1/price-strategy
// ═══════════════════════════════════════════════════════════════════════════

// ── API：策略分页列表 ──────────────────────────────────────────────────
export function getPriceStrategyPage(params = {}) {
  return http.get('/v1/price-strategy/page', { params })
}

// ── API：策略详情 ──────────────────────────────────────────────────────
export function getPriceStrategyDetail(id) {
  return http.get(`/v1/price-strategy/${id}`)
}

// ── API：新增策略 ──────────────────────────────────────────────────────
export function createPriceStrategy(data) {
  return http.post('/v1/price-strategy', data)
}

// ── API：编辑策略 ──────────────────────────────────────────────────────
export function updatePriceStrategy(id, data) {
  return http.put(`/v1/price-strategy/${id}`, data)
}

// ── API：删除策略（逻辑删除）───────────────────────────────────────────
export function deletePriceStrategy(id) {
  return http.delete(`/v1/price-strategy/${id}`)
}

// ── API：启停策略 ──────────────────────────────────────────────────────
export function togglePriceStrategyStatus(id, status) {
  return http.put(`/v1/price-strategy/${id}/status`, null, { params: { status } })
}

// ═══════════════════════════════════════════════════════════════════════════
//  Mock 数据
// ═══════════════════════════════════════════════════════════════════════════

const MOCK_STRATEGIES = [
  {
    id: 1,
    strategyName: '固定电价-商业用电',
    strategyCode: 'FIXED_COMMERCIAL',
    projectId: null,
    priceType: 1,
    timeSlots: null,
    fixedPrice: 0.85,
    plusPrice: 0.00,
    effectiveDate: '2025-01-01',
    expireDate: null,
    status: 1,
    remark: '默认商业用电固定单价',
    createTime: '2025-01-01T00:00:00',
    updateTime: '2025-06-01T10:00:00',
    createBy: 'admin',
  },
  {
    id: 2,
    strategyName: '夏季分时电价',
    strategyCode: 'SUMMER_TOU',
    projectId: null,
    priceType: 2,
    timeSlots: JSON.stringify([
      { slotName: '尖', startHour: 19, endHour: 21, price: 1.20 },
      { slotName: '峰', startHour: 8, endHour: 11, price: 1.00 },
      { slotName: '平', startHour: 7, endHour: 8, price: 0.65 },
      { slotName: '谷', startHour: 23, endHour: 7, price: 0.35 },
    ]),
    fixedPrice: null,
    plusPrice: 0.00,
    effectiveDate: '2025-06-01',
    expireDate: '2025-09-30',
    status: 1,
    remark: '夏季尖峰平谷分时电价',
    createTime: '2025-05-15T08:00:00',
    updateTime: '2025-05-15T08:00:00',
    createBy: 'admin',
  },
  {
    id: 3,
    strategyName: '阶梯电价-居民用电',
    strategyCode: 'TIER_RESIDENTIAL',
    projectId: 1,
    priceType: 3,
    timeSlots: JSON.stringify([
      { amount: 200, price: 0.50 },
      { amount: 400, price: 0.60 },
      { price: 0.85 },
    ]),
    fixedPrice: null,
    plusPrice: 0.00,
    effectiveDate: '2025-01-01',
    expireDate: null,
    status: 1,
    remark: '居民阶梯电价，三档累进',
    createTime: '2025-01-01T00:00:00',
    updateTime: '2025-03-01T12:00:00',
    createBy: 'admin',
  },
  {
    id: 4,
    strategyName: '冬季分时电价',
    strategyCode: 'WINTER_TOU',
    projectId: null,
    priceType: 2,
    timeSlots: JSON.stringify([
      { slotName: '峰', startHour: 8, endHour: 12, price: 0.95 },
      { slotName: '平', startHour: 12, endHour: 17, price: 0.60 },
      { slotName: '谷', startHour: 21, endHour: 8, price: 0.30 },
    ]),
    fixedPrice: null,
    plusPrice: 0.00,
    effectiveDate: '2025-10-01',
    expireDate: '2026-03-31',
    status: 0,
    remark: '冬季峰谷电价，已停用',
    createTime: '2025-09-20T14:00:00',
    updateTime: '2026-04-01T00:00:00',
    createBy: 'admin',
  },
  {
    id: 5,
    strategyName: '工业固定电价',
    strategyCode: 'FIXED_INDUSTRIAL',
    projectId: 1,
    priceType: 1,
    timeSlots: null,
    fixedPrice: 1.15,
    plusPrice: 0.05,
    effectiveDate: '2025-01-01',
    expireDate: null,
    status: 1,
    remark: '工业用电固定单价，含附加费',
    createTime: '2025-01-01T00:00:00',
    updateTime: '2025-01-01T00:00:00',
    createBy: 'admin',
  },
]

export function mockGetPriceStrategyPage(params = {}) {
  const pageNum = Number(params.pageNum) || 1
  const pageSize = Number(params.pageSize) || 20
  const keyword = params.keyword

  let filtered = [...MOCK_STRATEGIES]

  if (keyword) {
    const kw = keyword.toLowerCase()
    filtered = filtered.filter(
      r =>
        r.strategyName.toLowerCase().includes(kw) ||
        r.strategyCode.toLowerCase().includes(kw)
    )
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
          size: pageSize,
          current: pageNum,
          pages: Math.ceil(total / pageSize),
        },
        time: Date.now(),
      })
    }, 300)
  })
}

export function mockGetPriceStrategyDetail(id) {
  return new Promise(resolve => {
    setTimeout(() => {
      const strategy = MOCK_STRATEGIES.find(r => r.id === Number(id))
      if (strategy) {
        resolve({ success: true, code: 0, msg: '操作成功', data: { ...strategy }, time: Date.now() })
      } else {
        resolve({ success: false, code: 404, msg: '策略不存在', data: null, time: Date.now() })
      }
    }, 200)
  })
}

export function mockCreatePriceStrategy(data) {
  return new Promise(resolve => {
    setTimeout(() => {
      const newId = Math.max(...MOCK_STRATEGIES.map(r => r.id), 0) + 1
      const strategy = {
        id: newId,
        ...data,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        createBy: 'admin',
      }
      MOCK_STRATEGIES.push(strategy)
      resolve({ success: true, code: 0, msg: '新增成功', data: strategy, time: Date.now() })
    }, 300)
  })
}

export function mockUpdatePriceStrategy(id, data) {
  return new Promise(resolve => {
    setTimeout(() => {
      const idx = MOCK_STRATEGIES.findIndex(r => r.id === Number(id))
      if (idx < 0) {
        resolve({ success: false, code: 404, msg: '策略不存在', data: null, time: Date.now() })
        return
      }
      MOCK_STRATEGIES[idx] = {
        ...MOCK_STRATEGIES[idx],
        ...data,
        id: Number(id),
        updateTime: new Date().toISOString(),
      }
      resolve({ success: true, code: 0, msg: '编辑成功', data: MOCK_STRATEGIES[idx], time: Date.now() })
    }, 300)
  })
}

export function mockDeletePriceStrategy(id) {
  return new Promise(resolve => {
    setTimeout(() => {
      const idx = MOCK_STRATEGIES.findIndex(r => r.id === Number(id))
      if (idx >= 0) {
        MOCK_STRATEGIES.splice(idx, 1)
        resolve({ success: true, code: 0, msg: '删除成功', data: null, time: Date.now() })
      } else {
        resolve({ success: false, code: 404, msg: '策略不存在', data: null, time: Date.now() })
      }
    }, 200)
  })
}

export function mockTogglePriceStrategyStatus(id, status) {
  return new Promise(resolve => {
    setTimeout(() => {
      const strategy = MOCK_STRATEGIES.find(r => r.id === Number(id))
      if (!strategy) {
        resolve({ success: false, code: 404, msg: '策略不存在', data: null, time: Date.now() })
        return
      }
      strategy.status = status
      strategy.updateTime = new Date().toISOString()
      resolve({
        success: true,
        code: 0,
        msg: status === 1 ? '策略已启用' : '策略已停用',
        data: null,
        time: Date.now(),
      })
    }, 200)
  })
}
