<template>
  <div class="novel-list">
    <div class="page-header">
      <h2>我的小说</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建新小说
      </el-button>
    </div>

    <div v-if="novels.length" class="novel-grid">
      <div v-for="novel in novels" :key="novel.id" class="novel-card" @click="goToDetail(novel.id)">
        <div v-if="novel.coverUrl" class="card-cover">
          <img :src="novel.coverUrl" alt="封面" />
        </div>
        <div class="card-genre">{{ novel.genre }}</div>
        <h3 class="card-title">{{ novel.title }}</h3>
        <p class="card-desc">{{ novel.description || '暂无简介' }}</p>
        <div class="card-meta">
          <el-tag :type="statusType(novel.status)" size="small">{{ statusLabel(novel.status) }}</el-tag>
          <span>{{ novel.chapterCount ?? 0 }} 章</span>
          <span>{{ formatWords(novel.totalWords) }}</span>
        </div>
        <div class="card-footer">
          <span class="time">{{ formatTime(novel.updatedAt) }}</span>
          <el-button text type="danger" size="small" @click.stop="handleDelete(novel)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="还没有创建小说，点击上方按钮开始创作吧！" />

    <el-dialog v-model="showCreateDialog" title="创建新小说" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="小说标题" prop="title">
          <el-input v-model="form.title" placeholder="输入你的小说标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="小说类型" prop="genre">
          <el-select v-model="form.genre" placeholder="选择类型" style="width: 100%">
            <el-option v-for="g in genres" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="写作风格">
          <el-select v-model="form.style" placeholder="选择风格（可选）" clearable style="width: 100%">
            <el-option v-for="s in styles" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="小说简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述你的小说故事"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="世界观设定">
          <el-input
            v-model="form.worldSetting"
            type="textarea"
            :rows="3"
            placeholder="描述故事发生的世界背景（可选）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { novelApi } from '@/api'
import type { Novel } from '@/api'

const router = useRouter()
const novels = ref<Novel[]>([])
const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref<FormInstance>()

const genres = ['玄幻', '奇幻', '武侠', '仙侠', '都市', '现实', '科幻', '历史', '军事', '游戏', '体育', '悬疑', '灵异', '言情', '轻小说']
const styles = ['热血', '轻松', '搞笑', '严谨', '黑暗', '温馨', '虐心', '爽文', '慢热']

const form = ref({
  title: '',
  genre: '',
  style: '',
  description: '',
  worldSetting: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入小说标题', trigger: 'blur' }],
  genre: [{ required: true, message: '请选择小说类型', trigger: 'change' }],
}

async function loadNovels() {
  try {
    const res = await novelApi.list()
    const list = res.data || []
    const needBackfill = list.filter((n) => (n.totalWords ?? 0) === 0 && (n.chapterCount ?? 0) > 0)

    if (needBackfill.length === 0) {
      novels.value = list
      return
    }

    const detailMap = new Map<number, number>()
    await Promise.all(
      needBackfill.map(async (n) => {
        try {
          const detail = await novelApi.get(n.id)
          const words = (detail.data.chapters || []).reduce((sum, ch) => sum + (ch.wordCount || 0), 0)
          detailMap.set(n.id, words)
        } catch {
          // ignore backfill errors
        }
      }),
    )

    novels.value = list.map((n) => ({
      ...n,
      totalWords: detailMap.get(n.id) ?? n.totalWords,
    }))
  } catch {
    // handled by interceptor
  }
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const res = await novelApi.create(form.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    router.push(`/novels/${res.data.id}`)
  } catch {
    // handled by interceptor
  } finally {
    creating.value = false
  }
}

async function handleDelete(novel: Novel) {
  const confirmed = await ElMessageBox.confirm(
    `确定要删除《${novel.title}》吗？此操作将删除所有章节和角色。`,
    '删除确认',
    { type: 'warning' },
  ).catch(() => false)

  if (!confirmed) return

  try {
    await novelApi.delete(novel.id)
    ElMessage.success('已删除')
    loadNovels()
  } catch {
    // handled by interceptor
  }
}

function goToDetail(id: number) {
  router.push(`/novels/${id}`)
}

function statusLabel(s: string) {
  return { DRAFT: '草稿', IN_PROGRESS: '创作中', COMPLETED: '已完结' }[s] ?? s
}

function statusType(s: string) {
  return ({ DRAFT: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' } as Record<string, any>)[s] ?? 'info'
}

function formatWords(n: number) {
  if (!n) return '0 字'
  return n >= 10000 ? `${(n / 10000).toFixed(1)} 万字` : `${n} 字`
}

function formatTime(t: string) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}

onMounted(loadNovels)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  color: #303133;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.novel-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #ebeef5;
  position: relative;
}

.novel-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-cover {
  width: 100%;
  height: 160px;
  margin: -24px -24px 16px -24px;
  border-radius: 12px 12px 0 0;
  overflow: hidden;
  background: #f5f7fa;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-genre {
  display: inline-block;
  background: #ecf5ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  margin-bottom: 10px;
}

.card-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 14px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-footer .time {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
