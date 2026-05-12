<script setup>
import { ref, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { Table, Tag, Input, Select, Button, Steps, message, Popconfirm } from 'ant-design-vue'
import { listTasks, cancelTask, stopTask, retryTask } from '../api/taskList.js'

const router = useRouter()

const Step = Steps.Step

const tasks = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const actionLoadingId = ref(null)

const statusOptions = [
  { label: '全部', value: '' },
  { label: '排队中', value: 'pending' },
  { label: '进行中', value: 'processing' },
  { label: '已停止', value: 'stopped' },
  { label: '已完成', value: 'completed' },
  { label: '失败', value: 'failed' },
]

const stepItems = [
  { title: '提示词' },
  { title: '大模型' },
  { title: '写文件' },
  { title: '样式化' },
  { title: '去AI味' },
  { title: '完成' },
]

function fetchTasks() {
  loading.value = true
  listTasks({
    keyword: keyword.value || undefined,
    status: statusFilter.value || undefined,
  })
    .then(res => {
      tasks.value = res?.list || []
    })
    .catch(() => {
      message.error('加载任务列表失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function onSearch() {
  fetchTasks()
}

function onStatusChange() {
  fetchTasks()
}

function getStatusTag(status, step) {
  switch (status) {
    case 'pending':
      return { color: 'blue', text: '排队中' }
    case 'processing':
      return { color: 'orange', text: step ? (step === 1 ? '构建提示词' : step === 2 ? '大模型生成' : step === 3 ? '写入文件' : step === 4 ? '样式优化' : step === 5 ? '去除AI味' : '处理中') : '进行中' }
    case 'stopped':
      return { color: 'default', text: '已停止' }
    case 'completed':
      return { color: 'green', text: '已完成' }
    case 'failed':
      return { color: 'red', text: '失败' }
    default:
      return { color: 'default', text: status }
  }
}

function getStepCurrent(status, step) {
  if (status === 'pending') return 0
  if (status === 'processing') {
    if (step === 1) return 0
    if (step === 2) return 1
    if (step === 3) return 2
    if (step === 4) return 3
    if (step === 5) return 4
    return 0
  }
  if (status === 'completed') return 6
  if (status === 'stopped') return step || 0
  if (status === 'failed') return step || 0
  return 0
}

async function handleCancel(record) {
  actionLoadingId.value = record.id
  try {
    await cancelTask(record.id)
    message.success('任务已取消')
    fetchTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || '取消失败')
  } finally {
    actionLoadingId.value = null
  }
}

async function handleStop(record) {
  actionLoadingId.value = record.id
  try {
    await stopTask(record.id)
    message.success('任务已停止')
    fetchTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || '停止失败')
  } finally {
    actionLoadingId.value = null
  }
}

async function handleRetry(record) {
  actionLoadingId.value = record.id
  try {
    const res = await retryTask(record.id)
    message.success(res.message || '重跑任务已创建')
    fetchTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || '重跑失败')
  } finally {
    actionLoadingId.value = null
  }
}

const columns = [
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    ellipsis: true,
    width: 220,
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    customRender: ({ record }) => {
      const tag = getStatusTag(record.status, record.progressStep)
      return h(Tag, { color: tag.color }, () => tag.text)
    },
  },
  {
    title: '进度',
    key: 'progress',
    width: 500,
    customRender: ({ record }) => {
      const current = getStepCurrent(record.status, record.progressStep)
      return h('div', {}, [
        h(Steps, { size: 'small', current: current }, () =>
          stepItems.map((item, idx) => h(Step, { key: idx, title: item.title }))
        ),
        record.progressMessage
          ? h('div', { style: 'margin-top: 4px; font-size: 12px; color: #888;' }, record.progressMessage)
          : null,
      ])
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    align: 'center',
    customRender: ({ record }) => {
      const isLoading = actionLoadingId.value === record.id
      const btns = []

      if (record.status === 'pending') {
        btns.push(
          h(Popconfirm, {
            title: '确定取消该任务吗？',
            onConfirm: () => handleCancel(record),
          }, () => h(Button, { type: 'link', size: 'small', danger: true, loading: isLoading }, () => '取消'))
        )
      } else if (record.status === 'processing') {
        btns.push(
          h(Popconfirm, {
            title: '确定停止该任务吗？',
            onConfirm: () => handleStop(record),
          }, () => h(Button, { type: 'link', size: 'small', danger: true, loading: isLoading }, () => '停止'))
        )
      } else {
        btns.push(
          h(Button, { type: 'link', size: 'small', loading: isLoading, onClick: () => handleRetry(record) }, () => '重跑')
        )
      }

      if (record.resultFileUrl) {
        btns.push(
          h('a', { href: record.resultFileUrl, target: '_blank', rel: 'noopener noreferrer', style: 'margin-left: 4px;' }, () => '下载')
        )
      }

      return h('div', { style: 'display: flex; align-items: center; justify-content: center;' }, btns)
    },
  },
]

onMounted(() => {
  fetchTasks()
  // 每 5 秒自动刷新
  const interval = setInterval(fetchTasks, 5000)
  // 组件卸载时清理
  onUnmounted(() => clearInterval(interval))
})
</script>

<template>
  <div>
    <div style="display: flex; gap: 12px; margin-bottom: 16px;">
      <Input
        v-model:value="keyword"
        placeholder="搜索标题"
        style="width: 240px"
        @pressEnter="onSearch"
      />
      <Select
        v-model:value="statusFilter"
        :options="statusOptions"
        style="width: 140px"
        placeholder="状态筛选"
        @change="onStatusChange"
      />
      <Button type="primary" @click="onSearch">搜索</Button>
      <Button style="margin-left: auto;" @click="router.push('/title-library')">标题库</Button>
    </div>

    <Table
      :columns="columns"
      :dataSource="tasks"
      :loading="loading"
      rowKey="id"
      :pagination="{ pageSize: 20 }"
      size="small"
      :scroll="{ x: 1200 }"
    />
  </div>
</template>
