<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  icon: { type: String, default: 'info' },
  iconBg: { type: String, default: 'bg-primary-fixed' },
  iconColor: { type: String, default: 'text-primary' },
  trend: { type: String, default: '' },
  trendType: { type: String, default: 'up' },
  trendLabel: { type: String, default: '' },
  valueColor: { type: String, default: 'text-primary' },
})
</script>

<template>
  <div class="bg-surface border border-outline-variant rounded-lg p-md flex flex-col justify-between">
    <div class="flex justify-between items-start">
      <span class="text-label-caps text-on-surface-variant">{{ label }}</span>
      <div class="p-1.5 rounded-lg" :class="[iconBg, iconColor]">
        <span class="material-symbols-outlined text-[20px]">{{ icon }}</span>
      </div>
    </div>
    <div class="mt-md">
      <h2 class="text-display-lg font-bold" :class="valueColor">{{ value }}</h2>
      <div v-if="trend" class="flex items-center gap-xs mt-xs">
        <span
          class="flex items-center text-[12px] font-bold"
          :class="trendType === 'up' ? 'text-error' : 'text-secondary'"
        >
          <span class="material-symbols-outlined text-[14px]">
            {{ trendType === 'up' ? 'trending_up' : 'trending_down' }}
          </span>
          {{ trend }}
        </span>
        <span v-if="trendLabel" class="text-body-sm text-on-surface-variant">{{ trendLabel }}</span>
      </div>
    </div>
  </div>
</template>
