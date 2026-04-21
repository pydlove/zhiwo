<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { listHelps, getHelp, listHelpCategories } from '../api/help.js'

const { isMobile } = useViewport()
const helps = ref([])
const categories = ref([])
const activeId = ref('')
const searchKeyword = ref('')
const loading = ref(false)
const activeAnchor = ref('')
const sidebarOpen = ref(false)

const sidebarData = computed(() => {
  const keyword = searchKeyword.value.trim()
  const filtered = keyword
    ? helps.value.filter(h => (h.title || '').includes(keyword) || (h.category || '').includes(keyword))
    : helps.value

  const map = {}
  filtered.forEach(h => {
    const cat = h.category || '其他'
    if (!map[cat]) map[cat] = []
    map[cat].push(h)
  })

  // Sort categories by backend sortOrder, then items within each category by sortOrder
  const catOrderMap = {}
  categories.value.forEach((c, idx) => {
    catOrderMap[c.name] = idx
  })

  return Object.keys(map)
    .sort((a, b) => {
      const orderA = catOrderMap[a] !== undefined ? catOrderMap[a] : 999
      const orderB = catOrderMap[b] !== undefined ? catOrderMap[b] : 999
      return orderA - orderB
    })
    .map(cat => ({
      title: cat,
      items: map[cat].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)),
    }))
})

const currentHelp = computed(() => {
  if (!activeId.value) return null
  return helps.value.find(h => h.id === activeId.value) || null
})

function generateAnchor(text) {
  return 'heading-' + text.trim().replace(/\s+/g, '-').replace(/[^\w\-]/g, '')
}

const processedContent = computed(() => {
  if (!currentHelp.value || !currentHelp.value.content) return ''
  let html = currentHelp.value.content
  let index = 0
  html = html.replace(/<(h[23])([^>]*)>(.*?)<\/\1>/gi, (match, tag, attrs, text) => {
    const anchor = generateAnchor(text) + '-' + index++
    return `<${tag} id="${anchor}" ${attrs}>${text}</${tag}>`
  })
  return html
})

const tocList = computed(() => {
  if (!currentHelp.value || !currentHelp.value.content) return []
  const list = []
  let index = 0
  const regex = /<(h[23])([^>]*)>(.*?)<\/\1>/gi
  let match
  while ((match = regex.exec(currentHelp.value.content)) !== null) {
    const level = match[1] === 'h2' ? 2 : 3
    const text = match[3].replace(/<[^>]+>/g, '')
    const anchor = generateAnchor(text) + '-' + index++
    list.push({ level, text, anchor })
  }
  return list
})

