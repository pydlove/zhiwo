<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { login } from '../api/auth.js'
import { getConfigs } from '../api/config.js'

const router = useRouter()
const { isMobile } = useViewport()
const form = ref({ account: '', password: '', captcha: '' })
const captchaCode = ref('A7K9')
const loading = ref(false)

const systemName = ref('Aicloud')
const logoUrl = ref('')
const qrCodeUrl = ref('')
const qrPreviewOpen = ref(false)

async function loadConfigs() {
  try {
    const data = await getConfigs()
    if (data) {
      systemName.value = data.systemName || 'Aicloud'
      logoUrl.value = data.logoUrl || ''
      qrCodeUrl.value = data.qrCodeUrl || ''
      if (data.systemName) {
        document.title = `${data.systemName} - 登录`
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
    message.success('登录成功')
    router.push('/app/home')
  } catch (e) {
    message.error(e.message || '账号或密码错误')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
  loadConfigs()
})
</script>

<template>
  <div :style="{ minHeight: '100vh', display: 'flex', flexDirection: isMobile ? 'column' : 'row', fontFamily: '-apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, \'Helvetica Neue\', Arial, sans-serif' }">
    <!-- Left -->
    <div v-if="!isMobile" style="flex: 1; background: linear-gradient(160deg, #07C160 0%, #06AD56 100%); display: flex; flex-direction: column; justify-content: center; padding: 64px; color: #fff;">
      <div style="width: 80px; height: 80px; border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 32px; margin-bottom: 1px;">
        <img src="../assets/images/logo-green1.png" style="width: 64px; height: 64px; object-fit: contain; border-radius: 8px;">
      </div>
      <h1 style="font-size: 40px; font-weight: 700; margin-bottom: 20px; line-height: 1.2;">发现热门赛道<br/>让 AI 帮你创作爆款内容</h1>
      <p style="font-size: 18px; opacity: 0.9; line-height: 1.6; max-width: 420px;">汇聚公众号、今日头条头部博主数据，基于爆款文章特征，AI 实时生成创作方向和优质内容。</p>
      <div style="margin-top: 48px; display: flex; flex-direction: column; gap: 20px;">
        <div style="display: flex; align-items: center; gap: 14px; font-size: 15px; opacity: 0.95;">
          <img src="../assets/images/saidao.png" style="width: 22px; height: 22px; object-fit: cover; border-radius: 8px;">
          <span>覆盖 36+ 热门赛道，200+ 头部博主数据</span>
        </div>
        <div style="display: flex; align-items: center; gap: 14px; font-size: 15px; opacity: 0.95;">
          <img src="../assets/images/ai.png" style="width: 22px; height: 22px; object-fit: cover; border-radius: 8px;">
          <span>AI 智能创作，每日推荐你对应赛道的文章</span>
        </div>
        <!-- <div style="display: flex; align-items: center; gap: 14px; font-size: 15px; opacity: 0.95;">
          <div style="width: 36px; height: 36px; background: rgba(255,255,255,0.15); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 16px;">📝</div>
          <span>半人工 / 零人工创作，一键导出 Word</span>
        </div> -->
      </div>
    </div>

    <!-- Right -->
    <div :style="{ width: isMobile ? '100%' : '480px', background: '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: isMobile ? '32px 24px' : '64px', boxShadow: isMobile ? 'none' : '-20px 0 60px rgba(0,0,0,0.06)' }">
      <div style="font-size: 24px; font-weight: 700; color: #111827; display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
        <div style="width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: 700;">
          <img v-if="logoUrl" :src="logoUrl" style="width: 36px; height: 36px; object-fit: cover; border-radius: 4px;">
          <span v-else>AI</span>
        </div>
        {{ systemName }}
      </div>
      <div style="font-size: 14px; color: #6b7280; margin-bottom: 40px;">管理员开通账号后登录使用</div>

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

      <div style="margin-top: 48px; padding: 16px; background: #f8fafc; border-radius: 10px; text-align: center; font-size: 13px; color: #6b7280; border: 1px solid #f1f5f9;">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" @click="qrPreviewOpen = true" style="max-width: 140px; max-height: 140px; border-radius: 8px; margin: 0 auto 12px; object-fit: contain; cursor: pointer;">
        <div v-else style="width: 140px; height: 140px; background: #f3f4f6; border-radius: 8px; margin: 0 auto 12px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #9ca3af;">客服二维码</div>
        <div style="font-size: 13px; color: #374151; margin-bottom: 4px;">扫码联系管理员开通账号</div>
        <div style="font-size: 12px; color: #9ca3af;">工作时间：周一至周五 9:00-18:00</div>
      </div>
    </div>
  </div>

  <!-- QR Preview Overlay -->
  <div v-if="qrPreviewOpen" @click="qrPreviewOpen = false" style="position: fixed; inset: 0; background: rgba(0,0,0,0.75); display: flex; align-items: center; justify-content: center; z-index: 9999; cursor: zoom-out;">
    <img :src="qrCodeUrl" style="max-width: 80vw; max-height: 80vh; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); object-fit: contain;">
  </div>
</template>
