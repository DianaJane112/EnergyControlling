<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lightning, WaterRateTwo } from '@icon-park/vue-next'
import { elecMenuItems, waterMenuItems } from '@/config/navigation.js'

const route = useRoute()
const router = useRouter()

function buildLabelMap(items) {
  const map = {}
  for (const item of items) {
    if (item.children) {
      for (const child of item.children) {
        map[child.route] = item.label + ' - ' + child.label
      }
    } else if (item.route) {
      map[item.route] = item.label
    }
  }
  return map
}

const labelMap = { ...buildLabelMap(elecMenuItems), ...buildLabelMap(waterMenuItems) }

function routeInMenu(path, items) {
  return items.some(item => {
    if (item.route === path) return true
    return item.children?.some(c => c.route === path)
  })
}
function isElectricTab(path) { return routeInMenu(path, elecMenuItems) }
function isWaterTab(path) { return routeInMenu(path, waterMenuItems) }
function isSharedTab(path) { return isElectricTab(path) && isWaterTab(path) }

const tabs = ref([
  { label: '首页', route: '/dashboard', pinned: true }
])

function syncTab(path) {
  const existingIdx = tabs.value.findIndex(t => t.route === path)
  if (existingIdx === -1) {
    const label = labelMap[path] || document.title || path
    tabs.value.splice(1, 0, { label, route: path })
  }
}

if (route.path !== '/dashboard') syncTab(route.path)

watch(() => route.path, syncTab)

function closeTab(index) {
  if (tabs.value[index].pinned) return
  const closing = tabs.value[index]
  const isActive = closing.route === route.path
  tabs.value.splice(index, 1)
  if (isActive) {
    const next = tabs.value[Math.min(index, tabs.value.length - 1)]
    router.push(next.route)
  }
}

function activateTab(tab) {
  router.push(tab.route)
}
</script>

<template>
  <div v-if="tabs.length > 0" class="flex bg-[#f0f2f5] pt-2 pb-1 px-2.5 mb-2">
    <div
      v-for="(tab, index) in tabs"
      :key="tab.route"
      class="py-2.5 px-3 bg-white mr-[5px] rounded-md text-[13px] border border-gray-200 cursor-pointer w-[250px] shrink-0 flex items-center gap-1.5"
      :class="route.path === tab.route ? '!bg-[#e8f0fe] !border-[#3a64ab] text-[#1e293b] font-bold' : 'text-[#666]'"
      @click="activateTab(tab)"
    >
      <Lightning v-if="isElectricTab(tab.route) && !isSharedTab(tab.route)" theme="two-tone" size="16" :fill="route.path === tab.route ? ['#292929', '#82a8ff'] : ['#bfbfbf', '#d9d9d9']" :strokeWidth="2"/>
      <WaterRateTwo v-else-if="isWaterTab(tab.route) && !isSharedTab(tab.route)" theme="multi-color" size="16" :fill="route.path === tab.route ? ['#292929', '#82a8ff', '#82a8ff', '#2F88FF'] : ['#bfbfbf', '#d9d9d9', '#d9d9d9', '#d9d9d9']" :strokeWidth="2"/>
      <span class="truncate flex-1 min-w-0 text-left">{{ tab.label }}</span>
      <span v-if="!tab.pinned" class="text-[#999] text-xs hover:text-[#ff4d4f] shrink-0 leading-none" @click.stop="closeTab(index)">&#x2715;</span>
    </div>
  </div>
</template>

