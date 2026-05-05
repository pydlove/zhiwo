<script setup>
import { ref, onMounted } from 'vue'

const income = ref({
  yesterday: 66.24,
  dayRatio: 28.60,
  weekRatio: -5.10,
  monthTotal: 2345.60,
})

const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    // TODO: 替换为真实接口
    // const data = await request.get('/income/overview')
    // income.value = data
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="income-page">
    <!-- 顶部导航 -->
    <div class="income-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>
      <div class="nav-title">流量主</div>
      <button class="nav-action">广告管理</button>
    </div>

    <!-- 内容区 -->
    <div class="income-content">
      <!-- 收入卡片 -->
      <div class="income-card">
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="header-left">
            <span class="header-label">昨日收入（元）</span>
            <span class="header-help">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </span>
          </div>
          <div class="header-right">
            <span>明细</span>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </div>
        </div>

        <!-- 金额 -->
        <div class="card-amount">
          <span v-if="loading" class="amount-loading">--</span>
          <span v-else>{{ income.yesterday != null ? income.yesterday.toFixed(2) : '0.00' }}</span>
        </div>

        <!-- 指标 -->
        <div class="card-metrics">
          <div class="metric-item">
            <div class="metric-value" :class="{ positive: income.dayRatio >= 0, negative: income.dayRatio < 0 }">
              {{ income.dayRatio >= 0 ? '+' : '' }}{{ income.dayRatio }}%
            </div>
            <div class="metric-label">日环比</div>
          </div>
          <div class="metric-item">
            <div class="metric-value" :class="{ positive: income.weekRatio >= 0, negative: income.weekRatio < 0 }">
              {{ income.weekRatio >= 0 ? '+' : '' }}{{ income.weekRatio }}%
            </div>
            <div class="metric-label">周同比</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ income.monthTotal != null ? income.monthTotal.toFixed(2) : '0.00' }}</div>
            <div class="metric-label">本月收入（元）</div>
          </div>
        </div>

        <!-- 分割线 -->
        <div class="income-divider"></div>

        <!-- 提现入口 -->
        <div class="income-link">
          <span>如何提现收入？</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.income-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 顶部导航 */
.income-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f5f5f5;
  position: sticky;
  top: 0;
  z-index: 10;
}

.nav-back {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #262626;
  cursor: pointer;
  padding: 0;
}

.nav-title {
  font-size: 17px;
  font-weight: 600;
  color: #262626;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.nav-action {
  background: none;
  border: none;
  color: #576b95;
  font-size: 15px;
  cursor: pointer;
  padding: 4px 0;
}

/* 内容区 */
.income-content {
  padding: 8px 16px;
}

/* 收入卡片 */
.income-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 16px 8px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-label {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.header-help {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  color: #999;
  cursor: pointer;
}

.header-right svg {
  margin-top: 1px;
}

/* 金额 */
.card-amount {
  font-size: 26px;
  font-weight: 400;
  color: #00C87F;
  margin-bottom: 20px;
  letter-spacing: -0.5px;
}

.amount-loading {
  color: #bfbfbf;
}

/* 指标 */
.card-metrics {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
}

.metric-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-item:not(:last-child) {
  border-right: 1px solid #f0f0f0;
  padding-right: 12px;
  margin-right: 12px;
}

.metric-value {
  font-size: 16px;
  font-weight: 400;
  color: #262626;
}

.metric-value.positive {
  color: #262626;
}

.metric-value.negative {
  color: #262626;
}

.metric-label {
  font-size: 12px;
  color: #999;
}

/* 分割线 */
.income-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 0 -16px;
}

/* 提现入口 */
.income-link {
  padding: 14px 4px;
  font-size: 13px;
  color: #576b95;
  cursor: pointer;
}
</style>
