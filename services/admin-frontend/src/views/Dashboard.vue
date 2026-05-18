<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Card, Row, Col, Table, Tag } from 'ant-design-vue'
import { getDashboardStats } from '../api/dashboard.js'

const router = useRouter()

const stats = ref([
  { label: '赛道总数', value: 0, key: 'totalTracks', color: '#1890ff', bg: '#e6f7ff' },
  { label: '博主总数', value: 0, key: 'totalBloggers', color: '#52c41a', bg: '#f6ffed' },
  { label: '文章总数', value: 0, key: 'totalPosts', color: '#fa8c16', bg: '#fff7e6' },
  { label: '注册用户', value: 0, key: 'totalUsers', color: '#722ed1', bg: '#f9f0ff' },
])

const pendingCreations = ref(0)
const todayCreations = ref(0)
const topTracks = ref([])
const platformDistribution = ref([])
const loading = ref(false)

const quickActions = [
  { label: '赛道管理', path: '/tracks', color: '#1890ff', bg: '#e6f7ff', icon: 'M3 6h18M3 12h18M3 18h18' },
  { label: '博主管理', path: '/bloggers', color: '#52c41a', bg: '#f6ffed', icon: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75' },
  { label: '用户管理', path: '/users', color: '#fa8c16', bg: '#fff7e6', icon: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z' },
  { label: '创作审阅', path: '/creation-review', color: '#722ed1', bg: '#f9f0ff', icon: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8M16 17H8M10 9H8' },
]

const trackColumns = [
  { title: '赛道名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '博主数', dataIndex: 'bloggerCount', key: 'bloggerCount', width: 100 },
  { title: '文章数', dataIndex: 'postCount', key: 'postCount', width: 100 },
  { title: '平台', dataIndex: 'platforms', key: 'platforms', ellipsis: true },
]

const platformColumns = [
  { title: '平台', dataIndex: 'platform', key: 'platform' },
  { title: '文章数', dataIndex: 'cnt', key: 'cnt', width: 100, align: 'right' },
]

async function loadStats() {
  loading.value = true
  try {
    const data = await getDashboardStats()
    stats.value = stats.value.map(s => ({
      ...s,
      value: data[s.key] || 0,
    }))
    pendingCreations.value = data.pendingCreations || 0
    todayCreations.value = data.todayCreations || 0
    topTracks.value = (data.topTracks || []).map(t => ({
      ...t,
      name: t.name || '-',
      bloggerCount: t.bloggerCount || 0,
      postCount: t.postCount || 0,
      platforms: t.platforms || '-',
    }))
    platformDistribution.value = (data.platformDistribution || []).map(p => ({
      ...p,
      platform: p.platform || '其他',
    }))
  } catch (e) {
    // fallback
  } finally {
    loading.value = false
  }
}

function formatNumber(n) {
  if (n == null) return '0'
  return n.toLocaleString()
}

onMounted(loadStats)
</script>

<template>
  <div>
    <!-- Stats Cards -->
    <Row :gutter="[16, 16]" style="margin-bottom: 24px;">
      <Col :xs="12" :sm="12" :md="6" :lg="6" v-for="s in stats" :key="s.label">
        <Card :body-style="{ padding: '0' }" style="border-radius: 2px;">
          <div style="display: flex; align-items: stretch;">
            <div style="width: 4px;" :style="{ background: s.color }"></div>
            <div style="padding: 24px; flex: 1;">
              <div style="font-size: 14px; color: #8c8c8c; margin-bottom: 8px;">{{ s.label }}</div>
              <div style="font-size: 32px; font-weight: 700; color: #262626; line-height: 1;">{{ formatNumber(s.value) }}</div>
            </div>
          </div>
        </Card>
      </Col>
    </Row>

    <!-- Pending Review Alert -->
    <Card v-if="pendingCreations > 0" :body-style="{ padding: '16px 24px' }" style="border-radius: 2px; margin-bottom: 24px; border-left: 4px solid #faad14;">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <div style="display: flex; align-items: center; gap: 12px;">
          <div style="width: 40px; height: 40px; border-radius: 50%; background: #fff7e6; color: #fa8c16; display: flex; align-items: center; justify-content: center; font-size: 18px;">!</div>
          <div>
            <div style="font-size: 15px; font-weight: 500; color: #262626;">今日有 <strong style="color: #fa8c16;">{{ pendingCreations }}</strong> 条用户创作待审阅</div>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 2px;">今日新增创作 {{ todayCreations }} 条</div>
          </div>
        </div>
        <a style="color: #1890ff; font-size: 14px; cursor: pointer; font-weight: 500;" @click="router.push('/creation-review')">去审阅 →</a>
      </div>
    </Card>

    <!-- Quick Actions -->
    <Card :body-style="{ padding: '24px' }" style="border-radius: 2px; margin-bottom: 24px;">
      <template #title>
        <span style="font-size: 16px; font-weight: 500; color: #262626;">快捷入口</span>
      </template>
      <div class="quick-actions-grid">
        <div
          v-for="q in quickActions"
          :key="q.label"
          @click="router.push(q.path)"
          style="display: flex; align-items: center; gap: 14px; padding: 18px 20px; background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; cursor: pointer; transition: all 0.2s;"
          @mouseenter="$event.currentTarget.style.borderColor = q.color; $event.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.06)'; $event.currentTarget.style.transform = 'translateY(-1px)'"
          @mouseleave="$event.currentTarget.style.borderColor = '#f0f0f0'; $event.currentTarget.style.boxShadow = 'none'; $event.currentTarget.style.transform = 'none'"
        >
          <div
            style="width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;"
            :style="{ background: q.bg }"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" :stroke="q.color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path :d="q.icon" />
            </svg>
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="font-size: 14px; font-weight: 500; color: #262626;">{{ q.label }}</div>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#bfbfbf" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 18l6-6-6-6" />
          </svg>
        </div>
      </div>
    </Card>

    <Row :gutter="[16, 16]">
      <!-- Top Tracks -->
      <Col :xs="24" :sm="24" :md="16" :lg="16">
        <Card :body-style="{ padding: '0' }" style="border-radius: 2px;">
          <template #title>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>热门赛道 TOP 5</span>
              <a style="color: #1890ff; font-size: 14px; cursor: pointer;" @click="router.push('/tracks')">查看全部 →</a>
            </div>
          </template>
          <Table :columns="trackColumns" :data-source="topTracks" :pagination="false" size="small">
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'name'">
                <span style="color: #262626; font-weight: 500;">{{ text }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'platforms'">
                <Tag size="small" style="font-size: 12px;">{{ text }}</Tag>
              </template>
            </template>
          </Table>
          <div v-if="topTracks.length === 0" style="text-align: center; padding: 40px; color: #bfbfbf; font-size: 14px;">
            暂无赛道数据
          </div>
        </Card>
      </Col>

      <!-- Platform Distribution -->
      <Col :xs="24" :sm="24" :md="8" :lg="8">
        <Card title="平台文章分布" :body-style="{ padding: '0' }" style="border-radius: 2px;">
          <Table :columns="platformColumns" :data-source="platformDistribution" :pagination="false" size="small">
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'platform'">
                <span style="color: #262626;">{{ text }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'cnt'">
                <span style="color: #262626; font-weight: 500;">{{ formatNumber(text) }}</span>
              </template>
            </template>
          </Table>
          <div v-if="platformDistribution.length === 0" style="text-align: center; padding: 40px; color: #bfbfbf; font-size: 14px;">
            暂无数据
          </div>
        </Card>
      </Col>
    </Row>
  </div>
</template>

<style scoped>
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .quick-actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
