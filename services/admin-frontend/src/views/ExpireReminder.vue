<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Card, Table, Button, Select, message, Tag, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getExpiringUsers, sendExpireReminderEmails } from '../api/user.js'

const data = ref([])
const loading = ref(false)
const selectedRowKeys = ref([])
const days = ref(7)

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => { selectedRowKeys.value = keys },
}))

async function loadData() {
  loading.value = true
  selectedRowKeys.value = []
  try {
    const result = await getExpiringUsers(days.value)
    data.value = result || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadData()
}

function getDaysLeft(expireDate) {
  if (!expireDate) return 0
  const expire = dayjs(expireDate)
  const now = dayjs()
  return expire.diff(now, 'day')
}

function getUrgencyColor(expireDate) {
  const daysLeft = getDaysLeft(expireDate)
  if (daysLeft <= 1) return '#ff4d4f'
  if (daysLeft <= 3) return '#fa8c16'
  return '#52c41a'
}

const columns = [
  { title: '用户名', dataIndex: 'username', width: 160 },
  { title: '邮箱', dataIndex: 'email', ellipsis: true },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  {
    title: '当前套餐',
    dataIndex: 'planName',
    width: 140,
    customRender: ({ text }) => text || '—',
  },
  {
    title: '到期时间',
    dataIndex: 'expireDate',
    width: 130,
    customRender: ({ text }) => text || '—',
  },
  {
    title: '剩余天数',
    key: 'daysLeft',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      const daysLeft = getDaysLeft(record.expireDate)
      const color = getUrgencyColor(record.expireDate)
      return h('span', { style: { color, fontWeight: 600 } }, daysLeft + '天')
    },
  },
]

const confirmModalOpen = ref(false)
const sending = ref(false)

async function handleSend() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要发送邮件的用户')
    return
  }
  confirmModalOpen.value = true
}

async function handleConfirmSend() {
  sending.value = true
  try {
    const result = await sendExpireReminderEmails(selectedRowKeys.value)
    message.success(`发送完成：成功 ${result.sent} 封，失败 ${result.failed} 封`)
    confirmModalOpen.value = false
  } catch (e) {
    message.error(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <Card title="到期提醒" :bordered="false">
    <!-- 筛选区 -->
    <div style="display: flex; gap: 12px; margin-bottom: 16px; align-items: center; flex-wrap: wrap;">
      <span style="font-size: 14px; color: #595959;">查看未来</span>
      <Select show-search v-model:value="days" style="width: 100px;" @change="onSearch">
        <Select.Option :value="3">3天内</Select.Option>
        <Select.Option :value="7">7天内</Select.Option>
        <Select.Option :value="14">14天内</Select.Option>
        <Select.Option :value="30">30天内</Select.Option>
      </Select>
      <span style="font-size: 14px; color: #595959;">即将到期的用户</span>
      <Button type="primary" @click="onSearch" style="margin-left: 8px;">刷新</Button>
      <span style="margin-left: auto; font-size: 14px; color: #8c8c8c;">
        已选 {{ selectedRowKeys.length }} 条
      </span>
      <Button
        type="primary"
        danger
        :disabled="selectedRowKeys.length === 0"
        @click="handleSend"
      >
        批量发送续费提醒邮件
      </Button>
    </div>

    <!-- 说明提示 -->
    <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; font-size: 13px; color: #52c41a; line-height: 1.6;">
      <strong>说明：</strong>仅向<strong>已设置邮箱且邮箱有效的用户</strong>发送邮件。未设置邮箱的用户将在发送结果中计入失败数。
    </div>

    <!-- Table -->
    <Table
      :columns="columns"
      :data-source="data"
      :row-selection="rowSelection"
      row-key="id"
      :loading="loading"
      :pagination="false"
      size="small"
    />
  </Card>

  <!-- 确认发送弹窗 -->
  <Modal
    v-model:open="confirmModalOpen"
    title="确认发送续费提醒邮件"
    :confirm-loading="sending"
    @ok="handleConfirmSend"
  >
    <div style="font-size: 15px; color: #374151; line-height: 1.8;">
      确定向以下 <strong style="color: #16a34a;">{{ selectedRowKeys.length }}</strong> 位用户发送续费提醒邮件吗？<br><br>
      <div style="background: #f0fdf4; border-radius: 8px; padding: 12px 16px; font-size: 13px; color: #15803d;">
        邮件将包含算账话术和账号资产价值的说明，帮助用户理解续费的意义。
      </div>
    </div>
  </Modal>
</template>
