<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { Input, Select, Button, Modal, Form, message, Popconfirm, Empty, Upload, Image as AImage, Pagination } from 'ant-design-vue'
import { InboxOutlined, DeleteOutlined, FileImageOutlined, EditOutlined } from '@ant-design/icons-vue'
import { listImages, uploadImage, deleteImage, updateImage, downloadImages } from '../api/imageLibrary.js'
import { listTracks } from '../api/track.js'

const images = ref([])
const tracks = ref([])
const loading = ref(false)
const keyword = ref('')
const categoryFilter = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)

const uploadModalOpen = ref(false)
const uploadFiles = ref([])
const uploadCategories = ref([])
const uploading = ref(false)

// 编辑裁剪
const editModalOpen = ref(false)
const editingImage = ref(null)
const editCategories = ref([])
const editSaving = ref(false)

// 下载图片
const downloadModalOpen = ref(false)
const downloadSource = ref('picsum')
const downloadCount = ref(15)
const downloadTrackId = ref(null)
const downloading = ref(false)

const sourceOptions = [
  { label: 'Picsum (稳定随机)', value: 'picsum' },
  { label: 'Unsplash (按主题)', value: 'unsplash' },
  { label: 'Mixed (混合)', value: 'mixed' },
  { label: '必应壁纸 (国内CDN)', value: 'bing' },
  { label: '百度图片 (中文内容)', value: 'baidu' },
]

const sourceCanvasRef = ref(null)
const previewCanvasRef = ref(null)
const cropRect = ref({ x: 0, y: 0, w: 0, h: 0 })
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const imgObj = ref(null)
const scaleFactor = ref(1)

function resetCrop() {
  cropRect.value = { x: 0, y: 0, w: 0, h: 0 }
  isDragging.value = false
  imgObj.value = null
  scaleFactor.value = 1
}

const trackOptions = computed(() => {
  return tracks.value.map(t => ({
    label: t.platforms ? `${t.name}（${t.platforms}）` : t.name,
    value: t.id
  }))
})

function getTrackName(trackId) {
  const t = tracks.value.find(tr => String(tr.id) === String(trackId))
  if (!t) return trackId
  return t.platforms ? `${t.name}（${t.platforms}）` : t.name
}

function getTrackNames(categoriesJson) {
  if (!categoriesJson) return []
  try {
    const arr = JSON.parse(categoriesJson)
    if (Array.isArray(arr)) {
      return arr.map(id => getTrackName(id)).filter(Boolean)
    }
  } catch (e) {
    // ignore
  }
  return []
}

