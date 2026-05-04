<script setup>
import { ref, computed, onMounted, h, watch } from 'vue'
import dayjs from 'dayjs'
import {
  Card, DatePicker, Tabs, Table, Tag, Button, Badge, Input, Select,
  Drawer, message, Row, Col, Statistic, Spin, Empty, Pagination
} from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { getPushOverview, batchPushEmail } from '../api/titleLibrary.js'

const STORAGE_KEY = 'push-overview-filters'

function saveFilters() {
  const filters = {
    date: selectedDate.value?.format('YYYY-MM-DD'),
    type: activeTypeTab.value,
    keyword: searchKeyword.value,
    emailPushed: filterEmailPushed.value,
    articleComplete: filterArticleComplete.value,
    pageSize: pageSize.value,
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(filters))
}

function loadFilters() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const filters = JSON.parse(raw)
    if (filters.date) selectedDate.value = dayjs(filters.date)
    if (filters.type) {
      const validTypes = ['all', 'accountOpened', 'distributor', 'trial']
      activeTypeTab.value = validTypes.includes(filters.type) ? filters.type : 'all'
    }
    if (filters.keyword !== undefined) searchKeyword.value = filters.keyword
    if (filters.emailPushed !== undefined) filterEmailPushed.value = filters.emailPushed
    if (filters.articleComplete !== undefined) filterArticleComplete.value = filters.articleComplete
    if (filters.pageSize) pageSize.value = filters.pageSize
  } catch (e) {
    // ignore parse error
  }
}

const selectedDate = ref(dayjs())
const activeTypeTab = ref('all')
const loading = ref(false)
const data = ref([])
const total = ref(0)

// Search & filters
const searchKeyword = ref('')
const filterEmailPushed = ref('')
const filterArticleComplete = ref('')

// Pagination
const currentPage = ref(1)
const pageSize = ref(20)

const typeOptions = [
  { key: 'all', label: '全部' },
  { key: 'accountOpened', label: '开户' },
  { key: 'distributor', label: '分成' },
  { key: 'trial', label: '试用' },
]

const emailPushedOptions = [
  { value: '', label: '全部邮件状态' },
  { value: '1', label: '已推送' },
  { value: '0', label: '未推送' },
]

const articleCompleteOptions = [
  { value: '', label: '全部文章状态' },
  { value: '1', label: '文章完整' },
  { value: '0', label: '文章缺失' },
]

const stats = computed(() => {
  const list = data.value
  const pageTotal = list.length
  const pushed = list.filter(u => u.isEmailPushed).length
  const unpushed = pageTotal - pushed
  const complete = list.filter(u => u.tracksWithPost === u.totalTracks && u.totalTracks > 0).length
  const incomplete = pageTotal - complete
  return { total: total.value, pageTotal, pushed, unpushed, complete, incomplete }
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      date: selectedDate.value.format('YYYY-MM-DD'),
      type: activeTypeTab.value,
      keyword: searchKeyword.value.trim() || undefined,
      emailPushed: filterEmailPushed.value || undefined,
      articleComplete: filterArticleComplete.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    }
    const result = await getPushOverview(params)
    if (result && result.list) {
      data.value = result.list
      total.value = result.total || 0
    } else {
      data.value = []
      total.value = 0
    }
  } catch (e) {
    message.error('数据加载失败')
    data.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  loadData()
}

function onFilterChange() {
  currentPage.value = 1
  loadData()
}

function onPageChange(page, size) {
  currentPage.value = page
  pageSize.value = size
  loadData()
}

watch(selectedDate, () => {
  currentPage.value = 1
  saveFilters()
  loadData()
})

watch(activeTypeTab, () => {
  currentPage.value = 1
  saveFilters()
  loadData()
})

watch(searchKeyword, () => saveFilters())
watch(filterEmailPushed, () => saveFilters())
watch(filterArticleComplete, () => saveFilters())
watch(pageSize, () => saveFilters())

onMounted(() => {
  loadFilters()
  loadData()
})

