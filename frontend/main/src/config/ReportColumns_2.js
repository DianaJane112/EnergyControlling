/**
 * 统计报表（电表）列配置
 *
 * 字段名与后端 VO 完全一致，确保前后端联调时零翻译成本。
 *
 * 后端 VO → 报表 ID 对应关系：
 *   DailyElectricityVO     → elec-daily (GET /v1/report/daily-electricity)
 *   MonthlyMeterDetailVO  → elec-monthly (GET /v1/report/monthly-detail)
 *   ReportSummaryVO        → monthly-elec-summary, yearly-elec-summary
 *   TimeOfUseReportVO      → multi-rate-daily, multi-rate-summary
 *   BillingReportVO        → billing-elec-summary, billing-elec-monthly, billing-elec-yearly
 *   DeletedMeterReportVO   → elec-monthly-deleted
 *   CustomerComprehensiveVO → customer-comprehensive
 *   ElectricLoadVO         → electric-load
 */

export const reportColumnsConfig = {

  // ── 用电量日报表明细（DailyElectricityVO） ──
  'elec-daily': [
    { key: 'reportDate',          label: '日期',               width: 'w-[100px]' },
    { key: 'customerName',        label: '商户名',             width: 'w-[120px]' },
    { key: 'groupName',           label: '分组',               width: 'w-[90px]' },
    { key: 'deviceNo',            label: '表号',               width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',       width: 'w-[120px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',           width: 'w-[130px]' },
    { key: 'forwardConsumed',     label: '正向有功(kWh)',      align: 'right' },
    { key: 'reverseConsumed',     label: '反向有功(kWh)',      align: 'right' },
    { key: 'reverseRatio',        label: '反向占比(%)',        align: 'right' },
    { key: 'electricityConsumed', label: '总电量(kWh)',        align: 'right' },
  ],

  // ── 用电量月报表（MonthlyMeterDetailVO）—— 静态列，日列由组件动态生成 ──
  'elec-monthly': [
    { key: 'groupName',           label: '分组',           width: 'w-[100px]' },
    { key: 'customerName',        label: '商户名',         width: 'w-[130px]' },
    { key: 'deviceNo',            label: '表号',           width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',   width: 'w-[120px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',       width: 'w-[130px]' },
  ],

  // ── 月度用电量汇总表（宽表：MonthlyElecSummaryVO，按表计×月度） ──
  // 两级表头：正向有功电量(5子列) / 反向有功电量(5子列) / 总起止示数(2子列)
  'monthly-elec-summary': [
    { key: 'groupName',           label: '分组',              width: 'w-[90px]' },
    { key: 'deviceNo',            label: '表号',              width: 'w-[130px]' },
    { key: 'customerName',        label: '商户名',            width: 'w-[120px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',          width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',      width: 'w-[120px]' },
    { key: 'statMonth',           label: '月度',              width: 'w-[90px]' },
    { key: 'forwardStartReading', label: '上期示数',          align: 'right', group: '正向有功电量' },
    { key: 'forwardStartTime',    label: '上期时间',          width: 'w-[140px]', group: '正向有功电量' },
    { key: 'forwardEndReading',   label: '本期示数',          align: 'right', group: '正向有功电量' },
    { key: 'forwardEndTime',      label: '本期时间',          width: 'w-[140px]', group: '正向有功电量' },
    { key: 'forwardUsage',        label: '用量(kWh)',         align: 'right', group: '正向有功电量' },
    { key: 'reverseStartReading', label: '上期示数',          align: 'right', group: '反向有功电量' },
    { key: 'reverseStartTime',    label: '上期时间',          width: 'w-[140px]', group: '反向有功电量' },
    { key: 'reverseEndReading',   label: '本期示数',          align: 'right', group: '反向有功电量' },
    { key: 'reverseEndTime',      label: '本期时间',          width: 'w-[140px]', group: '反向有功电量' },
    { key: 'reverseUsage',        label: '用量(kWh)',         align: 'right', group: '反向有功电量' },
    { key: 'reverseRatio',        label: '反向占比(%)',       align: 'right' },
    { key: 'totalStartReading',   label: '总起示数',          align: 'right', group: '总起止示数' },
    { key: 'totalEndReading',     label: '总止示数',          align: 'right', group: '总起止示数' },
    { key: 'totalEnergy',         label: '总电量(kWh)',       align: 'right' },
    { key: 'previousBalance',     label: '上期余额(元)',      align: 'right' },
    { key: 'currentBalance',      label: '本期余额(元)',      align: 'right' },
    { key: 'dataUpdateTime',      label: '数据更新时间',      width: 'w-[150px]' },
  ],

  // ── 年度用电量汇总表（YearlyMeterDetailVO — 按表计×12月明细） ──
  'yearly-elec-summary': [
    { key: 'customerName',        label: '商户名',         width: 'w-[120px]' },
    { key: 'deviceNo',            label: '表号',           width: 'w-[130px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',       width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',   width: 'w-[120px]' },
    { key: 'month01',             label: '1月(kWh)',       align: 'right' },
    { key: 'month02',             label: '2月(kWh)',       align: 'right' },
    { key: 'month03',             label: '3月(kWh)',       align: 'right' },
    { key: 'month04',             label: '4月(kWh)',       align: 'right' },
    { key: 'month05',             label: '5月(kWh)',       align: 'right' },
    { key: 'month06',             label: '6月(kWh)',       align: 'right' },
    { key: 'month07',             label: '7月(kWh)',       align: 'right' },
    { key: 'month08',             label: '8月(kWh)',       align: 'right' },
    { key: 'month09',             label: '9月(kWh)',       align: 'right' },
    { key: 'month10',             label: '10月(kWh)',      align: 'right' },
    { key: 'month11',             label: '11月(kWh)',      align: 'right' },
    { key: 'month12',             label: '12月(kWh)',      align: 'right' },
    { key: 'subtotal',            label: '小计(kWh)',      align: 'right' },
  ],

  // ── 复费率计费电量日报表明细（TimeOfUseReportVO - daily-detail） ──
  'multi-rate-daily': [
    { key: 'customerCode',  label: '商户号',           width: 'w-[100px]' },
    { key: 'deviceNo',      label: '表号',             width: 'w-[130px]' },
    { key: 'reportDate',    label: '日期',             width: 'w-[100px]', sortable: true },
    { key: 'totalConsumed', label: '总电量(kWh)',      align: 'right', group: '总用量' },
    { key: 'totalCharge',   label: '总电费(¥)',        align: 'right', group: '总用量' },
    { key: 'peakConsumed',  label: '尖电量(kWh)',      align: 'right', group: '尖用量' },
    { key: 'peakCharge',    label: '尖电费(¥)',        align: 'right', group: '尖用量' },
    { key: 'busyConsumed',  label: '峰电量(kWh)',      align: 'right', group: '峰用量' },
    { key: 'busyCharge',    label: '峰电费(¥)',        align: 'right', group: '峰用量' },
    { key: 'normalConsumed',label: '平电量(kWh)',      align: 'right', group: '平用量' },
    { key: 'normalCharge',  label: '平电费(¥)',        align: 'right', group: '平用量' },
    { key: 'idleConsumed',  label: '谷电量(kWh)',      align: 'right', group: '谷用量' },
    { key: 'idleCharge',    label: '谷电费(¥)',        align: 'right', group: '谷用量' },
  ],

  // ── 复费率计费电量汇总表（TimeOfUseReportVO - monthly） ──
  'multi-rate-summary': [
    { key: 'period',        label: '月份/周期',       width: 'w-[120px]', sortable: true },
    { key: 'totalConsumed', label: '总电量(kWh)',      align: 'right', sortable: true },
    { key: 'totalCharge',   label: '总电费(¥)',        align: 'right' },
    { key: 'peakConsumed',  label: '尖电量(kWh)',      align: 'right' },
    { key: 'peakCharge',    label: '尖电费(¥)',        align: 'right' },
    { key: 'busyConsumed',  label: '峰电量(kWh)',      align: 'right' },
    { key: 'busyCharge',    label: '峰电费(¥)',        align: 'right' },
    { key: 'normalConsumed',label: '平电量(kWh)',      align: 'right' },
    { key: 'normalCharge',  label: '平电费(¥)',        align: 'right' },
    { key: 'idleConsumed',  label: '谷电量(kWh)',      align: 'right' },
    { key: 'idleCharge',    label: '谷电费(¥)',        align: 'right' },
  ],

  // ── 用电量月报表（已删除电表）（DeletedMeterReportVO） ──
  // 注：日列 + 总用量由 CommonElecReport 动态拼接（与 elec-monthly 模式一致）
  'elec-monthly-deleted': [
    { key: 'groupName',           label: '分组',           width: 'w-[100px]' },
    { key: 'merchantName',        label: '商户名',         width: 'w-[120px]' },
    { key: 'deviceNo',            label: '表号',           width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',   width: 'w-[120px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',       width: 'w-[130px]' },
  ],

  // ── 计费电量汇总表（BillingMeterSummaryVO — 按表计维度） ──
  'billing-elec-summary': [
    { key: 'customerName',      label: '商户名',           width: 'w-[120px]' },
    { key: 'deviceNo',          label: '表号',             width: 'w-[130px]' },
    { key: 'totalChargeAmount', label: '计费金额(元)',     align: 'right' },
    { key: 'totalConsumed',     label: '计费电量(kWh)',    align: 'right' },
    { key: 'averagePrice',      label: '计费均价(元/度)',   align: 'right' },
  ],

  'billing-elec-monthly': [
    { key: 'groupName',          label: '分组',           width: 'w-[90px]' },
    { key: 'merchantName',       label: '商户名',         width: 'w-[120px]' },
    { key: 'deviceNo',           label: '表号',           width: 'w-[130px]' },
    { key: 'virtualMerchantName',label: '虚拟表商户名',   width: 'w-[120px]' },
    { key: 'virtualMeterNo',     label: '虚拟表号',       width: 'w-[130px]' },
    // 注：日列（总电量/总费用）与汇总列由 BillingElecMonthlyReport.vue 动态生成
  ],

  // ── 计费电量年报表（BillingYearlyMeterDetailVO，按表计+年度/本月汇总宽表） ──
  // 两级表头：当年年度(金额/用量) / 本月(金额/用量/本期余额/上期余额)
  'billing-elec-yearly': [
    { key: 'customerName',        label: '商户名',           width: 'w-[120px]' },
    { key: 'deviceNo',            label: '表号',             width: 'w-[130px]' },
    { key: 'virtualMeterNo',      label: '虚拟表号',         width: 'w-[130px]' },
    { key: 'virtualMerchantName', label: '虚拟表商户名',     width: 'w-[120px]' },
    { key: 'yearAmount',          label: '金额(元)',         align: 'right', group: '当年年度' },
    { key: 'yearUsage',           label: '用量(kWh)',        align: 'right', group: '当年年度' },
    { key: 'monthAmount',         label: '金额(元)',         align: 'right', group: '本月' },
    { key: 'monthUsage',          label: '用量(kWh)',        align: 'right', group: '本月' },
    { key: 'currentBalance',      label: '本期余额(元)',     align: 'right', group: '本月' },
    { key: 'previousBalance',     label: '上期余额(元)',     align: 'right', group: '本月' },
  ],

  // ── 客户综合报表（CustomerComprehensiveVO） ──
  'customer-comprehensive': [
    { key: 'customerName',    label: '商户名称',       width: 'w-[120px]' },
    { key: 'totalRecharge',   label: '总充值金额(元)', align: 'right' },
    { key: 'totalConsumed',   label: '总用电量(kWh)',  align: 'right' },
    { key: 'totalCharge',     label: '总扣费金额(元)', align: 'right' },
    { key: 'currentBalance',  label: '当前余额(元)',   align: 'right' },
  ],

  // ── 用电负荷（ElectricLoadVO） ──
  'electric-load': [
    { key: 'period',        label: '统计时段',         width: 'w-[100px]' },
    { key: 'maxLoad',       label: '最大负荷(kWh)',    align: 'right' },
    { key: 'averageLoad',   label: '平均负荷(kWh)',    align: 'right' },
    { key: 'minLoad',       label: '最小负荷(kWh)',    align: 'right' },
    { key: 'loadRate',      label: '负荷率(%)',        align: 'right' },
    { key: 'totalConsumed', label: '总用电量(kWh)',    align: 'right' },
    { key: 'meterCount',    label: '涉及表计数',       align: 'right' },
    { key: 'dayCount',      label: '统计天数',         align: 'right' },
  ],
}

// ════════════════════════════════════════════════════════════════════════
// 向后兼容：旧版 key 别名（如果旧代码还在用旧 key 名，自动映射）
// ════════════════════════════════════════════════════════════════════════

/**
 * 根据报表 ID 和后端响应数据，返回适配前端表格的列配置。
 * 如果该报表 ID 没有预定义配置，返回空数组。
 *
 * @param {string} reportId - 报表 ID（如 'elec-daily'）
 * @returns {Array} 列配置数组
 */
export function getColumnsByReportId(reportId) {
  return reportColumnsConfig[reportId] || []
}

/**
 * 根据报表 ID 和数据列表自动推断列配置。
 * 如果无预定义配置，则从第一行数据的 key 自动生成列。
 *
 * @param {string} reportId - 报表 ID
 * @param {Array}  data     - 后端响应数据列表
 * @returns {Array} 列配置数组
 */
export function inferColumns(reportId, data = []) {
  const preset = reportColumnsConfig[reportId]
  if (preset) return preset

  if (!data || data.length === 0) return []

  const sample = data[0]
  return Object.keys(sample).map(key => ({
    key,
    label: key,
    align: typeof sample[key] === 'number' ? 'right' : undefined,
  }))
}
