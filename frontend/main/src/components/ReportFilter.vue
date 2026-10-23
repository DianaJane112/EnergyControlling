<template>
  <div class="bg-white rounded p-5 mb-4 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
    <div class="flex flex-wrap gap-4 items-center">
      <input
        v-model="form.keyword"
        class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none focus:border-[#3a64ab] transition-colors"
        placeholder="商户名/表号"
        type="text"
      />
      <input
        v-model="form.virtualKeyword"
        class="py-2 px-3.5 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none focus:border-[#3a64ab] transition-colors"
        placeholder="虚拟表号"
        type="text"
      />
      <DropdownSelect v-model="form.group" :options="groupOptions" placeholder="分组" />

      <div v-if="showShadowMeter" class="flex items-center gap-1.5">
        <span class="text-[13px] text-[#333] whitespace-nowrap">统计影子电表</span>
        <DropdownSelect v-model="form.shadowMeter" :options="shadowMeterOptions" placeholder="请选择" />
      </div>

      <DateRangePicker
        v-if="showDateRange"
        v-model:start-value="form.dateStart"
        v-model:end-value="form.dateEnd"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />

      <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="emitSearch">查询</button>
      <button class="py-2 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="handleReset">重置</button>

      <div class="flex-1"></div>
      <button class="py-2 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="emit('export')">导出</button>
      <button class="py-2 px-3 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="$emit('help')">说明</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch, onMounted } from 'vue'
import DateRangePicker from '@/components/forms/DateRangePicker.vue'
import DropdownSelect from '@/components/forms/DropdownSelect.vue'
import { getMeterGroupList } from '@/api/meterGroup.js'

const props = defineProps({
  showShadowMeter: { type: Boolean, default: true },
  showDateRange: { type: Boolean, default: true },
  defaultDateStart: { type: String, default: '' },
  defaultDateEnd: { type: String, default: '' },
  resetKey: { type: Number, default: 0 },
})

const emit = defineEmits(['search', 'reset', 'export', 'help'])

const groupOptions = ref([{ value: '', label: '全部' }])

async function loadGroups() {
  try {
    const res = await getMeterGroupList({ pageSize: 500 })
    let records = res.data?.records
    if (!records && Array.isArray(res.data)) records = res.data
    if (records) {
      groupOptions.value = [
        { value: '', label: '全部' },
        ...records.map(g => ({ value: String(g.id), label: g.groupName || g.name || '' })),
      ]
    }
  } catch { /* silent */ }
}

const shadowMeterOptions = [
  { value: '是', label: '是' },
  { value: '否', label: '否' },
]

function makeEmptyForm() {
  return {
    keyword: '',
    virtualKeyword: '',
    group: '',
    shadowMeter: '否',
    dateStart: props.defaultDateStart,
    dateEnd: props.defaultDateEnd,
  }
}

const form = reactive(makeEmptyForm())

watch([() => props.defaultDateStart, () => props.defaultDateEnd], ([newStart, newEnd]) => {
  form.dateStart = newStart
  form.dateEnd = newEnd
})

watch(() => props.resetKey, () => {
  Object.assign(form, makeEmptyForm())
})

const emitSearch = () => {
  emit('search', {
    ...form,
    groupId: form.group ? Number(form.group) : null,
  })
}

const handleReset = () => {
  Object.assign(form, makeEmptyForm())
  emitSearch()
  emit('reset')
}

onMounted(() => { loadGroups() })
</script>
