<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Card, Row, Col, Table, Button, Tag, Modal, Form, Input, Select, DatePicker, message, Pagination, Statistic } from 'ant-design-vue'
import dayjs from 'dayjs'
import { listOrders, createOrder, getOrderStats, refundOrder, exportOrders } from '../api/order.js'
import { listUsers } from '../api/user.js'
import { listMembershipPlans } from '../api/membershipPlan.js'

const stats = ref({
  todayAmount: 0,
  monthAmount: 0,
  yearAmount: 0,
  totalAmount: 0,
  orderCount: 0,
  byPlan: [],
  refundToday: 0,
  refundMonth: 0,
  refundTotal: 0,
})
const data = ref([])
const total = ref(0)
const loading = ref(false)

// Filters
const filterUserId = ref('')
const filterType = ref('')
const filterPlanId = ref('')
const filterDateRange = ref(null)

// Pagination
const currentPage = ref(1)
const pageSize = ref(20)

// Options
const users = ref([])
const plans = ref([])
const typeOptions = [
  { value: 'open_account', label: '开户' },
  { value: 'renew', label: '续费' },
  { value: 'upgrade', label: '升级' },
]

const typeColorMap = {
  open_account: 'blue',
  renew: 'green',
  upgrade: 'purple',
}
const typeLabelMap = {
  open_account: '开户',
  renew: '续费',
  upgrade: '升级',
}

function isRefunded(record) {
  return record.refundAmount != null && Number(record.refundAmount) > 0
}

async function loadStats() {
  try {
    const result = await getOrderStats()
    stats.value = result || {}
  } catch (e) {
    // silent
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
    }
    if (filterUserId.value) params.userId = filterUserId.value
    if (filterType.value) params.type = filterType.value
    if (filterPlanId.value) params.planId = filterPlanId.value
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      params.dateStart = filterDateRange.value[0].format('YYYY-MM-DD')
      params.dateEnd = filterDateRange.value[1].format('YYYY-MM-DD')
    }
    const result = await listOrders(params)
    if (result && result.list) {
      data.value = result.list
      total.value = result.total || 0
    }
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [u, p] = await Promise.all([listUsers(), listMembershipPlans()])
    users.value = (u || []).filter(x => x.status !== 0)
    plans.value = (p || []).filter(x => x.isActive !== 0)
  } catch (e) {
    // silent
  }
}

function onSearch() {
  currentPage.value = 1
  loadData()
}

function onPageChange(page, size) {
  currentPage.value = page
  pageSize.value = size
  loadData()
}

