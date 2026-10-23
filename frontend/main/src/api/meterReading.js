import axios from 'axios'

// ── API 配置 ──────────────────────────────────────────────────────────
const BASE_URL = '/meter-api'
const SIGN_VALUE = 'HW123'

// ── Axios 实例 ────────────────────────────────────────────────────────
const http = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
})

// 请求拦截器：注入签名
http.interceptors.request.use(config => {
  if (config.method === 'get') {
    config.params = { ...config.params, sign: SIGN_VALUE }
  }
  return config
})

// 响应拦截器：统一解包
http.interceptors.response.use(
  res => res.data,
  err => Promise.reject(err)
)

// ════════════════════════════════════════════════════════════════════════
//  API 接口
// ════════════════════════════════════════════════════════════════════════

// ── 1. 表信息同步 ─────────────────────────────────────────────────────
export function meterTran(row) {
  return http.post('/ws/rsyw/meterTran', { row })
}

// ── 2. 客户信息同步 ────────────────────────────────────────────────────
export function customTran(row) {
  return http.post('/ws/rsyw/customTran', { row })
}

// ── 3. 客户状态变更 ────────────────────────────────────────────────────
export function customerStateChange(row) {
  return http.post('/ws/rsyw/customerStateChange', { row })
}

// ── 4. 阀控 ────────────────────────────────────────────────────────────
export function valveControl(row) {
  return http.post('/ws/rsyw/valveControl', { row })
}

// ── 5. 接收抄表数据 ────────────────────────────────────────────────────
export function getMeterReading(params = {}) {
  return http.get('/ws/rsyw/GetMeterReading', { params })
}

// ── 6. 接收表计状态更新数据 ────────────────────────────────────────────
export function getMeterState(params = {}) {
  return http.get('/ws/rsyw/GetMeterState', { params })
}

// ── 7. 获取生产厂家列表 ────────────────────────────────────────────────
export function getVender(params = {}) {
  return http.get('/ws/rsyw/GetVender', { params })
}

// ── 8. 获取区域列表 ────────────────────────────────────────────────────
export function getArea(params = {}) {
  return http.get('/ws/rsyw/GetArea', { params })
}

// ── 9. 获取表计类型 ────────────────────────────────────────────────────
export function getMeterType(params = {}) {
  return http.get('/ws/rsyw/GetMeterType', { params })
}

// ── 10. 获取终端类型 ───────────────────────────────────────────────────
export function getTerminal(params = {}) {
  return http.get('/ws/rsyw/GetTerminal', { params })
}

// ── 11. 根据时间及客户号返回最新抄表数据 ──────────────────────────────
export function getMeterReadingByTime(row) {
  return http.post('/ws/rsyw/GetMeterReadingByTime', { row })
}

// ════════════════════════════════════════════════════════════════════════
//  Mock 实现
// ════════════════════════════════════════════════════════════════════════

// ── Mock：表信息同步 ──────────────────────────────────────────────────
const MOCK_METERS_SYNC = [
  { steal_no: '6145200009', type: '0', address: '商业用户物联表', meter_type_name: '积成远传水表', caliber: '1.6', vender: '青岛积成电子', switch_op: '2', start_val: '0', terminal_name: '政协花园3号楼集中器', channel_type: '99', meter_phone: '1064826176010', telecomm_sn: '1', imei: '0123456789', iccid: '123456', create_date: '2015-09-15 00:00:00' },
  { steal_no: '6145200010', type: '0', address: '观湖壹号3栋', meter_type_name: 'CPU水表', caliber: '2.0', vender: '春城水表厂', switch_op: '1', start_val: '150', terminal_name: '观湖壹号集中器', channel_type: '0', meter_phone: '1064826176011', telecomm_sn: '2', imei: '0123456790', iccid: '123457', create_date: '2015-10-20 00:00:00' },
  { steal_no: '6145200011', type: '0', address: '成南新居5栋', meter_type_name: '江苏远传水表', caliber: '1.6', vender: '江苏远传', switch_op: '2', start_val: '0', terminal_name: '成南新居3号集中器', channel_type: '1', meter_phone: '1064826176012', telecomm_sn: '3', imei: '0123456791', iccid: '123458', create_date: '2016-01-05 00:00:00' },
  { steal_no: '6145200012', type: '0', address: '测试小区12栋', meter_type_name: '易维电子水表', caliber: '1.6', vender: '易维电子', switch_op: '1', start_val: '320', terminal_name: '测试集中器1号', channel_type: '2', meter_phone: '1064826176013', telecomm_sn: '4', imei: '0123456792', iccid: '123459', create_date: '2016-03-15 00:00:00' },
  { steal_no: '6145200013', type: '0', address: '天府新区商业街', meter_type_name: '宁波水表', caliber: '3.0', vender: '宁波水表', switch_op: '2', start_val: '0', terminal_name: '天府集中器A', channel_type: '99', meter_phone: '1064826176014', telecomm_sn: '5', imei: '0123456793', iccid: '123460', create_date: '2016-06-01 00:00:00' },
]

