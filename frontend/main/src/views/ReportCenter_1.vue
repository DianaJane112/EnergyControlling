<script setup>
import { ref, reactive, computed, watch, provide } from 'vue'
import { useRoute } from 'vue-router'

// ── 1. 引入统一的筛选组件 ──
import ReportFilter from '@/components/ReportFilter.vue'

// ── 2. 引入所有的特种兵报表 ──
import MultiRateDailyReport from '@/views/MultiRateDailyReport.vue'
import MultiRateSummaryReport from '@/views/MultiRateSummaryReport.vue'
import MonthlyElecSummaryReport from '@/views/MonthlyElecSummaryReport.vue'
import YearlyElecSummaryReport from '@/views/YearlyElecSummaryReport.vue'
import BillingElecMonthlyReport from '@/views/BillingElecMonthlyReport.vue'
import BillingElecSummaryReport from '@/views/BillingElecSummaryReport.vue'
import BillingElecYearlyReport from '@/views/BillingElecYearlyReport.vue'

// ── 3. 引入统管剩下 3 个基础报表的"步兵连" ──
import CommonElecReport from '@/views/CommonElecReport.vue'

const route = useRoute()
const activeReportId = computed(() => route.params.reportId || 'elec-daily')

// ── 全局筛选状态管理 ──
const defaultDateStart = ref('')
const defaultDateEnd = ref('')
const filterResetKey = ref(0)

const searchParams = reactive({
  // 前端筛选表单原始值
  keyword: '',
  virtualKeyword: '',
  group: '',
  shadowMeter: '否',
  dateStart: '',
  dateEnd: '',
  // 后端接口对齐字段
  customerId: null,
  groupId: null,
  merchantId: null,
  projectId: null,
})

// 接收子组件筛选栏的搜索请求
function handleSearch(payload) {
  Object.assign(searchParams, payload)
}

// 切换路由/报表时，重置筛选条件
watch(activeReportId, () => {
  if (activeReportId.value === 'elec-monthly') {
    // 用电量月报表：默认显示当月 1 日 ~ 今天
    const now = new Date()
    const y = now.getFullYear()
    const m = String(now.getMonth() + 1).padStart(2, '0')
    const d = String(now.getDate()).padStart(2, '0')
    defaultDateStart.value = `${y}-${m}-01`
    defaultDateEnd.value = `${y}-${m}-${d}`
  } else if (['elec-monthly-deleted', 'billing-elec-monthly'].includes(activeReportId.value)) {
    defaultDateStart.value = '2026-05-01'
    defaultDateEnd.value = '2026-05-17'
  } else {
    defaultDateStart.value = ''
    defaultDateEnd.value = ''
  }

  Object.assign(searchParams, {
    keyword: '',
    virtualKeyword: '',
    group: '',
    shadowMeter: '否',
    dateStart: defaultDateStart.value,
    dateEnd: defaultDateEnd.value,
    customerId: null,
    groupId: null,
    merchantId: null,
    projectId: null,
  })
  filterResetKey.value++
}, { immediate: true })

const helpModalOpen = ref(false)

// ── 导出功能：子组件注册导出回调 ──
const exportHandler = ref(null)
provide('registerExport', (fn) => { exportHandler.value = fn })
function handleExport() {
  if (exportHandler.value) {
    exportHandler.value()
  } else {
    console.warn('当前报表不支持导出')
  }
}

// ── 重置时通知子组件清除排序状态 ──
const tableResetKey = ref(0)
function handleReset() {
  tableResetKey.value++
}
</script>

<template>
  <div>
    <ReportFilter
      :default-date-start="defaultDateStart"
      :default-date-end="defaultDateEnd"
      :reset-key="filterResetKey"
      @search="handleSearch"
      @reset="handleReset"
      @export="handleExport"
      @help="helpModalOpen = true"
    />

    <MultiRateDailyReport v-if="activeReportId === 'multi-rate-daily'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <MultiRateSummaryReport v-else-if="activeReportId === 'multi-rate-summary'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <MonthlyElecSummaryReport v-else-if="activeReportId === 'monthly-elec-summary'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <YearlyElecSummaryReport v-else-if="activeReportId === 'yearly-elec-summary'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <BillingElecMonthlyReport v-else-if="activeReportId === 'billing-elec-monthly'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <BillingElecSummaryReport v-else-if="activeReportId === 'billing-elec-summary'" :search-params="searchParams" :table-reset-key="tableResetKey" />
    <BillingElecYearlyReport v-else-if="activeReportId === 'billing-elec-yearly'" :search-params="searchParams" :table-reset-key="tableResetKey" />

    <CommonElecReport
      v-else
      :report-id="activeReportId"
      :search-params="searchParams"
      :table-reset-key="tableResetKey"
    />

    <Teleport to="body">
      <div v-if="helpModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/45" @click.self="helpModalOpen = false">
        <div class="bg-white rounded-lg shadow-2xl w-[680px] max-h-[75vh] overflow-y-auto" @click.stop>
          <div class="flex justify-between items-center px-6 py-4 border-b border-[#f0f0f0]">
            <span class="text-[16px] font-medium text-[#333]">报表说明</span>
            <button class="border-none bg-transparent cursor-pointer text-lg text-[#999] leading-none" @click="helpModalOpen = false">&times;</button>
          </div>
          <div class="px-6 py-5 text-[13px] leading-relaxed text-[#333] space-y-3">
            <p><b>报表数据来源：</b>基于电表定时采集的冻结数据（日冻结 / 月冻结），结合费率模板与计费规则自动生成。</p>
            <p><b>日 / 月报表：</b>展示各电表在统计周期内的尖峰平谷分时段用电量及总用电量，日冻结数据次日凌晨自动汇总。</p>
            <p><b>复费率报表：</b>按尖峰平谷四个时段分别统计计费电量，适用于执行分时电价的用户分析。</p>
            <p><b>汇总表：</b>按楼栋、楼层、分组等维度对用电量进行聚合汇总，支持月度 / 年度两个粒度。</p>
            <p><b>计费电量报表：</b>展示实际参与电费计算的净电量（已扣除线损、变损等附加电量），与账单金额一一对应。</p>
            <p class="text-[#ff4d4f]"><b>注意事项：</b>报表数据冻结时间为每日凌晨 2:00，当月数据在月末最后一天 24:00 完成月结后方可查询。</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
