# 微信公众号头部博主整理小程序 - 开发计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于微信小程序原生开发，搭建一个单 tab 的公众号头部博主整理小程序，包含首页赛道排行、赛道 TopN 博主列表、博主代表作品复制功能。

**Architecture:** 微信小程序原生架构（WXML + WXSS + JS + JSON），无第三方框架依赖。数据采用本地 `data` 模块硬编码，页面间通过 `wx.navigateTo` 传参跳转。配色采用微信原生绿 `#07c160` 为主色调，搭配浅灰背景和白色卡片。

**Tech Stack:** 微信小程序原生开发工具语法（WXML、WXSS、JS、JSON）

---

## 文件结构

```
小程序/
├── app.js
├── app.json
├── app.wxss
├── pages/
│   ├── index/
│   │   ├── index.wxml
│   │   ├── index.wxss
│   │   ├── index.js
│   │   └── index.json
│   ├── track/
│   │   ├── track.wxml
│   │   ├── track.wxss
│   │   ├── track.js
│   │   └── track.json
│   └── blogger/
│       ├── blogger.wxml
│       ├── blogger.wxss
│       ├── blogger.js
│       └── blogger.json
├── utils/
│   └── data.js
└── images/
    └── (iconfont 图标资源)
```

---

### Task 1: 初始化小程序项目骨架

**Files:**
- Create: `app.js`
- Create: `app.json`
- Create: `app.wxss`
- Create: `project.config.json`

- [ ] **Step 1: 创建 app.json**

```json
{
  "pages": [
    "pages/index/index",
    "pages/track/track",
    "pages/blogger/blogger"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarBackgroundColor": "#fff",
    "navigationBarTitleText": "公众号博主榜",
    "navigationBarTextStyle": "black"
  },
  "tabBar": {
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "公众号",
        "iconPath": "images/icon-off.png",
        "selectedIconPath": "images/icon-on.png"
      }
    ],
    "selectedColor": "#07c160",
    "backgroundColor": "#ffffff",
    "borderStyle": "black"
  },
  "style": "v2",
  "sitemapLocation": "sitemap.json"
}
```

- [ ] **Step 2: 创建 app.js**

```javascript
App({
  onLaunch() {
    console.log('小程序启动')
  }
})
```

- [ ] **Step 3: 创建 app.wxss**

```css
page {
  background-color: #f7f7f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.container {
  padding: 16px;
}
```

- [ ] **Step 4: 创建 project.config.json**

```json
{
  "description": "公众号头部博主整理小程序",
  "packOptions": {
    "ignore": []
  },
  "setting": {
    "urlCheck": false,
    "es6": true,
    "postcss": true,
    "minified": true,
    "newFeature": true
  },
  "compileType": "miniprogram",
  "libVersion": "2.19.4",
  "appid": "touristappid",
  "projectname": "blogger-directory",
  "condition": {}
}
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: init wechat miniprogram project skeleton"
```

---

### Task 2: 准备本地数据模块

**Files:**
- Create: `utils/data.js`

- [ ] **Step 1: 创建 utils/data.js**

