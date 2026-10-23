export const physicalMeters = [
  {
    id: '192251700194',
    group: '计量',
    merchant: '物业-消火栓泵(主用)',
    station: 'C32023003912',
    meterType: '公共表',
    commStatus: 'online',
    powerStatus: 'on',
    todayKwh: 0.8,
    balance: null,
  },
  {
    id: '192251700243',
    group: '计量',
    merchant: '物业-B13-消防风机(备用)',
    station: 'C32023004867',
    meterType: '公共表',
    commStatus: 'online',
    powerStatus: 'on',
    todayKwh: 0,
    balance: null,
  },
  {
    id: '192251700410',
    group: '电子科大科技园天府园',
    merchant: '8WL2-5(备用)',
    station: 'C32023003912',
    meterType: '总表',
    commStatus: 'online',
    powerStatus: 'on',
    todayKwh: 0,
    balance: null,
  },
  {
    id: '192251700420',
    group: '计费',
    merchant: 'B16-普通电梯(主用)',
    station: 'C32023005555',
    meterType: '商户',
    commStatus: 'online',
    powerStatus: 'on',
    todayKwh: 9.2,
    balance: null,
  },
]

export const meterStats = [
  { label: '总数', value: '517', unit: '只' },
  { label: '在线', value: '507', unit: '只' },
  { label: '离线', value: '10', unit: '只' },
  { label: '停用', value: '0', unit: '只' },
  { label: '预付费', value: '311', unit: '只' },
  { label: '后付费', value: '0', unit: '只' },
  { label: '低余额', value: '0', unit: '只' },
  { label: '分摊', value: '3', unit: '只' },
  { label: '合同', value: '514', unit: '只' },
]

export const projectTreeNodes = [
  {
    id: 'tianchen',
    label: '天辰科技园',
    icon: 'location_city',
    iconColor: 'text-primary',
    expanded: true,
    children: [
      { id: 'building-1', label: '1号办公楼', icon: 'domain', active: true },
      { id: 'building-2', label: '2号研发楼', icon: 'domain' },
      { id: 'building-3', label: '配套公寓 A', icon: 'domain' },
    ]
  },
  {
    id: 'binhai',
    label: '滨海住宅区',
    icon: 'location_city',
    expanded: false,
    children: []
  }
]

export const virtualMeters = [
  { id: 'VM-001', group: '计量', merchant: '1号楼总表', meterCount: 48, paymentType: '预付费', balance: '12,580.00', overdraft: '-5,000.00', alarmAmount: '1,000.00', limitPayment: '否', createdAt: '2024-06-15 14:32:01' },
  { id: 'VM-002', group: '计费', merchant: '2号楼总表', meterCount: 36, paymentType: '后付费', balance: '8,320.00', overdraft: '-10,000.00', alarmAmount: '2,000.00', limitPayment: '是', createdAt: '2024-07-20 09:15:33' },
  { id: 'VM-003', group: '电子科大科技园天府园', merchant: '园区总表', meterCount: 120, paymentType: '预付费', balance: '45,100.00', overdraft: '-20,000.00', alarmAmount: '5,000.00', limitPayment: '否', createdAt: '2024-03-01 11:08:17' },
  { id: 'VM-004', group: '计量', merchant: '3号研发楼总表', meterCount: 64, paymentType: '预付费', balance: '23,450.00', overdraft: '-8,000.00', alarmAmount: '3,000.00', limitPayment: '否', createdAt: '2024-08-10 16:45:52' },
  { id: 'VM-005', group: '计费', merchant: '配套公寓A总表', meterCount: 80, paymentType: '后付费', balance: '6,780.00', overdraft: '-15,000.00', alarmAmount: '1,500.00', limitPayment: '是', createdAt: '2024-09-05 08:22:40' },
  { id: 'VM-006', group: '计量', merchant: '商业中心总表', meterCount: 32, paymentType: '预付费', balance: '18,200.00', overdraft: '-3,000.00', alarmAmount: '2,500.00', limitPayment: '否', createdAt: '2024-10-18 13:56:09' },
]

