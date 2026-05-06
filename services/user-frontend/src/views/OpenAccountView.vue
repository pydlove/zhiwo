<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Card, Form, Input, Select, Button, message, Tag } from 'ant-design-vue'
import request from '../api/request.js'

const route = useRoute()

const form = ref({
  nickName: '',
  email: '',
  trackIds: [],
  wxName: '',
})

const loading = ref(false)
const tracks = ref([])
const configLoading = ref(true)
const configError = ref('')
const config = ref({ platform: '', count: 3, membershipPlanId: '', adminId: '' })
const successInfo = ref(null)

const shortCode = computed(() => route.query.c || '')

const OA_SUCCESS_KEY = 'blogger_oa_success'

function saveSuccessToCache(code, data) {
  try {
    const raw = localStorage.getItem(OA_SUCCESS_KEY)
    const map = raw ? JSON.parse(raw) : {}
    map[code] = {
      username: data.username,
      password: data.password,
      userId: data.userId,
      time: Date.now(),
    }
    localStorage.setItem(OA_SUCCESS_KEY, JSON.stringify(map))
  } catch (e) {
    // ignore
  }
}

function getSuccessFromCache(code) {
  try {
    const raw = localStorage.getItem(OA_SUCCESS_KEY)
    const map = raw ? JSON.parse(raw) : {}
    return map[code] || null
  } catch (e) {
    return null
  }
}

const filteredTracks = computed(() => {
  if (!config.value.platform) return tracks.value
  return tracks.value.filter(t => t.platforms && t.platforms.includes(config.value.platform))
})

const trackOptions = computed(() => {
  return filteredTracks.value.map(t => ({
    value: t.id,
    label: t.platforms + '-' + t.name,
  }))
})

async function loadConfig() {
  configLoading.value = true
  configError.value = ''
  try {
    // 先检查本地缓存：该短链是否已开户成功
    if (shortCode.value) {
      const cached = getSuccessFromCache(shortCode.value)
      if (cached) {
        successInfo.value = {
          username: cached.username,
          password: cached.password,
          userId: cached.userId,
        }
        configLoading.value = false
        return
      }
    }

    if (shortCode.value) {
      const data = await request.get('/open-account/config?code=' + encodeURIComponent(shortCode.value))
      config.value.platform = data.platform || ''
      config.value.count = data.count || 3
      config.value.membershipPlanId = data.membershipPlanId || ''
      config.value.adminId = data.adminId || ''
    } else {
      // No short code, allow all platforms with default count
      config.value.platform = route.query.platform || ''
      const c = parseInt(route.query.count)
      config.value.count = isNaN(c) ? 3 : c
    }
  } catch (e) {
    configError.value = e.message || '链接验证失败'
  } finally {
    configLoading.value = false
  }
}

async function loadTracks() {
  try {
    const data = await request.get('/open-account/tracks?platform=' + encodeURIComponent(config.value.platform || ''))
    tracks.value = data || []
  } catch (e) {
    // ignore
  }
}

async function handleSubmit() {
  if (!form.value.nickName.trim()) {
    message.warning('请输入微信名称')
    return
  }
  if (!form.value.wxName.trim()) {
    message.warning('请输入公众号名称')
    return
  }
  if (!form.value.email.trim()) {
    message.warning('请输入邮箱')
    return
  }
  if (!form.value.trackIds || form.value.trackIds.length === 0) {
    message.warning('请至少选择一个订阅赛道')
    return
  }
  if (form.value.trackIds.length > config.value.count) {
    message.warning(`最多只能选择 ${config.value.count} 个赛道`)
    return
  }

  loading.value = true
  try {
    const payload = {
      nickName: form.value.nickName.trim(),
      wxName: form.value.wxName.trim(),
      email: form.value.email.trim(),
      trackIds: form.value.trackIds,
      membershipPlanId: config.value.membershipPlanId || undefined,
    }
    if (config.value.adminId) {
      payload.adminId = config.value.adminId
    }
    const data = await request.post('/auth/open-account', payload)
    successInfo.value = data
    if (shortCode.value) {
      saveSuccessToCache(shortCode.value, data)
    }
    message.success('开户申请提交成功')
  } catch (e) {
    message.error(e.message || '开户失败')
  } finally {
    loading.value = false
  }
}

function handleReset() {
  form.value = { nickName: '', email: '', trackIds: [], wxName: '' }
  successInfo.value = null
}

onMounted(async () => {
  await loadConfig()
  if (!configError.value) {
    await loadTracks()
  }
})
</script>

