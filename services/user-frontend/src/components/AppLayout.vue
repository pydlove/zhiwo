<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Tooltip } from 'ant-design-vue'
import { usePlatformStore } from '../stores/platform.js'
import { useViewport } from '../composables/useViewport.js'
import { usePermissions } from '../composables/usePermissions.js'
import request from '../api/request.js'
import { getConfigs } from '../api/config.js'

const route = useRoute()
const router = useRouter()
const platformStore = usePlatformStore()
const { isMobile } = useViewport()
const { plan, planData, loadPermissions, allowedPlatforms, trackLimit, canEmailPush, canOnlinePreview, canGuideAccess } = usePermissions()

const activeKey = computed(() => route.path)
const menuOpen = ref(false)
const mobileMenuOpen = ref(false)

const systemName = ref('Aicloud')
const logoUrl = ref('')

const platforms = platformStore.platforms

const navs = [
  { key: '/app/home', label: '首页' },
  { key: '/app/create', label: '订阅中心' },
  { key: '/app/help', label: '帮助文档' },
  { key: '/app/tips', label: '创作技巧' },
]

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const expireDate = computed(() => user.value.expireDate || '2026-12-31')
const planName = computed(() => plan.value?.name || '')

const planTagStyle = computed(() => {
  const name = planName.value
  if (name.includes('旗舰')) {
    return { background: '#fef3c7', border: '1px solid #fde68a', color: '#b45309' }
  }
  if (name.includes('专业')) {
    return { background: '#f3e8ff', border: '1px solid #d8b4fe', color: '#7c3aed' }
  }
  if (name.includes('标准')) {
    return { background: '#dbeafe', border: '1px solid #93c5fd', color: '#2563eb' }
  }
  // 基础版 / 默认
  return { background: '#f3f4f6', border: '1px solid #d1d5db', color: '#4b5563' }
})

