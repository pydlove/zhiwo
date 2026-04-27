<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useViewport } from '../composables/useViewport.js'
import { listMembershipPlans } from '../api/membershipPlan.js'
import { getConfigs } from '../api/config.js'
import { message } from 'ant-design-vue'

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

const router = useRouter()
const { isMobile } = useViewport()

const currentUser = ref(null)
const inviteCopied = ref(false)
const membershipPlans = ref([])
const qrCodeUrl = ref('')

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
  router.push('/login')
}

function goHome() {
  router.push('/')
}

function getRegisterUrl() {
  const base = window.location.origin
  return base + '/register?invite=' + encodeURIComponent(currentUser.value.inviteCode)
}

async function copyInviteCode() {
  if (!currentUser.value || !currentUser.value.inviteCode) {
    message.warning('请先登录后再复制邀请码')
    goLogin()
    return
  }
  const text = `推荐你使用知我公众号创作助手！

基于 AI 的公众号爆款文章创作平台，覆盖 36+ 热门赛道，每日智能推荐，让你轻松写出 10w+。

点击链接注册，开启你的爆款创作之旅：
${getRegisterUrl()}

注册后请添加客服微信审核，即可使用全部功能。`
  if (doCopy(text)) {
    inviteCopied.value = true
    message.success('邀请文案已复制')
    setTimeout(() => { inviteCopied.value = false }, 2000)
  } else {
    message.error('复制失败，请手动复制')
  }
}

async function loadMembershipPlans() {
  try {
    const data = await listMembershipPlans()
    membershipPlans.value = (data || []).filter(p => p.status === 1)
  } catch (e) {
    membershipPlans.value = []
  }
}

const commissionExamples = computed(() => {
  return membershipPlans.value.map(plan => ({
    name: plan.name,
    price: plan.price,
    commission: (plan.price * 0.3).toFixed(2),
  }))
})

const faqs = [
  {
    q: '佣金是如何计算的？',
    a: '当您的好友通过您的邀请码注册并开通会员后，您将获得好友支付会员费用的 30% 作为佣金。例如好友开通 99 元/月的套餐，您将获得 29.70 元佣金。',
  },
  {
    q: '佣金什么时候到账？',
    a: '好友完成支付后，佣金将实时计入您的账户，月底结算。',
  },
  {
    q: '好友开通后取消会员，佣金会退回吗？',
    a: '如果好友在开通后 7 天内申请退款，对应佣金将被扣除。7 天后退款不影响已发放的佣金。',
  },
  {
    q: '邀请码有有效期吗？',
    a: '邀请码永久有效，您可以随时分享给好友，不限次数。每成功邀请一位好友开通会员，您都将获得对应的佣金奖励。',
  },
  {
    q: '可以邀请多少人？',
    a: '邀请人数不设上限，邀请越多，收益越多。我们鼓励您将邀请码分享给更多有公众号创作需求的朋友。',
  },
]

const activeFaq = ref(null)
function toggleFaq(index) {
  activeFaq.value = activeFaq.value === index ? null : index
}

async function loadConfigs() {
  try {
    const data = await getConfigs()
    if (data && data.qrCodeUrl) {
      qrCodeUrl.value = data.qrCodeUrl
    }
  } catch (e) {
    qrCodeUrl.value = ''
  }
}

onMounted(() => {
  checkLogin()
  loadMembershipPlans()
  loadConfigs()
})
</script>

