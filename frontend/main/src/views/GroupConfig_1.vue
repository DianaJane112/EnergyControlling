<script setup>
import { ref, reactive, provide, onMounted } from 'vue'
import { getMeterGroupTree, getMeterGroupById, createMeterGroup, updateMeterGroup } from '@/api/meterGroup.js'
import { translateError } from '@/api/request.js'
import TreeNode from '@/components/common/TreeNode.vue'
import { useToast } from '@/composables/useToast.js'
import ToastMessage from '@/components/common/ToastMessage.vue'
import { useConfirm } from '@/composables/useConfirm.js'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const { showToast } = useToast()
const { showConfirm: showConfirmDialog } = useConfirm()

// ── Filters ─────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterMerchantId = ref('')

// ── Tree data ───────────────────────────────────────────────────────
const treeData = ref([])
const expandedIds = reactive(new Set())
const loading = ref(false)
const errorMsg = ref('')

// ── Full tree cache (unfiltered) ─────────────────────────────────────
const fullTree = ref([])

function filterTree(nodes, keyword, merchantId) {
  const kw = keyword.toLowerCase()
  const mid = merchantId ? Number(merchantId) : null
  return nodes.reduce((acc, node) => {
    const label = (node.label || '').toLowerCase()
    const mId = node.merchantId
    const kwMatch = !kw || label.includes(kw)
    const midMatch = !mid || mId === mid
    const filteredChildren = node.children ? filterTree(node.children, keyword, merchantId) : []
    if (kwMatch && midMatch) {
      acc.push({ ...node, children: filteredChildren.length ? filteredChildren : node.children })
    } else if (filteredChildren.length) {
      acc.push({ ...node, children: filteredChildren })
    }
    return acc
  }, [])
}

async function fetchTree() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getMeterGroupTree()
    const nodes = (Array.isArray(res.data) ? res.data : (res.data?.records || []))
      .map(n => ({ ...n, label: n.name || n.groupName || n.label || '--' }))
    fullTree.value = nodes
    const kw = filterKeyword.value.trim()
    const mid = filterMerchantId.value.trim()
    treeData.value = (kw || mid) ? filterTree(nodes, kw, mid) : nodes
    treeData.value.forEach(n => expandedIds.add(n.id))
  } catch (e) {
    treeData.value = []
    errorMsg.value = e?.response?.data?.msg || e.message || '加载分组数据失败'
  }
  finally { loading.value = false }
}

function handleSearch() { fetchTree() }
function handleReset() {
  filterKeyword.value = ''
  filterMerchantId.value = ''
  fetchTree()
}

function collectAllIds(nodes) {
  let ids = []
  for (const n of nodes) {
    ids.push(n.id)
    if (n.children) ids = ids.concat(collectAllIds(n.children))
  }
  return ids
}

function expandAll() { collectAllIds(treeData.value).forEach(id => expandedIds.add(id)) }
function collapseAll() { expandedIds.clear() }

// ── Expand ancestors of a node ──────────────────────────────────────
function expandAncestors(nodeId, nodes, path = []) {
  for (const n of nodes) {
    if (n.id === nodeId) {
      path.forEach(id => expandedIds.add(id))
      return true
    }
    if (n.children && expandAncestors(nodeId, n.children, [...path, n.id])) return true
  }
  return false
}

// ── CRUD ────────────────────────────────────────────────────────────
const saving = ref(false)

const addChild = async (parent, name, merchantId, description) => {
  saving.value = true
  try {
    const payload = { groupName: name, parentId: parent.id }
    if (merchantId) payload.merchantId = Number(merchantId)
    if (description) payload.description = description
    await createMeterGroup(payload)
    await fetchTree()
    // Auto-expand parent to reveal the new child
    expandedIds.add(parent.id)
    showToast('添加成功')
  } catch (e) {
    const msg = translateError(e?.response?.data?.msg) || e.message || '添加失败'
    showToast(msg, 'error')
  } finally { saving.value = false }
}