function fetchImages() {
  loading.value = true
  const params = { page: currentPage.value, pageSize: pageSize.value }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (categoryFilter.value && categoryFilter.value.length > 0) {
    params.category = JSON.stringify(categoryFilter.value)
  }
  listImages(params)
    .then(res => {
      if (res && Array.isArray(res.list)) {
        images.value = res.list
        totalCount.value = res.total || 0
      } else {
        images.value = res || []
        totalCount.value = res?.length || 0
      }
    })
    .catch(() => {
      message.error('加载图片库失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function onPageChange(page) {
  currentPage.value = page
  fetchImages()
}

function onSearch() {
  currentPage.value = 1
  fetchImages()
}

function onCategoryChange() {
  currentPage.value = 1
  fetchImages()
}

function openUploadModal() {
  uploadModalOpen.value = true
  uploadFiles.value = []
  uploadCategories.value = []
}

function openDownloadModal() {
  downloadModalOpen.value = true
  downloadSource.value = 'picsum'
  downloadCount.value = 15
  downloadTrackId.value = null
}

async function handleDownloadImages() {
  if (!downloadTrackId.value) {
    message.warning('请选择赛道')
    return
  }
  if (!downloadCount.value || downloadCount.value < 1 || downloadCount.value > 50) {
    message.warning('数量必须在 1-50 之间')
    return
  }
  downloading.value = true
  try {
    const formData = new FormData()
    formData.append('source', downloadSource.value)
    formData.append('count', String(downloadCount.value))
    formData.append('trackId', downloadTrackId.value)
    const res = await downloadImages(formData)
    const importedCount = res?.importedCount || 0
    const failedCount = res?.failedCount || 0
    let msg = `下载导入完成：成功 ${importedCount} 张`
    if (failedCount > 0) {
      msg += `，失败 ${failedCount} 张`
    }
    message.success(msg)
    downloadModalOpen.value = false
    fetchImages()
  } catch (e) {
    message.error(e?.message || '下载失败')
  } finally {
    downloading.value = false
  }
}

function handleAddFiles(info) {
  const newFiles = info.fileList.filter(f => {
    const isImage = (f.type || '').startsWith('image/')
    const notExists = !uploadFiles.value.some(uf => uf.name === f.name && uf.size === f.size)
    return isImage && notExists
  })
  uploadFiles.value.push(...newFiles)
}

function removeFile(file) {
  uploadFiles.value = uploadFiles.value.filter(f => f !== file)
}

async function handleUpload() {
  if (uploadFiles.value.length === 0) {
    message.warning('请选择图片文件')
    return
  }
  const formData = new FormData()
  for (const file of uploadFiles.value) {
    formData.append('files', file.originFileObj || file)
  }
  if (uploadCategories.value && uploadCategories.value.length > 0) {
    formData.append('categories', JSON.stringify(uploadCategories.value))
  }
  uploading.value = true
  try {
    const res = await uploadImage(formData)
    const uploadedCount = res?.uploadedCount || 0
    const skippedCount = res?.skippedCount || 0
    const skippedNames = res?.skippedNames || []
    let msg = `成功上传 ${uploadedCount} 张图片`
    if (skippedCount > 0) {
      msg += `，跳过 ${skippedCount} 张重复图片（${skippedNames.join('、')}）`
    }
    message.success(msg)
    uploadModalOpen.value = false
    uploadFiles.value = []
    fetchImages()
  } catch (e) {
    message.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(record) {
  try {
    await deleteImage(record.id)
    message.success('删除成功')
    fetchImages()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

// ========== 编辑裁剪 ==========
function openEditModal(record) {
  editingImage.value = record
  editCategories.value = []
  if (record.categories) {
    try {
      const arr = JSON.parse(record.categories)
      if (Array.isArray(arr)) editCategories.value = arr
    } catch (e) { /* ignore */ }
  }
  editModalOpen.value = true
  resetCrop()
  nextTick(() => loadImageToCanvas(record.url))
}

function loadImageToCanvas(url) {
  const canvas = sourceCanvasRef.value
  console.log('[loadImageToCanvas] canvas=', canvas, 'url=', url)
  if (!canvas) {
    setTimeout(() => loadImageToCanvas(url), 50)
    return
  }
  const ctx = canvas.getContext('2d')
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    console.log('[img.onload] width=', img.width, 'height=', img.height)
    if (!img.width || !img.height || img.width <= 0 || img.height <= 0) {
      message.error('图片尺寸无效')
      return
    }
    imgObj.value = img
    const maxW = 520
    const maxH = 340
    const ratio = Math.min(maxW / img.width, maxH / img.height, 1)
    scaleFactor.value = ratio
    canvas.width = img.width * ratio
    canvas.height = img.height * ratio
    console.log('[img.onload] canvas size=', canvas.width, 'x', canvas.height)
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

    const cw = canvas.width * 0.8
    const ch = canvas.height * 0.8
    cropRect.value = {
      x: (canvas.width - cw) / 2,
      y: (canvas.height - ch) / 2,
      w: cw,
      h: ch,
    }
    drawCropOverlay()
    drawPreview()
  }
  img.onerror = (e) => {
    console.error('[img.onerror]', e)
    message.error('图片加载失败')
  }
  img.src = url
}

function drawCropOverlay() {
  const canvas = sourceCanvasRef.value
  if (!canvas || !imgObj.value) return
  const ctx = canvas.getContext('2d')
  const { x, y, w, h } = cropRect.value
  // 重绘原图
  ctx.drawImage(imgObj.value, 0, 0, canvas.width, canvas.height)
  // 半透明遮罩
  ctx.fillStyle = 'rgba(0, 0, 0, 0.4)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  // 清除裁剪区域（显示原图对应区域）
  ctx.clearRect(x, y, w, h)
  ctx.drawImage(
    imgObj.value,
    x / scaleFactor.value, y / scaleFactor.value,
    w / scaleFactor.value, h / scaleFactor.value,
    x, y, w, h
  )
  // 绘制边框
  ctx.strokeStyle = '#1890ff'
  ctx.lineWidth = 2
  ctx.strokeRect(x, y, w, h)
  // 绘制四个角点
  const r = 4
  ctx.fillStyle = '#1890ff'
  ;[[x, y], [x + w, y], [x, y + h], [x + w, y + h]].forEach(([cx, cy]) => {
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    ctx.fill()
  })
}

function drawPreview() {
  const pCanvas = previewCanvasRef.value
  if (!pCanvas || !imgObj.value) return
  const { x, y, w, h } = cropRect.value
  if (w <= 0 || h <= 0) return
  // 从原始图片绘制，避免 sourceCanvas 上的遮罩/边框伪影
  const srcX = x / scaleFactor.value
  const srcY = y / scaleFactor.value
  const srcW = w / scaleFactor.value
  const srcH = h / scaleFactor.value

  // 计算预览尺寸（保持比例，最大 280x200）
  const maxPW = 280
  const maxPH = 200
  const pr = Math.min(maxPW / w, maxPH / h, 1)
  pCanvas.width = w * pr
  pCanvas.height = h * pr
  const ctx = pCanvas.getContext('2d')
  ctx.clearRect(0, 0, pCanvas.width, pCanvas.height)
  ctx.drawImage(imgObj.value, srcX, srcY, srcW, srcH, 0, 0, pCanvas.width, pCanvas.height)
}

function getCanvasCoords(e) {
  const canvas = sourceCanvasRef.value
  if (!canvas) return { x: 0, y: 0 }
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height
  return {
    x: (e.clientX - rect.left) * scaleX,
    y: (e.clientY - rect.top) * scaleY,
  }
}

function onMouseDown(e) {
  const canvas = sourceCanvasRef.value
  if (!canvas) return
  const { x, y } = getCanvasCoords(e)
  const cr = cropRect.value
  const handleSize = 10
  // 判断是否点在角上（用于调整大小）
  const corners = [
    { key: 'tl', x: cr.x, y: cr.y },
    { key: 'tr', x: cr.x + cr.w, y: cr.y },
    { key: 'bl', x: cr.x, y: cr.y + cr.h },
    { key: 'br', x: cr.x + cr.w, y: cr.y + cr.h },
  ]
  for (const c of corners) {
    if (Math.abs(x - c.x) < handleSize && Math.abs(y - c.y) < handleSize) {
      isDragging.value = c.key
      dragStart.value = { x, y }
      return
    }
  }
  // 判断是否点在框内（用于移动）
  if (x >= cr.x && x <= cr.x + cr.w && y >= cr.y && y <= cr.y + cr.h) {
    isDragging.value = 'move'
    dragStart.value = { x, y }
    return
  }
  // 否则创建新裁剪区域
  isDragging.value = 'new'
  dragStart.value = { x, y }
  cropRect.value = { x, y, w: 0, h: 0 }
}

function onMouseMove(e) {
  if (!isDragging.value) return
  const canvas = sourceCanvasRef.value
  if (!canvas) return
  const { x, y } = getCanvasCoords(e)
  const ds = dragStart.value
  const cr = cropRect.value

  if (isDragging.value === 'new') {
    const nx = Math.min(ds.x, x)
    const ny = Math.min(ds.y, y)
    let nw = Math.abs(x - ds.x)
    let nh = Math.abs(y - ds.y)
    // 限制在画布内
    if (nx < 0) { nw += nx; }
    if (ny < 0) { nh += ny; }
    if (nx + nw > canvas.width) { nw = canvas.width - Math.max(0, nx); }
    if (ny + nh > canvas.height) { nh = canvas.height - Math.max(0, ny); }
    cropRect.value = {
      x: Math.max(0, nx),
      y: Math.max(0, ny),
      w: Math.max(0, nw),
      h: Math.max(0, nh),
    }
  } else if (isDragging.value === 'move') {
    const dx = x - ds.x
    const dy = y - ds.y
    let nx = cr.x + dx
    let ny = cr.y + dy
    nx = Math.max(0, Math.min(nx, canvas.width - cr.w))
    ny = Math.max(0, Math.min(ny, canvas.height - cr.h))
    cropRect.value = { ...cr, x: nx, y: ny }
    dragStart.value = { x, y }
  } else if (isDragging.value === 'tl') {
    let nx = Math.min(x, cr.x + cr.w)
    let ny = Math.min(y, cr.y + cr.h)
    nx = Math.max(0, nx)
    ny = Math.max(0, ny)
    cropRect.value = {
      x: nx,
      y: ny,
      w: cr.x + cr.w - nx,
      h: cr.y + cr.h - ny,
    }
  } else if (isDragging.value === 'tr') {
    let ny = Math.min(y, cr.y + cr.h)
    ny = Math.max(0, ny)
    let nw = Math.max(0, x - cr.x)
    nw = Math.min(nw, canvas.width - cr.x)
    cropRect.value = {
      x: cr.x,
      y: ny,
      w: nw,
      h: cr.y + cr.h - ny,
    }
  } else if (isDragging.value === 'bl') {
    let nx = Math.min(x, cr.x + cr.w)
    nx = Math.max(0, nx)
    let nh = Math.max(0, y - cr.y)
    nh = Math.min(nh, canvas.height - cr.y)
    cropRect.value = {
      x: nx,
      y: cr.y,
      w: cr.x + cr.w - nx,
      h: nh,
    }
  } else if (isDragging.value === 'br') {
    let nw = Math.max(0, x - cr.x)
    let nh = Math.max(0, y - cr.y)
    nw = Math.min(nw, canvas.width - cr.x)
    nh = Math.min(nh, canvas.height - cr.y)
    cropRect.value = {
      x: cr.x,
      y: cr.y,
      w: nw,
      h: nh,
    }
  }
  drawCropOverlay()
  drawPreview()
}

function onMouseUp() {
  isDragging.value = false
}

async function handleEditSave() {
  if (!imgObj.value || cropRect.value.w <= 0 || cropRect.value.h <= 0) {
    message.warning('请先选择裁剪区域')
    return
  }
  editSaving.value = true
  try {
    const { x, y, w, h } = cropRect.value
    const srcX = x / scaleFactor.value
    const srcY = y / scaleFactor.value
    const srcW = w / scaleFactor.value
    const srcH = h / scaleFactor.value

    const exportCanvas = document.createElement('canvas')
    exportCanvas.width = Math.round(srcW)
    exportCanvas.height = Math.round(srcH)
    const ctx = exportCanvas.getContext('2d')
    ctx.drawImage(imgObj.value, srcX, srcY, srcW, srcH, 0, 0, exportCanvas.width, exportCanvas.height)

    const originalName = (editingImage.value && editingImage.value.name) || 'cropped.png'
    const extMatch = originalName.toLowerCase().match(/\.(jpe?g|png|webp)$/)
    const ext = extMatch ? extMatch[0] : '.png'
    const mime = ext === '.jpg' || ext === '.jpeg' ? 'image/jpeg' : 'image/png'
    const blob = await new Promise((resolve, reject) => {
      try {
        exportCanvas.toBlob((b) => {
          if (b) resolve(b)
          else reject(new Error('裁剪失败'))
        }, mime)
      } catch (err) {
        reject(err)
      }
    })
    if (!blob) {
      message.error('裁剪失败')
      return
    }
    const outName = originalName.replace(/\.[^.]+$/, '') + (ext === '.jpeg' ? '.jpg' : ext)
    const file = new File([blob], outName, { type: mime })
    const formData = new FormData()
    formData.append('file', file)
    if (editCategories.value && editCategories.value.length > 0) {
      formData.append('categories', JSON.stringify(editCategories.value))
    }
    await updateImage(editingImage.value.id, formData)
    message.success('更新成功')
    editModalOpen.value = false
    fetchImages()
  } catch (e) {
    message.error(e?.message || '更新失败')
  } finally {
    editSaving.value = false
  }
}

onMounted(() => {
  fetchImages()
  listTracks().then(res => {
    tracks.value = res || []
  }).catch(() => {})
})
</script>

<template>
  <div>
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
      <Input
        v-model:value="keyword"
        placeholder="搜索图片名称"
        style="width: 240px"
        @pressEnter="onSearch"
      />
      <Select
        show-search
        v-model:value="categoryFilter"
        mode="multiple"
        :options="trackOptions"
        placeholder="按赛道筛选"
        style="width: 260px"
        allowClear
        @change="onCategoryChange"
      />
      <Button type="primary" @click="onSearch">搜索</Button>
      <Button style="margin-left: auto;" type="primary" @click="openUploadModal">上传图片</Button>
      <Button @click="openDownloadModal">下载图片</Button>
    </div>

    <div v-if="loading" style="text-align: center; padding: 40px;">
      加载中...
    </div>

    <div v-else-if="images.length === 0" style="padding: 40px;">
      <Empty description="暂无图片" />
    </div>

    <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px;">
      <div
        v-for="img in images"
        :key="img.id"
        style="border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; background: #fff; position: relative;"
      >
        <div style="width: 100%; aspect-ratio: 16/10; overflow: hidden; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
          <AImage :src="img.url" :preview="true" style="width: 100%; height: 100%; object-fit: cover;" />
        </div>
        <div style="padding: 10px 12px;">
          <div style="font-size: 13px; color: #333; margin-bottom: 6px; word-break: break-all; line-height: 1.4;">
            {{ img.name }}
          </div>
          <div style="display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px;">
            <span
              v-for="(name, idx) in getTrackNames(img.categories)"
              :key="idx"
              style="font-size: 11px; background: #e6f7ff; color: #1890ff; padding: 1px 6px; border-radius: 4px;"
            >
              {{ name }}
            </span>
            <span v-if="!img.categories || getTrackNames(img.categories).length === 0" style="font-size: 11px; color: #999;">
              未分类
            </span>
          </div>
          <div style="display: flex; justify-content: flex-end; gap: 8px;">
            <Button type="link" size="small" @click="() => openEditModal(img)">
              <EditOutlined /> 编辑
            </Button>
            <Popconfirm
              title="确定删除该图片吗？"
              @confirm="() => handleDelete(img)"
            >
              <Button type="link" size="small" danger>删除</Button>
            </Popconfirm>
          </div>
        </div>
      </div>
    </div>

    <div v-if="totalCount > 0" style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="totalCount"
        :pageSizeOptions="['20', '40', '60']"
        show-size-changer
        @change="onPageChange"
        @showSizeChange="onPageChange"
      />
    </div>

    <Modal
      v-model:open="uploadModalOpen"
      title="上传图片"
      :maskClosable="false"
      :confirm-loading="uploading"
      @ok="handleUpload"
      width="520"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <Form.Item label="选择图片" required>
          <div class="upload-area">
            <div class="upload-dragger-inner">
              <p class="upload-drag-icon">
                <InboxOutlined style="font-size: 48px; color: #40a9ff;" />
              </p>
              <p class="upload-text">点击选择文件或文件夹上传</p>
              <p class="upload-hint">
                <Upload
                  :showUploadList="false"
                  :multiple="true"
                  :beforeUpload="() => false"
                  @change="handleAddFiles"
                >
                  <a>选择文件</a>
                </Upload>
                <span class="upload-divider">|</span>
                <Upload
                  :showUploadList="false"
                  directory
                  :beforeUpload="() => false"
                  @change="handleAddFiles"
                >
                  <a>选择文件夹</a>
                </Upload>
              </p>
            </div>
          </div>

          <div v-if="uploadFiles.length > 0" class="upload-file-list">
            <div v-for="file in uploadFiles" :key="file.uid" class="upload-file-item">
              <FileImageOutlined style="color: #1890ff;" />
              <span class="upload-file-name">{{ file.name }}</span>
              <Button type="text" size="small" danger @click="removeFile(file)">
                <DeleteOutlined />
              </Button>
            </div>
          </div>
        </Form.Item>
        <Form.Item label="归属赛道（类别）">
          <Select
            show-search
            v-model:value="uploadCategories"
            mode="multiple"
            :options="trackOptions"
            placeholder="请选择赛道，可多选"
            style="width: 100%;"
            allowClear
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            不选则图片不归属任何赛道
          </div>
        </Form.Item>
      </Form>
    </Modal>

    <!-- 编辑裁剪弹窗 -->
    <Modal
      v-model:open="editModalOpen"
      title="编辑图片"
      :maskClosable="false"
      :confirm-loading="editSaving"
      @ok="handleEditSave"
      width="600"
    >
      <Form layout="vertical" style="margin-top: 12px;"
      >
        <Form.Item label="裁剪区域" required>
          <div style="display: flex; gap: 16px; flex-wrap: wrap;"
          >
            <div>
              <div style="font-size: 12px; color: #999; margin-bottom: 4px;"
              >拖动蓝色框调整裁剪区域，拖拽边框角可调整大小</div
              >
              <canvas
                ref="sourceCanvasRef"
                style="border: 1px solid #f0f0f0; border-radius: 4px; cursor: crosshair; max-width: 100%;"
                @mousedown="onMouseDown"
                @mousemove="onMouseMove"
                @mouseup="onMouseUp"
                @mouseleave="onMouseUp"
              />
            </div>
            <div>
              <div style="font-size: 12px; color: #999; margin-bottom: 4px;"
              >裁剪预览</div
              >
              <canvas
                ref="previewCanvasRef"
                style="border: 1px solid #f0f0f0; border-radius: 4px; max-width: 100%;"
              />
            </div>
          </div>
        </Form.Item>
        <Form.Item label="归属赛道（类别）"
        >
          <Select
            show-search
            v-model:value="editCategories"
            mode="multiple"
            :options="trackOptions"
            placeholder="请选择赛道，可多选"
            style="width: 100%;"
            allowClear
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 下载图片弹窗 -->
    <Modal
      v-model:open="downloadModalOpen"
      title="下载图片"
      :maskClosable="false"
      :confirm-loading="downloading"
      @ok="handleDownloadImages"
      width="480"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <Form.Item label="图片来源" required>
          <Select
            v-model:value="downloadSource"
            :options="sourceOptions"
            style="width: 100%;"
          />
        </Form.Item>
        <Form.Item label="赛道" required>
          <Select
            show-search
            v-model:value="downloadTrackId"
            :options="trackOptions"
            placeholder="请选择赛道"
            style="width: 100%;"
            allowClear
          />
        </Form.Item>
        <Form.Item label="下载数量" required>
          <Input
            v-model:value="downloadCount"
            type="number"
            min="1"
            max="50"
            style="width: 100%;"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            默认15张，范围 1-50
          </div>
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  padding: 24px;
  text-align: center;
  transition: border-color 0.3s;
  cursor: pointer;
}
.upload-area:hover {
  border-color: #40a9ff;
}
.upload-drag-icon {
  margin-bottom: 12px;
}
.upload-text {
  font-size: 15px;
  color: #262626;
  margin-bottom: 8px;
}
.upload-hint {
  font-size: 13px;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.upload-divider {
  color: #d9d9d9;
}
.upload-file-list {
  margin-top: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 8px 12px;
  background: #fff;
  max-height: 200px;
  overflow-y: auto;
}
.upload-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
}
.upload-file-item:last-child {
  border-bottom: none;
}
.upload-file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #262626;
}
</style>
