<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Card, Input, Button, Form, message } from 'ant-design-vue'
import request from '../api/request.js'

const router = useRouter()
const form = ref({ account: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.account || !form.value.password) {
    message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await request.post('/auth/login', {
      username: form.value.account,
      password: form.value.password,
    })
    localStorage.setItem('admin-token', data.token)
    localStorage.setItem('admin-user', JSON.stringify(data.user || {}))
    localStorage.setItem('admin-perms', data.permissions || '[]')
    router.push('/dashboard')
  } catch (e) {
    message.error(e.message || '账号或密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="min-height: 100vh; background: #f0f2f5; display: flex; align-items: center; justify-content: center;">
    <Card style="width: 360px; border-radius: 2px;">
      <div style="text-align: center; margin-bottom: 24px;">
        <div style="width: 48px; height: 48px; border-radius: 4px; background: #1890ff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; margin: 0 auto 12px;">A</div>
        <div style="font-size: 20px; font-weight: 500; color: #262626;">知我公众号创作助手 后台管理</div>
      </div>
      <Form layout="vertical">
        <Form.Item label="管理员账号">
          <Input v-model:value="form.account" placeholder="请输入账号" />
        </Form.Item>
        <Form.Item label="密码">
          <Input.Password v-model:value="form.password" placeholder="请输入密码" />
        </Form.Item>
        <Button type="primary" block size="large" :loading="loading" @click="handleLogin">登录</Button>
      </Form>
    </Card>
  </div>
</template>
