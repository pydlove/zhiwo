<script setup>
import { ref, onMounted, onUnmounted, computed, h } from 'vue'
import { Table, Tag, Input, Select, Button, Modal, Form, message, Popconfirm } from 'ant-design-vue'
import { listTracks } from '../api/track.js'
import { createTitleGenerateTask, listTitleGenerateTasks, cancelTitleGenerateTask } from '../api/titleGenerate.js'

// ---- 生成弹框状态 ----
const GENERATE_CONFIG_KEY = 'titleGenerate_generateConfig'
const savedGenerateConfig = JSON.parse(localStorage.getItem(GENERATE_CONFIG_KEY) || 'null')

const generateModalOpen = ref(false)
const generateCount = ref(savedGenerateConfig?.count || 3)
const generateOutputPath = ref(savedGenerateConfig?.outputPath || '')
const generatePlatforms = ref(savedGenerateConfig?.platforms || [])
const generateTrackIds = ref(savedGenerateConfig?.trackIds || [])
const generateInstruction = ref(savedGenerateConfig?.instruction || '')
const generating = ref(false)

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const tracks = ref([])

const filteredTracksForGenerate = computed(() => {
  if (!generatePlatforms.value || generatePlatforms.value.length === 0) {
    return tracks.value
  }
  return tracks.value.filter(t => {
    if (!t.platforms) return false
    const trackPlatforms = t.platforms.split(',')
    return generatePlatforms.value.some(p => trackPlatforms.includes(p))
  })
})

function saveGenerateConfig() {
  localStorage.setItem(GENERATE_CONFIG_KEY, JSON.stringify({
    count: generateCount.value,
    outputPath: generateOutputPath.value,
    platforms: generatePlatforms.value,
    trackIds: generateTrackIds.value,
    instruction: generateInstruction.value,
  }))
}

function openGenerateModal() {
  generateModalOpen.value = true
}

function onPlatformChange() {
  if (!generatePlatforms.value || generatePlatforms.value.length === 0) {
    return
  }
  generateTrackIds.value = generateTrackIds.value.filter(trackId => {
    const track = tracks.value.find(t => t.id === trackId)
    if (!track || !track.platforms) return false
    const trackPlatforms = track.platforms.split(',')
    return generatePlatforms.value.some(p => trackPlatforms.includes(p))
  })
}

