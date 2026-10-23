<script setup>
import { watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '面板' },
})

const emit = defineEmits(['update:open', 'close'])

function close() {
  emit('update:open', false)
  emit('close')
}

watch(() => props.open, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 bg-black/40 z-[60] backdrop-blur-sm"
      @click.self="close"
    >
      <div class="absolute right-0 top-0 h-full w-[480px] bg-white shadow-2xl flex flex-col">
        <div class="p-lg border-b border-outline-variant flex justify-between items-center">
          <h3 class="text-title-sm font-bold">{{ title }}</h3>
          <button class="p-xs hover:bg-surface-container rounded-full" @click="close">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-lg">
          <slot />
        </div>
        <div v-if="$slots.footer" class="p-lg border-t border-outline-variant flex justify-end gap-md">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>
