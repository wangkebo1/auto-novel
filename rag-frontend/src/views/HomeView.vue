<template>
  <div class="home-page">
    <div class="hero">
      <div class="hero-badge">AI 驱动的创作引擎</div>
      <h1 class="hero-title">
        让 AI 帮你写出
        <br>
        <span class="gradient-text">下一部精彩小说</span>
      </h1>
      <p class="hero-subtitle">
        从灵感到成稿，AI 小说工坊覆盖大纲生成、角色设定、逐章创作全流程。
        <br>
        只需描述你的故事设定，AI 即可为你构建完整的小说世界。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" round @click="$router.push('/novels')">
          <el-icon><EditPen /></el-icon>
          开始创作
        </el-button>
        <el-button size="large" round @click="$router.push('/chat')">
          <el-icon><ChatDotSquare /></el-icon>
          智能问答
        </el-button>
      </div>
      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-num">{{ novelCount }}</span>
          <span class="stat-label">已创作小说</span>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <span class="stat-num">{{ formatStatWords(totalWords) }}</span>
          <span class="stat-label">累计创作字数</span>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <span class="stat-num">{{ chapterCount }}</span>
          <span class="stat-label">已生成章节</span>
        </div>
      </div>
    </div>

    <div class="features">
      <div v-for="f in features" :key="f.title" class="feature-card" @click="$router.push(f.route)">
        <div class="feature-icon-wrap" :style="{ background: f.bg }">
          <el-icon size="28" color="#fff">
            <component :is="f.icon" />
          </el-icon>
        </div>
        <div class="feature-body">
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
        <el-icon class="feature-arrow" color="#c0c4cc">
          <ArrowRight />
        </el-icon>
      </div>
    </div>

    <div class="quick-section">
      <h2 class="section-title">快捷操作</h2>
      <div class="quick-grid">
        <div class="quick-card" @click="$router.push('/novels')">
          <el-icon size="32" color="#409EFF"><Plus /></el-icon>
          <span>创建新小说</span>
        </div>
        <div class="quick-card" @click="$router.push('/knowledge-base')">
          <el-icon size="32" color="#67C23A"><Upload /></el-icon>
          <span>上传知识库</span>
        </div>
        <div class="quick-card" @click="$router.push('/video')">
          <el-icon size="32" color="#E6A23C"><VideoCamera /></el-icon>
          <span>AI 生成视频</span>
        </div>
        <div class="quick-card" @click="$router.push('/chat')">
          <el-icon size="32" color="#909399"><ChatDotSquare /></el-icon>
          <span>开始对话</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { novelApi } from '@/api'
import type { Novel } from '@/api'
import { handleViewError } from '@/utils/error'

const novelCount = ref(0)
const totalWords = ref(0)
const chapterCount = ref(0)

const features = [
  {
    icon: 'MagicStick',
    bg: 'linear-gradient(135deg, #409EFF, #66b1ff)',
    title: 'AI 大纲生成',
    desc: '输入设定与角色，一键生成完整章节大纲',
    route: '/novels',
  },
  {
    icon: 'EditPen',
    bg: 'linear-gradient(135deg, #67C23A, #85ce61)',
    title: '逐章智能创作',
    desc: '基于上下文摘要，AI 逐章生成内容并实时预览',
    route: '/novels',
  },
  {
    icon: 'User',
    bg: 'linear-gradient(135deg, #E6A23C, #f0c78a)',
    title: '角色与世界观',
    desc: '自定义角色性格、关系与世界设定',
    route: '/novels',
  },
  {
    icon: 'ChatDotSquare',
    bg: 'linear-gradient(135deg, #909399, #b1b3b8)',
    title: '智能问答',
    desc: '基于知识库 RAG 检索，精准回答你的问题',
    route: '/chat',
  },
]

function formatStatWords(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)} 万`
  return String(n)
}

async function getEffectiveNovels(list: Novel[]) {
  const needBackfill = list.filter((n) => (n.totalWords ?? 0) === 0 && (n.chapterCount ?? 0) > 0)
  if (needBackfill.length === 0) {
    return list
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

  return list.map((n) => ({
    ...n,
    totalWords: detailMap.get(n.id) ?? n.totalWords,
  }))
}

onMounted(async () => {
  try {
    const res = await novelApi.list()
    const novels = (res.data || []) as Novel[]
    const effectiveNovels = await getEffectiveNovels(novels)
    novelCount.value = effectiveNovels.length
    totalWords.value = effectiveNovels.reduce((sum, n) => sum + (n.totalWords || 0), 0)
    chapterCount.value = effectiveNovels.reduce((sum, n) => sum + (n.chapterCount || 0), 0)
  } catch (error) {
    handleViewError('HomeView', error, '首页数据加载失败', false)
  }
})
</script>

<style scoped>
.home-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}

.hero {
  text-align: center;
  padding: 56px 0 40px;
}

.hero-badge {
  display: inline-block;
  padding: 4px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 20px;
  margin-bottom: 20px;
}

.hero-title {
  font-size: 36px;
  font-weight: 800;
  line-height: 1.3;
  color: #1d1d1f;
  margin-bottom: 16px;
}

.gradient-text {
  background: linear-gradient(135deg, #409eff 0%, #7c8dff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 15px;
  color: #86868b;
  line-height: 1.7;
  margin-bottom: 32px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 40px;
}

.hero-stats {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  padding: 20px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #1d1d1f;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: #86868b;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #ebeef5;
}

.features {
  margin-bottom: 24px;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #ebeef5;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.feature-card:hover {
  border-color: #d9ecff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.feature-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-body {
  flex: 1;
  min-width: 0;
}

.feature-body h3 {
  margin: 0 0 4px;
  color: #303133;
  font-size: 16px;
}

.feature-body p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.quick-section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 12px;
  color: #303133;
  font-size: 18px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  padding: 18px 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-card:hover {
  border-color: #d9ecff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.quick-card span {
  font-size: 13px;
  color: #606266;
}

@media (max-width: 900px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero {
    padding-top: 36px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .hero-stats {
    flex-direction: column;
    gap: 14px;
    padding: 16px 20px;
  }

  .stat-divider {
    width: 100%;
    height: 1px;
  }
}
</style>
