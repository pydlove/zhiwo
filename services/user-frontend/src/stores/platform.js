import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePlatformStore = defineStore('platform', () => {
  const platforms = ['公众号', '今日头条', '百家号']
  const current = ref('公众号')

  const allowedPlatforms = () => {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const limit = user.platformLimit
    if (!limit || limit.trim() === '') return platforms
    return limit.split(/[,，]/).map(p => p.trim()).filter(Boolean)
  }

  const setPlatform = (p) => {
    if (!platforms.includes(p)) return true
    const allowed = allowedPlatforms()
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
    const allowed = allowedPlatforms()
    return allowed.some(p => trackPlatforms.includes(p))
  }

  return { platforms, current, allowedPlatforms, setPlatform, trackMatches }
})
