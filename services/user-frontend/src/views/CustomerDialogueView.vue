<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Tag, message, Modal, Input, Button, Form } from 'ant-design-vue'
import { listCustomerDialogues, listCategories, saveCustomerDialogue } from '../api/customerDialogue.js'

const dialogues = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const loading = ref(false)
const searchKeyword = ref('')
const modalOpen = ref(false)
const form = ref({
  category: '',
  question: '',
  reply: '',
  imageUrl: '',
})
const saving = ref(false)

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

const STORAGE_KEY = 'blogger_cd_state'

function saveState() {
  const state = {
    category: activeCategory.value,
    keyword: searchKeyword.value,
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function restoreState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const state = JSON.parse(raw)
    if (state.category) {
      activeCategory.value = state.category
    }
    if (state.keyword !== undefined) {
      searchKeyword.value = state.keyword
    }
  } catch (e) {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const [data, cats] = await Promise.all([
      listCustomerDialogues(),
      listCategories(),
    ])
    dialogues.value = data || []
    categories.value = ['全部', ...(cats || [])]
    // 恢复上次状态，如果分类不存在则重置为"全部"
    restoreState()
    if (activeCategory.value !== '全部' && !categories.value.includes(activeCategory.value)) {
      activeCategory.value = '全部'
    }
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

watch(activeCategory, saveState)
watch(searchKeyword, saveState)

function openModal() {
  form.value = {
    category: activeCategory.value !== '全部' ? activeCategory.value : (categories.value[1] || ''),
    question: '',
    reply: '',
    imageUrl: '',
  }
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

async function handleSave() {
  if (!form.value.category || !form.value.category.trim()) {
    message.warning('请输入分类')
    return
  }
  if (!form.value.question || !form.value.question.trim()) {
    message.warning('请输入提问/场景')
    return
  }
  if (!form.value.reply || !form.value.reply.trim()) {
    message.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await saveCustomerDialogue({
      category: form.value.category.trim(),
      question: form.value.question.trim(),
      reply: form.value.reply.trim(),
      imageUrl: form.value.imageUrl || null,
      sortOrder: 0,
    })
    message.success('添加成功')
    closeModal()
    loadData()
  } catch (e) {
    message.error(e.message || '添加失败')
  } finally {
    saving.value = false
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

    <!-- 悬浮添加按钮 -->
    <button class="cd-fab" @click="openModal" title="新增话术">
      <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
    </button>

    <!-- 新增话术弹窗 -->
    <Modal
      v-model:open="modalOpen"
      title="新增话术"
      @ok="handleSave"
      :confirm-loading="saving"
      width="480"
    >
      <Form layout="vertical" :model="form" style="margin-top: 12px;">
        <Form.Item label="分类" required>
          <Input v-model:value="form.category" placeholder="请输入分类名称" />
        </Form.Item>
        <Form.Item label="提问/场景" required>
          <Input v-model:value="form.question" placeholder="请输入提问或场景描述" />
        </Form.Item>
        <Form.Item label="回复内容" required>
          <Input.TextArea v-model:value="form.reply" :rows="4" placeholder="请输入回复内容" />
        </Form.Item>
        <Form.Item label="图片链接（可选）">
          <Input v-model:value="form.imageUrl" placeholder="粘贴图片 URL，如 https://example.com/img.jpg" />
        </Form.Item>
      </Form>
    </Modal>
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
  padding: 10px 16px;
}

.cd-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cd-brand-logo {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #52c41a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.cd-brand-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.cd-content {
  flex: 1;
  padding: 10px 12px;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.cd-search-wrap {
  margin-bottom: 8px;
}

.cd-search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
  background: #fff;
}

.cd-search-input:focus {
  border-color: #52c41a;
}

.cd-categories {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
  margin-bottom: 10px;
  -webkit-overflow-scrolling: touch;
}

.cd-categories::-webkit-scrollbar {
  display: none;
}

.cd-category-btn {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 12px;
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
  padding: 40px 16px;
  color: #8c8c8c;
  font-size: 13px;
}

.cd-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cd-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.cd-card:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.cd-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.cd-copy-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #389e0d;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}

.cd-copy-btn:hover {
  background: #d9f7be;
}

.cd-question {
  margin-bottom: 6px;
}

.cd-label {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  color: #8c8c8c;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cd-text {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  line-height: 1.4;
}

.cd-reply {
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 6px;
  padding: 8px;
}

.cd-reply-text {
  margin: 2px 0 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.cd-image-section {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cd-image {
  max-width: 100%;
  max-height: 140px;
  border-radius: 4px;
  object-fit: cover;
  cursor: pointer;
}

.cd-copy-img-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #ffd591;
  background: #fff7e6;
  color: #d46b08;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
}

.cd-copy-img-btn:hover {
  background: #ffe7ba;
}

.cd-footer {
  text-align: center;
  padding: 10px;
  font-size: 11px;
  color: #bfbfbf;
}

.cd-fab {
  position: fixed;
  right: 20px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  z-index: 100;
}

.cd-fab:hover {
  background: #389e0d;
  transform: scale(1.08);
  box-shadow: 0 6px 16px rgba(82, 196, 26, 0.45);
}

.cd-fab:active {
  transform: scale(0.96);
}

@media (max-width: 576px) {
  .cd-content {
    padding: 8px;
  }

  .cd-card {
    padding: 10px;
  }

  .cd-search-input {
    padding: 7px 10px;
    font-size: 12px;
  }

  .cd-category-btn {
    padding: 3px 8px;
    font-size: 11px;
  }

  .cd-reply-text {
    font-size: 12px;
  }

  .cd-text {
    font-size: 12px;
  }
}
</style>