async function handleExport() {
  try {
    const params = {}
    if (filterUserId.value) params.userId = filterUserId.value
    if (filterType.value) params.type = filterType.value
    if (filterPlanId.value) params.planId = filterPlanId.value
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      params.dateStart = filterDateRange.value[0].format('YYYY-MM-DD')
      params.dateEnd = filterDateRange.value[1].format('YYYY-MM-DD')
    }
    const blob = await exportOrders(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '订单列表_' + dayjs().format('YYYYMMDD') + '.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

// Manual create order modal
const createModalOpen = ref(false)
const createForm = ref({ userId: '', planId: '', type: 'renew', amount: '', remark: '' })

// Refund modal
const refundModalOpen = ref(false)
const refundForm = ref({ id: '', amount: '', refundAmount: '' })

function openCreateModal() {
  createForm.value = { userId: '', planId: '', type: 'renew', amount: '', remark: '' }
  createModalOpen.value = true
}

function openRefundModal(record) {
  refundForm.value = {
    id: record.id,
    amount: record.amount,
    refundAmount: record.refundAmount || record.amount || '',
  }
  refundModalOpen.value = true
}

async function handleRefund() {
  if (!refundForm.value.refundAmount && refundForm.value.refundAmount !== 0) {
    message.warning('请输入退单金额')
    return
  }
  const amt = parseFloat(refundForm.value.refundAmount)
  if (isNaN(amt) || amt <= 0) {
    message.warning('退单金额必须大于0')
    return
  }
  const orderAmount = parseFloat(refundForm.value.amount || 0)
  if (amt > orderAmount) {
    message.warning('退单金额不能大于订单金额')
    return
  }
  try {
    await refundOrder({
      id: refundForm.value.id,
      refundAmount: amt,
    })
    message.success('退单成功')
    refundModalOpen.value = false
    loadData()
    loadStats()
  } catch (e) {
    message.error(e.message || '退单失败')
  }
}

async function handleCreateOrder() {
  if (!createForm.value.userId) {
    message.warning('请选择用户')
    return
  }
  if (!createForm.value.planId) {
    message.warning('请选择套餐')
    return
  }
  try {
    const payload = {
      userId: createForm.value.userId,
      planId: createForm.value.planId,
      type: createForm.value.type,
      remark: createForm.value.remark,
    }
    if (createForm.value.amount) {
      payload.amount = parseFloat(createForm.value.amount)
    }
    await createOrder(payload)
    message.success('订单创建成功')
    createModalOpen.value = false
    loadData()
    loadStats()
  } catch (e) {
    message.error('创建失败')
  }
}

const columns = [
  { title: '用户名', dataIndex: 'userName', width: 120 },
  { title: '套餐', dataIndex: 'planName', width: 140 },
  {
    title: '类型',
    dataIndex: 'type',
    width: 90,
    align: 'center',
    customRender: ({ text }) => h(Tag, { color: typeColorMap[text] || 'default' }, () => typeLabelMap[text] || text),
  },
  {
    title: '金额',
    dataIndex: 'amount',
    width: 100,
    align: 'right',
    customRender: ({ text }) => '¥' + (text != null ? Number(text).toFixed(2) : '0.00'),
  },
  {
    title: '退单金额',
    dataIndex: 'refundAmount',
    width: 100,
    align: 'right',
    customRender: ({ text }) => {
      if (text != null && Number(text) > 0) {
        return h('span', { style: { color: '#ff4d4f' } }, '¥' + Number(text).toFixed(2))
      }
      return h('span', { style: { color: '#999' } }, '-')
    },
  },
  { title: '备注', dataIndex: 'remark', ellipsis: true, width: 150 },
  {
    title: '时间',
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
      if (isRefunded(record)) {
        return h(Tag, { color: 'red' }, () => '已退单')
      }
      return h(Button, {
        type: 'link',
        size: 'small',
        danger: true,
        onClick: () => openRefundModal(record),
      }, () => '退单')
    },
  },
]

onMounted(() => {
  loadOptions()
  loadStats()
  loadData()
})
</script>

<template>
  <Card title="收益管理" :bordered="false">
    <!-- Stats Cards -->
    <Row :gutter="16" style="margin-bottom: 24px;">
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="今日收益" :value="stats.todayAmount || 0" :precision="2" prefix="¥" value-style="color: #52c41a;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="本月收益" :value="stats.monthAmount || 0" :precision="2" prefix="¥" value-style="color: #1890ff;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="本年收益" :value="stats.yearAmount || 0" :precision="2" prefix="¥" value-style="color: #722ed1;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="总订单数" :value="stats.orderCount || 0" value-style="color: #fa8c16;" />
        </Card>
      </Col>
    </Row>

    <!-- Refund Stats -->
    <Row :gutter="16" style="margin-bottom: 24px;">
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="今日退单" :value="stats.refundToday || 0" :precision="2" prefix="¥" value-style="color: #ff4d4f;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="本月退单" :value="stats.refundMonth || 0" :precision="2" prefix="¥" value-style="color: #ff4d4f;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="累计退单" :value="stats.refundTotal || 0" :precision="2" prefix="¥" value-style="color: #ff4d4f;" />
        </Card>
      </Col>
      <Col :span="6" :xs="12" :sm="12" :md="6" :lg="6">
        <Card size="small">
          <Statistic title="实际总收益" :value="(stats.totalAmount || 0) - (stats.refundTotal || 0)" :precision="2" prefix="¥" value-style="color: #52c41a;" />
        </Card>
      </Col>
    </Row>

    <!-- Plan Breakdown -->
    <Row :gutter="16" style="margin-bottom: 24px;">
      <Col :span="24">
        <Card size="small" title="按套餐统计（已排除退单）">
          <div style="display: flex; gap: 16px; flex-wrap: wrap;">
            <div v-for="item in stats.byPlan || []" :key="item.planName" style="padding: 8px 16px; background: #f6ffed; border-radius: 4px; border: 1px solid #b7eb8f;">
              <span style="font-weight: 500;">{{ item.planName }}</span>
              <span style="margin-left: 8px; color: #52c41a;">¥{{ Number(item.amount || 0).toFixed(2) }}</span>
              <span style="margin-left: 8px; color: #999; font-size: 12px;">{{ item.count }}单</span>
            </div>
          </div>
        </Card>
      </Col>
    </Row>

    <!-- Filters -->
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <Select v-model:value="filterUserId" placeholder="选择用户" style="width: 160px;" allowClear>
        <Select.Option v-for="u in users" :key="u.id" :value="u.id">{{ u.nickName ? u.username + '(' + u.nickName + ')' : u.username }}</Select.Option>
      </Select>
      <Select v-model:value="filterType" placeholder="订单类型" style="width: 120px;" allowClear>
        <Select.Option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</Select.Option>
      </Select>
      <Select v-model:value="filterPlanId" placeholder="选择套餐" style="width: 160px;" allowClear>
        <Select.Option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}</Select.Option>
      </Select>
      <DatePicker.RangePicker v-model:value="filterDateRange" style="width: 240px;" />
      <Button type="primary" @click="onSearch">查询</Button>
      <Button @click="() => { filterUserId = ''; filterType = ''; filterPlanId = ''; filterDateRange = null; onSearch(); }">重置</Button>
      <Button @click="handleExport">导出</Button>
      <Button type="primary" @click="openCreateModal">手动创建订单</Button>
    </div>

    <!-- Table -->
    <Table
      :columns="columns"
      :data-source="data"
      row-key="id"
      :loading="loading"
      :pagination="false"
      size="small"
    />

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
  </Card>

  <!-- Create Order Modal -->
  <Modal
    v-model:open="createModalOpen"
    title="手动创建订单"
    @ok="handleCreateOrder"
  >
    <Form layout="vertical" :model="createForm">
      <Form.Item label="用户" required>
        <Select v-model:value="createForm.userId" placeholder="选择用户">
          <Select.Option v-for="u in users" :key="u.id" :value="u.id">{{ u.nickName ? u.username + '(' + u.nickName + ')' : u.username }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="套餐" required>
        <Select v-model:value="createForm.planId" placeholder="选择套餐">
          <Select.Option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="类型" required>
        <Select v-model:value="createForm.type">
          <Select.Option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="金额（留空自动取套餐价格）">
        <Input v-model:value="createForm.amount" placeholder="自动计算" />
      </Form.Item>
      <Form.Item label="备注">
        <Input v-model:value="createForm.remark" placeholder="选填" />
      </Form.Item>
    </Form>
  </Modal>

  <!-- Refund Modal -->
  <Modal
    v-model:open="refundModalOpen"
    title="订单退单"
    @ok="handleRefund"
  >
    <Form layout="vertical" :model="refundForm">
      <Form.Item label="订单金额">
        <Input :value="'¥' + Number(refundForm.amount || 0).toFixed(2)" disabled />
      </Form.Item>
      <Form.Item label="退单金额" required>
        <Input v-model:value="refundForm.refundAmount" placeholder="请输入退单金额" />
      </Form.Item>
      <div style="font-size: 12px; color: #ff4d4f;">
        退单后该订单金额将不再计入收益统计，退单金额会单独记录。
      </div>
    </Form>
  </Modal>
</template>
