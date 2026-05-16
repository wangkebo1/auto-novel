<template>
  <div class="cover-generator">
    <div class="cover-preview">
      <div v-if="coverUrl" class="preview-image">
        <img :src="coverUrl" alt="封面预览" />
      </div>
      <div v-else class="preview-placeholder">
        <el-icon :size="60"><Picture /></el-icon>
        <p>暂无封面</p>
      </div>
    </div>

    <div class="cover-actions">
      <el-upload
        :show-file-list="false"
        :before-upload="handleUpload"
        accept="image/*"
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          上传图片
        </el-button>
      </el-upload>

      <el-button @click="showTemplates = true">
        <el-icon><MagicStick /></el-icon>
        模板生成
      </el-button>

      <el-button v-if="coverUrl" type="danger" plain @click="handleRemove">
        <el-icon><Delete /></el-icon>
        删除封面
      </el-button>
    </div>

    <el-dialog v-model="showTemplates" title="选择封面模板" width="800px" @opened="renderPreviews">
      <div class="template-grid">
        <div
          v-for="(template, index) in templates"
          :key="index"
          class="template-item"
          @click="generateCover(template)"
        >
          <canvas :id="`preview-${index}`" width="300" height="400"></canvas>
          <p>{{ template.name }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

interface Props {
  title: string
  genre: string
  coverUrl?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [url: string]
}>()

const showTemplates = ref(false)

const templates = [
  { name: '简约风格', gradient: ['#667eea', '#764ba2'], textColor: '#fff' },
  { name: '暖色调', gradient: ['#f093fb', '#f5576c'], textColor: '#fff' },
  { name: '冷色调', gradient: ['#4facfe', '#00f2fe'], textColor: '#fff' }
]

function renderPreviews() {
  setTimeout(() => {
    templates.forEach((template, index) => {
      const canvas = document.getElementById(`preview-${index}`) as HTMLCanvasElement
      if (!canvas) return
      const ctx = canvas.getContext('2d')!
      const gradient = ctx.createLinearGradient(0, 0, 0, 400)
      gradient.addColorStop(0, template.gradient[0])
      gradient.addColorStop(1, template.gradient[1])
      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, 300, 400)
      ctx.fillStyle = template.textColor
      ctx.font = 'bold 30px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('示例标题', 150, 200)
      ctx.font = '16px sans-serif'
      ctx.fillText('类型', 150, 250)
    })
  }, 100)
}

function handleUpload(file: File) {
  const reader = new FileReader()
  reader.onload = (e) => {
    const url = e.target?.result as string
    emit('update', url)
    ElMessage.success('封面上传成功')
  }
  reader.readAsDataURL(file)
  return false
}

function generateCover(template: any) {
  const canvas = document.createElement('canvas')
  canvas.width = 600
  canvas.height = 800
  const ctx = canvas.getContext('2d')!

  const gradient = ctx.createLinearGradient(0, 0, 0, 800)
  gradient.addColorStop(0, template.gradient[0])
  gradient.addColorStop(1, template.gradient[1])
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 600, 800)

  ctx.fillStyle = template.textColor
  ctx.textAlign = 'center'

  // 标题自动换行
  const title = props.title
  const maxWidth = 500
  ctx.font = 'bold 50px sans-serif'

  if (ctx.measureText(title).width > maxWidth) {
    // 标题太长，分两行
    const words = title.split('')
    let line1 = ''
    let line2 = ''
    for (let i = 0; i < words.length; i++) {
      if (ctx.measureText(line1 + words[i]).width < maxWidth) {
        line1 += words[i]
      } else {
        line2 = words.slice(i).join('')
        break
      }
    }
    ctx.fillText(line1, 300, 360)
    ctx.fillText(line2, 300, 440)
  } else {
    ctx.fillText(title, 300, 400)
  }

  ctx.font = '30px sans-serif'
  ctx.fillText(props.genre, 300, 520)

  const url = canvas.toDataURL('image/jpeg', 0.8)
  emit('update', url)
  showTemplates.value = false
  ElMessage.success('封面生成成功')
}

function handleRemove() {
  emit('update', '')
  ElMessage.success('封面已删除')
}
</script>

<style scoped>
.cover-generator {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cover-preview {
  width: 200px;
  height: 267px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.cover-actions {
  display: flex;
  gap: 8px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.template-item {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}

.template-item:hover {
  transform: scale(1.05);
}

.template-item canvas {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.template-item p {
  margin-top: 8px;
  font-size: 14px;
}
</style>
