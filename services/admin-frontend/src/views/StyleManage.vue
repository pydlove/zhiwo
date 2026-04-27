<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Card, Input, Select, Button, Tag, Modal, Form, message, Checkbox } from 'ant-design-vue'
import { listStyles, saveStyle, deleteStyle, exportStyles } from '../api/style.js'
import { listTracks } from '../api/track.js'
import JSZip from 'jszip'

const search = ref('')
const sceneFilter = ref(undefined)
const templates = ref([])

const defaultStyleJson = {
  fontSize: '15px',
  lineHeight: '1.8',
  paragraphSpacing: '14px',
  fontFamily: '系统默认',
  titleColor: '#262626',
  textColor: '#4a4a4a',
  quoteBg: '#e6f7ff',
  h1Size: '18px',
  h2Size: '16px',
}

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const designModalOpen = ref(false)
const designForm = ref({})
const editingId = ref(null)
const designImportFileName = ref('')
const tracks = ref([])
const exporting = ref(false)

// Selection and export
const selectedStyleIds = ref([])
const exportModalOpen = ref(false)
const exportTargetDir = ref('')
const exportModalStyleIds = ref([])

const filteredTracksByPlatform = computed(() => {
  const platform = designForm.value.platform
  if (!platform) return []
  return tracks.value.filter(t => {
    const ps = (t.platforms || '').split(/[·、,，\s]+/).filter(Boolean)
    return ps.includes(platform)
  })
})

async function loadTracks() {
  try {
    const data = await listTracks()
    tracks.value = data || []
  } catch (e) {
    tracks.value = []
  }
}

function parseStyleJson(t) {
  try {
    return t.styleJson ? JSON.parse(t.styleJson) : { ...defaultStyleJson }
  } catch (e) {
    return { ...defaultStyleJson }
  }
}