<template>
  <div class="open-account-page">
    <div class="header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-logo">A</div>
          <span class="brand-name">知我公众号创作助手</span>
        </div>
      </div>
    </div>

    <div class="content">
      <div v-if="configLoading" class="loading-wrap">
        <div class="loading-text">加载中...</div>
      </div>

      <div v-else-if="configError" class="error-wrap">
        <div class="error-icon">!</div>
        <div class="error-title">链接异常</div>
        <div class="error-desc">{{ configError }}</div>
      </div>

      <div v-else-if="successInfo" class="success-wrap">
        <div class="success-icon">✓</div>
        <div class="success-title">开户申请提交成功</div>
        <div class="success-desc">
          您的账号已创建，请保存以下信息用于登录：
        </div>
        <div class="info-card">
          <div class="info-row">
            <span class="info-label">登录账号（公众号名称）</span>
            <span class="info-value">{{ successInfo.username }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">初始密码</span>
            <span class="info-value">{{ successInfo.password }}</span>
          </div>
        </div>
        <div class="success-tip">
          请截图保存，初始密码仅在本次显示。账号审核通过后即可登录使用。
        </div>

        <div class="docs-card">
          <div class="docs-header">
            <div class="docs-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M12 16v-4"></path>
                <path d="M12 8h.01"></path>
              </svg>
            </div>
            <div class="docs-title">新用户必读</div>
          </div>
          <div class="doc-list">
            <a class="doc-row must" href="https://docs.qq.com/doc/DRkVJS2Rnd2ZyRUJs" target="_blank" rel="noopener noreferrer">
              <div class="doc-row-left">
                <span class="doc-badge">必设</span>
                <span class="doc-name">关闭群发需要扫码的问题</span>
              </div>
              <svg class="doc-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7 17L17 7"></path>
                <path d="M7 7h10v10"></path>
              </svg>
            </a>
            <div class="doc-row-group">
              <div class="doc-group-label">
                <span class="doc-badge blue">教程</span>
                <span class="doc-group-title">发文流程</span>
                <span class="doc-hint">（优先推荐电脑/平板）</span>
              </div>
              <a class="doc-sub-link" href="https://docs.qq.com/doc/DRmZvZEZrbUdBWXBQ" target="_blank" rel="noopener noreferrer">
                <span>电脑/平板端操作</span>
                <svg class="doc-arrow" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 17L17 7"></path>
                  <path d="M7 7h10v10"></path>
                </svg>
              </a>
              <a class="doc-sub-link" href="https://docs.qq.com/doc/DRkhEZkRvZFFTWXp2" target="_blank" rel="noopener noreferrer">
                <span>手机浏览器操作</span>
                <svg class="doc-arrow" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 17L17 7"></path>
                  <path d="M7 7h10v10"></path>
                </svg>
              </a>
            </div>
          </div>
        </div>

        <div class="tips-card">
          <div class="tips-header">
            <div class="tips-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                <path d="M2 17l10 5 10-5"></path>
                <path d="M2 12l10 5 10-5"></path>
              </svg>
            </div>
            <div class="tips-title">运营技巧</div>
          </div>
          <div class="tips-list">
            <a class="tips-row" href="https://docs.qq.com/doc/DRkhpc3Z1eFNJRE9N" target="_blank" rel="noopener noreferrer">
              <div class="tips-row-left">
                <span class="tips-badge">建议</span>
                <span class="tips-name">新号刚刚注册，建议可以延迟两天再发文</span>
              </div>
              <svg class="tips-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7 17L17 7"></path>
                <path d="M7 7h10v10"></path>
              </svg>
            </a>
            <a class="tips-row" href="https://docs.qq.com/doc/DRkp1WE5RUmpYc2dS" target="_blank" rel="noopener noreferrer">
              <div class="tips-row-left">
                <span class="tips-badge">避坑</span>
                <span class="tips-name">新手公众号避坑的操作</span>
              </div>
              <svg class="tips-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7 17L17 7"></path>
                <path d="M7 7h10v10"></path>
              </svg>
            </a>
          </div>
        </div>
      </div>

      <div v-else class="form-wrap">
        <div class="form-header">
          <h1>注册开户</h1>
          <p>填写以下信息完成开户申请</p>
        </div>

        <Form layout="vertical" :model="form">
          <Form.Item label="微信名称" required>
            <Input
              v-model:value="form.nickName"
              placeholder="请输入您的微信名称"
              size="large"
            />
          </Form.Item>

          <Form.Item label="邮箱" required>
            <Input
              v-model:value="form.email"
              placeholder="请输入您的邮箱"
              size="large"
              type="email"
            />
          </Form.Item>

          <Form.Item label="公众号名称" required>
            <Input
              v-model:value="form.wxName"
              placeholder="请输入您的公众号名称"
              size="large"
            />
            <div class="form-hint">该名称将作为您的登录账号</div>
          </Form.Item>

          <Form.Item label="订阅赛道" required>
            <div v-if="config.platform" class="platform-tag">
              <Tag color="blue">{{ config.platform }}</Tag>
              <span class="count-hint">最多可选 {{ config.count }} 个赛道</span>
            </div>
            <Select
              v-model:value="form.trackIds"
              mode="multiple"
              placeholder="请选择订阅赛道"
              size="large"
              :max-tag-count="3"
              :options="trackOptions"
            />
          </Form.Item>

          <div class="actions">
            <Button
              type="primary"
              size="large"
              block
              :loading="loading"
              @click="handleSubmit"
            >
              注册开户
            </Button>
            <Button
              size="large"
              block
              style="margin-top: 12px;"
              @click="$router.push('/guide/mp-register')"
            >
              如何注册公众号
            </Button>
          </div>
        </Form>
      </div>
    </div>

    <div class="footer">
      <div>知我公众号创作助手 &copy; 2026</div>
    </div>
  </div>
</template>

<style scoped>
.open-account-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.header-inner {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.brand-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.content {
  flex: 1;
  padding: 24px;
}

.form-wrap,
.loading-wrap,
.error-wrap,
.success-wrap {
  max-width: 480px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 32px 24px;
}

.form-header {
  text-align: center;
  margin-bottom: 24px;
}

.form-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.platform-tag {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-hint {
  font-size: 13px;
  color: #8c8c8c;
}

.form-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.actions {
  margin-top: 8px;
}

.loading-wrap,
.error-wrap,
.success-wrap {
  text-align: center;
  padding: 48px 24px;
}

.loading-text {
  color: #8c8c8c;
  font-size: 14px;
}

.error-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #ff4d4f;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 16px;
}

.error-title {
  font-size: 18px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: #8c8c8c;
}

.success-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 16px;
}

.success-title {
  font-size: 18px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  color: #595959;
  margin-bottom: 20px;
}

.info-card {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  text-align: left;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #d9f7be;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #595959;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.success-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 20px;
  line-height: 1.6;
}

.docs-card {
  background: linear-gradient(135deg, #fff7e6 0%, #fff1b8 100%);
  border: 1px solid #ffd591;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(255, 169, 64, 0.08);
}

.docs-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #ffd591;
}

.docs-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fa8c16;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.docs-title {
  font-size: 16px;
  font-weight: 700;
  color: #d46b08;
  letter-spacing: 0.5px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ffe7ba;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.doc-row:hover {
  border-color: #fa8c16;
  box-shadow: 0 2px 8px rgba(250, 140, 22, 0.12);
  transform: translateY(-1px);
}

.doc-row-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doc-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #ff4d4f;
  color: #fff;
  flex-shrink: 0;
}

.doc-badge.blue {
  background: #1890ff;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.doc-arrow {
  color: #bfbfbf;
  flex-shrink: 0;
  transition: color 0.2s ease;
}

.doc-row:hover .doc-arrow {
  color: #fa8c16;
}

.doc-row-group {
  background: #fff;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  padding: 12px 14px;
}

.doc-group-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.doc-group-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.doc-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.doc-sub-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 6px;
  background: #fff7e6;
  text-decoration: none;
  transition: all 0.2s ease;
}