export function mockMeterTran(row) {
  return new Promise(resolve => {
    setTimeout(() => {
      const results = (row || []).map(item => {
        const meter = MOCK_METERS_SYNC.find(m => m.steal_no === item.steal_no)
        if (!meter && item.type !== '0') {
          return { steal_no: item.steal_no, returnvalue: '1', message: '表计不存在', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', vender: item.vender || '', sign: SIGN_VALUE }
        }
        if (item.type === '0') {
          if (!meter) {
            MOCK_METERS_SYNC.push({ ...item, create_date: item.create_date || new Date().toISOString().slice(0, 10) + ' 00:00:00' })
          }
          return { steal_no: item.steal_no, returnvalue: '0', message: '', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', vender: item.vender || '', sign: SIGN_VALUE }
        } else if (item.type === '1') {
          Object.assign(meter, item)
          return { steal_no: item.steal_no, returnvalue: '0', message: '', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', vender: item.vender || '', sign: SIGN_VALUE }
        } else if (item.type === '2') {
          const idx = MOCK_METERS_SYNC.indexOf(meter)
          if (idx >= 0) MOCK_METERS_SYNC.splice(idx, 1)
          return { steal_no: item.steal_no, returnvalue: '0', message: '', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', vender: item.vender || '', sign: SIGN_VALUE }
        }
        return { steal_no: item.steal_no, returnvalue: '1', message: '未知操作类型', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', vender: item.vender || '', sign: SIGN_VALUE }
      })
      resolve(results)
    }, 400)
  })
}

// ── Mock：客户信息同步 ──────────────────────────────────────────────────
const MOCK_CUSTOMERS = [
  { customer_code: '100366', customer_name: '测试用户A', area_name: '测试片区', address: '商业用户物联表', phone: '13800138001', customer_type: '民用', use_nature: '民用', meter_type_name: '积成远传水表', terminal_name: '成南新居3号集中器', channel_type: '99', caliber: '1.6', meter_phone: '1064826176010', steal_no: '6145200009', start_val: '0', switch_op: '2', vender: '青岛积成电子', telecomm_sn: '1', imei: '0123456789', iccid: '123456', create_date: '2015-09-15 00:00:00' },
  { customer_code: '100399', customer_name: '测试用户B', area_name: '观湖壹号', address: '观湖壹号3栋', phone: '13800138002', customer_type: '商业', use_nature: '商业', meter_type_name: 'CPU水表', terminal_name: '观湖壹号集中器', channel_type: '0', caliber: '2.0', meter_phone: '1064826176011', steal_no: '6145200010', start_val: '150', switch_op: '1', vender: '春城水表厂', telecomm_sn: '2', imei: '0123456790', iccid: '123457', create_date: '2015-10-20 00:00:00' },
  { customer_code: '850000001', customer_name: '积成测试用户', area_name: '政协花园', address: '政协花园1栋', phone: '13800138003', customer_type: '民用', use_nature: '民用', meter_type_name: '积成远传水表', terminal_name: '政协花园3号楼集中器', channel_type: '99', caliber: '1.6', meter_phone: '1064826176015', steal_no: '1354654654', start_val: '0', switch_op: '2', vender: '青岛积成电子', telecomm_sn: '6', imei: '0123456794', iccid: '123461', create_date: '2015-06-10 00:00:00' },
]

export function mockCustomTran(row) {
  return new Promise(resolve => {
    setTimeout(() => {
      const results = (row || []).map(item => {
        if (item.type === '0') {
          MOCK_CUSTOMERS.push({ ...item, customer_code: item.customer_code || ('C' + Date.now()) })
          return { customer_code: item.customer_code, returnvalue: '0', message: '', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', sign: SIGN_VALUE }
        } else if (item.type === '1' || item.type === '2' || item.type === '3') {
          const existing = MOCK_CUSTOMERS.find(c => c.customer_code === item.customer_code)
          if (existing) {
            if (item.type === '3') {
              existing.old_steal_no = existing.steal_no
              existing.old_val = existing.start_val
              existing.steal_no = item.steal_no
              existing.start_val = item.start_val
            } else {
              Object.assign(existing, item)
            }
            return { customer_code: item.customer_code, returnvalue: '0', message: '', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', sign: SIGN_VALUE }
          }
          return { customer_code: item.customer_code, returnvalue: '1', message: '客户不存在', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', sign: SIGN_VALUE }
        }
        return { customer_code: item.customer_code, returnvalue: '1', message: '未知操作类型', operate_date: new Date().toISOString().slice(0, 10) + ' 00:00:00', sign: SIGN_VALUE }
      })
      resolve(results)
    }, 400)
  })
}

// ── Mock：客户状态变更 ──────────────────────────────────────────────────
const MOCK_CUSTOMER_STATES = {}

export function mockCustomerStateChange(row) {
  return new Promise(resolve => {
    setTimeout(() => {
      const results = (row || []).map(item => {
        const key = item.customer_code
        MOCK_CUSTOMER_STATES[key] = { type: item.type, operator: item.operator }
        const exists = MOCK_CUSTOMERS.find(c => c.customer_code === item.customer_code)
        if (!exists) {
          return { customer_code: item.customer_code, returnvalue: '1', sign: SIGN_VALUE }
        }
        return { customer_code: item.customer_code, returnvalue: '0', sign: SIGN_VALUE }
      })
      resolve(results)
    }, 300)
  })
}

// ── Mock：阀控 ────────────────────────────────────────────────────────
const MOCK_VALVE_STATES = {}

export function mockValveControl(row) {
  return new Promise(resolve => {
    setTimeout(() => {
      const results = (row || []).map(item => {
        const meter = MOCK_METERS_SYNC.find(m => m.steal_no === item.steal_no)
        if (!meter) {
          return { steal_no: item.steal_no, vender: item.vender || '', returnvalue: '1', operate_date: new Date().toISOString().slice(0, 19).replace('T', ' '), sign: SIGN_VALUE }
        }
        MOCK_VALVE_STATES[item.steal_no] = item.type
        return { steal_no: item.steal_no, vender: item.vender || '', returnvalue: '0', operate_date: new Date().toISOString().slice(0, 19).replace('T', ' '), sign: SIGN_VALUE }
      })
      resolve(results)
    }, 500)
  })
}

// ── Mock：接收抄表数据 ──────────────────────────────────────────────────
const MOCK_METER_READINGS = [
  { customer_code: '850000001', steal_no: '1354654654', vender: '青岛积成电子', read_date: '2016-05-26 00:00:00', read_val: 1250, read_jval: 380.50, sign: SIGN_VALUE },
  { customer_code: '100366', steal_no: '6145200009', vender: '青岛积成电子', read_date: '2025-09-15 00:00:00', read_val: 890, read_jval: 210.00, sign: SIGN_VALUE },
  { customer_code: '100399', steal_no: '6145200010', vender: '春城水表厂', read_date: '2025-10-20 00:00:00', read_val: 2340, read_jval: 150.00, sign: SIGN_VALUE },
  { customer_code: '100366', steal_no: '6145200009', vender: '青岛积成电子', read_date: '2025-10-15 00:00:00', read_val: 910, read_jval: 200.00, sign: SIGN_VALUE },
  { customer_code: '100366', steal_no: '6145200009', vender: '青岛积成电子', read_date: '2025-11-15 00:00:00', read_val: 935, read_jval: 185.00, sign: SIGN_VALUE },
]

export function mockGetMeterReading() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_METER_READINGS } })
    }, 300)
  })
}

