<script setup>
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  level: { type: Number, default: 0 },
  selectedId: { type: [String, Number], default: '' },
  expanded: { type: Set, required: true },
})

const emit = defineEmits(['toggle', 'select'])

const hasChildren = computed(() => Boolean(props.node.children?.length))
const isOpen = computed(() => props.node.forceOpen || props.expanded.has(props.node.id))
</script>

<template>
  <div role="none">
    <div
      role="treeitem"
      :aria-expanded="hasChildren ? isOpen : undefined"
      class="flex items-center min-h-9 hover:bg-[#f5f7fa]"
      :class="String(selectedId) === String(node.id) ? 'bg-[#eef3fa] text-[#3a64ab]' : 'text-[#333]'"
      :style="{ paddingLeft: (level * 18 + 8) + 'px' }"
    >
      <button
        type="button"
        class="w-7 h-7 shrink-0 text-[#777]"
        :class="!hasChildren && 'invisible'"
        :aria-label="isOpen ? '收起子分组' : '展开子分组'"
        @click="emit('toggle', node.id)"
      >{{ isOpen ? '⌄' : '›' }}</button>
      <button
        type="button"
        class="flex-1 min-w-0 flex items-center gap-2 py-2 pr-3 text-left"
        @click="emit('select', node)"
      >
        <span aria-hidden="true">▣</span>
        <span class="truncate">{{ node.groupName }}</span>
      </button>
    </div>
    <template v-if="hasChildren && isOpen">
      <GroupSelectorNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :selected-id="selectedId"
        :expanded="expanded"
        @toggle="emit('toggle', $event)"
        @select="emit('select', $event)"
      />
    </template>
  </div>
</template>
