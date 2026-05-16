<template>
  <div class="kb-page">
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button
        type="primary"
        @click="showCreateDialog = true"
      >
        <el-icon><Plus /></el-icon> 新建知识库
      </el-button>
    </div>

    <!-- 知识库列表 -->
    <el-row :gutter="16">
      <el-col
        v-for="kb in knowledgeBases"
        :key="kb.id"
        :span="8"
      >
        <el-card
          class="kb-card"
          shadow="hover"
          @click="selectKb(kb)"
        >
          <div class="kb-card-header">
            <el-icon
              size="24"
              color="#409EFF"
            >
              <FolderOpened />
            </el-icon>
            <span class="kb-name">{{ kb.name }}</span>
            <el-tag
              size="small"
              type="success"
            >
              启用
            </el-tag>
          </div>
          <p class="kb-desc">
            {{ kb.description || '暂无描述' }}
          </p>
          <div class="kb-footer">
            <span class="kb-count">{{ docCountMap[kb.id] ?? 0 }} 个文档</span>
            <div
              class="kb-actions"
              @click.stop
            >
              <el-button
                size="small"
                type="primary"
                @click="goToUpload(kb)"
              >
                上传文档
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="deleteKb(kb.id)"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 文档管理区域（选中知识库后展示） -->
    <el-card
      v-if="activeKb"
      class="doc-panel"
      style="margin-top: 24px"
    >
      <template #header>
        <div class="doc-panel-header">
          <span>
            <el-icon><Document /></el-icon>
            {{ activeKb.name }} - 文档列表
          </span>
          <el-button
            size="small"
            @click="activeKb = null"
          >
            收起
          </el-button>
        </div>
      </template>

      <!-- 文件上传区 -->
      <el-upload
        :action="''"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        :show-file-list="false"
        drag
        multiple
        class="uploader"
      >
        <el-icon
          class="el-icon--upload"
          size="40"
        >
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF / Word / Excel / TXT / Markdown，单文件最大 100MB
          </div>
        </template>
      </el-upload>

      <!-- 上传进度 -->
      <div
        v-if="uploading"
        class="uploading-tip"
      >
        <el-icon class="is-loading">
          <Loading />
        </el-icon>
        正在处理文档（解析 + 分块 + 向量化），请稍候...
      </div>

      <!-- 文档表格 -->
      <el-table
        v-loading="docLoading"
        :data="documents"
        stripe
        style="margin-top: 16px"
      >
        <el-table-column
          prop="fileName"
          label="文件名"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="fileType"
          label="类型"
          width="80"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ row.fileType?.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="fileSize"
          label="大小"
          width="100"
        >
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="chunkCount"
          label="分块数"
          width="80"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusType(row.status)"
              size="small"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="上传时间"
          width="160"
        />
        <el-table-column
          label="操作"
          width="80"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              @click="deleteDoc(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建知识库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建知识库"
      width="480px"
    >
      <el-form
        :model="createForm"
        label-width="80px"
      >
        <el-form-item
          label="名称"
          required
        >
          <el-input
            v-model="createForm.name"
            placeholder="请输入知识库名称"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            rows="3"
            placeholder="可选：描述知识库用途"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="creating"
          @click="createKb"
        >
          确认创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { knowledgeBaseApi, documentApi, type KnowledgeBase, type DocumentRecord } from '@/api'
import { useChatStore } from '@/stores/useChat'

const router = useRouter()
const chatStore = useChatStore()

const knowledgeBases = ref<KnowledgeBase[]>([])
const docCountMap = ref<Record<number, number>>({})
const activeKb = ref<KnowledgeBase | null>(null)
const documents = ref<DocumentRecord[]>([])
const docLoading = ref(false)
const uploading = ref(false)
const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', description: '' })

onMounted(loadKnowledgeBases)

async function loadKnowledgeBases() {
  const res = await knowledgeBaseApi.list()
  knowledgeBases.value = res.data ?? []
  // 从后端返回的 documents 列表计算文档数
  const map: Record<number, number> = {}
  for (const kb of knowledgeBases.value) {
    map[kb.id] = kb.documents?.length ?? 0
  }
  docCountMap.value = map
}

async function selectKb(kb: KnowledgeBase) {
  activeKb.value = kb
  docLoading.value = true
  try {
    const res = await documentApi.list(kb.id)
    documents.value = res.data ?? []
  } finally {
    docLoading.value = false
  }
}

async function createKb() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    await knowledgeBaseApi.create(createForm.value)
    ElMessage.success('知识库创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', description: '' }
    await loadKnowledgeBases()
  } finally {
    creating.value = false
  }
}

async function deleteKb(id: number) {
  await ElMessageBox.confirm('确认删除该知识库？相关文档记录也将一并清除。', '警告', { type: 'warning' })
  await knowledgeBaseApi.delete(id)
  ElMessage.success('已删除')
  if (activeKb.value?.id === id) activeKb.value = null
  await loadKnowledgeBases()
}

function beforeUpload(file: File) {
  const maxSize = 100 * 1024 * 1024 // 100MB
  const allowed = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'md']
  const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!allowed.includes(ext)) {
    ElMessage.error(`不支持的文件格式：${ext}`)
    return false
  }
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions) {
  if (!activeKb.value) return
  uploading.value = true
  try {
    const res = await documentApi.upload(options.file as File, activeKb.value.id)
    const data = res.data
    if (data.status === 'DUPLICATE') {
      ElMessage.warning(`文档「${data.fileName}」已存在，跳过`)
    } else if (data.status === 'PROCESSED') {
      ElMessage.success(`文档「${data.fileName}」处理完成，共 ${data.chunkCount} 个文本块`)
    } else {
      ElMessage.error(`处理失败：${data.message}`)
    }
    await selectKb(activeKb.value)
    await loadKnowledgeBases()
  } finally {
    uploading.value = false
  }
}

async function deleteDoc(id: number) {
  await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
  await documentApi.delete(id)
  ElMessage.success('已删除')
  if (activeKb.value) await selectKb(activeKb.value)
  await loadKnowledgeBases()
}

function goToUpload(kb: KnowledgeBase) {
  chatStore.selectKnowledgeBase(kb)
  selectKb(kb)
}

function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function statusType(status: string) {
  return { PROCESSED: 'success', PROCESSING: 'warning', FAILED: 'danger', DUPLICATE: 'info' }[status] ?? ''
}

function statusLabel(status: string) {
  return { PROCESSED: '已完成', PROCESSING: '处理中', FAILED: '失败', DUPLICATE: '已存在' }[status] ?? status
}
</script>

<style scoped>
.kb-page { max-width: 1200px; margin: 0 auto; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 { font-size: 20px; font-weight: 600; }

.kb-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.15s;
}
.kb-card:hover { transform: translateY(-3px); }

.kb-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.kb-name { font-size: 15px; font-weight: 600; flex: 1; }

.kb-desc { font-size: 13px; color: #909399; margin-bottom: 12px; min-height: 20px; }

.kb-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-count { font-size: 12px; color: #606266; }

.kb-actions { display: flex; gap: 8px; }

.doc-panel-header { display: flex; justify-content: space-between; align-items: center; }

.uploader { margin-bottom: 4px; }

.uploading-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409EFF;
  font-size: 14px;
  padding: 8px 0;
}
</style>
