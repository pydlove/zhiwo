<script setup>
import { ref, computed } from 'vue'
import { Card, Input, Button, Table, Tag, message, Progress, Tooltip } from 'ant-design-vue'
import { detectArticleAi } from '../api/articleAiDetect.js'

const content = ref('')
const detecting = ref(false)
const result = ref(null)
const history = ref([])

async function handleDetect() {
  if (!content.value || content.value.trim().length < 10) {
    message.warning('文章内容不能少于10个字')
    return
  }
  detecting.value = true
  result.value = null
  try {
    const data = await detectArticleAi(content.value)
    result.value = data
    // 加入历史记录
    history.value.unshift({
      id: Date.now(),
      content: content.value.slice(0, 80) + (content.value.length > 80 ? '...' : ''),
      score: data.score,
      level: data.level,
      reasons: data.reasons,
      charCount: data.charCount,
      time: new Date().toLocaleTimeString(),
      source: data.source || 'rule',
    })
    if (history.value.length > 20) history.value.pop()
    message.success('检测完成')
  } catch (e) {
    message.error(e.message || '检测失败')
  } finally {
    detecting.value = false
  }
}

function handleClear() {
  content.value = ''
  result.value = null
}

const progressColor = computed(() => {
  if (!result.value) return '#52c41a'
  const s = result.value.score
  if (s >= 70) return '#f5222d'
  if (s >= 40) return '#fa8c16'
  return '#52c41a'
})

const levelColor = computed(() => {
  if (!result.value) return 'green'
  const s = result.value.score
  if (s >= 70) return 'red'
  if (s >= 40) return 'orange'
  return 'green'
})

const historyColumns = [
  {
    title: '文章摘要',
    dataIndex: 'content',
    key: 'content',
    ellipsis: true,
  },
  {
    title: 'AI概率',
    key: 'score',
    width: 100,
    align: 'center',
  },
  {
    title: '风险等级',
    dataIndex: 'level',
    key: 'level',
    width: 100,
    align: 'center',
  },
  {
    title: '字数',
    dataIndex: 'charCount',
    key: 'charCount',
    width: 80,
    align: 'right',
  },
  {
    title: '检测时间',
    dataIndex: 'time',
    key: 'time',
    width: 100,
  },
  {
    title: '来源',
    key: 'source',
    width: 90,
    align: 'center',
  },
]
</script>

<template>
  <Card title="AI 文章检测" :bordered="false" style="border-radius: 8px;">
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px;">
      <!-- 左侧：输入 -->
      <div>
        <div style="margin-bottom: 12px;">
          <div style="font-size: 14px; font-weight: 500; margin-bottom: 8px;">文章内容</div>
          <Input.TextArea
            v-model:value="content"
            placeholder="请粘贴文章内容，支持短文本到长篇文章分析..."
            :rows="18"
            :maxlength="50000"
            show-count
            style="font-size: 14px; line-height: 1.8;"
          />
        </div>
        <div style="display: flex; gap: 12px; margin-bottom: 16px;">
          <Button type="primary" :loading="detecting" @click="handleDetect">
            {{ detecting ? '分析中...' : '开始检测' }}
          </Button>
          <Button @click="handleClear">清空</Button>
        </div>
        <div style="font-size: 12px; color: #999; line-height: 1.8;">
          <div style="font-weight: 500; color: #595959; margin-bottom: 4px;">检测说明</div>
          优先使用深度学习模型检测（Hello-SimpleAI/roberta-chinese），模型服务不可用时自动降级为规则检测。<br>
          <span style="color: #f5222d;">高风险（70分以上）</span>：多项特征明显符合 AI 写作模式<br>
          <span style="color: #fa8c16;">中风险（40-69分）</span>：部分特征异常，建议人工复核<br>
          <span style="color: #52c41a;">低风险（40分以下）</span>：未检测到明显 AI 特征
        </div>
      </div>

      <!-- 右侧：结果 -->
      <div>
        <!-- 检测结果 -->
        <div v-if="result" style="margin-bottom: 24px;">
          <div style="font-size: 14px; font-weight: 500; margin-bottom: 16px;">检测结果</div>

          <!-- 概率仪表 -->
          <div style="text-align: center; margin-bottom: 20px;">
            <Progress
              type="circle"
              :percent="result.score"
              :stroke-color="progressColor"
              :format="p => p + '%'"
              size={120}
              style="margin: 0 auto;"
            />
            <div style="margin-top: 12px; text-align: center;">
              <Tag :color="levelColor" style="font-size: 16px; padding: 2px 12px;">
                {{ result.level }}
              </Tag>
            </div>
            <div style="font-size: 13px; color: #595959; margin-top: 8px;">
              AI 生成概率：{{ result.score }}%
            </div>
            <div style="margin-top: 8px;">
              <Tag v-if="result.source === 'model'" color="blue" style="font-size: 12px;">
                模型检测
              </Tag>
              <Tag v-else color="default" style="font-size: 12px;">
                规则检测
              </Tag>
              <span v-if="result.elapsedMs" style="font-size: 12px; color: #8c8c8c; margin-left: 8px;">
                耗时 {{ result.elapsedMs }}ms
              </span>
            </div>
          </div>

          <!-- 风险原因 -->
          <div v-if="result.reasons && result.reasons.length > 0" style="margin-bottom: 16px;">
            <div style="font-size: 13px; font-weight: 500; margin-bottom: 8px; color: #595959;">判定依据</div>
            <div style="display: flex; flex-direction: column; gap: 6px;">
              <div
                v-for="(reason, idx) in result.reasons"
                :key="idx"
                style="display: flex; align-items: center; gap: 8px; font-size: 13px; color: #8c8c8c;"
              >
                <span style="color: #ff4d4f; font-size: 16px; line-height: 1;">✕</span>
                {{ reason }}
              </div>
            </div>
          </div>

          <!-- 统计信息 -->
          <div style="display: flex; gap: 24px; font-size: 13px; color: #8c8c8c;">
            <div>字符数：<strong style="color: #595959;">{{ result.charCount }}</strong></div>
            <div>词数：<strong style="color: #595959;">{{ result.wordCount }}</strong></div>
          </div>
        </div>

        <!-- 无结果占位 -->
        <div v-else style="text-align: center; padding: 80px 0; color: #bfbfbf; font-size: 14px;">
          <div style="font-size: 48px; margin-bottom: 16px;">🔍</div>
          左侧输入文章内容<br>点击"开始检测"查看 AI 概率
        </div>
      </div>
    </div>

    <!-- 历史记录 -->
    <div v-if="history.length > 0" style="margin-top: 32px;">
      <div style="font-size: 14px; font-weight: 500; margin-bottom: 12px;">检测历史</div>
      <Table
        :data-source="history"
        :columns="historyColumns"
        :pagination="{ pageSize: 10 }"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'score'">
            <Tag :color="record.score >= 70 ? 'red' : record.score >= 40 ? 'orange' : 'green'">
              {{ record.score }}%
            </Tag>
          </template>
          <template v-else-if="column.key === 'level'">
            <Tag :color="record.score >= 70 ? 'red' : record.score >= 40 ? 'orange' : 'green'">
              {{ record.level }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <Tag v-if="record.source === 'model'" color="blue" style="font-size: 12px;">模型</Tag>
            <Tag v-else color="default" style="font-size: 12px;">规则</Tag>
          </template>
        </template>
      </Table>
    </div>
  </Card>
</template>
