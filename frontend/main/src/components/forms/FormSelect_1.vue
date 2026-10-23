<script setup>
defineProps({
  label: { type: String, required: true },
  modelValue: { type: String, default: '' },
  options: { type: Array, required: true },
  error: { type: String, default: '' },
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div>
    <label class="text-label-caps text-outline block mb-xs">{{ label }}</label>
    <select
      :value="modelValue"
      class="w-full h-10 px-md border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary text-body-md outline-none bg-white"
      :class="error ? 'border-error' : 'border-outline-variant'"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
    </select>
    <p v-if="error" class="text-error text-body-sm mt-xs">{{ error }}</p>
  </div>
</template>
