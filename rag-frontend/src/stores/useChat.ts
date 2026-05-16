import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KnowledgeBase, SourceChunk } from '@/api'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: string[]
  chunks?: SourceChunk[]
  imageUrl?: string
  imageName?: string
  timestamp: Date
  loading?: boolean
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const selectedKbId = ref<number | null>(null)
  const selectedKb = ref<KnowledgeBase | null>(null)
  const isStreaming = ref(false)

  function addMessage(msg: Omit<Message, 'id' | 'timestamp'> & Partial<Pick<Message, 'id' | 'timestamp'>>): Message {
    const newMsg: Message = {
      id: msg.id ?? Date.now().toString(),
      timestamp: msg.timestamp ?? new Date(),
      ...msg,
    }
    messages.value.push(newMsg)
    return newMsg
  }

  function updateLastAssistantMessage(content: string) {
    const last = [...messages.value].reverse().find((m) => m.role === 'assistant')
    if (last) last.content = content
  }

  function clearMessages() {
    messages.value = []
  }

  function selectKnowledgeBase(kb: KnowledgeBase | null) {
    selectedKb.value = kb
    selectedKbId.value = kb?.id ?? null
  }

  return {
    messages,
    selectedKbId,
    selectedKb,
    isStreaming,
    addMessage,
    updateLastAssistantMessage,
    clearMessages,
    selectKnowledgeBase,
  }
})
