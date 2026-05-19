<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination, Row, Col, Statistic } from 'ant-design-vue'
import { listTitles, markTitleUsed, batchChangeTrack } from '../api/titleLibrary.js'
import { listTracks } from '../api/track.js'

const route = useRoute()
const router = useRouter()

// ---- 数据状态 ----
const tableData = ref([])
const tracks = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)

// ---- 搜索状态 ----
const searchKeyword = ref('')
const searchPlatform = ref('')
const searchTrack = ref('')
const searchIsUsed = ref('')

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const filteredTracksForSearch = computed(() => {
  if (!searchPlatform.value) {
    return tracks.value
  }
  return tracks.value.filter(t => {
    if (!t.platforms) return false
    const trackPlatforms = t.platforms.split(',')
    return trackPlatforms.includes(searchPlatform.value)
  })
})

// ---- 表格选择 ----
const selectedRowKeys = ref([])
const selectedRows = ref([])
const rowSelection = {
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
}

// ---- 加载数据 ----
async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value.trim()
    if (searchPlatform.value) params.platform = searchPlatform.value
    if (searchTrack.value) params.trackId = searchTrack.value
    if (searchIsUsed.value !== '' && searchIsUsed.value !== undefined) params.isUsed = searchIsUsed.value
    params.page = currentPage.value
    params.pageSize = pageSize.value

    const result = await listTitles(params)
    if (result && Array.isArray(result.list)) {
      tableData.value = result.list
      totalCount.value = result.total || 0
    } else {
      tableData.value = result || []
      totalCount.value = result?.length || 0
    }
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败: ' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handlePageChange() {
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  searchPlatform.value = ''
  searchTrack.value = ''
  searchIsUsed.value = ''
  currentPage.value = 1
  loadData()
}

// ---- 操作 ----
async function handleMarkUsed(record) {
  try {
    const isUsed = record.isUsed === 1
    await markTitleUsed(record.id)
    message.success(isUsed ? '已取消使用' : '已标记使用')
    loadData()
  } catch (e) {
    message.error('操作失败: ' + (e?.message || '未知错误'))
  }
}

// ---- 改赛道 ----
const changeTrackModalOpen = ref(false)
const changeTrackForm = ref({ titleIds: [], trackId: '', singleRecord: null })
const changeTrackLoading = ref(false)

function openSingleChangeTrack(record) {
  changeTrackForm.value = { titleIds: [record.id], trackId: record.trackId || '', singleRecord: record }
  changeTrackModalOpen.value = true
}

function openBatchChangeTrack() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要修改赛道的标题')
    return
  }
  changeTrackForm.value = { titleIds: selectedRowKeys.value, trackId: '', singleRecord: null }
  changeTrackModalOpen.value = true
}

async function handleChangeTrackConfirm() {
  if (!changeTrackForm.value.trackId) {
    message.warning('请选择赛道')
    return
  }
  changeTrackLoading.value = true
  try {
    await batchChangeTrack(changeTrackForm.value.titleIds, changeTrackForm.value.trackId)
    message.success('修改赛道成功')
    changeTrackModalOpen.value = false
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
  } catch (e) {
    message.error('修改赛道失败: ' + (e?.message || '未知错误'))
  } finally {
    changeTrackLoading.value = false
  }
}

// ---- 表格列 ----
const columns = [
  {
    title: '标题内容',
    key: 'title',
    width: 280,
    customRender: ({ record }) => {
      const isUsed = record.isUsed === 1
      return h('div', {
        style: 'display: flex; align-items: center; gap: 8px;'
      }, [
        h(Button, {
          type: 'link',
          size: 'small',
          style: 'padding: 0; flex-shrink: 0; font-size: 12px;',
          onClick: () => {
            navigator.clipboard.writeText(record.title).then(() => {
              message.success('已复制')
            }).catch(() => {
              message.error('复制失败')
            })
          }
        }, () => '复制'),
        h(Button, {
          type: isUsed ? 'default' : 'primary',
          ghost: !isUsed,
          size: 'small',
          style: 'padding: 0 4px; flex-shrink: 0; font-size: 12px;',
          onClick: () => handleMarkUsed(record)
        }, () => isUsed ? '取消使用' : '使用了'),
        h('span', {
          style: `flex: 1; ${isUsed ? 'text-decoration: line-through; color: #999;' : ''}`
        }, record.title)
      ])
    },
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 360,
  },
  {
    title: '是否使用',
    key: 'isUsed',
    width: 90,
    align: 'center',
    customRender: ({ record }) => {
      const isUsed = record.isUsed === 1
      return h(Tag, { color: isUsed ? 'green' : 'default' }, () => isUsed ? '已使用' : '未使用')
    },
  },
  {
    title: '生成状态',
    key: 'generateStatus',
    width: 90,
    align: 'center',
    customRender: ({ record }) => {
      const status = record.generateStatus
      if (status === 2) return h(Tag, { color: 'orange' }, () => '生成中')
      if (status === 1) return h(Tag, { color: 'green' }, () => '已生成')
      return h(Tag, { color: 'default' }, () => '未生成')
    },
  },
  {
    title: '平台/赛道',
    key: 'platformTrack',
    ellipsis: true,
    width: 140,
    customRender: ({ record }) => {
      return h('span', {}, `${record.platform || '-'} / ${record.trackName || '-'}`)
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      return h(Button, { type: 'link', size: 'small', onClick: () => openSingleChangeTrack(record) }, () => '改赛道')
    },
  },
]