const columns = [
  {
    title: '用户名',
    dataIndex: 'username',
    ellipsis: true,
    width: 120,
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    ellipsis: true,
    width: 180,
  },
  {
    title: '用户类型',
    key: 'userType',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      const tags = []
      if (record.userType === 1 || record.userType === '1') tags.push(h(Tag, { color: 'blue' }, () => '开户'))
      if (record.userType === 2 || record.userType === '2') tags.push(h(Tag, { color: 'purple' }, () => '分成'))
      if (record.userType === 3 || record.userType === '3') tags.push(h(Tag, { color: 'orange' }, () => '试用'))
      return h('div', { style: 'display: flex; gap: 4px; justify-content: center;' }, tags)
    },
  },
  {
    title: '订阅赛道',
    key: 'tracksInfo',
    ellipsis: true,
    width: 200,
    customRender: ({ record }) => {
      const names = (record.tracks || []).map(t => t.trackName).join('、')
      return h('span', { title: names }, names || '-')
    },
  },
  {
    title: '标题数量',
    key: 'titleCount',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      const total = record.totalTracks || 0
      const done = record.tracksWithTitle || 0
      const color = done === total && total > 0 ? '#52c41a' : done > 0 ? '#faad14' : '#f5222d'
      return h('span', { style: `color: ${color}; font-weight: 500;` }, `${done}/${total}`)
    },
  },
  {
    title: '文章进度',
    key: 'progress',
    width: 110,
    align: 'center',
    customRender: ({ record }) => {
      const total = record.totalTracks || 0
      const done = record.tracksWithPost || 0
      const color = done === total && total > 0 ? '#52c41a' : done > 0 ? '#faad14' : '#f5222d'
      return h('span', { style: `color: ${color}; font-weight: 500;` }, `${done}/${total}`)
    },
  },
  {
    title: '邮件状态',
    key: 'emailStatus',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      if (record.isEmailPushed) {
        return h(Tag, { color: 'success' }, () => '已推送')
      }
      return h(Tag, { color: 'error' }, () => '未推送')
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 160,
    align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 8px; justify-content: center;' }, [
        h(Button, {
          type: 'link',
          size: 'small',
          onClick: () => openDrawer(record),
        }, () => '查看详情'),
        !record.isEmailPushed && record.tracksWithPost > 0
          ? h(Button, {
              type: 'primary',
              size: 'small',
              loading: pushLoadingId.value === record.userId,
              onClick: () => handlePushSingle(record),
            }, () => '推送邮件')
          : null,
      ].filter(Boolean))
    },
  },
]

const drawerOpen = ref(false)
const drawerRecord = ref(null)

function openDrawer(record) {
  drawerRecord.value = record
  drawerOpen.value = true
}

const pushLoadingId = ref(null)

async function handlePushSingle(record) {
  pushLoadingId.value = record.userId
  try {
    await batchPushEmail({
      date: selectedDate.value.format('YYYY-MM-DD'),
      userIds: [record.userId],
    })
    message.success('推送成功')
    record.isEmailPushed = true
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '推送失败')
  } finally {
    pushLoadingId.value = null
  }
}

const selectedUserIds = ref([])
const batchPushLoading = ref(false)

const rowSelection = {
  onChange: (keys) => {
    selectedUserIds.value = keys
  },
}

async function handleBatchPush() {
  if (selectedUserIds.value.length === 0) {
    message.warning('请选择要推送的用户')
    return
  }
  batchPushLoading.value = true
  try {
    const result = await batchPushEmail({
      date: selectedDate.value.format('YYYY-MM-DD'),
      userIds: selectedUserIds.value,
    })
    const { success = 0, failed = 0 } = result
    if (failed === 0) {
      message.success(`推送成功，共发送 ${success} 封邮件`)
    } else {
      message.success(`推送完成：成功 ${success} 封，失败 ${failed} 封`)
    }
    selectedUserIds.value = []
    loadData()
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '推送失败')
  } finally {
    batchPushLoading.value = false
  }
}
</script>

