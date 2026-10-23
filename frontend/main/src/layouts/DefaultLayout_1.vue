<script setup>
import SideNavBar from '@/components/shell/SideNavBar.vue'
import TopAppBar from '@/components/shell/TopAppBar.vue'
import TabsBar from '@/components/shell/TabsBar.vue'
import ToastContainer from '@/components/shell/ToastContainer.vue'
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-[#f0f2f5]">
    <SideNavBar />
    <div class="flex-1 flex flex-col overflow-hidden">
      <TopAppBar />
      <TabsBar />
      <div class="flex-1 overflow-y-auto  p-[15px]">
        <router-view />
      </div>
    </div>
    <ToastContainer />
  </div>
</template>
