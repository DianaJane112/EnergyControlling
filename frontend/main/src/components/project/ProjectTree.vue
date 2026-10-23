<script setup>
import { ref } from 'vue'

const props = defineProps({
  nodes: { type: Array, required: true },
})

const emit = defineEmits(['select', 'action'])

const expandedIds = ref(new Set())

function initExpand(nodes) {
  nodes.forEach(node => {
    expandedIds.value.add(node.id)
    if (node.children) initExpand(node.children)
  })
}
initExpand(props.nodes)

function toggleExpand(id) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function isExpanded(id) {
  return expandedIds.value.has(id)
}
</script>

<template>
  <div class="flex flex-col gap-base">
    <template v-for="node in nodes" :key="node.id">
      <div class="tree-node relative group">
        <div
          class="flex items-center p-sm hover:bg-surface-container rounded-lg group transition-colors cursor-pointer border border-transparent hover:border-outline-variant"
          :class="{ 'bg-secondary-fixed/30 border-l-2 border-primary ml-xs': node.active }"
          @click="$emit('select', node)"
        >
          <span
            v-if="node.children"
            class="material-symbols-outlined text-primary mr-sm cursor-pointer"
            :style="isExpanded(node.id) ? { fontVariationSettings: '\'FILL\' 1' } : {}"
            @click.stop="toggleExpand(node.id)"
          >{{ isExpanded(node.id) ? 'expand_more' : 'chevron_right' }}</span>
          <span v-if="node.icon" class="material-symbols-outlined mr-sm" :class="node.iconColor || 'text-on-surface-variant'">{{ node.icon }}</span>
          <span class="text-body-md" :class="node.active ? 'text-primary font-bold' : ''">{{ node.label }}</span>
          <span v-if="node.tag" class="text-label-caps px-sm py-[2px] rounded-full ml-md" :class="node.tagClass || 'bg-primary-container text-on-primary-container'">{{ node.tag }}</span>
          <div class="node-actions ml-auto flex items-center gap-sm opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              v-for="action in (node.actions || [])"
              :key="action.label"
              class="p-xs rounded"
              :class="action.class || 'text-outline hover:bg-surface-container-high'"
              :title="action.label"
              @click.stop="$emit('action', { action: action.key, node })"
            >
              <span class="material-symbols-outlined text-[18px]">{{ action.icon }}</span>
            </button>
          </div>
        </div>

        <!-- Children -->
        <div v-if="node.children && isExpanded(node.id)" class="ml-xl pl-md relative">
          <div class="tree-line-v"></div>
          <div v-for="(child, cIdx) in node.children" :key="child.id">
            <div class="tree-node relative" :class="{ 'mt-xs': cIdx > 0 }">
              <div class="tree-line-h"></div>
              <div
                class="flex items-center p-sm hover:bg-surface-container rounded-lg cursor-pointer border border-transparent hover:border-outline-variant group/child"
                :class="{ 'bg-secondary-fixed/30 border-l-2 border-primary ml-xs': child.active }"
                @click="$emit('select', child)"
              >
                <span
                  v-if="child.children"
                  class="material-symbols-outlined text-primary mr-sm cursor-pointer"
                  :style="isExpanded(child.id) ? { fontVariationSettings: '\'FILL\' 1' } : {}"
                  @click.stop="toggleExpand(child.id)"
                >{{ isExpanded(child.id) ? 'expand_more' : 'chevron_right' }}</span>
                <span v-if="child.icon" class="material-symbols-outlined mr-sm" :class="child.iconColor || 'text-on-surface-variant'">{{ child.icon }}</span>
                <span class="text-body-md" :class="child.active ? 'text-primary font-bold' : ''">{{ child.label }}</span>
                <span v-if="child.tag" class="text-label-caps px-sm py-[2px] rounded-full ml-md" :class="child.tagClass || 'bg-surface-container-high text-on-surface-variant'">{{ child.tag }}</span>
                <div class="node-actions ml-auto flex items-center gap-sm opacity-0 group-hover/child:opacity-100 transition-opacity">
                  <button
                    v-for="action in (child.actions || [])"
                    :key="action.label"
                    class="p-xs rounded"
                    :class="action.class || 'text-outline hover:bg-surface-container-high'"
                    :title="action.label"
                    @click.stop="$emit('action', { action: action.key, node: child })"
                  >
                    <span class="material-symbols-outlined text-[18px]">{{ action.icon }}</span>
                  </button>
                </div>
              </div>
              <!-- Recursive grandchildren (L3) -->
              <div v-if="child.children && isExpanded(child.id)" class="ml-xl pl-md relative">
                <div class="tree-line-v"></div>
                <div v-for="(grandchild, gIdx) in child.children" :key="grandchild.id">
                  <div class="tree-node relative" :class="{ 'mt-xs': gIdx > 0 }">
                    <div class="tree-line-h"></div>
                    <div
                      class="flex items-center p-sm hover:bg-surface-container rounded-lg cursor-pointer border border-transparent hover:border-outline-variant group/gc"
                      :class="{ 'bg-secondary-fixed/30 border-l-2 border-primary ml-xs': grandchild.active }"
                      @click="$emit('select', grandchild)"
                    >
                      <span v-if="grandchild.icon" class="material-symbols-outlined mr-sm text-outline">{{ grandchild.icon }}</span>
                      <span class="text-body-sm" :class="grandchild.active ? 'text-primary font-bold' : ''">{{ grandchild.label }}</span>
                      <span v-if="grandchild.tag" class="text-label-caps px-sm py-[2px] rounded-full ml-md bg-surface-container-high text-on-surface-variant">{{ grandchild.tag }}</span>
                      <div class="node-actions ml-auto flex items-center gap-sm opacity-0 group-hover/gc:opacity-100 transition-opacity">
                        <button
                          v-for="action in (grandchild.actions || [])"
                          :key="action.label"
                          class="p-xs rounded"
                          :class="action.class || 'text-outline hover:bg-surface-container-high'"
                          :title="action.label"
                          @click.stop="$emit('action', { action: action.key, node: grandchild })"
                        >
                          <span class="material-symbols-outlined text-[18px]">{{ action.icon }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.tree-line-v {
  position: absolute;
  left: 20px;
  top: -12px;
  bottom: 12px;
  width: 1px;
  background-color: #c4c5d5;
}
.tree-line-h {
  position: absolute;
  left: 20px;
  top: 20px;
  width: 16px;
  height: 1px;
  background-color: #c4c5d5;
}
</style>