export const deletedMeters = [
  { id: '201912150001', group: '计量', merchant: '旧厂区-101照明', powerStatus: '合闸', commStatus: '离线', ctRatio: '200/5', ptRatio: '10/0.1', totalKw: 12.5, overdraft: '-500.00', balance: '320.00', deletedAt: '2024-04-15 09:30:22' },
  { id: '202008120045', group: '计费', merchant: '3号旧楼-205空调', powerStatus: '拉闸', commStatus: '离线', ctRatio: '150/5', ptRatio: '10/0.1', totalKw: 0, overdraft: '-1,000.00', balance: '-85.50', deletedAt: '2024-04-10 14:22:08' },
  { id: '202105200089', group: 'B13栋', merchant: '公寓B-307总表', powerStatus: '合闸', commStatus: '在线', ctRatio: '100/5', ptRatio: '10/0.1', totalKw: 8.2, overdraft: '-300.00', balance: '156.00', deletedAt: '2024-03-28 11:05:41' },
  { id: '202201060112', group: '计量', merchant: '1号楼地下车库', powerStatus: '拉闸', commStatus: '离线', ctRatio: '200/5', ptRatio: '10/0.1', totalKw: 0, overdraft: '-800.00', balance: '0.00', deletedAt: '2024-02-15 16:48:33' },
  { id: '202207180234', group: '电子科大科技园天府园', merchant: '商业街-102商铺', powerStatus: '合闸', commStatus: '在线', ctRatio: '150/5', ptRatio: '10/0.1', totalKw: 5.6, overdraft: '-200.00', balance: '45.20', deletedAt: '2024-01-20 10:12:55' },
  { id: '202303220056', group: '计费', merchant: '2号研发楼-电梯', powerStatus: '拉闸', commStatus: '离线', ctRatio: '300/5', ptRatio: '10/0.1', totalKw: 0, overdraft: '-2,000.00', balance: '-320.00', deletedAt: '2023-12-05 08:30:17' },
  { id: '202309150078', group: 'B17栋', merchant: '公寓A-105照明', powerStatus: '合闸', commStatus: '在线', ctRatio: '100/5', ptRatio: '10/0.1', totalKw: 3.8, overdraft: '-150.00', balance: '78.50', deletedAt: '2023-11-10 15:45:09' },
  { id: '202311080145', group: '计量', merchant: '3号楼-消防水泵', powerStatus: '合闸', commStatus: '离线', ctRatio: '400/5', ptRatio: '10/0.1', totalKw: 28.0, overdraft: '-5,000.00', balance: '1,200.00', deletedAt: '2023-10-28 17:20:44' },
]

