<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Card, Table, Button, Tag, Modal, Form, Input, message } from 'ant-design-vue'
import { listAiFlavorRules, saveAiFlavorRule, deleteAiFlavorRule } from '../api/aiFlavorRule.js'

const data = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const editingId = ref(null)
const form = ref({ ruleFrom: '', ruleTo: '' })

async function loadData() {
  loading.value = true
  try {
    const result = await listAiFlavorRules()
    data.value = result || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openModal(record) {
  if (record) {
    editingId.value = record.id
    form.value = {
      ruleFrom: record.ruleFrom || '',
      ruleTo: record.ruleTo || '',
    }
  } else {
    editingId.value = null
    form.value = { ruleFrom: '', ruleTo: '' }
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.ruleFrom || !form.value.ruleFrom.trim()) {
    message.warning('请输入替换前内容')
    return
  }
  try {
    const payload = { ...form.value, ruleFrom: form.value.ruleFrom.trim(), ruleTo: form.value.ruleTo || '' }
    if (editingId.value) {
      payload.id = editingId.value
    }
    await saveAiFlavorRule(payload)
    message.success(editingId.value ? '修改成功' : '添加成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除规则 "${record.ruleFrom}" 吗？`,
    onOk: async () => {
      try {
        await deleteAiFlavorRule(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

const columns = [
  {
    title: '替换前',
    dataIndex: 'ruleFrom',
    width: 250,
  },
  {
    title: '替换后',
    dataIndex: 'ruleTo',
    width: 250,
    customRender: ({ text }) => text || <span style="color: #999;">（删除）</span>,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 8px; justify-content: center;' }, [
        h(Button, { type: 'link', size: 'small', onClick: () => openModal(record) }, () => '编辑'),
        h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleDelete(record) }, () => '删除'),
      ])
    },
  },
]

onMounted(loadData)
</script>

<template>
  <Card title="AI去除规则管理" :bordered="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <Button type="primary" @click="() => openModal(null)">新增规则</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="data"
      row-key="id"
      :loading="loading"
      :pagination="{ pageSize: 20 }"
      size="small"
    />
  </Card>

  <Modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑规则' : '新增规则'"
    @ok="handleSave"
  >
    <Form layout="vertical" :model="form">
      <Form.Item label="替换前" required>
        <Input v-model:value="form.ruleFrom" placeholder="要替换的字符串" />
      </Form.Item>
      <Form.Item label="替换后">
        <Input v-model:value="form.ruleTo" placeholder="替换后的字符串（留空则删除）" />
      </Form.Item>
    </Form>
  </Modal>
</template>