<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Tooltip } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { usePlatformStore } from '../stores/platform.js'
import { getTrack } from '../api/track.js'

const route = useRoute()
const router = useRouter()
const { isMobile } = useViewport()
const platformStore = usePlatformStore()
const trackId = route.params.id

const activeBlogger = ref(0)
const bloggerDrawerOpen = ref(false)

const track = ref({})
const bloggers = ref([])
const articles = ref([])

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const isExpired = computed(() => {
  const expire = user.value.expireDate
  if (!expire) return false
  return new Date(expire + 'T23:59:59') < new Date()
})

const filteredBloggers = computed(() => bloggers.value)

const filteredArticles = computed(() => {
  if (!currentBlogger.value) return articles.value
  return articles.value.filter(a => a.bloggerId === currentBlogger.value.id)
})

const currentBlogger = computed(() => {
  return filteredBloggers.value[activeBlogger.value] || filteredBloggers.value[0] || null
})

const estimatedRevenue = computed(() => {
  const totalReads = filteredArticles.value.reduce((sum, a) => sum + (a.readsNum || 0), 0)
  if (totalReads <= 0) return 0
  return ((totalReads * 5) / 1000).toFixed(2)
})

watch(() => platformStore.current, () => {
  activeBlogger.value = 0
})

function openPreview(a) {
  if (a.url) {
    window.open(a.url, '_blank')
  } else {
    message.warning('暂无原文链接')
  }
}

function goToTitleSelect() {
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return
  }
  router.push(`/app/create?trackId=${trackId}`)
}

function formatMeta(b) {
  const count = b.articleCount || 0
  return `${count} 篇爆款`
}

function parseReads(val) {
  if (val == null) return 0
  const str = String(val).replace(/,/g, '').trim()
  if (str.endsWith('w') || str.endsWith('W')) {
    const num = parseFloat(str.slice(0, -1))
    return isNaN(num) ? 0 : num * 10000
  }
  if (str.endsWith('k') || str.endsWith('K')) {
    const num = parseFloat(str.slice(0, -1))
    return isNaN(num) ? 0 : num * 1000
  }
  const num = parseFloat(str)
  return isNaN(num) ? 0 : num
}

function formatReads(val) {
  const num = parseReads(val)
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return String(num)
}

onMounted(async () => {
  try {
    const data = await getTrack(trackId)
    if (data.track) {
      track.value = data.track
    }
    if (data.bloggers) {
      bloggers.value = data.bloggers.map(b => ({
        ...b,
        avatar: b.avatar || (b.name ? b.name.slice(0, 1) : '?'),
        meta: formatMeta(b),
        platforms: b.platform || '公众号',
      }))
    }
    if (data.articles) {
      articles.value = data.articles.map(a => {
        const readsNum = parseReads(a.reads)
        const revenue = ((readsNum * 20) / 6000).toFixed(2)
        return {
          ...a,
          tag: readsNum >= 100000 ? (a.tag || '10w+') : '',
          summary: a.summary || (a.content ? a.content.replace(/<[^>]+>/g, '').slice(0, 60) + '...' : ''),
          reads: formatReads(a.reads),
          readsNum,
          revenue,
          likes: formatReads(a.likes),
          comments: formatReads(a.comments),
          date: a.createdAt ? a.createdAt.slice(0, 10) : '-',
          platform: a.platform || '公众号',
          content: a.content || '<p>暂无内容</p>',
        }
      })
    }
  } catch (e) {}
})
</script>

