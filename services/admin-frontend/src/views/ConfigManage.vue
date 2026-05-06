<script setup>
import { ref, onMounted } from 'vue'
import { Card, Input, Select, Button, Form, message } from 'ant-design-vue'
import request from '../api/request.js'
import { listTracks } from '../api/track.js'

const apiKey = ref('sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx')
const model = ref('moonshot-v1-8k')
const systemName = ref('知我公众号创作助手')
const logoUrl = ref('')
const qrCodeUrl = ref('')
const loading = ref(false)

const parseText = ref('')
const parsePlatform = ref('公众号')
const parseTrackId = ref(undefined)
const trackOptions = ref([])
const parseLoading = ref(false)
const avatarUrls = ref('')

const oaBaseUrl = ref(window.location.hostname === 'localhost' ? 'http://localhost:5174' : 'http://www.mmshuo.tech')
const mainOperator = ref('')
const operatorOptions = ref([])

const PARSE_HISTORY_KEY = 'blogger_parse_export_history'

function loadParseHistory() {
  try {
    const raw = localStorage.getItem(PARSE_HISTORY_KEY)
    if (raw) {
      const data = JSON.parse(raw)
      if (data.parseText !== undefined) parseText.value = data.parseText
      if (data.parsePlatform !== undefined) parsePlatform.value = data.parsePlatform
      if (data.parseTrackId !== undefined) parseTrackId.value = data.parseTrackId
      if (data.avatarUrls !== undefined) avatarUrls.value = data.avatarUrls
    }
  } catch (e) {
    // ignore
  }
}

function saveParseHistory() {
  try {
    const data = {
      parseText: parseText.value,
      parsePlatform: parsePlatform.value,
      parseTrackId: parseTrackId.value,
      avatarUrls: avatarUrls.value,
    }
    localStorage.setItem(PARSE_HISTORY_KEY, JSON.stringify(data))
  } catch (e) {
    // ignore
  }
}

function clearParseHistory() {
  parseText.value = ''
  parsePlatform.value = '公众号'
  parseTrackId.value = undefined
  avatarUrls.value = ''
  try {
    localStorage.removeItem(PARSE_HISTORY_KEY)
  } catch (e) {
    // ignore
  }
}

const notifyEmailEnabled = ref(false)
const notifyEmailAddress = ref('')

const backupLoading = ref(false)

async function handleBackupDb() {
  backupLoading.value = true
  try {
    const res = await fetch('/api/configs/backup', {
      method: 'POST',
    })
    if (!res.ok) throw new Error('备份失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'db_backup_' + new Date().toISOString().slice(0, 10) + '.sql'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    message.success('备份成功')
  } catch (e) {
    message.error('备份失败')
  } finally {
    backupLoading.value = false
  }
}

async function loadConfig() {
  try {
    const data = await request.get('/configs')
    if (data) {
      systemName.value = data.systemName || '知我公众号创作助手'
      logoUrl.value = data.logoUrl || ''
      qrCodeUrl.value = data.qrCodeUrl || ''
      if (data.apiKey) apiKey.value = data.apiKey
      if (data.model) model.value = data.model
      notifyEmailEnabled.value = data.notifyEmailEnabled === '1'
      if (data.notifyEmailAddress) notifyEmailAddress.value = data.notifyEmailAddress
      if (data.mainOperator) mainOperator.value = data.mainOperator
    }
  } catch (e) {
    // ignore
  }
}

async function loadOperators() {
  try {
    const data = await request.get('/admins')
    operatorOptions.value = (data || []).filter(a => a.role === '运营管理员')
  } catch (e) {
    // ignore
  }
}

async function handleSave() {
  loading.value = true
  try {
    await request.post('/configs', {
      apiKey: apiKey.value,
      model: model.value,
      systemName: systemName.value,
      logoUrl: logoUrl.value,
      qrCodeUrl: qrCodeUrl.value,
      notifyEmailEnabled: notifyEmailEnabled.value ? '1' : '0',
      notifyEmailAddress: notifyEmailAddress.value,
      mainOperator: mainOperator.value || '',
    })
    message.success('配置已保存')
  } catch (e) {
    message.error('保存失败')
  } finally {
    loading.value = false
  }
}

function handleLogoUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    logoUrl.value = reader.result
  }
  reader.readAsDataURL(file)
}

function handleQrUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    qrCodeUrl.value = reader.result
  }
  reader.readAsDataURL(file)
}

async function loadTracks() {
  try {
    const data = await listTracks()
    trackOptions.value = data || []
  } catch (e) {
    // ignore
  }
}

