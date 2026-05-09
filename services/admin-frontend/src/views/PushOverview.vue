<script setup>
import { ref, computed, onMounted, h, watch } from 'vue'
import dayjs from 'dayjs'
import {
  Card, Tabs, Table, Tag, Button, Badge, Input, Select,
  Drawer, message, Row, Col, Statistic, Spin, Empty, Pagination, Modal, TimePicker, Popconfirm, DatePicker
} from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { getPushOverview, batchPushEmail, getUserHistory } from '../api/titleLibrary.js'
import { createScheduledPush, listScheduledPush, cancelScheduledPush } from '../api/scheduledPush.js'

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
  loadScheduledTasks()
})

// ---- Scheduled Push ----
const scheduledPushModalOpen = ref(false)
const scheduledPushLoading = ref(false)
const scheduledPushForm = ref({
  pushTime: null,
  userFilterType: 'all',
  userIds: [],
})

async function openScheduledPushModal() {
  scheduledPushForm.value = { pushTime: null, userFilterType: 'all', userIds: [] }
  scheduledPushModalOpen.value = true
}

async function handleCreateScheduledPush() {
  if (!scheduledPushForm.value.pushTime) {
    message.warning('请选择每日推送时间')
    return
  }
  const timeStr = scheduledPushForm.value.pushTime.format('HH:mm')
  scheduledPushLoading.value = true
  try {
    await createScheduledPush({
      pushTime: timeStr,
      userFilterType: scheduledPushForm.value.userFilterType,
      userIds: scheduledPushForm.value.userFilterType === 'selected' ? scheduledPushForm.value.userIds : [],
    })
    message.success('定时推送任务已创建')
    scheduledPushModalOpen.value = false
    loadScheduledTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || e.message || '创建失败')
  } finally {
    scheduledPushLoading.value = false
  }
}

// ---- Scheduled Task List ----
const scheduledTasks = ref([])
const scheduledTasksTotal = ref(0)
const scheduledTasksPage = ref(1)
const scheduledTasksLoading = ref(false)

async function loadScheduledTasks() {
  scheduledTasksLoading.value = true
  try {
    const result = await listScheduledPush({ page: scheduledTasksPage.value, pageSize: 10 })
    scheduledTasks.value = result?.list || []
    scheduledTasksTotal.value = result?.total || 0
  } catch (e) {
    // silent
  } finally {
    scheduledTasksLoading.value = false
  }
}

function onScheduledTasksPageChange(page) {
  scheduledTasksPage.value = page
  loadScheduledTasks()
}

async function handleCancelTask(id) {
  try {
    await cancelScheduledPush(id)
    message.success('任务已取消')
    loadScheduledTasks()
  } catch (e) {
    message.error(e?.response?.data?.msg || e.message || '取消失败')
  }
}

const scheduledStatusMap = {
  0: { color: 'processing', label: '待执行' },
  1: { color: 'warning', label: '执行中' },
  2: { color: 'success', label: '已执行' },
  3: { color: 'default', label: '已取消' },
}

const scheduledColumns = [
  {
    title: '每日推送时间',
    dataIndex: 'pushTime',
    width: 120,
    customRender: ({ record }) => record.pushTime ? record.pushTime + ':00' : '-',
  },
  {
    title: '用户范围',
    dataIndex: 'userFilterType',
    width: 100,
    customRender: ({ record }) => record.userFilterType === 'all' ? '全部用户' : '指定用户',
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    align: 'center',
    customRender: ({ record }) => {
      const s = scheduledStatusMap[record.status] || { color: 'default', label: '未知' }
      return h(Tag, { color: s.color }, () => s.label)
    },
  },
  {
    title: '上次执行',
    dataIndex: 'lastExecutedDate',
    width: 110,
    customRender: ({ text }) => text || '未执行过',
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 170,
    customRender: ({ text }) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-',
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      if (record.status === 0) {
        return h(Popconfirm, {
          title: '确认取消该任务？',
          okText: '确认',
          cancelText: '取消',
          onConfirm: () => handleCancelTask(record.id),
        }, () => h(Button, { type: 'link', size: 'small', danger: true }, () => '取消'))
      }
      return h('span', { style: 'color: #999;' }, '—')
    },
  },
]

