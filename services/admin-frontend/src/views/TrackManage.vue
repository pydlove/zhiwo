<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, Radio, message, Pagination } from 'ant-design-vue'
import { listTracks, saveTrack, deleteTrack } from '../api/track.js'

const router = useRouter()
const searchName = ref('')
const platformFilter = ref(undefined)
const page = ref(1)
const pageSize = ref(10)

const rawList = ref([])

const columns = [
  { title: '赛道名称', dataIndex: 'name', key: 'name' },
  { title: '平台', dataIndex: 'platform', key: 'platform' },
  { title: '博主数', dataIndex: 'bloggerCount', key: 'bloggerCount' },
  { title: '文章数', dataIndex: 'articleCount', key: 'articleCount' },
  { title: '简介', dataIndex: 'intro', key: 'intro', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 180 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增赛道')
const form = ref({ name: '', platform: '公众号', intro: '' })
const coverImage = ref('')
const editingId = ref(null)
const selectedRowKeys = ref([])

const rowSelection = {
  onChange: (keys) => {
    selectedRowKeys.value = keys
  },
}

const defaultGradients = [
  'linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%)',
  'linear-gradient(135deg, #fdba74 0%, #fb923c 100%)',
  'linear-gradient(135deg, #86efac 0%, #4ade80 100%)',
  'linear-gradient(135deg, #c4b5fd 0%, #a78bfa 100%)',
  'linear-gradient(135deg, #fca5a5 0%, #f87171 100%)',
  'linear-gradient(135deg, #67e8f9 0%, #22d3ee 100%)',
  'linear-gradient(135deg, #f0abfc 0%, #e879f9 100%)',
  'linear-gradient(135deg, #fcd34d 0%, #fbbf24 100%)',
]

function randomGradient() {
  return defaultGradients[Math.floor(Math.random() * defaultGradients.length)]
}

async function loadData() {
  try {
    const list = await listTracks()
    rawList.value = list.map(t => ({
      ...t,
      platform: t.platforms || '-',
      articleCount: t.postCount || 0,
      createTime: t.createdAt ? t.createdAt.slice(0, 10) : '-',
    }))
    page.value = 1
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredList = computed(() => {
  let list = rawList.value
  const keyword = searchName.value.trim()
  if (keyword) {
    list = list.filter(t => (t.name || '').includes(keyword))
  }
  if (platformFilter.value) {
    list = list.filter(t => (t.platform || '').includes(platformFilter.value))
  }
  return list
})

const tableData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  page.value = 1
}

function handleReset() {
  searchName.value = ''
  platformFilter.value = undefined
  page.value = 1
}

function handlePageChange(p) {
  page.value = p
}

function handleAdd() {
  modalTitle.value = '新增赛道'
  editingId.value = null
  form.value = { name: '', platform: '公众号', intro: '' }
  coverImage.value = ''
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑赛道'
  editingId.value = record.id
  const firstPlatform = (record.platform || '公众号').split('、')[0].split(',')[0].trim()
  form.value = { name: record.name, platform: firstPlatform || '公众号', intro: record.intro || '' }
  coverImage.value = ''
  if (record.coverJson) {
    try {
      const parsed = JSON.parse(record.coverJson)
      if (parsed.image) coverImage.value = parsed.image
    } catch (e) {
      // ignore
    }
  }
  modalOpen.value = true
}

function handleCoverUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    coverImage.value = reader.result
  }
  reader.readAsDataURL(file)
}

async function handleSave() {
  if (!form.value.name || !form.value.platform) {
    message.warning('请填写必填项')
    return
  }
  try {
    let coverJson = ''
    if (coverImage.value) {
      coverJson = JSON.stringify({ image: coverImage.value, gradient: randomGradient() })
    }
    await saveTrack({
      id: editingId.value || undefined,
      name: form.value.name,
      platforms: form.value.platform,
      intro: form.value.intro,
      coverJson: coverJson || undefined,
    })
    message.success(modalTitle.value + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除该赛道？',
    content: '删除后数据将不在列表中显示，但可在数据库中保留。',
    async onOk() {
      try {
        await deleteTrack(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

function handleBatchDelete() {
  const ids = selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: `确认批量删除 ${ids.length} 条赛道？`,
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await Promise.all(ids.map(id => deleteTrack(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (e) {
        message.error('批量删除失败')
      }
    },
  })
}

function goToBloggers(record) {
  router.push('/bloggers?trackId=' + record.id)
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="searchName" placeholder="搜索赛道名称" style="width: 240px;" />
      <Select v-model:value="platformFilter" placeholder="全部平台" style="min-width: 140px;" allow-clear>
        <Select.Option value="公众号">公众号</Select.Option>
        <Select.Option value="今日头条">今日头条</Select.Option>
        <Select.Option value="百家号">百家号</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button v-if="selectedRowKeys.length" danger style="margin-left: auto;" @click="handleBatchDelete">批量删除 ({{ selectedRowKeys.length }})</Button>
      <Button type="primary" :style="selectedRowKeys.length ? { marginLeft: '12px' } : { marginLeft: 'auto' }" @click="handleAdd">+ 新增赛道</Button>
    </div>

    <Table :columns="columns" :data-source="tableData" :pagination="false" row-key="id" :row-selection="rowSelection">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" @click="goToBloggers(record)">查看博主</a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination :current="page" :total="filteredList.length" :page-size="pageSize" @change="handlePageChange" />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="赛道名称" required>
        <Input v-model:value="form.name" placeholder="请输入赛道名称，如：情感故事" />
      </Form.Item>
      <Form.Item label="所属平台" required>
        <Radio.Group v-model:value="form.platform">
          <Radio value="公众号">公众号</Radio>
          <Radio value="今日头条">今日头条</Radio>
          <Radio value="百家号">百家号</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item label="赛道简介">
        <Input.TextArea v-model:value="form.intro" placeholder="请输入赛道简介，帮助用户快速了解该赛道定位" :rows="3" />
      </Form.Item>
      <Form.Item label="封面图标">
        <div style="display: flex; gap: 12px; align-items: flex-start;">
          <label style="width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative;">
            <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleCoverUpload">
            <img v-if="coverImage" :src="coverImage" style="width: 100%; height: 100%; object-fit: cover;">
            <template v-else>
              <div style="font-size: 24px; margin-bottom: 8px;">+</div>
              <div>上传图标</div>
            </template>
          </label>
        </div>
        <div style="font-size: 12px; color: #999; margin-top: 4px;">建议尺寸 240×120px，首页赛道卡片将展示该图标</div>
      </Form.Item>
    </Form>
  </Modal>
</template>
