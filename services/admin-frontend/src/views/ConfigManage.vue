<script setup>
import { ref, onMounted } from 'vue'
import { Card, Input, InputNumber, Select, Button, Form, message } from 'ant-design-vue'
import request from '../api/request.js'
import { listTracks } from '../api/track.js'

const systemName = ref('知我公众号创作助手')
const logoUrl = ref('')
const qrCodeUrl = ref('')

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

const defaultArticleStyle = ref('')
const generationConcurrency = ref('1')

const imagePostSplitMode = ref('height')
const imagePostWidth = ref(1080)
const imagePostHeight = ref(1920)
const imagePostBgColor = ref('#ffffff')
const imagePostCoverGradient = ref('#f8f3e0')
const imagePostTheme = ref('classic-xhs')
const imagePostFont = ref('')
const imagePostBodyFont = ref('')

const imagePostThemeOptions = [
  { label: '经典小红书', value: 'classic-xhs' },
  { label: '小红书清新', value: 'xhs-fresh' },
  { label: '莫兰迪奶油', value: 'morandi-cream' },
  { label: '薄荷清新', value: 'mint-fresh' },
  { label: '落日橘粉', value: 'sunset-blush' },
  { label: '深夜高级', value: 'midnight' },
  { label: '薰衣草紫', value: 'lavender' },
  { label: '克莱因蓝', value: 'klein-blue' },
  { label: '渐变ins风', value: 'gradient-ins' },
  { label: '报纸复古', value: 'newspaper' },
]

const imagePostFontOptions = [
  { label: '自动选择（默认）', value: '' },
  { label: '思源黑体 Bold', value: 'NotoSansSC-Bold' },
  { label: '阿里妈妈方圆体', value: 'AlimamaFangYuanTiVF' },
  { label: '阿里妈妈刀隶体', value: '阿里妈妈刀隶体' },
  { label: '思源黑体 Regular', value: 'NotoSansSC-Regular' },
]

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
      notifyEmailEnabled.value = data.notifyEmailEnabled === '1'
      if (data.notifyEmailAddress) notifyEmailAddress.value = data.notifyEmailAddress
      if (data.mainOperator) mainOperator.value = data.mainOperator
      if (data.defaultArticleStyle !== undefined) {
        defaultArticleStyle.value = data.defaultArticleStyle
      }
      if (data.generation_task_concurrency !== undefined) {
        generationConcurrency.value = data.generation_task_concurrency
      }
      if (data.image_post_split_mode !== undefined) {
        imagePostSplitMode.value = data.image_post_split_mode
      }
      if (data.image_post_width !== undefined) {
        imagePostWidth.value = parseInt(data.image_post_width, 10) || 1080
      }
      if (data.image_post_height !== undefined) {
        imagePostHeight.value = parseInt(data.image_post_height, 10) || 1920
      }
      if (data.image_post_bg_color !== undefined) {
        imagePostBgColor.value = data.image_post_bg_color
      }
      if (data.image_post_cover_gradient !== undefined) {
        imagePostCoverGradient.value = data.image_post_cover_gradient
      }
      if (data.image_post_theme !== undefined) {
        imagePostTheme.value = data.image_post_theme
      }
      if (data.image_post_font !== undefined) {
        imagePostFont.value = data.image_post_font
      }
      if (data.image_post_body_font !== undefined) {
        imagePostBodyFont.value = data.image_post_body_font
      }
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

async function savePartial(fields) {
  try {
    await request.post('/configs', fields)
    message.success('保存成功')
  } catch (e) {
    message.error('保存失败')
    throw e
  }
}

async function saveBrandConfig() {
  await savePartial({
    systemName: systemName.value,
    logoUrl: logoUrl.value,
    qrCodeUrl: qrCodeUrl.value,
  })
}

async function saveStyleConfig() {
  await savePartial({ defaultArticleStyle: defaultArticleStyle.value })
}

async function saveMainOperator() {
  await savePartial({ mainOperator: mainOperator.value || '' })
}

async function saveGenerationConcurrency() {
  await savePartial({ generation_task_concurrency: generationConcurrency.value || '1' })
}

async function saveImagePostConfig() {
  await savePartial({
    image_post_split_mode: imagePostSplitMode.value || 'height',
    image_post_width: String(imagePostWidth.value || 1080),
    image_post_height: String(imagePostHeight.value || 1920),
    image_post_bg_color: imagePostBgColor.value || '#ffffff',
    image_post_cover_gradient: imagePostCoverGradient.value || '#f8f3e0',
    image_post_theme: imagePostTheme.value || 'classic-xhs',
    image_post_font: imagePostFont.value || '',
    image_post_body_font: imagePostBodyFont.value || '',
  })
}