async function loadData() {
  try {
    const list = await listStyles()
    templates.value = list.map(t => ({
      ...t,
      ...parseStyleJson(t),
      isDefault: t.isDefault === 1,
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredTemplates = computed(() => {
  let list = templates.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(t => (t.name || '').includes(keyword))
  }
  if (sceneFilter.value) {
    list = list.filter(t => (t.scene || '').includes(sceneFilter.value))
  }
  return list
})

function handleSearch() {
  // computed reactive
}

function handleReset() {
  search.value = ''
  sceneFilter.value = undefined
}

async function openDesign(t) {
  designModalOpen.value = true
  editingId.value = t ? t.id : null
  const parsed = t ? parseStyleJson(t) : { ...defaultStyleJson }
  const base = t ? { ...t, ...parsed } : { name: '', scene: '', isDefault: false, ...defaultStyleJson }
  const sceneArr = base.scene ? base.scene.split(',').map(s => s.trim()).filter(Boolean) : []

  // Load tracks first, then infer platform from saved scene
  await loadTracks()
  let inferredPlatform = undefined
  if (sceneArr.length > 0 && tracks.value.length > 0) {
    const firstTrack = tracks.value.find(tr => tr.name === sceneArr[0])
    if (firstTrack && firstTrack.platforms) {
      const platformList = firstTrack.platforms.split(/[·、,，\s]+/).filter(Boolean)
      if (platformList.length > 0) inferredPlatform = platformList[0]
    }
  }

  designForm.value = {
    name: base.name || '',
    platform: inferredPlatform,
    scene: sceneArr,
    isDefault: !!base.isDefault,
    fontSize: base.fontSize || defaultStyleJson.fontSize,
    lineHeight: base.lineHeight || defaultStyleJson.lineHeight,
    paragraphSpacing: base.paragraphSpacing || defaultStyleJson.paragraphSpacing,
    fontFamily: base.fontFamily || defaultStyleJson.fontFamily,
    titleColor: base.titleColor || defaultStyleJson.titleColor,
    textColor: base.textColor || defaultStyleJson.textColor,
    quoteBg: base.quoteBg || defaultStyleJson.quoteBg,
    h1Size: base.h1Size || defaultStyleJson.h1Size,
    h2Size: base.h2Size || defaultStyleJson.h2Size,
    previewHtml: base.previewHtml || '',
  }
  designImportFileName.value = ''
}

watch(() => designForm.value.platform, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    designForm.value.scene = []
  }
})

async function saveDesign() {
  if (!designForm.value.name) {
    message.warning('请填写模板名称')
    return
  }
  try {
    const payload = {
      id: editingId.value || undefined,
      name: designForm.value.name,
      scene: Array.isArray(designForm.value.scene) ? designForm.value.scene.join(',') : designForm.value.scene,
      isDefault: designForm.value.isDefault ? 1 : 0,
      status: '已启用',
      styleJson: JSON.stringify({
        fontSize: designForm.value.fontSize,
        lineHeight: designForm.value.lineHeight,
        paragraphSpacing: designForm.value.paragraphSpacing,
        fontFamily: designForm.value.fontFamily,
        titleColor: designForm.value.titleColor,
        textColor: designForm.value.textColor,
        quoteBg: designForm.value.quoteBg,
        h1Size: designForm.value.h1Size,
        h2Size: designForm.value.h2Size,
        previewHtml: designForm.value.previewHtml || '',
      }),
    }
    await saveStyle(payload)
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    designModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function openExportModal(styleIds) {
  exportModalStyleIds.value = styleIds || []
  exportTargetDir.value = ''
  exportModalOpen.value = true
}

async function handleConfirmExport() {
  exporting.value = true
  try {
    const payload = {
      styleIds: exportModalStyleIds.value.length > 0 ? exportModalStyleIds.value : undefined,
      targetDir: exportTargetDir.value || undefined,
    }
    const result = await exportStyles(payload)
    message.success(`导出完成：成功 ${result.exportedCount} 个文件到 ${result.targetDir}`)
    if (result.missingStyles && result.missingStyles.length > 0) {
      console.warn('未找到对应 docx 的样式：', result.missingStyles)
    }
    exportModalOpen.value = false
    selectedStyleIds.value = []
  } catch (e) {
    message.error('导出失败: ' + (e?.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

function toggleSelectAll() {
  if (selectedStyleIds.value.length === filteredTemplates.value.length) {
    selectedStyleIds.value = []
  } else {
    selectedStyleIds.value = filteredTemplates.value.map(t => t.id)
  }
}

function handleDelete(t) {
  if (t.isDefault) {
    message.warning('默认模板不可删除，请先设置其他模板为默认')
    return
  }
  Modal.confirm({
    title: '确认删除该模板？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteStyle(t.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

function triggerDesignImportFile() {
  const el = document.getElementById('style-design-word-import')
  if (el) el.click()
}

async function handleDesignImportFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  if (!file.name.endsWith('.doc') && !file.name.endsWith('.docx')) {
    message.warning('请上传 .doc 或 .docx 格式的 Word 文件')
    e.target.value = ''
    return
  }
  designImportFileName.value = file.name
  if (!editingId.value && !designForm.value.name) {
    designForm.value.name = file.name.replace(/\.(docx|doc)$/i, '')
  }
  e.target.value = ''
  if (file.name.endsWith('.docx')) {
    try {
      const arrayBuffer = await file.arrayBuffer()
      const extracted = await extractDocxContent(arrayBuffer)
      designForm.value.previewHtml = extracted.previewHtml
      designForm.value.fontSize = extracted.styles.fontSize
      designForm.value.lineHeight = extracted.styles.lineHeight
      designForm.value.paragraphSpacing = extracted.styles.paragraphSpacing
      designForm.value.fontFamily = extracted.styles.fontFamily
      designForm.value.titleColor = extracted.styles.titleColor
      designForm.value.textColor = extracted.styles.textColor
      designForm.value.quoteBg = extracted.styles.quoteBg
      designForm.value.h1Size = extracted.styles.h1Size
      designForm.value.h2Size = extracted.styles.h2Size
      message.success('Word 导入成功，预览已更新')
    } catch (err) {
      console.error(err)
      message.error('Word 解析失败')
    }
  } else {
    message.warning('当前仅支持 .docx 自动解析样式')
  }
}

function twipsToPx(twips) {
  return Math.round(parseInt(twips, 10) / 20 * 1.33)
}

async function extractDocxContent(arrayBuffer) {
  const styles = { ...defaultStyleJson }
  const htmlParts = []
  try {
    const zip = await JSZip.loadAsync(arrayBuffer)
    const xmlStr = await zip.file('word/document.xml').async('string')
    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(xmlStr, 'application/xml')
    const ps = xmlDoc.getElementsByTagName('w:p')

    for (let i = 0; i < ps.length; i++) {
      const p = ps[i]
      const pPr = p.getElementsByTagName('w:pPr')[0]
      let type = 'text'
      const pStyles = {}

      if (pPr) {
        const pStyleEl = pPr.getElementsByTagName('w:pStyle')[0]
        if (pStyleEl) {
          const styleVal = (pStyleEl.getAttribute('w:val') || '').toLowerCase()
          if (styleVal === 'title' || styleVal === 'heading1' || styleVal === '1') { type = 'title' }
          else if (styleVal === 'heading2' || styleVal === '2') { type = 'h1' }
          else if (styleVal === 'heading3' || styleVal === '3' || styleVal === 'heading4' || styleVal === '4') { type = 'h2' }
          else if (styleVal.includes('quote')) { type = 'quote' }
        }

        const jcEl = pPr.getElementsByTagName('w:jc')[0]
        if (jcEl) {
          const jcVal = jcEl.getAttribute('w:val')
          if (jcVal === 'center') pStyles.textAlign = 'center'
          else if (jcVal === 'right') pStyles.textAlign = 'right'
          else if (jcVal === 'both') pStyles.textAlign = 'justify'
        }

        const spacingEl = pPr.getElementsByTagName('w:spacing')[0]
        if (spacingEl) {
          const lineVal = spacingEl.getAttribute('w:line')
          const beforeVal = spacingEl.getAttribute('w:before')
          const afterVal = spacingEl.getAttribute('w:after')
          if (lineVal) {
            const v = parseInt(lineVal, 10)
            if (!isNaN(v) && v > 0) {
              const lh = Math.round((v / 240) * 10) / 10
              pStyles.lineHeight = lh
              if (styles.lineHeight === defaultStyleJson.lineHeight && lh >= 1.0 && lh <= 3.0) {
                styles.lineHeight = String(lh)
              }
            }
          }
          if (beforeVal) {
            pStyles.marginTop = twipsToPx(beforeVal) + 'px'
          }
          if (afterVal) {
            const px = twipsToPx(afterVal)
            pStyles.marginBottom = px + 'px'
            if (styles.paragraphSpacing === defaultStyleJson.paragraphSpacing) {
              const mapped = [12, 14, 16, 20].reduce((prev, cur) => Math.abs(cur - px) < Math.abs(prev - px) ? cur : prev, 14)
              styles.paragraphSpacing = mapped + 'px'
            }
          }
        }

        const indEl = pPr.getElementsByTagName('w:ind')[0]
        if (indEl) {
          const firstLine = indEl.getAttribute('w:firstLine')
          const leftInd = indEl.getAttribute('w:left')
          if (firstLine) pStyles.textIndent = twipsToPx(firstLine) + 'px'
          if (leftInd) pStyles.paddingLeft = twipsToPx(leftInd) + 'px'
        }
      }

      const runs = p.getElementsByTagName('w:r')
      const runHtmls = []
      for (let j = 0; j < runs.length; j++) {
        const r = runs[j]
        const ts = r.getElementsByTagName('w:t')
        let runText = ''
        for (let k = 0; k < ts.length; k++) {
          let txt = ts[k].textContent || ''
          if (ts[k].getAttribute('xml:space') !== 'preserve') {
            txt = txt.replace(/^\s+|\s+$/g, '').replace(/\s+/g, ' ')
          }
          runText += txt
        }
        if (!runText) continue

        const rStyles = {}
        const rPr = r.getElementsByTagName('w:rPr')[0]
        if (rPr) {
          const sz = rPr.getElementsByTagName('w:sz')[0] || rPr.getElementsByTagName('w:szCs')[0]
          const color = rPr.getElementsByTagName('w:color')[0]
          const rFonts = rPr.getElementsByTagName('w:rFonts')[0]
          const b = rPr.getElementsByTagName('w:b')[0]
          const i = rPr.getElementsByTagName('w:i')[0]
          const u = rPr.getElementsByTagName('w:u')[0]
          const highlight = rPr.getElementsByTagName('w:highlight')[0]

          if (sz) {
            const halfPts = parseInt(sz.getAttribute('w:val'), 10)
            const px = Math.round(halfPts / 2) + 'px'
            rStyles.fontSize = px
            if (type === 'h1' && styles.h1Size === defaultStyleJson.h1Size) styles.h1Size = px
            if (type === 'h2' && styles.h2Size === defaultStyleJson.h2Size) styles.h2Size = px
            if (type === 'text' && styles.fontSize === defaultStyleJson.fontSize) styles.fontSize = px
          }
          if (color) {
            const val = color.getAttribute('w:val')
            if (val) {
              rStyles.color = '#' + val
              if ((type === 'title' || type === 'h1' || type === 'h2') && styles.titleColor === defaultStyleJson.titleColor) styles.titleColor = '#' + val
              if (type === 'text' && styles.textColor === defaultStyleJson.textColor) styles.textColor = '#' + val
            }
          }
          if (rFonts) {
            const fontVal = rFonts.getAttribute('w:eastAsia') || rFonts.getAttribute('w:ascii')
            if (fontVal) {
              rStyles.fontFamily = fontVal
              if (styles.fontFamily === defaultStyleJson.fontFamily) {
                if (fontVal.includes('宋体')) styles.fontFamily = '宋体'
                else if (fontVal.includes('雅黑') || fontVal.includes('Microsoft YaHei')) styles.fontFamily = '微软雅黑'
                else if (fontVal.includes('思源') || fontVal.includes('Source Han')) styles.fontFamily = '思源黑体'
                else styles.fontFamily = fontVal
              }
            }
          }
          if (b) rStyles.fontWeight = 'bold'
          if (i) rStyles.fontStyle = 'italic'
          if (u) {
            const uVal = u.getAttribute('w:val')
            if (!uVal || uVal !== 'none') rStyles.textDecoration = 'underline'
          }
          if (highlight) {
            const hlVal = highlight.getAttribute('w:val')
            if (hlVal && hlVal !== 'none') rStyles.backgroundColor = hlVal
          }
        }

        const styleStr = Object.entries(rStyles).map(([k, v]) => {
          const cssKey = k.replace(/[A-Z]/g, m => '-' + m.toLowerCase())
          return `${cssKey}:${v}`
        }).join(';')

        const escapedText = runText.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        if (styleStr) {
          runHtmls.push(`<span style="${styleStr}">${escapedText}</span>`)
        } else {
          runHtmls.push(escapedText)
        }
      }

      if (runHtmls.length === 0) {
        if (type === 'quote') {
          htmlParts.push(`<p style="padding:12px 12px 12px 14px;background:${styles.quoteBg || defaultStyleJson.quoteBg};border-left:3px solid #1890ff;margin:16px 0;line-height:${styles.lineHeight}"><br></p>`)
        } else {
          htmlParts.push(`<p style="margin:0 0 ${styles.paragraphSpacing || defaultStyleJson.paragraphSpacing} 0;line-height:${styles.lineHeight || defaultStyleJson.lineHeight}"><br></p>`)
        }
        continue
      }

      const pStyleStr = Object.entries(pStyles).map(([k, v]) => {
        const cssKey = k.replace(/[A-Z]/g, m => '-' + m.toLowerCase())
        return `${cssKey}:${v}`
      }).join(';')

      if (type === 'quote') {
        const quoteBg = styles.quoteBg || defaultStyleJson.quoteBg
        htmlParts.push(`<p style="padding:12px 12px 12px 14px;background:${quoteBg};border-left:3px solid #1890ff;margin:16px 0;${pStyleStr}">${runHtmls.join('')}</p>`)
      } else {
        htmlParts.push(`<p style="margin:0 0 ${styles.paragraphSpacing || defaultStyleJson.paragraphSpacing} 0;${pStyleStr}">${runHtmls.join('')}</p>`)
      }
    }
  } catch (err) {
    console.error('extractDocxContent error:', err)
  }
  return { styles, previewHtml: htmlParts.join('') }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索模板名称" style="width: 240px;" />
      <Select v-model:value="sceneFilter" placeholder="全部场景" style="min-width: 140px;" allow-clear>
        <Select.Option value="通用">通用</Select.Option>
        <Select.Option value="商务">商务</Select.Option>
        <Select.Option value="情感">情感</Select.Option>
        <Select.Option value="科技">科技</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button @click="toggleSelectAll">
        {{ selectedStyleIds.length === filteredTemplates.length && filteredTemplates.length > 0 ? '取消全选' : '全选' }}
      </Button>
      <Button @click="openExportModal(selectedStyleIds)" :disabled="selectedStyleIds.length === 0" :loading="exporting">
        批量导出 ({{ selectedStyleIds.length }})
      </Button>
      <Button type="primary" style="margin-left: auto;" @click="openDesign(null)">+ 新增模板</Button>
    </div>

    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;">
      <div v-for="t in filteredTemplates" :key="t.id" style="background: #fff; border: 1px solid #f0f0f0; border-radius: 2px; overflow: hidden; transition: all 0.2s; position: relative;">
        <div style="position: absolute; top: 8px; left: 8px; z-index: 1;">
          <input
            type="checkbox"
            :value="t.id"
            v-model="selectedStyleIds"
            style="width: 16px; height: 16px; cursor: pointer;"
          />
        </div>
        <div style="height: 160px; background: #fafafa; padding: 16px; overflow: hidden; border-bottom: 1px solid #f0f0f0;">
          <div :style="{ fontSize: '12px', fontWeight: 600, color: t.titleColor, marginBottom: '6px', fontFamily: t.fontFamily }">示例标题</div>
          <div :style="{ fontSize: '11px', lineHeight: '1.6', color: t.textColor, fontFamily: t.fontFamily }">这是一段示例正文，展示该模板的字体、行高和颜色效果...</div>
        </div>
        <div style="padding: 16px;">
          <div style="font-size: 15px; font-weight: 500; color: #262626; margin-bottom: 6px; display: flex; align-items: center; gap: 8px;">
            <div :title="t.name" style="flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ t.name }}</div>
            <Tag v-if="t.isDefault" color="gold">默认</Tag>
          </div>
          <div style="font-size: 13px; color: #8c8c8c; margin-bottom: 12px;">适用：{{ t.scene ? t.scene.split(',').join('、') : '通用' }}</div>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <Tag color="green">{{ t.status }}</Tag>
            <div style="display: flex; gap: 12px; font-size: 13px;">
              <a @click="openExportModal([t.id])">导出</a>
              <a @click="openDesign(t)">设计</a>
              <a style="color: #f5222d;" @click="handleDelete(t)">删除</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Card>

  <Modal v-model:open="designModalOpen" :title="editingId ? '编辑样式模板' : '新增样式模板'" :mask-closable="false" @ok="saveDesign" :width="1200">
    <div style="display: flex; margin-top: 12px; min-height: 500px;">
      <!-- 左侧：编辑模式显示表单，新增模式显示上传 -->
      <div style="width: 360px; padding-right: 24px; border-right: 1px solid #f0f0f0; overflow-y: auto; max-height: 70vh;">
        <Form layout="vertical">
          <Form.Item label="模板名称" required>
            <Input v-model:value="designForm.name" />
          </Form.Item>
          <Form.Item label="适用平台" required>
            <Select v-model:value="designForm.platform" placeholder="请选择平台">
              <Select.Option v-for="opt in platformOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="适用赛道">
            <div v-if="!designForm.platform" style="font-size: 13px; color: #999; padding: 8px 0;">请先选择平台</div>
            <div v-else style="display: flex; flex-direction: column; gap: 8px;">
              <label v-for="t in filteredTracksByPlatform" :key="t.id" style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
                <input type="checkbox" :value="t.name" v-model="designForm.scene" style="width: 16px; height: 16px; cursor: pointer;">
                <span>{{ t.name }}</span>
              </label>
            </div>
            <div v-if="designForm.platform && !filteredTracksByPlatform.length" style="font-size: 13px; color: #999; padding: 8px 0;">该平台下暂无赛道</div>
          </Form.Item>
          <Form.Item>
            <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <input type="checkbox" v-model="designForm.isDefault" />
              <span>设为默认模板</span>
            </label>
            <div style="font-size: 12px; color: #999; margin-top: 4px;">新用户首次创作时将自动使用该模板</div>
          </Form.Item>
        </Form>

        <!-- 上传模板区域 -->
        <div style="font-size: 13px; font-weight: 500; color: #262626; margin: 20px 0 12px; padding-top: 12px; border-top: 1px solid #f0f0f0;">上传 Word 模板</div>
        <input id="style-design-word-import" type="file" accept=".doc,.docx" style="display: none;" @change="handleDesignImportFileSelect">
        <div
          style="border: 2px dashed #d9d9d9; border-radius: 2px; padding: 40px 24px; text-align: center; cursor: pointer; transition: all 0.2s; background: #fafafa;"
          @click="triggerDesignImportFile"
        >
          <div style="font-size: 32px; color: #8c8c8c; margin-bottom: 12px;">📁</div>
          <div v-if="!designImportFileName" style="font-size: 14px; color: #262626; margin-bottom: 6px;">点击上传 Word 模板</div>
          <div v-if="!designImportFileName" style="font-size: 12px; color: #8c8c8c;">支持 .docx 格式，自动解析样式</div>
          <div v-else style="font-size: 14px; color: #262626; margin-bottom: 6px;">已选择：{{ designImportFileName }}</div>
        </div>
        <div style="font-size: 12px; color: #999; margin-top: 8px;">系统将自动解析 Word 文件中的字体、颜色、段落样式并生成对应模板配置</div>

      </div>
      <!-- 右侧：预览 -->
      <div style="flex: 1; background: #f5f5f5; padding: 24px; overflow-y: auto; max-height: 70vh;">
        <div style="font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 16px;">实时预览</div>
        <div style="background: #fff; max-width: 480px; margin: 0 auto; padding: 40px; min-height: 600px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
          <template v-if="designForm.previewHtml">
            <div v-html="designForm.previewHtml"></div>
          </template>
          <template v-else>
            <div :style="{ fontSize: '20px', fontWeight: 600, color: designForm.titleColor, marginBottom: '20px', lineHeight: '1.4', fontFamily: designForm.fontFamily }">示例文章标题</div>
            <div :style="{ fontSize: designForm.fontSize, lineHeight: designForm.lineHeight, color: designForm.textColor, marginBottom: designForm.paragraphSpacing, textAlign: 'justify', fontFamily: designForm.fontFamily }">
              这是一段示例正文，用于实时预览当前模板配置的字体、字号、颜色和行高效果。
            </div>
            <div :style="{ fontSize: designForm.h1Size, fontWeight: 600, color: designForm.titleColor, margin: '24px 0 12px', fontFamily: designForm.fontFamily }">一、一级标题示例</div>
            <div :style="{ fontSize: designForm.fontSize, lineHeight: designForm.lineHeight, color: designForm.textColor, marginBottom: designForm.paragraphSpacing, textAlign: 'justify', fontFamily: designForm.fontFamily }">
              正文内容展示段落间距和排版效果，帮助您直观感受模板在实际文章中的呈现。
            </div>
            <div :style="{ fontSize: designForm.fontSize, lineHeight: designForm.lineHeight, color: designForm.textColor, padding: '12px 12px 12px 14px', background: designForm.quoteBg, borderLeft: '3px solid #1890ff', margin: '16px 0', fontFamily: designForm.fontFamily }">
              这是一段引用文本，展示引用块的背景色和左边框样式。
            </div>
            <div :style="{ fontSize: designForm.h2Size, fontWeight: 600, color: designForm.titleColor, margin: '20px 0 10px', fontFamily: designForm.fontFamily }">1.1 二级标题示例</div>
            <div :style="{ fontSize: designForm.fontSize, lineHeight: designForm.lineHeight, color: designForm.textColor, marginBottom: designForm.paragraphSpacing, textAlign: 'justify', fontFamily: designForm.fontFamily }">
              通过调整左侧配置项，您可以实时看到预览区域的变化，确保模板符合预期。
            </div>
          </template>
        </div>
      </div>
    </div>
  </Modal>

  <Modal v-model:open="exportModalOpen" title="导出样式文件" :mask-closable="false" @ok="handleConfirmExport" :confirm-loading="exporting"
  :ok-button-props="{ disabled: exporting }"
  >
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="导出目录">
        <Input v-model:value="exportTargetDir" placeholder="留空则默认导出到后端 styles/ 目录" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">支持绝对路径，如 /Users/panyong/Desktop/styles</div>
      </Form.Item>
      <Form.Item label="导出数量">
        <div style="font-size: 13px; color: #333;">
          已选择 <strong>{{ exportModalStyleIds.length > 0 ? exportModalStyleIds.length : '全部' }}</strong> 个样式
        </div>
      </Form.Item>
    </Form>
  </Modal>

</template>
