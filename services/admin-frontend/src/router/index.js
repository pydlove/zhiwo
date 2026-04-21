import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import Dashboard from '../views/Dashboard.vue'
import TrackManage from '../views/TrackManage.vue'
import BloggerManage from '../views/BloggerManage.vue'
import PostManage from '../views/PostManage.vue'
import UserManage from '../views/UserManage.vue'
import AdminManage from '../views/AdminManage.vue'
import RoleManage from '../views/RoleManage.vue'
import GuideManage from '../views/GuideManage.vue'
import HelpManage from '../views/HelpManage.vue'
import StyleManage from '../views/StyleManage.vue'
import ConfigManage from '../views/ConfigManage.vue'
import SubscriptionPostManage from '../views/SubscriptionPostManage.vue'
import MembershipPlanManage from '../views/MembershipPlanManage.vue'
const routes = [
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: Dashboard, meta: { title: '仪表盘', perm: 'dashboard' } },
      { path: 'tracks', name: 'TrackManage', component: TrackManage, meta: { title: '赛道管理', perm: 'track' } },
      { path: 'bloggers', name: 'BloggerManage', component: BloggerManage, meta: { title: '博主管理', perm: 'blogger' } },
      { path: 'posts', name: 'PostManage', component: PostManage, meta: { title: '文章管理', perm: 'post' } },
      { path: 'users', name: 'UserManage', component: UserManage, meta: { title: '用户管理', perm: 'user' } },
      { path: 'admins', name: 'AdminManage', component: AdminManage, meta: { title: '管理员管理', perm: 'admin' } },
      { path: 'roles', name: 'RoleManage', component: RoleManage, meta: { title: '角色权限', perm: 'role' } },
      { path: 'guides', name: 'GuideManage', component: GuideManage, meta: { title: '创作技巧', perm: 'guide' } },
      { path: 'helps', name: 'HelpManage', component: HelpManage, meta: { title: '帮助文档', perm: 'help' } },
      { path: 'styles', name: 'StyleManage', component: StyleManage, meta: { title: '样式管理', perm: 'style' } },
      { path: 'config', name: 'ConfigManage', component: ConfigManage, meta: { title: '系统配置', perm: 'config' } },
      { path: 'subscription-posts', name: 'SubscriptionPostManage', component: SubscriptionPostManage, meta: { title: '订阅文章', perm: 'subscription-post' } },
      { path: 'membership-plans', name: 'MembershipPlanManage', component: MembershipPlanManage, meta: { title: '会员权益', perm: 'membership-plan' } },
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