.doc-sub-link:last-child {
  margin-bottom: 0;
}

.doc-sub-link:hover {
  background: #ffe7ba;
}

.doc-sub-link span {
  font-size: 13px;
  color: #595959;
}

.doc-sub-link:hover span {
  color: #d46b08;
}

.doc-sub-link .doc-arrow {
  color: #d9d9d9;
}

.doc-sub-link:hover .doc-arrow {
  color: #fa8c16;
}

.tips-card {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
  border-radius: 12px;
  padding: 20px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.08);
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #b7eb8f;
}

.tips-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tips-title {
  font-size: 16px;
  font-weight: 700;
  color: #389e0d;
  letter-spacing: 0.5px;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tips-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #d9f7be;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.tips-row:hover {
  border-color: #52c41a;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.12);
  transform: translateY(-1px);
}

.tips-row-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tips-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #52c41a;
  color: #fff;
  flex-shrink: 0;
}

.tips-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.tips-arrow {
  color: #bfbfbf;
  flex-shrink: 0;
  transition: color 0.2s ease;
}

.tips-row:hover .tips-arrow {
  color: #52c41a;
}

.footer {
  text-align: center;
  padding: 16px;
  font-size: 12px;
  color: #bfbfbf;
}

@media (max-width: 576px) {
  .content {
    padding: 12px;
  }

  .form-wrap,
  .loading-wrap,
  .error-wrap,
  .success-wrap {
    padding: 24px 16px;
  }
}
</style>
