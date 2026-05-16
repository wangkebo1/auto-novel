<template>
  <router-view v-if="route.name === 'login'" />

  <el-container v-else class="app-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="28" color="#409EFF">
          <EditPen />
        </el-icon>
        <span>AI 小说工坊</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="#16213e"
        text-color="#d5dbeb"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/novels">
          <el-icon><Notebook /></el-icon>
          <span>我的小说</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotSquare /></el-icon>
          <span>智能问答</span>
        </el-menu-item>
        <el-menu-item index="/video">
          <el-icon><VideoCamera /></el-icon>
          <span>AI 视频</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/admin">
          <el-icon><Tools /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>

      <div class="user-bar">
        <el-icon color="#d5dbeb">
          <User />
        </el-icon>
        <span class="user-name">{{ authStore.username || userFallback }}</span>
        <el-button text size="small" class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">AI 小说自动生成平台</span>
        <span class="header-desc">写大纲、生成章节、做问答和视频都放在一个工作台里。</span>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const activeMenu = computed(() => route.path)
const userFallback = '未登录用户'

function handleLogout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html,
body,
#app {
  height: 100%;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f8f9fb;
}

.app-container {
  min-height: 100vh;
}

.aside {
  background: #16213e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid #1e3054;
}

.el-menu {
  border-right: none !important;
  flex: 1;
}

.user-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #1e3054;
  color: #d5dbeb;
  font-size: 13px;
}

.user-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  color: #d5dbeb !important;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 60px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-desc {
  font-size: 13px;
  color: #909399;
}

.main {
  padding: 24px;
  overflow-y: auto;
  background: #f8f9fb;
}

@media (max-width: 960px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    padding: 12px 16px;
    height: auto !important;
  }

  .main {
    padding: 16px;
  }
}
</style>