<template>
  <div class="affiliate-page">
    <!-- Navbar -->
    <nav class="navbar">
      <div class="nav-brand" @click="goHome">
        <img src="https://foruda.gitee.com/images/1776834561924666968/e0f84414_8060302.png" class="nav-logo-img" alt="logo">
        <span class="nav-title">知我公众号创作助手</span>
      </div>
      <div style="display: flex; align-items: center; gap: 16px;">
        <a class="nav-link" @click="goHome">首页</a>
        <button class="login-btn" @click="goLogin">登录</button>
      </div>
    </nav>

    <!-- Hero Banner -->
    <section class="hero-banner">
      <div class="hero-banner-bg"></div>
      <div class="hero-banner-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          限时招募分销合伙人
        </div>
        <h1 class="hero-title">
          分享邀请码<br />
          <span class="hero-highlight">轻松赚取 30% 佣金</span>
        </h1>
        <p class="hero-subtitle">
          无需投入任何成本，只需将您的专属邀请码分享给有公众号创作需求的好友，
          好友开通会员后，您即可获得高达 30% 的佣金返利。收益无上限，多邀多得！
        </p>
        <div class="hero-stats">
          <div class="hero-stat">
            <div class="hero-stat-num">30%</div>
            <div class="hero-stat-label">佣金比例</div>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <div class="hero-stat-num">0</div>
            <div class="hero-stat-label">门槛费用</div>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <div class="hero-stat-num">&infin;</div>
            <div class="hero-stat-label">邀请上限</div>
          </div>
        </div>
      </div>
    </section>

    <!-- How It Works -->
    <section class="section steps-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">参与方式</span>
          <h2 class="section-title">三步轻松赚取佣金</h2>
          <p class="section-subtitle">零门槛、零投入，分享即收益</p>
        </div>
        <div class="steps-grid">
          <div class="step-card">
            <div class="step-num">01</div>
            <div class="step-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#07C160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="8.5" cy="7" r="4"/>
                <polyline points="17 11 19 13 23 9"/>
              </svg>
            </div>
            <h3 class="step-title">获取邀请码</h3>
            <p class="step-desc">登录平台后，在个人中心或本页面获取您的专属邀请码。每位用户拥有唯一的邀请码，永久有效。</p>
          </div>
          <div class="step-card">
            <div class="step-num">02</div>
            <div class="step-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#07C160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="18" cy="5" r="3"/>
                <circle cx="6" cy="12" r="3"/>
                <circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
            </div>
            <h3 class="step-title">分享给好友</h3>
            <p class="step-desc">点击「复制邀请码」，系统会自动生成包含专属注册链接的邀请文案。通过微信、朋友圈、社群等渠道分享给好友，好友点击链接注册，邀请关系自动绑定。</p>
          </div>
          <div class="step-card">
            <div class="step-num">03</div>
            <div class="step-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#07C160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"/>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
              </svg>
            </div>
            <h3 class="step-title">获得佣金</h3>
            <p class="step-desc">好友通过您的邀请码注册并开通任意会员套餐后，您将立即获得好友支付金额的 30% 作为佣金奖励。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Commission Calculation -->
    <section class="section commission-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">收益测算</span>
          <h2 class="section-title">邀请越多，收益越高</h2>
          <p class="section-subtitle">以当前会员套餐为例，每成功邀请一位好友的收益</p>
        </div>
        <div class="commission-grid">
          <div v-for="(item, i) in commissionExamples" :key="i" class="commission-card">
            <div class="commission-name">{{ item.name }}</div>
            <div class="commission-price-row">
              <span class="commission-price">¥{{ item.price }}</span>
              <span class="commission-unit">/ 月</span>
            </div>
            <div class="commission-arrow">&darr;</div>
            <div class="commission-label">您可获得</div>
            <div class="commission-value">¥{{ item.commission }}</div>
            <div class="commission-tag">30% 佣金</div>
          </div>
          <div class="commission-card highlight">
            <div class="commission-name">邀请 10 位好友</div>
            <div class="commission-price-row">
              <span class="commission-price">开通任意套餐</span>
            </div>
            <div class="commission-arrow">&darr;</div>
            <div class="commission-label">预估年收益</div>
            <div class="commission-value">¥2000+</div>
            <div class="commission-tag">被动收入</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Invite Code Section -->
    <section class="section invite-section">
      <div class="section-container">
        <div class="invite-card">
          <div class="invite-left">
            <h2 class="invite-title">您的专属邀请码</h2>
            <p class="invite-desc">将此邀请码分享给好友，好友注册时填写即可建立绑定关系</p>
            <div v-if="currentUser && currentUser.inviteCode" class="invite-code-box">
              <div class="invite-code">{{ currentUser.inviteCode }}</div>
              <button class="invite-copy-btn" @click="copyInviteCode">
                {{ inviteCopied ? '已复制 ✓' : '复制邀请码' }}
              </button>
            </div>
            <div v-else class="invite-login-box">
              <p class="invite-login-text">登录后即可获取您的专属邀请码</p>
              <button class="invite-copy-btn" @click="goLogin">立即登录</button>
            </div>
          </div>
          <div class="invite-right">
            <div class="invite-qr-box">
              <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="客服二维码" class="invite-qr-img" />
              <div v-else class="invite-qr-placeholder">
                <div style="font-size: 13px; color: #94a3b8; text-align: center;">微信客服</div>
                <div style="font-size: 11px; color: #cbd5e1; margin-top: 6px;">扫码了解更多</div>
              </div>
            </div>
            <div class="invite-contact-info">
              <div class="invite-contact-title">联系客服</div>
              <div class="invite-contact-desc">
                添加客服微信了解分销活动详情<br>
                佣金实时结算，提现无门槛
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Rules Section -->
    <section class="section rules-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">活动规则</span>
          <h2 class="section-title">活动细则</h2>
        </div>
        <div class="rules-list">
          <div class="rule-item">
            <div class="rule-num">1</div>
            <div class="rule-content">
              <h4>活动时间</h4>
              <p>本活动长期有效，平台保留随时调整活动规则的权利，调整前将提前通知所有用户。</p>
            </div>
          </div>
          <div class="rule-item">
            <div class="rule-num">2</div>
            <div class="rule-content">
              <h4>参与资格</h4>
              <p>所有注册并登录平台的用户均可参与。无需缴纳任何费用，零门槛加入分销计划。</p>
            </div>
          </div>
          <div class="rule-item">
            <div class="rule-num">3</div>
            <div class="rule-content">
              <h4>佣金比例</h4>
              <p>每成功邀请一位好友开通会员，邀请人可获得好友首次支付金额的 30% 作为佣金。续费订单不参与佣金分成。</p>
            </div>
          </div>
          <div class="rule-item">
            <div class="rule-num">4</div>
            <div class="rule-content">
              <h4>结算周期</h4>
              <p>佣金月结，结算日期为月底。</p>
            </div>
          </div>
          <div class="rule-item">
            <div class="rule-num">5</div>
            <div class="rule-content">
              <h4>邀请关系</h4>
              <p>好友注册时填写您的邀请码即建立绑定关系，绑定关系永久有效。每个账户只能填写一次邀请码。</p>
            </div>
          </div>
          <div class="rule-item">
            <div class="rule-num">6</div>
            <div class="rule-content">
              <h4>违规处理</h4>
              <p>禁止通过刷量、欺诈等不正当手段获取佣金。一经发现，平台有权取消违规收益并冻结账户。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ Section -->
    <section class="section faq-section">
      <div class="section-container">
        <div class="section-header center">
          <span class="section-label">常见问题</span>
          <h2 class="section-title">FAQ</h2>
        </div>
        <div class="faq-list">
          <div v-for="(item, i) in faqs" :key="i" class="faq-item" :class="{ active: activeFaq === i }" @click="toggleFaq(i)">
            <div class="faq-q">
              <span class="faq-q-icon">Q</span>
              <span class="faq-q-text">{{ item.q }}</span>
              <span class="faq-toggle">{{ activeFaq === i ? '−' : '+' }}</span>
            </div>
            <div v-show="activeFaq === i" class="faq-a">
              {{ item.a }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="section cta-section">
      <div class="section-container">
        <div class="cta-box">
          <h2 class="cta-title">立即开始赚取佣金</h2>
          <p class="cta-desc">
            零成本、零风险，只需分享您的邀请码，即可开启被动收入模式
          </p>
          <button v-if="currentUser && currentUser.inviteCode" class="btn-primary large" @click="copyInviteCode">
            {{ inviteCopied ? '已复制 ✓' : '复制邀请码开始分享' }}
          </button>
          <button v-else class="btn-primary large" @click="goLogin">
            登录获取邀请码
            <span class="btn-arrow">→</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-brand">
          <img src="https://foruda.gitee.com/images/1776834561924666968/e0f84414_8060302.png" class="footer-logo-img" alt="logo">
          <span>知我公众号创作助手</span>
        </div>
        <p class="footer-copy">让每个人都能轻松运营公众号</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ========== Base ========== */
.affiliate-page {
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

/* ========== Hero Banner ========== */
.hero-banner {
  position: relative;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120px 48px 80px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #f0fdf4 100%);
  overflow: hidden;
}

.hero-banner-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 50% at 20% 30%, rgba(7, 193, 96, 0.1) 0%, transparent 60%),
    radial-gradient(ellipse 50% 40% at 80% 70%, rgba(7, 193, 96, 0.06) 0%, transparent 50%);
}

