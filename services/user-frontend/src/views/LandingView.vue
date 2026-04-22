<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useViewport } from '../composables/useViewport.js'
import { listMembershipPlans } from '../api/membershipPlan.js'

const router = useRouter()
const { isMobile } = useViewport()
const membershipPlans = ref([])

function goLogin() {
  router.push('/login')
}

function goHome() {
  router.push('/app/home')
}

function scrollToMembership() {
  const el = document.getElementById('membership-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

const painPoints = [
  { icon: '✍️', title: '写不出文章', desc: '面对空白文档，半天憋不出一个字' },
  { icon: '😰', title: '质量不稳定', desc: '写出来的文章阅读量惨淡，毫无水花' },
  { icon: '⏰', title: '时间成本高', desc: '一篇原创文章需要耗费数小时甚至数天' },
  { icon: '💸', title: '投入产出低', desc: '付出大量精力，收益却微乎其微' },
]

const features = [
  {
    icon: '',
    title: '订阅赛道',
    desc: '选择你感兴趣的赛道，平台每日精准推送该领域爆款文章',
  },
  {
    icon: '',
    title: '一键导入',
    desc: '下载平台推送的优质文章，直接导入到你的公众号后台',
  },
  {
    icon: '',
    title: '绝对原创',
    desc: '每篇文章均经过AI深度改写，确保原创度，无需担心侵权',
  },
  {
    icon: '',
    title: '持续收益',
    desc: '一次投入，长期受益，每年获取稳定的额外收入来源',
  },
]

const stats = [
  { num: '36+', label: '热门赛道' },
  { num: '200+', label: '头部博主' },
  { num: '100%', label: '原创保障' },
  { num: '365', label: '全年推送' },
]

function parseFeatures(plan) {
  try {
    return plan.featuresJson ? JSON.parse(plan.featuresJson) : []
  } catch (e) {
    return []
  }
}

async function loadMembershipPlans() {
  try {
    const data = await listMembershipPlans()
    membershipPlans.value = data || []
  } catch (e) {
    membershipPlans.value = []
  }
}

onMounted(() => {
  loadMembershipPlans()
})
</script>

<template>
  <div class="landing">
    <!-- Navbar -->
    <nav class="navbar">
      <div class="nav-brand" @click="goHome">
        <img src="https://foruda.gitee.com/images/1776834561924666968/e0f84414_8060302.png" class="nav-logo-img" alt="logo">
        <span class="nav-title">公众号创作助手</span>
      </div>
      <div style="display: flex; align-items: center; gap: 16px;">
        <a class="nav-link" @click="scrollToMembership">会员权益</a>
        <button class="login-btn" @click="goLogin">登录</button>
      </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          全新智能创作方式
        </div>
        <h1 class="hero-title">
          不用写文章<br />
          <span class="hero-highlight">也能做好公众号</span>
        </h1>
        <p class="hero-subtitle">
          以前大家不想做公众号，大多是因为自己不愿意写文章，
          亦或者说是文章写不好，还有就是付出和收入不能成正比。
          现在，一切都变了。
        </p>
        <div class="hero-actions">
          <button class="btn-primary" @click="goLogin">
            立即开始
            <span class="btn-arrow">→</span>
          </button>
          <button class="btn-secondary" @click="goHome">
            了解更多
          </button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="floating-card card-1">
          <div class="card-icon">
            <img src="../assets/images/baok.png" style="width:22px; height: 22px; object-fit: cover;" alt="card-icon-1">
          </div>
          <div class="card-text">
            <div class="card-title">自动推荐</div>
            <div class="card-desc">订阅赛道，爆款自动推荐</div>
          </div>
        </div>
        <div class="floating-card card-2">
          <div class="card-icon">
            <img src="../assets/images/data.png" style="width:22px; height: 22px; object-fit: cover;" alt="card-icon-1">
          </div>
          <div class="card-text">
            <div class="card-title">数据洞察</div>
            <div class="card-desc">200+博主实时追踪</div>
          </div>
        </div>
        <div class="floating-card card-3">
          <div class="card-icon">
            <img src="../assets/images/ai-zn.png" style="width:22px; height: 22px; object-fit: cover;" alt="card-icon-1">
          </div>
          <div class="card-text">
            <div class="card-title">AI推送</div>
            <div class="card-desc">客服把关质量+AI推送文章</div>
          </div>
        </div>
        <div class="hero-orb"></div>
      </div>
    </section>

    <!-- Pain Points Section -->
    <section class="section pain-section">
      <div class="section-container">
        <div class="section-header">
          <span class="section-label">01 / 痛点</span>
          <h2 class="section-title">
            为什么很多人<br />想做公众号却坚持不下去？
          </h2>
        </div>
        <div class="pain-grid">
          <div v-for="(item, i) in painPoints" :key="i" class="pain-card">
            <div class="pain-icon">{{ item.icon }}</div>
            <h3 class="pain-title">{{ item.title }}</h3>
            <p class="pain-desc">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Solution Section -->
    <section class="section solution-section">
      <div class="section-container">
        <div class="section-header">
          <span class="section-label">02 / 解决方案</span>
          <h2 class="section-title">
            公众号创作助手<br />让创作变得前所未有的简单
          </h2>
        </div>
        <div class="solution-body">
          <div class="solution-text">
            <p class="solution-lead">
              平台每天会根据你订阅的赛道，给你推送爆款文章。
              你可以下载导入到公众号，绝对原创。
            </p>
            <p class="solution-detail">
              这相当于是一次投资。你可以通过这种方式，
              每年获取一笔不小的额外收入。不需要你亲自创作，
              不需要你绞尽脑汁想选题，不需要你熬夜赶稿子。
              你只需要订阅赛道，接收推送，导入发布。
            </p>
            <div class="solution-quote">
              <div class="quote-line"></div>
              <p>从"想写却不会写"到"不用写也能发"，<br />这是你公众号运营方式的根本性转变。</p>
            </div>
          </div>
          <div class="solution-visual">
            <div class="solution-phone">
              <div class="phone-screen">
                <div class="phone-header">
                  <span class="phone-dot red"></span>
                  <span class="phone-dot yellow"></span>
                  <span class="phone-dot green"></span>
                </div>
                <div class="phone-content">
                  <div class="phone-item">
                    <div class="phone-item-title">🔥 情感赛道 · 今日爆款</div>
                    <div class="phone-item-desc">《人到中年，才真正读懂了这3句话》</div>
                    <div class="phone-item-meta">阅读 10w+ · 点赞 2.3k</div>
                  </div>
                  <div class="phone-item">
                    <div class="phone-item-title">🔥 职场赛道 · 今日爆款</div>
                    <div class="phone-item-desc">《真正厉害的人，都懂得这一点》</div>
                    <div class="phone-item-meta">阅读 8.5w+ · 点赞 1.8k</div>
                  </div>
                  <div class="phone-item">
                    <div class="phone-item-title">🔥 育儿赛道 · 今日爆款</div>
                    <div class="phone-item-desc">《孩子越管越叛逆？问题出在这里》</div>
                    <div class="phone-item-meta">阅读 12w+ · 点赞 3.1k</div>
                  </div>
                  <div class="phone-action">下载导入 →</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="section features-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">03 / 核心功能</span>
          <h2 class="section-title">四大能力，全面赋能</h2>
        </div>
        <div class="features-grid">
          <div v-for="(item, i) in features" :key="i" class="feature-card">
            <!--<div class="feature-icon">{{ item.icon }}</div>-->
            <h3 class="feature-title">{{ item.title }}</h3>
            <p class="feature-desc">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Membership Section -->
    <section id="membership-section" class="section membership-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">04 / 会员权益</span>
          <h2 class="section-title">选择适合你的套餐</h2>
          <p class="section-subtitle">多种档位，满足不同需求，按月订阅，随时可退</p>
        </div>
        <div class="membership-grid">
          <div v-for="(plan, i) in membershipPlans" :key="plan.id" class="membership-card" :class="{ popular: i === 2 }">
            <div v-if="i === 2" class="popular-badge">最受欢迎</div>
            <div class="membership-name">{{ plan.name }}</div>
            <div class="membership-price-row">
              <span class="membership-price">¥{{ plan.price }}</span>
              <span class="membership-unit">/ 月</span>
            </div>
            <div v-if="plan.originalPrice && plan.originalPrice > 0" class="membership-original">
              原价 ¥{{ plan.originalPrice }}
            </div>
            <div class="membership-divider"></div>
            <ul class="membership-features">
              <li v-for="(f, j) in parseFeatures(plan)" :key="j">
                <span class="feature-check">✓</span>
                {{ f }}
              </li>
            </ul>
            <button class="membership-btn" :class="{ primary: i === 2 }" @click="goLogin">
              {{ i === 2 ? '立即开通' : '选择此套餐' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="section stats-section">
      <div class="stats-bg"></div>
      <div class="section-container">
        <div class="stats-grid">
          <div v-for="(item, i) in stats" :key="i" class="stat-item">
            <div class="stat-num">{{ item.num }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="section cta-section">
      <div class="section-container">
        <div class="cta-box">
          <h2 class="cta-title">开始你的公众号之旅</h2>
          <p class="cta-desc">
            这相当于是一次投资。你可以通过这种方式，
            每年获取一笔不小的额外收入。
          </p>
          <button class="btn-primary large" @click="goLogin">
            立即登录使用
            <span class="btn-arrow">→</span>
          </button>
          <p class="cta-note">管理员开通账号后即可登录使用</p>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-brand">
          <img src="../assets/images/logo-green.png" class="footer-logo-img" alt="logo">
          <span>公众号创作助手</span>
        </div>
        <p class="footer-copy">让每个人都能轻松运营公众号</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ========== WeChat Color System ========== */
/* Primary: #07C160  |  Light: #E6F7ED  |  Dark: #06AD56 */

/* ========== Base ========== */
.landing {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: #1e293b;
  background: #fff;
  overflow-x: hidden;
}

/* ========== Navbar ========== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.nav-logo-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: contain;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.login-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid #07C160;
  background: transparent;
  color: #07C160;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.login-btn:hover {
  background: #07C160;
  color: #fff;
}

.nav-link {
  font-size: 14px;
  color: #4b5563;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
  font-weight: 500;
}

.nav-link:hover {
  color: #07C160;
}

/* ========== Hero ========== */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 120px 48px 80px;
  background: linear-gradient(180deg, #f0fdf4 0%, #fff 100%);
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 50% at 15% 30%, rgba(7, 193, 96, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse 50% 40% at 85% 70%, rgba(7, 193, 96, 0.04) 0%, transparent 50%);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 600px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(7, 193, 96, 0.1);
  color: #07C160;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 28px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #07C160;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.15;
  color: #1e293b;
  margin-bottom: 24px;
  letter-spacing: -0.02em;
}

.hero-highlight {
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 17px;
  line-height: 1.7;
  color: #64748b;
  margin-bottom: 36px;
  max-width: 480px;
}

.hero-actions {
  display: flex;
  gap: 14px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 20px rgba(7, 193, 96, 0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(7, 193, 96, 0.4);
}

.btn-primary.large {
  padding: 16px 36px;
  font-size: 16px;
}

.btn-arrow {
  transition: transform 0.2s;
}

.btn-primary:hover .btn-arrow {
  transform: translateX(3px);
}

.btn-secondary {
  padding: 14px 28px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  color: #1e293b;
}

/* Hero Visual */
.hero-visual {
  position: absolute;
  right: 48px;
  top: 50%;
  transform: translateY(-50%);
  width: 420px;
  height: 500px;
}

.hero-orb {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(7, 193, 96, 0.12) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: orbFloat 6s ease-in-out infinite;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -55%) scale(1.05); }
}

.floating-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
  animation: cardFloat 5s ease-in-out infinite;
}

.card-1 {
  top: 60px;
  left: 0;
  animation-delay: 0s;
}

.card-2 {
  top: 200px;
  right: 0;
  animation-delay: 1.5s;
}

.card-3 {
  bottom: 80px;
  left: 40px;
  animation-delay: 3s;
}

@keyframes cardFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(7, 193, 96, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.card-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

/* ========== Section Common ========== */
.section {
  padding: 100px 48px;
}

.section-container {
  max-width: 1100px;
  margin: 0 auto;
}

.section-header {
  margin-bottom: 60px;
}

.section-header.center {
  text-align: center;
}

.section-label {
  display: inline-block;
  font-size: 13px;
  font-weight: 500;
  color: #07C160;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 16px;
}

.section-title {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.25;
  color: #1e293b;
  letter-spacing: -0.01em;
}

/* ========== Pain Section ========== */
.pain-section {
  background: #f8fafc;
}

.pain-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.pain-card {
  padding: 32px 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.pain-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  border-color: #07C160;
}

.pain-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.pain-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.pain-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}

/* ========== Solution Section ========== */
.solution-section {
  background: #fff;
}

.solution-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: center;
}

.solution-text {
  max-width: 480px;
}

.solution-lead {
  font-size: 20px;
  font-weight: 500;
  line-height: 1.7;
  color: #1e293b;
  margin-bottom: 20px;
}

.solution-detail {
  font-size: 15px;
  line-height: 1.8;
  color: #64748b;
  margin-bottom: 32px;
}

.solution-quote {
  display: flex;
  gap: 16px;
}

.quote-line {
  width: 3px;
  border-radius: 2px;
  background: linear-gradient(180deg, #07C160 0%, #06AD56 100%);
  flex-shrink: 0;
}

.solution-quote p {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.7;
  color: #334155;
  font-style: italic;
}

/* Solution Visual - Phone */
.solution-visual {
  display: flex;
  justify-content: center;
}

.solution-phone {
  width: 300px;
  padding: 8px;
  border-radius: 36px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  box-shadow: 0 24px 60px rgba(7, 193, 96, 0.12);
}

.phone-screen {
  border-radius: 28px;
  background: #fff;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.phone-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.phone-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.phone-dot.red { background: #ef4444; }
.phone-dot.yellow { background: #f59e0b; }
.phone-dot.green { background: #07C160; }

.phone-content {
  padding: 16px;
}

.phone-item {
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  margin-bottom: 10px;
  border: 1px solid #f1f5f9;
}

.phone-item-title {
  font-size: 12px;
  font-weight: 500;
  color: #07C160;
  margin-bottom: 6px;
}

.phone-item-desc {
  font-size: 13px;
  color: #1e293b;
  margin-bottom: 4px;
  line-height: 1.4;
}

.phone-item-meta {
  font-size: 11px;
  color: #9ca3af;
}

.phone-action {
  margin-top: 14px;
  padding: 12px;
  border-radius: 10px;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  cursor: pointer;
}

/* ========== Features Section ========== */
.features-section {
  background: #f8fafc;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature-card {
  padding: 36px 28px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-bottom: 20px;
}

.feature-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 10px;
}

.feature-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}

/* ========== Membership Section ========== */
.membership-section {
  background: #f8fafc;
}

.section-subtitle {
  font-size: 17px;
  color: #64748b;
  margin-top: 12px;
}

.membership-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.membership-card {
  position: relative;
  padding: 32px 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.membership-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
}

.membership-card.popular {
  border-color: #07C160;
  box-shadow: 0 8px 30px rgba(7, 193, 96, 0.12);
}

.popular-badge {
  position: absolute;
  top: -1px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 16px;
  border-radius: 0 0 8px 8px;
}

.membership-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  margin-top: 8px;
}

.membership-price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}

.membership-price {
  font-size: 36px;
  font-weight: 800;
  color: #07C160;
  line-height: 1;
}

.membership-unit {
  font-size: 14px;
  color: #9ca3af;
}

.membership-original {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: line-through;
  margin-bottom: 16px;
}

.membership-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 16px 0;
}

.membership-features {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
  flex: 1;
}

.membership-features li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #4b5563;
  padding: 6px 0;
  line-height: 1.5;
}

