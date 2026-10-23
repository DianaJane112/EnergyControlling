export const statsCards = [
  {
    label: '本月总用电量',
    value: '124,582.00',
    icon: 'bolt',
    iconBg: 'bg-primary-fixed',
    iconColor: 'text-primary',
    trend: '12.5%',
    trendType: 'up',
    trendLabel: '较上月同期',
    valueColor: 'text-primary',
  },
  {
    label: '今日充值总额',
    value: '¥ 45,210.50',
    icon: 'account_balance_wallet',
    iconBg: 'bg-tertiary-fixed',
    iconColor: 'text-tertiary',
    trend: '3.2%',
    trendType: 'down',
    trendLabel: '较昨日此时',
    valueColor: 'text-on-surface',
  },
]

export const stationStatus = {
  online: { count: '1,248', label: '在线基站' },
  abnormal: { count: '3', label: '异常基站' },
}

export const chartBars = [
  { height: '60%', label: '05-01' },
  { height: '75%', label: '05-02' },
  { height: '45%', label: '05-03' },
  { height: '90%', label: '05-04' },
  { height: '82%', label: '05-05', color: 'bg-primary-container' },
  { height: '70%', label: '05-06' },
  { height: '55%', label: '今日' },
]

export const operationLogs = [
  { time: '14:30:45', operator: 'Admin_Zhang', target: '#A-402 智能三相表', type: '远程下线/合闸', value: '-', status: '成功', statusType: 'success' },
  { time: '14:28:12', operator: 'WeChat_Pay', target: '#B-1025 民用单相', type: '在线充值', value: '+¥ 200.00', status: '成功', statusType: 'success', highlight: true },
  { time: '14:15:00', operator: 'System_Task', target: '#C-G02 集中器', type: '自动抄表', value: '842 台成功', status: '部分失败', statusType: 'error' },
  { time: '13:55:33', operator: 'Admin_Li', target: '#D-201 门面用电', type: '人工调整余额', value: '-¥ 1,500.00', status: '成功', statusType: 'success', negative: true },
]
