import { ref } from 'vue'

/**
 * 在 docx-preview 渲染后的 DOM 中高亮违禁词/敏感词
 */
export function useDocxHighlight() {
  const highlightStats = ref({
    totalChars: 0,
    极限词: 0,
    诱导词: 0,
    敏感词: 0,
    医疗词: 0,
    金融词: 0,
    政治敏感: 0,
    其他: 0,
  })

  /**
   * 对容器内的文本节点进行高亮
   * @param containerEl - docx-preview 渲染的 DOM 容器
   * @param matches - 后端返回的匹配结果数组 [{word, category, severity, count, positions}]
   * @param totalChars - 总字数
   */
  function applyHighlight(containerEl, matches, totalChars) {
    if (!containerEl) return

    // 重置统计
    highlightStats.value = {
      totalChars: totalChars || 0,
      极限词: 0,
      诱导词: 0,
      敏感词: 0,
      医疗词: 0,
      金融词: 0,
      政治敏感: 0,
      其他: 0,
    }

    if (!matches || matches.length === 0) return

    // 统计分类数量
    for (const m of matches) {
      const cat = m.category || '其他'
      if (highlightStats.value[cat] !== undefined) {
        highlightStats.value[cat] += m.count || 0
      }
    }

    // 按词长度降序，优先匹配长词
    const sortedMatches = [...matches].sort((a, b) => (b.word || '').length - (a.word || '').length)
    const words = sortedMatches.map(m => m.word).filter(Boolean)
    if (words.length === 0) return

    // 构建正则：按长度降序，避免短词干扰
    const escapedWords = words.map(w => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    const pattern = new RegExp(`(${escapedWords.join('|')})`, 'g')

    // 递归遍历文本节点
    const walker = document.createTreeWalker(
      containerEl,
      NodeFilter.SHOW_TEXT,
      null,
      false
    )

    const nodesToReplace = []
    let node
    while ((node = walker.nextNode())) {
      if (pattern.test(node.textContent)) {
        nodesToReplace.push(node)
      }
      // 重置正则 lastIndex
      pattern.lastIndex = 0
    }

    for (const textNode of nodesToReplace) {
      const text = textNode.textContent
      const parts = text.split(pattern)
      if (parts.length <= 1) continue

      const fragment = document.createDocumentFragment()
      for (const part of parts) {
        if (!part) continue
        const matchedWord = sortedMatches.find(m => m.word === part)
        if (matchedWord) {
          const span = document.createElement('span')
          const isSensitive = matchedWord.category === '敏感词'
          span.className = isSensitive ? 'highlight-sensitive' : 'highlight-banned'
          span.textContent = part
          fragment.appendChild(span)
        } else {
          fragment.appendChild(document.createTextNode(part))
        }
      }

      if (textNode.parentNode) {
        textNode.parentNode.replaceChild(fragment, textNode)
      }
    }
  }

  /**
   * 清除高亮，恢复原始文本
   */
  function clearHighlight(containerEl) {
    if (!containerEl) return
    const spans = containerEl.querySelectorAll('.highlight-sensitive, .highlight-banned')
    for (const span of spans) {
      const parent = span.parentNode
      if (parent) {
        parent.replaceChild(document.createTextNode(span.textContent), span)
        parent.normalize()
      }
    }
  }

  return {
    highlightStats,
    applyHighlight,
    clearHighlight,
  }
}