const editNode = async (node, name, merchantId, description) => {
  saving.value = true
  try {
    const payload = { groupName: name }
    if (merchantId) payload.merchantId = Number(merchantId)
    if (description) payload.description = description
    await updateMeterGroup(node.id, payload)
    node.label = name
    showToast('修改成功')
    await fetchTree()
  } catch (e) {
    const msg = translateError(e?.response?.data?.msg) || e.message || '修改失败'
    showToast(msg, 'error')
  } finally { saving.value = false }
}

const disableNode = async (node) => {
  const ok = await showConfirmDialog(`确定停用分组「${node.label}」吗？停用后该分组将不可见。`, '停用确认')
  if (!ok) return
  saving.value = true
  try {
    const res = await updateMeterGroup(node.id, { groupName: node.label, status: 0 })
    if (res.success === false) {
      showToast(translateError(res.msg) || '停用失败', 'error')
      return
    }
    showToast('停用成功')
    await fetchTree()
  } catch (e) {
    const msg = translateError(e?.response?.data?.msg) || e.message || '停用失败'
    showToast(msg, 'error')
    await fetchTree()
  } finally { saving.value = false }
}

// ── View detail ─────────────────────────────────────────────────────
const detailModal = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

async function viewDetail(node) {
  detailModal.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getMeterGroupById(node.id)
    detailData.value = res.data || null
  } catch (e) {
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() { detailModal.value = false }

provide('addChild', addChild)
provide('editNode', editNode)
provide('deleteNode', disableNode)
provide('expandedIds', expandedIds)
provide('detailNode', viewDetail)

onMounted(() => fetchTree())
</script>

<template>
  <div>
    <ToastMessage />

    <!-- Header -->
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center gap-3">
        <div>
          <h2 class="text-[18px] font-bold text-[#1a1a2e]">分组管理</h2>
          <p class="text-[12px] text-[#999] mt-0.5">点击节点圆圈可添加、修改或删除分组</p>
        </div>
      </div>
      <div class="flex gap-2.5">
        <button class="py-1.5 px-3.5 rounded text-[13px] border border-[#d9d9d9] bg-white text-[#666] cursor-pointer hover:border-[#3a64ab] hover:text-[#3a64ab] transition-all flex items-center gap-1" @click="expandAll">
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          全部展开
        </button>
        <button class="py-1.5 px-3.5 rounded text-[13px] border border-[#d9d9d9] bg-white text-[#666] cursor-pointer hover:border-[#3a64ab] hover:text-[#3a64ab] transition-all flex items-center gap-1" @click="collapseAll">
          <svg class="w-3.5 h-3.5 rotate-180" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
          全部折叠
        </button>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="bg-white rounded-lg p-4 mb-3 shadow-sm border border-[#f0f0f0]">
      <div class="flex flex-wrap gap-3 items-center">
        <div class="relative">
          <svg class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#bfbfbf]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input
            v-model="filterKeyword"
            class="py-1.5 pl-8 pr-3 border border-[#d9d9d9] rounded-md text-[13px] w-[200px] outline-none focus:border-[#3a64ab] focus:shadow-[0_0_0_2px_rgba(58,100,171,0.1)] transition-all"
            placeholder="搜索分组名称"
            type="text"
            @keyup.enter="handleSearch"
          />
        </div>
        <input
          v-model="filterMerchantId"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded-md text-[13px] w-[140px] outline-none focus:border-[#3a64ab] focus:shadow-[0_0_0_2px_rgba(58,100,171,0.1)] transition-all"
          placeholder="商户ID"
          type="text"
          @keyup.enter="handleSearch"
        />
        <button class="py-1.5 px-5 rounded-md bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#2e528e] transition-colors font-medium" @click="handleSearch">查询</button>
        <button class="py-1.5 px-4 rounded-md bg-white border border-[#d9d9d9] text-[#666] text-[13px] cursor-pointer hover:border-[#3a64ab] hover:text-[#3a64ab] transition-all" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- Tree Panel -->
    <div class="bg-white rounded-lg shadow-sm border border-[#f0f0f0] relative" style="min-height: 200px;">
      <div class="px-5 py-3 border-b border-[#f5f5f5] flex justify-between items-center">
        <span class="text-[13px] font-medium text-[#333]">
          分组树
          <span v-if="treeData.length" class="text-[12px] text-[#999] ml-2 font-normal">共 {{ treeData.length }} 个根节点</span>
        </span>
        <span class="text-[11px] text-[#bfbfbf]">点击节点圆圈进行操作</span>
      </div>
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center rounded-b-lg">
        <span class="text-[#3a64ab] text-[13px] animate-pulse">加载中...</span>
      </div>

      <p v-if="errorMsg" class="text-[13px] text-[#ff4d4f] bg-red-50 px-4 py-2.5 mx-5 mt-3 rounded-md">{{ errorMsg }}</p>

      <div class="p-5">
        <TreeNode
          v-for="node in treeData"
          :key="node.id"
          :node="node"
          :depth="0"
        />
        <div v-if="!loading && !errorMsg && treeData.length === 0" class="py-16 text-center">
          <svg class="w-12 h-12 text-[#d9d9d9] mx-auto mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 3v18"/></svg>
          <p class="text-[14px] text-[#999]">暂无分组数据</p>
          <p class="text-[12px] text-[#bfbfbf] mt-1">点击根节点圆圈可创建首个分组</p>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="detailModal" class="fixed inset-0 bg-black/40 z-[90] flex items-center justify-center" @click.self="closeDetail">
          <div class="bg-white rounded-xl shadow-2xl w-[420px] max-w-[92vw] overflow-hidden">
            <div class="px-5 py-3.5 border-b border-[#f0f0f0] flex justify-between items-center">
              <h3 class="text-[15px] font-semibold text-[#333]">分组详情</h3>
              <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none hover:text-[#333] transition-colors" @click="closeDetail">&times;</button>
            </div>

            <div v-if="detailLoading" class="flex items-center justify-center py-16">
              <span class="text-[#3a64ab] text-[13px] animate-pulse">加载中...</span>
            </div>

            <div v-else-if="!detailData" class="flex flex-col items-center py-16 gap-2">
              <span class="text-[13px] text-[#999]">获取详情失败</span>
              <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] border-none cursor-pointer hover:bg-[#264c8d] transition-colors" @click="closeDetail">关闭</button>
            </div>

            <template v-else>
              <div class="px-5 py-5 space-y-3">
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">名称</span>
                  <span class="text-[13px] text-[#333] font-medium">{{ detailData.groupName || detailData.name || '--' }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">描述</span>
                  <span class="text-[13px] text-[#333]">{{ detailData.description || '--' }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">商户</span>
                  <span class="text-[13px] text-[#333]">{{ detailData.merchantName || detailData.merchantId || '--' }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">状态</span>
                  <span class="text-[13px]" :class="detailData.status === 1 ? 'text-[#52c41a]' : 'text-[#ff4d4f]'">{{ detailData.status === 1 ? '启用' : '停用' }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">备注</span>
                  <span class="text-[13px] text-[#333]">{{ detailData.remark || '--' }}</span>
                </div>
                <div class="flex items-center">
                  <span class="text-[13px] text-[#999] w-16 shrink-0">创建时间</span>
                  <span class="text-[13px] text-[#333]">{{ detailData.createTime || '--' }}</span>
                </div>
              </div>
              <div class="px-5 py-3 border-t border-[#f0f0f0] flex justify-end">
                <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="closeDetail">关闭</button>
              </div>
            </template>
          </div>
        </div>
      </Transition>
    </Teleport>

    <ConfirmDialog />
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from,
.toast-leave-to { opacity: 0; transform: translateY(-10px); }

.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
</style>
