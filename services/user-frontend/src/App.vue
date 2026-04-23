<script setup>
import { onMounted } from 'vue'
import { usePermissions } from './composables/usePermissions.js'

const { loadPermissions } = usePermissions()

onMounted(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.id) {
    loadPermissions(user.id)
  }
})
</script>

<template>
  <router-view />
</template>
