<template>
  <div class="admin-panel">
    <div class="page-head">
      <div>
        <h1>&#31649;&#29702;&#21518;&#21488;</h1>
        <p>&#31649;&#29702;&#29992;&#25143;&#12289;&#25935;&#24863;&#35789;&#12289;&#23567;&#35828;&#21644;&#36864;&#27454;&#30003;&#35831;&#12290;</p>
      </div>
      <el-button :loading="loading" @click="loadAll">&#21047;&#26032;</el-button>
    </div>

    <div v-if="statistics" class="stats-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-label">&#24635;&#29992;&#25143;&#25968;</div>
        <div class="stat-value">{{ statistics.totalUsers }}</div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-label">&#24635;&#23567;&#35828;&#25968;</div>
        <div class="stat-value">{{ statistics.totalNovels }}</div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-label">&#24635;&#23383;&#25968;</div>
        <div class="stat-value">{{ formatWords(statistics.totalWords) }}</div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-label">&#24635;&#31456;&#33410;&#25968;</div>
        <div class="stat-value">{{ statistics.totalChapters }}</div>
      </el-card>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="decodeText('&#29992;&#25143;&#31649;&#29702;')" name="users">
        <el-table :data="users" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" :label="decodeText('&#29992;&#25143;&#21517;')" min-width="140" />
          <el-table-column prop="email" label="Email" min-width="180" />
          <el-table-column prop="roles" :label="decodeText('&#35282;&#33394;')" min-width="150" />
          <el-table-column prop="novelCount" :label="decodeText('&#23567;&#35828;&#25968;')" width="100" />
          <el-table-column :label="decodeText('&#24635;&#23383;&#25968;')" width="120">
            <template #default="scope">{{ formatWords(scope.row.totalWords) }}</template>
          </el-table-column>
          <el-table-column :label="decodeText('&#29366;&#24577;')" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.enabled ? 'success' : 'danger'">
                {{ scope.row.enabled ? decodeText('&#27491;&#24120;') : decodeText('&#23553;&#31105;') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="decodeText('&#27880;&#20876;&#26102;&#38388;')" min-width="180" />
          <el-table-column :label="decodeText('&#25805;&#20316;')" width="120">
            <template #default="scope">
              <el-button :type="scope.row.enabled ? 'danger' : 'success'" size="small" @click="toggleUser(scope.row)">
                {{ scope.row.enabled ? decodeText('&#23553;&#31105;') : decodeText('&#35299;&#23553;') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="decodeText('&#36864;&#27454;&#31649;&#29702;')" name="refunds">
        <el-table :data="refunds" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="refundNo" :label="decodeText('&#36864;&#27454;&#21333;&#21495;')" min-width="180" />
          <el-table-column prop="paymentOrderId" :label="decodeText('&#20851;&#32852;&#35746;&#21333;')" width="120" />
          <el-table-column prop="reason" :label="decodeText('&#36864;&#27454;&#21407;&#22240;')" min-width="180" />
          <el-table-column :label="decodeText('&#29366;&#24577;')" width="120">
            <template #default="scope">
              <el-tag :type="refundTagType(scope.row.status)">{{ refundStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewerNote" :label="decodeText('&#23457;&#26680;&#22791;&#27880;')" min-width="180" />
          <el-table-column prop="createdAt" :label="decodeText('&#30003;&#35831;&#26102;&#38388;')" min-width="180">
            <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column :label="decodeText('&#25805;&#20316;')" width="160">
            <template #default="scope">
              <el-button v-if="scope.row.status === 'PENDING'" type="primary" link @click="openRefundReview(scope.row, true)">
                &#36890;&#36807;
              </el-button>
              <el-button v-if="scope.row.status === 'PENDING'" type="danger" link @click="openRefundReview(scope.row, false)">
                &#25302;&#32477;
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="decodeText('&#25935;&#24863;&#35789;&#31649;&#29702;')" name="words">
        <div class="toolbar">
          <el-button type="primary" @click="showAddWordDialog = true">&#28155;&#21152;&#25935;&#24863;&#35789;</el-button>
        </div>
        <el-table :data="sensitiveWords" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="word" :label="decodeText('&#25935;&#24863;&#35789;')" min-width="180" />
          <el-table-column prop="category" :label="decodeText('&#20998;&#31867;')" width="140" />
          <el-table-column prop="createdAt" :label="decodeText('&#28155;&#21152;&#26102;&#38388;')" min-width="180" />
          <el-table-column :label="decodeText('&#25805;&#20316;')" width="120">
            <template #default="scope">
              <el-button type="danger" size="small" @click="deleteWord(scope.row)">&#21024;&#38500;</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="decodeText('&#23567;&#35828;&#31649;&#29702;')" name="novels">
        <el-table :data="novels" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" :label="decodeText('&#26631;&#39064;')" min-width="180" />
          <el-table-column prop="genre" :label="decodeText('&#31867;&#22411;')" width="120" />
          <el-table-column :label="decodeText('&#23383;&#25968;')" width="120">
            <template #default="scope">{{ formatWords(scope.row.totalWords) }}</template>
          </el-table-column>
          <el-table-column prop="chapterCount" :label="decodeText('&#31456;&#33410;&#25968;')" width="100" />
          <el-table-column prop="status" :label="decodeText('&#29366;&#24577;')" width="120" />
          <el-table-column :label="decodeText('&#25805;&#20316;')" width="120">
            <template #default="scope">
              <el-button type="danger" size="small" @click="deleteNovel(scope.row)">&#21024;&#38500;</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showAddWordDialog" title="&#28155;&#21152;&#25935;&#24863;&#35789;" width="420px">
      <el-form :model="newWord" label-position="top">
        <el-form-item :label="decodeText('&#25935;&#24863;&#35789;')">
          <el-input v-model="newWord.word" :placeholder="decodeText('&#35831;&#36755;&#20837;&#25935;&#24863;&#35789;')" />
        </el-form-item>
        <el-form-item :label="decodeText('&#20998;&#31867;')">
          <el-input v-model="newWord.category" :placeholder="decodeText('&#20363;&#22914;&#65306;&#25919;&#27835;&#12289;&#26292;&#21147;&#12289;&#33394;&#24773;')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddWordDialog = false">&#21462;&#28040;</el-button>
        <el-button type="primary" @click="addWord">&#30830;&#23450;</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundReviewVisible" :title="refundApprove ? '&#36890;&#36807;&#36864;&#27454;' : '&#25302;&#32477;&#36864;&#27454;'" width="480px">
      <el-form label-position="top">
        <el-form-item :label="decodeText('&#23457;&#26680;&#22791;&#27880;')">
          <el-input
            v-model="refundReviewNote"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            :placeholder="decodeText('&#21487;&#20197;&#36873;&#22635;&#23457;&#26680;&#35828;&#26126;')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundReviewVisible = false">&#21462;&#28040;</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitRefundReview">&#25552;&#20132;</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import type { AdminStatistics, AdminUserResponse, Novel, RefundOrder, SensitiveWord } from '@/api'

const activeTab = ref('users')
const loading = ref(false)
const statistics = ref<AdminStatistics | null>(null)
const users = ref<AdminUserResponse[]>([])
const refunds = ref<RefundOrder[]>([])
const sensitiveWords = ref<SensitiveWord[]>([])
const novels = ref<Novel[]>([])
const showAddWordDialog = ref(false)
const refundReviewVisible = ref(false)
const reviewLoading = ref(false)
const refundApprove = ref(true)
const refundReviewId = ref<number | null>(null)
const refundReviewNote = ref('')
const newWord = ref({ word: '', category: '' })

const textDecoder = document.createElement('textarea')
function decodeText(value?: string | null) {
  if (!value) {
    return '-'
  }
  textDecoder.innerHTML = value
  return textDecoder.value
}

function formatWords(words: number) {
  if (!words) return '0'
  if (words >= 10000) return (words / 10000).toFixed(1) + '万'
  return words.toLocaleString()
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function refundStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
  }
  return map[status] || status
}

function refundTagType(status: string) {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [statisticsRes, usersRes, refundsRes, wordsRes, novelsRes] = await Promise.all([
      adminApi.getStatistics(),
      adminApi.listUsers(),
      adminApi.listRefunds(),
      adminApi.listSensitiveWords(),
      adminApi.listAllNovels(),
    ])
    statistics.value = statisticsRes.data
    users.value = usersRes.data
    refunds.value = refundsRes.data
    sensitiveWords.value = wordsRes.data
    novels.value = novelsRes.data
  } finally {
    loading.value = false
  }
}

async function toggleUser(user: AdminUserResponse) {
  const action = user.enabled ? '封禁' : '解封'
  await ElMessageBox.confirm(action + '用户 ' + user.username + '？', '提示')
  await adminApi.toggleUserStatus(user.id)
  ElMessage.success('操作成功')
  await loadAll()
}

async function addWord() {
  if (!newWord.value.word.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  await adminApi.addSensitiveWord({
    word: newWord.value.word.trim(),
    category: newWord.value.category.trim(),
  })
  ElMessage.success('添加成功')
  showAddWordDialog.value = false
  newWord.value = { word: '', category: '' }
  await loadAll()
}

async function deleteWord(word: SensitiveWord) {
  await ElMessageBox.confirm('确认删除敏感词 ' + word.word + '？', '提示', { type: 'warning' })
  await adminApi.deleteSensitiveWord(word.id)
  ElMessage.success('删除成功')
  await loadAll()
}

async function deleteNovel(novel: Novel) {
  await ElMessageBox.confirm('确认删除小说?' + novel.title + '?？', '提示', { type: 'warning' })
  await adminApi.deleteNovel(novel.id)
  ElMessage.success('删除成功')
  await loadAll()
}

function openRefundReview(refund: RefundOrder, approved: boolean) {
  refundReviewId.value = refund.id
  refundApprove.value = approved
  refundReviewNote.value = ''
  refundReviewVisible.value = true
}

async function submitRefundReview() {
  if (!refundReviewId.value) {
    return
  }
  reviewLoading.value = true
  try {
    await adminApi.reviewRefund(refundReviewId.value, {
      approved: refundApprove.value,
      reviewerNote: refundReviewNote.value.trim(),
    })
    ElMessage.success('审核完成')
    refundReviewVisible.value = false
    refundReviewId.value = null
    refundReviewNote.value = ''
    await loadAll()
  } finally {
    reviewLoading.value = false
  }
}

onMounted(() => {
  void loadAll()
})
</script>

<style scoped>
.admin-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 28px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  text-align: center;
  border-radius: 18px;
}

.stat-label {
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 14px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #0f766e;
}

.toolbar {
  margin-bottom: 16px;
}

@media (max-width: 960px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