.hero-banner-content {
  position: relative;
  z-index: 2;
  max-width: 720px;
  text-align: center;
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
  font-size: 48px;
  font-weight: 800;
  line-height: 1.15;
  color: #1e293b;
  margin-bottom: 20px;
  letter-spacing: -0.02em;
}

.hero-highlight {
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 16px;
  line-height: 1.7;
  color: #64748b;
  margin-bottom: 40px;
  max-width: 560px;
  margin-left: auto;
  margin-right: auto;
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.hero-stat {
  padding: 0 40px;
  text-align: center;
}

.hero-stat-num {
  font-size: 36px;
  font-weight: 800;
  color: #07C160;
  line-height: 1;
  margin-bottom: 8px;
}

.hero-stat-label {
  font-size: 14px;
  color: #64748b;
}

.hero-stat-divider {
  width: 1px;
  height: 48px;
  background: #d1d5db;
}

/* ========== Section Common ========== */
.section {
  padding: 80px 48px;
}

.section-container {
  max-width: 1000px;
  margin: 0 auto;
}

.section-header {
  margin-bottom: 48px;
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
  margin-bottom: 12px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.25;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.section-subtitle {
  font-size: 15px;
  color: #64748b;
  margin-top: 10px;
}

/* ========== Steps Section ========== */
.steps-section {
  background: #fff;
}

.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.step-card {
  padding: 36px 28px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  text-align: center;
  transition: all 0.3s;
}

.step-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
  border-color: #07C160;
}

.step-num {
  font-size: 14px;
  font-weight: 700;
  color: #07C160;
  margin-bottom: 16px;
}

.step-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(7, 193, 96, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 10px;
}

.step-desc {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

/* ========== Commission Section ========== */
.commission-section {
  background: #f8fafc;
}

.commission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.commission-card {
  padding: 28px 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  text-align: center;
  transition: all 0.3s;
}

.commission-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
}

.commission-card.highlight {
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  border-color: #07C160;
  color: #fff;
}

.commission-card.highlight .commission-name {
  color: rgba(255, 255, 255, 0.9);
}

.commission-card.highlight .commission-price {
  color: #fff;
}

.commission-card.highlight .commission-unit {
  color: rgba(255, 255, 255, 0.7);
}

.commission-card.highlight .commission-arrow {
  color: rgba(255, 255, 255, 0.6);
}

.commission-card.highlight .commission-label {
  color: rgba(255, 255, 255, 0.8);
}

.commission-card.highlight .commission-value {
  color: #fff;
}

.commission-card.highlight .commission-tag {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.commission-name {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}

.commission-price-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 12px;
}

.commission-price {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.commission-unit {
  font-size: 13px;
  color: #9ca3af;
}

.commission-arrow {
  font-size: 20px;
  color: #cbd5e1;
  margin: 8px 0;
}

.commission-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.commission-value {
  font-size: 32px;
  font-weight: 800;
  color: #07C160;
  line-height: 1;
  margin-bottom: 12px;
}

.commission-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  background: rgba(7, 193, 96, 0.1);
  color: #07C160;
  font-size: 12px;
  font-weight: 500;
}

/* ========== Invite Section ========== */
.invite-section {
  background: #fff;
}

.invite-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
  padding: 48px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f0fdf4 0%, #f6fef9 100%);
  border: 1px solid #bbf7d0;
}

