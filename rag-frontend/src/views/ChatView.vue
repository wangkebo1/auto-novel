<template>
  <div class="chat-page">
    <!-- 左侧：知识库选择 -->
    <div class="chat-sidebar">
      <div class="sidebar-title">
        选择知识库
      </div>
      <div
        v-if="kbLoading"
        class="kb-skeleton"
      >
        <el-skeleton
          v-for="n in 4"
          :key="n"
          animated
          :rows="1"
        >
          <template #template>
            <el-skeleton-item
              variant="text"
              style="width: 88%; margin: 10px 0;"
            />
          </template>
        </el-skeleton>
      </div>
      <el-radio-group
        v-else
        v-model="selectedKbId"
        class="kb-radio-group"
      >
        <el-radio
          :value="null"
          class="kb-radio-item"
        >
          <span>全部知识库</span>
        </el-radio>
        <el-radio
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :value="kb.id"
          class="kb-radio-item"
        >
          <span>{{ kb.name }}</span>
        </el-radio>
      </el-radio-group>

      <el-divider />

      <div class="sidebar-title">
        问答设置
      </div>
      <el-form
        label-position="top"
        size="small"
      >
        <el-form-item label="检索数量 (Top-K)">
          <el-slider
            v-model="topK"
            :min="1"
            :max="10"
            show-stops
          />
        </el-form-item>
        <el-form-item label="流式输出">
          <el-switch v-model="useStream" />
        </el-form-item>
      </el-form>

      <el-button
        size="small"
        style="width: 100%"
        @click="chatStore.clearMessages()"
      >
        <el-icon><Delete /></el-icon> 清空对话
      </el-button>
    </div>

    <!-- 右侧：对话区域 -->
    <div class="chat-main">
      <!-- 消息列表 -->
      <div
        ref="messageListRef"
        class="message-list"
      >
        <!-- 欢迎语 -->
        <div
          v-if="messages.length === 0"
          class="empty-chat"
        >
          <el-icon
            size="64"
            color="#dcdfe6"
          >
            <ChatDotRound />
          </el-icon>
          <p>请在左侧选择知识库，然后输入您的问题</p>
        </div>

        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
        />
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <div class="attachment-row">
          <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            class="image-input"
            @change="handleImageChange"
          />
          <el-button size="mini" type="text" class="attachment-btn" @click="triggerImageUpload">
            <el-icon><Upload /></el-icon> 上传图片
          </el-button>
          <span v-if="attachedImage" class="attachment-label">图片已附加为提问的一部分</span>
        </div>
        <div v-if="attachedImage" class="attachment-preview">
          <img :src="attachedImage.base64" :alt="attachedImage.fileName" />
          <div class="preview-meta">
            <span>{{ attachedImage.fileName }}</span>
            <el-button type="text" size="mini" @click="clearAttachment">移除</el-button>
          </div>
        </div>
        <div v-if="attachedImage" class="attachment-status">图片已附加，聊天发送后会随请求一起上传。</div>
        <div class="input-row">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="请输入您的问题... (Enter 发送，Shift+Enter 换行)"
          :disabled="isLoading"
          @keydown.enter.exact.prevent="sendMessage"
          @paste.native="handlePaste"
        />
          <el-button
            type="primary"
            :loading="isLoading"
            :disabled="!inputText.trim() && !attachedImage"
            class="send-btn"
            @click="sendMessage"
          >
            <el-icon v-if="!isLoading">
              <Promotion />
            </el-icon>
            {{ isLoading ? '思考中...' : '发送' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeBaseApi, chatApi, type KnowledgeBase } from '@/api'
import { useChatStore } from '@/stores/useChat'
import ChatMessage from '@/components/ChatMessage.vue'
import { handleViewError } from '@/utils/error'

interface ChatAttachment {
  fileName: string
  mimeType: string
  base64: string
}

const chatStore = useChatStore()
const messages = computed(() => chatStore.messages)

const knowledgeBases = ref<KnowledgeBase[]>([])
const kbLoading = ref(true)
const selectedKbId = ref<number | null>(chatStore.selectedKbId)
const topK = ref(5)
const useStream = ref(true)
const inputText = ref('')
const isLoading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const attachedImage = ref<ChatAttachment | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)

onMounted(async () => {
  try {
    const res = await knowledgeBaseApi.list()
    knowledgeBases.value = res.data ?? []
  } catch (error) {
    handleViewError('ChatView', error, '知识库加载失败', false)
  } finally {
    kbLoading.value = false
  }
})

// 自动滚动到底部
watch(
  messages,
  async () => {
    await nextTick()
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  },
  { deep: true }
)

function triggerImageUpload() {
  imageInputRef.value?.click()
}

function clearAttachment() {
  attachedImage.value = null
}

async function handleImageChange(event: Event) {
  const files = (event.target as HTMLInputElement).files
  const file = files?.[0]
  if (!file) return
  await attachImageFile(file)
  if (event.target) {
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function handlePaste(event: ClipboardEvent) {
  const items = event.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (!file) continue
      event.preventDefault()
      await attachImageFile(file)
      break
    }
  }
}

async function attachImageFile(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只支持图片上传')
    return
  }

  try {
    const base64 = await readFileAsDataUrl(file)
    attachedImage.value = {
      fileName: file.name,
      mimeType: file.type,
      base64,
    }
    if (useStream.value) {
      useStream.value = false
      ElMessage.info('图片上传后自动切换为非流式模式')
    }
  } catch (error) {
    handleViewError('ChatView', error, '读取图片失败')
  }
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      resolve(reader.result as string)
    }
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

