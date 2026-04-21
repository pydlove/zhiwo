import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import HomeView from '../views/HomeView.vue'
import CreateView from '../views/CreateView.vue'
import DraftView from '../views/DraftView.vue'
import CreationsView from '../views/CreationsView.vue'
import HelpView from '../views/HelpView.vue'
import TipsView from '../views/TipsView.vue'
import LoginView from '../views/LoginView.vue'
import TrackDetailView from '../views/TrackDetailView.vue'
import TitleSelectView from '../views/TitleSelectView.vue'
import ProfileView from '../views/ProfileView.vue'
import LandingView from '../views/LandingView.vue'

const routes = [
  { path: '/', name: 'Landing', component: LandingView, meta: { title: '公众号创作助手' } },
  { path: '/login', name: 'Login', component: LoginView, meta: { title: '登录' } },
  {
    path: '/app',
    component: AppLayout,
    redirect: '/app/home',
    children: [
      { path: 'home', name: 'Home', component: HomeView, meta: { title: '首页' } },
      { path: 'create', name: 'Create', component: CreateView, meta: { title: '创作中心' } },
      { path: 'drafts', name: 'Drafts', component: DraftView, meta: { title: '我的草稿' } },
      { path: 'creations', name: 'Creations', component: CreationsView, meta: { title: '我的创作' } },
      { path: 'help', name: 'Help', component: HelpView, meta: { title: '帮助文档' } },
      { path: 'tips', name: 'Tips', component: TipsView, meta: { title: '创作技巧' } },
      { path: 'track/:id', name: 'TrackDetail', component: TrackDetailView, meta: { title: '赛道详情' } },
      { path: 'title-select', name: 'TitleSelect', component: TitleSelectView, meta: { title: '选择创作方向' } },
      { path: 'profile', name: 'Profile', component: ProfileView, meta: { title: '个人中心' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