async function saveNotifyConfig() {
  await savePartial({
    notifyEmailEnabled: notifyEmailEnabled.value ? '1' : '0',
    notifyEmailAddress: notifyEmailAddress.value,
  })
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
        <div style="margin-top: 16px;">
          <Button type="primary" @click="saveBrandConfig">保存</Button>
        </div>
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
        <span style="font-size: 16px; font-weight: 500; color: #262626;">默认文章样式提示词</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="样式提示词" style="max-width: 800px;">
          <Input.TextArea
            v-model:value="defaultArticleStyle"
            :rows="6"
            placeholder="请输入默认的文章排版样式描述，例如：标题使用 #E74C3C 颜色，字体使用 PingFang SC，引用框背景为 #F8F9FA..."
          />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">此处填写全局默认的样式描述。用户未单独配置时，复制提示词会自动将此处内容填充到 ${stylePrompt} 变量中。</div>
        </Form.Item>
        <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px; padding: 12px; font-size: 12px; color: #52c41a; max-width: 800px;">
          <div style="font-weight: 600; margin-bottom: 8px;">使用说明</div>
          <div>在「标题库」的提示词模板中使用 <code style="font-family: monospace;">${stylePrompt}</code> 变量即可引用此处内容。</div>
          <div style="margin-top: 4px;">管理员也可以在「用户管理」中为用户单独配置样式提示词，单独配置的优先级高于默认样式。</div>
        </div>
        <Button type="primary" style="margin-top: 16px;" @click="saveStyleConfig">保存</Button>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">主运营人员</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="默认归属运营者">
          <Select show-search v-model:value="mainOperator" placeholder="请选择主运营人员" style="max-width: 480px;" allow-clear>
            <Select.Option v-for="op in operatorOptions" :key="op.username" :value="op.username" :label="op.name || op.username">{{ op.name || op.username }}</Select.Option>
          </Select>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">用户直接访问（不带 op 参数）时，默认展示该运营者的二维码和话术</div>
        </Form.Item>
        <Button type="primary" @click="saveMainOperator">保存</Button>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">文章生成并发配置</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="同时运行任务数" style="max-width: 480px;">
          <Input
            v-model:value="generationConcurrency"
            type="number"
            min="1"
            max="10"
            placeholder="例如：1"
          />
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">设置同时执行的文章生成任务数量，范围 1-10，默认 1。增大并发会加快队列处理速度，但会占用更多系统资源。</div>
        </Form.Item>
        <Button type="primary" @click="saveGenerationConcurrency">保存</Button>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">贴图设置</span>
      </template>
      <Form layout="vertical">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; max-width: 800px;">
          <Form.Item label="切分模式">
            <Select v-model:value="imagePostSplitMode" style="width: 100%;">
              <Select.Option value="height">按高度切</Select.Option>
              <Select.Option value="paragraph">按段落切</Select.Option>
            </Select>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">按高度切：内容填满即截断；按段落切：不在段落中间断开</div>
          </Form.Item>
          <Form.Item label="图片尺寸">
            <div style="display: flex; gap: 12px; align-items: center;">
              <InputNumber v-model:value="imagePostWidth" :min="540" :max="1440" style="width: 120px;" />
              <span style="color: #999;">×</span>
              <InputNumber v-model:value="imagePostHeight" :min="960" :max="2560" style="width: 120px;" />
            </div>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">默认 1080×1920（9:16）</div>
          </Form.Item>
          <Form.Item label="内容背景色">
            <Input v-model:value="imagePostBgColor" style="width: 200px;" />
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">如 #ffffff、#f5f5f5</div>
          </Form.Item>
          <Form.Item label="封面背景色">
            <Input v-model:value="imagePostCoverGradient" style="width: 280px;" />
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">如 #f8f3e0（米白）、#f8df8b（暖黄）</div>
          </Form.Item>
          <Form.Item label="封面主题风格" style="grid-column: 1 / -1;">
            <Select v-model:value="imagePostTheme" style="width: 320px;">
              <Select.Option v-for="opt in imagePostThemeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
            </Select>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">选择贴图封面的视觉风格，不同主题配色和装饰元素不同</div>
          </Form.Item>
          <Form.Item label="封面字体" style="grid-column: 1 / -1;">
            <Select v-model:value="imagePostFont" style="width: 320px;">
              <Select.Option v-for="opt in imagePostFontOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
            </Select>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">选择封面标题字体，留空则由系统按主题自动匹配</div>
          </Form.Item>
          <Form.Item label="正文字体" style="grid-column: 1 / -1;">
            <Select v-model:value="imagePostBodyFont" style="width: 320px;">
              <Select.Option v-for="opt in imagePostFontOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
            </Select>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">选择正文段落字体，留空则由系统按主题自动匹配</div>
          </Form.Item>
        </div>
        <Button type="primary" style="margin-top: 16px;" @click="saveImagePostConfig">保存</Button>
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
        <Button type="primary" style="margin-top: 16px;" @click="saveNotifyConfig">保存</Button>
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
            <Select show-search v-model:value="parsePlatform" style="width: 100%;">
              <Select.Option value="公众号">公众号</Select.Option>
              <Select.Option value="今日头条">今日头条</Select.Option>
              <Select.Option value="百家号">百家号</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="赛道" required>
            <Select show-search v-model:value="parseTrackId" placeholder="请选择赛道" style="width: 100%;" allow-clear>
              <Select.Option v-for="t in trackOptions" :key="t.id" :value="t.id" :label="t.name">{{ t.name }}</Select.Option>
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

  </div>
</template>
