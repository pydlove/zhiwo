import { ref, computed } from 'vue'
import { getUserPlan } from '../api/user.js'

const plan = ref(null)
const permissions = ref({})
const planData = ref({})
const loading = ref(false)

function parsePermissions(planData) {
  if (!planData || !planData.permissionsJson) {
    return {}
  }
  try {
    return JSON.parse(planData.permissionsJson)
  } catch (e) {
    return {}
  }
}

export function usePermissions() {
  const trackLimit = computed(() => plan.value?.trackLimit ?? 0)
  const aiLimit = computed(() => plan.value?.aiLimit ?? 0)
  const expireDate = computed(() => plan.value?.expireDate ?? null)

  const allowedPlatforms = computed(() => {
    const userPlatforms = (planData.value.platformLimit || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
    if (userPlatforms.length > 0) return userPlatforms
    return ['公众号', '今日头条', '百家号']
  })

  const allowedTemplates = computed(() => {
    const ts = permissions.value.templates
    return Array.isArray(ts) ? ts : []
  })

  const canEmailPush = computed(() => !!permissions.value.emailPush)
  const canOnlinePreview = computed(() => !!permissions.value.onlinePreview)
  const canGuideAccess = computed(() => !!permissions.value.guideAccess)

  async function loadPermissions(userId) {
    if (!userId) {
      plan.value = null
      planData.value = {}
      permissions.value = {}
      return
    }
    loading.value = true
    try {
      const data = await getUserPlan(userId)
      planData.value = data || {}
      plan.value = data.plan || null
      permissions.value = parsePermissions(data.plan)
    } catch (e) {
      plan.value = null
      planData.value = {}
      permissions.value = {}
    } finally {
      loading.value = false
    }
  }

  function clearPermissions() {
    plan.value = null
    planData.value = {}
    permissions.value = {}
  }

  return {
    plan,
    planData,
    permissions,
    loading,
    trackLimit,
    aiLimit,
    expireDate,
    allowedPlatforms,
    allowedTemplates,
    canEmailPush,
    canOnlinePreview,
    canGuideAccess,
    loadPermissions,
    clearPermissions,
  }
}