// ---- Main table columns ----
const columns = [
  {
    title: '用户',
    key: 'userInfo',
    ellipsis: true,
    width: 140,
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; flex-direction: column; line-height: 1.4;' }, [
        h('span', { style: 'font-weight: 500; color: #262626;' }, record.username || '-'),
        record.wxName ? h('span', { style: 'font-size: 12px; color: #8c8c8c;' }, record.wxName) : null,
      ].filter(Boolean))
    },
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
        record.tracksUnpushed > 0
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
const historyList = ref([])
const historyLoading = ref(false)

async function openDrawer(record) {
  drawerRecord.value = record
  drawerOpen.value = true
  historyLoading.value = true
  historyList.value = []
  try {
    const list = await getUserHistory(record.userId)
    historyList.value = list || []
  } catch (e) {
    message.error('加载历史绑定失败')
  } finally {
    historyLoading.value = false
  }
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
      <Button type="primary" @click="openScheduledPushModal">定时推送</Button>
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
                <Tag v-if="track.hasPost && track.isPushed" color="blue">已推送</Tag>
                <Tag v-if="track.hasPost && !track.isPushed" color="warning">待推送</Tag>
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

    <!-- 定时推送任务列表 -->
    <div style="margin-top: 32px;">
      <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #262626;">定时推送任务</div>
      <Spin :spinning="scheduledTasksLoading">
        <Table
          :columns="scheduledColumns"
          :data-source="scheduledTasks"
          row-key="id"
          :pagination="false"
          size="small"
        />
        <div style="display: flex; justify-content: flex-end; margin-top: 12px;">
          <Pagination
            v-model:current="scheduledTasksPage"
            :total="scheduledTasksTotal"
            :page-size="10"
            :show-total="t => `共 ${t} 条`"
            @change="onScheduledTasksPageChange"
          />
        </div>
      </Spin>
    </div>
  </Card>

  <!-- 定时推送弹窗 -->
  <Modal
    v-model:open="scheduledPushModalOpen"
    title="创建每日定时推送任务"
    :confirm-loading="scheduledPushLoading"
    @ok="handleCreateScheduledPush"
  >
    <div style="display: flex; flex-direction: column; gap: 16px;">
      <div>
        <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">每日推送时间 <span style="color: #ff4d4f;">*</span></div>
        <TimePicker
          v-model:value="scheduledPushForm.pushTime"
          style="width: 100%;"
          format="HH:mm"
          placeholder="选择每日推送时间"
        />
      </div>
      <div>
        <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">用户范围</div>
        <Select v-model:value="scheduledPushForm.userFilterType" style="width: 100%;">
          <Select.Option value="all">全部用户</Select.Option>
          <Select.Option value="selected">指定用户</Select.Option>
        </Select>
      </div>
      <div v-if="scheduledPushForm.userFilterType === 'selected'" style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px; font-size: 13px; color: #52c41a;">
        将向选中的 {{ scheduledPushForm.userIds.length }} 位用户推送邮件
      </div>
      <div style="font-size: 12px; color: #8c8c8c;">
        系统每日在设定时间自动执行推送（每分钟检查一次），已执行的任务会显示在"上次执行"列。
      </div>
    </div>
  </Modal>

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

      <div>
        <div style="font-size: 12px; color: #999; margin-bottom: 8px;">
          历史绑定标题（共 {{ historyList.length }} 条）
        </div>
        <Spin :spinning="historyLoading">
          <div v-if="historyList.length > 0" style="display: flex; flex-direction: column; gap: 6px; max-height: 300px; overflow-y: auto;">
            <div
              v-for="item in historyList"
              :key="item.recommendDate + '-' + item.titleName"
              style="display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px;"
            >
              <span style="font-size: 12px; color: #389e0d; min-width: 90px; flex-shrink: 0;">{{ item.recommendDate }}</span>
              <span style="color: #333; font-size: 13px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="item.titleName">
                {{ item.titleName }}
              </span>
            </div>
          </div>
          <div v-else style="color: #999; font-size: 13px; padding: 12px; text-align: center;">
            暂无历史绑定记录
          </div>
        </Spin>
      </div>
    </div>
  </Drawer>
</template>
