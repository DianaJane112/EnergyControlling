<script setup>
import { ref, computed, inject } from 'vue'
import { BuildingFour, Down } from '@icon-park/vue-next'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  isLast: { type: Boolean, default: false },
})

const hasChildren = () => props.node.children && props.node.children.length > 0

// ── inject ────────────────────────────────────────────────────────
const addChild = inject('addChild')
const editNode = inject('editNode')
const deleteNode = inject('deleteNode')
const expandedIds = inject('expandedIds')
const detailNode = inject('detailNode', null)

const expanded = computed({
  get: () => expandedIds.has(props.node.id),
  set: (v) => { if (v) expandedIds.add(props.node.id); else expandedIds.delete(props.node.id) }
})

// ── Depth-based colors ────────────────────────────────────────────
const depthColors = ['#1890ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']
const dotColor = depthColors[props.depth % depthColors.length]
const lineColor = props.depth > 0 ? depthColors[(props.depth - 1) % depthColors.length] : '#d9d9d9'

// ── Click bubble ──────────────────────────────────────────────────
const bubble = ref({ show: false, x: 0, y: 0 })
const editMode = ref(null)
const editName = ref('')
const editMerchantId = ref('')
const editDescription = ref('')

function onDotClick(e) {
  e.stopPropagation()
  bubble.value = { show: true, x: e.clientX, y: e.clientY }
  editMode.value = null
  editName.value = ''
  editMerchantId.value = ''
  editDescription.value = ''
}

function closeBubble() { bubble.value.show = false; editMode.value = null }
function startAdd() {
  editMode.value = 'add'
  editName.value = ''
  editMerchantId.value = ''
  editDescription.value = ''
}
function startEdit() {
  editMode.value = 'edit'
  editName.value = props.node.label
  editMerchantId.value = props.node.merchantId ? String(props.node.merchantId) : ''
  editDescription.value = props.node.description || ''
}

function confirmEdit() {
  const name = editName.value.trim()
  if (!name) return
  const mid = editMerchantId.value.trim() || null
  const desc = editDescription.value.trim() || null
  if (editMode.value === 'add') addChild(props.node, name, mid, desc)
  else if (editMode.value === 'edit') editNode(props.node, name, mid, desc)
  closeBubble()
}

function handleDelete() { deleteNode(props.node); closeBubble() }
function handleDetail() { detailNode?.(props.node); closeBubble() }
</script>

<template>
  <div>
    <!-- node row -->
    <div
      class="flex items-center gap-1.5 h-[34px] relative"
      :style="{ paddingLeft: depth * 28 + 'px' }"
    >
      <!-- vertical line toward parent -->
      <div
        v-if="depth > 0"
        class="absolute left-0 border-l-2"
        :class="isLast ? 'h-[17px]' : 'h-full'"
        :style="{ left: (depth - 1) * 28 + 14 + 'px', top: 0, borderColor: lineColor }"
      ></div>

      <!-- horizontal line connector -->
      <div
        v-if="depth > 0"
        class="shrink-0 border-t-2"
        style="width: 12px;"
        :style="{ borderColor: lineColor }"
      ></div>

      <!-- expand/collapse arrow -->
      <span
        v-if="hasChildren()"
        class="shrink-0 w-[14px] text-center text-[10px] text-[#999] cursor-pointer leading-none select-none transition-transform duration-150"
        :class="expanded ? 'rotate-90' : ''"
        @click.stop="expanded = !expanded"
      >&#9654;</span>
      <span v-else class="shrink-0 w-[14px]"></span>

      <button
        class="shrink-0 w-7 h-7 rounded-full border bg-white flex items-center justify-center cursor-pointer hover:shadow-sm transition-all relative z-10"
        :style="{ borderColor: dotColor, color: dotColor }"
        title="分组操作"
        @click.stop="onDotClick"
      ><BuildingFour theme="outline" size="15" /></button>

      <!-- label -->
      <button class="text-[13px] text-[#333] select-none whitespace-nowrap bg-transparent border-0 cursor-pointer flex items-center gap-1" @click.stop="onDotClick">
        {{ node.label }}
        <Down v-if="hasChildren()" theme="outline" size="12" :class="expanded ? 'rotate-180' : ''" />
      </button>

      <!-- type tag -->
      <span v-if="node.type" class="text-[11px] text-[#999] bg-[#f5f5f5] px-1.5 py-0.5 rounded shrink-0 ml-1">{{ node.type }}</span>
    </div>

    <!-- children -->
    <div v-if="hasChildren() && expanded">
      <TreeNode
        v-for="(child, idx) in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :isLast="idx === node.children.length - 1"
      />
    </div>

    <!-- ═══ Click bubble ═══ -->
    <Teleport to="body">
      <div v-if="bubble.show" class="fixed inset-0 z-[80]" @click="closeBubble">
        <div
          class="absolute bg-white border border-[#d9d9d9] rounded-lg shadow-lg p-3 min-w-[220px]"
          :style="{ left: bubble.x + 'px', top: bubble.y + 'px' }"
          @click.stop
        >
          <template v-if="!editMode">
            <div class="text-[13px] py-1.5 px-2 hover:bg-[#f5f5f5] rounded cursor-pointer mb-1" @click="startAdd">添加子节点</div>
            <div class="text-[13px] py-1.5 px-2 hover:bg-[#f5f5f5] rounded cursor-pointer mb-1" @click="startEdit">修改</div>
            <div v-if="detailNode" class="text-[13px] py-1.5 px-2 hover:bg-[#f5f5f5] rounded cursor-pointer mb-1" @click="handleDetail">详情</div>
            <div class="text-[13px] py-1.5 px-2 hover:bg-[#fff2f0] rounded cursor-pointer text-[#ff4d4f]" @click="handleDelete">删除</div>
          </template>
          <template v-else>
            <div class="text-[12px] text-[#999] mb-3">{{ editMode === 'add' ? '添加子节点' : '修改节点' }}</div>
            <input
              v-model="editName"
              class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] mb-2"
              placeholder="分组名称"
              type="text"
              @keyup.enter="confirmEdit"
            />
            <input
              v-model="editDescription"
              class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] mb-2"
              placeholder="分组描述（可选）"
              type="text"
            />
            <input
              v-model="editMerchantId"
              class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none focus:border-[#3a64ab] mb-2"
              placeholder="商户ID（可选）"
              type="text"
              @keyup.enter="confirmEdit"
            />
            <div class="flex justify-end gap-2">
              <button class="py-1 px-3 rounded text-[12px] bg-white border border-[#d9d9d9] text-[#333] cursor-pointer hover:border-[#3a64ab]" @click="closeBubble">取消</button>
              <button class="py-1 px-3 rounded text-[12px] bg-[#3a64ab] text-white border-none cursor-pointer hover:bg-[#264c8d]" @click="confirmEdit">确定</button>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>