// ── Mock：接收表计状态更新数据 ──────────────────────────────────────────
const MOCK_METER_STATES = [
  { customer_code: '850000001', steal_no: '1354654654', vender: '青岛积成电子', status: '1', message: '欠费', record_date: '2015-11-11 00:00:00', sign: SIGN_VALUE },
  { customer_code: '100366', steal_no: '6145200009', vender: '青岛积成电子', status: '0', message: '', record_date: '2025-12-01 00:00:00', sign: SIGN_VALUE },
  { customer_code: '100399', steal_no: '6145200010', vender: '春城水表厂', status: '2', message: '磁干扰报警', record_date: '2025-11-20 00:00:00', sign: SIGN_VALUE },
]

export function mockGetMeterState() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_METER_STATES } })
    }, 300)
  })
}

// ── Mock：获取生产厂家列表 ──────────────────────────────────────────────
const MOCK_VENDERS = [
  { FACT_CODE: '0431', FACT_NAME: '春城水表厂' },
  { FACT_CODE: '3809', FACT_NAME: '青岛积成电子' },
  { FACT_CODE: '3801', FACT_NAME: '江苏远传' },
  { FACT_CODE: '3802', FACT_NAME: '江西三川' },
  { FACT_CODE: '3803', FACT_NAME: '济南瑞泉' },
  { FACT_CODE: '3804', FACT_NAME: '宁波水表' },
  { FACT_CODE: '3805', FACT_NAME: '易维电子' },
  { FACT_CODE: '3806', FACT_NAME: '山东潍微' },
  { FACT_CODE: '3807', FACT_NAME: '杭州竞达' },
  { FACT_CODE: '3808', FACT_NAME: '杭州山科' },
  { FACT_CODE: '3378', FACT_NAME: '青岛积成' },
  { FACT_CODE: '3810', FACT_NAME: '信息部' },
]

