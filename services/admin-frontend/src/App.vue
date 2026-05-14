<template>
  <a-config-provider :theme="themeConfig" :component="componentConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup>
import { provide, reactive, computed } from 'vue'
import { theme } from 'ant-design-vue'

const savedTheme = localStorage.getItem('admin-theme') || 'light'

const themeConfig = reactive({
  algorithm: savedTheme === 'dark'
    ? [theme.darkAlgorithm, theme.compactAlgorithm]
    : theme.compactAlgorithm,
})

const isDark = computed(() =>
  Array.isArray(themeConfig.algorithm)
    && themeConfig.algorithm.includes(theme.darkAlgorithm)
)

function toggleTheme() {
  if (isDark.value) {
    themeConfig.algorithm = theme.compactAlgorithm
    localStorage.setItem('admin-theme', 'light')
  } else {
    themeConfig.algorithm = [theme.darkAlgorithm, theme.compactAlgorithm]
    localStorage.setItem('admin-theme', 'dark')
  }
}

provide('themeConfig', themeConfig)
provide('isDark', isDark)
provide('toggleTheme', toggleTheme)

const componentConfig = {
  Modal: {
    centered: true,
  },
}
</script>

<style>
/* 全局字体 */
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
}

/* 全局弹框居中 + 最大宽度限制 */
.ant-modal {
  margin: 0 auto !important;
  top: 0 !important;
  max-width: 1200px !important;
}
</style>
