<script setup>
import { ref, reactive, provide, onMounted } from 'vue'
import { getMeterGroupTree, getMeterGroupById, createMeterGroup, updateMeterGroup, deleteMeterGroup } from '@/api/meterGroup.js'
import TreeNode from '@/components/common/TreeNode.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useConfirm } from '@/composables/useConfirm.js'

// ── Filters ─────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterMerchantId = ref('')

// ── Tree data ───────────────────────────────────────────────────────
const treeData = ref([])
const expandedIds = reactive(new Set())
const loading = ref(false)
const errorMsg = ref('')
const fullTree = ref([])
const { showConfirm } = useConfirm()

function normalizeTree(nodes) {
  return (nodes || []).map(node => ({ ...node, label: node.groupName || node.name || node.label || '--', type: node.manageType || node.groupType || node.type || '', children: normalizeTree(node.children) }))
}

function filterTree(nodes, keyword, merchantId) {
  const kw = keyword.toLowerCase()
  return nodes.reduce((result, node) => {
    const children = filterTree(node.children || [], keyword, merchantId)
    const ownMatch = (!kw || node.label.toLowerCase().includes(kw)) && (!merchantId || node.merchantId === merchantId)
    if (ownMatch || children.length) result.push({ ...node, children: children.length ? children : (ownMatch ? node.children : []) })
    return result
  }, [])
}

