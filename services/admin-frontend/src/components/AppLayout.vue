<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Layout, Avatar, Modal, Form, Input, message, Dropdown } from 'ant-design-vue'
import request from '../api/request.js'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => route.meta?.title || '管理后台')

const adminUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('admin-user') || '{}')
  } catch (e) {
    return {}
  }
})

const adminPerms = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('admin-perms') || '[]')
  } catch (e) {
    return []
  }
})

const isSuperAdmin = computed(() => adminPerms.value.includes('all'))

function hasPerm(perm) {
  if (isSuperAdmin.value) return true
  return adminPerms.value.includes(perm)
}

const allMenuItems = [
  { key: '/dashboard', label: '仪表盘', perm: 'dashboard' },
  { key: '/tracks', label: '赛道管理', perm: 'track' },
  { key: '/bloggers', label: '博主管理', perm: 'blogger' },
  { key: '/posts', label: '文章管理', perm: 'post' },
  { key: '/users', label: '用户管理', perm: 'user' },
  { key: '/guides', label: '创作技巧', perm: 'guide' },
  { key: '/helps', label: '帮助文档', perm: 'help' },
  { key: '/styles', label: '样式管理', perm: 'style' },
  { key: '/subscription-posts', label: '订阅文章', perm: 'subscription-post' },
  { key: '/admins', label: '管理员管理', perm: 'admin' },
  { key: '/roles', label: '角色权限', perm: 'role' },
  { key: '/model-config', label: '模型配置', perm: 'config' },
  { key: '/config', label: '系统配置', perm: 'config' },
  { key: '/membership-plans', label: '会员权益', perm: 'membership-plan' },
  { key: '/title-library', label: '标题库', perm: 'title-library' },
  { key: '/push-overview', label: '推送概览', perm: 'title-library' },
  { key: '/task-list', label: '任务列表', perm: 'task-list' },
  { key: '/banned-words', label: '违禁词管理', perm: 'config' },
  { key: '/orders', label: '收益管理', perm: 'config' },
  { key: '/expire-reminder', label: '到期提醒', perm: 'user' },
  { key: '/process', label: '流程管理', perm: 'title-library' },
  { key: '/customer-dialogues', label: '客服对话', perm: 'config' },
  { key: '/ai-flavor-rules', label: 'AI去除规则', perm: 'config' },
]

const menuGroups = [
  {
    label: '概览',
    items: [{ key: '/dashboard', label: '仪表盘', perm: 'dashboard' }],
  },
  {
    label: '内容管理',
    items: [
      { key: '/tracks', label: '赛道管理', perm: 'track' },
      { key: '/bloggers', label: '博主管理', perm: 'blogger' },
      { key: '/posts', label: '文章管理', perm: 'post' },
      { key: '/subscription-posts', label: '订阅文章', perm: 'subscription-post' },
      // FIXME: AI 检测功能暂时禁用
      // { key: '/article-ai-detect', label: 'AI检测', perm: 'title-library' },
    ],
  },
  {
    label: '用户管理',
    items: [
      { key: '/users', label: '用户管理', perm: 'user' },
      { key: '/orders', label: '收益管理', perm: 'config' },
      { key: '/expire-reminder', label: '到期提醒', perm: 'user' },
    ],
  },
  {
    label: '运营管理',
    items: [
      { key: '/customer-dialogues', label: '客服对话', perm: 'config' },
      { key: '/process', label: '流程管理', perm: 'title-library' },
      { key: '/banned-words', label: '违禁词管理', perm: 'config' },
      { key: '/ai-flavor-rules', label: 'AI去除规则', perm: 'config' },
    ],
  },
  {
    label: '系统管理',
    items: [
      { key: '/admins', label: '管理员管理', perm: 'admin' },
      { key: '/roles', label: '角色权限', perm: 'role' },
      { key: '/model-config', label: '模型配置', perm: 'config' },
      { key: '/config', label: '系统配置', perm: 'config' },
      { key: '/membership-plans', label: '会员权益', perm: 'membership-plan' },
      { key: '/title-library', label: '标题库', perm: 'title-library' },
      { key: '/push-overview', label: '推送概览', perm: 'title-library' },
      { key: '/task-list', label: '任务列表', perm: 'task-list' },
      { key: '/styles', label: '样式管理', perm: 'style' },
      { key: '/guides', label: '创作技巧', perm: 'guide' },
      { key: '/helps', label: '帮助文档', perm: 'help' },
    ],
  },
]

