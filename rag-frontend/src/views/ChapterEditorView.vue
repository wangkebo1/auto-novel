<template>
  <div class="chapter-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button text @click="$router.push(`/novels/${novelId}`)">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="sep">/</span>
        <span class="chapter-label">第{{ chapter?.chapterNumber }}章</span>
        <span class="chapter-title">{{ chapter?.title }}</span>
      </div>
      <div class="header-right">
        <el-tag :type="chapterStatusType(chapter?.status)" size="small">
          {{ chapterStatusLabel(chapter?.status) }}
        </el-tag>
        <span class="word-count">{{ chapter?.wordCount || 0 }} 字</span>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <div class="editor-body">
      <div class="sidebar">
        <div class="sidebar-section">
          <h4>章节大纲</h4>
          <el-input v-model="outlineText" type="textarea" :rows="8" placeholder="本章大纲..." />
        </div>

        <el-divider />

        <div class="sidebar-section">
          <h4>章节摘要</h4>
          <p class="summary-text">{{ chapter?.summary || '暂无摘要，生成后会自动补全。' }}</p>
        </div>

        <el-divider />

        <div class="sidebar-section">
          <h4>创作备注</h4>
          <el-input
            v-model="chapterNotes"
            type="textarea"
            :rows="4"
            placeholder="记录创作灵感、待修改点等..."
            @blur="saveNotes"
          />
        </div>

        <el-divider />

        <div class="sidebar-section">
          <div class="section-head">
            <h4>剧情分支建议</h4>
            <el-button link size="small" :loading="loadingBranchSuggestions" @click="loadBranchSuggestions">
              {{ loadedBranchSuggestions ? '刷新' : '生成' }}
            </el-button>
          </div>
          <p class="hint-text branch-hint">让 AI 给出 3 条不同走向，适合卡文时快速找灵感。</p>
          <div v-if="branchSuggestions.length" class="branch-list">
            <div
              v-for="(item, index) in branchSuggestions"
              :key="`${item.title}-${index}`"
              class="branch-card"
            >
              <div class="branch-card-header">
                <span class="branch-index">方案 {{ index + 1 }}</span>
                <strong class="branch-title">{{ item.title }}</strong>
              </div>
              <p><span>走向：</span>{{ item.direction }}</p>
              <p><span>冲突：</span>{{ item.conflict }}</p>
              <p><span>钩子：</span>{{ item.hook }}</p>
              <div class="branch-actions">
                <el-button text size="small" @click="appendSuggestionToNotes(item)">写入备注</el-button>
              </div>
            </div>
          </div>
          <el-empty
            v-else-if="loadedBranchSuggestions && !loadingBranchSuggestions"
            :image-size="60"
            description="这次没拿到建议，稍后可以再试"
          />
        </div>

        <el-divider />

        <div class="sidebar-section">
          <h4>AI 改写润色</h4>
          <el-select v-model="rewriteStyle" placeholder="选择风格" size="small" style="width: 100%; margin-bottom: 8px">
            <el-option label="生动形象" value="vivid" />
            <el-option label="简洁明快" value="concise" />
            <el-option label="专业严谨" value="professional" />
            <el-option label="古风雅致" value="ancient" />
            <el-option label="幽默风趣" value="humorous" />
          </el-select>
          <el-button size="small" :loading="rewriting" style="width: 100%" @click="handleRewrite">改写全文</el-button>
        </div>

        <el-divider />

        <div class="sidebar-section">
          <h4>敏感词检测</h4>
          <el-button size="small" :loading="checkingSensitive" @click="checkSensitiveWords">检测敏感词</el-button>
          <el-alert
            v-if="checkedSensitive && sensitiveWords.length > 0"
            type="error"
            :closable="false"
            style="margin-top: 8px"
          >
            <template #title>
              发现 {{ sensitiveWords.length }} 个敏感词：{{ sensitiveWords.join('、') }}
            </template>
          </el-alert>
          <el-alert
            v-else-if="checkedSensitive && sensitiveWords.length === 0 && !checkingSensitive"
            type="success"
            :closable="false"
            style="margin-top: 8px"
          >
            未发现敏感词
          </el-alert>
        </div>

        <el-divider />

        <div class="sidebar-section">
          <p class="hint-text">
            章节内容既可以由小说详情页的一键创作批量生成，也可以在这里直接手动编辑。
          </p>
        </div>
      </div>

      <div class="content-area">
        <div v-if="chapter?.status === 'GENERATING'" class="stream-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>后台正在生成本章...</span>
        </div>
        <el-input
          v-model="contentText"
          type="textarea"
          class="content-editor"
          placeholder="章节内容会显示在这里，你也可以直接编辑..."
          resize="none"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { novelApi } from '@/api'
