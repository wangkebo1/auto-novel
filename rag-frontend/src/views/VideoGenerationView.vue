<template>
  <div class="video-page">
    <h2>AI 视频生成</h2>
    <p class="page-desc">输入文字描述，AI 自动生成视频（基于 Kling 视频大模型）</p>

    <!-- 创建任务表单 -->
    <el-card class="create-card">
      <template #header><span>创建视频任务</span></template>
      <el-form :model="form" label-width="90px" @submit.prevent="handleCreate">
        <el-form-item label="视频描述" required>
          <el-input
            v-model="form.prompt"
            type="textarea"
            :rows="4"
            maxlength="2500"
            show-word-limit
            placeholder="请输入视频内容描述，例如：一只金色的猫咪在阳光下的花园中奔跑，慢动作，电影感画面"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="视频时长">
              <el-select v-model="form.duration" style="width: 100%">
                <el-option :value="5" label="5 秒" />
                <el-option :value="10" label="10 秒" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="视频尺寸">
              <el-select v-model="form.size" style="width: 100%">
                <el-option value="1280x720" label="1280×720 (横屏)" />
                <el-option value="720x1280" label="720×1280 (竖屏)" />
                <el-option value="960x960" label="960×960 (方形)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生成模式">
              <el-select v-model="form.mode" style="width: 100%">
                <el-option value="std" label="标准模式" />
                <el-option value="pro" label="专业模式" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="handleCreate">
            <el-icon><VideoCamera /></el-icon> 生成视频
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="list-card">
      <template #header>
        <div class="list-header">
          <span>我的视频任务</span>
          <el-button text type="primary" @click="loadTasks">
            <el-icon><Refresh /></el-icon> 刷新列表
          </el-button>
        </div>
      </template>

      <el-empty v-if="tasks.length === 0" description="暂无视频任务" />

      <div v-else class="task-grid">
        <el-card
          v-for="task in tasks"
          :key="task.id"
          shadow="hover"
          class="task-card"
        >
          <!-- 视频预览 / 封面 / 状态占位 -->
          <div class="task-preview">
            <video
              v-if="task.status === 'completed' && task.videoUrl"
              :src="task.videoUrl"
              :poster="task.coverUrl || undefined"
              controls
              preload="metadata"
              class="task-video"
            />
            <div v-else class="task-placeholder">
              <el-icon v-if="isProcessing(task.status)" size="48" color="#409EFF" class="spin-icon">
                <Loading />
              </el-icon>
              <el-icon v-else-if="task.status === 'failed'" size="48" color="#F56C6C">
                <CircleCloseFilled />
              </el-icon>
              <el-icon v-else size="48" color="#909399">
                <VideoCamera />
              </el-icon>
              <p>{{ statusText(task.status) }}</p>
            </div>
          </div>

          <!-- 任务信息 -->
          <div class="task-info">
            <el-text class="task-prompt" truncated>{{ task.prompt }}</el-text>
            <div class="task-meta">
              <el-tag :type="statusType(task.status)" size="small">{{ statusText(task.status) }}</el-tag>
              <el-tag size="small" type="info">{{ task.duration }}s</el-tag>
              <el-tag size="small" type="info">{{ task.size }}</el-tag>
            </div>
            <el-text type="info" size="small">{{ formatTime(task.createdAt) }}</el-text>
            <p v-if="task.errorMessage" class="error-msg">{{ task.errorMessage }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="task-actions">
            <el-button
              v-if="isProcessing(task.status)"
              text
              type="primary"
              size="small"
              :loading="refreshingId === task.id"
              @click="handleRefresh(task)"
            >
              <el-icon><Refresh /></el-icon> 刷新状态
            </el-button>
            <el-button
              v-if="task.status === 'completed' && task.videoUrl"
              text
              type="success"
              size="small"
              @click="handleDownload(task)"
            >
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-popconfirm title="确定删除该任务？" @confirm="handleDelete(task)">
              <template #reference>
                <el-button text type="danger" size="small">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { videoApi, type VideoTask } from '@/api'
import { ElMessage } from 'element-plus'

const form = ref({
  prompt: '',
  duration: 5,
  size: '1280x720',
  mode: 'std',
})

const creating = ref(false)
const tasks = ref<VideoTask[]>([])
const refreshingId = ref<number | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadTasks()
  // 前端每 15 秒自动刷新列表
  pollTimer = setInterval(loadTasks, 15000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function loadTasks() {
  try {
    const res = await videoApi.list()
    tasks.value = res.data
  } catch {
    // 拦截器已处理
  }
}

async function handleCreate() {
  if (!form.value.prompt.trim()) {
    ElMessage.warning('请输入视频描述')
    return
  }
  creating.value = true
  try {
    await videoApi.create({
      prompt: form.value.prompt,
      duration: form.value.duration,
      size: form.value.size,
      mode: form.value.mode,
    })
    ElMessage.success('视频任务已创建，正在生成中...')
    form.value.prompt = ''
    await loadTasks()
  } finally {
    creating.value = false
  }
}

async function handleRefresh(task: VideoTask) {
  refreshingId.value = task.id
  try {
    const res = await videoApi.refresh(task.id)
    // 更新列表中对应任务
    const idx = tasks.value.findIndex((t) => t.id === task.id)
    if (idx >= 0) tasks.value[idx] = res.data
    if (res.data.status === 'completed') {
      ElMessage.success('视频生成完成！')
    }
  } finally {
    refreshingId.value = null
  }
}

async function handleDelete(task: VideoTask) {
  await videoApi.delete(task.id)
  ElMessage.success('已删除')
  tasks.value = tasks.value.filter((t) => t.id !== task.id)
}

function handleDownload(task: VideoTask) {
  if (task.videoUrl) {
    window.open(task.videoUrl, '_blank')
  }
}

function isProcessing(status: string) {
  return !['completed', 'failed'].includes(status)
}

function statusText(status: string) {
  const map: Record<string, string> = {
    queued: '排队中',
    initializing: '初始化',
    in_progress: '生成中',
    downloading: '下载中',
    uploading: '上传中',
    completed: '已完成',
    failed: '失败',
  }
  return map[status] ?? status
}

function statusType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'queued') return 'info'
  return 'warning'
}

function formatTime(iso: string) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('zh-CN')
}
</script>

<style scoped>
.video-page {
  max-width: 1100px;
  margin: 0 auto;
}

.video-page h2 {
  margin-bottom: 4px;
}

.page-desc {
  color: #909399;
  margin-bottom: 20px;
}

.create-card {
  margin-bottom: 20px;
}

.list-card {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.task-card {
  display: flex;
  flex-direction: column;
}

.task-preview {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.task-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.spin-icon {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-info {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-prompt {
  font-size: 14px;
  line-height: 1.4;
}

.task-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.error-msg {
  color: #F56C6C;
  font-size: 12px;
  margin: 0;
}

.task-actions {
  display: flex;
  gap: 4px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}
</style>
