<script setup>
import { ref, computed, inject } from 'vue'

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
const depthColors = ['#3a64ab', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']
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
  const rect = e.currentTarget.getBoundingClientRect()
  const popupW = 210
  const popupH = 340
  let x = rect.left
  let y = rect.bottom + 4
  if (x + popupW > window.innerWidth) x = Math.max(4, rect.right - popupW)
  if (y + popupH > window.innerHeight) y = rect.top - popupH - 4
  bubble.value = { show: true, x, y }
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

function countDescendants(node) {
  let c = 0
  if (node.children) {
    for (const ch of node.children) {
      c += 1 + countDescendants(ch)
    }
  }
  return c
}

const descendantCount = computed(() => countDescendants(props.node))
</script>

<template>
  <div>
    <!-- node row -->
    <div
      class="flex items-center gap-1.5 h-[36px] relative group/node rounded-md -ml-1 pr-2 hover:bg-[#f8f9fc] transition-colors duration-150"
      :style="{ paddingLeft: depth * 28 + 'px' }"
    >
      <!-- vertical line toward parent -->
      <div
        v-if="depth > 0"
        class="absolute border-l-2"
        :class="isLast ? 'h-[18px]' : 'h-full'"
        :style="{ left: (depth - 1) * 28 + 14 + 'px', top: 0, borderColor: lineColor + '40' }"
      ></div>

      <!-- horizontal line connector -->
      <div
        v-if="depth > 0"
        class="shrink-0 border-t-2"
        style="width: 14px;"
        :style="{ borderColor: lineColor + '40' }"
      ></div>

      <!-- expand/collapse arrow -->
      <span
        v-if="hasChildren()"
        class="shrink-0 w-[16px] text-center text-[11px] text-[#999] cursor-pointer leading-none select-none transition-transform duration-200 hover:text-[#3a64ab]"
        :class="expanded ? 'rotate-90' : ''"
        @click.stop="expanded = !expanded"
      >&#9654;</span>
      <span v-else class="shrink-0 w-[16px]"></span>

      <!-- circular ring node -->
      <span
        class="shrink-0 w-[20px] h-[20px] rounded-full border-2 bg-white flex items-center justify-center cursor-pointer hover:scale-125 transition-all duration-150 relative z-10 shadow-sm"
        :class="hasChildren() ? 'hover:shadow-md' : ''"
        :style="{ borderColor: dotColor }"
        @click.stop="onDotClick"
      >
        <span class="w-[8px] h-[8px] rounded-full transition-transform duration-150" :style="{ backgroundColor: dotColor }"></span>
      </span>

      <!-- label -->
      <span class="text-[13px] text-[#333] select-none whitespace-nowrap font-medium">{{ node.label }}</span>

      <!-- children count badge -->
      <span v-if="hasChildren()" class="text-[11px] text-[#999] bg-[#f0f0f0] rounded-full px-1.5 py-px leading-[18px] shrink-0 ml-0.5">
        {{ descendantCount }}
      </span>

      <!-- type tag -->
      <span v-if="node.type" class="text-[11px] text-[#3a64ab] bg-[#e8f0fe] px-1.5 py-px rounded shrink-0 ml-1">{{ node.type }}</span>
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
      <Transition name="bubble-pop">
        <div v-if="bubble.show" class="fixed inset-0 z-[80]" @click="closeBubble">
          <div
            class="absolute bg-white border border-[#e8e8e8] rounded-xl shadow-[0_8px_30px_rgba(0,0,0,0.12)] py-1.5 min-w-[200px] overflow-hidden"
            :style="{ left: bubble.x + 'px', top: bubble.y + 'px' }"
            @click.stop
          >
            <template v-if="!editMode">
              <!-- node name header -->
              <div class="px-3 pb-1.5 mb-1 border-b border-[#f5f5f5]">
                <span class="text-[11px] text-[#bfbfbf] uppercase tracking-wide">当前节点</span>
                <div class="text-[13px] font-medium text-[#333] leading-tight mt-0.5 truncate max-w-[190px]">{{ node.label }}</div>
              </div>
              <div class="flex items-center gap-2.5 px-3 py-1.5 text-[13px] hover:bg-[#f5f7fa] cursor-pointer transition-colors" @click="startAdd">
                <svg class="w-4 h-4 text-[#52c41a]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>
                <span>添加子节点</span>
              </div>
              <div class="flex items-center gap-2.5 px-3 py-1.5 text-[13px] hover:bg-[#f5f7fa] cursor-pointer transition-colors" @click="startEdit">
                <svg class="w-4 h-4 text-[#3a64ab]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                <span>修改</span>
              </div>
              <div v-if="detailNode" class="flex items-center gap-2.5 px-3 py-1.5 text-[13px] hover:bg-[#f5f7fa] cursor-pointer transition-colors" @click="handleDetail">
                <svg class="w-4 h-4 text-[#13c2c2]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
                <span>详情</span>
              </div>
              <div class="border-t border-[#f5f5f5] mt-1 pt-1">
                <div class="flex items-center gap-2.5 px-3 py-1.5 text-[13px] hover:bg-[#fff2f0] cursor-pointer text-[#fa8c16] transition-colors" @click="handleDelete">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg>
                  <span>停用</span>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="px-3 pt-1 pb-2">
                <span class="text-[12px] text-[#999]">{{ editMode === 'add' ? '添加子节点' : '修改节点' }}</span>
              </div>
              <div class="px-3 space-y-2">
                <input
                  v-model="editName"
                  class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded-md text-[13px] outline-none focus:border-[#3a64ab] focus:shadow-[0_0_0_2px_rgba(58,100,171,0.1)] transition-all"
                  placeholder="分组名称"
                  type="text"
                  @keyup.enter="confirmEdit"
                />
                <input
                  v-model="editDescription"
                  class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded-md text-[13px] outline-none focus:border-[#3a64ab] focus:shadow-[0_0_0_2px_rgba(58,100,171,0.1)] transition-all"
                  placeholder="分组描述（可选）"
                  type="text"
                />
                <input
                  v-model="editMerchantId"
                  class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded-md text-[13px] outline-none focus:border-[#3a64ab] focus:shadow-[0_0_0_2px_rgba(58,100,171,0.1)] transition-all"
                  placeholder="商户ID（可选）"
                  type="text"
                  @keyup.enter="confirmEdit"
                />
              </div>
              <div class="flex justify-end gap-2 px-3 pt-2.5 pb-1.5 border-t border-[#f5f5f5] mt-2.5">
                <button class="py-1.5 px-3.5 rounded-md text-[12px] bg-white border border-[#d9d9d9] text-[#666] cursor-pointer hover:border-[#3a64ab] hover:text-[#3a64ab] transition-all" @click="closeBubble">取消</button>
                <button class="py-1.5 px-4 rounded-md text-[12px] bg-[#3a64ab] text-white border-none cursor-pointer hover:bg-[#2e528e] transition-colors font-medium" @click="confirmEdit">确定</button>
              </div>
            </template>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.bubble-pop-enter-active { transition: all 0.18s cubic-bezier(0.22, 0.61, 0.36, 1); }
.bubble-pop-leave-active { transition: all 0.12s ease-in; }
.bubble-pop-enter-from { opacity: 0; transform: translateY(-6px) scale(0.95); }
.bubble-pop-leave-to { opacity: 0; transform: translateY(-4px) scale(0.97); }
</style>
