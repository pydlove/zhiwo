<script setup>
import { ref, onMounted } from 'vue'
import { Card, Form, FormItem, InputNumber, Input, Switch, Button, message, Spin, Divider } from 'ant-design-vue'
import { getAgentConfig, saveAgentConfig } from '../api/agent.js'

const loading = ref(false)
const saving = ref(false)
const config = ref({
  enabled: 0,
  cronExpr: '0 0 6 * * ?',
  similarityThreshold: 0.15,
  homogeneityThreshold: 0.15,
  minTitlesPerTrack: 5,
  historyDays: 30,
  candidateLimit: 50,
  maxGenerationConcurrency: 3,
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getAgentConfig()
    if (res.code === 200 && res.data) {
      config.value = { ...config.value, ...res.data }
    }
  } catch (e) {
    message.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload = {
      ...config.value,
      enabled: config.value.enabled ? 1 : 0,
      similarityThreshold: Number(config.value.similarityThreshold),
      homogeneityThreshold: Number(config.value.homogeneityThreshold),
    }
    await saveAgentConfig(payload)
    message.success('保存成功')
  } catch (e) {
    message.error('保存失败: ' + (e?.response?.data?.msg || e?.message))
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <div style="max-width: 800px; margin: 0 auto; padding: 24px;">
    <h2 style="font-size: 20px; font-weight: 600; margin-bottom: 24px;">AI Agent 自动流水线配置</h2>

    <Spin :spinning="loading">
      <Card>
        <Form layout="vertical">
          <FormItem label="启用 Agent">
            <Switch v-model:checked="config.enabled" :checkedValue="1" :unCheckedValue="0" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              开启后，系统将按照定时表达式自动执行标题匹配、补全和文章生成流程
            </div>
          </FormItem>

          <FormItem label="定时表达式 (Cron)">
            <Input v-model:value="config.cronExpr" placeholder="0 0 6 * * ?" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              默认每天早 6 点执行。格式：秒 分 时 日 月 周
            </div>
          </FormItem>

          <Divider />

          <FormItem label="标题相似度阈值">
            <InputNumber v-model:value="config.similarityThreshold" :min="0" :max="1" :step="0.01" style="width: 200px;" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              新标题与用户历史标题的相似度上限（默认 0.15 = 15%）
            </div>
          </FormItem>

          <FormItem label="同质化阈值">
            <InputNumber v-model:value="config.homogeneityThreshold" :min="0" :max="1" :step="0.01" style="width: 200px;" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              选出的标题之间平均相似度上限（默认 0.15 = 15%）
            </div>
          </FormItem>

          <FormItem label="每赛道最少推荐数">
            <InputNumber v-model:value="config.minTitlesPerTrack" :min="1" :max="20" style="width: 200px;" />
          </FormItem>

          <Divider />

          <FormItem label="历史标题天数">
            <InputNumber v-model:value="config.historyDays" :min="7" :max="90" style="width: 200px;" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              取用户近 N 天的已推荐标题用于去重对比
            </div>
          </FormItem>

          <FormItem label="候选标题上限">
            <InputNumber v-model:value="config.candidateLimit" :min="10" :max="200" style="width: 200px;" />
            <div style="font-size: 12px; color: #999; margin-top: 4px;">
              从标题库中最多取 N 条候选标题进入 AI 筛选
            </div>
          </FormItem>

          <FormItem>
            <Button type="primary" :loading="saving" @click="handleSave">保存配置</Button>
          </FormItem>
        </Form>
      </Card>
    </Spin>
  </div>
</template>