export function mockGetVender() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_VENDERS } })
    }, 200)
  })
}

// ── Mock：获取区域列表 ──────────────────────────────────────────────────
const MOCK_AREAS = [
  { AREA_CODE: '001', AREA_NAME: '观湖壹号', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
  { AREA_CODE: '002', AREA_NAME: '测试片区', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
  { AREA_CODE: '003', AREA_NAME: '政协花园', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
  { AREA_CODE: '004', AREA_NAME: '成南新居', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
  { AREA_CODE: '005', AREA_NAME: '天府新区商业街', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
  { AREA_CODE: '006', AREA_NAME: '电子科大科技园', PARENT_CODE: '', MEMO: '', sign: 'ceshi' },
]

export function mockGetArea() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_AREAS } })
    }, 200)
  })
}

// ── Mock：获取表计类型 ──────────────────────────────────────────────────
const MOCK_METER_TYPES = [
  { METER_TYPE_CODE: 'YCYCB', METER_TYPE: '银川远传表', METER_CONN_MODE: 'CJ/T188', WAVE_RATE: '2400,8,1,Even', CALIBER: '2.0', MINUENT: '9999', FACT_CODE: '3809' },
  { METER_TYPE_CODE: 'JCCPU', METER_TYPE: '积成CPU水表', METER_CONN_MODE: 'CJ/T188', WAVE_RATE: '2400,8,1,Even', CALIBER: '1.6', MINUENT: '9999', FACT_CODE: '3378' },
  { METER_TYPE_CODE: 'YCWLB', METER_TYPE: '江苏远传表', METER_CONN_MODE: 'M-BUS', WAVE_RATE: '2400,8,1,None', CALIBER: '1.6', MINUENT: '9999', FACT_CODE: '3801' },
  { METER_TYPE_CODE: 'NBWLB', METER_TYPE: '宁波无线表', METER_CONN_MODE: 'LoRa', WAVE_RATE: '9600,8,1,None', CALIBER: '2.0', MINUENT: '99999', FACT_CODE: '3804' },
]

export function mockGetMeterType() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_METER_TYPES } })
    }, 200)
  })
}

// ── Mock：获取终端类型 ──────────────────────────────────────────────────
const MOCK_TERMINALS = [
  { COLL_CODE: 'jc000001', COLL_NAME: '积成集中器', FACT_CODE: '3378', LOCATION: '1213', AREA_CODE: '001', MEMO: '', sign: 'ceshi' },
  { COLL_CODE: 'jc000002', COLL_NAME: '成南新居3号集中器', FACT_CODE: '3809', LOCATION: '3栋地下室', AREA_CODE: '004', MEMO: '', sign: 'ceshi' },
  { COLL_CODE: 'jc000003', COLL_NAME: '政协花园3号楼集中器', FACT_CODE: '3809', LOCATION: '3号楼负一层', AREA_CODE: '003', MEMO: '', sign: 'ceshi' },
  { COLL_CODE: 'jc000004', COLL_NAME: '观湖壹号集中器', FACT_CODE: '0431', LOCATION: '1栋配电间', AREA_CODE: '001', MEMO: '', sign: 'ceshi' },
  { COLL_CODE: 'jc000005', COLL_NAME: '天府集中器A', FACT_CODE: '3804', LOCATION: '商业街B1', AREA_CODE: '005', MEMO: '', sign: 'ceshi' },
]

export function mockGetTerminal() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({ data: { all: MOCK_TERMINALS } })
    }, 200)
  })
}

// ── Mock：根据时间及客户号返回最新抄表数据 ──────────────────────────────
const MOCK_READINGS_BY_TIME = {
  '100366': [
    { customer_code: '100366', steal_no: '6145200011', read_date: '2015-09-15', read_val: 123.45 },
    { customer_code: '100366', steal_no: '6145200011', read_date: '2015-10-15', read_val: 145.60 },
  ],
  '100399': [
    { customer_code: '100399', steal_no: '6145200010', read_date: '2015-09-15', read_val: 256.00 },
  ],
}

export function mockGetMeterReadingByTime(row) {
  return new Promise(resolve => {
    setTimeout(() => {
      const all = []
      for (const item of row || []) {
        const readings = MOCK_READINGS_BY_TIME[item.customer_code]
        if (readings) {
          const match = readings.find(r => r.read_date === item.read_date)
          if (match) {
            all.push(match)
          } else {
            all.push({ customer_code: item.customer_code, read_date: item.read_date, read_val: null, steal_no: readings[0]?.steal_no || '' })
          }
        } else {
          all.push({ customer_code: item.customer_code, read_date: item.read_date, read_val: null, steal_no: '' })
        }
      }
      resolve({ data: { all } })
    }, 300)
  })
}