import type { BranchSuggestion, ChapterDetail } from '@/api'
import { usePolling } from '@/composables/usePolling'
import { handleViewError } from '@/utils/error'

const route = useRoute()
const novelId = Number(route.params.novelId)
const chapterId = Number(route.params.chapterId)

const chapter = ref<ChapterDetail | null>(null)
const outlineText = ref('')
const contentText = ref('')
const chapterNotes = ref('')
const saving = ref(false)
const sensitiveWords = ref<string[]>([])
const checkingSensitive = ref(false)
const checkedSensitive = ref(false)
const rewriteStyle = ref('vivid')
const rewriting = ref(false)
const branchSuggestions = ref<BranchSuggestion[]>([])
const loadingBranchSuggestions = ref(false)
const loadedBranchSuggestions = ref(false)

const { start: startPolling, stop: stopPolling } = usePolling(async () => {
  await loadChapter()
  if (chapter.value && chapter.value.status !== 'GENERATING') {
    stopPolling()
  }
}, 4000)

async function loadChapter() {
  try {
    const res = await novelApi.getChapter(novelId, chapterId)
    chapter.value = res.data
    outlineText.value = res.data.outline || ''
    chapterNotes.value = res.data.notes || ''
    if (res.data.status === 'GENERATING' || !contentText.value) {
      contentText.value = res.data.content || ''
    }
  } catch (error) {
    handleViewError('ChapterEditor', error, '章节加载失败', false)
  }
}

async function handleSave() {
  saving.value = true
  try {
    await novelApi.updateChapter(novelId, chapterId, {
      title: chapter.value?.title || '',
      outline: outlineText.value,
      content: contentText.value,
    })
    ElMessage.success('已保存')
    await loadChapter()
  } catch (error) {
    handleViewError('ChapterEditor', error, '保存章节失败')
  } finally {
    saving.value = false
  }
}

async function saveNotes() {
  try {
    await novelApi.updateChapterNotes(novelId, chapterId, chapterNotes.value)
  } catch (error) {
    console.error('保存备注失败:', error)
  }
}

async function loadBranchSuggestions() {
  loadingBranchSuggestions.value = true
  try {
    const res = await novelApi.suggestChapterBranches(novelId, chapterId)
    branchSuggestions.value = res.data || []
    loadedBranchSuggestions.value = true
    if (!branchSuggestions.value.length) {
      ElMessage.info('暂时没有生成建议，可以稍后再试')
    }
  } catch (error) {
    branchSuggestions.value = buildLocalBranchSuggestions()
    loadedBranchSuggestions.value = true
    ElMessage.warning('AI 建议生成失败，已切换为本地建议')
  } finally {
    loadingBranchSuggestions.value = false
  }
}

function buildLocalBranchSuggestions(): BranchSuggestion[] {
  const chapterTitle = chapter.value?.title?.trim() || `第${chapter.value?.chapterNumber || ''}章`
  const summarySource = chapter.value?.summary?.trim() || outlineText.value.trim() || contentText.value.trim().slice(0, 80)

  return [
    {
      title: '冲突升级',
      direction: `围绕${chapterTitle}继续推进，让主角刚建立的优势遭遇更强阻力。`,
      conflict: '原本可控的局面突然失衡，主角必须立刻在两个代价都不小的选择中站队。',
      hook: '章节末尾抛出更大的危机，制造强烈追读点。',
    },
    {
      title: '秘密揭开',
      direction: `从“${summarySource || '当前剧情线'}”延伸，揭开一个与主角或核心势力有关的新秘密。`,
      conflict: '真相会动摇现有信任关系，也可能让主角之前的判断被推翻。',
      hook: '先揭开一半答案，把最关键的一层留到下一章。',
    },
    {
      title: '人物变局',
      direction: '让一个关键人物突然改变立场、身份或目标，带来新的关系张力。',
      conflict: '盟友、亲人或对手的转向打乱原计划，迫使主角重新布局。',
      hook: '用一次意外站队或关键背叛作为章节收束。',
    },
  ]
}

