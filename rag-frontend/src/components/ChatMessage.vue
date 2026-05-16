<template>
  <div :class="['message-item', message.role]">
    <!-- 用户消息 -->
    <template v-if="message.role === 'user'">
      <div class="message-content user-content">
        <p>{{ message.content }}</p>
        <div v-if="message.imageUrl" class="message-image">
          <img :src="message.imageUrl" :alt="message.imageName || 'attachment'" />
          <span v-if="message.imageName" class="image-name">{{ message.imageName }}</span>
        </div>
      </div>
      <div class="avatar user-avatar">
        <el-icon><User /></el-icon>
      </div>
    </template>

    <!-- AI 消息 -->
    <template v-else>
      <div class="avatar ai-avatar">
        <el-icon><DataAnalysis /></el-icon>
      </div>
      <div class="message-content ai-content">
        <!-- 加载动画 -->
        <div
          v-if="message.loading"
          class="loading-dots"
        >
          <span /><span /><span />
        </div>
        <!-- 回答内容（Markdown 渲染） -->
        <div
          v-else
          class="answer-text"
          v-html="renderedContent"
        />

        <!-- 参考来源 -->
        <div
          v-if="message.sources && message.sources.length > 0"
          class="sources"
        >
          <div class="sources-title">
            <el-icon><Document /></el-icon> 参考来源
          </div>
          <div class="source-tags">
            <el-tag
              v-for="(src, i) in message.sources"
              :key="i"
              size="small"
              type="info"
              effect="plain"
            >
              {{ src }}
            </el-tag>
          </div>
        </div>

        <!-- 时间戳 -->
        <div class="timestamp">
          {{ formatTime(message.timestamp) }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { Message } from '@/stores/useChat'

const props = defineProps<{ message: Message }>()

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  const raw = marked.parse(props.message.content) as string
  return DOMPurify.sanitize(raw)
})

function formatTime(date: Date) {
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.user-avatar {
  background: #409EFF;
  color: #fff;
}

.ai-avatar {
  background: #f0f9ff;
  color: #409EFF;
  border: 1px solid #bde0fe;
}

.message-content {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}

.user-content {
  background: #409EFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-image {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-image img {
  width: 120px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.message-image .image-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.ai-content {
  background: #f8f9fa;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

/* Markdown 渲染样式 */
.answer-text :deep(p) { margin: 0 0 8px; }
.answer-text :deep(p:last-child) { margin-bottom: 0; }
.answer-text :deep(code) {
  background: #f0f0f0;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Consolas', monospace;
}
.answer-text :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.answer-text :deep(pre code) { background: none; padding: 0; }
.answer-text :deep(ul), .answer-text :deep(ol) { padding-left: 20px; margin: 6px 0; }
.answer-text :deep(li) { margin-bottom: 4px; }

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 6px;
  padding: 4px 0;
}
.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409EFF;
  animation: bounce 1.2s infinite ease-in-out;
}
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}

.sources-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.timestamp {
  margin-top: 8px;
  font-size: 11px;
  color: #c0c4cc;
  text-align: right;
}
</style>
