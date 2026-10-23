export const rechargeRecords = [
  { id: 1, group: '计费', meterId: '192251700410', merchantName: '8WL2-5(备用)', userName: '张三', orderNo: 'TXN20260501001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 7000.00, transTime: '2026-05-01 09:15:22', remark: '2026年4月电费充值，银行转账' },
  { id: 2, group: '计费', meterId: '202309150203', merchantName: '科技园食堂', userName: '李四', orderNo: 'TXN20260503002', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 8500.00, transTime: '2026-05-03 14:30:08', remark: '5月份电费预存' },
  { id: 3, group: '计费', meterId: '202309150042', merchantName: '咖啡角', userName: '王五', orderNo: 'TXN20260507003', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 3200.00, transTime: '2026-05-07 10:42:55', remark: '转账时间 2026-05-07 10:30' },
  { id: 4, group: '计费', meterId: '202309150118', merchantName: '天辰超市', userName: '张三', orderNo: 'TXN20260509004', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 12000.00, transTime: '2026-05-09 16:18:33', remark: '转账时间 2026-05-09 16:00，含4-5月电费' },
  { id: 5, group: '计费', meterId: '202309150367', merchantName: '洗衣房', userName: '赵六', orderNo: 'TXN20260511005', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 9663.05, transTime: '2026-05-11 11:25:40', remark: '补缴3月差额' },
  { id: 6, group: '计费', meterId: '202309150001', merchantName: '便利店', userName: '李四', orderNo: 'TXN20260513006', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 5000.00, transTime: '2026-05-13 08:55:17', remark: '转账时间 2026-05-13 08:50' },
]

export const refundRecords = [
  { id: 'REF-20240315001', applyTime: '2024-03-15 10:30:00', merchantName: '已注销商户A', meterId: '201912150001', contactName: '张伟', contactPhone: '138****1234', refundAmount: 128.50, reviewStatus: '已通过', reviewTime: '2024-03-16 14:20:00' },
  { id: 'REF-20240320002', applyTime: '2024-03-20 09:15:00', merchantName: '已迁出商户B', meterId: '202008120045', contactName: '李华', contactPhone: '139****5678', refundAmount: 45.20, reviewStatus: '已驳回', reviewTime: '2024-03-21 11:00:00' },
  { id: 'REF-20240408003', applyTime: '2024-04-08 16:20:00', merchantName: '预存过多商户C', meterId: '202103150056', contactName: '王小明', contactPhone: '136****9012', refundAmount: 500.00, reviewStatus: '已通过', reviewTime: '2024-04-09 09:30:00' },
  { id: 'REF-20240412004', applyTime: '2024-04-12 14:00:00', merchantName: '咖啡角', meterId: '202309150042', contactName: '赵丽', contactPhone: '137****3456', refundAmount: 200.00, reviewStatus: '待审核', reviewTime: '-' },
  { id: 'REF-20240505005', applyTime: '2024-05-05 11:30:00', merchantName: '天辰超市', meterId: '202309150118', contactName: '孙磊', contactPhone: '135****7890', refundAmount: 350.00, reviewStatus: '已通过', reviewTime: '2024-05-06 08:15:00' },
  { id: 'REF-20240510006', applyTime: '2024-05-10 08:45:00', merchantName: '洗衣房', meterId: '202309150367', contactName: '周敏', contactPhone: '133****2345', refundAmount: 80.00, reviewStatus: '已驳回', reviewTime: '2024-05-11 16:40:00' },
]

export const balanceAdjustmentSummary = {
  totalHouseholds: 37,
  refundCount: 11,
  refundAmount: -51401.75,
  rechargeCount: 25,
  rechargeAmount: 66252.96,
  netChange: 14851.21,
}

export const balanceAdjustments = [
  { id: 'BA-20260501001', groupName: '电子科大科技园天府园', meterId: '202309150001', merchantName: '天辰超市', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -2850.00, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-01 09:30:00', creator: 'Admin_Li', remark: '电价政策调整，退还4月差价' },
  { id: 'BA-20260501002', groupName: '电子科大科技园天府园', meterId: '202309150042', merchantName: '科技园食堂', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -4120.50, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-01 09:35:00', creator: 'Admin_Li', remark: '电价政策调整，退还4月差价' },
  { id: 'BA-20260501003', groupName: '天辰科技园', meterId: '202309150118', merchantName: '咖啡角', originalPrice: 0.9000, originalSurcharge: 0.0300, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: -1520.80, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-01 10:00:00', creator: 'Admin_Wang', remark: '附加电价下调，退还差额' },
  { id: 'BA-20260501004', groupName: '天辰科技园', meterId: '202309150203', merchantName: '洗衣房', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -2350.25, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-01 10:15:00', creator: 'Admin_Wang', remark: '电价政策调整，退还4月差价' },
  { id: 'BA-20260502001', groupName: '滨海住宅区', meterId: '202309150367', merchantName: '便利店', originalPrice: 0.8500, originalSurcharge: 0.0100, adjustPrice: 0.8800, adjustSurcharge: 0.0200, adjustAmount: 3850.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-02 08:30:00', creator: 'Admin_Zhang', remark: '谷段电价上调，补收1-4月差额' },
  { id: 'BA-20260502002', groupName: '滨海住宅区', meterId: '202309150482', merchantName: '商业街-102', originalPrice: 0.8500, originalSurcharge: 0.0100, adjustPrice: 0.8800, adjustSurcharge: 0.0200, adjustAmount: 5210.50, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-02 08:45:00', creator: 'Admin_Zhang', remark: '谷段电价上调，补收1-4月差额' },
  { id: 'BA-20260503001', groupName: '电子科大科技园天府园', meterId: '192251700410', merchantName: '8WL2-5(备用)', originalPrice: 0.9000, originalSurcharge: 0.0300, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: -6800.00, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-03 14:00:00', creator: 'Admin_Li', remark: '对方沟通错误，退还多收电费' },
  { id: 'BA-20260504001', groupName: '天辰科技园', meterId: '202309150001', merchantName: '天辰超市', originalPrice: 0.8200, originalSurcharge: 0.0100, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: 1950.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-04 10:20:00', creator: 'Admin_Chen', remark: '历史用电量校准，补收差额' },
  { id: 'BA-20260504002', groupName: '电子科大科技园天府园', meterId: '202309150118', merchantName: '咖啡角', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -3120.60, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-04 11:00:00', creator: 'Admin_Li', remark: '电价下调退还差价' },
  { id: 'BA-20260505001', groupName: '滨海住宅区', meterId: '202309150042', merchantName: '科技园食堂', originalPrice: 0.8200, originalSurcharge: 0.0100, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: 2750.80, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-05 09:10:00', creator: 'Admin_Zhang', remark: '附加电价调整，追缴欠款' },
  { id: 'BA-20260506001', groupName: '电子科大科技园天府园', meterId: '202309150203', merchantName: '洗衣房', originalPrice: 0.8200, originalSurcharge: 0.0100, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: 1800.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-06 15:30:00', creator: 'Admin_Wang', remark: '历史充值错误，补收差额' },
  { id: 'BA-20260507001', groupName: '天辰科技园', meterId: '192251700410', merchantName: '8WL2-5(备用)', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -4980.50, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-07 11:45:00', creator: 'Admin_Chen', remark: '调整费控参数，退还多扣费用' },
  { id: 'BA-20260508001', groupName: '电子科大科技园天府园', meterId: '202309150367', merchantName: '便利店', originalPrice: 0.8800, originalSurcharge: 0.0200, adjustPrice: 0.8500, adjustSurcharge: 0.0100, adjustAmount: -3520.00, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-08 08:00:00', creator: 'Admin_Li', remark: '高峰时段电价下调退还' },
  { id: 'BA-20260509001', groupName: '滨海住宅区', meterId: '202309150482', merchantName: '商业街-102', originalPrice: 0.8800, originalSurcharge: 0.0200, adjustPrice: 0.9200, adjustSurcharge: 0.0300, adjustAmount: 6800.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-09 10:30:00', creator: 'Admin_Zhang', remark: '季节性电价上调，补收差额' },
  { id: 'BA-20260510001', groupName: '天辰科技园', meterId: '202309150001', merchantName: '天辰超市', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -1550.00, adjustType: '余额退费', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-10 14:20:00', creator: 'Admin_Wang', remark: '批量电价核对后退费' },
  { id: 'BA-20260511001', groupName: '电子科大科技园天府园', meterId: '202309150118', merchantName: '咖啡角', originalPrice: 0.8200, originalSurcharge: 0.0100, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: 3200.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-11 09:00:00', creator: 'Admin_Li', remark: '电价标准更新，补收差额' },
  { id: 'BA-20260512001', groupName: '滨海住宅区', meterId: '202309150042', merchantName: '科技园食堂', originalPrice: 0.8500, originalSurcharge: 0.0100, adjustPrice: 0.8200, adjustSurcharge: 0.0100, adjustAmount: -2230.10, adjustType: '余额退费', paymentStatus: '处理中', deductMethod: '延期扣除', createTime: '2026-05-12 16:00:00', creator: 'Admin_Chen', remark: '临时电价优惠退还' },
  { id: 'BA-20260513001', groupName: '天辰科技园', meterId: '202309150203', merchantName: '洗衣房', originalPrice: 0.8500, originalSurcharge: 0.0200, adjustPrice: 0.8800, adjustSurcharge: 0.0300, adjustAmount: 4500.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-13 11:30:00', creator: 'Admin_Zhang', remark: '附加费上调补收差额' },
  { id: 'BA-20260514001', groupName: '电子科大科技园天府园', meterId: '202309150482', merchantName: '商业街-102', originalPrice: 0.9200, originalSurcharge: 0.0300, adjustPrice: 0.8800, adjustSurcharge: 0.0200, adjustAmount: -4850.00, adjustType: '余额退费', paymentStatus: '处理中', deductMethod: '延期扣除', createTime: '2026-05-14 08:30:00', creator: 'Admin_Li', remark: '商户协商后电价下调退费' },
  { id: 'BA-20260514002', groupName: '天辰科技园', meterId: '192251700410', merchantName: '8WL2-5(备用)', originalPrice: 0.8200, originalSurcharge: 0.0100, adjustPrice: 0.8500, adjustSurcharge: 0.0200, adjustAmount: 5600.00, adjustType: '余额补收', paymentStatus: '已完成', deductMethod: '余额扣除', createTime: '2026-05-14 10:15:00', creator: 'Admin_Wang', remark: '电表更换后核算补收' },
]

export const publicAllocationRecords = [
  {
    id: 'PA-001',
    name: '公共电费分摊 - 2026Q1',
    method: '按面积均摊',
    deductMethod: '预扣',
    settlementDay: '每月5号',
    meterCount: 12,
    merchantCount: 48,
    mergeBill: '是',
    mergeRule: '按商户合并',
  },
  {
    id: 'PA-002',
    name: '公共照明电费',
    method: '按用电量比例',
    deductMethod: '余额扣除',
    settlementDay: '每月1号',
    meterCount: 8,
    merchantCount: 36,
    mergeBill: '是',
    mergeRule: '按楼栋合并',
  },
  {
    id: 'PA-003',
    name: '电梯用电分摊',
    method: '按户均摊',
    deductMethod: '后扣',
    settlementDay: '每季度末',
    meterCount: 6,
    merchantCount: 24,
    mergeBill: '否',
    mergeRule: '—',
  },
  {
    id: 'PA-004',
    name: '水泵房电费分摊',
    method: '按用水量比例',
    deductMethod: '预扣',
    settlementDay: '每月5号',
    meterCount: 4,
    merchantCount: 48,
    mergeBill: '是',
    mergeRule: '按商户合并',
  },
  {
    id: 'PA-005',
    name: '公共空调电费',
    method: '固定比例',
    deductMethod: '余额扣除',
    settlementDay: '每月10号',
    meterCount: 10,
    merchantCount: 42,
    mergeBill: '是',
    mergeRule: '按商户合并',
  },
]

export const rechargeStatsDaily = [
  { period: '05-06', wechat: 12000, alipay: 8500, bank: 3200, total: 23700 },
  { period: '05-07', wechat: 13500, alipay: 9200, bank: 2800, total: 25500 },
  { period: '05-08', wechat: 11000, alipay: 7800, bank: 3500, total: 22300 },
  { period: '05-09', wechat: 14500, alipay: 10200, bank: 2900, total: 27600 },
  { period: '05-10', wechat: 12800, alipay: 8900, bank: 3100, total: 24800 },
  { period: '05-11', wechat: 15200, alipay: 9800, bank: 3600, total: 28600 },
  { period: '05-12', wechat: 9800, alipay: 6500, bank: 2400, total: 18700 },
]

export const rechargeStatsMonthly = [
  { period: '2024-01', wechat: 320000, alipay: 240000, bank: 85000, total: 645000 },
  { period: '2024-02', wechat: 280000, alipay: 210000, bank: 72000, total: 562000 },
  { period: '2024-03', wechat: 350000, alipay: 260000, bank: 92000, total: 702000 },
  { period: '2024-04', wechat: 380000, alipay: 275000, bank: 88000, total: 743000 },
  { period: '2024-05', wechat: 210000, alipay: 155000, bank: 52000, total: 417000 },
]

export const splitAccountStatsDaily = [
  { date: '2026-05-01', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-02', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-03', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-04', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-05', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-06', totalAmount: 2000.00, epayAmount: 2000.00, txCount: 1, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-07', totalAmount: 8000.24, epayAmount: 8000.24, txCount: 1, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-08', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-09', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-10', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-11', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-12', totalAmount: 29000.00, epayAmount: 25000.00, txCount: 3, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-13', totalAmount: 0, epayAmount: 0, txCount: 0, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { date: '2026-05-14', totalAmount: 11362.81, epayAmount: 11362.81, txCount: 2, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
]

export const splitAccountStatsMonthly = [
  { period: '2026-05', totalAmount: 50363.05, epayAmount: 46362.05, txCount: 7, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { period: '2026-04', totalAmount: 124500.00, epayAmount: 98700.00, txCount: 22, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { period: '2026-03', totalAmount: 89200.50, epayAmount: 65400.00, txCount: 18, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { period: '2026-02', totalAmount: 67800.00, epayAmount: 52300.00, txCount: 15, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { period: '2026-01', totalAmount: 105600.00, epayAmount: 89100.00, txCount: 24, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
]

export const splitAccountStatsYearly = [
  { period: '2026', totalAmount: 437463.55, epayAmount: 351862.05, txCount: 86, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
  { period: '2025', totalAmount: 2890500.00, epayAmount: 2340600.00, txCount: 520, serviceFeeCurrent: 0, serviceFeePrevious: 0, splitAmount: 0, splitCount: 0, handlingFee: 0 },
]

export const waterBalanceAdjustmentSummary = {
  totalMeters: 0,
  refundCount: 0,
  refundAmount: 0,
  rechargeCount: 0,
  rechargeAmount: 0,
  netChange: 0,
}

export const waterBalanceAdjustments = []

export const splitAccountDetailRecords = [
  // Historical (before May 2026)
  { id: 101, group: '计费', merchantName: '天辰超市', meterId: '202309150118', transDate: '2026-01-15', orderNo: 'TXN20260115001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 250000.00, serviceFee: 0, handlingFee: 0, balance: 250000.00 },
  { id: 102, group: '计费', merchantName: '科技园食堂', meterId: '202309150203', transDate: '2026-01-20', orderNo: 'TXN20260120001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 180000.00, serviceFee: 0, handlingFee: 0, balance: 180000.00 },
  { id: 103, group: '计费', merchantName: '咖啡角', meterId: '202309150042', transDate: '2026-02-10', orderNo: 'TXN20260210001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 320000.00, serviceFee: 0, handlingFee: 0, balance: 320000.00 },
  { id: 104, group: '计费', merchantName: '洗衣房', meterId: '202309150367', transDate: '2026-03-05', orderNo: 'TXN20260305001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 560000.00, serviceFee: 0, handlingFee: 0, balance: 560000.00 },
  { id: 105, group: '计费', merchantName: '便利店', meterId: '202309150001', transDate: '2026-03-18', orderNo: 'TXN20260318001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 120000.00, serviceFee: 0, handlingFee: 0, balance: 120000.00 },
  { id: 106, group: '计费', merchantName: '商业街-102', meterId: '202309150482', transDate: '2026-04-08', orderNo: 'TXN20260408001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 443274.48, serviceFee: 0, handlingFee: 0, balance: 443274.48 },
  // May 2026 (within filter range)
  { id: 1, group: '计费', merchantName: '8WL2-5(备用)', meterId: '192251700410', transDate: '2026-05-01', orderNo: 'TXN20260501001', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 7000.00, serviceFee: 0, handlingFee: 0, balance: 7000.00 },
  { id: 2, group: '计费', merchantName: '科技园食堂', meterId: '202309150203', transDate: '2026-05-03', orderNo: 'TXN20260503002', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 8500.00, serviceFee: 0, handlingFee: 0, balance: 8500.00 },
  { id: 3, group: '计费', merchantName: '咖啡角', meterId: '202309150042', transDate: '2026-05-07', orderNo: 'TXN20260507003', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 3200.00, serviceFee: 0, handlingFee: 0, balance: 3200.00 },
  { id: 4, group: '计费', merchantName: '天辰超市', meterId: '202309150118', transDate: '2026-05-09', orderNo: 'TXN20260509004', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 12000.00, serviceFee: 0, handlingFee: 0, balance: 12000.00 },
  { id: 5, group: '计费', merchantName: '洗衣房', meterId: '202309150367', transDate: '2026-05-11', orderNo: 'TXN20260511005', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 9663.05, serviceFee: 0, handlingFee: 0, balance: 9663.05 },
  { id: 6, group: '计费', merchantName: '便利店', meterId: '202309150001', transDate: '2026-05-13', orderNo: 'TXN20260513006', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 5000.00, serviceFee: 0, handlingFee: 0, balance: 5000.00 },
  { id: 7, group: '计费', merchantName: '商业街-102', meterId: '202309150482', transDate: '2026-05-14', orderNo: 'TXN20260514007', transType: '充值电费', paymentMethod: '对公转账 - 人工缴费', amount: 5000.00, serviceFee: 0, handlingFee: 0, balance: 5000.00 },
]