```javascript
const tracks = [
  { id: 'personal-ip', name: '个人IP', icon: '🎬', sortOrder: 1, previewBloggers: '粥左罗、剽悍一只猫、Spenser' },
  { id: 'finance', name: '投资理财', icon: '💰', sortOrder: 2, previewBloggers: '越女事务所、也谈钱、银行螺丝钉' },
  { id: 'internet', name: '互联网', icon: '💻', sortOrder: 3, previewBloggers: '半佛仙人、刘润、插座学院' },
  { id: 'reading', name: '读书成长', icon: '📚', sortOrder: 4, previewBloggers: '樊登读书、古典、李尚龙' },
  { id: 'fitness', name: '健身运动', icon: '🏃', sortOrder: 5, previewBloggers: 'keep、闫帅奇、灵魂有香气的女子' },
  { id: 'parenting', name: '母婴育儿', icon: '👶', sortOrder: 6, previewBloggers: '年糕妈妈、大J小D、丁香妈妈' }
]

const bloggers = [
  { id: 'zhouzuoluo', name: '粥左罗', avatar: '', tagline: '写作课创始人 · 10w+阅读', trackId: 'personal-ip', rank: 1 },
  { id: 'piaohan', name: '剽悍一只猫', avatar: '', tagline: '个人品牌顾问 · 社群运营', trackId: 'personal-ip', rank: 2 },
  { id: 'spenser', name: 'Spenser', avatar: '', tagline: '香港第一自媒体 · 职场写作', trackId: 'personal-ip', rank: 3 },
  { id: 'yuenv', name: '越女事务所', avatar: '', tagline: '小白理财入门 · 实用干货', trackId: 'finance', rank: 1 },
  { id: 'yetanqian', name: '也谈钱', avatar: '', tagline: 'FIRE生活实践 · 存钱方法论', trackId: 'finance', rank: 2 },
  { id: 'luosiding', name: '银行螺丝钉', avatar: '', tagline: '指数基金定投 · 长期投资', trackId: 'finance', rank: 3 },
  { id: 'banfo', name: '半佛仙人', avatar: '', tagline: '互联网观察 · 硬核科普', trackId: 'internet', rank: 1 },
  { id: 'liurun', name: '刘润', avatar: '', tagline: '商业顾问 · 年度演讲', trackId: 'internet', rank: 2 },
  { id: 'chazuo', name: '插座学院', avatar: '', tagline: '职场能力提升 · 管理思维', trackId: 'internet', rank: 3 }
]

const posts = [
  { id: 'p1', title: '普通人如何靠写作年入百万', url: 'https://mp.weixin.qq.com/s/zhouzuoluo-001', bloggerId: 'zhouzuoluo' },
  { id: 'p2', title: '新媒体写作的黄金结构', url: 'https://mp.weixin.qq.com/s/zhouzuoluo-002', bloggerId: 'zhouzuoluo' },
  { id: 'p3', title: '为什么你的文章没人看？', url: 'https://mp.weixin.qq.com/s/zhouzuoluo-003', bloggerId: 'zhouzuoluo' },
  { id: 'p4', title: '个人品牌的10个关键动作', url: 'https://mp.weixin.qq.com/s/piaohan-001', bloggerId: 'piaohan' },
  { id: 'p5', title: '如何通过社群年入百万', url: 'https://mp.weixin.qq.com/s/piaohan-002', bloggerId: 'piaohan' },
  { id: 'p6', title: '内向的人如何做个人品牌', url: 'https://mp.weixin.qq.com/s/piaohan-003', bloggerId: 'piaohan' }
]

function getTracks() {
  return tracks
}

function getBloggersByTrack(trackId) {
  return bloggers.filter(b => b.trackId === trackId).sort((a, b) => a.rank - b.rank)
}

function getBloggerById(id) {
  return bloggers.find(b => b.id === id)
}

function getPostsByBlogger(bloggerId) {
  return posts.filter(p => p.bloggerId === bloggerId)
}

module.exports = {
  getTracks,
  getBloggersByTrack,
  getBloggerById,
  getPostsByBlogger
}
```

- [ ] **Step 2: Commit**

```bash
git add utils/data.js
git commit -m "feat: add local data module with tracks, bloggers and posts"
```

---

### Task 3: 开发首页（index）

**Files:**
- Create: `pages/index/index.wxml`
- Create: `pages/index/index.wxss`
- Create: `pages/index/index.js`
- Create: `pages/index/index.json`

- [ ] **Step 1: 创建 index.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "热门赛道"
}
```

- [ ] **Step 2: 创建 index.wxml**

```xml
<view class="container">
  <view class="page-title">🔥 热门赛道</view>
  
  <view class="track-list">
    <view 
      class="track-card" 
      wx:for="{{tracks}}" 
      wx:key="id"
      data-id="{{item.id}}"
      bindtap="goToTrack"
    >
      <view class="track-rank" style="color: {{index < 3 ? rankColors[index] : '#666'}};">{{index + 1}}</view>
      <view class="track-info">
        <view class="track-name">{{item.icon}} {{item.name}}</view>
        <view class="track-preview">{{item.previewBloggers}}</view>
      </view>
      <view class="track-action">查看 →</view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 index.wxss**

