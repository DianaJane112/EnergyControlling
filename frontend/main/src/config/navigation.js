export const elecMenuItems = [
  { label: '首页', icon: 'home', route: '/dashboard' },
  {
    label: '计量管理',
    icon: 'speed',
    expanded: true,
    children: [
      { label: '表计管理', route: '/meters/physical', active: true },
      { label: '费率策略', route: '/meters/price-strategy?meterType=1' },
      { label: '虚拟电表', route: '/meters/virtual' },
      { label: '基站管理', route: '/stations' },
      { label: '电表客户信息', route: '/merchant/electric' },
      { label: '删除电表', route: '/meters/deleted' },
      { label: '分组管理', route: '/operations/group-config' },
    ]
  },
  {
    label: '客户收费',
    icon: 'headset_mic',
    expanded: false,
    children: [
      { label: '人工缴费', route: '/operations/manual-payment' },
    ]
  },
  {
    label: '财务管理',
    icon: 'account_balance',
    expanded: false,
    children: [
      { label: '充值明细', route: '/billing/recharge-details' },
      { label: '分账统计', route: '/billing/split-account-stats' },
      { label: '分账明细', route: '/billing/split-account-details' },
      { label: '退费明细', route: '/billing/refund-details' },
    ]
  },
  {
    label: '物业管理',
    icon: 'buildings',
    expanded: false,
    children: [
      { label: '即时账单', route: '/merchant/instant-bill' },
    ]
  },
  {
    label: '统计报表',
    icon: 'bar_chart',
    expanded: false,
    children: [
      { label: '用电量日报表', route: '/reports/elec-daily' },
      { label: '用电量月报表', route: '/reports/elec-monthly' },
      { label: '用电量月报表(删除电表)', route: '/reports/elec-monthly-deleted' },
      { label: '复费率计费电量日报表', route: '/reports/multi-rate-daily' },
      { label: '复费率计费电量汇总表', route: '/reports/multi-rate-summary' },
      { label: '月度用电量汇总表', route: '/reports/monthly-elec-summary' },
      { label: '年度用电量汇总表', route: '/reports/yearly-elec-summary' },
      { label: '计费电量汇总表', route: '/reports/billing-elec-summary' },
      { label: '计费电量月报表', route: '/reports/billing-elec-monthly' },
      { label: '计费电量年报表', route: '/reports/billing-elec-yearly' },
      { label: '用电负荷查询', route: '/reports/electric-load' },
      { label: '用能趋势', route: '/reports/energy-trend' },
    ]
  },
  {
    label: '余额退补',
    icon: 'account_balance_wallet',
    route: '/billing/balance-adjustment',
  },
  {
    label: '公共分摊',
    icon: 'share',
    route: '/billing/public-allocation',
  },
  {
    label: '电表对接',
    icon: 'settings_input_component',
    route: '/meters/electric-vendor',
  },
]

export const waterMenuItems = [
  { label: '首页', icon: 'home', route: '/dashboard' },
  {
    label: '计量管理',
    icon: 'speed',
    expanded: true,
    children: [
      { label: '水表管理', route: '/meters/water' },
      { label: '费率策略', route: '/meters/price-strategy?meterType=0' },
      { label: '虚拟水表', route: '/meters/water-virtual' },
      { label: '基站管理', route: '/stations/water' },
      { label: '水表客户信息', route: '/merchant/water' },
      { label: '删除水表', route: '/meters/water-deleted' },
      { label: '分组管理', route: '/operations/group-config' },
    ]
  },
  {
    label: '客户收费',
    icon: 'headset_mic',
    expanded: false,
    children: [
      { label: '人工缴费', route: '/operations/water-manual-payment' },
    ]
  },
  {
    label: '财务管理',
    icon: 'account_balance',
    expanded: false,
    children: [
      { label: '充值明细', route: '/billing/recharge-details' },
      { label: '分账统计', route: '/billing/split-account-stats' },
      { label: '分账明细', route: '/billing/split-account-details' },
      { label: '退费明细', route: '/billing/refund-details' },
    ]
  },
  {
    label: '物业管理',
    icon: 'buildings',
    expanded: false,
    children: [
      { label: '即时账单', route: '/merchant/instant-bill' },
    ]
  },
  {
    label: '统计报表',
    icon: 'bar_chart',
    expanded: false,
    children: [
      { label: '用水量日报表', route: '/reports/water-daily' },
      { label: '用水量月报表', route: '/reports/water-monthly' },
      { label: '用水量月报表(删除水表)', route: '/reports/water-monthly-deleted' },
      { label: '月度用水量汇总表', route: '/reports/water-monthly-summary' },
      { label: '年度用水量汇总表', route: '/reports/water-yearly-summary' },
      { label: '计费水量汇总表', route: '/reports/billing-water-summary' },
      { label: '计费水量月报表', route: '/reports/billing-water-monthly' },
      { label: '计费水量年报表', route: '/reports/billing-water-yearly' },
    ]
  },
  {
    label: '余额退补',
    icon: 'account_balance_wallet',
    route: '/billing/water-balance-adjustment',
  },
  {
    label: '公共分摊',
    icon: 'share',
    route: '/billing/public-allocation',
  },
  {
    label: '水表对接',
    icon: 'settings_input_component',
    route: '/meters/water-vendor',
  },
]

// Legacy alias — defaults to electric
export const menuItems = elecMenuItems
