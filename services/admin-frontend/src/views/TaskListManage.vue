<script setup>
import { ref, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { Table, Tag, Input, Select, Button, Steps, message, Popconfirm, Dropdown, Menu } from 'ant-design-vue'
import { listTasks, cancelTask, stopTask, retryTask, rerunBackend } from '../api/taskList.js'

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
  { title: '去AI味' },
  { title: '写文件' },
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
      message.error('加载生成文章任务失败')
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
      return { color: 'orange', text: step ? (step === 1 ? '构建提示词' : step === 2 ? '大模型生成' : step === 3 ? '去除AI味' : step === 4 ? '写入文件' : step === 5 ? '已完成' : '处理中') : '进行中' }
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
  if (status === 'completed') return 5
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

async function handleRerunBackend(record) {
  actionLoadingId.value = record.id
  try {
    await rerunBackend(record.id)
    message.success('后半段已重新执行')
    fetchTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || '重跑后半段失败')
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
    width: 320,
    customRender: ({ record }) => {
      const status = record.status
      const step = record.progressStep || 0
      const msg = record.progressMessage || ''

      const config = {
        pending:    { color: '#1890ff', bg: '#e6f7ff', tag: 'blue',    text: '排队中' },
        processing: { color: '#fa8c16', bg: '#fff7e6', tag: 'orange',  text: stepItems[Math.min(step - 1, 5)]?.title || '处理中' },
        stopped:    { color: '#bfbfbf', bg: '#f5f5f5', tag: 'default', text: '已停止' },
        completed:  { color: '#52c41a', bg: '#f6ffed', tag: 'success', text: '已完成' },
        failed:     { color: '#ff4d4f', bg: '#fff2f0', tag: 'error',   text: '失败' },
      }[status] || config.pending

      const percent = status === 'completed' ? 100 : Math.min(Math.round((step / 5) * 100), 95)

      return h('div', { style: 'min-width: 180px;' }, [
        h('div', { style: 'display: flex; align-items: center; gap: 8px; margin-bottom: 6px;' }, [
          h(Tag, { color: config.tag, style: 'font-size: 12px; line-height: 18px; padding: 0 6px; margin: 0;' }, () => config.text),
          h('span', { style: 'font-size: 12px; color: #888; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 140px;' }, msg),
        ]),
        h('div', { style: `width: 100%; height: 6px; background: ${config.bg}; border-radius: 3px; overflow: hidden;` }, [
          h('div', { style: `height: 100%; width: ${percent}%; background: ${config.color}; border-radius: 3px; transition: width 0.3s ease;` }),
        ]),
      ])
    },
  },
  {
    title: '耗时',
    dataIndex: 'duration',
    key: 'duration',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      return h('span', { style: 'color: #595959; font-size: 13px;' }, record.duration || '-')
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
    width: 160,
    align: 'center',
    customRender: ({ record }) => {
      const isLoading = actionLoadingId.value === record.id

      if (record.status === 'pending') {
        return h('div', { style: 'display: flex; justify-content: center;' }, [
          h(Popconfirm, {
            title: '确定取消该任务吗？',
            onConfirm: () => handleCancel(record),
          }, () => h(Button, { type: 'link', size: 'small', danger: true, loading: isLoading }, () => '取消')),
        ])
      }

      if (record.status === 'processing') {
        return h('div', { style: 'display: flex; justify-content: center;' }, [
          h(Popconfirm, {
            title: '确定停止该任务吗？',
            onConfirm: () => handleStop(record),
          }, () => h(Button, { type: 'link', size: 'small', danger: true, loading: isLoading }, () => '停止')),
        ])
      }

      const mainBtns = [
        h(Button, { type: 'link', size: 'small', loading: isLoading, onClick: () => handleRetry(record) }, () => '重跑'),
      ]

      if (record.resultFileUrl) {
        mainBtns.push(
          h('a', { href: record.resultFileUrl, target: '_blank', rel: 'noopener noreferrer', style: 'padding: 0 4px;' }, () => '下载')
        )
      }

      const moreItems = [
        h(Menu.Item, { key: 'match', onClick: () => router.push(`/title-match?keyword=${encodeURIComponent(record.title)}`) }, () => '标题匹配'),
        h(Menu.Item, { key: 'rerun', onClick: () => handleRerunBackend(record) }, () => '重跑后半段'),
      ]

      const moreDropdown = h(Dropdown, {}, {
        default: () => h(Button, { type: 'link', size: 'small', style: 'padding: 0 4px;' }, () => '更多'),
        overlay: () => h(Menu, {}, () => moreItems),
      })

      return h('div', { style: 'display: flex; align-items: center; justify-content: center; gap: 2px; flex-wrap: wrap;' }, [...mainBtns, moreDropdown])
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
      <Button style="margin-left: auto;" @click="router.push('/title-match')">标题匹配</Button>
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
