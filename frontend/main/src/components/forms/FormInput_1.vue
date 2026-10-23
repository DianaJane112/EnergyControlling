<script setup>
defineProps({
  label: { type: String, required: true },
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div>
    <label class="text-label-caps text-outline block mb-xs">{{ label }}</label>
    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      class="w-full h-10 px-md border rounded-lg focus:ring-2 focus:ring-primary focus:border-primary text-body-md outline-none"
      :class="error ? 'border-error' : 'border-outline-variant'"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <p v-if="error" class="text-error text-body-sm mt-xs">{{ error }}</p>
  </div>
</template>