.feature-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(7, 193, 96, 0.1);
  color: #07C160;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 1px;
}

.membership-btn {
  width: 100%;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.membership-btn:hover {
  border-color: #07C160;
  color: #07C160;
}

.membership-btn.primary {
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  border: none;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.3);
}

.membership-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(7, 193, 96, 0.4);
}

/* ========== Stats Section ========== */
.stats-section {
  position: relative;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  padding: 80px 48px;
}

.stats-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 60% 80% at 50% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
}

.stats-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 44px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

/* ========== CTA Section ========== */
.cta-section {
  background: #fff;
  padding: 100px 48px;
}

.cta-box {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
  padding: 64px 48px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f0fdf4 0%, #f6fef9 100%);
  border: 1px solid #bbf7d0;
}

.cta-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
}

.cta-desc {
  font-size: 16px;
  line-height: 1.7;
  color: #64748b;
  margin-bottom: 32px;
}

.cta-note {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 20px;
}

/* ========== Footer ========== */
.footer {
  background: #f8fafc;
  padding: 40px 48px;
  border-top: 1px solid #e2e8f0;
}

.footer-content {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.footer-logo-img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: contain;
}

.footer-copy {
  font-size: 13px;
  color: #9ca3af;
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
  .navbar {
    padding: 0 20px;
  }

  .hero {
    flex-direction: column;
    padding: 100px 24px 60px;
    text-align: center;
  }

  .hero-content {
    max-width: 100%;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-actions {
    justify-content: center;
  }

  .hero-visual {
    position: relative;
    right: auto;
    top: auto;
    transform: none;
    width: 100%;
    height: 320px;
    margin-top: 48px;
  }

  .floating-card {
    padding: 12px 16px;
  }

  .card-1 { top: 20px; left: 10px; }
  .card-2 { top: 120px; right: 10px; }
  .card-3 { bottom: 20px; left: 30px; }

  .section {
    padding: 60px 24px;
  }

  .section-title {
    font-size: 28px;
  }

  .pain-grid,
  .features-grid,
  .membership-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .solution-body {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .solution-text {
    max-width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 32px;
  }

  .stat-num {
    font-size: 32px;
  }

  .cta-box {
    padding: 40px 24px;
  }

  .cta-title {
    font-size: 24px;
  }

  .footer-content {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .pain-grid,
  .features-grid,
  .membership-grid {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 30px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
    justify-content: center;
  }
}
</style>
