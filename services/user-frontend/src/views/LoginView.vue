<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { usePermissions } from '../composables/usePermissions.js'
import { login, register } from '../api/auth.js'
import { getConfigs } from '../api/config.js'

const router = useRouter()
const route = useRoute()
const { isMobile } = useViewport()
const { loadPermissions } = usePermissions()

const mode = ref('login')
const form = ref({ account: '', password: '', confirmPassword: '', captcha: '', inviteCode: '' })
const captchaCode = ref('A7K9')
const loading = ref(false)

const systemName = ref('知我公众号创作助手')
const logoUrl = ref('')
const qrCodeUrl = ref('')
const qrPreviewOpen = ref(false)
const registerSuccessOpen = ref(false)
const registeredUser = ref(null)

async function loadConfigs() {
  try {
    const data = await getConfigs()
    if (data) {
      systemName.value = data.systemName || '知我公众号创作助手'
      logoUrl.value = data.logoUrl || ''
      qrCodeUrl.value = data.qrCodeUrl || ''
      const title = mode.value === 'login' ? '登录' : '注册'
      if (data.systemName) {
        document.title = `${data.systemName} - ${title}`
      }
    }
  } catch (e) {
    // ignore
  }
}

function refreshCaptcha() {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) code += chars[Math.floor(Math.random() * chars.length)]
  captchaCode.value = code
}

function switchMode(m) {
  mode.value = m
  refreshCaptcha()
  const title = m === 'login' ? '登录' : '注册'
  document.title = `${systemName.value} - ${title}`
}