function parseFeaturesJson(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

const planTooltipItems = computed(() => {
  // 优先展示套餐配置的权益列表
  const features = parseFeaturesJson(plan.value?.featuresJson)
  if (features.length > 0) {
    return features
  }
  // 兜底：手动拼接
  const platforms = allowedPlatforms.value.join('、') || '-'
  const track = trackLimit.value > 0 ? `${trackLimit.value} 个` : '不限'
  const items = [`可用平台：${platforms}`, `赛道上限：${track}`]
  if (canEmailPush.value) items.push('邮件每日推送')
  if (canOnlinePreview.value) items.push('在线预览文章')
  if (canGuideAccess.value) items.push('创作技巧学习')
  return items
})

onMounted(() => {
  loadConfigs()
  if (user.value.id) {
    loadPermissions(user.value.id)
  }
})

const isExpired = computed(() => {
  const ed = expireDate.value
  if (!ed) return false
  return new Date(ed + 'T23:59:59') < new Date()
})

const creationPaths = ['/app/create', '/app/drafts', '/app/creations']

function navTo(path) {
  if (creationPaths.includes(path) && isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return
  }
  if (path === '/app/tips' && !canGuideAccess.value) {
    message.warning('您当前的权益暂不支持访问创作技巧页面')
    return
  }
  router.push(path)
}

function logout() {
  router.push('/login')
}

async function handlePlatformClick(p) {
  const uid = user.value.id
  if (uid) {
    try {
      const latest = await request.get('/users/' + uid)
      if (latest) {
        localStorage.setItem('user', JSON.stringify(latest))
      }
      await loadPermissions(uid)
    } catch (e) {
      // ignore fetch error, fallback to localStorage
    }
  }
  const ok = platformStore.setPlatform(p)
  if (!ok) {
    const allowed = (platformStore.allowedPlatforms || []).join('、')
    message.warning(`您当前的权益仅支持访问${allowed}平台，如需更多请联系管理员`)
  }
}

async function loadConfigs() {
  try {
    const data = await getConfigs()
    if (data) {
      systemName.value = data.systemName || 'Aicloud'
      logoUrl.value = data.logoUrl || ''
    }
  } catch (e) {
    // ignore
  }
}

// removed duplicate onMounted, merged above
</script>

<template>
  <div style="min-height: 100vh; background: #f8fafc;" @click="menuOpen = false">
    <!-- Desktop Nav -->
    <nav v-if="!isMobile" style="background: #fff; border-bottom: 1px solid #e5e7eb; padding: 0 24px; height: 56px; display: flex; align-items: center; justify-content: space-between;">
      <div style="display: flex; align-items: center; gap: 28px;">
        <div style="font-size: 18px; font-weight: 700; color: #111827; display: flex; align-items: center; gap: 8px; cursor: pointer;" @click="navTo('/app/home')">
          <div style="width: 35px; height: 33px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 700; overflow: hidden;">
            <img v-if="logoUrl" :src="logoUrl" style="width: 35px; height: 33px;">
            <span v-else>AI</span>
          </div>
          {{ systemName }}
        </div>
        <div style="display: flex; gap: 24px; font-size: 14px; color: #4b5563;">
          <a
            v-for="n in navs"
            :key="n.key"
            @click.stop="navTo(n.key)"
            style="text-decoration: none; color: inherit; font-weight: 500; cursor: pointer;"
            :style="activeKey === n.key || activeKey.startsWith(n.key + '/') ? { color: '#2563eb' } : {}"
          >{{ n.label }}</a>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 12px;">
        <Tooltip v-if="planName" placement="bottom" overlay-class-name="plan-tooltip">
          <div
            :style="{ display: 'flex', alignItems: 'center', gap: '6px', padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: 500, cursor: 'pointer', ...planTagStyle }"
          >
            <img src="../assets/images/VIP.png" style="width: 16px; height: 16px;">
            <span>{{ planName }}</span>
          </div>
          <template #title>
            <div style="max-width: 260px;">
              <div style="font-size: 14px; font-weight: 600; color: #fff; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid rgba(255,255,255,0.2);">
                {{ planName }}
              </div>
              <div style="display: flex; flex-direction: column; gap: 6px;">
                <div v-for="(item, i) in planTooltipItems" :key="i" style="display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: rgba(255,255,255,0.92); line-height: 1.5;">
                  <span style="color: #86efac; font-size: 13px; line-height: 1.5; flex-shrink: 0;">✓</span>
                  <span>{{ item }}</span>
                </div>
              </div>
              <div v-if="expireDate && expireDate !== '2026-12-31'" style="margin-top: 10px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.2); font-size: 11px; color: rgba(255,255,255,0.7); display: flex; align-items: center; gap: 4px;">
                <span style="width: 6px; height: 6px; border-radius: 50%;" :style="{ background: isExpired ? '#f87171' : '#4ade80' }"></span>
                有效期至 {{ expireDate }}{{ isExpired ? ' (已到期)' : '' }}
              </div>
            </div>
          </template>
        </Tooltip>
        <div
          style="display: flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 500;"
          :style="isExpired
            ? { background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c' }
            : { background: '#f0fdf4', border: '1px solid #bbf7d0', color: '#15803d' }"
        >
          <span style="width: 6px; height: 6px; border-radius: 50%;" :style="{ background: isExpired ? '#ef4444' : '#22c55e' }"></span>
          <span>有效期至 {{ expireDate }}{{ isExpired ? ' 已到期' : '' }}</span>
        </div>
        <div style="display: flex; background: #f3f4f6; border-radius: 6px; padding: 3px; gap: 2px;">
          <span
            v-for="p in platforms"
            :key="p"
            @click="handlePlatformClick(p)"
            style="padding: 4px 10px; font-size: 12px; border-radius: 4px; cursor: pointer; font-weight: 500; transition: all 0.15s;"
            :style="platformStore.current === p
              ? { background: '#fff', color: '#2563eb', boxShadow: '0 1px 2px rgba(0,0,0,0.06)' }
              : { color: '#6b7280' }"
          >{{ p }}</span>
        </div>
        <div style="position: relative;" @click.stop>
          <div @click="menuOpen = !menuOpen" style="width: 32px; height: 32px; border-radius: 50%; background: #e5e7eb; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #6b7280; cursor: pointer; overflow: hidden;">
            <img v-if="user.avatar" :src="user.avatar" style="width: 100%; height: 100%; object-fit: cover;">
            <span v-else>U</span>
          </div>
          <div v-if="menuOpen" style="position: absolute; top: 40px; right: 0; background: #fff; border: 1px solid #f1f5f9; border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); min-width: 160px; padding: 6px; z-index: 50;">
            <div style="padding: 8px 14px; font-size: 12px; color: #6b7280;">
              <div v-if="planName" style="color: #2563eb; font-weight: 500; margin-bottom: 2px;">{{ planName }}</div>
              <div>有效期至</div>
              <div style="color: #f59e0b; font-weight: 500;">{{ expireDate }}</div>
            </div>
            <div style="height: 1px; background: #f1f5f9; margin: 6px 0;"></div>
            <div @click="navTo('/app/profile')" style="padding: 10px 14px; font-size: 13px; color: #374151; border-radius: 6px; cursor: pointer; transition: background 0.15s;" @mouseenter="$event.currentTarget.style.background = '#f8fafc'" @mouseleave="$event.currentTarget.style.background = 'transparent'">个人中心</div>
            <div style="padding: 10px 14px; font-size: 13px; color: #374151; border-radius: 6px; cursor: pointer; transition: background 0.15s;" @mouseenter="$event.currentTarget.style.background = '#f8fafc'" @mouseleave="$event.currentTarget.style.background = 'transparent'">密码设置</div>
            <div style="height: 1px; background: #f1f5f9; margin: 6px 0;"></div>
            <div @click="logout" style="padding: 10px 14px; font-size: 13px; color: #374151; border-radius: 6px; cursor: pointer; transition: background 0.15s;" @mouseenter="$event.currentTarget.style.background = '#f8fafc'" @mouseleave="$event.currentTarget.style.background = 'transparent'">退出登录</div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Mobile Nav -->
    <nav v-else style="background: #fff; border-bottom: 1px solid #e5e7eb; padding: 0 16px; height: 52px; display: flex; align-items: center; justify-content: space-between;">
      <div style="font-size: 17px; font-weight: 700; color: #111827; display: flex; align-items: center; gap: 8px; cursor: pointer;" @click="navTo('/app/home')">
        <div style="width: 26px; height: 26px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 11px; font-weight: 700; overflow: hidden;">
          <img v-if="logoUrl" :src="logoUrl" style="width: 100%; height: 100%; object-fit: cover;">
          <span v-else>AI</span>
        </div>
        {{ systemName }}
      </div>
      <div style="display: flex; align-items: center; gap: 10px;">
        <div style="display: flex; background: #f3f4f6; border-radius: 6px; padding: 2px; gap: 2px;">
          <span
            v-for="p in platforms"
            :key="p"
            @click="handlePlatformClick(p)"
            style="padding: 3px 8px; font-size: 11px; border-radius: 4px; cursor: pointer; font-weight: 500; transition: all 0.15s;"
            :style="platformStore.current === p
              ? { background: '#fff', color: '#2563eb', boxShadow: '0 1px 2px rgba(0,0,0,0.06)' }
              : { color: '#6b7280' }"
          >{{ p }}</span>
        </div>
        <div @click.stop="mobileMenuOpen = !mobileMenuOpen" style="width: 32px; height: 32px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; cursor: pointer;">
          <span style="display: block; width: 18px; height: 2px; background: #374151; border-radius: 1px; transition: all 0.2s;" :style="mobileMenuOpen ? { transform: 'rotate(45deg) translate(4px, 4px)' } : {}"></span>
          <span style="display: block; width: 18px; height: 2px; background: #374151; border-radius: 1px; transition: all 0.2s;" :style="mobileMenuOpen ? { opacity: 0 } : {}"></span>
          <span style="display: block; width: 18px; height: 2px; background: #374151; border-radius: 1px; transition: all 0.2s;" :style="mobileMenuOpen ? { transform: 'rotate(-45deg) translate(4px, -4px)' } : {}"></span>
        </div>
      </div>
    </nav>

    <!-- Mobile Menu Drawer -->
    <div v-if="isMobile && mobileMenuOpen" style="position: fixed; inset: 52px 0 0 0; background: rgba(0,0,0,0.4); z-index: 40;" @click="mobileMenuOpen = false">
      <div style="background: #fff; border-bottom: 1px solid #e5e7eb; padding: 12px 16px 20px;" @click.stop>
        <div style="display: flex; flex-direction: column; gap: 4px;">
          <div
            v-for="n in navs"
            :key="n.key"
            @click="mobileMenuOpen = false; navTo(n.key)"
            style="padding: 12px 8px; font-size: 15px; font-weight: 500; color: #374151; border-radius: 8px; cursor: pointer;"
            :style="activeKey === n.key || activeKey.startsWith(n.key + '/') ? { color: '#2563eb', background: '#eff6ff' } : {}"
          >{{ n.label }}</div>
          <div style="height: 1px; background: #f1f5f9; margin: 6px 0;"></div>
          <div @click="mobileMenuOpen = false; navTo('/app/profile')" style="padding: 12px 8px; font-size: 15px; font-weight: 500; color: #374151; border-radius: 8px; cursor: pointer;">个人中心</div>
          <div @click="mobileMenuOpen = false; logout()" style="padding: 12px 8px; font-size: 15px; font-weight: 500; color: #374151; border-radius: 8px; cursor: pointer;">退出登录</div>
        </div>
        <div style="margin-top: 12px; padding-top: 12px; border-top: 1px solid #f1f5f9; display: flex; align-items: center; gap: 6px; font-size: 12px; color: #6b7280;">
          <span style="width: 6px; height: 6px; border-radius: 50%;" :style="{ background: isExpired ? '#ef4444' : '#22c55e' }"></span>
          <Tooltip v-if="planName" placement="bottom" overlay-class-name="plan-tooltip">
            <span :style="{ color: planTagStyle.color, fontWeight: 500, cursor: 'pointer' }">{{ planName }}</span>
            <template #title>
              <div style="max-width: 260px;">
                <div style="font-size: 14px; font-weight: 600; color: #fff; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid rgba(255,255,255,0.2);">
                  {{ planName }}
                </div>
                <div style="display: flex; flex-direction: column; gap: 6px;">
                  <div v-for="(item, i) in planTooltipItems" :key="i" style="display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: rgba(255,255,255,0.92); line-height: 1.5;">
                    <span style="color: #86efac; font-size: 13px; line-height: 1.5; flex-shrink: 0;">✓</span>
                    <span>{{ item }}</span>
                  </div>
                </div>
                <div v-if="expireDate && expireDate !== '2026-12-31'" style="margin-top: 10px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.2); font-size: 11px; color: rgba(255,255,255,0.7); display: flex; align-items: center; gap: 4px;">
                  <span style="width: 6px; height: 6px; border-radius: 50%;" :style="{ background: isExpired ? '#f87171' : '#4ade80' }"></span>
                  有效期至 {{ expireDate }}{{ isExpired ? ' (已到期)' : '' }}
                </div>
              </div>
            </template>
          </Tooltip>
          <span>有效期至 {{ expireDate }}{{ isExpired ? ' 已到期' : '' }}</span>
        </div>
      </div>
    </div>

    <main :style="{ padding: isMobile ? '12px' : '24px' }">
      <router-view />
    </main>
  </div>
</template>

<style>
.plan-tooltip .ant-tooltip-inner {
  white-space: pre-line;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.8;
  border-radius: 8px;
}
</style>