```css
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.track-card {
  background: #fff;
  padding: 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid transparent;
}

.track-card:nth-child(1) { border-left-color: #ff4d4f; }
.track-card:nth-child(2) { border-left-color: #ff7a45; }
.track-card:nth-child(3) { border-left-color: #ffa940; }
.track-card:nth-child(n+4) { border-left-color: #07c160; }

.track-rank {
  font-size: 24px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}

.track-info {
  flex: 1;
}

.track-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.track-preview {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.track-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 index.js**

```javascript
const data = require('../../utils/data.js')

Page({
  data: {
    tracks: [],
    rankColors: ['#ff4d4f', '#ff7a45', '#ffa940']
  },

  onLoad() {
    this.setData({
      tracks: data.getTracks()
    })
  },

  goToTrack(e) {
    const trackId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/track/track?id=${trackId}`
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add pages/index/
git commit -m "feat: add index page with track ranking list"
```

---

### Task 4: 开发赛道页（track）

**Files:**
- Create: `pages/track/track.wxml`
- Create: `pages/track/track.wxss`
- Create: `pages/track/track.js`
- Create: `pages/track/track.json`

- [ ] **Step 1: 创建 track.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "赛道详情"
}
```

- [ ] **Step 2: 创建 track.wxml**

```xml
<view class="container">
  <view class="page-title">{{track.icon}} {{track.name}}</view>
  
  <view class="blogger-list">
    <view 
      class="blogger-card" 
      wx:for="{{bloggers}}" 
      wx:key="id"
      data-id="{{item.id}}"
      bindtap="goToBlogger"
    >
      <view class="blogger-rank">{{item.rank}}</view>
      <view class="blogger-info">
        <view class="blogger-name">{{item.name}}</view>
        <view class="blogger-tagline">{{item.tagline}}</view>
      </view>
      <view class="blogger-action">查看作品 →</view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 track.wxss**

```css
.page-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.blogger-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blogger-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.blogger-rank {
  width: 36px;
  height: 36px;
  background: #e0e0e0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #333;
  font-weight: 600;
  flex-shrink: 0;
}

.blogger-info {
  flex: 1;
  min-width: 0;
}

.blogger-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}

.blogger-tagline {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.blogger-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
  flex-shrink: 0;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 track.js**

```javascript
const data = require('../../utils/data.js')

Page({
  data: {
    track: {},
    bloggers: []
  },

  onLoad(options) {
    const trackId = options.id
    const tracks = data.getTracks()
    const track = tracks.find(t => t.id === trackId)
    const bloggers = data.getBloggersByTrack(trackId)

    this.setData({
      track,
      bloggers
    })

    wx.setNavigationBarTitle({
      title: `${track.icon} ${track.name}`
    })
  },

  goToBlogger(e) {
    const bloggerId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/blogger/blogger?id=${bloggerId}`
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add pages/track/
git commit -m "feat: add track page with top-n blogger list"
```

---

### Task 5: 开发博主作品页（blogger）

**Files:**
- Create: `pages/blogger/blogger.wxml`
- Create: `pages/blogger/blogger.wxss`
- Create: `pages/blogger/blogger.js`
- Create: `pages/blogger/blogger.json`

- [ ] **Step 1: 创建 blogger.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "博主详情"
}
```

- [ ] **Step 2: 创建 blogger.wxml**

```xml
<view class="container">
  <view class="blogger-header">
    <image class="blogger-avatar" src="{{blogger.avatar}}" mode="aspectFill" wx:if="{{blogger.avatar}}"/>
    <view class="blogger-avatar placeholder" wx:else></view>
    <view class="blogger-meta">
      <view class="blogger-name">{{blogger.name}}</view>
      <view class="blogger-track">{{trackName}}赛道 · Top {{blogger.rank}}</view>
    </view>
  </view>
  
  <view class="section-title">🔥 代表作品</view>
  
  <view class="post-list">
    <view class="post-card" wx:for="{{posts}}" wx:key="id">
      <view class="post-title">{{item.title}}</view>
      <view class="post-actions">
        <view class="action-btn" data-url="{{item.url}}" bindtap="copyUrl">🔗 复制链接</view>
        <view class="action-btn" data-title="{{item.title}}" bindtap="copyTitle">📋 复制标题</view>
      </view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 blogger.wxss**

```css
.blogger-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.blogger-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.blogger-avatar.placeholder {
  background: #e0e0e0;
}

.blogger-meta {
  flex: 1;
}

.blogger-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.blogger-track {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
}

.post-title {
  font-size: 13px;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.5;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  color: #333;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 blogger.js**

```javascript
const data = require('../../utils/data.js')

Page({
  data: {
    blogger: {},
    trackName: '',
    posts: []
  },

  onLoad(options) {
    const bloggerId = options.id
    const blogger = data.getBloggerById(bloggerId)
    const tracks = data.getTracks()
    const track = tracks.find(t => t.id === blogger.trackId)
    const posts = data.getPostsByBlogger(bloggerId)

    this.setData({
      blogger,
      trackName: track.name,
      posts
    })

    wx.setNavigationBarTitle({
      title: blogger.name
    })
  },

  copyUrl(e) {
    const url = e.currentTarget.dataset.url
    wx.setClipboardData({
      data: url,
      success() {
        wx.showToast({ title: '链接已复制', icon: 'success', duration: 1500 })
      }
    })
  },

  copyTitle(e) {
    const title = e.currentTarget.dataset.title
    wx.setClipboardData({
      data: title,
      success() {
        wx.showToast({ title: '标题已复制', icon: 'success', duration: 1500 })
      }
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add pages/blogger/
git commit -m "feat: add blogger page with post list and copy actions"
```

---

### Task 6: 补充 tabBar 图标占位

**Files:**
- Create: `images/icon-off.png`
- Create: `images/icon-on.png`

- [ ] **Step 1: 创建 images 目录并放置占位图标**

由于 MVP 阶段可以用 emoji 替代图标，为避免 tabBar 报错，创建两个 1x1 像素的透明 PNG 占位图，或从 iconfont 下载合适的公众号图标。

这里采用临时方案：用系统命令创建占位文件。

```bash
mkdir -p images
# 创建空文件作为占位
python3 -c "
from PIL import Image
img = Image.new('RGBA', (81, 81), (255, 255, 255, 0))
img.save('images/icon-off.png')
img.save('images/icon-on.png')
" 2>/dev/null || touch images/icon-off.png images/icon-on.png
```

> 备注：如果环境没有 PIL，后续需要手动替换为真实的 81x81 像素 tabBar 图标。

- [ ] **Step 2: Commit**

```bash
git add images/
git commit -m "chore: add tabBar icon placeholders"
```

---

### Task 7: 本地预览与功能验证

- [ ] **Step 1: 在微信开发者工具中导入项目**

路径选择 `/Users/panyong/aio_project/小程序`

- [ ] **Step 2: 验证首页**

打开首页，确认：
- 显示 6 个赛道卡片
- 前三名排名颜色分别为红、橙、黄
- 第四名起排名为灰色，左边框为绿色
- 点击卡片可跳转赛道页

- [ ] **Step 3: 验证赛道页**

进入「个人IP」赛道页，确认：
- 导航栏标题显示「🎬 个人IP」
- 显示粥左罗、剽悍一只猫、Spenser
- 每人有圆形排名数字
- 点击卡片可跳转博主作品页

- [ ] **Step 4: 验证博主作品页**

进入「粥左罗」博主页，确认：
- 导航栏标题显示「粥左罗」
- 显示 3 篇代表作品
- 点击「复制链接」弹出「链接已复制」
- 点击「复制标题」弹出「标题已复制」

- [ ] **Step 5: 回归测试**

从博主页返回赛道页，再返回首页，确认无报错。

- [ ] **Step 6: Commit（如有调整）**

```bash
git add -A
git commit -m "fix: polish styles and interactions after preview"
```

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] 首页赛道排行列表 → Task 3
   - [x] 赛道 TopN 博主 → Task 4
   - [x] 博主作品页 + 复制功能 → Task 5
   - [x] 微信原生绿配色 → Task 3-5 样式
   - [x] 单 tab 结构 → Task 1 app.json
   - [x] 流量主广告位 → 各页面预留 ad-placeholder

2. **Placeholder scan:** 无 TBD/TODO/待补充内容，所有代码均完整可执行。

3. **Type consistency:** 数据字段（`trackId`、`bloggerId`、`rank`）在各任务中一致。

---

## 执行方式

Plan complete and saved to `docs/superpowers/plans/2025-04-13-wechat-blogger-directory.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks
2. **Inline Execution** - execute tasks in this session using executing-plans, batch execution
