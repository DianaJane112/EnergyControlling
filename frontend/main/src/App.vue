<script setup>
import { useAuth } from '@/stores/auth.js'

const { token, fetchUser } = useAuth()

if (token.value) {
  fetchUser()
}
</script>

<template>
  <router-view />
</template>
