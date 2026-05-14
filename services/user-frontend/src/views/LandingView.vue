<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useViewport } from '../composables/useViewport.js'
import { listMembershipPlans } from '../api/membershipPlan.js'
import { getConfigs } from '../api/config.js'
import { getOperatorInfo } from '../api/operator.js'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
const { isMobile } = useViewport()
const membershipPlans = ref([])

const currentUser = ref(null)
const inviteCopied = ref(false)
const qrCodeUrl = ref('')
const contactModalOpen = ref(false)
const operatorName = ref('')

function checkLogin() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user && user.id) {
      currentUser.value = user
    }
  } catch (e) {
    currentUser.value = null
  }
}

function goLogin() {
  const op = route.query.op
  if (op) {
    router.push({ path: '/login', query: { op } })
  } else {
    router.push('/login')
  }
}

function goHome() {
  const op = route.query.op
  if (op) {
    router.push({ path: '/app/home', query: { op } })
  } else {
    router.push('/app/home')
  }
}

function scrollToMembership() {
  const el = document.getElementById('membership-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToCases() {
  const el = document.getElementById('cases-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToFeatures() {
  const el = document.getElementById('features-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToPushPreview() {
  const el = document.getElementById('push-preview-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToValuePitch() {
  const el = document.getElementById('value-pitch-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToMatrix() {
  const el = document.getElementById('matrix-section')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function doCopy(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.cssText = 'position:fixed;left:-9999px;top:0;'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  let success = false
  try {
    success = document.execCommand('copy')
  } catch (e) {}
  document.body.removeChild(textarea)
  return success
}

// 点击导航栏「分销活动」时跳转，若 URL 带有运营人员参数 op 则透传
function goAffiliate() {
  const op = route.query.op
  if (op) {
    router.push({ path: '/affiliate', query: { op } })
  } else {
    router.push('/affiliate')
  }
}

// 分享活动：复制当前登录用户的专属邀请码，用于分销返利
async function copyInviteCode() {
  if (!currentUser.value || !currentUser.value.inviteCode) {
    message.warning('请先登录后再复制邀请码')
    goLogin()
    return
  }
  if (doCopy(currentUser.value.inviteCode)) {
    inviteCopied.value = true
    message.success('邀请码已复制')
    setTimeout(() => { inviteCopied.value = false }, 2000)
  } else {
    message.error('复制失败，请手动复制')
  }
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

const caseStudies = [
  {
    tag: '新手宝妈',
    title: '情感赛道 · 运营3个月',
    desc: '每天花10分钟复制平台推送的文章，坚持发了一个月，流量主开始每天有稳定收入',
    yesterday: '45.20',
    dayRatio: '+164.64%',
    weekRatio: '+223.32%',
    monthTotal: '2,738.65',
    chartData: [18, 15, 19, 17, 85, 16, 45],
    chartLabels: ['04/16', '04/17', '04/18', '04/19', '04/20', '04/21', '04/22'],
  },
  {
    tag: '上班族副业',
    title: '职场赛道 · 运营5个月',
    desc: '利用通勤时间发布文章，目前已有稳定读者群，广告收益稳步增长',
    yesterday: '128.50',
    dayRatio: '+45.20%',
    weekRatio: '+89.60%',
    monthTotal: '3,560.30',
    chartData: [32, 45, 38, 52, 78, 95, 128],
    chartLabels: ['04/16', '04/17', '04/18', '04/19', '04/20', '04/21', '04/22'],
  },
  {
    tag: '退休大叔',
    title: '健康养生赛道 · 运营2个月',
    desc: '退休后想找点事做，跟着平台推荐发文，没想到第一月就有收益',
    yesterday: '62.30',
    dayRatio: '+28.50%',
    weekRatio: '+56.80%',
    monthTotal: '1,387.60',
    chartData: [12, 18, 15, 22, 35, 48, 62],
    chartLabels: ['04/16', '04/17', '04/18', '04/19', '04/20', '04/21', '04/22'],
  },
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

async function loadConfigs() {
  try {
    const op = route.query.op
    if (op) {
      const info = await getOperatorInfo(op)
      if (info) {
        qrCodeUrl.value = info.qrCodeUrl || ''
        operatorName.value = info.name || op
        return
      }
    }
    // 没有 op 参数时，读取主运营人员配置
    const data = await getConfigs()
    if (data) {
      qrCodeUrl.value = data.qrCodeUrl || ''
      if (data.mainOperator) {
        const mainInfo = await getOperatorInfo(data.mainOperator)
        if (mainInfo && mainInfo.qrCodeUrl) {
          qrCodeUrl.value = mainInfo.qrCodeUrl
          operatorName.value = mainInfo.name || data.mainOperator
        }
      }
    }
  } catch (e) {
    // ignore
  }
}

function getChartLine(data, w = 240, h = 64) {
  const max = Math.max(...data) * 1.15
  const step = w / (data.length - 1)
  return data.map((v, i) => {
    const x = i * step
    const y = h - (v / max) * (h - 12) - 6
    return `${x},${y}`
  }).join(' ')
}

function getChartArea(data, w = 240, h = 64) {
  const max = Math.max(...data) * 1.15
  const step = w / (data.length - 1)
  const points = data.map((v, i) => {
    const x = i * step
    const y = h - (v / max) * (h - 12) - 6
    return `${x},${y}`
  })
  return `0,${h} ` + points.join(' ') + ` ${w},${h}`
}

onMounted(() => {
  loadMembershipPlans()
  loadConfigs()
  checkLogin()
})
</script>

<template>
  <div class="landing">
    <!-- Navbar -->
    <nav class="navbar">
      <div class="nav-brand" @click="goHome">
        <img src="https://foruda.gitee.com/images/1776834561924666968/e0f84414_8060302.png" class="nav-logo-img" alt="logo">
        <span class="nav-title">知我公众号创作助手</span>
      </div>
      <div style="display: flex; align-items: center; gap: 16px;">
        <a class="nav-link" @click="scrollToFeatures">功能特点</a>
        <a class="nav-link" @click="scrollToPushPreview">每日推送</a>
        <a class="nav-link" @click="scrollToCases">真实案例</a>
        <a class="nav-link" @click="scrollToValuePitch">算笔账</a>
        <a class="nav-link" @click="scrollToMembership">会员权益</a>
        <!-- <a class="nav-link" @click="goAffiliate">分销活动</a> -->
        <a class="nav-link" @click="contactModalOpen = true">联系客服</a>
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
            知我公众号创作助手<br />让创作变得前所未有的简单
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
    <section id="features-section" class="section features-section">
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

    <!-- Case Studies Section -->
    <section id="cases-section" class="section cases-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">04 / 真实案例</span>
          <h2 class="section-title">他们已经在赚钱了</h2>
          <p class="section-subtitle">真实用户通过平台每日推送的文章，实现流量主收益</p>
        </div>
        <div class="cases-grid">
          <div v-for="(item, i) in caseStudies" :key="i" class="case-card">
            <div class="case-badge">{{ item.tag }}</div>
            <div class="case-screenshot">
              <div class="earning-card">
                <div class="earning-header">
                  <span class="earning-header-title">流量主收益</span>
                  <span class="earning-header-sub">7日趋势</span>
                </div>
                <div class="earning-main">
                  <div class="earning-label">昨日收入</div>
                  <div class="earning-value">¥{{ item.yesterday }}</div>
                </div>
                <div class="earning-pills">
                  <div class="earning-pill">
                    <span class="pill-label">日环比</span>
                    <span class="pill-value up">{{ item.dayRatio }}</span>
                  </div>
                  <div class="earning-pill">
                    <span class="pill-label">周同比</span>
                    <span class="pill-value up">{{ item.weekRatio }}</span>
                  </div>
                  <div class="earning-pill">
                    <span class="pill-label">本月累计</span>
                    <span class="pill-value">¥{{ item.monthTotal }}</span>
                  </div>
                </div>
                <div class="earning-chart">
                  <svg viewBox="0 0 240 64" class="chart-svg">
                    <defs>
                      <linearGradient :id="'grad' + i" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stop-color="#07C160" stop-opacity="0.3" />
                        <stop offset="100%" stop-color="#07C160" stop-opacity="0" />
                      </linearGradient>
                    </defs>
                    <polygon :points="getChartArea(item.chartData)" :fill="'url(#grad' + i + ')'" />
                    <polyline :points="getChartLine(item.chartData)" fill="none" stroke="#07C160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <circle v-for="(val, idx) in item.chartData" :key="idx"
                      :cx="idx * (240 / (item.chartData.length - 1))"
                      :cy="64 - (val / (Math.max(...item.chartData) * 1.15)) * 52 - 6"
                      r="2.5" fill="#fff" stroke="#07C160" stroke-width="1.5" />
                  </svg>
                </div>
              </div>
            </div>
            <div class="case-info">
              <div class="case-title">{{ item.title }}</div>
              <div class="case-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
        <p class="cases-tip">
          * 以上数据为用户真实收益，仅供展示参考，实际收益因人而异
        </p>
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

    <!-- ==================== 文章推送展示区块 ==================== -->
    <section id="push-preview-section" class="section push-preview-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">03 / 每日推送</span>
          <h2 class="section-title">每天一篇，精准送达</h2>
          <p class="section-subtitle">订阅你感兴趣的赛道，系统每日为你推送优质原创文章</p>
        </div>

        <div class="push-preview-body">
          <!-- 左侧：邮件推送卡片 -->
          <div class="push-email-card">
            <div class="push-email-header">
              <div class="push-email-brand">知我公众号创作助手</div>
              <div class="push-email-sub">让 AI 成为你的创作引擎</div>
            </div>
            <div class="push-email-content">
              <div class="push-email-greeting">尊敬的 <strong>创作者</strong>，您好！</div>
              <div class="push-email-intro">
                今日为您推荐 <span class="push-tag">历史文化</span> 赛道的优质文章，附件已随邮件送达，请查收。
              </div>
              <div class="push-article-box">
                <div class="push-article-label">文章标题</div>
                <div class="push-article-title">古人临终前留下的8个字，藏着中国人千年都没看透的智慧</div>
                <div class="push-article-meta">
                  <span class="push-meta-tag">发布平台：公众号</span>
                </div>
              </div>
              <div class="push-email-tips">
                <div class="push-tips-title">温馨提示：</div>
                <ol>
                  <li>附件为 Word 文档（.docx），可保存到本地；</li>
                  <li>保存后可直接上传至公众号平台使用；</li>
                  <li>文章内容已根据您的个人风格偏好进行排版。</li>
                </ol>
              </div>
            </div>
            <div class="push-email-footer">
              知我公众号创作助手 · 让创作更简单，让变现更高效<br>
              如您不希望继续收到此类邮件，可登录后台取消订阅。
            </div>
          </div>

          <!-- 右侧：推送特性说明 -->
          <div class="push-features">
            <div class="push-feature-item">
              <div class="push-feature-icon">📬</div>
              <div class="push-feature-title">邮件 + 站内双通道推送</div>
              <div class="push-feature-desc">每日早 8 点准时送达，支持邮箱订阅和站内消息两种接收方式，不错过任何一篇好文。</div>
            </div>
            <div class="push-feature-item">
              <div class="push-feature-icon">🎯</div>
              <div class="push-feature-title">按赛道精准匹配</div>
              <div class="push-feature-desc">根据你订阅的赛道和偏好标签，AI 自动筛选最符合你账号定位的文章主题。</div>
            </div>
            <div class="push-feature-item">
              <div class="push-feature-icon">📎</div>
              <div class="push-feature-title">Word 附件直接可用</div>
              <div class="push-feature-desc">推送邮件附带排版精美的 .docx 文件，下载后可直接复制到公众号编辑器发布。</div>
            </div>
            <div class="push-feature-item">
              <div class="push-feature-icon">🎨</div>
              <div class="push-feature-title">多种风格自由切换</div>
              <div class="push-feature-desc">清新文艺、商务正式、科技简约……10+ 种排版风格随心选择，总有一款适合你。</div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <!-- ==================== 文章推送展示区块结束 ==================== -->

    <!-- ==================== 文章内容样式展示区块 ==================== -->
    <section id="article-style-section" class="section article-style-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">04 / 排版样式</span>
          <h2 class="section-title">精美排版，即拿即用</h2>
          <p class="section-subtitle">多种文章风格模板，下载后直接复制到编辑器即可发布</p>
        </div>

        <div class="article-style-showcase">
          <!-- 左侧：样式导航 -->
          <div class="style-nav">
            <div class="style-nav-item active">
              <span class="style-nav-dot" style="background: #4ade80;"></span>
              清新律动风
            </div>
            <div class="style-nav-item">
              <span class="style-nav-dot" style="background: #60a5fa;"></span>
              科技蓝标签风
            </div>
            <div class="style-nav-item">
              <span class="style-nav-dot" style="background: #f472b6;"></span>
              北欧杂志风
            </div>
            <div class="style-nav-item">
              <span class="style-nav-dot" style="background: #9ca3af;"></span>
              工业水泥风
            </div>
          </div>

          <!-- 右侧：文章预览 -->
          <div class="article-preview-card">
            <div class="article-preview-header">
              <div class="article-preview-badge">清新律动风</div>
            </div>
            <div class="article-preview-body docx-qingxin">
              <h1 class="qx-title">人永远赚不到认知以外的钱</h1>
              <div class="qx-divider">—— 🌿 ——</div>
              <p class="qx-subtitle">▲ 认知，决定你能走多远</p>
              <div class="qx-divider">—— 🌿 ——</div>
              <p class="qx-para">
                最近，一个年轻人靠内容创作月入过万的故事引发了热议。有人羡慕，有人质疑，但很少有人看到背后的真相：他的成功，本质是认知的胜利。
              </p>
              <h2 class="qx-heading">认知差，才是最大的贫富差距</h2>
              <p class="qx-para">
                同样的时代，同样的机会，为什么有人能抓住，有人只能旁观？答案就是认知差。
              </p>
              <p class="qx-para">
                他懂得平台规则、内容逻辑、用户心理，而这些认知，很多人至今一片空白。不是他多聪明，而是他的认知维度更高。
              </p>
              <ul class="qx-list">
                <li><span class="qx-emoji">🌱</span> 你永远赚不到认知以外的钱，除非靠运气；但靠运气赚到的钱，最后往往靠实力亏掉。</li>
                <li><span class="qx-emoji">🍃</span> 认知决定选择，选择决定命运</li>
                <li><span class="qx-emoji">🍃</span> 提升认知，比埋头苦干更重要</li>
                <li><span class="qx-emoji">🍃</span> 向有结果的人学习，是最快的捷径</li>
              </ul>
              <div class="qx-divider">—— 🌿 ——</div>
              <h2 class="qx-heading">信息差正在消失，认知差正在拉大</h2>
              <p class="qx-para">
                过去，赚钱靠信息差——你知道的，别人不知道。如今互联网让信息透明，信息差越来越小。
              </p>
              <p class="qx-para">
                但认知差却在拉大。同样的信息，有人看到机会，有人看到热闹；有人立刻行动，有人只会观望。这就是差距的根源。
              </p>
              <blockquote class="qx-quote">
                "你所赚的每一分钱，都是你对这个世界认知的变现。"
                <span class="qx-quote-source">—— 投资名言</span>
              </blockquote>
              <ul class="qx-list">
                <li><span class="qx-emoji">🍃</span> 不要只收集信息，要提升认知</li>
                <li><span class="qx-emoji">🍃</span> 学会深度思考，而非浅层浏览</li>
                <li><span class="qx-emoji">🍃</span> 把信息转化为行动，才能创造价值</li>
              </ul>
              <div class="qx-divider">—— 🌿 ——</div>
              <h2 class="qx-heading">认知升级，永远不晚</h2>
              <p class="qx-para">
                有人觉得，自己起步晚了，学不动了。但认知升级和年龄无关，和心态有关。
              </p>
              <p class="qx-para">
                很多后来居上的创作者，都在用行动证明：只要愿意打开自己，任何时候都可以重新开始。
              </p>
              <p class="qx-para qx-highlight">
                🌱 限制你的从来不是时间，而是你以为自己不行了的念头。
              </p>
              <div class="qx-divider">—— 🌿 ——</div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <!-- ==================== 文章内容样式展示区块结束 ==================== -->

    <!-- ==================== 价值说服区块 ==================== -->
    <section id="value-pitch-section" class="section value-pitch-section">
      <div class="section-container">
        <div class="value-pitch-body">
          <div class="value-pitch-left">
            <div class="value-pitch-tag">算一笔账</div>
            <h2 class="value-pitch-title">为什么聪明人选择借力，<br>而不是独自死磕？</h2>
            <div class="value-math-card">
              <div class="value-math-row">
                <span class="value-math-label">标准版年费</span>
                <span class="value-math-num">≈ ¥400</span>
              </div>
              <div class="value-math-row">
                <span class="value-math-label">平均每天</span>
                <span class="value-math-num">¥1.1</span>
              </div>
              <div class="value-math-divider"></div>
              <div class="value-math-highlight">
                <div class="value-math-result">爆一篇 5 万阅读</div>
                <div class="value-math-result-sub">流量主收益即可回本</div>
              </div>
              <div class="value-math-multi">
                <div class="value-math-multi-title">🚀 一文多发，收益放大</div>
                <div class="value-math-multi-row">
                  <span class="value-math-multi-platform">公众号 5 万阅读</span>
                  <span class="value-math-multi-money">≈ ¥100</span>
                </div>
                <div class="value-math-multi-row">
                  <span class="value-math-multi-platform">今日头条 5 万阅读</span>
                  <span class="value-math-multi-money">≈ ¥40</span>
                </div>
                <div class="value-math-multi-row">
                  <span class="value-math-multi-platform">百家号 5 万阅读</span>
                  <span class="value-math-multi-money">≈ ¥30</span>
                </div>
                <div class="value-math-multi-total">
                  同一篇文章发到 3 个平台，单篇合计 ≈ <strong>¥170</strong>，回本速度快 3 倍
                </div>
              </div>
              <div class="value-math-note">
                而且账号和粉丝是你永久的数字资产，只会越积越值钱。
              </div>
            </div>
          </div>
          <div class="value-pitch-right">
            <div class="value-pain-card">
              <div class="value-pain-header">
                <span class="value-pain-icon">💔</span>
                <span class="value-pain-title">90% 的人倒在了起跑线</span>
              </div>
              <p class="value-pain-text">
                连续发 30 篇文章，阅读量始终个位数——这不是你不够努力，是<strong>选题、素材、排版</strong>每个环节都在无声消耗你的意志力。
              </p>
              <p class="value-pain-text">
                付出和收获长期不成正比，谁都会想放弃。<span class="value-pain-emphasis">这是人性，不是缺陷。</span>
              </p>
            </div>
            <div class="value-solution-card">
              <div class="value-solution-header">
                <span class="value-solution-icon">🚀</span>
                <span class="value-solution-title">我们帮你跨过这道坎</span>
              </div>
              <p class="value-solution-text">
                每天把一篇<strong>现成的优质文章</strong>送到你手上，排版精美、选题精准、多平台可用。
              </p>
              <p class="value-solution-text">
                你不需要再为"今天写什么"发愁，把有限的精力留给<strong>坚持和放大</strong>，而不是从 0 开始死磕。
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
    <!-- ==================== 价值说服区块结束 ==================== -->

    <!-- ==================== 会员权益 / 套餐选择区块 ==================== -->
    <section id="membership-section" class="section membership-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">05 / 会员权益</span>
          <h2 class="section-title">选择适合你的套餐</h2>
          <p class="section-subtitle">多种档位，满足不同需求，按月订阅，随时可退</p>
        </div>

        <!-- 套餐推荐建议 -->
        <div class="plan-recommend-bar">
          <div class="rec-item">
            <span class="rec-dot" style="background: #07C160;"></span>
            <strong>个人创作者</strong>推荐「标准版」，性价比之王，每天不到一杯奶茶钱
          </div>
          <div class="rec-item">
            <span class="rec-dot" style="background: #f59e0b;"></span>
            <strong>做矩阵运营</strong>推荐「旗舰版」或「专业版」，多账号多渠道收益翻倍
          </div>
        </div>

        <div class="membership-grid">
          <div v-for="(plan, i) in membershipPlans" :key="plan.id" class="membership-card" :class="{ popular: plan.name && plan.name.includes('标准') }">
            <div v-if="plan.name && plan.name.includes('标准')" class="popular-badge">标准版</div>
            <div class="membership-name">{{ plan.name }}</div>
            <div class="membership-price-row">
              <span class="membership-price">¥{{ plan.price }}</span>
              <span class="membership-unit">/ 月</span>
            </div>
            <div v-if="plan.originalPrice && plan.originalPrice > 0" class="membership-original">
              原价 ¥{{ plan.originalPrice }}
            </div>
            <!-- 标准版专属高性价比宣传语 -->
            <div v-if="plan.name && plan.name.includes('标准')" class="plan-promo">
              平均每天不到 1 块钱，平台每日为你推送一篇原创好文，一年下来就是 365 篇素材。你只需要花几分钟复制粘贴发布，就有可能获得流量主收益。这笔账怎么算都值。
            </div>
            <div class="membership-divider"></div>
            <ul class="membership-features">
              <li v-for="(f, j) in parseFeatures(plan)" :key="j">
                <span class="feature-check">✓</span>
                {{ f }}
              </li>
            </ul>
            <button class="membership-btn" :class="{ primary: plan.name && plan.name.includes('标准') }" @click="goLogin">
              {{ (plan.name && plan.name.includes('标准')) ? '立即开通' : '选择此套餐' }}
            </button>
          </div>
        </div>
      </div>
    </section>
    <!-- ==================== 会员权益 / 套餐选择区块结束 ==================== -->

    <!-- ==================== 矩阵运营与收益说明区块 ==================== -->
    <section id="matrix-section" class="section matrix-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">进阶玩法</span>
          <h2 class="section-title">一文多发，收益多倍</h2>
          <p class="section-subtitle">了解矩阵运营和收益来源，让你的创作回报最大化</p>
        </div>

        <div class="matrix-grid">
          <!-- 什么是矩阵 -->
          <div class="matrix-card">
            <div class="matrix-icon">🌐</div>
            <h3 class="matrix-card-title">什么是矩阵？</h3>
            <p class="matrix-card-desc">
              矩阵运营就是同时在多个平台、多个账号上发布内容。比如你可以同时运营 3 个公众号、2 个百家号、2 个今日头条号，形成内容分发网络。
              <br><br>
              平台每天推送的同一篇文章，你可以稍作调整后发到所有账号上。1 篇文章变成 7 篇，曝光量和收益机会也随之乘以 7。
            </p>
          </div>

          <!-- 什么是一文多发 -->
          <div class="matrix-card">
            <div class="matrix-icon">🚀</div>
            <h3 class="matrix-card-title">什么是一文多发？</h3>
            <p class="matrix-card-desc">
              平台每天为你生成的原创文章，你可以在<strong>公众号、百家号、今日头条</strong>等多个平台同步发布。
              <br><br>
              同一篇文章在不同平台的推荐算法是独立的，意味着你在公众号没爆的文章，可能在今日头条获得 10w+ 阅读。多平台分发 = 多倍曝光 = 多倍收益可能。
            </p>
          </div>

          <!-- 公众号收益 -->
          <div class="matrix-card">
            <div class="matrix-icon">💰</div>
            <h3 class="matrix-card-title">公众号收益来源</h3>
            <p class="matrix-card-desc">
              <strong>流量主广告：</strong>开通后文章底部自动展示广告，按曝光和点击计费。1 万阅读通常收益 100-300 元，高价值领域（如金融、教育）可达 500-1000 元。
              <br><br>
              <strong>互选广告：</strong>粉丝达 500 即可开通，广告主直接找你投放软文。按阅读计费，单价通常 0.5-2 元/阅读，头部账号单条广告收入可达 5 万元以上。
            </p>
          </div>

          <!-- 今日头条收益 -->
          <div class="matrix-card">
            <div class="matrix-icon">📈</div>
            <h3 class="matrix-card-title">今日头条收益来源</h3>
            <p class="matrix-card-desc">
              <strong>阅读分成：</strong>收益 = 千次阅读单价 × 互动系数 × 获利阅读量 ÷ 1000。
              <br><br>
              2025 年头条新增<strong>互动系数</strong>（上限 3 倍）：评论、收藏、进入主页等行为越多，收益越高。粉丝阅读价值是非粉丝的 10-20 倍。首发平台声明首发可获得 3 倍收益加成。
            </p>
          </div>

          <!-- 百家号收益 -->
          <div class="matrix-card">
            <div class="matrix-icon">📰</div>
            <h3 class="matrix-card-title">百家号收益来源</h3>
            <p class="matrix-card-desc">
              <strong>广告分成：</strong>基于广告展现量和内容质量动态计算。图文内容万次阅读收益约 30-80 元，垂直领域和原创内容单价更高。
              <br><br>
              粉丝阅读收益明显高于非粉丝流量。内容完成率越高、用户停留时间越长，收益越高。优质账号还可获得平台专项奖励。
            </p>
          </div>

          <!-- 收益总结 -->
          <div class="matrix-card highlight">
            <div class="matrix-icon">✨</div>
            <h3 class="matrix-card-title">算一笔账</h3>
            <p class="matrix-card-desc">
              假设你每天花 10 分钟发布 1 篇文章到 3 个平台（公众号 + 今日头条 + 百家号），平均每个平台获得 5000 阅读：
              <br><br>
              公众号 5000 阅读 ≈ 50-150 元<br>
              今日头条 5000 阅读 ≈ 15-60 元<br>
              百家号 5000 阅读 ≈ 15-40 元<br>
              <strong>单日合计 ≈ 80-250 元</strong>
              <br><br>
              一个月就是 2400-7500 元。而你的投入只是一杯奶茶钱的月费和每天 10 分钟。
            </p>
          </div>
        </div>
      </div>
    </section>
    <!-- ==================== 矩阵运营与收益说明区块结束 ==================== -->

    <!-- ==================== 分享活动 / 分销活动区块（已注释） ==================== -->
    <!--
    <section id="affiliate-section" class="section affiliate-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">06 / 分销活动</span>
          <h2 class="section-title">邀请好友，赚取佣金</h2>
          <p class="section-subtitle">邀请好友注册并开通会员，您可获得好友会员费用的 30% 作为佣金</p>
        </div>
        <div class="affiliate-body">
          <div class="affiliate-card">
            <div class="affiliate-steps">
              <div class="affiliate-step">
                <div class="affiliate-step-num">01</div>
                <div class="affiliate-step-title">分享邀请码</div>
                <div class="affiliate-step-desc">将您的专属邀请码分享给好友</div>
              </div>
              <div class="affiliate-step-arrow">→</div>
              <div class="affiliate-step">
                <div class="affiliate-step-num">02</div>
                <div class="affiliate-step-title">好友注册开通</div>
                <div class="affiliate-step-desc">好友使用您的邀请码注册并开通会员</div>
              </div>
              <div class="affiliate-step-arrow">→</div>
              <div class="affiliate-step">
                <div class="affiliate-step-num">03</div>
                <div class="affiliate-step-title">获得佣金返利</div>
                <div class="affiliate-step-desc">您可获得好友会员费用的 30% 作为佣金</div>
              </div>
            </div>
            <div class="affiliate-invite-box">
              <div v-if="currentUser && currentUser.inviteCode" class="affiliate-invite-active">
                <div class="affiliate-invite-label">我的专属邀请码</div>
                <div class="affiliate-invite-code">{{ currentUser.inviteCode }}</div>
                <button class="affiliate-copy-btn" @click="copyInviteCode">
                  {{ inviteCopied ? '已复制 ✓' : '复制邀请码' }}
                </button>
              </div>
              <div v-else class="affiliate-invite-login">
                <div class="affiliate-invite-label">登录后即可获取专属邀请码</div>
                <button class="affiliate-copy-btn" @click="goLogin">立即登录</button>
              </div>
            </div>
            <div class="affiliate-contact">
              <div class="affiliate-qr">
                <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="客服二维码" class="affiliate-qr-img">
                <div v-else class="affiliate-qr-placeholder">
                  <div style="font-size: 11px; color: #94a3b8; text-align: center;">微信客服</div>
                  <div style="font-size: 10px; color: #cbd5e1; margin-top: 4px;">请替换二维码</div>
                </div>
              </div>
              <div class="affiliate-contact-info">
                <div class="affiliate-contact-title">{{ operatorName ? '联系 ' + operatorName + ' 参与活动' : '联系客服参与活动' }}</div>
                <div class="affiliate-contact-desc">
                  添加客服微信了解更多分销活动详情，佣金实时结算，提现无门槛
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
    -->
    <!-- ==================== 分享活动 / 分销活动区块结束（已注释） ==================== -->

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
          <span>知我公众号创作助手</span>
        </div>
        <p class="footer-copy">让每个人都能轻松运营公众号</p>
      </div>
    </footer>

    <!-- Contact Modal -->
    <div v-if="contactModalOpen" class="modal-overlay" @click.self="contactModalOpen = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">联系客服</h3>
          <button class="modal-close" @click="contactModalOpen = false">×</button>
        </div>
        <div class="modal-body">
          <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="客服二维码" class="modal-qr-img">
          <div v-else style="width: 200px; height: 200px; background: #f3f4f6; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 14px; color: #9ca3af;">客服二维码</div>
          <p class="modal-desc">扫码添加客服微信</p>
        </div>
      </div>
    </div>
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

/* 套餐推荐建议栏 */
.plan-recommend-bar {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.rec-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 100px;
  font-size: 14px;
  color: #4b5563;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.rec-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 标准版专属高性价比宣传语 */
.plan-promo {
  font-size: 13px;
  color: #07C160;
  line-height: 1.6;
  padding: 10px 12px;
  background: rgba(7, 193, 96, 0.06);
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px dashed rgba(7, 193, 96, 0.2);
}

/* ========== Matrix Section ========== */
.matrix-section {
  background: #fff;
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.matrix-card {
  padding: 28px 24px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.matrix-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.matrix-card.highlight {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border-color: #86efac;
}

.matrix-icon {
  font-size: 32px;
  margin-bottom: 12px;
  line-height: 1;
}

.matrix-card-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
}

.matrix-card-desc {
  font-size: 14px;
  line-height: 1.8;
  color: #4b5563;
}

.matrix-card-desc strong {
  color: #07C160;
}

/* ========== Push Preview Section ========== */
.push-preview-section {
  background: #f8fafc;
}

.push-preview-body {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 40px;
  align-items: start;
}

.push-email-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.push-email-header {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  padding: 28px 24px;
  text-align: center;
}

.push-email-brand {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.push-email-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.push-email-content {
  padding: 28px 24px;
}

.push-email-greeting {
  font-size: 15px;
  color: #374151;
  margin-bottom: 16px;
}

.push-email-intro {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin-bottom: 20px;
}

.push-tag {
  display: inline-block;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
}

.push-article-box {
  border-left: 3px solid #3b82f6;
  background: #f8fafc;
  padding: 16px 20px;
  border-radius: 0 10px 10px 0;
  margin-bottom: 20px;
}

.push-article-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 6px;
}

.push-article-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.5;
  margin-bottom: 8px;
}

.push-article-meta {
  font-size: 12px;
  color: #6b7280;
}

.push-meta-tag {
  display: inline-block;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 4px;
}

.push-email-tips {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 10px;
  padding: 14px 18px;
}

.push-tips-title {
  font-size: 13px;
  font-weight: 600;
  color: #b45309;
  margin-bottom: 8px;
}

.push-email-tips ol {
  margin: 0;
  padding-left: 18px;
}

.push-email-tips li {
  font-size: 12px;
  color: #92400e;
  line-height: 1.7;
}

.push-email-footer {
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  padding: 16px 24px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.6;
}

.push-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.push-feature-item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 24px;
  transition: all 0.3s;
}

.push-feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  border-color: #cbd5e1;
}

.push-feature-icon {
  font-size: 28px;
  margin-bottom: 10px;
  line-height: 1;
}

.push-feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.push-feature-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

/* ========== Article Style Section ========== */
.article-style-section {
  background: #fff;
}

.article-style-showcase {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  align-items: start;
}

.style-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.style-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.style-nav-item:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.style-nav-item.active {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #166534;
  font-weight: 600;
}

.style-nav-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.article-preview-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.article-preview-header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.article-preview-badge {
  display: inline-block;
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.article-preview-body {
  padding: 36px 40px;
  max-height: 560px;
  overflow-y: auto;
}

/* 清新律动风排版 */
.docx-qingxin {
  font-family: 'PingFang SC', 'Microsoft YaHei', '微软雅黑', sans-serif;
  color: #2c3e50;
}

.docx-qingxin .qx-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  text-align: center;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.docx-qingxin .qx-divider {
  text-align: center;
  font-size: 14px;
  color: #86efac;
  margin: 20px 0;
  letter-spacing: 4px;
}

.docx-qingxin .qx-subtitle {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.docx-qingxin .qx-heading {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 20px 0 12px;
  text-align: center;
}

.docx-qingxin .qx-para {
  font-size: 14px;
  line-height: 2;
  color: #4b5563;
  margin-bottom: 12px;
  text-indent: 2em;
}

.docx-qingxin .qx-para.qx-highlight {
  background: rgba(74, 222, 128, 0.08);
  border-radius: 8px;
  padding: 12px 16px;
  text-indent: 0;
  color: #166534;
  font-weight: 500;
}

.docx-qingxin .qx-list {
  list-style: none;
  padding: 0;
  margin: 12px 0;
}

.docx-qingxin .qx-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  line-height: 2;
  color: #4b5563;
  padding: 4px 0;
}

.docx-qingxin .qx-emoji {
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 4px;
}

.docx-qingxin .qx-quote {
  background: #f8fafc;
  border-left: 3px solid #4ade80;
  padding: 16px 20px;
  margin: 16px 0;
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
  font-style: italic;
  border-radius: 0 8px 8px 0;
}

.docx-qingxin .qx-quote-source {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  font-style: normal;
  margin-top: 6px;
}

/* ========== Value Pitch Section ========== */
.value-pitch-section {
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}

.value-pitch-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: start;
}

.value-pitch-tag {
  display: inline-block;
  background: rgba(7, 193, 96, 0.1);
  color: #07C160;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  margin-bottom: 16px;
}

.value-pitch-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  margin-bottom: 28px;
}

.value-math-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
}

.value-math-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.value-math-label {
  font-size: 14px;
  color: #64748b;
}

.value-math-num {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.value-math-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 12px 0;
}

.value-math-highlight {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  margin: 16px 0;
}

.value-math-result {
  font-size: 18px;
  font-weight: 700;
  color: #07C160;
  margin-bottom: 4px;
}

.value-math-result-sub {
  font-size: 13px;
  color: #166534;
}

.value-math-note {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  text-align: center;
}

.value-math-multi {
  background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  padding: 16px;
  margin: 12px 0;
}

.value-math-multi-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 10px;
  text-align: center;
}

.value-math-multi-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
}

.value-math-multi-platform {
  color: #4b5563;
}

.value-math-multi-money {
  font-weight: 600;
  color: #1e293b;
}

.value-math-multi-total {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #bfdbfe;
  font-size: 13px;
  color: #1d4ed8;
  text-align: center;
  font-weight: 500;
}

.value-math-multi-total strong {
  color: #2563eb;
  font-weight: 700;
}

.value-pain-card {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
}

.value-pain-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.value-pain-icon {
  font-size: 22px;
  line-height: 1;
}

.value-pain-title {
  font-size: 16px;
  font-weight: 700;
  color: #b91c1c;
}

.value-pain-text {
  font-size: 14px;
  line-height: 1.8;
  color: #4b5563;
  margin-bottom: 10px;
}

.value-pain-text strong {
  color: #1e293b;
  font-weight: 600;
}

.value-pain-emphasis {
  display: inline-block;
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 4px;
}

.value-solution-card {
  background: linear-gradient(135deg, #f0fdf4 0%, #f6fef9 100%);
  border: 1px solid #bbf7d0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.06);
}

.value-solution-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.value-solution-icon {
  font-size: 22px;
  line-height: 1;
}

.value-solution-title {
  font-size: 16px;
  font-weight: 700;
  color: #166534;
}

.value-solution-text {
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
  margin-bottom: 10px;
}

.value-solution-text strong {
  color: #07C160;
  font-weight: 600;
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

/* ========== Cases Section ========== */
.cases-section {
  background: #fff;
}

.cases-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.case-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s;
  position: relative;
}

.case-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
  border-color: #07C160;
}

.case-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  z-index: 2;
}

.case-screenshot {
  background: #f8fafc;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}

.case-img {
  max-width: 100%;
  max-height: 320px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  object-fit: contain;
}

.case-info {
  padding: 20px 24px 24px;
}

.case-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.case-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 12px;
}

.case-highlight {
  font-size: 14px;
  font-weight: 600;
  color: #07C160;
  background: #f0fdf4;
  padding: 8px 12px;
  border-radius: 8px;
  display: inline-block;
}

.earning-card {
  width: 100%;
  max-width: 280px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.earning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.earning-header-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.earning-header-sub {
  font-size: 11px;
  color: #9ca3af;
}

.earning-main {
  text-align: center;
}

.earning-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.earning-value {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
}

.earning-pills {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.earning-pill {
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 10px;
  text-align: center;
  flex: 1;
}

.pill-label {
  font-size: 10px;
  color: #9ca3af;
  margin-bottom: 2px;
}

.pill-value {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.pill-value.up {
  color: #ef4444;
}

.earning-chart {
  margin-top: 4px;
}

.chart-svg {
  width: 100%;
  height: 64px;
  overflow: visible;
}

.cases-tip {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 24px;
}

/* ========== Affiliate Section ========== */
.affiliate-section {
  background: linear-gradient(180deg, #fff 0%, #f0fdf4 100%);
}

.affiliate-body {
  max-width: 900px;
  margin: 0 auto;
}

.affiliate-card {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  padding: 48px;
  box-shadow: 0 8px 40px rgba(7, 193, 96, 0.08);
}

.affiliate-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.affiliate-step {
  text-align: center;
  flex: 1;
  min-width: 160px;
}

.affiliate-step-num {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.affiliate-step-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.affiliate-step-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.affiliate-step-arrow {
  font-size: 24px;
  color: #07C160;
  font-weight: 700;
  flex-shrink: 0;
}

.affiliate-invite-box {
  background: linear-gradient(135deg, #f0fdf4 0%, #f6fef9 100%);
  border: 1px solid #bbf7d0;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  margin-bottom: 32px;
}

.affiliate-invite-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
}

.affiliate-invite-code {
  font-family: monospace;
  font-size: 32px;
  font-weight: 700;
  color: #07C160;
  letter-spacing: 4px;
  margin-bottom: 16px;
}

.affiliate-copy-btn {
  padding: 12px 32px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.3);
}

.affiliate-copy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(7, 193, 96, 0.4);
}

.affiliate-contact {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.affiliate-qr {
  flex-shrink: 0;
}

.affiliate-qr-img {
  width: 140px;
  height: 140px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  object-fit: contain;
}

.affiliate-qr-placeholder {
  width: 140px;
  height: 140px;
  border-radius: 12px;
  border: 2px dashed #cbd5e1;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.affiliate-contact-info {
  max-width: 320px;
}

.affiliate-contact-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.affiliate-contact-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .affiliate-card {
    padding: 28px 20px;
  }

  .affiliate-steps {
    flex-direction: column;
    gap: 16px;
  }

  .affiliate-step-arrow {
    transform: rotate(90deg);
  }

  .affiliate-invite-code {
    font-size: 24px;
  }

  .affiliate-contact {
    flex-direction: column;
    text-align: center;
  }
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

/* ========== Modal ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 360px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: scaleIn 0.2s ease;
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.modal-qr-img {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  object-fit: contain;
  border: 1px solid #e5e7eb;
}

.modal-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
  .navbar {
    padding: 0 16px;
    height: 56px;
  }

  .nav-title {
    font-size: 14px;
    max-width: 140px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .nav-link {
    display: none;
  }

  .login-btn {
    padding: 6px 16px;
    font-size: 13px;
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
  .membership-grid,
  .cases-grid {
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

  .push-preview-body {
    grid-template-columns: 1fr;
  }

  .push-features {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .push-feature-item {
    flex: 1;
    min-width: 220px;
  }

  .article-style-showcase {
    grid-template-columns: 1fr;
  }

  .style-nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .style-nav-item {
    flex: 1;
    min-width: 140px;
    justify-content: center;
  }

  .article-preview-body {
    padding: 24px;
    max-height: 480px;
  }

  .value-pitch-body {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .value-pitch-title {
    font-size: 24px;
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
  .membership-grid,
  .cases-grid {
    grid-template-columns: 1fr;
  }

  .push-feature-item {
    min-width: 100%;
  }

  .style-nav-item {
    min-width: 100%;
  }

  .article-preview-body {
    padding: 20px;
    max-height: 400px;
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
