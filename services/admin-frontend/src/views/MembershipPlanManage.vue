<script setup>
import { ref, onMounted } from 'vue'
import { Card, Table, Button, Modal, Form, Input, InputNumber, Switch, message, Tag } from 'ant-design-vue'
import { listMembershipPlans, saveMembershipPlan, deleteMembershipPlan } from '../api/membershipPlan.js'

const plans = ref([])
const modalOpen = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = ref({
  name: '',
  price: 0,
  originalPrice: 0,
  features: [],
  featureInput: '',
  sortOrder: 0,
  isActive: true,
})

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '现价', dataIndex: 'price', key: 'price', customRender: ({ text }) => '¥' + text },
  { title: '原价', dataIndex: 'originalPrice', key: 'originalPrice', customRender: ({ text }) => text ? '¥' + text : '-' },
  { title: '权益', dataIndex: 'featuresJson', key: 'features', width: 300 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 80 },
  { title: '操作', key: 'action', width: 160 },
]

function parseFeatures(plan) {
  try {
    return plan.featuresJson ? JSON.parse(plan.featuresJson) : []
  } catch (e) {
    return []
  }
}

async function loadData() {
  try {
    const data = await listMembershipPlans()
    plans.value = data || []
  } catch (e) {
    message.error('加载失败')
  }
}

function openModal(plan) {
  modalOpen.value = true
  editingId.value = plan ? plan.id : null
  if (plan) {
    const features = parseFeatures(plan)
    form.value = {
      name: plan.name || '',
      price: plan.price || 0,
      originalPrice: plan.originalPrice || 0,
      features: [...features],
      featureInput: '',
      sortOrder: plan.sortOrder || 0,
      isActive: plan.isActive === 1,
    }
  } else {
    form.value = {
      name: '',
      price: 0,
      originalPrice: 0,
      features: [],
      featureInput: '',
      sortOrder: 0,
      isActive: true,
    }
  }
}

function addFeature() {
  const text = form.value.featureInput.trim()
  if (!text) return
  form.value.features.push(text)
  form.value.featureInput = ''
}

function removeFeature(index) {
  form.value.features.splice(index, 1)
}

async function handleSave() {
  if (!form.value.name) {
    message.warning('请填写套餐名称')
    return
  }
  try {
    const payload = {
      id: editingId.value || undefined,
      name: form.value.name,
      price: form.value.price,
      originalPrice: form.value.originalPrice,
      featuresJson: JSON.stringify(form.value.features),
      sortOrder: form.value.sortOrder,
      isActive: form.value.isActive ? 1 : 0,
    }
    await saveMembershipPlan(payload)
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(plan) {
  Modal.confirm({
    title: '确认删除该套餐？',
    content: '删除后不可恢复。',
    async onOk() {
      try {
        await deleteMembershipPlan(plan.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
      <div style="font-size: 16px; font-weight: 500; color: #262626;">会员权益管理</div>
      <Button type="primary" @click="openModal(null)">+ 新增套餐</Button>
    </div>

    <Table :dataSource="plans" :columns="columns" rowKey="id" size="middle" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'features'">
          <div style="display: flex; flex-wrap: wrap; gap: 4px;">
            <Tag v-for="(f, i) in parseFeatures(record)" :key="i" color="green" size="small">{{ f }}</Tag>
          </div>
        </template>
        <template v-if="column.key === 'isActive'">
          <Tag :color="record.isActive === 1 ? 'green' : 'red'">{{ record.isActive === 1 ? '启用' : '禁用' }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <div style="display: flex; gap: 12px;">
            <a @click="openModal(record)">编辑</a>
            <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
          </div>
        </template>
      </template>
    </Table>
  </Card>

  <Modal v-model:open="modalOpen" :title="editingId ? '编辑套餐' : '新增套餐'" :mask-closable="false" @ok="handleSave" width="560">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="套餐名称" required>
        <Input v-model:value="form.name" placeholder="如：基础版" />
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="现价（元/月）" required>
          <InputNumber v-model:value="form.price" :min="0" :precision="2" style="width: 100%;" />
        </Form.Item>
        <Form.Item label="原价（元/月）">
          <InputNumber v-model:value="form.originalPrice" :min="0" :precision="2" style="width: 100%;" />
        </Form.Item>
      </div>
      <Form.Item label="权益列表">
        <div style="display: flex; gap: 8px; margin-bottom: 8px;">
          <Input v-model:value="form.featureInput" placeholder="输入权益后按回车添加" @pressEnter="addFeature" />
          <Button @click="addFeature">添加</Button>
        </div>
        <div style="display: flex; flex-wrap: wrap; gap: 6px;">
          <Tag v-for="(f, i) in form.features" :key="i" closable color="green" @close="removeFeature(i)">{{ f }}</Tag>
        </div>
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="排序">
          <InputNumber v-model:value="form.sortOrder" :min="0" style="width: 100%;" />
        </Form.Item>
        <Form.Item label="状态">
          <div style="padding-top: 4px;">
            <Switch v-model:checked="form.isActive" checked-children="启用" un-checked-children="禁用" />
          </div>
        </Form.Item>
      </div>
    </Form>
  </Modal>
</template>
