export const reportColumnsConfig = {
  // 用电量日报表明细（全面换成后端 Swagger 里的真实字段名！）
  'elec-daily': [
    { key: 'meterId', label: '表号', width: 'w-[130px]' },
    { key: 'customerName', label: '商户名', width: 'w-[120px]' },
    { key: 'electricityConsumed', label: '总用电量(kWh)', align: 'right' },
    { key: 'peakConsumed', label: '尖电量(kWh)', align: 'right' },
    { key: 'busyConsumed', label: '峰电量(kWh)', align: 'right' },
    { key: 'normalConsumed', label: '平电量(kWh)', align: 'right' },
    { key: 'idleConsumed', label: '谷电量(kWh)', align: 'right' },
    { key: 'chargeAmount', label: '电费金额(元)', align: 'right' }
  ],
  
  // 月报表占位（等你以后接月报表真实接口时，在这里把字段加上就行）
  'elec-monthly': [
    { key: 'meterId', label: '表号', width: 'w-[130px]' },
    { key: 'customerName', label: '商户名', width: 'w-[120px]' },
    { key: 'electricityConsumed', label: '总用电量(kWh)', align: 'right' }
  ],

  'elec-monthly-deleted': [
    { key: 'meterId', label: '表号', width: 'w-[130px]' },
    { key: 'customerName', label: '商户名', width: 'w-[120px]' },
    { key: 'electricityConsumed', label: '总用电量(kWh)', align: 'right' }
  ]
}