const filteredGroups = computed(() => {
  return menuGroups.map(group => ({
    ...group,
    items: group.items.filter(i => hasPerm(i.perm)),
  })).filter(group => group.items.length > 0)
})

const searchKeyword = ref('')

const filteredMenuItems = computed(() => {
  if (!searchKeyword.value.trim()) return null
  const kw = searchKeyword.value.trim().toLowerCase()
  return allMenuItems
    .filter(i => hasPerm(i.perm) && i.label.toLowerCase().includes(kw))
    .slice(0, 8)
})

function onMenuClick(key) {
  router.push(key)
  searchKeyword.value = ''
  closeAll()
}

const logoImage = computed(() => {
  return new URL('/src/assets/wechat.png', import.meta.url).href
})

const openGroup = ref('')

function toggleGroup(label) {
  if (openGroup.value === label) {
    openGroup.value = ''
  } else {
    openGroup.value = label
  }
}

function closeAll() {
  openGroup.value = ''
}

function onSearchFocus() {
  openGroup.value = ''
}

const pwdModalOpen = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

function handleDropdownClick({ key }) {
  if (key === 'logout') {
    localStorage.removeItem('admin-token')
    localStorage.removeItem('admin-user')
    localStorage.removeItem('admin-perms')
    router.push('/login')
  } else if (key === 'password') {
    pwdModalOpen.value = true
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
}

async function handlePwdOk() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword || !pwdForm.value.confirmPassword) {
    message.warning('请填写完整密码信息')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  try {
    await request.post('/auth/change-password', {
      adminId: adminUser.value.id,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
    })
    message.success('密码修改成功')
    pwdModalOpen.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '修改失败')
  }
}
</script>

<template>
  <div class="mac-layout">
    <!-- macOS 风格顶部菜单栏 -->
    <div class="mac-menu-bar" @click.stop>
      <div class="mac-menu-bar-left">
        <div class="mac-logo">
          <img :src="logoImage" style="width: 22px; height: 22px; object-fit: cover;">
        </div>

        <div class="mac-menus">
          <div
            v-for="group in filteredGroups"
            :key="group.label"
            class="mac-menu-item"
            :class="{ active: openGroup === group.label }"
            @click.stop="toggleGroup(group.label)"
          >
            <span class="mac-menu-label">{{ group.label }}</span>
            <!-- 下拉面板 -->
            <div v-if="openGroup === group.label" class="mac-dropdown" @click.stop>
              <div
                v-for="item in group.items"
                :key="item.key"
                class="mac-dropdown-item"
                :class="{ current: route.path === item.key }"
                @click="onMenuClick(item.key)"
              >
                <span>{{ item.label }}</span>
                <span v-if="route.path === item.key" class="current-dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="mac-menu-bar-right">
        <!-- 搜索框 -->
        <div class="mac-search-wrap">
          <svg class="mac-search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="searchKeyword"
            class="mac-search-input"
            placeholder="搜索菜单..."
            @focus="onSearchFocus"
          />
        </div>
        <!-- 搜索结果 -->
        <div v-if="filteredMenuItems !== null" class="mac-search-result" @click.stop>
          <div v-if="filteredMenuItems.length === 0" class="mac-search-empty">无匹配结果</div>
          <div
            v-for="item in filteredMenuItems"
            :key="item.key"
            class="mac-dropdown-item"
            :class="{ current: route.path === item.key }"
            @click="onMenuClick(item.key)"
          >
            <span>{{ item.label }}</span>
          </div>
        </div>

        <!-- 管理员下拉 -->
        <Dropdown @click.stop>
          <div class="mac-admin-info">
            <Avatar size="small" style="background: #1890ff; color: #fff; font-size: 11px;">
              {{ (adminUser.name || adminUser.username || 'A')[0].toUpperCase() }}
            </Avatar>
            <span class="mac-admin-name">{{ adminUser.name || adminUser.username }}</span>
            <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
          </div>
          <template #overlay>
            <div class="mac-admin-dropdown">
              <div class="mac-admin-dropdown-item" @click="handleDropdownClick({ key: 'password' })">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink:0"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0110 0v4"></path></svg>
                修改密码
              </div>
              <div class="mac-admin-dropdown-divider"></div>
              <div class="mac-admin-dropdown-item danger" @click="handleDropdownClick({ key: 'logout' })">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink:0"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                退出登录
              </div>
            </div>
          </template>
        </Dropdown>
      </div>
    </div>

    <!-- 全局遮罩，点击关闭下拉 -->
    <div v-if="openGroup || filteredMenuItems !== null" class="mac-overlay" @click="closeAll(); searchKeyword = ''"></div>

    <!-- 内容区域 -->
    <div class="mac-content">
      <div class="mac-page-header">
        <div class="mac-page-title">{{ pageTitle }}</div>
      </div>
      <div class="mac-page-content">
        <router-view />
      </div>
    </div>
  </div>

  <Modal v-model:open="pwdModalOpen" title="修改密码" :mask-closable="false" width="400" @ok="handlePwdOk">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="旧密码" required>
        <Input.Password v-model:value="pwdForm.oldPassword" placeholder="请输入当前密码" />
      </Form.Item>
      <Form.Item label="新密码" required>
        <Input.Password v-model:value="pwdForm.newPassword" placeholder="请输入新密码" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">密码长度不少于 8 位，需包含字母和数字</div>
      </Form.Item>
      <Form.Item label="确认新密码" required>
        <Input.Password v-model:value="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
      </Form.Item>
    </Form>
  </Modal>
