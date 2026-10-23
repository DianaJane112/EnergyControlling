<script setup>
import { ref, computed, provide, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lightning, WaterRateTwo } from '@icon-park/vue-next'
import { elecMenuItems, waterMenuItems } from '@/config/navigation.js'

const route = useRoute()
const router = useRouter()

// ── Flatten menu routes for mode detection ────────────────────────────
function flattenRoutes(items) {
  const routes = new Set()
  for (const item of items) {
    if (item.route) routes.add(item.route)
    if (item.children) for (const c of item.children) routes.add(c.route)
  }
  return routes
}
const elecRouteSet = flattenRoutes(elecMenuItems)
const waterRouteSet = flattenRoutes(waterMenuItems)

// ── Collapse / Expand ──────────────────────────────────────────────────
const collapsed = ref(false)
function toggleCollapse() { collapsed.value = !collapsed.value }

// ── Energy mode ──────────────────────────────────────────────────────
const energyMode = ref(localStorage.getItem('energyMode') || 'electric')
provide('energyMode', energyMode)

const modeSelected = ref(localStorage.getItem('modeSelected') === 'true')

// Auto-switch mode when navigating to a mode-specific page via tabs
watch(() => route.path, (path) => {
  const inElec = elecRouteSet.has(path)
  const inWater = waterRouteSet.has(path)
  if (inElec && !inWater && energyMode.value !== 'electric') {
    energyMode.value = 'electric'
    localStorage.setItem('energyMode', 'electric')
    modeSelected.value = true
    localStorage.setItem('modeSelected', 'true')
  } else if (inWater && !inElec && energyMode.value !== 'water') {
    energyMode.value = 'water'
    localStorage.setItem('energyMode', 'water')
    modeSelected.value = true
    localStorage.setItem('modeSelected', 'true')
  }
})

const menuItems = computed(() => energyMode.value === 'electric' ? elecMenuItems : waterMenuItems)

const expandedGroups = ref(new Set(
  menuItems.value.filter(item => item.expanded).map(item => item.label)
))

function findGroupForPath(items, path) {
  for (const item of items) {
    if (item.route === path) return item.label
    if (item.children && item.children.some(c => c.route === path)) return item.label
  }
  return null
}

watch(energyMode, () => {
  const activeGroup = findGroupForPath(menuItems.value, route.path)
  if (activeGroup) {
    expandedGroups.value = new Set([activeGroup])
  } else {
    expandedGroups.value = new Set(
      menuItems.value.filter(item => item.expanded).map(item => item.label)
    )
  }
})

function toggleGroup(label) {
  if (expandedGroups.value.has(label)) {
    expandedGroups.value.delete(label)
  } else {
    expandedGroups.value = new Set([label])
  }
}
function isGroupActive(item) {
  if (!item.children) return false
  return item.children.some(c => route.path === c.route)
}
function isChildActive(child) {
  return route.path === child.route
}
function navigateTo(r) {
  if (r) router.push(r)
}

const modeLabel = computed(() => energyMode.value === 'electric' ? '电能管理' : '水能管理')

// ── Mode panel ───────────────────────────────────────────────────────
const modePanelOpen = ref(false)
function openModePanel() { modePanelOpen.value = true }
function closeModePanel() { modePanelOpen.value = false }

const panelElecCategories = computed(() => elecMenuItems.filter(item => item.label !== '首页'))
const panelWaterCategories = computed(() => waterMenuItems.filter(item => item.label !== '首页'))

function selectMode(mode, item) {
  energyMode.value = mode
  localStorage.setItem('energyMode', mode)
  modeSelected.value = true
  localStorage.setItem('modeSelected', 'true')
  modePanelOpen.value = false
  // Find the matching item in the target mode's actual menu
  const targetMenu = mode === 'electric' ? elecMenuItems : waterMenuItems
  const targetItem = targetMenu.find(m => m.label === item.label)
  // Expand the target group after the mode watcher fires
  nextTick(() => {
    expandedGroups.value = new Set([item.label])
  })
  if (targetItem) {
    if (targetItem.children && targetItem.children.length > 0) {
      router.push(targetItem.children[0].route)
    } else if (targetItem.route) {
      router.push(targetItem.route)
    }
  }
}
</script>