<template>
  <Card title="推送概览" :bordered="false">
    <!-- 筛选栏 -->
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 4px;">推送日期</div>
        <DatePicker v-model:value="selectedDate" style="width: 160px;" />
      </div>
      <Input
        v-model:value="searchKeyword"
        placeholder="搜索用户名 / 邮箱"
        style="width: 220px;"
        @pressEnter="onSearch"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </Input>
      <Select v-model:value="filterEmailPushed" style="width: 150px;" @change="onFilterChange">
        <Select.Option v-for="opt in emailPushedOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
      </Select>
      <Select v-model:value="filterArticleComplete" style="width: 150px;" @change="onFilterChange">
        <Select.Option v-for="opt in articleCompleteOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
      </Select>
      <Button type="primary" @click="onSearch">查询</Button>
      <Button @click="() => { searchKeyword = ''; filterEmailPushed = ''; filterArticleComplete = ''; onSearch() }">重置</Button>
    </div>

    <!-- 用户类型 Tabs -->
    <Tabs v-model:activeKey="activeTypeTab" style="margin-bottom: 16px;">
      <Tabs.TabPane v-for="t in typeOptions" :key="t.key" :tab="t.label" />
    </Tabs>

    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 24px;">
      <Col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
        <Card size="small">
          <Statistic title="总用户" :value="stats.total" />
        </Card>
      </Col>
      <Col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
        <Card size="small">
          <Statistic title="已推送" :value="stats.pushed" value-style="color: #52c41a;" />
        </Card>
      </Col>
      <Col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
        <Card size="small">
          <Statistic title="未推送" :value="stats.unpushed" value-style="color: #f5222d;" />
        </Card>
      </Col>
      <Col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
        <Card size="small">
          <Statistic title="文章完整" :value="stats.complete" value-style="color: #52c41a;" />
        </Card>
      </Col>
      <Col :span="4" :xs="12" :sm="8" :md="6" :lg="4">
        <Card size="small">
          <Statistic title="文章缺失" :value="stats.incomplete" value-style="color: #f5222d;" />
        </Card>
      </Col>
    </Row>

    <!-- 批量操作 -->
    <div v-if="selectedUserIds.length > 0" style="margin-bottom: 16px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; display: flex; align-items: center; justify-content: space-between;">
      <span style="font-size: 14px; color: #096dd9;">已选择 {{ selectedUserIds.length }} 位用户</span>
      <Button type="primary" size="small" :loading="batchPushLoading" @click="handleBatchPush">
        批量推送邮件
      </Button>
    </div>

    <!-- 数据表格 -->
    <Spin :spinning="loading">
      <Table
        :columns="columns"
        :data-source="data"
        row-key="userId"
        :row-selection="rowSelection"
        :scroll="{ x: 'max-content' }"
        :pagination="false"
      >
        <template #expandedRowRender="{ record }">
          <div style="padding: 12px 24px; background: #fafafa;">
            <div style="font-size: 14px; font-weight: 500; margin-bottom: 12px;">
              {{ record.username }} — 赛道文章明细
            </div>
            <div style="display: flex; flex-direction: column; gap: 8px;">
              <div
                v-for="track in record.tracks"
                :key="track.trackId"
                style="display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #fff; border-radius: 4px; border: 1px solid #f0f0f0;"
              >
                <span style="font-weight: 500; min-width: 100px;">{{ track.trackName }}</span>
                <Tag v-if="track.hasPost" color="success">已生成</Tag>
                <Tag v-else color="error">未生成</Tag>
                <span v-if="track.postTitle" style="color: #666; font-size: 13px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  {{ track.postTitle }}
                </span>
                <span v-else style="color: #999; font-size: 13px;">—</span>
              </div>
            </div>
          </div>
        </template>
        <template #emptyText>
          <Empty description="该日期下暂无数据" />
        </template>
      </Table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <Pagination
          v-model:current="currentPage"
          v-model:pageSize="pageSize"
          :total="total"
          show-size-changer
          :page-size-options="['10', '20', '50']"
          :show-total="t => `共 ${t} 条`"
          @change="onPageChange"
        />
      </div>
    </Spin>
  </Card>

  <!-- 详情抽屉 -->
  <Drawer
    v-model:open="drawerOpen"
    :title="drawerRecord?.username + ' — 推送详情'"
    width="480"
    :footer="null"
  >
    <div v-if="drawerRecord" style="display: flex; flex-direction: column; gap: 16px;">
      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 4px;">邮箱</div>
        <div>{{ drawerRecord.email || '-' }}</div>
      </div>
      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 4px;">用户类型</div>
        <div style="display: flex; gap: 8px;">
          <Tag v-if="drawerRecord.userType === 1 || drawerRecord.userType === '1'" color="blue">开户</Tag>
          <Tag v-if="drawerRecord.userType === 2 || drawerRecord.userType === '2'" color="purple">分成</Tag>
          <Tag v-if="drawerRecord.userType === 3 || drawerRecord.userType === '3'" color="orange">试用</Tag>
        </div>
      </div>
      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 4px;">邮件状态</div>
        <Tag v-if="drawerRecord.isEmailPushed" color="success">已推送</Tag>
        <Tag v-else color="error">未推送</Tag>
      </div>
      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 8px;">赛道文章状态</div>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <div
            v-for="track in drawerRecord.tracks"
            :key="track.trackId"
            style="display: flex; flex-direction: column; gap: 6px; padding: 10px 12px; border-radius: 4px;"
            :style="track.hasPost ? 'background: #f6ffed; border: 1px solid #b7eb8f;' : 'background: #fff2f0; border: 1px solid #ffccc7;'"
          >
            <div style="display: flex; align-items: center; gap: 12px;">
              <span style="font-weight: 500; min-width: 100px;">{{ track.trackName }}</span>
              <Badge v-if="track.hasPost" status="success" text="已生成" />
              <Badge v-else status="error" text="未生成" />
              <span v-if="track.postTitle" style="color: #666; font-size: 13px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="track.postTitle">
                {{ track.postTitle }}
              </span>
            </div>
            <div v-if="track.titleName" style="display: flex; align-items: center; gap: 8px; padding-left: 112px;">
              <Tag size="small" color="blue">标题</Tag>
              <span style="color: #1890ff; font-size: 13px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="track.titleName">
                {{ track.titleName }}
              </span>
            </div>
            <div v-else style="display: flex; align-items: center; gap: 8px; padding-left: 112px;">
              <Tag size="small" color="default">标题</Tag>
              <span style="color: #999; font-size: 13px;">—</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Drawer>
</template>
