<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Card, Input, Button, Table, Tag, Pagination, Space, message } from 'ant-design-vue'
import { SearchOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import { listUserHomogeneity } from '../api/titleLibrary.js'

const router = useRouter()

const data = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const sortField = ref('homogeneityScore')
const sortOrder = ref('desc')

const columns = [
  {
    title: '用户',
    key: 'user',
    width: 200,
    ellipsis: true,
  },
  {
    title: '同质化程度',
    key: 'homogeneityScore',
    width: 120,
    align: 'center',
    sorter: true,
  },
  {
    title: '历史文章数',
    key: 'historyCount',
    width: 110,
    align: 'center',
    sorter: true,
  },
  {
    title: '计算时间',
    key: 'calculatedAt',
    width: 170,
    align: 'center',
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    align: 'center',
  },
]

const stats = computed(() => {
  const list = data.value
  const totalUsers = list.length
  const highRisk = list.filter(i => (i.homogeneityScore || 0) >= 50).length
  const mediumRisk = list.filter(i => {
    const s = i.homogeneityScore || 0
    return s >= 25 && s < 50
  }).length
  const avgScore = totalUsers > 0
    ? Math.round(list.reduce((sum, i) => sum + (i.homogeneityScore || 0), 0) / totalUsers)
    : 0
  return { totalUsers, highRisk, mediumRisk, avgScore }
})

function getScoreColor(score) {
  if (score >= 50) return 'red'
  if (score >= 25) return 'orange'
  return 'green'
}

function getScoreLabel(score) {
  if (score >= 50) return '高同质化'
  if (score >= 25) return '中等同质化'
  return '低同质化'
}

async function loadData() {
  loading.value = true
  try {
    const res = await listUserHomogeneity({
      keyword: keyword.value,
      sortField: sortField.value,
      sortOrder: sortOrder.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    data.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  loadData()
}

function onPageChange(page) {
  currentPage.value = page
  loadData()
}

function onTableChange(pagination, filters, sorter) {
  if (sorter && sorter.field) {
    sortField.value = sorter.field
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  }
  loadData()
}

function goToUserHistory(userId) {
  router.push('/title-match?userId=' + encodeURIComponent(userId))
}

onMounted(loadData)
</script>

<template>
  <div>
    <!-- 统计卡片 -->
    <div class="stats-row" style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px;">
      <Card :body-style="{ padding: '20px 24px' }">
        <div style="font-size: 14px; color: #8c8c8c; margin-bottom: 8px;">总用户</div>
        <div style="font-size: 28px; font-weight: 700; color: #262626;">{{ stats.totalUsers }}</div>
      </Card>
      <Card :body-style="{ padding: '20px 24px' }">
        <div style="font-size: 14px; color: #8c8c8c; margin-bottom: 8px;">高同质化（≥50%）</div>
        <div style="font-size: 28px; font-weight: 700; color: #ff4d4f;">{{ stats.highRisk }}</div>
      </Card>
      <Card :body-style="{ padding: '20px 24px' }">
        <div style="font-size: 14px; color: #8c8c8c; margin-bottom: 8px;">中等同质化（25%~50%）</div>
        <div style="font-size: 28px; font-weight: 700; color: #fa8c16;">{{ stats.mediumRisk }}</div>
      </Card>
      <Card :body-style="{ padding: '20px 24px' }">
        <div style="font-size: 14px; color: #8c8c8c; margin-bottom: 8px;">平均同质化分数</div>
        <div style="font-size: 28px; font-weight: 700; color: #1890ff;">{{ stats.avgScore }}%</div>
      </Card>
    </div>

    <!-- 搜索栏 -->
    <Card :body-style="{ padding: '16px 24px' }" style="margin-bottom: 16px;">
      <Space>
        <Input
          v-model:value="keyword"
          placeholder="搜索用户名、昵称..."
          style="width: 280px;"
          @pressEnter="onSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </Input>
        <Button type="primary" @click="onSearch">查询</Button>
        <Button @click="keyword = ''; onSearch()">重置</Button>
      </Space>
    </Card>

    <!-- 数据表格 -->
    <Card>
      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="false"
        row-key="userId"
        size="small"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <div>
              <div style="font-weight: 500; color: #262626;">{{ record.username || '-' }}</div>
              <div v-if="record.nickName || record.wxName" style="font-size: 12px; color: #8c8c8c;">
                {{ record.nickName }} {{ record.wxName ? '(' + record.wxName + ')' : '' }}
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'homogeneityScore'">
            <Tag :color="getScoreColor(record.homogeneityScore)" style="font-size: 13px; font-weight: 500;">
              {{ record.homogeneityScore || 0 }}%
            </Tag>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 2px;">{{ getScoreLabel(record.homogeneityScore) }}</div>
          </template>
          <template v-else-if="column.key === 'historyCount'">
            <span style="color: #262626;">{{ record.historyCount || 0 }}</span>
          </template>
          <template v-else-if="column.key === 'calculatedAt'">
            <span style="color: #8c8c8c; font-size: 13px;">{{ record.calculatedAt || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <Button type="link" size="small" @click="goToUserHistory(record.userId)">
              查看历史
            </Button>
          </template>
        </template>
      </Table>

      <div v-if="total > 0" style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <Pagination
          v-model:current="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          show-size-changer
          :page-size-options="['10', '20', '50', '100']"
          show-total
          @change="onPageChange"
        />
      </div>

      <div v-if="!loading && data.length === 0" style="text-align: center; padding: 60px; color: #bfbfbf;">
        暂无数据
      </div>
    </Card>
  </div>
</template>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
