import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'

const routes = [
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘', perm: 'dashboard' } },
      { path: 'tracks', name: 'TrackManage', component: () => import('../views/TrackManage.vue'), meta: { title: '赛道管理', perm: 'track' } },
      { path: 'bloggers', name: 'BloggerManage', component: () => import('../views/BloggerManage.vue'), meta: { title: '博主管理', perm: 'blogger' } },
      { path: 'posts', name: 'PostManage', component: () => import('../views/PostManage.vue'), meta: { title: '文章管理', perm: 'post' } },
      { path: 'users', name: 'UserManage', component: () => import('../views/UserManage.vue'), meta: { title: '用户管理', perm: 'user' } },
      { path: 'admins', name: 'AdminManage', component: () => import('../views/AdminManage.vue'), meta: { title: '管理员管理', perm: 'admin' } },
      { path: 'roles', name: 'RoleManage', component: () => import('../views/RoleManage.vue'), meta: { title: '角色权限', perm: 'role' } },
      { path: 'guides', name: 'GuideManage', component: () => import('../views/GuideManage.vue'), meta: { title: '创作技巧', perm: 'guide' } },
      { path: 'helps', name: 'HelpManage', component: () => import('../views/HelpManage.vue'), meta: { title: '帮助文档', perm: 'help' } },
      { path: 'styles', name: 'StyleManage', component: () => import('../views/StyleManage.vue'), meta: { title: '样式管理', perm: 'style' } },
      { path: 'config', name: 'ConfigManage', component: () => import('../views/ConfigManage.vue'), meta: { title: '系统配置', perm: 'config' } },
      { path: 'subscription-posts', name: 'SubscriptionPostManage', component: () => import('../views/SubscriptionPostManage.vue'), meta: { title: '订阅文章', perm: 'subscription-post' } },
      { path: 'membership-plans', name: 'MembershipPlanManage', component: () => import('../views/MembershipPlanManage.vue'), meta: { title: '会员权益', perm: 'membership-plan' } },
      { path: 'title-library', name: 'TitleLibraryManage', component: () => import('../views/TitleLibraryManage.vue'), meta: { title: '标题库', perm: 'title-library' } },
      { path: 'push-overview', name: 'PushOverview', component: () => import('../views/PushOverview.vue'), meta: { title: '推送概览', perm: 'title-library' } },
      { path: 'banned-words', name: 'BannedWordManage', component: () => import('../views/BannedWordManage.vue'), meta: { title: '违禁词管理', perm: 'config' } },
      { path: 'orders', name: 'OrderManage', component: () => import('../views/OrderManage.vue'), meta: { title: '收益管理', perm: 'config' } },
    ],
  },
  { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { title: '管理员登录' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function hasRoutePerm(to) {
  if (to.path === '/login') return true
  const perm = to.meta?.perm
  if (!perm) return true
  try {
    const perms = JSON.parse(localStorage.getItem('admin-perms') || '[]')
    if (perms.includes('all')) return true
    return perms.includes(perm)
  } catch (e) {
    return false
  }
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin-token')
  if (!token && to.path !== '/login') {
    next('/login')
    return
  }
  if (token && !hasRoutePerm(to)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
