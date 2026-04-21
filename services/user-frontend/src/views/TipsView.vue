<script setup>
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { listGuides, getGuide } from '../api/guide.js'

const { isMobile } = useViewport()
const activeTab = ref('全部')
const tabs = ['全部', '创作技巧', '养号技巧']
const searchKeyword = ref('')
const searchFocus = ref(false)

const articles = ref([])
const previewOpen = ref(false)
const currentArticle = ref(null)

const categoryColorMap = {
  '标题技巧': 'blue',
  '开头写法': 'orange',
  '结构模板': 'purple',
  '平台规则': 'green',
  '创作技巧': 'blue',
  '养号技巧': 'orange',
}

async function loadData() {
  try {
    const list = await listGuides()
    articles.value = list
      .map(g => ({
        ...g,
        tag: g.category || '创作技巧',
        tagClass: (g.category === '养号技巧' || g.category === '开头写法') ? 'orange' : 'blue',
        summary: g.description || (g.content ? g.content.replace(/<[^\u003e]+>/g, '').slice(0, 80) + '...' : '点击查看详情'),
        reads: '—',
        date: g.createdAt ? g.createdAt.slice(0, 10) : '-',
      }))
      .sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

const filtered = computed(() => {
  let list = articles.value
  if (activeTab.value !== '全部') {
    list = list.filter(a => a.tag === activeTab.value)
  }
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    list = list.filter(a =>
      (a.title || '').includes(keyword) ||
      (a.summary || '').includes(keyword) ||
      (a.description || '').includes(keyword)
    )
  }
  return list
})

async function openDetail(a) {
  if (a.link) {
    window.open(a.link, '_blank')
    return
  }
  try {
    const detail = await getGuide(a.id)
    currentArticle.value = detail
    previewOpen.value = true
  } catch (e) {
    message.error('加载详情失败')
  }
}

function closePreview() {
  previewOpen.value = false
  currentArticle.value = null
}

onMounted(loadData)
</script>

<template>
  <div :style="{ maxWidth: '960px', margin: '0 auto', padding: isMobile ? '20px 12px 32px' : '32px 24px 48px' }">
    <div style="text-align: center; margin-bottom: 28px;">
      <h1 :style="{ fontSize: isMobile ? '20px' : '26px', fontWeight: 700, marginBottom: '8px' }">创作技巧与运营干货</h1>
      <p style="font-size: 14px; color: #6b7280;">平台精选的创作方法论和账号运营技巧，助你快速成长</p>
    </div>

    <div style="display: flex; justify-content: center; gap: 8px; margin-bottom: 16px; overflow-x: auto; white-space: nowrap; padding-bottom: 4px;">
      <button
        v-for="t in tabs"
        :key="t"
        @click="activeTab = t"
        style="padding: 8px 20px; font-size: 14px; font-weight: 500; border-radius: 8px; cursor: pointer; border: 1px solid #e5e7eb; background: #fff;"
        :style="activeTab === t ? { color: '#fff', background: '#2563eb', borderColor: '#2563eb' } : { color: '#6b7280' }"
      >{{ t }}</button>
    </div>

    <div style="max-width: 560px; margin: 0 auto 28px;">
      <div style="display: flex; align-items: center; gap: 12px; padding: 12px 18px; border-radius: 9999px; border: 1px solid #e5e7eb; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: box-shadow 0.2s;" :style="{ boxShadow: searchFocus ? '0 4px 16px rgba(0,0,0,0.08)' : '0 2px 8px rgba(0,0,0,0.04)' }" @mouseenter="$event.currentTarget.style.borderColor = '#bfdbfe'" @mouseleave="$event.currentTarget.style.borderColor = '#e5e7eb'">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input
          v-model="searchKeyword"
          placeholder="搜索技巧标题或内容"
          style="flex: 1; border: none; outline: none; font-size: 15px; color: #374151; background: transparent;"
          @focus="searchFocus = true"
          @blur="searchFocus = false"
        />
      </div>
    </div>

    <div style="display: flex; flex-direction: column; gap: 16px;">
      <div
        v-for="a in filtered"
        :key="a.id"
        style="background: #fff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 20px 24px; display: flex; justify-content: space-between; align-items: flex-start; transition: box-shadow 0.15s;"
      >
        <div style="flex: 1;">
          <span
            style="display: inline-block; padding: 3px 10px; font-size: 12px; border-radius: 4px; font-weight: 500; margin-bottom: 10px;"
            :style="a.tagClass === 'blue'
              ? { background: '#eff6ff', color: '#1e40af' }
              : { background: '#fff7ed', color: '#9a3412' }"
          >{{ a.tag }}</span>
          <div style="font-size: 16px; font-weight: 600; margin-bottom: 8px; line-height: 1.4;">{{ a.title }}</div>
          <div style="font-size: 13px; color: #6b7280; line-height: 1.6; margin-bottom: 10px;">{{ a.summary }}</div>
          <div style="font-size: 12px; color: #9ca3af; display: flex; gap: 16px;">
            <span>{{ a.date }}</span>
          </div>
        </div>
        <button @click="openDetail(a)" style="padding: 8px 16px; background: #fff; color: #2563eb; border: 1px solid #2563eb; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; white-space: nowrap; margin-left: 16px;">
          {{ a.link ? '打开链接 ↗' : '阅读全文 ↗' }}
        </button>
      </div>
    </div>

    <Modal v-model:open="previewOpen" title="文章详情" :footer="null" :width="isMobile ? '90vw' : 800" @cancel="closePreview">
      <div v-if="currentArticle" style="padding: 8px 0;">
        <div style="font-size: 20px; font-weight: 600; margin-bottom: 12px;">{{ currentArticle.title }}</div>
        <Tag :color="categoryColorMap[currentArticle.category] || 'default'" style="margin-bottom: 16px;">{{ currentArticle.category }}</Tag>
        <div class="article-preview-content" style="font-size: 15px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="currentArticle.content"></div>
      </div>
    </Modal>
  </div>
</template>

<style>
.article-preview-content img {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>
