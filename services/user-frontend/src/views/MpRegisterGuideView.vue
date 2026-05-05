<script setup>
import { Button, Card } from 'ant-design-vue'
</script>

<template>
  <div class="guide-page">
    <div class="header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-logo">A</div>
          <span class="brand-name">知我公众号创作助手</span>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="guide-wrap">
        <div class="guide-header">
          <h1>如何注册微信公众号</h1>
          <p>我们为您准备了两种注册方式的详细教程</p>
        </div>

        <Card class="guide-card" :bordered="false">
          <div class="card-title">
            <span class="card-icon">💻</span>
            <span>电脑网页注册方式</span>
          </div>
          <p class="card-desc">
            适合有电脑操作环境的用户，通过浏览器访问微信公众平台官网完成注册。
          </p>
          <a
            href="https://docs.qq.com/doc/DRm1FTmtjanRhb2R3"
            target="_blank"
            rel="noopener noreferrer"
            class="guide-link"
          >
            <Button type="primary" size="large" block>
              查看电脑网页注册教程
            </Button>
          </a>
        </Card>

        <Card class="guide-card" :bordered="false" style="margin-top: 16px;">
          <div class="card-title">
            <span class="card-icon">📱</span>
            <span>手机APP注册方式</span>
          </div>
          <p class="card-desc">
            适合没有电脑、通过手机完成注册的用户，下载「订阅号助手」APP即可操作。
          </p>
          <a
            href="https://docs.qq.com/doc/DRkd6eFpySHRyZ0hS"
            target="_blank"
            rel="noopener noreferrer"
            class="guide-link"
          >
            <Button type="primary" size="large" block>
              查看手机APP注册教程
            </Button>
          </a>
        </Card>

        <div class="back-action">
          <Button size="large" block @click="$router.back()">
            返回开户页面
          </Button>
        </div>
      </div>
    </div>

    <div class="footer">
      <div>知我公众号创作助手 &copy; 2026</div>
    </div>
  </div>
</template>

<style scoped>
.guide-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.header-inner {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.brand-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.content {
  flex: 1;
  padding: 24px;
}

.guide-wrap {
  max-width: 480px;
  margin: 0 auto;
}

.guide-header {
  text-align: center;
  margin-bottom: 24px;
  padding-top: 8px;
}

.guide-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px;
}

.guide-header p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.guide-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  font-size: 20px;
}

.card-desc {
  font-size: 14px;
  color: #595959;
  margin-bottom: 16px;
  line-height: 1.6;
}

.guide-link {
  display: block;
  text-decoration: none;
}

.back-action {
  margin-top: 24px;
}

.footer {
  text-align: center;
  padding: 16px;
  font-size: 12px;
  color: #bfbfbf;
}

@media (max-width: 576px) {
  .content {
    padding: 12px;
  }
}
</style>