onMounted(() => {
  loadData()
  listTracks().then(res => {
    tracks.value = res || []
    // 如果 URL 带有 trackId，自动设置筛选并查询
    const queryTrackId = route.query.trackId
    if (queryTrackId) {
      const track = tracks.value.find(t => t.id === queryTrackId)
      if (track && track.platforms) {
        const firstPlatform = track.platforms.split(/[,，\s]+/).filter(Boolean)[0]
        if (firstPlatform) searchPlatform.value = firstPlatform
      }
      searchTrack.value = queryTrackId
      currentPage.value = 1
      loadData()
    }
  }).catch(() => {})
})

function goBackToMatch() {
  router.push('/title-match')
}
</script>

<template>
  <div>
    <Card title="标题库" :bordered="false">
      <template #extra>
        <Button size="small" @click="router.push('/title-library-track-stats')">赛道统计视图</Button>
        <Button size="small" @click="router.push('/title-generate')" style="margin-left: 8px;">生成标题</Button>
        <Button size="small" @click="goBackToMatch" style="margin-left: 8px;">返回标题匹配</Button>
      </template>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
        <Input v-model:value="searchKeyword" placeholder="搜索标题关键词" style="width: 220px;" @pressEnter="handleSearch" />
        <Select show-search v-model:value="searchPlatform" placeholder="选择平台" style="width: 140px;" allowClear>
          <Select.Option v-for="p in platformOptions" :key="p.value" :value="p.value" :label="p.label">{{ p.label }}</Select.Option>
        </Select>
        <Select show-search v-model:value="searchTrack" placeholder="选择赛道" style="width: 160px;" allowClear :disabled="!searchPlatform">
          <Select.Option v-for="t in filteredTracksForSearch" :key="t.id" :value="t.id" :label="t.name">{{ t.name }}</Select.Option>
        </Select>
        <Select show-search v-model:value="searchIsUsed" placeholder="使用状态" style="width: 130px;" allowClear>
          <Select.Option value="1">已使用</Select.Option>
          <Select.Option value="0">未使用</Select.Option>
        </Select>
        <Button type="primary" @click="handleSearch">查询</Button>
        <Button @click="handleReset">重置</Button>
      </div>

      <div v-if="selectedRowKeys.length > 0" style="margin-bottom: 12px;">
        <Button type="primary" @click="openBatchChangeTrack">批量修改赛道（已选 {{ selectedRowKeys.length }} 项）</Button>
      </div>

      <Table
        :columns="columns"
        :data-source="tableData"
        :pagination="false"
        row-key="id"
        :loading="loading"
        :row-selection="rowSelection"
        :scroll="{ x: 'max-content' }"
      />
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <Pagination
          v-model:current="currentPage"
          v-model:pageSize="pageSize"
          :total="totalCount"
          show-size-changer
          :page-size-options="['10', '20', '50']"
          :show-total="total => `共 ${total} 条`"
          @change="handlePageChange"
        />
      </div>
    </Card>

    <!-- 修改赛道弹窗 -->
    <Modal
      v-model:open="changeTrackModalOpen"
      :title="changeTrackForm.singleRecord ? '修改赛道' : '批量修改赛道'"
      :confirm-loading="changeTrackLoading"
      @ok="handleChangeTrackConfirm"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <Form.Item label="选择赛道" required>
          <Select show-search v-model:value="changeTrackForm.trackId" placeholder="请选择赛道">
            <Select.Option v-for="t in tracks" :key="t.id" :value="t.id" :label="t.name">{{ t.name }}</Select.Option>
          </Select>
        </Form.Item>
        <div v-if="!changeTrackForm.singleRecord" style="color: #999; font-size: 12px;">
          已选择 {{ changeTrackForm.titleIds.length }} 条标题
        </div>
      </Form>
    </Modal>

  </div>
</template>

