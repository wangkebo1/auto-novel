import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const TOKEN_KEY = 'rag_token'
const SILENT_ERROR_HEADER = 'X-Silent-Error'

const http = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== undefined && body.code !== 200) {
      const silent = res.config?.headers?.[SILENT_ERROR_HEADER]
      if (!silent) {
        ElMessage.error(body.message || '操作失败')
      }
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('rag_username')
      router.replace('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(err)
    }
    const msg = err.response?.data?.message ?? err.message ?? '请求失败'
    const silent = err.config?.headers?.[SILENT_ERROR_HEADER]
    if (!silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export interface KnowledgeBase {
  id: number
  name: string
  description: string
  enabled: boolean
  createdAt: string
  documents?: DocumentRecord[]
}

export interface DocumentRecord {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  chunkCount: number
  status: 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'DUPLICATE'
  createdAt: string
}

export interface UploadResponse {
  documentId: number | null
  fileName: string
  chunkCount: number
  status: string
  message: string
}

export interface SourceChunk {
  content: string
  fileName: string
  score: number | null
}

export interface ChatResponse {
  answer: string
  sources: string[]
  chunks: SourceChunk[]
  timestamp: string
}

export interface ChatImagePayload {
  fileName: string
  mimeType: string
  base64: string
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export const knowledgeBaseApi = {
  list: (): Promise<Result<KnowledgeBase[]>> =>
    http.get('/knowledge-bases'),

  create: (data: { name: string; description?: string }): Promise<Result<KnowledgeBase>> =>
    http.post('/knowledge-bases', data),

  delete: (id: number): Promise<Result<void>> =>
    http.delete(`/knowledge-bases/${id}`),
}

export const documentApi = {
  list: (knowledgeBaseId: number): Promise<Result<DocumentRecord[]>> =>
    http.get('/documents', { params: { knowledgeBaseId } }),

  upload: (file: File, knowledgeBaseId: number): Promise<Result<UploadResponse>> => {
    const form = new FormData()
    form.append('file', file)
    form.append('knowledgeBaseId', String(knowledgeBaseId))
    return http.post('/documents/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  delete: (id: number): Promise<Result<void>> =>
    http.delete(`/documents/${id}`),
}

export const chatApi = {
  chat: (data: {
    message: string
    knowledgeBaseId?: number | null
    topK?: number
    image?: ChatImagePayload
  }): Promise<Result<ChatResponse>> =>
    http.post('/chat', data),

  buildStreamUrl: (message: string, knowledgeBaseId?: number | null, topK = 5): string => {
    const params = new URLSearchParams({ message, topK: String(topK) })
    if (knowledgeBaseId) params.append('knowledgeBaseId', String(knowledgeBaseId))
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) params.append('token', token)
    return `/api/chat/stream?${params.toString()}`
  },
}

export interface AuthResponse {
  token: string
  username: string
  roles: string
}

export interface ProfileResponse {
  id: number
  username: string
  email: string
  nickname: string
  points: number
  roles: string
}

export interface PaymentPackage {
  code: string
  name: string
  points: number
  amountCents: number
}

export interface PaymentOrder {
  id: number
  orderNo: string
  packageName: string
  points: number
  amountCents: number
  status: string
  paymentChannel: string | null
  refundable: boolean
  paidAt: string | null
  createdAt: string
}

export interface RefundOrder {
  id: number
  paymentOrderId: number
  refundNo: string
  reason: string
  status: string
  reviewerNote: string | null
  refundedAt: string | null
  createdAt: string
}

export interface PointTransaction {
  id: number
  type: string
  changeAmount: number
  balanceAfter: number
  sourceType: string
  sourceId: number | null
  description: string
  createdAt: string
}

export const authApi = {
  login: (data: { username: string; password: string }): Promise<Result<AuthResponse>> =>
    http.post('/auth/login', data),

  register: (data: { username: string; password: string }): Promise<Result<AuthResponse>> =>
    http.post('/auth/register', data),
}

export const profileApi = {
  getProfile: (): Promise<Result<ProfileResponse>> =>
    http.get('/profile'),

  listPackages: (): Promise<Result<PaymentPackage[]>> =>
    http.get('/profile/packages'),

  createOrder: (data: { packageCode: string }): Promise<Result<PaymentOrder>> =>
    http.post('/profile/orders', data),

  payOrder: (orderId: number): Promise<Result<PaymentOrder>> =>
    http.post('/profile/orders/' + orderId + '/pay'),

  listOrders: (): Promise<Result<PaymentOrder[]>> =>
    http.get('/profile/orders'),

  requestRefund: (orderId: number, data: { reason: string }): Promise<Result<RefundOrder>> =>
    http.post('/profile/orders/' + orderId + '/refund', data),

  listRefunds: (): Promise<Result<RefundOrder[]>> =>
    http.get('/profile/refunds'),

  listPointTransactions: (): Promise<Result<PointTransaction[]>> =>
    http.get('/profile/points/transactions'),
}

export interface GenerationStatus {
  status: string
  currentChapter?: number
  totalChapters?: number
  completedChapters?: number
  currentChapterTitle?: string
  message?: string
}

export interface Novel {
  id: number
  title: string
  genre: string
  style: string
  description: string
  worldSetting: string
  status: string
  totalWords: number
  chapterCount: number
  coverUrl?: string
  chapters?: ChapterBrief[]
  characters?: CharacterInfo[]
  createdAt: string
  updatedAt: string
}

export interface ChapterBrief {
  id: number
  chapterNumber: number
  title: string
  outline: string
  wordCount: number
  status: string
}

export interface CharacterInfo {
  id: number
  name: string
  roleType: string
  personality: string
  background: string
  appearance: string
  relationships: string
}

export interface ChapterDetail {
  id: number
  novelId: number
  chapterNumber: number
  title: string
  outline: string
  content: string
  summary: string
  wordCount: number
  status: string
  createdAt: string
  updatedAt: string
  notes?: string
}

export interface BranchSuggestion {
  title: string
  direction: string
  conflict: string
  hook: string
}

export interface NovelStatistics {
  totalWords: number
  totalChapters: number
  completedChapters: number
  averageWordsPerChapter: number
  longestChapterWords: number
  shortestChapterWords: number
  completionRate: number
}

export const novelApi = {
  list: (): Promise<Result<Novel[]>> =>
    http.get('/novels'),

  create: (data: { title: string; genre: string; style?: string; description?: string; worldSetting?: string }): Promise<Result<Novel>> =>
    http.post('/novels', data),

  get: (id: number): Promise<Result<Novel>> =>
    http.get(`/novels/${id}`),

  update: (id: number, data: { title: string; genre: string; style?: string; description?: string; worldSetting?: string }): Promise<Result<Novel>> =>
    http.put(`/novels/${id}`, data),

  delete: (id: number): Promise<Result<void>> =>
    http.delete(`/novels/${id}`),

  addCharacter: (novelId: number, data: { name: string; roleType: string; personality?: string; background?: string; appearance?: string; relationships?: string }): Promise<Result<CharacterInfo>> =>
    http.post(`/novels/${novelId}/characters`, data),

  updateCharacter: (novelId: number, characterId: number, data: { name: string; roleType: string; personality?: string; background?: string; appearance?: string; relationships?: string }): Promise<Result<CharacterInfo>> =>
    http.put(`/novels/${novelId}/characters/${characterId}`, data),

  updateCharacterWithReplace: (novelId: number, characterId: number, oldName: string, data: { name: string; roleType: string; personality?: string; background?: string; appearance?: string; relationships?: string }): Promise<Result<CharacterInfo>> =>
    http.put(`/novels/${novelId}/characters/${characterId}/replace?oldName=${encodeURIComponent(oldName)}`, data),

  deleteCharacter: (novelId: number, characterId: number): Promise<Result<void>> =>
    http.delete(`/novels/${novelId}/characters/${characterId}`),

  extractCharacters: (novelId: number): Promise<Result<Array<{ name: string; roleType: string; personality: string; background: string }>>> =>
    http.post(`/novels/${novelId}/extract-characters`),

  generateOutline: (novelId: number, chapterCount?: number): Promise<Result<any>> =>
    http.post(`/novels/${novelId}/generate-outline?chapterCount=${chapterCount ?? 10}`),

  getOutlineStatus: (novelId: number): Promise<Result<any>> =>
    http.get(`/novels/${novelId}/outline-status`),

  getChapter: (novelId: number, chapterId: number): Promise<Result<ChapterDetail>> =>
    http.get(`/novels/${novelId}/chapters/${chapterId}`),

  updateChapter: (novelId: number, chapterId: number, data: { title?: string; outline?: string; content?: string }): Promise<Result<ChapterDetail>> =>
    http.put(`/novels/${novelId}/chapters/${chapterId}`, data),

  updateChapterNotes: (novelId: number, chapterId: number, notes: string): Promise<Result<ChapterDetail>> =>
    http.put(`/novels/${novelId}/chapters/${chapterId}/notes`, { notes }),

  suggestChapterBranches: (novelId: number, chapterId: number): Promise<Result<BranchSuggestion[]>> =>
    http.post(`/novels/${novelId}/chapters/${chapterId}/branch-suggestions`, null, {
      headers: { [SILENT_ERROR_HEADER]: 'true' },
    }),

  checkSensitive: (content: string): Promise<Result<string[]>> =>
    http.post('/novels/check-sensitive', { content }),

  deleteChapter: (novelId: number, chapterId: number): Promise<Result<void>> =>
    http.delete(`/novels/${novelId}/chapters/${chapterId}`),

  deleteOutlineChapters: (novelId: number): Promise<Result<void>> =>
    http.delete(`/novels/${novelId}/outline-chapters`),

  generateChapter: (novelId: number, chapterId: number, data?: { targetWords?: number; userPrompt?: string }): Promise<Result<ChapterDetail>> =>
    http.post(`/novels/${novelId}/chapters/${chapterId}/generate`, data ?? {}),

  startGeneration: (novelId: number, data?: { targetWords?: number }): Promise<Result<GenerationStatus>> =>
    http.post(`/novels/${novelId}/start-generation`, data ?? {}),

  stopGeneration: (novelId: number): Promise<Result<GenerationStatus>> =>
    http.post(`/novels/${novelId}/stop-generation`),

  getGenerationStatus: (novelId: number): Promise<Result<GenerationStatus>> =>
    http.get(`/novels/${novelId}/generation-status`),

  getStatistics: (novelId: number): Promise<Result<NovelStatistics>> =>
    http.get(`/novels/${novelId}/statistics`),

  getWordFrequency: (novelId: number): Promise<Result<Array<{ word: string; count: number }>>> =>
    http.get(`/novels/${novelId}/word-frequency`),

  exportNovel: (novelId: number, format: 'txt' | 'md' | 'epub' = 'txt'): void => {
    const token = localStorage.getItem(TOKEN_KEY)
    const url = `/api/novels/${novelId}/export?format=${format}${token ? '&token=' + token : ''}`
    window.open(url, '_blank')
  },

  rewrite: (data: { content: string; style: string }): Promise<Result<string>> =>
    http.post('/novels/rewrite', data),
}

export interface VideoTask {
  id: number
  taskId: string
  model: string
  prompt: string
  status: 'queued' | 'initializing' | 'in_progress' | 'downloading' | 'uploading' | 'completed' | 'failed'
  duration: number
  size: string
  mode: string
  videoUrl: string | null
  coverUrl: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface VideoTaskRequest {
  prompt: string
  duration?: number
  size?: string
  mode?: string
}

export const videoApi = {
  create: (data: VideoTaskRequest): Promise<Result<VideoTask>> =>
    http.post('/videos', data),

  list: (): Promise<Result<VideoTask[]>> =>
    http.get('/videos'),

  get: (id: number): Promise<Result<VideoTask>> =>
    http.get(`/videos/${id}`),

  refresh: (id: number): Promise<Result<VideoTask>> =>
    http.post(`/videos/${id}/refresh`),

  delete: (id: number): Promise<Result<void>> =>
    http.delete(`/videos/${id}`),
}

export interface AdminUserResponse {
  id: number
  username: string
  email: string
  nickname: string
  enabled: boolean
  roles: string
  novelCount: number
  totalWords: number
  createdAt: string
}

export interface AdminStatistics {
  totalUsers: number
  totalNovels: number
  totalWords: number
  totalChapters: number
}

export interface SensitiveWord {
  id: number
  word: string
  category: string
  createdAt: string
}

export interface RefundReviewRequest {
  approved: boolean
  reviewerNote?: string
}

export const adminApi = {
  getStatistics: (): Promise<Result<AdminStatistics>> =>
    http.get('/admin/statistics'),

  listUsers: (): Promise<Result<AdminUserResponse[]>> =>
    http.get('/admin/users'),

  toggleUserStatus: (userId: number): Promise<Result<void>> =>
    http.put(`/admin/users/${userId}/toggle`),

  listSensitiveWords: (): Promise<Result<SensitiveWord[]>> =>
    http.get('/admin/sensitive-words'),

  addSensitiveWord: (data: { word: string; category?: string }): Promise<Result<SensitiveWord>> =>
    http.post('/admin/sensitive-words', data),

  deleteSensitiveWord: (id: number): Promise<Result<void>> =>
    http.delete(`/admin/sensitive-words/${id}`),

  listRefunds: (): Promise<Result<RefundOrder[]>> =>
    http.get('/admin/refunds'),

  reviewRefund: (refundId: number, data: RefundReviewRequest): Promise<Result<RefundOrder>> =>
    http.post('/admin/refunds/' + refundId + '/review', data),

  listAllNovels: (): Promise<Result<Novel[]>> =>
    http.get('/admin/novels'),

  deleteNovel: (novelId: number): Promise<Result<void>> =>
    http.delete(`/admin/novels/${novelId}`),
}
