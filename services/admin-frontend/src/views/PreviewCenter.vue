<script setup>
import { ref, onMounted } from 'vue'
import { Card } from 'ant-design-vue'
import { listTracks } from '../api/track.js'

const tracks = ref([])
const activeId = ref('')
const previewSrc = ref('/preview.html')

async function load() {
  tracks.value = await listTracks()
  if (tracks.value.length) {
    previewTrack(tracks.value[0].id)
  }
}

function previewTrack(id) {
  activeId.value = id
  previewSrc.value = '/preview.html?page=track&id=' + encodeURIComponent(id)
}

onMounted(load)
</script>

<template>
  <div>
    <div style="font-size: 14px; color: rgba(0,0,0,0.45); margin-bottom: 16px;">
      首页 / <span style="color: rgba(0,0,0,0.85); font-weight: 500;">预览中心</span>
    </div>
    <div style="display: flex; gap: 24px; align-items: flex-start;">
      <div style="width: 320px; flex-shrink: 0;">
        <Card title="选择预览内容" :body-style="{ padding: 0 }">
          <div v-if="!tracks.length" style="padding: 24px; color: #999; text-align: center;">请先添加赛道</div>
          <div
            v-for="t in tracks"
            :key="t.id"
            @click="previewTrack(t.id)"
            style="padding: 16px 24px; border-bottom: 1px solid #f0f0f0; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: all 0.3s;"
            :style="activeId === t.id ? { background: '#e6f7ff', borderLeft: '3px solid #1890ff' } : {}"
          >
            <div><span style="margin-right: 8px;">{{ t.icon }}</span><strong>{{ t.name }}</strong></div>
            <span style="display: inline-block; padding: 2px 10px; border-radius: 2px; font-size: 12px; background: #f5f5f5; color: rgba(0,0,0,0.65); border: 1px solid #d9d9d9;">赛道页</span>
          </div>
        </Card>
      </div>
      <div style="flex: 1; display: flex; justify-content: center;">
        <div style="width: 375px; height: 760px; border: 12px solid #262626; border-radius: 40px; overflow: hidden; background: #000; position: relative; box-shadow: 0 4px 12px rgba(0,0,0,0.15);">
          <iframe :src="previewSrc" style="width: 100%; height: 100%; border: none; background: #f5f5f5;" />
        </div>
      </div>
    </div>
  </div>
</template>