<template>
  <div :style="{ maxWidth: '1200px', margin: '0 auto', padding: isMobile ? '0 12px 32px' : '0 0 48px' }">
    <div style="padding: 20px 0; font-size: 14px; color: #6b7280;">
      <span style="cursor: pointer;" @click="router.push('/app/home')">首页</span> > <span style="color: #2563eb; font-weight: 500;">{{ track.name || '赛道详情' }}</span>
      <span style="margin-left: 12px; font-size: 12px; padding: 2px 8px; background: #eff6ff; border-radius: 4px; color: #1d4ed8; font-weight: 500;">{{ platformStore.current }}</span>
    </div>

    <!-- Mobile Blogger Toggle -->
    <div v-if="isMobile && filteredBloggers.length > 0" style="margin-bottom: 12px;">
      <button @click="bloggerDrawerOpen = !bloggerDrawerOpen" style="width: 100%; padding: 10px 14px; background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; font-size: 14px; font-weight: 500; color: #374151; cursor: pointer; display: flex; align-items: center; justify-content: space-between;">
        <span>博主：{{ currentBlogger?.name || '请选择' }}</span>
        <span style="transition: transform 0.2s;" :style="bloggerDrawerOpen ? { transform: 'rotate(180deg)' } : {}">▼</span>
      </button>
      <div v-if="bloggerDrawerOpen" style="margin-top: 8px; background: #fff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.06);">
        <div
          v-for="(b, idx) in filteredBloggers"
          :key="b.id"
          @click="activeBlogger = idx; bloggerDrawerOpen = false"
          style="display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer;"
          :style="activeBlogger === idx ? { background: '#eff6ff', border: '1px solid #bfdbfe' } : { border: '1px solid transparent' }"
        >
          <div style="width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%); display: flex; align-items: center; justify-content: center; font-size: 14px; overflow: hidden;">
            <img v-if="b.avatar && (b.avatar.startsWith('http') || b.avatar.startsWith('data:image'))" :src="b.avatar" style="width: 100%; height: 100%; object-fit: cover;">
            <template v-else>{{ b.avatar }}</template>
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="font-size: 14px; font-weight: 600; color: #111827;">{{ b.name }}</div>
            <div style="font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ b.tagline || '暂无简介' }}</div>
          </div>
        </div>
      </div>
    </div>

    <div :style="{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '16px' : '24px' }">
      <!-- Sidebar (desktop only) -->
      <div v-if="!isMobile" style="width: 280px; display: flex; flex-direction: column; gap: 16px;">
        <div style="background: #fff; border-radius: 16px; border: 1px solid #f1f5f9; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
          <div style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #111827;">{{ track.name || '赛道介绍' }}</div>
          <div style="font-size: 13px; color: #6b7280; line-height: 1.6;">{{ track.intro || '暂无赛道描述' }}</div>
        </div>

        <div style="background: #fff; border-radius: 16px; border: 1px solid #f1f5f9; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); height: fit-content;">
          <div style="font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #111827;">头部博主</div>
        <div v-if="filteredBloggers.length === 0" style="text-align: center; padding: 24px; font-size: 13px; color: #9ca3af;">
          当前平台暂无相关博主数据
        </div>
        <div
          v-for="(b, idx) in filteredBloggers"
          :key="b.id"
          @click="activeBlogger = idx"
          style="display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 10px; cursor: pointer; transition: background 0.15s;"
          :style="activeBlogger === idx ? { background: '#eff6ff', border: '1px solid #bfdbfe' } : { border: '1px solid transparent' }"
        >
          <div style="width: 44px; height: 44px; border-radius: 50%; background: linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%); display: flex; align-items: center; justify-content: center; font-size: 18px; overflow: hidden;">
            <img v-if="b.avatar && (b.avatar.startsWith('http') || b.avatar.startsWith('data:image'))" :src="b.avatar" style="width: 100%; height: 100%; object-fit: cover;">
            <template v-else>{{ b.avatar }}</template>
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="font-size: 14px; font-weight: 600; color: #111827;">{{ b.name }}</div>
            <div style="font-size: 12px; color: #6b7280; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ b.tagline || '暂无简介' }}</div>
          </div>
        </div>
      </div>
      </div>

      <!-- Content -->
      <div :style="{ flex: 1, background: '#fff', borderRadius: '16px', border: '1px solid #f1f5f9', padding: isMobile ? '16px' : '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }">
        <div :style="{ display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'center', marginBottom: '20px', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '12px' : '0' }">
          <div>
            <div style="font-size: 18px; font-weight: 600; color: #111827;">{{ currentBlogger ? currentBlogger.name + ' · 爆款文章' : '爆款文章' }}</div>
            <div v-if="currentBlogger" style="margin-top: 10px;">
              <div style="font-size: 13px; color: #6b7280; line-height: 1.6; max-width: 600px;">
                {{ currentBlogger.tagline || '暂无简介' }}
              </div>
              <div style="margin-top: 6px; font-size: 13px;">
                <a v-if="currentBlogger.link && !(currentBlogger.platform || '').includes('公众号')" :href="currentBlogger.link" target="_blank" style="color: #2563eb; text-decoration: none;">
                  访问主页 ↗
                </a>
                <span v-else-if="(currentBlogger.platform || '').includes('公众号')" style="color: #9ca3af;">
                  公众号暂无主页
                </span>
              </div>
            </div>
          </div>
          <div :style="{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }">
            <div v-if="estimatedRevenue !== null" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: #fff7ed; border-radius: 8px; border: 1px solid #fed7aa;">
              <span style="font-size: 13px; color: #9a3412;">预计收益</span>
              <span style="font-size: 18px; font-weight: 700; color: #ea580c;">¥ {{ estimatedRevenue }}</span>
              <Tooltip title="该收益 5 eCPM 评估计算，并非真实收益，真实收益还需要看具体的 eCPM">
                <span style="cursor: help; width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #fdba74; color: #fff; font-size: 12px; font-weight: 600;">?</span>
              </Tooltip>
            </div>
            <button @click="goToTitleSelect" :disabled="!currentBlogger" style="padding: 10px 20px; background: #2563eb; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer;" :style="!currentBlogger ? { opacity: 0.5, cursor: 'not-allowed' } : {}">📥 订阅中心</button>
          </div>
        </div>

        <div v-if="filteredArticles.length === 0" style="text-align: center; padding: 60px 20px; color: #9ca3af; background: #f8fafc; border-radius: 12px; border: 1px dashed #e5e7eb;">
          该博主暂无相关文章数据
        </div>
        <div v-else style="display: flex; flex-direction: column; gap: 12px;">
          <div
            v-for="a in filteredArticles"
            :key="a.id"
            @click="openPreview(a)"
            style="padding: 16px; border: 1px solid #f1f5f9; border-radius: 12px; transition: border-color 0.15s, box-shadow 0.15s; cursor: pointer;"
            @mouseenter="$event.currentTarget.style.borderColor = '#bfdbfe'; $event.currentTarget.style.boxShadow = '0 2px 8px rgba(37,99,235,0.06)'"
            @mouseleave="$event.currentTarget.style.borderColor = '#f1f5f9'; $event.currentTarget.style.boxShadow = 'none'"
          >
            <div style="font-size: 15px; font-weight: 600; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; color: #111827;">
              {{ a.title }}
              <span v-if="a.tag" style="font-size: 11px; padding: 2px 8px; background: #fef3c7; color: #92400e; border-radius: 4px; font-weight: 500;">{{ a.tag }}</span>
            </div>
            <div style="font-size: 13px; color: #6b7280; line-height: 1.5; margin-bottom: 8px;">{{ a.summary }}</div>
            <div style="font-size: 12px; color: #9ca3af; display: flex; gap: 16px; align-items: center;">
              <span :style="a.readsNum >= 10000 ? { color: a.readsNum >= 100000 ? '#ef4444' : a.readsNum >= 50000 ? '#f97316' : '#f59e0b', fontWeight: 500 } : {}">阅读 {{ a.reads }}</span>
              <span>点赞 {{ a.likes }}</span>
              <span>评论 {{ a.comments }}</span>
              <span>{{ a.date }}</span>
              <span v-if="platformStore.current === '公众号'" style="font-size: 12px; font-weight: 600; color: #ea580c;">预计收益 ¥{{ a.revenue }}</span>
              <span style="font-size: 11px; padding: 2px 8px; background: #eff6ff; color: #1d4ed8; border-radius: 4px; font-weight: 500;">{{ a.platform || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

</template>