async function sendMessage() {
  const question = inputText.value.trim()
  const hasAttachment = !!attachedImage.value
  if ((!question && !hasAttachment) || isLoading.value) return

  inputText.value = ''
  isLoading.value = true

  const attachment = attachedImage.value
  chatStore.addMessage({
    role: 'user',
    content: question || (attachment ? '（图片提问）' : ''),
    imageUrl: attachment?.base64,
    imageName: attachment?.fileName,
  })

  const useStreamNow = useStream.value && !attachment

  try {
    if (useStreamNow) {
      await sendStreamMessage(question)
    } else {
      await sendSyncMessage(question, attachment ?? undefined)
    }
  } finally {
    attachedImage.value = null
  }

  isLoading.value = false
}

async function sendSyncMessage(question: string, image?: ChatAttachment) {
  // 添加 loading 占位消息
  chatStore.addMessage({
    role: 'assistant',
    content: '',
    loading: true,
  })
  const msgIndex = chatStore.messages.length - 1

  try {
    const res = await chatApi.chat({
      message: question,
      knowledgeBaseId: selectedKbId.value,
      topK: topK.value,
      image: image
        ? {
            fileName: image.fileName,
            mimeType: image.mimeType,
            base64: image.base64,
          }
        : undefined,
    })
    const data = res.data
    // 通过数组索引修改以触发 Vue 响应式更新
    chatStore.messages[msgIndex].content = data.answer
    chatStore.messages[msgIndex].sources = data.sources
    chatStore.messages[msgIndex].chunks = data.chunks
    chatStore.messages[msgIndex].loading = false
  } catch (error) {
    handleViewError('ChatView', error, '问答请求失败', false)
    chatStore.messages[msgIndex].content = '问答请求失败，请检查后端服务是否正常运行。'
    chatStore.messages[msgIndex].loading = false
  }
}

async function sendStreamMessage(question: string) {
  chatStore.addMessage({
    role: 'assistant',
    content: '',
    loading: true,
  })
  const msgIndex = chatStore.messages.length - 1

  const url = chatApi.buildStreamUrl(question, selectedKbId.value, topK.value)
  const eventSource = new EventSource(url)
  let fullContent = ''
  let receivedAny = false
  let isThinking = false

  // 过滤 <think>...</think> 标签内容（MiniMax 模型思考过程）
  function stripThinkTags(text: string): string {
    // 移除已完成的 <think>...</think> 块
    let result = text.replace(/<think>[\s\S]*?<\/think>/g, '')
    // 移除未关闭的 <think>... （正在思考中）
    const openIdx = result.indexOf('<think>')
    if (openIdx >= 0) {
      result = result.substring(0, openIdx)
    }
    return result.trim()
  }

  // 检测是否在思考阶段
  function isInThinkPhase(text: string): boolean {
    const hasOpen = text.includes('<think>')
    const hasClose = text.includes('</think>')
    return hasOpen && !hasClose
  }

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      eventSource.close()
      chatStore.messages[msgIndex].content = stripThinkTags(fullContent) || '（模型未生成有效回复，请重试）'
      chatStore.messages[msgIndex].loading = false
      isLoading.value = false
      return
    }
    // 心跳事件忽略
    if (event.data === ':heartbeat' || event.data === '') return

    receivedAny = true
    fullContent += event.data
    const displayContent = stripThinkTags(fullContent)

    if (displayContent) {
      // 有可显示内容
      chatStore.messages[msgIndex].content = displayContent
      chatStore.messages[msgIndex].loading = false
      isThinking = false
    } else if (isInThinkPhase(fullContent)) {
      // 模型正在思考，显示提示
      if (!isThinking) {
        isThinking = true
        chatStore.messages[msgIndex].content = '🤔 AI 正在思考中...'
        chatStore.messages[msgIndex].loading = false
      }
    }
  }

  eventSource.addEventListener('done', () => {
    eventSource.close()
    chatStore.messages[msgIndex].content = stripThinkTags(fullContent) || '（模型未生成有效回复，请重试）'
    chatStore.messages[msgIndex].loading = false
    isLoading.value = false
  })

  eventSource.onerror = () => {
    eventSource.close()
    const displayContent = stripThinkTags(fullContent)
    if (displayContent) {
      // 已有内容，可能只是连接断开，保留现有内容
      chatStore.messages[msgIndex].content = displayContent
    } else if (receivedAny) {
      // 收到了数据但没有可显示内容（还在思考中就断了）
      chatStore.messages[msgIndex].content = '⏱️ 模型思考时间过长，连接已断开。请重试或关闭流式输出使用同步模式。'
    } else {
      chatStore.messages[msgIndex].content = '流式请求失败，请检查后端服务是否正常运行。'
    }
    chatStore.messages[msgIndex].loading = false
    isLoading.value = false
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 108px);
}

.chat-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.kb-radio-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb-radio-item {
  width: 100%;
  padding: 6px 8px;
  border-radius: 6px;
  margin-right: 0 !important;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #c0c4cc;
}

.empty-chat p {
  font-size: 14px;
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-area .el-textarea {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
  height: 40px;
}

.attachment-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.attachment-btn {
  padding: 0 12px;
}

.attachment-label {
  font-size: 12px;
  color: #909399;
}

.attachment-preview {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fafc;
}

.attachment-preview img {
  width: 100px;
  border-radius: 8px;
  border: 1px solid #dfe4ea;
}

.attachment-status {
  font-size: 12px;
  color: #909399;
}

.preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-input {
  display: none;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
</style>