// ── Deleted meter detail (for the 详情 modal) ──────────────────────
export const deletedMeterDetailMap = {
  '201912150001': {
    basic: { merchant: '旧厂区-101照明', project: '天辰科技园', meterType: '分表', initialReading: '0.00', initialTime: '2020-06-01 00:00:00', billingReading: '12580.50', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2024-03-15 10:00:00', createdAt: '2020-06-01 10:30:00', creator: '张三', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '三相四线', iccid: '89860426102071401001', signalStrength: 8, reportInterval: 15, power: 12.5, current: '35.2A', voltage: '380V', forwardActive: '12580.50', reverseActive: '0.00', updatedAt: '2024-04-15 09:28:00', initialDisplay: '0.00' }
  },
  '202008120045': {
    basic: { merchant: '3号旧楼-205空调', project: '滨海住宅区', meterType: '总表', initialReading: '1250.00', initialTime: '2020-08-15 00:00:00', billingReading: '8920.30', peak: '09:00-21:00', flat: '21:00-24:00', valley: '00:00-09:00', lastBillingTime: '2024-03-10 10:00:00', createdAt: '2020-08-15 14:20:00', creator: '李四', faultInfo: '计量芯片异常', rateSupport: '尖峰平谷' },
    status: { meterType2: '三相四线', iccid: '89860426102071402002', signalStrength: 0, reportInterval: 30, power: 0, current: '0A', voltage: '380V', forwardActive: '8920.30', reverseActive: '0.00', updatedAt: '2024-04-10 14:20:00', initialDisplay: '1250.00' }
  },
  '202105200089': {
    basic: { merchant: '公寓B-307总表', project: '天辰科技园', meterType: '总表', initialReading: '0.00', initialTime: '2021-05-25 00:00:00', billingReading: '4520.80', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2024-03-20 10:00:00', createdAt: '2021-05-25 09:00:00', creator: '张三', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '单相', iccid: '89860426102071403003', signalStrength: 12, reportInterval: 15, power: 8.2, current: '22.5A', voltage: '220V', forwardActive: '4520.80', reverseActive: '0.00', updatedAt: '2024-03-28 11:03:00', initialDisplay: '0.00' }
  },
  '202201060112': {
    basic: { merchant: '1号楼地下车库', project: '天辰科技园', meterType: '分表', initialReading: '500.00', initialTime: '2022-01-10 00:00:00', billingReading: '3200.00', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2024-01-15 10:00:00', createdAt: '2022-01-10 08:30:00', creator: '王五', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '三相四线', iccid: '89860426102071404004', signalStrength: 0, reportInterval: 20, power: 0, current: '0A', voltage: '380V', forwardActive: '3200.00', reverseActive: '0.00', updatedAt: '2024-02-15 16:45:00', initialDisplay: '500.00' }
  },
  '202207180234': {
    basic: { merchant: '商业街-102商铺', project: '电子科大科技园天府园', meterType: '分表', initialReading: '0.00', initialTime: '2022-07-20 00:00:00', billingReading: '1850.60', peak: '09:00-21:00', flat: '21:00-24:00', valley: '00:00-09:00', lastBillingTime: '2023-12-20 10:00:00', createdAt: '2022-07-20 11:00:00', creator: '李四', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '单相', iccid: '89860426102071405005', signalStrength: 15, reportInterval: 10, power: 5.6, current: '16.8A', voltage: '220V', forwardActive: '1850.60', reverseActive: '0.00', updatedAt: '2024-01-20 10:10:00', initialDisplay: '0.00' }
  },
  '202303220056': {
    basic: { merchant: '2号研发楼-电梯', project: '电子科大科技园天府园', meterType: '总表', initialReading: '300.00', initialTime: '2023-03-25 00:00:00', billingReading: '15600.00', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2023-11-05 10:00:00', createdAt: '2023-03-25 15:00:00', creator: '赵六', faultInfo: 'CT二次侧开路', rateSupport: '尖峰平谷' },
    status: { meterType2: '三相四线', iccid: '89860426102071406006', signalStrength: 0, reportInterval: 30, power: 0, current: '0A', voltage: '380V', forwardActive: '15600.00', reverseActive: '0.00', updatedAt: '2023-12-05 08:28:00', initialDisplay: '300.00' }
  },
  '202309150078': {
    basic: { merchant: '公寓A-105照明', project: '滨海住宅区', meterType: '分表', initialReading: '0.00', initialTime: '2023-09-20 00:00:00', billingReading: '980.30', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2023-10-10 10:00:00', createdAt: '2023-09-20 10:30:00', creator: '张三', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '单相', iccid: '89860426102071407007', signalStrength: 10, reportInterval: 15, power: 3.8, current: '11.2A', voltage: '220V', forwardActive: '980.30', reverseActive: '0.00', updatedAt: '2023-11-10 15:43:00', initialDisplay: '0.00' }
  },
  '202311080145': {
    basic: { merchant: '3号楼-消防水泵', project: '天辰科技园', meterType: '总表', initialReading: '0.00', initialTime: '2023-11-10 00:00:00', billingReading: '28000.00', peak: '08:00-22:00', flat: '22:00-24:00', valley: '00:00-08:00', lastBillingTime: '2023-10-25 10:00:00', createdAt: '2023-11-10 09:00:00', creator: '王五', faultInfo: '无', rateSupport: '尖峰平谷' },
    status: { meterType2: '三相四线', iccid: '89860426102071408008', signalStrength: 5, reportInterval: 15, power: 28.0, current: '75.0A', voltage: '380V', forwardActive: '28000.00', reverseActive: '0.00', updatedAt: '2023-10-28 17:18:00', initialDisplay: '0.00' }
  },
}

// ── Operation records per meter ────────────────────────────────────
export const operationRecords = {
  '201912150001': [
    { merchant: '旧厂区-101照明', meterId: '201912150001', operatorSide: '管理端', eventType: '电价信息', detail: '修改尖峰电价 1.20→1.35', result: '成功', operator: '张三', operatedAt: '2024-03-15 10:05:33', remark: '季度调价' },
    { merchant: '旧厂区-101照明', meterId: '201912150001', operatorSide: '管理端', eventType: '基本信息', detail: '修改表计类型 总表→分表', result: '成功', operator: '张三', operatedAt: '2024-02-20 14:22:10', remark: '' },
    { merchant: '旧厂区-101照明', meterId: '201912150001', operatorSide: '微信端', eventType: '消息推送', detail: '余额不足提醒', result: '成功', operator: '系统', operatedAt: '2024-02-01 09:00:00', remark: '自动触发' },
  ],
  '202008120045': [
    { merchant: '3号旧楼-205空调', meterId: '202008120045', operatorSide: '管理端', eventType: '状态信息', detail: '标记为通讯离线', result: '成功', operator: '李四', operatedAt: '2024-04-10 14:20:00', remark: '设备已拆除' },
  ],
  '202105200089': [
    { merchant: '公寓B-307总表', meterId: '202105200089', operatorSide: '管理端', eventType: '时段操作', detail: '修改峰谷时段 08:00-22:00→09:00-21:00', result: '成功', operator: '王五', operatedAt: '2024-02-10 16:45:22', remark: '' },
    { merchant: '公寓B-307总表', meterId: '202105200089', operatorSide: '管理端', eventType: '电价信息', detail: '修改谷段电价 0.30→0.35', result: '成功', operator: '王五', operatedAt: '2024-01-15 11:30:08', remark: '' },
  ],
}

export const stationList = [
  { id: 'ST-001', name: '1号楼集中器', model: 'DJGZ23-TF', commStatus: '在线', signalStrength: '强', meters: 64, reportTime: '2026-05-13 14:32:01', creator: '张三', status: '启用' },
  { id: 'ST-002', name: '2号楼采集器-A', model: 'CJQ-102B', commStatus: '在线', signalStrength: '中', meters: 32, reportTime: '2026-05-13 14:31:55', creator: '李四', status: '启用' },
  { id: 'ST-003', name: '3号楼集中器', model: 'DJGZ23-TF', commStatus: '在线', signalStrength: '强', meters: 48, reportTime: '2026-05-13 14:30:12', creator: '张三', status: '启用' },
  { id: 'ST-004', name: '配套公寓采集器', model: 'CJQ-301A', commStatus: '离线', signalStrength: '--', meters: 16, reportTime: '2026-05-12 08:15:44', creator: '王五', status: '停用' },
  { id: 'ST-005', name: '商业中心集中器', model: 'DJGZ23-TF', commStatus: '在线', signalStrength: '强', meters: 80, reportTime: '2026-05-13 14:33:20', creator: '李四', status: '启用' },
  { id: 'ST-006', name: '研发楼采集器-B', model: 'CJQ-102B', commStatus: '在线', signalStrength: '弱', meters: 24, reportTime: '2026-05-13 14:28:07', creator: '王五', status: '启用' },
  { id: 'ST-007', name: '地下车库采集器', model: 'CJQ-301A', commStatus: '在线', signalStrength: '中', meters: 12, reportTime: '2026-05-13 14:29:55', creator: '张三', status: '启用' },
  { id: 'ST-008', name: '8号办公楼集中器', model: 'DJGZ23-TF', commStatus: '离线', signalStrength: '--', meters: 56, reportTime: '2026-05-11 18:42:33', creator: '赵六', status: '启用' },
  { id: 'ST-009', name: '消防泵房采集器', model: 'CJQ-102B', commStatus: '在线', signalStrength: '强', meters: 6, reportTime: '2026-05-13 14:31:18', creator: '赵六', status: '启用' },
  { id: 'ST-010', name: '配电房集中器', model: 'DJGZ23-TF', commStatus: '在线', signalStrength: '强', meters: 96, reportTime: '2026-05-13 14:33:45', creator: '李四', status: '启用' },
]

// ── Station detail (extended info for the detail modal) ────────────
export const stationDetailMap = {
  'ST-001': { name: 'B13-5F 配电间', stationNo: 'C32023000601', model: '电表 4G 微型基站', iccid: '89860426102071408687', version: 'S0.1.4', signalStrength: 13, commStatus: '在线', updateCycle: 15, temperature: 31, createdAt: '2024-04-16 17:05:06', creator: '何坤金' },
  'ST-002': { name: '2号楼1层弱电间', stationNo: 'C32023004867', model: '电表 4G 微型基站', iccid: '89860426102071412345', version: 'S0.1.5', signalStrength: 8, commStatus: '在线', updateCycle: 10, temperature: 28, createdAt: '2024-05-20 09:22:15', creator: '何坤金' },
  'ST-003': { name: '3号楼B1层配电室', stationNo: 'C32023005555', model: '电表 4G 微型基站', iccid: '89860426102071423456', version: 'S0.1.4', signalStrength: 15, commStatus: '在线', updateCycle: 15, temperature: 33, createdAt: '2024-06-10 14:08:42', creator: '何坤金' },
  'ST-004': { name: '配套公寓A栋电井', stationNo: 'C32023006789', model: '电表 4G 微型基站', iccid: '89860426102071434567', version: 'S0.1.3', signalStrength: 0, commStatus: '离线', updateCycle: 20, temperature: 26, createdAt: '2024-03-08 11:30:00', creator: '李文' },
  'ST-005': { name: '商业中心B2配电房', stationNo: 'C32023007890', model: '电表 4G 微型基站', iccid: '89860426102071445678', version: 'S0.1.5', signalStrength: 18, commStatus: '在线', updateCycle: 10, temperature: 35, createdAt: '2024-07-22 16:45:30', creator: '何坤金' },
  'ST-006': { name: '研发楼3层弱电井', stationNo: 'C32023008901', model: '电表 4G 微型基站', iccid: '89860426102071456789', version: 'S0.1.4', signalStrength: 5, commStatus: '在线', updateCycle: 15, temperature: 29, createdAt: '2024-08-15 08:12:55', creator: '李文' },
  'ST-007': { name: '地下车库B1层', stationNo: 'C32023009012', model: '电表 4G 微型基站', iccid: '89860426102071467890', version: 'S0.1.4', signalStrength: 9, commStatus: '在线', updateCycle: 20, temperature: 24, createdAt: '2024-09-01 13:20:10', creator: '何坤金' },
  'ST-008': { name: '8号办公楼顶层机房', stationNo: 'C32023010123', model: '电表 4G 微型基站', iccid: '89860426102071478901', version: 'S0.1.2', signalStrength: 2, commStatus: '离线', updateCycle: 30, temperature: 38, createdAt: '2024-02-28 10:05:33', creator: '赵六' },
  'ST-009': { name: '消防泵房控制室', stationNo: 'C32023011234', model: '电表 4G 微型基站', iccid: '89860426102071489012', version: 'S0.1.5', signalStrength: 20, commStatus: '在线', updateCycle: 10, temperature: 27, createdAt: '2024-10-05 15:38:20', creator: '赵六' },
  'ST-010': { name: '总配电房高压室', stationNo: 'C32023012345', model: '电表 4G 微型基站', iccid: '89860426102071490123', version: 'S0.1.5', signalStrength: 22, commStatus: '在线', updateCycle: 10, temperature: 36, createdAt: '2024-01-15 09:00:00', creator: '何坤金' },
}

// ── Bound meters per station ───────────────────────────────────────
export const stationBoundMeters = {
  'ST-001': [
    { group: 'B13栋', merchant: 'B13-5F 普通照明总', id: '192307001648', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '0.00', todayKwh: 74, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
    { group: 'B13栋', merchant: 'B13-5F 空调总', id: '192307001649', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '152.30', todayKwh: 128, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
  ],
  'ST-002': [
    { group: '计量', merchant: '2号楼公共照明', id: '192307001650', meterStatus: '正常', model: '单相电能表 RS485', balance: '45.00', todayKwh: 32, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
  ],
  'ST-003': [
    { group: '计费', merchant: '3号楼电梯(主用)', id: '192307001651', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '89.50', todayKwh: 56, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
    { group: '计费', merchant: '3号楼电梯(备用)', id: '192307001652', meterStatus: '告警', model: '三相互感电能表 RS485', balance: '-12.00', todayKwh: 18, paymentType: '后付费', commStatus: '在线', powerStatus: '合闸' },
    { group: '计量', merchant: '3号楼公共照明', id: '192307001653', meterStatus: '正常', model: '单相电能表 RS485', balance: '67.80', todayKwh: 41, paymentType: '预付费', commStatus: '离线', powerStatus: '合闸' },
  ],
  'ST-004': [],
  'ST-005': [
    { group: '计费', merchant: '商业中心-1F商铺总', id: '192307001654', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '234.00', todayKwh: 95, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
  ],
  'ST-006': [
    { group: '计量', merchant: '研发楼3层照明', id: '192307001655', meterStatus: '正常', model: '单相电能表 RS485', balance: '12.00', todayKwh: 8, paymentType: '预付费', commStatus: '在线', powerStatus: '合闸' },
  ],
  'ST-007': [],
  'ST-008': [
    { group: '计量', merchant: '8号楼办公照明', id: '192307001656', meterStatus: '离线', model: '单相电能表 RS485', balance: '200.00', todayKwh: 0, paymentType: '预付费', commStatus: '离线', powerStatus: '拉闸' },
  ],
  'ST-009': [],
  'ST-010': [
    { group: '计量', merchant: '总配电房监测表', id: '192307001657', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '0.00', todayKwh: 210, paymentType: '后付费', commStatus: '在线', powerStatus: '合闸' },
    { group: '计费', merchant: '高压室进线总表', id: '192307001658', meterStatus: '正常', model: '三相互感电能表 RS485', balance: '0.00', todayKwh: 180, paymentType: '后付费', commStatus: '在线', powerStatus: '合闸' },
  ],
}

// ── Water meter management ───────────────────────────────────────────
export const waterMeters = [
  { id: 'WM20230501001', group: '电子科大科技园天府园', merchant: '张伟', stationId: 'C32023003912', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '是', commStatus: 'online', deductionType: '预付费', balance: 1250.30, forwardTotal: 15230.5, yesterdayUsage: 2.8, alertLimit: 50, overdraftLimit: 100, limitPayment: '否', batteryLevel: '85%', statusUpdateTime: '2026-05-17 10:23:15' },
  { id: 'WM20230501002', group: '电子科大科技园天府园', merchant: '李娜', stationId: 'C32023004867', model: 'LXSY-20', valveStatus: '开阀', autoSwitch: '否', commStatus: 'online', deductionType: '预付费', balance: 856.80, forwardTotal: 11250.3, yesterdayUsage: 1.5, alertLimit: 30, overdraftLimit: 80, limitPayment: '否', batteryLevel: '72%', statusUpdateTime: '2026-05-17 09:45:32' },
  { id: 'WM20230501003', group: 'B13栋', merchant: '王强', stationId: 'C32023005555', model: 'LXSY-25', valveStatus: '关阀', autoSwitch: '是', commStatus: 'offline', deductionType: '预付费', balance: -85.20, forwardTotal: 8456.7, yesterdayUsage: 0, alertLimit: 40, overdraftLimit: 100, limitPayment: '是', batteryLevel: '31%', statusUpdateTime: '2026-05-16 18:12:08' },
  { id: 'WM20230501004', group: 'B13栋', merchant: '赵丽', stationId: 'C32023003912', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '否', commStatus: 'online', deductionType: '后付费', balance: 2340.50, forwardTotal: 18760.2, yesterdayUsage: 3.2, alertLimit: 0, overdraftLimit: 0, limitPayment: '否', batteryLevel: '90%', statusUpdateTime: '2026-05-17 11:05:44' },
  { id: 'WM20230501005', group: 'B15栋', merchant: '孙磊', stationId: 'C32023004867', model: 'LXSY-20', valveStatus: '开阀', autoSwitch: '是', commStatus: 'online', deductionType: '预付费', balance: 45.60, forwardTotal: 9870.4, yesterdayUsage: 4.1, alertLimit: 50, overdraftLimit: 150, limitPayment: '否', batteryLevel: '68%', statusUpdateTime: '2026-05-17 12:30:21' },
  { id: 'WM20230501006', group: 'B15栋', merchant: '周明', stationId: 'C32023005555', model: 'LXSY-15', valveStatus: '关阀', autoSwitch: '否', commStatus: 'online', deductionType: '预付费', balance: -320.75, forwardTotal: 6540.8, yesterdayUsage: 0, alertLimit: 30, overdraftLimit: 80, limitPayment: '是', batteryLevel: '55%', statusUpdateTime: '2026-05-17 08:15:09' },
  { id: 'WM20230501007', group: 'B16栋', merchant: '陈芳', stationId: 'C32023003912', model: 'LXSY-25', valveStatus: '开阀', autoSwitch: '是', commStatus: 'online', deductionType: '后付费', balance: 5670.20, forwardTotal: 25430.1, yesterdayUsage: 5.6, alertLimit: 0, overdraftLimit: 0, limitPayment: '否', batteryLevel: '93%', statusUpdateTime: '2026-05-17 14:22:50' },
  { id: 'WM20230501008', group: 'B16栋', merchant: '吴杰', stationId: 'C32023004867', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '否', commStatus: 'offline', deductionType: '预付费', balance: 12.30, forwardTotal: 4320.6, yesterdayUsage: 0, alertLimit: 20, overdraftLimit: 50, limitPayment: '否', batteryLevel: '22%', statusUpdateTime: '2026-05-15 16:48:33' },
  { id: 'WM20230501009', group: 'B17栋', merchant: '郑伟', stationId: 'C32023005555', model: 'LXSY-20', valveStatus: '开阀', autoSwitch: '是', commStatus: 'online', deductionType: '预付费', balance: 789.45, forwardTotal: 10980.3, yesterdayUsage: 2.3, alertLimit: 40, overdraftLimit: 120, limitPayment: '否', batteryLevel: '76%', statusUpdateTime: '2026-05-17 13:55:12' },
  { id: 'WM20230501010', group: 'B17栋', merchant: '刘洋', stationId: 'C32023003912', model: 'LXSY-15', valveStatus: '关阀', autoSwitch: '否', commStatus: 'online', deductionType: '后付费', balance: -150.80, forwardTotal: 7650.9, yesterdayUsage: 0, alertLimit: 0, overdraftLimit: 0, limitPayment: '是', batteryLevel: '45%', statusUpdateTime: '2026-05-17 07:30:45' },
  { id: 'WM20230501011', group: '电子科大科技园天府园', merchant: '黄敏', stationId: 'C32023004867', model: 'LXSY-25', valveStatus: '开阀', autoSwitch: '是', commStatus: 'online', deductionType: '预付费', balance: 3456.70, forwardTotal: 22340.5, yesterdayUsage: 6.8, alertLimit: 100, overdraftLimit: 300, limitPayment: '否', batteryLevel: '88%', statusUpdateTime: '2026-05-17 15:10:38' },
  { id: 'WM20230501012', group: 'B15栋', merchant: '马超', stationId: 'C32023005555', model: 'LXSY-20', valveStatus: '开阀', autoSwitch: '否', commStatus: 'offline', deductionType: '预付费', balance: -45.30, forwardTotal: 5230.7, yesterdayUsage: 0, alertLimit: 25, overdraftLimit: 60, limitPayment: '是', batteryLevel: '18%', statusUpdateTime: '2026-05-14 22:05:16' },
]

// ── Water virtual meter management ───────────────────────────────────
export const waterVirtualMeters = [
  { id: 'WVM-001', group: '电子科大科技园天府园', merchant: '园区总水表', meterCount: 120, paymentType: '预付费', balance: 45100.00, overdraft: -20000.00, alarmAmount: 5000.00, limitPayment: '否', createdAt: '2024-03-01 11:08:17' },
  { id: 'WVM-002', group: 'B13栋', merchant: 'B13栋总水表', meterCount: 48, paymentType: '预付费', balance: 12580.00, overdraft: -5000.00, alarmAmount: 1000.00, limitPayment: '否', createdAt: '2024-06-15 14:32:01' },
  { id: 'WVM-003', group: 'B15栋', merchant: 'B15栋总水表', meterCount: 36, paymentType: '后付费', balance: 8320.00, overdraft: -10000.00, alarmAmount: 2000.00, limitPayment: '是', createdAt: '2024-07-20 09:15:33' },
  { id: 'WVM-004', group: 'B16栋', merchant: 'B16栋总水表', meterCount: 64, paymentType: '预付费', balance: 23450.00, overdraft: -8000.00, alarmAmount: 3000.00, limitPayment: '否', createdAt: '2024-08-10 16:45:52' },
  { id: 'WVM-005', group: 'B17栋', merchant: 'B17栋总水表', meterCount: 32, paymentType: '预付费', balance: 6780.00, overdraft: -15000.00, alarmAmount: 1500.00, limitPayment: '是', createdAt: '2024-09-05 08:22:40' },
  { id: 'WVM-006', group: '电子科大科技园天府园', merchant: '商业中心总水表', meterCount: 80, paymentType: '预付费', balance: 18200.00, overdraft: -3000.00, alarmAmount: 2500.00, limitPayment: '否', createdAt: '2024-10-18 13:56:09' },
]

// ── Water station management ─────────────────────────────────────────
export const waterStationList = [
  { id: 'WST-001', name: '1号楼水表集中器', model: 'SGZ23-WF', commStatus: '在线', meters: 48, createdAt: '2024-03-15 09:30:22', creator: '张三', status: '启用' },
  { id: 'WST-002', name: '2号楼水表采集器-A', model: 'SCQ-202B', commStatus: '在线', meters: 32, createdAt: '2024-05-20 14:15:08', creator: '李四', status: '启用' },
  { id: 'WST-003', name: '3号楼水表集中器', model: 'SGZ23-WF', commStatus: '在线', meters: 56, createdAt: '2024-06-10 10:45:33', creator: '张三', status: '启用' },
  { id: 'WST-004', name: '配套公寓水表采集器', model: 'SCQ-301A', commStatus: '离线', meters: 16, createdAt: '2024-07-08 08:20:17', creator: '王五', status: '启用' },
  { id: 'WST-005', name: '商业中心水表集中器', model: 'SGZ23-WF', commStatus: '在线', meters: 72, createdAt: '2024-08-22 16:55:41', creator: '李四', status: '启用' },
  { id: 'WST-006', name: '研发楼水表采集器-B', model: 'SCQ-202B', commStatus: '在线', meters: 20, createdAt: '2024-09-14 11:30:05', creator: '王五', status: '启用' },
  { id: 'WST-007', name: '地下车库水表采集器', model: 'SCQ-301A', commStatus: '离线', meters: 8, createdAt: '2024-10-28 13:10:52', creator: '赵六', status: '停用' },
  { id: 'WST-008', name: '8号办公楼水表集中器', model: 'SGZ23-WF', commStatus: '在线', meters: 40, createdAt: '2024-11-05 09:25:18', creator: '赵六', status: '启用' },
]

export const waterStationDetailMap = {
  'WST-001': { name: '1号楼水表集中器', stationNo: 'WST-001', model: 'SGZ23-WF', commStatus: '在线', signalStrength: '强', iccid: '89860426102071501001', version: 'V2.3.1', updateCycle: 15, temperature: 38, createdAt: '2024-03-15 09:30:22', creator: '张三' },
  'WST-002': { name: '2号楼水表采集器-A', stationNo: 'WST-002', model: 'SCQ-202B', commStatus: '在线', signalStrength: '中', iccid: '89860426102071502002', version: 'V2.2.8', updateCycle: 30, temperature: 42, createdAt: '2024-05-20 14:15:08', creator: '李四' },
  'WST-003': { name: '3号楼水表集中器', stationNo: 'WST-003', model: 'SGZ23-WF', commStatus: '在线', signalStrength: '强', iccid: '89860426102071503003', version: 'V2.3.1', updateCycle: 15, temperature: 35, createdAt: '2024-06-10 10:45:33', creator: '张三' },
  'WST-004': { name: '配套公寓水表采集器', stationNo: 'WST-004', model: 'SCQ-301A', commStatus: '离线', signalStrength: '--', iccid: '89860426102071504004', version: 'V2.1.5', updateCycle: 30, temperature: 0, createdAt: '2024-07-08 08:20:17', creator: '王五' },
  'WST-005': { name: '商业中心水表集中器', stationNo: 'WST-005', model: 'SGZ23-WF', commStatus: '在线', signalStrength: '强', iccid: '89860426102071505005', version: 'V2.3.1', updateCycle: 15, temperature: 40, createdAt: '2024-08-22 16:55:41', creator: '李四' },
  'WST-006': { name: '研发楼水表采集器-B', stationNo: 'WST-006', model: 'SCQ-202B', commStatus: '在线', signalStrength: '弱', iccid: '89860426102071506006', version: 'V2.2.8', updateCycle: 30, temperature: 45, createdAt: '2024-09-14 11:30:05', creator: '王五' },
  'WST-007': { name: '地下车库水表采集器', stationNo: 'WST-007', model: 'SCQ-301A', commStatus: '离线', signalStrength: '--', iccid: '89860426102071507007', version: 'V2.1.5', updateCycle: 30, temperature: 0, createdAt: '2024-10-28 13:10:52', creator: '赵六' },
  'WST-008': { name: '8号办公楼水表集中器', stationNo: 'WST-008', model: 'SGZ23-WF', commStatus: '在线', signalStrength: '中', iccid: '89860426102071508008', version: 'V2.3.1', updateCycle: 15, temperature: 37, createdAt: '2024-11-05 09:25:18', creator: '赵六' },
}

export const waterStationBoundMeters = {
  'WST-001': [
    { id: 'WM20230501001', group: '电子科大科技园天府园', merchant: '张伟', meterStatus: '正常', model: 'LXSY-15', balance: 1250.30, todayUsage: 2.8, paymentType: '预付费', commStatus: '在线', valveStatus: '开阀' },
    { id: 'WM20230501004', group: 'B13栋', merchant: '赵丽', meterStatus: '正常', model: 'LXSY-15', balance: 2340.50, todayUsage: 3.2, paymentType: '后付费', commStatus: '在线', valveStatus: '开阀' },
    { id: 'WM20230501007', group: 'B16栋', merchant: '陈芳', meterStatus: '正常', model: 'LXSY-25', balance: 5670.20, todayUsage: 5.6, paymentType: '后付费', commStatus: '在线', valveStatus: '开阀' },
  ],
  'WST-002': [
    { id: 'WM20230501002', group: '电子科大科技园天府园', merchant: '李娜', meterStatus: '正常', model: 'LXSY-20', balance: 856.80, todayUsage: 1.5, paymentType: '预付费', commStatus: '在线', valveStatus: '开阀' },
    { id: 'WM20230501005', group: 'B15栋', merchant: '孙磊', meterStatus: '告警', model: 'LXSY-20', balance: 45.60, todayUsage: 4.1, paymentType: '预付费', commStatus: '在线', valveStatus: '开阀' },
    { id: 'WM20230501011', group: '电子科大科技园天府园', merchant: '黄敏', meterStatus: '正常', model: 'LXSY-25', balance: 3456.70, todayUsage: 6.8, paymentType: '预付费', commStatus: '在线', valveStatus: '开阀' },
  ],
  'WST-003': [
    { id: 'WM20230501003', group: 'B13栋', merchant: '王强', meterStatus: '告警', model: 'LXSY-25', balance: -85.20, todayUsage: 0, paymentType: '预付费', commStatus: '离线', valveStatus: '关阀' },
    { id: 'WM20230501006', group: 'B15栋', merchant: '周明', meterStatus: '离线', model: 'LXSY-15', balance: -320.75, todayUsage: 0, paymentType: '预付费', commStatus: '在线', valveStatus: '关阀' },
    { id: 'WM20230501009', group: 'B17栋', merchant: '郑伟', meterStatus: '正常', model: 'LXSY-20', balance: 789.45, todayUsage: 2.3, paymentType: '预付费', commStatus: '在线', valveStatus: '开阀' },
  ],
  'WST-004': [],
  'WST-005': [
    { id: 'WM20230501012', group: 'B15栋', merchant: '马超', meterStatus: '告警', model: 'LXSY-20', balance: -45.30, todayUsage: 0, paymentType: '预付费', commStatus: '离线', valveStatus: '开阀' },
    { id: 'WM20230501010', group: 'B17栋', merchant: '刘洋', meterStatus: '正常', model: 'LXSY-15', balance: -150.80, todayUsage: 0, paymentType: '后付费', commStatus: '在线', valveStatus: '关阀' },
  ],
  'WST-006': [],
  'WST-007': [],
  'WST-008': [
    { id: 'WM20230501008', group: 'B16栋', merchant: '吴杰', meterStatus: '离线', model: 'LXSY-15', balance: 12.30, todayUsage: 0, paymentType: '预付费', commStatus: '离线', valveStatus: '开阀' },
  ],
}

// ── Water deleted meter management ───────────────────────────────────
export const waterDeletedMeters = [
  { id: 'WM20201015001', group: 'B13栋', merchant: '旧园区-101水表', stationId: 'C32023001001', model: 'LXSY-15', valveStatus: '关阀', autoSwitch: '否', commStatus: '离线', deductionType: '预付费', balance: 320.00, forwardTotal: 12580.5, deletedAt: '2024-04-15 09:30:22' },
  { id: 'WM20210108002', group: 'B15栋', merchant: '3号旧楼-205水表', stationId: 'C32023001002', model: 'LXSY-20', valveStatus: '关阀', autoSwitch: '否', commStatus: '离线', deductionType: '预付费', balance: -85.50, forwardTotal: 6540.8, deletedAt: '2024-04-10 14:22:08' },
  { id: 'WM20210620003', group: 'B13栋', merchant: '公寓B-307水表', stationId: 'C32023001003', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '是', commStatus: '在线', deductionType: '预付费', balance: 156.00, forwardTotal: 4520.3, deletedAt: '2024-03-28 11:05:41' },
  { id: 'WM20220206004', group: 'B16栋', merchant: '1号楼地下车库水表', stationId: 'C32023001004', model: 'LXSY-25', valveStatus: '关阀', autoSwitch: '否', commStatus: '离线', deductionType: '后付费', balance: 0.00, forwardTotal: 8760.1, deletedAt: '2024-02-15 16:48:33' },
  { id: 'WM20220718005', group: '电子科大科技园天府园', merchant: '商业街-102水表', stationId: 'C32023001005', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '是', commStatus: '在线', deductionType: '预付费', balance: 45.20, forwardTotal: 3210.6, deletedAt: '2024-01-20 10:12:55' },
  { id: 'WM20230322006', group: 'B17栋', merchant: '2号研发楼-水表', stationId: 'C32023001006', model: 'LXSY-20', valveStatus: '关阀', autoSwitch: '否', commStatus: '离线', deductionType: '后付费', balance: -320.00, forwardTotal: 10980.4, deletedAt: '2023-12-05 08:30:17' },
  { id: 'WM20230915007', group: 'B17栋', merchant: '公寓A-105水表', stationId: 'C32023001007', model: 'LXSY-15', valveStatus: '开阀', autoSwitch: '是', commStatus: '在线', deductionType: '预付费', balance: 78.50, forwardTotal: 2340.9, deletedAt: '2023-11-10 15:45:09' },
  { id: 'WM20231108008', group: 'B15栋', merchant: '3号楼-消防水表', stationId: 'C32023001008', model: 'LXSY-25', valveStatus: '开阀', autoSwitch: '是', commStatus: '离线', deductionType: '预付费', balance: 1200.00, forwardTotal: 28760.5, deletedAt: '2023-10-28 17:20:44' },
]
