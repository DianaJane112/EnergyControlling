<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  options: { type: Array, default: () => [] }, // { value, label }[]
  placeholder: { type: String, default: '请选择' },
  zIndex: { type: Number, default: 80 },
})

const emit = defineEmits(['update:modelValue'])

const show = ref(false)
const triggerRef = ref(null)
const panelTop = ref('0px')
const panelLeft = ref('0px')

const displayLabel = computed(() => {
  if (!props.modelValue) return ''
  const opt = props.options.find(o => o.value === props.modelValue)
  return opt ? opt.label : ''
})

async function toggle() {
  if (show.value) { show.value = false; return }
  await nextTick()
  if (triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    panelTop.value = `${rect.bottom + 6}px`
    panelLeft.value = `${rect.left}px`
  }
  show.value = true
}

function select(value) {
  emit('update:modelValue', value)
  show.value = false
}

onBeforeUnmount(() => { show.value = false })
</script>

<template>
  <div ref="triggerRef" class="relative inline-block">
    <div
      class="flex items-center justify-between gap-1.5 py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[180px] cursor-pointer bg-white hover:border-[#3a64ab] transition-colors select-none"
      @click="toggle"
    >
      <span :class="modelValue ? 'text-[#333]' : 'text-[#999]'">{{ displayLabel || placeholder }}</span>
      <svg class="w-3 h-3 text-[#999] shrink-0 transition-transform" :class="show ? 'rotate-180' : ''" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </div>

    <Teleport to="body">
      <div v-if="show" class="fixed inset-0" :style="{ zIndex: zIndex }" @click.self="show = false">
        <div
          class="absolute bg-white rounded-lg py-1 min-w-[180px]"
          style="filter: drop-shadow(0 2px 8px rgba(0,0,0,0.15))"
          :style="{ top: panelTop, left: panelLeft }"
        >
          <div class="absolute -top-1.5 left-5 w-0 h-0 border-l-[6px] border-r-[6px] border-b-[6px] border-l-transparent border-r-transparent border-b-white" />
          <div class="max-h-[280px] overflow-y-auto">
            <div
              v-for="opt in options"
              :key="opt.value"
              class="px-4 py-2 text-[13px] cursor-pointer transition-colors"
              :class="modelValue === opt.value
                ? 'bg-[#eef3fa] text-[#3a64ab]'
                : 'text-[#333] hover:bg-[#f5f5f5]'"
              @click="select(opt.value)"
            >{{ opt.label }}</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
