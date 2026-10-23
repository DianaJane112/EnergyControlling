<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getMeterGroupTree } from '@/api/meterGroup.js'
import GroupSelectorNode from './GroupSelectorNode.vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  merchantId: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '请选择分组' },
  allowClear: { type: Boolean, default: true },
  showManageLink: { type: Boolean, default: true },
  currentLabel: { type: String, default: '' },
  relationStatus: { type: String, default: '' },
  widthClass: { type: String, default: 'w-[220px]' },
})

const emit = defineEmits(['update:modelValue', 'change'])
const router = useRouter()
const root = ref(null)
const open = ref(false)
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const groups = ref([])
const expanded = ref(new Set())

const normalize = nodes => (Array.isArray(nodes) ? nodes : []).map(node => ({
  ...node,
  children: normalize(node.children),
}))

const flatten = (nodes, parentPath = '') => nodes.flatMap(node => {
  const path = node.groupPath || [parentPath, node.groupName].filter(Boolean).join(' / ')
  return [{ ...node, resolvedPath: path }, ...flatten(node.children || [], path)]
})

const allGroups = computed(() => flatten(groups.value))
const selected = computed(() => allGroups.value.find(item => String(item.id) === String(props.modelValue)))
const invalidText = computed(() => ({
  DELETED: '原分组已删除，请重新选择',
  DISABLED: '原分组已禁用，请重新选择',
  INACCESSIBLE: '无权查看原分组，请重新选择',
}[props.relationStatus] || ''))
const displayText = computed(() => selected.value?.resolvedPath || props.currentLabel || invalidText.value || '')

function filterTree(nodes) {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return nodes
  return nodes.reduce((result, node) => {
    const children = filterTree(node.children || [])
    if ((node.groupName || '').toLowerCase().includes(term) || children.length) {
      result.push({ ...node, children, forceOpen: children.length > 0 })
    }
    return result
  }, [])
}
const visibleGroups = computed(() => filterTree(groups.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await getMeterGroupTree(props.merchantId ? Number(props.merchantId) : undefined)
    const data = response.data?.data ?? response.data ?? []
    groups.value = normalize(data).filter(item => item.status === undefined || Number(item.status) === 1)
  } catch (e) {
    groups.value = []
    error.value = e?.response?.data?.message || '分组加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function toggleOpen() {
  open.value = !open.value
  if (open.value) {
    await load()
    await nextTick()
    root.value?.querySelector('input')?.focus()
  }
}
function select(node) {
  emit('update:modelValue', node.id)
  emit('change', node)
  open.value = false
}
function clear() {
  emit('update:modelValue', '')
  emit('change', null)
}
function toggleNode(id) {
  const next = new Set(expanded.value)
  next.has(id) ? next.delete(id) : next.add(id)
  expanded.value = next
}
function onDocumentClick(event) {
  if (open.value && root.value && !root.value.contains(event.target)) open.value = false
}
function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
  if ((event.key === 'Enter' || event.key === ' ') && !open.value && event.target.dataset.trigger) {
    event.preventDefault(); toggleOpen()
  }
}
function goManage() {
  open.value = false
  router.push('/operations/group-config')
}

watch(() => props.merchantId, () => { groups.value = [] })
document.addEventListener('click', onDocumentClick)
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))
defineExpose({ reload: load })
</script>

<template>
  <div ref="root" class="relative inline-block max-w-full" :class="widthClass" @keydown="onKeydown">
    <button
      type="button"
      data-trigger="true"
      class="w-full min-h-9 flex items-center gap-2 px-3 border rounded bg-white text-left text-[13px] hover:border-[#3a64ab] focus:outline-none focus:ring-2 focus:ring-[#3a64ab]/20"
      :class="invalidText ? 'border-[#ff7875]' : 'border-[#d9d9d9]'"
      aria-haspopup="tree"
      :aria-expanded="open"
      @click.stop="toggleOpen"
    >
      <span class="flex-1 truncate" :class="displayText ? 'text-[#333]' : 'text-[#999]'">{{ displayText || placeholder }}</span>
      <span v-if="allowClear && modelValue" class="text-[#999] hover:text-[#333] px-1" aria-label="清空分组" @click.stop="clear">×</span>
      <span class="text-[#999]" aria-hidden="true">⌄</span>
    </button>
    <p v-if="invalidText" class="mt-1 text-[12px] text-[#cf1322]">{{ invalidText }}</p>

    <div v-if="open" class="absolute z-[90] left-0 mt-1 w-[min(360px,calc(100vw-32px))] bg-white border border-[#e5e5e5] rounded shadow-lg overflow-hidden">
      <div class="p-2 border-b border-[#f0f0f0]">
        <input v-model="keyword" type="search" class="w-full px-3 py-2 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab]" placeholder="搜索分组名称" aria-label="搜索分组" />
      </div>
      <div class="max-h-[320px] overflow-auto py-1" role="tree" aria-label="可关联分组">
        <div v-if="loading" class="px-4 py-8 text-center text-[13px] text-[#999]">正在加载分组...</div>
        <div v-else-if="error" class="px-4 py-6 text-center text-[13px] text-[#cf1322]">
          <p>{{ error }}</p><button type="button" class="mt-2 text-[#3a64ab]" @click="load">重新加载</button>
        </div>
        <div v-else-if="!visibleGroups.length" class="px-4 py-7 text-center text-[13px] text-[#999]">
          <p>{{ keyword ? '未找到匹配分组' : '暂无可关联分组' }}</p>
          <button v-if="showManageLink && !keyword" type="button" class="mt-2 text-[#3a64ab]" @click="goManage">前往分组管理</button>
        </div>
        <template v-else>
          <template v-for="node in visibleGroups" :key="node.id">
            <GroupSelectorNode :node="node" :level="0" :selected-id="modelValue" :expanded="expanded" @toggle="toggleNode" @select="select" />
          </template>
        </template>
      </div>
    </div>
  </div>
</template>

