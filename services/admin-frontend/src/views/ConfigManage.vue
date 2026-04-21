<script setup>
import { ref, onMounted } from 'vue'
import { Card, Input, Select, Button, Form, message } from 'ant-design-vue'
import request from '../api/request.js'

const apiKey = ref('sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx')
const model = ref('moonshot-v1-8k')
const systemName = ref('Aicloud')
const logoUrl = ref('')
const qrCodeUrl = ref('')
const loading = ref(false)

async function loadConfig() {
  try {
    const data = await request.get('/configs')
    if (data) {
      systemName.value = data.systemName || 'Aicloud'
      logoUrl.value = data.logoUrl || ''
      qrCodeUrl.value = data.qrCodeUrl || ''
      if (data.apiKey) apiKey.value = data.apiKey
      if (data.model) model.value = data.model
    }
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

onMounted(loadConfig)
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
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">用户端浏览器标签页标题及页面顶部展示</div>
          </Form.Item>
          <Form.Item label="平台 Logo">
            <div style="display: flex; gap: 12px; align-items: flex-start;">
              <label style="width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative;">
                <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleLogoUpload">
                <img v-if="logoUrl" :src="logoUrl" style="width: 100%; height: 100%; object-fit: cover;">
                <template v-else>
                  <div style="font-size: 24px; margin-bottom: 8px;">+</div>
                  <div>上传图片</div>
                </template>
              </label>
            </div>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">建议尺寸 120×120px，支持 PNG、JPG</div>
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
            <label style="width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative;">
              <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleQrUpload">
              <img v-if="qrCodeUrl" :src="qrCodeUrl" style="width: 100%; height: 100%; object-fit: cover;">
              <template v-else>
                <div style="font-size: 24px; margin-bottom: 8px;">+</div>
                <div>上传图片</div>
              </template>
            </label>
          </div>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">登录页「联系管理员开户」展示的图片</div>
        </Form.Item>
      </Form>
    </Card>

    <div style="display: flex; gap: 12px; margin-top: 8px;">
      <Button type="primary" :loading="loading" @click="handleSave">保存配置</Button>
      <Button @click="loadConfig">重置</Button>
    </div>
  </div>
</template>
