import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { usePermissions } from '../composables/usePermissions.js'

export const usePlatformStore = defineStore('platform', () => {
  const platforms = ['公众号', '今日头条', '百家号']
  const current = ref('公众号')
  const { allowedPlatforms } = usePermissions()

  const setPlatform = (p) => {
    if (!platforms.includes(p)) return true
    const allowed = allowedPlatforms.value
    if (!allowed.includes(p)) {
      return false
    }
    current.value = p
    return true
  }

  const trackMatches = (trackPlatforms) => {
    if (!trackPlatforms) return true
    // Must match current platform
    if (!trackPlatforms.includes(current.value)) return false
    // Must match at least one allowed platform
    const allowed = allowedPlatforms.value
    return allowed.some(p => trackPlatforms.includes(p))
  }

  return { platforms, current, allowedPlatforms, setPlatform, trackMatches }
})
