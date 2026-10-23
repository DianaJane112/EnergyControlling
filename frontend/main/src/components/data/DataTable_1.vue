<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, required: true },
  selectable: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  rowKey: { type: String, default: 'id' },
})

const emit = defineEmits(['row-click', 'action'])

const selectedIds = ref(new Set())
const selectAll = ref(false)

function toggleAll() {
  if (selectAll.value) {
    selectedIds.value = new Set()
    selectAll.value = false
  } else {
    selectedIds.value = new Set(props.rows.map(r => r[props.rowKey]))
    selectAll.value = true
  }
}

function toggleRow(id) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
    selectAll.value = false
  } else {
    selectedIds.value.add(id)
    if (selectedIds.value.size === props.rows.length) selectAll.value = true
  }
}

function isSelected(id) {
  return selectedIds.value.has(id)
}

function onAction(action, row) {
  emit('action', { action, row })
}
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-left border-collapse">
      <thead class="bg-surface-container-low text-label-caps text-outline uppercase">
        <tr>
          <th v-if="selectable" class="px-lg py-md border-b border-outline-variant w-12">
            <input type="checkbox" class="styled-checkbox" :checked="selectAll" @change="toggleAll" />
          </th>
          <th
            v-for="col in columns"
            :key="col.key"
            class="px-lg py-md border-b border-outline-variant"
            :class="col.align === 'right' ? 'text-right' : ''"
          >
            {{ col.label }}
          </th>
          <th v-if="$slots.actions" class="px-lg py-md border-b border-outline-variant text-right">快捷操作</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-outline-variant">
        <tr
          v-for="row in rows"
          :key="row[rowKey]"
          class="hover:bg-surface-container-low transition-colors cursor-pointer"
          @click="rowClick && $emit('row-click', row)"
        >
          <td v-if="selectable" class="px-lg py-md" @click.stop>
            <input
              type="checkbox"
              :checked="isSelected(row[rowKey])"
              @change="toggleRow(row[rowKey])"
              class="styled-checkbox"
            />
          </td>
          <td
            v-for="col in columns"
            :key="col.key"
            class="px-lg py-md"
            :class="{
              'font-mono text-primary font-bold': col.key === 'meterId' || col.key === 'id',
              'font-mono': col.mono,
              'font-mono text-green-600 font-bold': col.mono && col.highlight === 'positive',
              'font-mono text-error font-bold': col.mono && col.highlight === 'negative',
              'text-right': col.align === 'right',
            }"
          >
            <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
              {{ row[col.key] }}
            </slot>
          </td>
          <td v-if="$slots.actions" class="px-lg py-md text-right" @click.stop>
            <slot name="actions" :row="row" />
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td :colspan="columns.length + (selectable ? 1 : 0) + ($slots.actions ? 1 : 0)" class="px-lg py-xl text-center text-on-surface-variant">
            {{ emptyText }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