</template>

<style>
* { box-sizing: border-box; }

.mac-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

/* ---- macOS 菜单栏 ---- */
.mac-menu-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 1000;
  height: 52px;
  background: rgba(30, 30, 30, 0.97);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-bottom: 1px solid rgba(255,255,255,0.07);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  user-select: none;
}

.mac-menu-bar-left {
  display: flex;
  align-items: center;
  gap: 2px;
}

.mac-logo {
  width: 30px; height: 30px;
  border-radius: 6px; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  margin-right: 10px;
  background: rgba(255,255,255,0.08);
}

.mac-menus {
  display: flex;
  align-items: center;
  gap: 2px;
}

.mac-menu-item {
  position: relative;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  color: rgba(255,255,255,0.8);
  font-size: 13px;
}

.mac-menu-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
.mac-menu-item.active { background: rgba(255,255,255,0.14); color: #fff; }

/* ---- 下拉面板 ---- */
.mac-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 210px;
  background: rgba(44, 44, 44, 0.98);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.5), 0 0 0 0.5px rgba(255,255,255,0.05) inset;
  z-index: 1001;
}

.mac-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: rgba(255,255,255,0.85);
  cursor: pointer;
  transition: background 0.1s;
  gap: 8px;
}

.mac-dropdown-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
.mac-dropdown-item.current { background: rgba(24,144,255,0.22); color: #69b1ff; }

.current-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #1890ff;
  flex-shrink: 0;
}

/* ---- 搜索 ---- */
.mac-menu-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mac-search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.mac-search-icon {
  position: absolute;
  left: 10px;
  color: rgba(255,255,255,0.35);
  pointer-events: none;
}

.mac-search-input {
  width: 200px; height: 30px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  padding: 0 12px 0 32px;
  font-size: 12px;
  color: #fff;
  outline: none;
  transition: all 0.2s;
}

.mac-search-input::placeholder { color: rgba(255,255,255,0.3); }
.mac-search-input:focus {
  width: 240px;
  background: rgba(255,255,255,0.13);
  border-color: rgba(255,255,255,0.22);
}

.mac-search-result {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 200px;
  background: rgba(44, 44, 44, 0.98);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.5);
  z-index: 1001;
}

.mac-search-empty {
  padding: 12px 14px;
  font-size: 13px;
  color: rgba(255,255,255,0.35);
  text-align: center;
}

/* ---- 管理员信息 ---- */
.mac-admin-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  color: rgba(255,255,255,0.7);
}

.mac-admin-info:hover { background: rgba(255,255,255,0.1); color: #fff; }

.mac-admin-name {
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mac-admin-dropdown {
  background: rgba(44, 44, 44, 0.98);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 6px;
  min-width: 160px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.5);
}

.mac-admin-dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: rgba(255,255,255,0.85);
  cursor: pointer;
  transition: background 0.1s;
}

.mac-admin-dropdown-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
.mac-admin-dropdown-item.danger:hover { background: rgba(255,59,48,0.2); color: #ff8080; }

.mac-admin-dropdown-divider {
  height: 1px;
  background: rgba(255,255,255,0.08);
  margin: 4px 0;
}

/* ---- 全局遮罩 ---- */
.mac-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
}

/* ---- 内容区域 ---- */
.mac-content {
  padding-top: 52px;
}

.mac-page-header {
  height: 52px;
  background: #fff;
  padding: 0 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}

.mac-page-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.mac-page-content {
  padding: 20px 24px;
  min-height: calc(100vh - 52px);
}
</style>