async function handleLogin() {
  if (!form.value.account || !form.value.password || !form.value.captcha) {
    message.warning('请填写完整信息')
    return
  }
  if (form.value.captcha.toUpperCase() !== captchaCode.value) {
    message.warning('验证码错误')
    refreshCaptcha()
    return
  }
  loading.value = true
  try {
    const data = await login({ username: form.value.account, password: form.value.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    localStorage.removeItem('plan')
    if (data.user?.id) {
      await loadPermissions(data.user.id)
    }
    message.success('登录成功')
    router.push('/app/home')
  } catch (e) {
    message.error(e.message || '账号或密码错误')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!form.value.account || !form.value.password || !form.value.confirmPassword || !form.value.captcha) {
    message.warning('请填写完整信息')
    return
  }
  if (form.value.captcha.toUpperCase() !== captchaCode.value) {
    message.warning('验证码错误')
    refreshCaptcha()
    return
  }
  if (form.value.password.length < 6) {
    message.warning('密码长度不能少于6位')
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const data = await register({
      username: form.value.account,
      password: form.value.password,
      inviteCode: form.value.inviteCode,
    })
    registeredUser.value = data.user
    message.success('注册成功')
    registerSuccessOpen.value = true
  } catch (e) {
    message.error(e.message || '注册失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function closeRegisterSuccess() {
  registerSuccessOpen.value = false
  switchMode('login')
  form.value = { account: '', password: '', confirmPassword: '', captcha: '', inviteCode: '' }
}

onMounted(() => {
  refreshCaptcha()
  loadConfigs()
  const invite = route.query.invite
  if (invite) {
    form.value.inviteCode = invite
  }
})
</script>

<template>
  <div :style="{ minHeight: '100vh', display: 'flex', flexDirection: isMobile ? 'column' : 'row', fontFamily: '-apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, \'Helvetica Neue\', Arial, sans-serif' }">
    <!-- Left -->
    <div v-if="!isMobile" style="flex: 1; background: linear-gradient(160deg, #07C160 0%, #06AD56 100%); display: flex; flex-direction: column; justify-content: center; padding: 64px; color: #fff;">
      <div style="width: 80px; height: 80px; border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 32px; margin-bottom: 1px;">
        <img src="https://foruda.gitee.com/images/1776841720416623884/f00aea60_8060302.png" style="width: 64px; height: 64px; object-fit: contain; border-radius: 8px;">
      </div>
      <h1 style="font-size: 40px; font-weight: 700; margin-bottom: 20px; line-height: 1.2;">发现热门赛道<br/>让 AI 帮你创作爆款内容</h1>
      <p style="font-size: 18px; opacity: 0.9; line-height: 1.6; max-width: 420px;">汇聚公众号、今日头条头部博主数据，基于爆款文章特征，AI 每日给您推荐爆款文章。</p>
      <div style="margin-top: 48px; display: flex; flex-direction: column; gap: 20px;">
        <div style="display: flex; align-items: center; gap: 14px; font-size: 15px; opacity: 0.95;">
          <img src="../assets/images/saidao.png" style="width: 22px; height: 22px; object-fit: cover; border-radius: 8px;">
          <span>覆盖 36+ 热门赛道，200+ 头部博主数据</span>
        </div>
        <div style="display: flex; align-items: center; gap: 14px; font-size: 15px; opacity: 0.95;">
          <img src="../assets/images/ai.png" style="width: 22px; height: 22px; object-fit: cover; border-radius: 8px;">
          <span>AI 智能创作，每日推荐你对应赛道的文章</span>
        </div>
      </div>
    </div>

    <!-- Right -->
    <div :style="{ width: isMobile ? '100%' : '480px', background: '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: isMobile ? '32px 24px' : '64px', boxShadow: isMobile ? 'none' : '-20px 0 60px rgba(0,0,0,0.06)' }">
      <div style="font-size: 24px; font-weight: 700; color: #111827; display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
        <div style="width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: 700;">
          <img v-if="logoUrl" :src="logoUrl" style="width: 41px; height: 37px; object-fit: cover; border-radius: 4px;">
          <span v-else>AI</span>
        </div>
        {{ systemName }}
      </div>
      <div style="font-size: 14px; color: #6b7280; margin-bottom: 32px; display: flex; justify-content: space-between; align-items: center;">
        <span>{{ mode === 'login' ? '登录您的账号开始使用' : '创建新账号开始创作' }}</span>
        <a @click="router.push('/')" style="color: #07C160; text-decoration: none; font-weight: 500; cursor: pointer; font-size: 13px;">← 返回首页</a>
      </div>

      <!-- Tabs -->
      <div style="display: flex; gap: 0; margin-bottom: 28px; border-bottom: 1px solid #e5e7eb;">
        <div @click="switchMode('login')" :style="{ padding: '10px 20px', fontSize: '15px', fontWeight: 600, cursor: 'pointer', borderBottom: mode === 'login' ? '2px solid #07C160' : '2px solid transparent', color: mode === 'login' ? '#07C160' : '#6b7280', transition: 'all 0.2s' }">
          登录
        </div>
        <div @click="switchMode('register')" :style="{ padding: '10px 20px', fontSize: '15px', fontWeight: 600, cursor: 'pointer', borderBottom: mode === 'register' ? '2px solid #07C160' : '2px solid transparent', color: mode === 'register' ? '#07C160' : '#6b7280', transition: 'all 0.2s' }">
          注册
        </div>
      </div>

      <!-- Login Form -->
      <template v-if="mode === 'login'">
        <div style="margin-bottom: 20px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">账号</label>
          <input v-model="form.account" type="text" placeholder="手机号 / 邮箱 / 用户名" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 20px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 20px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">验证码</label>
          <div style="display: flex; gap: 12px;">
            <input v-model="form.captcha" type="text" placeholder="请输入验证码" style="flex: 1; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
            <div @click="refreshCaptcha" style="width: 110px; height: 46px; background: #f3f4f6; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-style: italic; letter-spacing: 3px; color: #4b5563; font-weight: 600; border: 1px solid #e5e7eb; cursor: pointer;">{{ captchaCode }}</div>
          </div>
        </div>

        <button @click="handleLogin" :disabled="loading" style="width: 100%; padding: 14px; background: #07C160; color: #fff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px;">{{ loading ? '登录中...' : '登 录' }}</button>

        <div style="margin-top: 24px; display: flex; justify-content: space-between; font-size: 13px; color: #6b7280;">
          <a style="color: #07C160; text-decoration: none; font-weight: 500; cursor: pointer;">忘记密码？</a>
          <span>请联系管理员开户</span>
        </div>
      </template>

      <!-- Register Form -->
      <template v-else>
        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">用户名</label>
          <input v-model="form.account" type="text" placeholder="请输入用户名" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">密码</label>
          <input v-model="form.password" type="password" placeholder="至少6位字符" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">确认密码</label>
          <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">邀请码（选填）</label>
          <input v-model="form.inviteCode" type="text" placeholder="如有邀请码请填写" style="width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
        </div>

        <div style="margin-bottom: 20px;">
          <label style="display: block; font-size: 14px; font-weight: 500; color: #374151; margin-bottom: 6px;">验证码</label>
          <div style="display: flex; gap: 12px;">
            <input v-model="form.captcha" type="text" placeholder="请输入验证码" style="flex: 1; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 15px; outline: none; box-sizing: border-box;" />
            <div @click="refreshCaptcha" style="width: 110px; height: 46px; background: #f3f4f6; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-style: italic; letter-spacing: 3px; color: #4b5563; font-weight: 600; border: 1px solid #e5e7eb; cursor: pointer;">{{ captchaCode }}</div>
          </div>
        </div>

        <button @click="handleRegister" :disabled="loading" style="width: 100%; padding: 14px; background: #07C160; color: #fff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px;">{{ loading ? '注册中...' : '注 册' }}</button>

        <div style="margin-top: 20px; font-size: 13px; color: #6b7280; text-align: center;">
          注册即表示同意 <a style="color: #07C160; text-decoration: none;">用户协议</a>
        </div>
      </template>

      <div style="margin-top: 48px; padding: 16px; background: #f8fafc; border-radius: 10px; text-align: center; font-size: 13px; color: #6b7280; border: 1px solid #f1f5f9;">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" @click="qrPreviewOpen = true" style="max-width: 140px; max-height: 140px; border-radius: 8px; margin: 0 auto 12px; object-fit: contain; cursor: pointer;">
        <div v-else style="width: 140px; height: 140px; background: #f3f4f6; border-radius: 8px; margin: 0 auto 12px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #9ca3af;">客服二维码</div>
        <div style="font-size: 13px; color: #374151; margin-bottom: 4px;">扫码联系客服</div>
        <div style="font-size: 12px; color: #9ca3af;">工作时间：9:00-21:00</div>
      </div>
    </div>
  </div>

  <!-- QR Preview Overlay -->
  <div v-if="qrPreviewOpen" @click="qrPreviewOpen = false" style="position: fixed; inset: 0; background: rgba(0,0,0,0.75); display: flex; align-items: center; justify-content: center; z-index: 9999; cursor: zoom-out;">
    <img :src="qrCodeUrl" style="max-width: 80vw; max-height: 80vh; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); object-fit: contain;">
  </div>

  <!-- Register Success Modal -->
  <div v-if="registerSuccessOpen" style="position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 9999;">
    <div style="background: #fff; border-radius: 16px; padding: 40px 32px; max-width: 380px; width: 90%; text-align: center; box-shadow: 0 20px 60px rgba(0,0,0,0.2);">
      <div style="width: 56px; height: 56px; background: rgba(7,193,96,0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px;">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#07C160" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
      <h3 style="font-size: 20px; font-weight: 700; color: #1e293b; margin-bottom: 12px;">注册成功</h3>
      <p style="font-size: 14px; color: #64748b; line-height: 1.7; margin-bottom: 8px;">
        欢迎来到知我公众号创作助手！
      </p>
      <div style="background: #fefce8; border: 1px solid #fde047; border-radius: 12px; padding: 16px; margin: 20px 0;">
        <p style="font-size: 13px; color: #854d0e; margin: 0 0 8px 0; font-weight: 500;">账号审核中</p>
        <p style="font-size: 12px; color: #a16207; margin: 0; line-height: 1.6;">您的账号已注册成功，目前处于待审核状态。请添加下方客服微信，发送您的用户名即可完成审核。审核通过后，您可以使用账号密码登录平台。</p>
      </div>
      <img v-if="qrCodeUrl" :src="qrCodeUrl" style="max-width: 160px; max-height: 160px; border-radius: 8px; margin-bottom: 24px; object-fit: contain;">
      <button @click="closeRegisterSuccess" style="width: 100%; padding: 12px; background: #07C160; color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer;">我知道了</button>
    </div>
  </div>
</template>
