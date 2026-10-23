/**
 * CSV 导出工具
 * 将表格列配置 + 行数据导出为 UTF-8 BOM 的 CSV 文件
 *
 * @param {Array}  columns  - 列配置 [{ key, label }]
 * @param {Array}  data     - 行数据 [{ key: value }]
 * @param {string} filename - 下载文件名（不含扩展名）
 */
export function downloadCSV(columns, data, filename) {
  if (!data || data.length === 0) {
    console.warn('没有数据可导出')
    return
  }

  // BOM 头确保 Excel 正确识别 UTF-8 中文
  const BOM = '﻿'

  // 表头行
  const header = columns.map(c => escapeCSV(c.label)).join(',')

  // 数据行
  const rows = data.map(row =>
    columns.map(col => {
      const val = row[col.key]
      if (val === null || val === undefined || val === '') return ''
      if (typeof val === 'number') return val
      return escapeCSV(String(val))
    }).join(',')
  )

  const csv = BOM + header + '\n' + rows.join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${filename}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * CSV 转义：如果包含逗号、双引号、换行，用双引号包裹
 */
function escapeCSV(str) {
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return '"' + str.replace(/"/g, '""') + '"'
  }
  return str
}

/**
 * XLSX 导出工具
 * 将表头 + 行数据导出为 .xlsx 文件（中文友好，Excel 可直接打开）
 *
 * @param {Array<{key:string, label:string}>} columns - 列配置
 * @param {Array<Object>} data    - 行数据对象数组
 * @param {string}        filename - 下载文件名（不含扩展名）
 * @param {Object}        [sheetOpts] - 可选 sheet 配置 { name, columnWidths }
 */
export function downloadXLSX(columns, data, filename, sheetOpts) {
  if (!data || data.length === 0) {
    console.warn('没有数据可导出')
    return
  }

  // 动态导入 xlsx 库（避免不必要的加载）
  import('xlsx').then(XLSX => {
    // 构建表头行
    const header = columns.map(c => c.label)

    // 构建数据行
    const rows = data.map(row =>
      columns.map(col => {
        const val = row[col.key]
        if (val === null || val === undefined || val === '') return '—'
        if (typeof val === 'number') return val
        return val
      })
    )

    // 组合为完整表格
    const sheetData = [header, ...rows]
    const ws = XLSX.utils.aoa_to_sheet(sheetData)

    // 可选：设置列宽
    if (sheetOpts?.columnWidths) {
      ws['!cols'] = sheetOpts.columnWidths.map(w => ({ wch: w }))
    }

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, sheetOpts?.name || 'Sheet1')

    const d = new Date()
    const ts = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    XLSX.writeFile(wb, `${filename}_${ts}.xlsx`)
  })
}