function scrollToAnchor(anchor) {
  activeAnchor.value = anchor
  nextTick(() => {
    const el = document.getElementById(anchor)
    if (el) {
      const container = document.querySelector('.help-content-scroll')
      if (container) {
        const top = el.offsetTop - container.offsetTop - 16
        container.scrollTo({ top, behavior: 'smooth' })
      } else {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
  })
}

async function loadData() {
  loading.value = true
  try {
    const [list, cats] = await Promise.all([listHelps(), listHelpCategories()])
    categories.value = cats || []
    helps.value = list || []
    if (helps.value.length > 0 && !activeId.value) {
      activeId.value = helps.value[0].id
    }
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function selectItem(id) {
  activeId.value = id
  activeAnchor.value = ''
  if (isMobile.value) sidebarOpen.value = false
}

onMounted(loadData)
</script>

<template>
  <div :style="{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', height: isMobile ? 'auto' : 'calc(100vh - 104px)', background: '#fff', borderRadius: '8px', overflow: 'hidden', border: '1px solid #e5e7eb' }">
    <!-- Mobile Sidebar Toggle -->
    <div v-if="isMobile" style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">
      <button @click="sidebarOpen = !sidebarOpen" style="width: 100%; padding: 10px 14px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; font-weight: 500; color: #374151; cursor: pointer; display: flex; align-items: center; justify-content: space-between;">
        <span>📚 帮助分类</span>
        <span style="transition: transform 0.2s;" :style="sidebarOpen ? { transform: 'rotate(180deg)' } : {}">▼</span>
      </button>
    </div>

    <!-- Sidebar -->
    <div v-show="!isMobile || sidebarOpen" :style="{ width: isMobile ? '100%' : '260px', borderRight: isMobile ? 'none' : '1px solid #e5e7eb', borderBottom: isMobile ? '1px solid #e5e7eb' : 'none', padding: '20px 0', overflowY: 'auto', background: '#fff', flexShrink: 0 }">
      <div style="padding: 0 16px 16px;">
        <div style="display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 9999px; border: 1px solid #e5e7eb; background: #fff;">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="searchKeyword"
            placeholder="搜索帮助内容"
            style="flex: 1; border: none; outline: none; font-size: 14px; color: #374151; background: transparent;"
          />
        </div>
      </div>

      <div v-if="loading" style="padding: 20px; text-align: center; color: #9ca3af; font-size: 13px;">加载中...</div>

      <div v-for="sec in sidebarData" :key="sec.title" style="margin-bottom: 4px;">
        <div style="padding: 8px 16px; font-size: 13px; font-weight: 600; color: #374151;">{{ sec.title }}</div>
        <div
          v-for="item in sec.items"
          :key="item.id"
          @click="selectItem(item.id)"
          style="padding: 8px 16px 8px 28px; font-size: 13px; color: #6b7280; cursor: pointer; border-left: 2px solid transparent;"
          :style="activeId === item.id ? { color: '#2563eb', background: '#eff6ff', borderLeftColor: '#2563eb', fontWeight: 500 } : {}"
        >
          {{ item.title }}
        </div>
      </div>

      <div v-if="!loading && sidebarData.length === 0" style="padding: 20px; text-align: center; color: #9ca3af; font-size: 13px;">暂无帮助文档</div>
    </div>

    <!-- Content -->
    <div class="help-content-scroll" :style="{ flex: 1, overflowY: 'auto', padding: isMobile ? '20px 16px' : '32px 40px', background: '#fff' }">
      <div v-if="currentHelp" style="max-width: 720px; margin: 0 auto;">
        <h1 style="font-size: 24px; font-weight: 700; margin-bottom: 20px;">{{ currentHelp.title }}</h1>
        <div class="help-detail-content" style="font-size: 15px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="processedContent"></div>
      </div>
      <div v-else style="padding: 60px 20px; text-align: center; color: #9ca3af; font-size: 14px;">
        请选择左侧文档查看详情
      </div>
    </div>

    <!-- TOC -->
    <div v-if="!isMobile" style="width: 220px; border-left: 1px solid #e5e7eb; padding: 24px 16px; overflow-y: auto; background: #fafafa; flex-shrink: 0;">
      <div v-if="tocList.length > 0">
        <div style="font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 12px;">目录</div>
        <div
          v-for="item in tocList"
          :key="item.anchor"
          @click="scrollToAnchor(item.anchor)"
          style="font-size: 13px; line-height: 1.6; color: #6b7280; cursor: pointer; margin-bottom: 8px; transition: color 0.15s;"
          :style="{ paddingLeft: (item.level - 2) * 12 + 'px', color: activeAnchor === item.anchor ? '#2563eb' : '#6b7280', fontWeight: activeAnchor === item.anchor ? 500 : 400 }"
        >
          {{ item.text }}
        </div>
      </div>
      <div v-else style="font-size: 13px; color: #9ca3af; text-align: center; padding-top: 20px;">暂无目录</div>
    </div>
  </div>
</template>

<style>
.help-detail-content img {
  max-width: 100%;
  height: auto;
  display: block;
}
.help-detail-content h2,
.help-detail-content h3 {
  margin-top: 28px;
  margin-bottom: 14px;
}
.help-detail-content p {
  margin-bottom: 12px;
}
.help-detail-content ul,
.help-detail-content ol {
  margin-bottom: 12px;
  padding-left: 20px;
}
</style>
