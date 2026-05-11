<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Tag, Modal, message } from 'ant-design-vue'
import { listCreations, reviewCreation } from '../api/creation.js'
import { listTracks } from '../api/track.js'
import { listUsers } from '../api/user.js'

const keyword = ref('')
const userFilter = ref(undefined)
const trackFilter = ref(undefined)
const dateFilter = ref('')

const creations = ref([])
const tracks = ref([])
const users = ref([])

async function loadData() {
  try {
    const [cList, tList, uList] = await Promise.all([listCreations(), listTracks(), listUsers()])
    creations.value = cList.map(c => ({
      ...c,
      user: c.userName || c.userId,
      track: c.trackName || c.trackId,
      time: c.createdAt ? c.createdAt.slice(11, 16) : '-',
      date: c.createdAt ? c.createdAt.slice(0, 10) : '-',
    }))
    tracks.value = tList
    users.value = uList
  } catch (e) {
    message.error('加载失败')
  }
}

const filtered = computed(() => {
  let list = creations.value
  if (keyword.value) {
    list = list.filter(c => c.title.includes(keyword.value))
  }
  if (userFilter.value) {
    list = list.filter(c => c.user === userFilter.value)
  }
  if (trackFilter.value) {
    list = list.filter(c => c.track === trackFilter.value)
  }
  if (dateFilter.value) {
    list = list.filter(c => c.date === dateFilter.value)
  }
  return list
})

const groups = computed(() => {
  const map = {}
  filtered.value.forEach(c => {
    if (!map[c.date]) map[c.date] = []
    map[c.date].push(c)
  })
  const today = new Date().toISOString().slice(0, 10)
  const yest = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
  return Object.keys(map).sort((a, b) => b.localeCompare(a)).map(date => {
    let header = date
    if (date === today) header = '今天 ' + date
    else if (date === yest) header = '昨天 ' + date
    return { header, items: map[date] }
  })
})

const modalOpen = ref(false)
const current = ref(null)

function openModal(record) {
  current.value = record
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  current.value = null
}

async function confirmReview() {
  if (!current.value) return
  try {
    await reviewCreation(current.value.id)
    message.success('已标记为审阅')
    closeModal()
    loadData()
  } catch (e) {
    message.error('审阅失败')
  }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center; flex-wrap: wrap;">
      <Input v-model:value="keyword" placeholder="搜索创作标题" style="width: 240px;" />
      <Select show-search v-model:value="userFilter" placeholder="全部用户" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="u in users" :key="u.id" :value="u.username" :label="u.username">{{ u.username }}</Select.Option>
      </Select>
      <Select show-search v-model:value="trackFilter" placeholder="全部赛道" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.name" :label="t.name">{{ t.name }}</Select.Option>
      </Select>
      <Input type="date" v-model:value="dateFilter" style="width: 140px;" />
      <Button type="primary">查询</Button>
      <Button @click="keyword = ''; userFilter = undefined; trackFilter = undefined; dateFilter = ''">重置</Button>
    </div>

    <div v-for="g in groups" :key="g.header" style="margin-bottom: 24px;">
      <div style="font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0;">{{ g.header }}</div>
      <div
        v-for="item in g.items"
        :key="item.id"
        style="display: flex; align-items: center; justify-content: space-between; padding: 16px 0; border-bottom: 1px solid #f0f0f0;"
      >
        <div style="flex: 1;">
          <div style="font-size: 15px; color: #262626; margin-bottom: 6px;">
            {{ item.title }}
            <Tag v-if="item.reviewed === 1" color="green">已审阅</Tag>
          </div>
          <div style="font-size: 13px; color: #8c8c8c;">用户：{{ item.user }} · 赛道：{{ item.track }} · 生成时间：{{ item.time }}</div>
        </div>
        <div style="display: flex; gap: 16px; align-items: center;">
          <Button v-if="item.reviewed !== 1" type="link" @click="openModal(item)">审阅</Button>
          <Button type="link" @click="openModal(item)">预览</Button>
        </div>
      </div>
    </div>
  </Card>

  <Modal v-model:open="modalOpen" title="审阅创作" width="600" :mask-closable="false" :footer="null">
    <div v-if="current">
      <div style="display: flex; gap: 24px; font-size: 13px; color: #595959; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0;">
        <span>用户：{{ current.user }}</span>
        <span>赛道：{{ current.track }}</span>
        <span>生成时间：{{ current.date }} {{ current.time }}</span>
      </div>
      <div style="font-size: 14px; line-height: 1.8; color: #262626; padding: 16px; background: #fafafa; border-radius: 2px; min-height: 200px;">
        <h1 style="font-size: 18px; font-weight: 600; margin: 16px 0 12px;">{{ current.title }}</h1>
        <div v-html="current.content"></div>
      </div>
      <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px;">
        <Button @click="closeModal">关闭</Button>
        <Button v-if="current.reviewed !== 1" type="primary" @click="confirmReview">标记为已审阅</Button>
      </div>
    </div>
  </Modal>
</template>