.invite-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.invite-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 24px;
}

.invite-code-box {
  text-align: center;
  padding: 24px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.invite-code {
  font-family: monospace;
  font-size: 28px;
  font-weight: 700;
  color: #07C160;
  letter-spacing: 4px;
  margin-bottom: 16px;
}

.invite-login-box {
  text-align: center;
  padding: 24px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.invite-login-text {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 16px;
}

.invite-copy-btn {
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

.invite-copy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(7, 193, 96, 0.4);
}

.invite-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.invite-qr-box {
  flex-shrink: 0;
}

.invite-qr-img {
  width: 140px;
  height: auto;
  max-height: 200px;
  border-radius: 12px;
  object-fit: contain;
  border: 1px solid #e2e8f0;
  display: block;
}

.invite-qr-placeholder {
  width: 140px;
  height: 140px;
  border-radius: 12px;
  border: 2px dashed #cbd5e1;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.invite-contact-info {
  flex: 1;
}

.invite-contact-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.invite-contact-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
}

/* ========== Rules Section ========== */
.rules-section {
  background: #f8fafc;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-item {
  display: flex;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.rule-item:hover {
  border-color: #07C160;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.08);
}

.rule-num {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #07C160 0%, #06AD56 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rule-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.rule-content p {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
  margin: 0;
}

/* ========== FAQ Section ========== */
.faq-section {
  background: #fff;
}

.faq-list {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-item {
  padding: 20px 24px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
}

.faq-item:hover {
  border-color: #07C160;
}

.faq-item.active {
  border-color: #07C160;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.08);
}

.faq-q {
  display: flex;
  align-items: center;
  gap: 12px;
}

.faq-q-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: rgba(7, 193, 96, 0.1);
  color: #07C160;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.faq-q-text {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
}

.faq-toggle {
  font-size: 20px;
  color: #07C160;
  font-weight: 300;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.faq-a {
  margin-top: 12px;
  padding-left: 36px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

/* ========== CTA Section ========== */
.cta-section {
  background: #f8fafc;
  padding: 80px 48px;
}

.cta-box {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
  padding: 56px 48px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f0fdf4 0%, #f6fef9 100%);
  border: 1px solid #bbf7d0;
}

.cta-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
}

.cta-desc {
  font-size: 15px;
  line-height: 1.7;
  color: #64748b;
  margin-bottom: 28px;
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

/* ========== Footer ========== */
.footer {
  background: #f8fafc;
  padding: 40px 48px;
  border-top: 1px solid #e2e8f0;
}

.footer-content {
  max-width: 1000px;
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

  .hero-banner {
    min-height: auto;
    padding: 100px 24px 60px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .hero-stat {
    padding: 0 20px;
  }

  .hero-stat-num {
    font-size: 28px;
  }

  .section {
    padding: 60px 24px;
  }

  .section-title {
    font-size: 24px;
  }

  .steps-grid {
    grid-template-columns: 1fr;
  }

  .commission-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .invite-card {
    grid-template-columns: 1fr;
    padding: 28px;
  }

  .invite-right {
    flex-direction: column;
    text-align: center;
  }

  .rule-item {
    padding: 20px;
  }

  .faq-a {
    padding-left: 0;
  }

  .cta-box {
    padding: 40px 24px;
  }

  .footer-content {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .hero-stats {
    flex-direction: column;
    gap: 16px;
  }

  .hero-stat-divider {
    display: none;
  }

  .commission-grid {
    grid-template-columns: 1fr;
  }

  .invite-code {
    font-size: 22px;
  }
}
</style>
