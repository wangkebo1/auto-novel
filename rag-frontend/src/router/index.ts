import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { title: '首页' },
    },
    {
      path: '/knowledge-base',
      name: 'knowledge-base',
      component: () => import('@/views/KnowledgeBaseView.vue'),
      meta: { title: '知识库' },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { title: '智能问答' },
    },
    {
      path: '/novels',
      name: 'novels',
      component: () => import('@/views/NovelListView.vue'),
      meta: { title: '我的小说' },
    },
    {
      path: '/novels/:id',
      name: 'novel-detail',
      component: () => import('@/views/NovelDetailView.vue'),
      meta: { title: '小说详情' },
    },
    {
      path: '/novels/:novelId/chapters/:chapterId',
      name: 'chapter-editor',
      component: () => import('@/views/ChapterEditorView.vue'),
      meta: { title: '章节编辑' },
    },
    {
      path: '/novels/:novelId/analytics',
      name: 'novel-analytics',
      component: () => import('@/views/NovelAnalyticsView.vue'),
      meta: { title: '数据分析' },
    },
    {
      path: '/video',
      name: 'video-generation',
      component: () => import('@/views/VideoGenerationView.vue'),
      meta: { title: 'AI 视频' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { title: '个人中心' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { title: '管理后台' },
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('rag_token')
  if (!to.meta.public && !token) {
    return { name: 'login' }
  }
})

router.afterEach((to) => {
  const title = String(to.meta.title ?? '')
  document.title = `${title} - AI 小说工坊`
})

export default router
