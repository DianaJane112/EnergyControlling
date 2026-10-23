<script setup>
defineProps({
  options: { type: Array, required: true },
  show: { type: Boolean, default: false },
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
})

const emit = defineEmits(['select', 'close'])

function onSelect(option) {
  emit('select', option)
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-[70]" @click="emit('close')">
      <div
        class="absolute bg-surface border border-outline-variant rounded-lg shadow-lg py-xs min-w-[160px]"
        :style="{ left: x + 'px', top: y + 'px' }"
        @click.stop
      >
        <button
          v-for="option in options"
          :key="option.label"
          class="w-full flex items-center gap-sm px-md py-sm text-body-sm hover:bg-surface-container-low transition-colors text-left"
          :class="option.danger ? 'text-error' : 'text-on-surface'"
          @click="onSelect(option)"
        >
          <span v-if="option.icon" class="material-symbols-outlined text-[18px]">{{ option.icon }}</span>
          {{ option.label }}
        </button>
      </div>
    </div>
  </Teleport>
</template>
