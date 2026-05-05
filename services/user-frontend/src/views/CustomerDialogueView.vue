<script setup>
import { ref, onMounted, computed } from 'vue'
import { Tag, message } from 'ant-design-vue'
import { listCustomerDialogues, listCategories } from '../api/customerDialogue.js'

const dialogues = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const loading = ref(false)
const searchKeyword = ref('')

const filteredDialogues = computed(() => {
  let list = dialogues.value
  if (activeCategory.value !== '全部') {
    list = list.filter(item => item.category === activeCategory.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(item =>
      (item.question && item.question.toLowerCase().includes(kw)) ||
      (item.reply && item.reply.toLowerCase().includes(kw))
    )
  }
  return list
})

async function loadData() {
  loading.value = true
  try {
    const [data, cats] = await Promise.all([
      listCustomerDialogues(),
      listCategories(),
    ])
    dialogues.value = data || []
    categories.value = ['全部', ...(cats || [])]
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleCopy(text, label = '回复') {
  if (!text) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        message.success(`${label}已复制`)
      }).catch(() => fallbackCopy(text, label))
    } else {
      fallbackCopy(text, label)
    }
  } catch (e) {
    fallbackCopy(text, label)
  }
}

function fallbackCopy(text, label) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  try {
    document.execCommand('copy')
    message.success(`${label}已复制`)
  } catch (e) {
    message.error('复制失败')
  }
  document.body.removeChild(textarea)
}

async function handleCopyImage(imageUrl) {
  if (!imageUrl) return
  try {
    // 尝试复制图片本身到剪贴板
    const response = await fetch(imageUrl, { mode: 'no-cors' })
    const blob = await response.blob()
    const item = new ClipboardItem({ [blob.type || 'image/png']: blob })
    await navigator.clipboard.write([item])
    message.success('图片已复制')
  } catch (e) {
    // 降级：复制图片链接
    handleCopy(imageUrl, '图片链接')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="cd-page">
    <div class="cd-header">
      <div class="cd-header-inner">
        <div class="cd-brand">
          <div class="cd-brand-logo">客</div>
          <span class="cd-brand-name">客服话术助手</span>
        </div>
      </div>
    </div>

    <div class="cd-content">
      <!-- 搜索 -->
      <div class="cd-search-wrap">
        <input
          v-model="searchKeyword"
          class="cd-search-input"
          placeholder="搜索问题或回复内容..."
          type="text"
        />
      </div>

      <!-- 分类标签 -->
      <div class="cd-categories">
        <button
          v-for="cat in categories"
          :key="cat"
          class="cd-category-btn"
          :class="{ active: activeCategory === cat }"
          @click="activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="cd-loading">
        <div class="cd-loading-text">加载中...</div>
      </div>

      <div v-else-if="filteredDialogues.length === 0" class="cd-empty">
        暂无数据
      </div>

      <div v-else class="cd-list">
        <div
          v-for="item in filteredDialogues"
          :key="item.id"
          class="cd-card"
        >
          <div class="cd-card-header">
            <Tag v-if="item.category" color="blue" size="small">{{ item.category }}</Tag>
            <button class="cd-copy-btn" @click="handleCopy(item.reply, '回复')">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
              </svg>
              复制回复
            </button>
          </div>

          <div class="cd-question">
            <span class="cd-label">场景</span>
            <span class="cd-text">{{ item.question }}</span>
          </div>

          <div class="cd-reply">
            <span class="cd-label">回复</span>
            <pre class="cd-reply-text">{{ item.reply }}</pre>
          </div>

          <div v-if="item.imageUrl" class="cd-image-section">
            <img
              :src="item.imageUrl"
              class="cd-image"
              @click="() => {}"
            />
            <button class="cd-copy-img-btn" @click="handleCopyImage(item.imageUrl)">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
              </svg>
              复制图片
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="cd-footer">
      客服话术助手 &copy; 2026
    </div>
  </div>
</template>

<style scoped>
.cd-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.cd-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.cd-header-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 14px 20px;
}

.cd-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cd-brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #52c41a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.cd-brand-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.cd-content {
  flex: 1;
  padding: 16px 20px;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.cd-search-wrap {
  margin-bottom: 12px;
}

.cd-search-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  background: #fff;
}

.cd-search-input:focus {
  border-color: #52c41a;
}

.cd-categories {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  margin-bottom: 16px;
  -webkit-overflow-scrolling: touch;
}

.cd-categories::-webkit-scrollbar {
  display: none;
}

.cd-category-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.cd-category-btn.active {
  background: #52c41a;
  border-color: #52c41a;
  color: #fff;
}

.cd-category-btn:not(.active):hover {
  border-color: #52c41a;
  color: #52c41a;
}

.cd-loading,
.cd-empty {
  text-align: center;
  padding: 60px 20px;
  color: #8c8c8c;
  font-size: 14px;
}

.cd-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cd-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.cd-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.cd-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.cd-copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #389e0d;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.cd-copy-btn:hover {
  background: #d9f7be;
}

.cd-question {
  margin-bottom: 10px;
}

.cd-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #8c8c8c;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cd-text {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  line-height: 1.5;
}

.cd-reply {
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 8px;
  padding: 12px;
}

.cd-reply-text {
  margin: 4px 0 0;
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.cd-image-section {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cd-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 6px;
  object-fit: cover;
  cursor: pointer;
}

.cd-copy-img-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #ffd591;
  background: #fff7e6;
  color: #d46b08;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
}

.cd-copy-img-btn:hover {
  background: #ffe7ba;
}

.cd-footer {
  text-align: center;
  padding: 16px;
  font-size: 12px;
  color: #bfbfbf;
}

@media (max-width: 576px) {
  .cd-content {
    padding: 12px;
  }

  .cd-card {
    padding: 12px;
  }
}
</style>