function formatSuggestionForNotes(item: BranchSuggestion) {
  return [`[${item.title}]`, `走向：${item.direction}`, `冲突：${item.conflict}`, `钩子：${item.hook}`].join('\n')
}

async function appendSuggestionToNotes(item: BranchSuggestion) {
  const nextNotes = chapterNotes.value.trim()
    ? `${chapterNotes.value.trim()}\n\n${formatSuggestionForNotes(item)}`
    : formatSuggestionForNotes(item)
  chapterNotes.value = nextNotes
  try {
    const res = await novelApi.updateChapterNotes(novelId, chapterId, nextNotes)
    chapter.value = res.data
    ElMessage.success('已写入备注')
  } catch (error) {
    handleViewError('ChapterEditor', error, '写入备注失败')
  }
}

async function checkSensitiveWords() {
  if (!contentText.value) return
  checkingSensitive.value = true
  try {
    const res = await novelApi.checkSensitive(contentText.value)
    sensitiveWords.value = res.data || []
    checkedSensitive.value = true
  } catch (error) {
    console.error('检测敏感词失败:', error)
  } finally {
    checkingSensitive.value = false
  }
}

async function handleRewrite() {
  if (!contentText.value) {
    ElMessage.warning('没有内容可改写')
    return
  }
  rewriting.value = true
  try {
    const res = await novelApi.rewrite({ content: contentText.value, style: rewriteStyle.value })
    contentText.value = res.data
    ElMessage.success('改写完成')
  } catch (error) {
    handleViewError('ChapterEditor', error, '改写失败')
  } finally {
    rewriting.value = false
  }
}

function chapterStatusLabel(status?: string) {
  if (!status) return ''
  return {
    OUTLINE: '大纲',
    GENERATING: '生成中',
    GENERATED: '已生成',
    EDITED: '已编辑',
  }[status] ?? status
}

function chapterStatusType(status?: string) {
  if (!status) return 'info'
  return ({
    OUTLINE: 'info',
    GENERATING: 'warning',
    GENERATED: 'success',
    EDITED: '',
  } as Record<string, any>)[status] ?? 'info'
}

onMounted(async () => {
  await loadChapter()
  if (chapter.value?.status === 'GENERATING') {
    startPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})

watch(contentText, () => {
  checkedSensitive.value = false
})
</script>

<style scoped>
.chapter-editor { display: flex; flex-direction: column; height: 100vh; padding: 0; }

.editor-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 8px; }
.sep { color: #dcdfe6; }
.chapter-label { color: #909399; font-size: 14px; }
.chapter-title { font-weight: 600; font-size: 16px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.word-count { font-size: 12px; color: #909399; }

.editor-body { display: flex; flex: 1; overflow: hidden; }

.sidebar {
  width: 300px;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  padding: 20px;
  overflow-y: auto;
}
.sidebar-section h4 { font-size: 14px; margin: 0 0 10px; color: #303133; }
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.section-head h4 {
  margin: 0;
}
.summary-text { margin: 0; color: #606266; line-height: 1.7; }
.hint-text { margin: 0; color: #909399; line-height: 1.7; font-size: 13px; }
.branch-hint {
  margin-bottom: 10px;
}
.branch-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.branch-card {
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}
.branch-card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}
.branch-index {
  font-size: 12px;
  color: #909399;
}
.branch-title {
  font-size: 14px;
  color: #303133;
}
.branch-card p {
  margin: 0 0 8px;
  color: #606266;
  line-height: 1.6;
  font-size: 13px;
}
.branch-card p span {
  color: #303133;
  font-weight: 600;
}
.branch-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.content-area { flex: 1; display: flex; flex-direction: column; position: relative; padding: 0; }

.stream-indicator {
  position: absolute; top: 12px; right: 16px; z-index: 10;
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 20px;
  color: #409EFF;
  font-size: 13px;
}

:deep(.content-editor) { height: 100%; }
:deep(.content-editor .el-textarea__inner) {
  height: 100% !important;
  border: none;
  border-radius: 0;
  padding: 24px 32px;
  font-size: 15px;
  line-height: 1.8;
  resize: none;
}
</style>
