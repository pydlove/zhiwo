<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, Popconfirm, message, Pagination } from 'ant-design-vue'
import { listAdmins, saveAdmin, deleteAdmin } from '../api/admin.js'
import { listMembershipPlans } from '../api/membershipPlan.js'
import request from '../api/request.js'

const search = ref('')
const roleFilter = ref(undefined)
const statusFilter = ref(undefined)

const data = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const roleColorMap = {
  '超级管理员': 'purple',
  '内容管理员': 'blue',
  '运营管理员': 'green',
}

const columns = [
  { title: '管理员账号', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '角色', key: 'role', width: 120 },
  { title: '二维码', key: 'qrCodeUrl', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '最近登录', dataIndex: 'lastLogin', key: 'lastLogin', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 260 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增管理员')
const form = ref({ username: '', name: '', role: undefined, password: 'Abc123456', status: 1, qrCodeUrl: '' })
const editingId = ref(null)

// 开户链接配置
const oaModalOpen = ref(false)
const oaRecord = ref(null)
const oaPlatform = ref('公众号')
const oaCount = ref(3)
const oaMembershipPlanId = ref('')
const oaBaseUrl = ref(window.location.hostname === 'localhost' ? 'http://localhost:5174' : 'http://www.mmshuo.tech')
const oaGenerating = ref(false)
const membershipPlanOptions = ref([])
const promoLinkLoading = ref('')
const oaLinks = ref([])        // 该运营者所有开户链接
const oaLoading = ref(false)
const oaDeleting = ref('')

async function loadData() {
  try {
    const list = await listAdmins()
    data.value = list.map(a => ({
      ...a,
      statusText: a.status === 1 ? '正常' : '已禁用',
      createTime: a.createdAt ? a.createdAt.slice(0, 10) : '-',
      lastLogin: a.lastLogin ? a.lastLogin.slice(0, 16).replace('T', ' ') : '-',
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = data.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(a => (a.username || '').includes(keyword) || (a.name || '').includes(keyword))
  }
  if (roleFilter.value) {
    list = list.filter(a => a.role === roleFilter.value)
  }
  if (statusFilter.value) {
    const target = statusFilter.value === '正常' ? 1 : 0
    list = list.filter(a => a.status === target)
  }
  return list
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  currentPage.value = 1
}

function handleReset() {
  search.value = ''
  roleFilter.value = undefined
  statusFilter.value = undefined
  currentPage.value = 1
}

function handleAdd() {
  modalTitle.value = '新增管理员'
  editingId.value = null
  form.value = { username: '', name: '', role: undefined, password: 'Abc123456', status: 1, qrCodeUrl: '' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑管理员'
  editingId.value = record.id
  form.value = {
    username: record.username,
    name: record.name || '',
    role: record.role || undefined,
    password: '',
    status: record.status === 1 ? 1 : 0,
    qrCodeUrl: record.qrCodeUrl || '',
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.username || !form.value.name || !form.value.role) {
    message.warning('请填写必填项')
    return
  }
  try {
    const payload = {
      id: editingId.value || undefined,
      username: form.value.username,
      name: form.value.name,
      role: form.value.role,
      status: form.value.status,
    }
    if (!editingId.value && form.value.password) {
      payload.password = form.value.password
    }
    if (form.value.qrCodeUrl) {
      payload.qrCodeUrl = form.value.qrCodeUrl
    }
    await saveAdmin(payload)
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  if (record.role === '超级管理员') {
    message.warning('超级管理员不可删除')
    return
  }
  Modal.confirm({
    title: '确认删除该管理员？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteAdmin(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

async function resetPassword(record) {
  try {
    await saveAdmin({ id: record.id, password: 'Abc123456' })
    message.success('密码已重置为 Abc123456')
  } catch (e) {
    message.error('重置失败')
  }
}

async function toggleStatus(record) {
  if (record.role === '超级管理员') {
    message.warning('超级管理员不可禁用')
    return
  }
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await saveAdmin({ id: record.id, status: newStatus })
    message.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

async function loadMembershipPlanOptions() {
  try {
    const data = await listMembershipPlans()
    membershipPlanOptions.value = data || []
  } catch (e) {
    // ignore
  }
}

async function openOaModal(record) {
  oaRecord.value = record
  oaPlatform.value = '公众号'
  oaCount.value = 3
  oaMembershipPlanId.value = ''
  oaLinks.value = []
  oaLoading.value = true
  oaModalOpen.value = true

  // 加载该运营者所有开户链接
  try {
    const list = await request.get('/configs/open-account-link', { params: { adminId: record.id } })
    oaLinks.value = list || []
  } catch (e) {
    oaLinks.value = []
  } finally {
    oaLoading.value = false
  }
}

async function generateOaLink() {
  oaGenerating.value = true
  try {
    const payload = {
      platform: oaPlatform.value,
      count: oaCount.value,
      membershipPlanId: oaMembershipPlanId.value || undefined,
      baseUrl: oaBaseUrl.value,
      adminId: oaRecord.value.id,
    }
    await request.post('/configs/open-account-link', payload)
    message.success('链接生成成功')
    // 重新加载列表
    const list = await request.get('/configs/open-account-link', { params: { adminId: oaRecord.value.id } })
    oaLinks.value = list || []
  } catch (e) {
    message.error(e.message || '链接生成失败')
  } finally {
    oaGenerating.value = false
  }
}

async function deleteOaLink(code) {
  try {
    await request.delete('/configs/open-account-link/' + code)
    message.success('删除成功')
    oaLinks.value = oaLinks.value.filter(l => l.code !== code)
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

function copyOaLink(link) {
  if (!link) return
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(link).then(() => {
      message.success('链接已复制到剪贴板')
    }).catch(() => {
      fallbackCopyText(link)
    })
  } else {
    fallbackCopyText(link)
  }
}

function fallbackCopyText(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
  message.success('链接已复制到剪贴板')
}

async function generatePromoLink(record) {
  const key = 'promo-' + record.id
  promoLinkLoading.value = key
  try {
    const payload = {
      adminId: record.id,
      username: record.username,
      baseUrl: oaBaseUrl.value,
      targetPath: '/login',
    }
    const data = await request.post('/configs/operator-promo-link', payload)
    if (data && data.url) {
      copyText(data.url)
      message.success('推广短链已复制: ' + data.url)
    } else {
      message.error('生成短链失败')
    }
  } catch (e) {
    message.error(e.message || '生成短链失败')
  } finally {
    promoLinkLoading.value = ''
  }
}

function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).catch(() => {
      fallbackCopyText(text)
    })
  } else {
    fallbackCopyText(text)
  }
}

function copyDialogueLink(record) {
  const url = oaBaseUrl.value + '/customer-dialogue?op=' + encodeURIComponent(record.username)
  copyText(url)
  message.success('客服话术链接已复制')
}

onMounted(() => {
  loadData()
  loadMembershipPlanOptions()
})
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索管理员账号/姓名" style="width: 240px;" />
      <Select show-search v-model:value="roleFilter" placeholder="全部角色" style="min-width: 140px;" allow-clear>
        <Select.Option value="超级管理员">超级管理员</Select.Option>
        <Select.Option value="内容管理员">内容管理员</Select.Option>
        <Select.Option value="运营管理员">运营管理员</Select.Option>
      </Select>
      <Select show-search v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="正常">正常</Select.Option>
        <Select.Option value="已禁用">已禁用</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增管理员</Button>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <Tag :color="roleColorMap[record.role] || 'default'">{{ record.role }}</Tag>
        </template>
        <template v-if="column.key === 'qrCodeUrl'">
          <img v-if="record.qrCodeUrl" :src="record.qrCodeUrl" style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px; cursor: pointer;" @click="() => { window.open(record.qrCodeUrl, '_blank') }" />
          <span v-else style="color: #999;">-</span>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === 1 ? 'green' : 'red'">{{ record.statusText }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a v-if="record.role === '运营管理员'" style="margin-right: 12px; color: #1890ff;" @click="openOaModal(record)">开户链接配置</a>
          <a v-if="record.role === '运营管理员'" style="margin-right: 12px; color: #52c41a;" :style="{ opacity: promoLinkLoading === 'promo-' + record.id ? 0.6 : 1 }" @click="generatePromoLink(record)">
            {{ promoLinkLoading === 'promo-' + record.id ? '生成中...' : '复制推广短链' }}
          </a>
          <a v-if="record.role === '运营管理员'" style="margin-right: 12px; color: #fa8c16;" @click="copyDialogueLink(record)">复制客服话术链接</a>
          <a v-if="record.role !== '超级管理员'" style="margin-right: 12px;" @click="resetPassword(record)">重置密码</a>
          <a v-if="record.role !== '超级管理员'" style="margin-right: 12px;" :style="{ color: record.status === 1 ? '#f5222d' : '#1890ff' }" @click="toggleStatus(record)">
            {{ record.status === 1 ? '禁用' : '启用' }}
          </a>
          <a v-if="record.role !== '超级管理员'" style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination v-model:current="currentPage" v-model:page-size="pageSize" :total="filteredData.length" show-total />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="600">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="管理员账号" required>
          <Input v-model:value="form.username" placeholder="请输入登录账号" />
        </Form.Item>
        <Form.Item label="姓名" required>
          <Input v-model:value="form.name" placeholder="请输入真实姓名" />
        </Form.Item>
      </div>
      <Form.Item label="分配角色" required>
        <Select show-search v-model:value="form.role" placeholder="请选择角色">
          <Select.Option value="超级管理员">超级管理员</Select.Option>
          <Select.Option value="内容管理员">内容管理员</Select.Option>
          <Select.Option value="运营管理员">运营管理员</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item v-if="!editingId" label="初始密码" required>
        <Input v-model:value="form.password" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">首次登录后需强制修改</div>
      </Form.Item>
      <Form.Item label="客服二维码URL">
        <Input v-model:value="form.qrCodeUrl" placeholder="请输入二维码图片URL" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">运营管理员专属，用户端登录页将展示此二维码</div>
      </Form.Item>
      <Form.Item v-if="editingId" label="账号状态">
        <Select show-search v-model:value="form.status">
          <Select.Option :value="1">正常</Select.Option>
          <Select.Option :value="0">已禁用</Select.Option>
        </Select>
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="oaModalOpen" :title="'开户链接配置 — ' + (oaRecord?.name || oaRecord?.username || '')" :mask-closable="false" :footer="null" :width="720">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px;">
        <Form.Item label="平台" required>
          <Select show-search v-model:value="oaPlatform" style="width: 100%;">
            <Select.Option value="公众号">公众号</Select.Option>
            <Select.Option value="今日头条">今日头条</Select.Option>
            <Select.Option value="百家号">百家号</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="可选赛道数量" required>
          <Input v-model:value="oaCount" type="number" min="1" max="20" placeholder="最多可选几个赛道" />
        </Form.Item>
      </div>
      <Form.Item label="会员套餐">
        <Select show-search v-model:value="oaMembershipPlanId" placeholder="不选择则不绑定特定套餐" style="width: 100%;" allow-clear>
          <Select.Option v-for="p in membershipPlanOptions" :key="p.id" :value="p.id" :label="p.name">{{ p.name }}</Select.Option>
        </Select>
        <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">选择后，通过该链接开户的用户将自动关联此会员套餐</div>
      </Form.Item>
      <Form.Item label="基础域名">
        <Input v-model:value="oaBaseUrl" placeholder="http://www.mmshuo.tech" />
        <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">本地开发用 localhost:5174，线上用 www.mmshuo.tech</div>
      </Form.Item>
      <Form.Item>
        <Button type="primary" :loading="oaGenerating" @click="generateOaLink">生成开户链接</Button>
      </Form.Item>
    </Form>

    <!-- 已有的开户链接列表 -->
    <div v-if="oaLinks.length > 0" style="margin-top: 24px;">
      <div style="font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #262626;">已生成的开户链接</div>
      <Table :data-source="oaLinks" :pagination="false" size="small" :scroll="{ x: 620 }">
        <Table.Column title="开户链接" dataIndex="url" key="url" :width="220">
          <template #default="{ record }">
            <div style="display: flex; gap: 8px; align-items: center;">
              <Input :value="record.url" readonly size="small" style="flex: 1;" />
              <Button size="small" @click="copyOaLink(record.url)">复制</Button>
            </div>
          </template>
        </Table.Column>
        <Table.Column title="平台" dataIndex="platform" key="platform" :width="80" />
        <Table.Column title="会员套餐" dataIndex="membershipPlanName" key="membershipPlanName" :width="120">
          <template #default="{ record }">
            {{ record.membershipPlanName || '-' }}
          </template>
        </Table.Column>
        <Table.Column title="可选赛道" dataIndex="count" key="count" :width="80">
          <template #default="{ record }">
            {{ record.count }}个
          </template>
        </Table.Column>
        <Table.Column title="创建时间" dataIndex="createdAt" key="createdAt" :width="120">
          <template #default="{ record }">
            {{ record.createdAt ? record.createdAt.slice(0, 16) : '-' }}
          </template>
        </Table.Column>
        <Table.Column title="操作" key="action" :width="60">
          <template #default="{ record }">
            <Popconfirm title="确定删除该链接？" @confirm="deleteOaLink(record.code)">
              <a style="color: #ff4d4f;">删除</a>
            </Popconfirm>
          </template>
        </Table.Column>
      </Table>
    </div>
    <div v-else-if="!oaLoading" style="margin-top: 24px; color: #999; text-align: center;">
      暂无开户链接
    </div>
  </Modal>
</template>