<template>
  <!-- ═══ Collapsed state ═══ -->
  <aside
    v-if="collapsed"
    class="w-[36px] bg-transparent flex flex-col z-10 shrink-0 relative select-none"
  >
    <!-- Top icon bar — matches TopAppBar background -->
    <div class="h-[60px] bg-[#3a64ab] flex items-center justify-center shrink-0">
      <Lightning v-if="energyMode === 'electric'" theme="two-tone" size="28" :fill="['#292929', '#82a8ff']" :strokeWidth="2"/>
      <WaterRateTwo v-else theme="multi-color" size="28" :fill="['#fff', '#c8daff', '#82a8ff', '#2F88FF']" :strokeWidth="2"/>
    </div>

    <!-- Blue expand trapezoid tab — inside the collapsed strip -->
    <div class="nav-toggle nav-toggle--expand" @click="toggleCollapse" title="展开导航">
      <span class="nav-toggle-text">展<br>开</span>
    </div>

  </aside>

  <!-- ═══ Expanded state ═══ -->
  <aside v-else class="w-[190px] bg-white flex flex-col z-10 shrink-0 relative mr-[32px]">
    <!-- Header with mode switcher -->
    <div class="h-[60px] bg-[#3a64ab] text-white flex items-center px-4 gap-2 shrink-0 cursor-pointer hover:bg-[#264c8d] transition-colors select-none -mr-[32px]" @click="openModePanel">
      <template v-if="!modeSelected">
        <span class="transition-transform duration-300 flex items-center" :class="{ 'rotate-180': modePanelOpen }">
          <svg viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="22" height="22"><path d="M323.038384 481.228283H188.185859c-75.507071 0-137.050505-61.414141-137.050505-137.050505V209.19596c0-75.507071 61.414141-137.050505 137.050505-137.050505h134.852525c75.507071 0 137.050505 61.414141 137.050505 137.050505v134.852525c0 75.636364-61.543434 137.179798-137.050505 137.179798z m0 0M642.650505 475.927273l-95.418182-95.418182c-53.39798-53.39798-53.39798-140.412121 0-193.810101l95.418182-95.418182c53.39798-53.39798 140.412121-53.39798 193.810101 0l95.418182 95.418182c53.39798 53.39798 53.39798 140.412121 0 193.810101l-95.418182 95.418182c-53.39798 53.527273-140.412121 53.527273-193.810101 0z m152.565657-343.272728c-30.771717-30.771717-80.678788-30.771717-111.450505 0l-95.418182 95.418182c-30.771717 30.771717-30.771717 80.678788 0 111.450505l95.418182 95.418182c30.771717 30.771717 80.678788 30.771717 111.450505 0l95.418181-95.418182c30.771717-30.771717 30.771717-80.678788 0-111.450505l-95.418181-95.418182zM323.038384 973.575758H188.185859c-75.507071 0-137.050505-61.414141-137.050505-137.050505V701.672727c0-75.507071 61.414141-137.050505 137.050505-137.050505h134.852525c75.507071 0 137.050505 61.543434 137.050505 137.050505V836.525253c0 75.636364-61.543434 137.050505-137.050505 137.050505z m0 0" fill="#fff"></path><path d="M812.929293 973.575758H678.076768c-75.507071 0-137.050505-61.414141-137.050505-137.050505V701.672727c0-75.507071 61.414141-137.050505 137.050505-137.050505h134.852525c75.507071 0 137.050505 61.543434 137.050505 137.050505V836.525253c0 75.636364-61.414141 137.050505-137.050505 137.050505z m0 0" fill="#fff"></path></svg>
        </span>
      </template>
      <template v-else>
        <Lightning v-if="energyMode === 'electric'" theme="two-tone" size="32" :fill="['#292929', '#82a8ff']" :strokeWidth="2"/>
        <WaterRateTwo v-else theme="multi-color" size="32" :fill="['#292929', '#82a8ff', '#82a8ff', '#2F88FF']" :strokeWidth="2"/>
        <span class="font-bold text-sm flex-1">{{ modeLabel }}</span>
        <span class="text-[10px] leading-none opacity-60">▼</span>
      </template>
    </div>

    <nav class="flex-1 overflow-y-auto pt-2.5">
      <template v-for="item in menuItems" :key="item.label">
        <div
          class="px-4 py-3 cursor-pointer flex justify-between items-center text-[#333] hover:text-[#3a64ab] hover:bg-[#f5f6fb] transition-colors"
          :class="{ 'text-[#3a64ab] font-semibold bg-[#eef3fa]': isGroupActive(item) || route.path === item.route }"
          @click="item.children && item.children.length ? toggleGroup(item.label) : navigateTo(item.route)"
        >
          <span>{{ item.label }}</span>
          <span v-if="item.children" class="text-[10px] transition-transform duration-200" :class="{ 'rotate-180': expandedGroups.has(item.label) }">▼</span>
        </div>
        <div v-if="item.children && expandedGroups.has(item.label)" class="bg-[#f5f6fb]">
          <div
            v-for="child in item.children"
            :key="child.route"
            class="py-2.5 pl-8 pr-4 cursor-pointer text-[#666] text-[13px] hover:text-[#3a64ab] hover:bg-[#dce5f5] transition-colors"
            :class="{ 'bg-[#dce5f5] text-[#3a64ab] font-semibold ': isChildActive(child) }"
            @click="navigateTo(child.route)"
          >{{ child.label }}</div>
        </div>
      </template>
    </nav>

    <!-- Collapse trapezoid tab — outside right edge -->
    <div class="nav-toggle nav-toggle--collapse" @click="toggleCollapse" title="收起导航">
      <span class="nav-toggle-text">收<br>起</span>
    </div>

  </aside>

  <!-- ═══ Shared Mode Selection Drawer ═══ -->
  <Teleport to="body">
    <Transition name="drawer-fade">
      <div v-if="modePanelOpen" class="fixed inset-0 z-[100] bg-black/30" @click="closeModePanel"></div>
    </Transition>
    <Transition name="drawer-slide">
      <div v-if="modePanelOpen" class="fixed top-0 h-full w-[380px] bg-white shadow-2xl z-[101] overflow-y-auto flex flex-col" :style="{ left: collapsed ? '36px' : '190px' }">
        <div class="flex justify-between items-center p-5 border-b border-[#f0f0f0] shrink-0">
          <span class="text-[15px] font-medium text-[#333]">选择管理类型</span>
          <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="closeModePanel">&times;</button>
        </div>
        <div class="p-5 flex-1 overflow-y-auto">
          <div class="mb-6">
            <div class="flex items-center gap-2 mb-3 pb-3 border-b border-[#f0f0f0]">
              <span class="text-2xl">
                <Lightning theme="two-tone" size="41" :fill="['#292929', '#82a8ff']" :strokeWidth="2"/>
              </span>
              <span class="text-[16px] font-bold" :class="energyMode === 'electric' ? 'text-[#3a64ab]' : 'text-[#333]'">电能管理</span>
              <span v-if="energyMode === 'electric'" class="text-[11px] bg-[#eef3fa] text-[#3a64ab] px-1.5 py-0.5 rounded ml-auto">当前</span>
            </div>
            <div v-for="item in panelElecCategories" :key="'elec-' + item.label"
              class="py-2.5 px-3 rounded cursor-pointer flex justify-between items-center text-[13px] hover:bg-[#f5f5f5] transition-colors"
              @click="selectMode('electric', item)">
              <span>{{ item.label }}</span>
              <span class="text-[#999] text-[11px]">{{ item.children ? item.children.length + '项' : '' }}</span>
            </div>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-3 pb-3 border-b border-[#f0f0f0]">
              <span class="text-2xl">
                <WaterRateTwo theme="multi-color" size="41" :fill="['#292929', '#82a8ff', '#82a8ff', '#2F88FF']" :strokeWidth="2"/>
              </span>
              <span class="text-[16px] font-bold" :class="energyMode === 'water' ? 'text-[#3a64ab]' : 'text-[#333]'">水能管理</span>
              <span v-if="energyMode === 'water'" class="text-[11px] bg-[#eef3fa] text-[#3a64ab] px-1.5 py-0.5 rounded ml-auto">当前</span>
            </div>
            <div v-for="item in panelWaterCategories" :key="'water-' + item.label"
              class="py-2.5 px-3 rounded cursor-pointer flex justify-between items-center text-[13px] hover:bg-[#f5f5f5] transition-colors"
              @click="selectMode('water', item)">
              <span>{{ item.label }}</span>
              <span class="text-[#999] text-[11px]">{{ item.children ? item.children.length + '项' : '' }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── Shared toggle tab ────────────────────────────────────── */
.nav-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: filter 0.15s;
}

.nav-toggle:hover {
  filter: brightness(0.92);
}

.nav-toggle-text {
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  user-select: none;
  white-space: nowrap;
}

/* ── Expand button (collapsed) — blue, trapezoid ──────────── */
.nav-toggle--expand {
  width: 28px;
  height: 180px;
  left: 2px;
  background: #3a64ab;
  color: #fff;
  border-radius: 0 6px 6px 0;
  clip-path: path('M 0,4 C 0,2 2,0 4,0 L 22,12 C 25,14 28,16 28,22 L 28,158 C 28,164 25,166 22,168 L 4,180 C 2,180 0,178 0,176 Z');
}

.nav-toggle--expand:hover {
  filter: brightness(0.90);
}

/* ── Collapse tab (expanded) — white, trapezoid ──────────── */
.nav-toggle--collapse {
  width: 32px;
  height: 180px;
  right: -32px;
  background: #fff;
  color: #666;
  border-radius: 0 6px 6px 0;
  clip-path: path('M 0,4 C 0,2 2,0 4,0 L 26,12 C 29,14 32,16 32,22 L 32,158 C 32,164 29,166 26,168 L 4,180 C 2,180 0,178 0,176 Z');
}

.nav-toggle--collapse:hover {
  background: #f0f0f0;
  color: #333;
}

/* ── Drawer transitions ─────────────────────────────────────── */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.3s ease;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}
</style>