function buildTree(flatList) {
  const map = {}
  const roots = []
  for (const item of flatList) {
    map[item.id] = { ...item, label: item.name || item.groupName || item.label || '--', children: [] }
  }
  for (const item of flatList) {
    const node = map[item.id]
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
}

async function fetchTree() {
  loading.value = true
  errorMsg.value = ''
  try {
    const merchantId = filterMerchantId.value.trim() ? Number(filterMerchantId.value.trim()) : null
    const res = await getMeterGroupTree(merchantId || undefined)
    fullTree.value = normalizeTree(Array.isArray(res.data) ? res.data : [])
    treeData.value = filterTree(fullTree.value, filterKeyword.value.trim(), merchantId)
    if (filterKeyword.value.trim() || merchantId) expandAll()
    else treeData.value.forEach(n => expandedIds.add(n.id))
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
function collapseAll() { expandedIds.clear(); treeData.value.forEach(n => expandedIds.add(n.id)) }

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
const form = reactive({ show:false, mode:'add', id:null, groupName:'', parentId:null, merchantId:'', manageType:'METERING', description:'' })
const toast = ref('')

function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 3000)
}
function openCreate(parent=null){ Object.assign(form,{show:true,mode:'add',id:null,groupName:'',parentId:parent?.id||null,merchantId:parent?.merchantId||filterMerchantId.value||'',manageType:'METERING',description:''}) }
function closeForm(){form.show=false}
async function submitForm(){if(!form.groupName.trim()){showToast('请输入分组名称');return}if(!form.manageType){showToast('请选择分组管理类型');return}saving.value=true;try{const p={groupName:form.groupName.trim(),parentId:form.parentId||null,merchantId:form.merchantId?Number(form.merchantId):null,manageType:form.manageType,description:form.description||null};await createMeterGroup(p);closeForm();await fetchTree();showToast('保存成功')}catch(e){showToast(e?.response?.data?.msg||e.message||'保存失败')}finally{saving.value=false}}

const addChild = async (parent, name, merchantId, description) => {
  saving.value = true
  try {
    const payload = { groupName: name, parentId: parent.id, merchantId: parent.merchantId, manageType: parent.manageType || parent.groupType || 'METERING' }
    if (description) payload.description = description
    await createMeterGroup(payload)
    await fetchTree()
    // Auto-expand parent to reveal the new child
    expandedIds.add(parent.id)
    showToast('添加成功')
  } catch (e) {
    const msg = e?.response?.data?.msg || e.message || '添加失败'
    showToast(msg)
  } finally { saving.value = false }
}

const editNode = async (node, name, merchantId, description) => {
  saving.value = true
  try {
    const payload = { groupName: name, manageType: node.manageType || node.groupType || 'METERING' }
    if (node.merchantId) payload.merchantId = node.merchantId
    if (description) payload.description = description
    await updateMeterGroup(node.id, payload)
    node.label = name
    showToast('修改成功')
    await fetchTree()
  } catch (e) {
    const msg = e?.response?.data?.msg || e.message || '修改失败'
    showToast(msg)
  } finally { saving.value = false }
}

const deleteNode = async (node) => {
  if (!confirm(`确定删除分组 "${node.label}" 吗？`)) return
  saving.value = true
  try {
    await deleteMeterGroup(node.id)
    showToast('删除成功')
    await fetchTree()
  } catch (e) {
    const msg = e?.response?.data?.msg || e.message || '删除失败'
    showToast(msg)
    await fetchTree()
  } finally { saving.value = false }
}

// ── View detail ─────────────────────────────────────────────────────
const confirmDeleteNode = async (node) => {
  const confirmed = await showConfirm(`确定删除分组“${node.label}”吗？存在子分组时将禁止删除。`, '删除分组')
  if (!confirmed) return
  saving.value = true
  try {
    await deleteMeterGroup(node.id)
    showToast('删除成功')
    await fetchTree()
  } catch (e) {
    showToast(e?.response?.data?.msg || e.message || '删除失败')
  } finally { saving.value = false }
}

const detailNode = async (node) => {
  try {
    const res = await getMeterGroupById(node.id)
    const d = res.data
    const lines = []
    if (d.groupName) lines.push(`名称：${d.groupName}`)
    if (d.description) lines.push(`描述：${d.description}`)
    if (d.merchantName) lines.push(`商户：${d.merchantName}`)
    if (d.status !== undefined) lines.push(`状态：${d.status === 1 ? '启用' : '停用'}`)
    if (d.remark) lines.push(`备注：${d.remark}`)
    lines.push(`创建时间：${d.createTime || '--'}`)
    alert(lines.join('\n'))
  } catch (e) {
    showToast('获取详情失败')
  }
}

provide('addChild', addChild)
provide('editNode', editNode)
provide('deleteNode', confirmDeleteNode)
provide('expandedIds', expandedIds)
provide('detailNode', detailNode)
provide('openCreate', openCreate)

onMounted(() => fetchTree())
</script>

<template>
  <div>
    <ConfirmDialog />
    <!-- Toast -->
    <Teleport to="body">
      <Transition name="toast">
        <div v-if="toast" class="fixed top-5 right-5 z-[200] py-3 px-5 rounded text-[13px] text-white shadow-lg" :class="toast.includes('成功') ? 'bg-[#52c41a]' : 'bg-[#ff4d4f]'">{{ toast }}</div>
      </Transition>
    </Teleport>

    <div class="flex justify-between items-end mb-4">
      <div>
        <h2 class="text-[18px] font-bold mb-1">分组管理</h2>
        <p class="text-[13px] text-[#666]">维护园区、楼栋等层级结构，可添加、修改、删除节点</p>
      </div>
      <div class="flex gap-2.5">
        <button class="py-2 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer" @click="openCreate()">＋ 新增分组</button>
        <button class="py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="collapseAll">全部折叠</button>
        <button class="py-2 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="expandAll">全部展开</button>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input
          v-model="filterKeyword"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[180px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="搜索分组名称"
          type="text"
          @keyup.enter="handleSearch"
        />
        <input
          v-model="filterMerchantId"
          class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[140px] outline-none focus:border-[#3a64ab] transition-colors"
          placeholder="商户ID"
          type="text"
          @keyup.enter="handleSearch"
        />
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="handleSearch">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- Tree Panel -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)] relative min-h-[120px]">
      <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
        <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
      </div>

      <p v-if="errorMsg" class="text-[13px] text-[#ff4d4f] bg-red-50 px-4 py-2.5 rounded-lg mb-3">{{ errorMsg }}</p>

      <TreeNode
        v-for="node in treeData"
        :key="node.id"
        :node="node"
        :depth="0"
      />
      <div v-if="!loading && !errorMsg && treeData.length === 0" class="py-16 text-center text-[14px] text-[#999]">暂无分组数据</div>
      <button v-if="!loading && !errorMsg && treeData.length === 0" class="block mx-auto mt-2 py-2 px-4 rounded bg-[#3a64ab] text-white text-[13px]" @click="openCreate()">＋ 新增分组</button>
    </div>
    <Teleport to="body"><div v-if="form.show" class="fixed inset-0 z-[120] bg-black/30 flex items-center justify-center" @click.self="closeForm"><div class="bg-white rounded-lg p-5 w-[420px] shadow-xl"><h3 class="font-bold mb-4">新增分组</h3><input v-model="form.groupName" class="w-full border rounded p-2 mb-3" placeholder="分组名称"/><input v-model="form.merchantId" class="w-full border rounded p-2 mb-3" placeholder="商户ID（可选）"/><div class="mb-3"><div class="text-[13px] mb-2">分组管理类型</div><label class="mr-5"><input v-model="form.manageType" value="BILLING" type="radio"/> 计费管理</label><label><input v-model="form.manageType" value="METERING" type="radio"/> 计量管理</label></div><textarea v-model="form.description" class="w-full border rounded p-2 mb-4" placeholder="备注（可选）"></textarea><div class="flex justify-end gap-2"><button class="border rounded px-3 py-1.5" @click="closeForm">取消</button><button class="bg-[#3a64ab] text-white rounded px-3 py-1.5" @click="submitForm">保存</button></div></div></div></Teleport>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from,
.toast-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
