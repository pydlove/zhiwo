<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../api/request.js'

const route = useRoute()
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const code = route.params.code
  if (!code) {
    error.value = '短链码无效'
    loading.value = false
    return
  }
  try {
    const data = await request.get('/open-account/short-links/' + encodeURIComponent(code))
    if (data && data.targetUrl) {
      window.location.replace(data.targetUrl)
    } else {
      error.value = '短链解析失败'
      loading.value = false
    }
  } catch (e) {
    error.value = e.message || '短链已失效或不存在'
    loading.value = false
  }
})
</script>

<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
    <div v-if="loading" style="text-align: center; color: #8c8c8c;">
      <div style="width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #07C160; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px;"></div>
      <div style="font-size: 14px;">正在跳转...</div>
    </div>
    <div v-else style="text-align: center; padding: 32px;">
      <div style="width: 56px; height: 56px; background: #fff2f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; color: #ff4d4f; font-size: 24px;">!</div>
      <div style="font-size: 16px; color: #262626; font-weight: 500; margin-bottom: 8px;">跳转失败</div>
      <div style="font-size: 14px; color: #8c8c8c;">{{ error }}</div>
    </div>
  </div>
</template>

<style scoped>
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
