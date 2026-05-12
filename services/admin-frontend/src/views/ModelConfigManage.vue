<script setup>
import { ref, onMounted } from 'vue'
import { Card, Input, Select, Button, Form, message } from 'ant-design-vue'
import request from '../api/request.js'

const apiKey = ref('sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx')
const model = ref('moonshot-v1-8k')
const miniMaxApiKey = ref('')
const miniMaxModel = ref('MiniMax-M2.7')
const selectedLLMModel = ref('kimi')

async function loadConfig() {
  try {
    const data = await request.get('/configs')
    if (data) {
      if (data.apiKey) apiKey.value = data.apiKey
      if (data.model) model.value = data.model
      if (data.miniMaxApiKey) miniMaxApiKey.value = data.miniMaxApiKey
      if (data.miniMaxModel) miniMaxModel.value = data.miniMaxModel
      if (data.selectedLLMModel) selectedLLMModel.value = data.selectedLLMModel
    }
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

async function saveLLMModel() {
  await savePartial({ selectedLLMModel: selectedLLMModel.value })
}

async function saveKimiConfig() {
  await savePartial({
    apiKey: apiKey.value,
    model: model.value,
  })
}

async function saveMiniMaxConfig() {
  await savePartial({
    miniMaxApiKey: miniMaxApiKey.value,
    miniMaxModel: miniMaxModel.value,
  })
}

onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div>
    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">大模型选择</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="默认模型">
          <Select v-model:value="selectedLLMModel" style="max-width: 480px; height: 36px;">
            <Select.Option value="kimi">Kimi K2.6</Select.Option>
            <Select.Option value="minimax">MiniMax M2.7</Select.Option>
          </Select>
          <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">生成文章时使用的默认大模型</div>
        </Form.Item>
        <Button type="primary" @click="saveLLMModel">保存</Button>
      </Form>
    </Card>

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
          <Select show-search v-model:value="model" style="max-width: 480px; height: 36px;">
            <Select.Option value="moonshot-v1-8k">moonshot-v1-8k</Select.Option>
            <Select.Option value="moonshot-v1-32k">moonshot-v1-32k</Select.Option>
            <Select.Option value="moonshot-v1-128k">moonshot-v1-128k</Select.Option>
            <Select.Option value="kimi-k2-6">kimi-k2-6</Select.Option>
          </Select>
        </Form.Item>
        <Button type="primary" @click="saveKimiConfig">保存</Button>
      </Form>
    </Card>

    <Card style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">MiniMax API 配置</span>
      </template>
      <Form layout="vertical">
        <Form.Item label="API Key" required>
          <Input.Password v-model:value="miniMaxApiKey" placeholder="请输入 MiniMax API Key" style="max-width: 480px;" />
        </Form.Item>
        <Form.Item label="模型选择">
          <Select show-search v-model:value="miniMaxModel" style="max-width: 480px; height: 36px;">
            <Select.Option value="MiniMax-M2.7">MiniMax-M2.7</Select.Option>
          </Select>
        </Form.Item>
        <Button type="primary" @click="saveMiniMaxConfig">保存</Button>
      </Form>
    </Card>
  </div>
</template>
