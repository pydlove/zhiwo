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
import AffiliateView from '../views/AffiliateView.vue'
import OpenAccountView from '../views/OpenAccountView.vue'
import MpRegisterGuideView from '../views/MpRegisterGuideView.vue'
import CustomerDialogueView from '../views/CustomerDialogueView.vue'

const routes = [
  { path: '/', name: 'Landing', component: LandingView, meta: { title: '知我公众号创作助手' } },
  { path: '/affiliate', name: 'Affiliate', component: AffiliateView, meta: { title: '分销活动 - 知我公众号创作助手' } },
  { path: '/open-account', name: 'OpenAccount', component: OpenAccountView, meta: { title: '注册开户 - 知我公众号创作助手' } },
  { path: '/guide/mp-register', name: 'MpRegisterGuide', component: MpRegisterGuideView, meta: { title: '如何注册公众号 - 知我公众号创作助手' } },
  { path: '/customer-dialogue', name: 'CustomerDialogue', component: CustomerDialogueView, meta: { title: '客服话术助手' } },
  { path: '/income', name: 'IncomeOverview', component: () => import('../views/IncomeOverview.vue'), meta: { title: '收益概览' } },
  { path: '/register', name: 'Register', component: LoginView, meta: { title: '注册 - 知我公众号创作助手' } },
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
