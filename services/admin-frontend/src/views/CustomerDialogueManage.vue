<script setup>
import { ref, onMounted, h, computed, watch } from 'vue'
import { Card, Table, Button, Modal, Form, Input, message, Image, Space, Tooltip, Tabs } from 'ant-design-vue'
import { CopyOutlined, EditOutlined, DeleteOutlined, PlusOutlined, PictureOutlined } from '@ant-design/icons-vue'
import {
  listCustomerDialogues,
  listCategories,
  saveCustomerDialogue,
  deleteCustomerDialogue,
} from '../api/customerDialogue.js'
import request from '../api/request.js'

const TabPane = Tabs.TabPane

const data = ref([])
const allData = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const loading = ref(false)
const modalOpen = ref(false)
const editingId = ref(null)
const form = ref({
  category: '',
  question: '',
  reply: '',
  imageUrl: '',
  sortOrder: 0,
})
const uploadLoading = ref(false)
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')

const tableData = computed(() => {
  if (activeCategory.value === '全部') return allData.value
  return allData.value.filter(item => item.category === activeCategory.value)
})

async function loadData() {
  loading.value = true
  try {
    const [dialogues, cats] = await Promise.all([
      listCustomerDialogues(),
      listCategories(),
    ])
    allData.value = dialogues || []
    categories.value = cats || []
    // 确保当前 activeCategory 在分类列表中
    if (activeCategory.value !== '全部' && !categories.value.includes(activeCategory.value)) {
      activeCategory.value = '全部'
    }
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function getNextSortOrder(category) {
  const list = category && category !== '全部'
    ? allData.value.filter(item => item.category === category)
    : allData.value
  if (!list.length) return 0
  const max = Math.max(...list.map(item => item.sortOrder ?? 0))
  return max + 1
}

function openModal(record) {
  if (record) {
    editingId.value = record.id
    form.value = {
      category: record.category || '默认',
      question: record.question || '',
      reply: record.reply || '',
      imageUrl: record.imageUrl || '',
      sortOrder: record.sortOrder ?? 0,
    }
  } else {
    editingId.value = null
    const cat = activeCategory.value === '全部' ? (categories.value[0] || '默认') : activeCategory.value
    form.value = {
      category: cat,
      question: '',
      reply: '',
      imageUrl: '',
      sortOrder: getNextSortOrder(cat),
    }
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.category || !form.value.category.trim()) {
    message.warning('请输入分类')
    return
  }
  if (!form.value.question || !form.value.question.trim()) {
    message.warning('请输入提问/场景')
    return
  }
  if (!form.value.reply || !form.value.reply.trim()) {
    message.warning('请输入回复内容')
    return
  }
  try {
    const payload = {
      category: form.value.category.trim(),
      question: form.value.question.trim(),
      reply: form.value.reply.trim(),
      imageUrl: form.value.imageUrl || null,
      sortOrder: Number(form.value.sortOrder) || 0,
    }
    if (editingId.value) {
      payload.id = editingId.value
    }
    await saveCustomerDialogue(payload)
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
    content: `确定删除这条客服对话吗？`,
    onOk: async () => {
      try {
        await deleteCustomerDialogue(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

async function handleCopy(text, label = '内容') {
  try {
    await navigator.clipboard.writeText(text)
    message.success(`${label}已复制到剪贴板`)
  } catch (e) {
    const input = document.createElement('textarea')
    input.value = text
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    message.success(`${label}已复制到剪贴板`)
  }
}

async function handleUploadImage(e) {
  const file = e.target.files[0]
  if (!file) return
  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    form.value.imageUrl = res
    message.success('图片上传成功')
  } catch (err) {
    message.error('图片上传失败')
  } finally {
    uploadLoading.value = false
    e.target.value = ''
  }
}

function handleRemoveImage() {
  form.value.imageUrl = ''
}

function handlePreviewImage(url) {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
}

function handleCopyImageUrl(url) {
  const fullUrl = url.startsWith('http') ? url : window.location.origin + url
  handleCopy(fullUrl, '图片地址')
}

const columns = [
  {
    title: '排序',
    dataIndex: 'sortOrder',
    width: 80,
    align: 'center',
    sorter: (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0),
  },
  {
    title: '提问/场景',
    dataIndex: 'question',
    width: 200,
    ellipsis: true,
  },
  {
    title: '回复',
    dataIndex: 'reply',
    width: 300,
    ellipsis: true,
    customRender: ({ text, record }) => {
      return h('div', {
        style: 'display: flex; align-items: center; gap: 8px;',
      }, [
        h('span', {
          style: 'flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;',
          title: text,
        }, text),
        h(Tooltip, { title: '复制回复' }, () =>
          h(Button, {
            type: 'link',
            size: 'small',
            style: 'padding: 0;',
            onClick: (e) => {
              e.stopPropagation()
              handleCopy(text, '回复')
            },
          }, () => h(CopyOutlined))
        ),
      ])
    },
  },
  {
    title: '图片',
    dataIndex: 'imageUrl',
    width: 120,
    align: 'center',
    customRender: ({ text }) => {
      if (!text) {
        return h('span', { style: 'color: #999;' }, '-')
      }
      const fullUrl = text.startsWith('http') ? text : window.location.origin + text
      return h('div', {
        style: 'display: flex; align-items: center; justify-content: center; gap: 8px;',
      }, [
        h(Image, {
          src: fullUrl,
          width: 50,
          height: 50,
          style: 'object-fit: cover; border-radius: 4px; cursor: pointer;',
          preview: false,
          onClick: () => handlePreviewImage(fullUrl),
        }),
        h(Tooltip, { title: '复制图片地址' }, () =>
          h(Button, {
            type: 'link',
            size: 'small',
            style: 'padding: 0;',
            onClick: (e) => {
              e.stopPropagation()
              handleCopyImageUrl(text)
            },
          }, () => h(CopyOutlined))
        ),
      ])
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    align: 'center',
    customRender: ({ record }) => {
      return h(Space, { size: 'small' }, [
        h(Button, {
          type: 'link',
          size: 'small',
          onClick: () => openModal(record),
        }, () => [h(EditOutlined), ' 编辑']),
        h(Button, {
          type: 'link',
          size: 'small',
          danger: true,
          onClick: () => handleDelete(record),
        }, () => [h(DeleteOutlined), ' 删除']),
      ])
    },
  },
]

onMounted(loadData)
</script>

<template>
  <Card title="客服对话" :bordered="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <Button type="primary" @click="() => openModal(null)">
        <PlusOutlined /> 新增对话
      </Button>
    </div>

    <Tabs v-model:activeKey="activeCategory" type="card">
      <TabPane key="全部" tab="全部">
        <Table
          :columns="columns"
          :data-source="tableData"
          row-key="id"
          :loading="loading"
          :pagination="false"
          size="small"
        />
      </TabPane>
      <TabPane v-for="cat in categories" :key="cat" :tab="cat">
        <Table
          :columns="columns"
          :data-source="tableData"
          row-key="id"
          :loading="loading"
          :pagination="false"
          size="small"
        />
      </TabPane>
    </Tabs>
  </Card>

  <!-- 新增/编辑弹窗 -->
  <Modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑客服对话' : '新增客服对话'"
    @ok="handleSave"
    width="600"
  >
    <Form layout="vertical" :model="form" style="margin-top: 12px;">
      <Form.Item label="分类" required>
        <Input
          v-model:value="form.category"
          placeholder="请输入分类名称，如：售前咨询、售后处理、常见问题"
        />
      </Form.Item>

      <Form.Item label="提问/场景" required>
        <Input
          v-model:value="form.question"
          placeholder="请输入提问或场景描述，用于告诉客服在什么情况下使用这条回复"
        />
      </Form.Item>

      <Form.Item label="回复内容" required>
        <Input.TextArea
          v-model:value="form.reply"
          :rows="4"
          placeholder="请输入客服回复内容"
        />
      </Form.Item>

      <Form.Item label="排序数字">
        <Input
          v-model:value="form.sortOrder"
          type="number"
          placeholder="数字越小排序越靠前，默认为0"
        />
      </Form.Item>

      <Form.Item label="图片">
        <div v-if="form.imageUrl" style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
          <Image
            :src="form.imageUrl.startsWith('http') ? form.imageUrl : (form.imageUrl)"
            :width="100"
            :height="100"
            style="object-fit: cover; border-radius: 4px;"
          />
          <Space>
            <Button size="small" @click="handleRemoveImage">移除图片</Button>
            <Button size="small" @click="handleCopyImageUrl(form.imageUrl)">
              <CopyOutlined /> 复制图片地址
            </Button>
          </Space>
        </div>
        <div v-else>
          <Button :loading="uploadLoading">
            <PictureOutlined /> 上传图片
            <input
              type="file"
              accept="image/*"
              style="position: absolute; left: 0; top: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer;"
              @change="handleUploadImage"
            />
          </Button>
          <span style="color: #999; font-size: 12px; margin-left: 8px;">支持 JPG、PNG、GIF 格式</span>
        </div>
      </Form.Item>
    </Form>
  </Modal>

  <!-- 图片预览 -->
  <Modal
    v-model:open="imagePreviewVisible"
    :footer="null"
    width="auto"
    :centered="true"
  >
    <img :src="previewImageUrl" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto;" />
  </Modal>
</template>