async function handleGenerate() {
  if (!generateCount.value || generateCount.value < 1) {
    message.warning('请输入有效的生成数量')
    return
  }
  generating.value = true
  try {
    const result = await createTitleGenerateTask({
      countPerCombo: parseInt(generateCount.value),
      outputPath: generateOutputPath.value.trim(),
      platforms: generatePlatforms.value,
      trackIds: generateTrackIds.value,
      instruction: generateInstruction.value.trim(),
    })
    generateModalOpen.value = false
    saveGenerateConfig()
    message.success(result.message || '生成任务已创建')
    fetchTasks()
  } catch (e) {
    message.error('提交失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

// ---- 任务列表状态 ----
const tasks = ref([])
const loading = ref(false)
const statusFilter = ref('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '排队中', value: 'pending' },
  { label: '进行中', value: 'processing' },
  { label: '已停止', value: 'stopped' },
  { label: '已完成', value: 'completed' },
  { label: '失败', value: 'failed' },
]

const stepItems = [
  { title: '准备数据' },
  { title: '大模型生成' },
  { title: '解析入库' },
  { title: '生成Excel' },
  { title: '完成' },
]

function fetchTasks() {
  loading.value = true
  listTitleGenerateTasks({
    status: statusFilter.value || undefined,
  })
    .then(res => {
      tasks.value = res?.list || []
    })
    .catch(() => {
      message.error('加载生成标题任务失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function onStatusChange() {
  fetchTasks()
}

function getStatusTag(status, step) {
  switch (status) {
    case 'pending':
      return { color: 'blue', text: '排队中' }
    case 'processing':
      return { color: 'orange', text: step ? (step === 1 ? '准备数据' : step === 2 ? '大模型生成' : step === 3 ? '解析入库' : step === 4 ? '生成Excel' : '处理中') : '进行中' }
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

async function handleCancel(record) {
  try {
    await cancelTitleGenerateTask(record.id)
    message.success('任务已取消')
    fetchTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || '取消失败')
  }
}

const columns = [
  {
    title: '任务ID',
    dataIndex: 'id',
    key: 'id',
    width: 220,
    ellipsis: true,
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
        processing: { color: '#fa8c16', bg: '#fff7e6', tag: 'orange',  text: stepItems[Math.min(step - 1, 4)]?.title || '处理中' },
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
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    align: 'center',
    customRender: ({ record }) => {
      if (record.status === 'pending') {
        return h('div', { style: 'display: flex; justify-content: center;' }, [
          h(Popconfirm, {
            title: '确定取消该任务吗？',
            onConfirm: () => handleCancel(record),
          }, () => h(Button, { type: 'link', size: 'small', danger: true }, () => '取消')),
        ])
      }
      if (record.status === 'completed' && record.resultFileUrl) {
        return h('div', { style: 'display: flex; justify-content: center;' }, [
          h('a', { href: record.resultFileUrl, target: '_blank', rel: 'noopener noreferrer' }, () => '下载结果'),
        ])
      }
      return null
    },
  },
]

onMounted(() => {
  listTracks().then(res => {
    tracks.value = res || []
  }).catch(() => {})
  fetchTasks()
  const interval = setInterval(fetchTasks, 5000)
  onUnmounted(() => clearInterval(interval))
})
</script>

<template>
  <div>
    <div style="display: flex; gap: 12px; margin-bottom: 16px;">
      <Button type="primary" @click="openGenerateModal">生成标题</Button>
      <Select
        v-model:value="statusFilter"
        :options="statusOptions"
        style="width: 140px; margin-left: auto;"
        placeholder="状态筛选"
        @change="onStatusChange"
      />
    </div>

    <Table
      :columns="columns"
      :dataSource="tasks"
      :loading="loading"
      rowKey="id"
      :pagination="{ pageSize: 20 }"
      size="small"
      :scroll="{ x: 1000 }"
    />

    <Modal
      v-model:open="generateModalOpen"
      title="生成标题（V2-大模型）"
      :mask-closable="false"
      :confirm-loading="generating"
      @ok="handleGenerate"
      :width="560"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <div style="background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
          <div style="font-size: 13px; color: #096dd9; margin-bottom: 8px;">
            <strong>生成说明</strong>
          </div>
          <div style="font-size: 12px; color: #096dd9; line-height: 1.8;">
            1. 选择平台和赛道，不选则生成全部<br>
            2. 数量指每个平台下每个赛道生成的标题数<br>
            3. 系统会按平台分批调用大模型（kimi/minimax）生成<br>
            4. 生成结果包含标题、平台、赛道名称和 SEO 描述
          </div>
        </div>
        <Form.Item label="选择平台">
          <Select
            show-search
            v-model:value="generatePlatforms"
            mode="multiple"
            placeholder="不选则生成全部平台"
            style="width: 100%;"
            @change="onPlatformChange"
          >
            <Select.Option v-for="p in platformOptions" :key="p.value" :value="p.value" :label="p.label">{{ p.label }}</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="选择赛道">
          <Select
            show-search
            v-model:value="generateTrackIds"
            mode="multiple"
            placeholder="不选则生成全部赛道"
            style="width: 100%;"
          >
            <Select.Option v-for="t in filteredTracksForGenerate" :key="t.id" :value="t.id" :label="t.name">{{ t.name }}</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="每个组合生成数量" required>
          <Input v-model:value="generateCount" type="number" min="1" max="20" placeholder="例如：3" />
        </Form.Item>
        <Form.Item label="生成方向（可选）">
          <Input.TextArea
            v-model:value="generateInstruction"
            placeholder="例如：更口语化、更具悬念、突出数字效果、适合抖音风格、偏新闻报道类..."
            :rows="2"
            :maxlength="1000"
            show-count
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">不填则使用默认策略生成</div>
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>
