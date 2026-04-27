import { ref } from 'vue'
import { getEmailConfig, updateEmailConfig } from '../api/user.js'

const emailConfig = ref({ email: '', emailReceive: 0, canSetEmail: 0 })
const loading = ref(false)

export function useEmailConfig() {
  async function loadEmailConfig(userId) {
    if (!userId) return
    loading.value = true
    try {
      const config = await getEmailConfig(userId)
      emailConfig.value = {
        email: config.email || '',
        emailReceive: config.emailReceive || 0,
        canSetEmail: config.canSetEmail || 0,
      }
    } catch (e) {
      console.error('加载邮箱配置失败:', e)
    } finally {
      loading.value = false
    }
  }

  async function saveEmailConfig(userId, data) {
    if (!userId) throw new Error('用户未登录')
    loading.value = true
    try {
      await updateEmailConfig(userId, data)
      emailConfig.value = { ...emailConfig.value, ...data }
    } catch (e) {
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    emailConfig,
    loading,
    loadEmailConfig,
    saveEmailConfig,
  }
}
