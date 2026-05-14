<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Card, Button, Row, Col, Statistic, Progress, Tag, Space, Tooltip } from 'ant-design-vue'
import { getTrackStats } from '../api/titleLibrary.js'

const router = useRouter()

const trackStatsLoading = ref(false)
const trackStatsData = ref([])

const trackStatsTotal = computed(() => trackStatsData.value.reduce((sum, item) => sum + (item.total || 0), 0))
const trackStatsUsed = computed(() => trackStatsData.value.reduce((sum, item) => sum + (item.usedCount || 0), 0))
const trackStatsUnused = computed(() => trackStatsData.value.reduce((sum, item) => sum + (item.unusedCount || 0), 0))

async function loadTrackStats() {
  trackStatsLoading.value = true
  try {
    const data = await getTrackStats()
    trackStatsData.value = (data || [])
      .filter(item => (item.total || 0) > 0)
      .sort((a, b) => (b.total || 0) - (a.total || 0))
  } catch (e) {
    console.error('loadTrackStats error:', e)
  } finally {
    trackStatsLoading.value = false
  }
}

function goBack() {
  router.push('/title-library')
}

function goToTitleLibrary(trackId) {
  router.push({ path: '/title-library', query: { trackId } })
}

function getSubscriberColor(count) {
  if (!count || count <= 0) return null
  const colors = ['#ffbb96', '#ff9c6e', '#ff7a45', '#fa541c', '#d4380d', '#ad2102', '#871400']
  return colors[Math.min(count, colors.length) - 1]
}

onMounted(() => {
  loadTrackStats()
})
</script>

<template>
  <div>
    <Card title="赛道统计视图" :bordered="false">
      <template #extra>
        <Button @click="goBack">返回标题库</Button>
      </template>

      <Row :gutter="[16, 16]" style="margin-bottom: 24px;">
        <Col :xs="24" :sm="8">
          <Card :bodyStyle="{ padding: '20px', textAlign: 'center' }">
            <Statistic title="总标题数" :value="trackStatsTotal" :valueStyle="{ color: '#1890ff', fontSize: '28px' }" />
          </Card>
        </Col>
        <Col :xs="24" :sm="8">
          <Card :bodyStyle="{ padding: '20px', textAlign: 'center' }">
            <Statistic title="已使用" :value="trackStatsUsed" :valueStyle="{ color: '#52c41a', fontSize: '28px' }" />
          </Card>
        </Col>
        <Col :xs="24" :sm="8">
          <Card :bodyStyle="{ padding: '20px', textAlign: 'center' }">
            <Statistic title="未使用" :value="trackStatsUnused" :valueStyle="{ color: '#faad14', fontSize: '28px' }" />
          </Card>
        </Col>
      </Row>

      <Row :gutter="[16, 16]">
        <Col v-for="item in trackStatsData" :key="item.trackId || item.trackName" :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
          <Card
            size="small"
            class="track-card"
            @click="goToTitleLibrary(item.trackId)"
          >
            <div class="track-card-header">
              <div class="track-card-title">
                <span class="track-name">{{ item.trackName || item.trackId || '未知赛道' }}</span>
                <span v-if="item.platforms" class="track-platform">{{ item.platforms }}</span>
              </div>
              <Tooltip v-if="item.subscriberCount > 0" :title="`已有 ${item.subscriberCount} 位用户订阅`">
                <Tag :color="getSubscriberColor(item.subscriberCount)" size="small">已订阅 {{ item.subscriberCount }}</Tag>
              </Tooltip>
            </div>

            <div class="track-card-total">
              <span class="total-number">{{ item.total || 0 }}</span>
              <span class="total-label">条标题</span>
            </div>

            <Progress
              :percent="item.total ? Math.round(((item.usedCount || 0) / item.total) * 100) : 0"
              :show-info="false"
              stroke-color="#52c41a"
              trail-color="#f5f5f5"
              size="small"
            />

            <div class="track-card-footer">
              <Space :size="4">
                <Tag color="success" size="small">已使用 {{ item.usedCount || 0 }}</Tag>
                <Tag color="warning" size="small">未使用 {{ item.unusedCount || 0 }}</Tag>
              </Space>
              <span class="track-percent">
                {{ item.total ? Math.round(((item.usedCount || 0) / item.total) * 100) : 0 }}%
              </span>
            </div>
          </Card>
        </Col>
      </Row>
    </Card>
  </div>
</template>

<style scoped>
.track-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid #f0f0f0;
}
.track-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.12);
  border-color: #d9d9d9;
}
.track-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 8px;
}
.track-card-title {
  flex: 1;
  min-width: 0;
}
.track-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}
.track-platform {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
  display: block;
}
.track-card-total {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}
.total-number {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  line-height: 1;
}
.total-label {
  font-size: 12px;
  color: #8c8c8c;
}
.track-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.track-percent {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
}
</style>
