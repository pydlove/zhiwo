<script setup>
import { ref, onMounted } from 'vue'
import { Card, Table, Button, Tag, message, Spin } from 'ant-design-vue'
import { getAgentExecutions, triggerAgentRun } from '../api/agent.js'

const loading = ref(false)
const triggerLoading = ref(false)
const executions = ref([])

const columns = [
  { title: '执行日期', dataIndex: 'executionDate', key: 'executionDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '用户/赛道', key: 'stats', width: 120 },
  { title: '匹配', dataIndex: 'matchedTitles', key: 'matchedTitles', width: 80 },
  { title: '生成', dataIndex: 'generatedTitles', key: 'generatedTitles', width: 80 },
  { title: '文章任务', dataIndex: 'articleTasks', key: 'articleTasks', width: 90 },
  { title: '失败', dataIndex: 'failedCount', key: 'failedCount', width: 80 },
  { title: '耗时', key: 'duration', width: 120 },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', width: 160 },
]

function getStatusTag(status) {
  switch (status) {
    case 'completed': return { color: 'success', text: '成功' }
    case 'running': return { color: 'processing', text: '执行中' }
    case 'partial': return { color: 'warning', text: '部分成功' }
    case 'failed': return { color: 'error', text: '失败' }
    default: return { color: 'default', text: status }
  }
}

async function loadExecutions() {
  loading.value = true
  try {
    const res = await getAgentExecutions(50)
    if (res.code === 200 && res.data) {
      executions.value = res.data
    }
  } catch (e) {
    message.error('加载执行记录失败')
  } finally {
    loading.value = false
  }
}

async function handleTrigger() {
  triggerLoading.value = true
  try {
    const res = await triggerAgentRun()
    if (res.code === 200) {
      message.success(res.data?.message || '触发成功')
      setTimeout(loadExecutions, 3000)
    } else {
      message.error(res.msg || '触发失败')
    }
  } catch (e) {
    message.error('触发失败: ' + (e?.message || '未知错误'))
  } finally {
    triggerLoading.value = false
  }
}

onMounted(loadExecutions)
</script>

<template>
  <div style="padding: 24px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
      <h2 style="font-size: 20px; font-weight: 600; margin: 0;">Agent 执行记录</h2>
      <Button type="primary" :loading="triggerLoading" @click="handleTrigger">立即执行一次</Button>
    </div>

    <Spin :spinning="loading">
      <Card>
        <Table :columns="columns" :dataSource="executions" rowKey="id" size="small" :pagination="{ pageSize: 20 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="getStatusTag(record.status).color">{{ getStatusTag(record.status).text }}</Tag>
            </template>
            <template v-if="column.key === 'stats'">
              {{ record.totalUsers }} / {{ record.totalTracks }}
            </template>
            <template v-if="column.key === 'duration'">
              <span v-if="record.startedAt && record.completedAt">
                {{ Math.round((new Date(record.completedAt) - new Date(record.startedAt)) / 1000) }}s
              </span>
              <span v-else-if="record.startedAt">执行中...</span>
              <span v-else>-</span>
            </template>
          </template>
        </Table>
      </Card>
    </Spin>
  </div>
</template>
