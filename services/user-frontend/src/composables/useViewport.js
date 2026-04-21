import { ref, onMounted, onUnmounted } from 'vue'

const MOBILE_BREAKPOINT = 768
const isMobile = ref(false)

function update() {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

export function useViewport() {
  onMounted(() => {
    update()
    window.addEventListener('resize', update)
  })
  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })
  return { isMobile }
}
