<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Layout, Menu, Dropdown, Avatar, Modal, Form, Input, message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()

const selectedKeys = computed(() => [route.path])
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
  { key: '/config', label: '系统配置', perm: 'config' },
  { key: '/membership-plans', label: '会员权益', perm: 'membership-plan' },
]

const logoImage = computed(() => {
  return new URL('/src/assets/wechat.png', import.meta.url).href
})

const menuItems = computed(() => allMenuItems.filter(i => hasPerm(i.perm)))

function onMenuClick({ key }) {
  router.push(key)
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

function handlePwdOk() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword || !pwdForm.value.confirmPassword) {
    message.warning('请填写完整密码信息')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  message.success('密码修改成功')
  pwdModalOpen.value = false
}
</script>

<template>
  <Layout style="min-height: 100vh;">
    <Layout.Sider theme="dark" width="208" style="background: #001529;">
      <div style="padding: 16px 24px 24px; font-size: 18px; font-weight: 600; color: #fff; display: flex; align-items: center; gap: 10px;">
        <img :src="logoImage" style="width: 40px; height: 40px; object-fit: cover;">
         <span style="font-size: 16px; font-weight: 600; color: #fff;">公众号创作助手管理系统</span>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :items="menuItems.map(i => ({ key: i.key, label: i.label }))"
        @click="onMenuClick"
        style="border-right: 0; background: #001529;"
      />
    </Layout.Sider>
    <Layout>
      <Layout.Header style="height: 64px; background: #fff; padding: 0 24px; border-bottom: 1px solid #f0f0f0; display: flex; align-items: center; justify-content: space-between;">
        <div style="font-size: 20px; font-weight: 500; color: #262626;">{{ pageTitle }}</div>
        <div style="display: flex; align-items: center; gap: 16px; font-size: 14px; color: #595959;">
          <span>{{ adminUser.name || adminUser.username || '管理员' }}</span>
          <Dropdown>
            <div style="display: flex; align-items: center; cursor: pointer;">
              <Avatar style="background: #1890ff; color: #fff;">A</Avatar>
            </div>
            <template #overlay>
              <Menu @click="handleDropdownClick">
                <Menu.Item key="password">修改密码</Menu.Item>
                <Menu.Item key="logout">退出登录</Menu.Item>
              </Menu>
            </template>
          </Dropdown>
        </div>
      </Layout.Header>
      <Layout.Content style="padding: 24px; background: #f0f2f5;">
        <router-view />
      </Layout.Content>
    </Layout>
  </Layout>

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