function copyCustomerDialogueLink() {
  const url = oaBaseUrl.value + '/customer-dialogue'
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(url).then(() => {
      message.success('链接已复制到剪贴板')
    }).catch(() => {
      fallbackCopyUrl(url)
    })
  } else {
    fallbackCopyUrl(url)
  }
}

function fallbackCopyUrl(url) {
  const textarea = document.createElement('textarea')
  textarea.value = url
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  try {
    document.execCommand('copy')
    message.success('链接已复制到剪贴板')
  } catch (e) {
    message.error('复制失败，请手动复制')
  }
  document.body.removeChild(textarea)
}

async function handleParseExport() {
  if (!parseText.value.trim()) {
    message.warning('请输入博主文本')
    return
  }
  if (!parseTrackId.value) {
    message.warning('请选择赛道')
    return
  }
  parseLoading.value = true
  try {
    const form = new URLSearchParams()
    form.append('text', parseText.value)
    form.append('platform', parsePlatform.value)
    form.append('trackId', parseTrackId.value)
    form.append('avatarUrls', avatarUrls.value)

    const res = await fetch('/api/bloggers/parse-export', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'blogger_export_' + Date.now() + '.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    message.success('导出成功')
    saveParseHistory()
  } catch (e) {
    message.error('导出失败')
  } finally {
    parseLoading.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadTracks()
  loadOperators()
  loadParseHistory()
})
</script>

<template>
  <div>
    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">Kimi API 配置</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="API Key" required>
          <Input.Password v-model:value="apiKey" placeholder="请输入 Kimi API Key" style="max-width: 480px;" />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">用于 AI 生成标题、大纲和全文，仅管理员可见</div>
        </Form.Item>
        <Form.Item label="模型选择">
          <Select v-model:value="model" style="max-width: 480px; height: 36px;">
            <Select.Option value="moonshot-v1-8k">moonshot-v1-8k</Select.Option>
            <Select.Option value="moonshot-v1-32k">moonshot-v1-32k</Select.Option>
            <Select.Option value="moonshot-v1-128k">moonshot-v1-128k</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">用户端品牌配置</span>
      </template>
      <Form layout="vertical">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; max-width: 480px;">
          <Form.Item label="系统名称">
            <Input v-model:value="systemName" placeholder="请输入系统名称" />
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px; width: 180px;">用户端浏览器标签页标题及页面顶部展示</div>
          </Form.Item>
          <Form.Item label="平台 Logo">
            <div style="display: flex; gap: 12px; align-items: flex-start;">
              <label style="width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative; flex-shrink: 0;">
                <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleLogoUpload">
                <img v-if="logoUrl" :src="logoUrl" style="width: 100%; height: 100%; object-fit: cover;">
                <template v-else>
                  <div style="font-size: 24px; margin-bottom: 8px;">+</div>
                  <div>上传图片</div>
                </template>
              </label>
              <div style="flex: 1; min-width: 0;">
                <Input v-model:value="logoUrl" placeholder="或填写图片 URL" style="width: 280px;" />
                <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">支持上传本地图片或粘贴图片链接</div>
              </div>
            </div>
          </Form.Item>
        </div>
        <div style="margin-top: 20px; padding: 16px; background: #f5f5f5; border-radius: 2px; display: flex; align-items: center; gap: 12px; max-width: 480px;">
          <img v-if="logoUrl" :src="logoUrl" style="width: 32px; height: 32px; border-radius: 4px; object-fit: cover;">
          <div v-else style="width: 32px; height: 32px; border-radius: 4px; background: #1890ff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600;">A</div>
          <span style="font-size: 16px; font-weight: 500; color: #262626;">{{ systemName }}</span>
        </div>
        <div style="font-size: 12px; color: #8c8c8c; margin-top: 8px;">上方为品牌效果预览</div>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">登录页配置</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="客服二维码">
          <div style="display: flex; gap: 12px; align-items: flex-start;">
            <label style="width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative; flex-shrink: 0;">
              <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleQrUpload">
              <img v-if="qrCodeUrl" :src="qrCodeUrl" style="width: 100%; height: 100%; object-fit: cover;">
              <template v-else>
                <div style="font-size: 24px; margin-bottom: 8px;">+</div>
                <div>上传图片</div>
              </template>
            </label>
            <div style="flex: 1; min-width: 0;">
              <Input v-model:value="qrCodeUrl" placeholder="或填写图片 URL" style="width: 280px;" />
              <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">支持上传本地图片或粘贴图片链接</div>
            </div>
          </div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">登录页「联系管理员开户」展示的图片</div>
        </Form.Item>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">主运营人员</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="默认归属运营者">
          <Select v-model:value="mainOperator" placeholder="请选择主运营人员" style="max-width: 480px;" allow-clear>
            <Select.Option v-for="op in operatorOptions" :key="op.username" :value="op.username">{{ op.name || op.username }}</Select.Option>
          </Select>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">用户直接访问（不带 op 参数）时，默认展示该运营者的二维码和话术</div>
        </Form.Item>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">开户邮件通知</span>
      </template>
      <Form layout="vertical">
        <Form.Item>
          <div style="display: flex; align-items: center; gap: 8px;">
            <input
              :checked="notifyEmailEnabled"
              type="checkbox"
              style="width: 16px; height: 16px; cursor: pointer;"
              @change="notifyEmailEnabled = $event.target.checked"
            />
            <span style="font-size: 14px; color: #262626;">启用邮件通知</span>
            <span style="font-size: 12px; color: #8c8c8c;">用户成功开户后，发送邮件通知管理员</span>
          </div>
        </Form.Item>
        <Form.Item label="通知邮箱">
          <Input v-model:value="notifyEmailAddress" placeholder="请输入接收通知的管理员邮箱" style="max-width: 480px;" />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">开户成功后将发送邮件到该邮箱，邮件内容由系统自动生成</div>
        </Form.Item>
        <div style="padding: 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px; font-size: 13px; color: #595959; max-width: 480px;">
          <strong>邮件内容示例：</strong><br/>
          主题：【开户通知】有新的用户提交了开户申请<br/>
          内容包含：微信名称、公众号名称、邮箱、申请时间
        </div>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">客服话术助手链接</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="访问链接">
          <div style="display: flex; gap: 12px; align-items: center; max-width: 600px;">
            <Input :value="oaBaseUrl + '/customer-dialogue'" readonly />
            <Button @click="copyCustomerDialogueLink">复制</Button>
          </div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">该链接为客服话术助手独立页面，可发送给客服人员使用</div>
        </Form.Item>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">数据库备份</span>
      </template>
      <Form layout="vertical">
        <Form.Item>
          <div style="display: flex; align-items: center; gap: 12px;">
            <Button type="primary" :loading="backupLoading" @click="handleBackupDb">手动备份数据库</Button>
            <span style="font-size: 12px; color: #8c8c8c;">导出所有表数据为 SQL 文件，可用于数据库恢复</span>
          </div>
        </Form.Item>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">博主文本解析导出</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="博主文本" required>
          <Input.TextArea v-model:value="parseText" placeholder="粘贴博主信息文本，每行一个，支持以下格式：&#10;李月亮：畅销书作家、新女性主义倡导者...&#10;周冲的影像声色 以电影、文学为切入点..." :rows="8" style="max-width: 600px;" />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">每行一条记录，名称与描述之间可用中文冒号、英文冒号、Tab 或空格分隔</div>
        </Form.Item>
        <Form.Item label="头像 URL 列表（可选）">
          <Input.TextArea v-model:value="avatarUrls" placeholder="粘贴头像图片 URL，每行一个，按顺序对应上方博主&#10;https://example.com/avatar1.jpg&#10;https://example.com/avatar2.jpg" :rows="4" style="max-width: 600px;" />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">每行一个头像 URL，按顺序与博主文本对应，导出到 avatarFileName 列</div>
        </Form.Item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; max-width: 600px;">
          <Form.Item label="平台" required>
            <Select v-model:value="parsePlatform" style="width: 100%;">
              <Select.Option value="公众号">公众号</Select.Option>
              <Select.Option value="今日头条">今日头条</Select.Option>
              <Select.Option value="百家号">百家号</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="赛道" required>
            <Select v-model:value="parseTrackId" placeholder="请选择赛道" style="width: 100%;" allow-clear>
              <Select.Option v-for="t in trackOptions" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
            </Select>
          </Form.Item>
        </div>
        <Form.Item>
          <div style="display: flex; gap: 12px;">
            <Button type="primary" :loading="parseLoading" @click="handleParseExport">导出 Excel</Button>
            <Button @click="clearParseHistory">清除记录</Button>
          </div>
        </Form.Item>
      </Form>
    </Card>

    <div style="display: flex; gap: 12px; margin-top: 8px;">
      <Button type="primary" :loading="loading" @click="handleSave">保存配置</Button>
      <Button @click="loadConfig">重置</Button>
    </div>
  </div>
</template>
