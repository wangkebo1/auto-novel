<template>
  <div v-if="novel" class="novel-detail">
    <section class="hero-card">
      <div class="hero">
        <div>
          <el-button text @click="router.push('/novels')"><el-icon><ArrowLeft /></el-icon>返回列表</el-button>
          <div class="title-row">
            <h1>{{ novel.title }}</h1>
            <el-tag :type="statusType(novel.status)" size="small">{{ statusLabel(novel.status) }}</el-tag>
          </div>
          <p class="meta">{{ novel.genre }}<span v-if="novel.style"> · {{ novel.style }}</span> · {{ formatWords(novel.totalWords) }}</p>
        </div>
        <div class="hero-actions">
          <el-tag type="info" effect="plain">当前点数 {{ currentPoints }}</el-tag>
          <el-button @click="loadAll"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button @click="openNovelDialog"><el-icon><EditPen /></el-icon>编辑小说</el-button>
          <el-button @click="router.push(`/novels/${novel.id}/analytics`)"><el-icon><DataAnalysis /></el-icon>数据分析</el-button>
          <el-dropdown @command="handleExport">
            <el-button><el-icon><Download /></el-icon>导出作品<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="txt">TXT</el-dropdown-item>
                <el-dropdown-item command="md">Markdown</el-dropdown-item>
                <el-dropdown-item command="epub">EPUB</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </section>

    <section v-if="statistics" class="stats-grid">
      <div class="stat-card"><span>总字数</span><strong>{{ formatWords(statistics.totalWords) }}</strong></div>
      <div class="stat-card"><span>已完成章节</span><strong>{{ statistics.completedChapters }}/{{ statistics.totalChapters }}</strong></div>
      <div class="stat-card"><span>平均每章</span><strong>{{ formatWords(statistics.averageWordsPerChapter) }}</strong></div>
      <div class="stat-card"><span>完成率</span><strong>{{ statistics.completionRate.toFixed(1) }}%</strong></div>
    </section>

    <section class="layout">
      <div class="main-col">
        <el-card shadow="hover">
          <template #header><div class="head"><div><h3>创作操作</h3><p>恢复了大纲、连续创作、角色检测和编辑入口。</p></div></div></template>
          <div class="toolbar">
            <div class="outline-box"><span>大纲章节数</span><el-input-number v-model="outlineCount" :min="3" :max="50" /></div>
            <el-button type="primary" :loading="generatingOutline" :disabled="!canGenerateOutline" @click="handleGenerateOutline"><el-icon><MagicStick /></el-icon>生成大纲</el-button>
            <el-button type="success" :disabled="!canStartGeneration" @click="handleStartGeneration"><el-icon><VideoPlay /></el-icon>开始连续创作</el-button>
            <el-button v-if="isGenerating" type="danger" @click="handleStopGeneration"><el-icon><VideoPause /></el-icon>停止创作</el-button>
            <el-button :loading="extractingCharacters" @click="handleExtractCharacters"><el-icon><MagicStick /></el-icon>角色检测</el-button>
            <el-button @click="openCharacterDialog()"><el-icon><Plus /></el-icon>新增角色</el-button>
            <el-button :disabled="outlineOnlyCount === 0" @click="handleDeleteOutlineChapters"><el-icon><Delete /></el-icon>清空大纲章</el-button>
          </div>
          <div class="tips">
            <div>{{ outlineCostText }}</div>
            <div>{{ continuousCostText }}</div>
            <div>{{ adminTipText }}</div>
          </div>
          <div v-if="isOutlineRunning" class="outline-progress">
            <div class="outline-progress-head">
              <span>大纲生成中</span>
              <span v-if="outlineStatus?.totalChapters">
                {{ outlineStatus?.completedChapters ?? 0 }}/{{ outlineStatus?.totalChapters ?? 0 }}
              </span>
            </div>
            <div class="indeterminate-bar" />
            <div class="outline-progress-msg">{{ outlineStatus?.message || '正在生成大纲...' }}</div>
          </div>
          <div v-if="isGenerating" class="progress-box">
            <div class="progress-head"><span>连续创作进度</span><span>{{ genStatus?.completedChapters ?? 0 }}/{{ genStatus?.totalChapters ?? 0 }}</span></div>
            <el-progress :percentage="genPercent" />
            <p>{{ genStatus?.message }}</p>
          </div>
        </el-card>

        <el-card shadow="hover">
          <template #header><div class="head"><div><h3>章节列表</h3><p>支持编辑、单章生成和删除。</p></div><span>共 {{ chapters.length }} 章</span></div></template>
          <div v-if="chapters.length" class="chapter-list">
            <div v-for="chapter in pagedChapters" :key="chapter.id" class="chapter-item" @click="goToChapter(chapter.id)">
              <div class="chapter-main">
                <div class="chapter-no">第 {{ chapter.chapterNumber }} 章</div>
                <div class="chapter-info">
                  <div class="chapter-title">{{ chapter.title }}</div>
                  <div class="chapter-outline">{{ chapter.outline || '暂无大纲' }}</div>
                </div>
              </div>
              <div class="chapter-side" @click.stop>
                <div class="chapter-meta">
                  <el-tag size="small" :type="chapterStatusType(chapter.status)">{{ chapterStatusLabel(chapter.status) }}</el-tag>
                  <span>{{ chapter.wordCount || 0 }} 字</span>
                </div>
                <div class="chapter-btns">
                  <el-button size="small" @click="goToChapter(chapter.id)">编辑</el-button>
                  <el-button v-if="!isChapterDone(chapter.status)" size="small" type="primary" :loading="generatingChapterId === chapter.id" :disabled="isGenerating || isOutlineRunning" @click="handleGenerateSingleChapter(chapter.id)">AI 生成</el-button>
                  <el-button size="small" type="danger" link :loading="deletingChapterId === chapter.id" @click="handleDeleteChapter(chapter.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有章节，先生成大纲吧" />
          <el-pagination
            v-model:current-page="chapterPage"
            v-model:page-size="chapterPageSize"
            class="pagination"
            :page-sizes="[10, 20, 50]"
            :total="chapters.length"
            layout="total, sizes, prev, pager, next, jumper"
            small
          />
        </el-card>
      </div>

      <div class="side-col">
        <el-card shadow="hover">
          <template #header><div class="head"><div><h3>小说信息</h3><p>基础设定和简介。</p></div><el-button text @click="openNovelDialog">编辑</el-button></div></template>
          <div class="info-list">
            <div><span>类型</span><strong>{{ novel.genre }}</strong></div>
            <div><span>风格</span><strong>{{ novel.style || '未设置' }}</strong></div>
            <div class="block"><span>简介</span><p>{{ novel.description || '暂无简介' }}</p></div>
            <div class="block"><span>世界观</span><p>{{ novel.worldSetting || '暂无设定' }}</p></div>
          </div>
        </el-card>

        <el-card shadow="hover">
          <template #header><div class="head"><div><h3>角色设定</h3><p>角色检测和手动编辑都已补回。</p></div><div><el-button text :loading="extractingCharacters" @click="handleExtractCharacters">角色检测</el-button><el-button text @click="openCharacterDialog()">新增</el-button></div></div></template>
          <div v-if="characters.length" class="character-list">
            <div v-for="item in characters" :key="item.id" class="character-item">
              <div class="character-top">
                <div><strong>{{ item.name }}</strong><el-tag size="small" :type="roleTagType(item.roleType)">{{ roleLabel(item.roleType) }}</el-tag></div>
                <div><el-button text @click="openCharacterDialog(item)">编辑</el-button><el-button text type="danger" :loading="deletingCharacterId === item.id" @click="handleDeleteCharacter(item)">删除</el-button></div>
              </div>
              <p v-if="item.personality"><span>性格：</span>{{ item.personality }}</p>
              <p v-if="item.background"><span>背景：</span>{{ item.background }}</p>
              <p v-if="item.appearance"><span>外貌：</span>{{ item.appearance }}</p>
              <p v-if="item.relationships"><span>关系：</span>{{ item.relationships }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无角色，点击“角色检测”或“新增”开始补充" />
        </el-card>
      </div>
    </section>

    <el-dialog v-model="showNovelDialog" title="编辑小说信息" width="640px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="novelForm.title" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="类型"><el-select v-model="novelForm.genre" style="width:100%"><el-option v-for="g in genres" :key="g" :label="g" :value="g" /></el-select></el-form-item>
        <el-form-item label="风格"><el-select v-model="novelForm.style" clearable style="width:100%"><el-option v-for="s in styles" :key="s" :label="s" :value="s" /></el-select></el-form-item>
        <el-form-item label="简介"><el-input v-model="novelForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="世界观"><el-input v-model="novelForm.worldSetting" type="textarea" :rows="5" maxlength="1500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showNovelDialog = false">取消</el-button><el-button type="primary" :loading="savingNovel" @click="handleSaveNovel">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="showCharacterDialog" :title="characterForm.id ? '编辑角色' : '新增角色'" width="680px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="角色名"><el-input v-model="characterForm.name" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item label="类型"><el-select v-model="characterForm.roleType" style="width:100%"><el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" /></el-select></el-form-item>
        <el-form-item label="性格"><el-input v-model="characterForm.personality" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
        <el-form-item label="背景"><el-input v-model="characterForm.background" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="外貌"><el-input v-model="characterForm.appearance" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
        <el-form-item label="关系"><el-input v-model="characterForm.relationships" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCharacterDialog = false">取消</el-button><el-button type="primary" :loading="savingCharacter" @click="handleSaveCharacter">保存</el-button></template>
    </el-dialog>
  </div>
  <div v-else class="empty-wrap"><el-empty description="小说不存在或已被删除" /></div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { novelApi, profileApi } from '@/api'
import type { CharacterInfo, GenerationStatus, Novel, NovelStatistics, ProfileResponse } from '@/api'
import { handleViewError } from '@/utils/error'

const OUTLINE_COST_PER_CHAPTER = 30, CHAPTER_COST = 20
const genres = ['玄幻', '奇幻', '武侠', '仙侠', '都市', '现实', '科幻', '历史', '军事', '游戏', '体育', '悬疑', '灵异', '言情', '轻小说']
const styles = ['热血', '轻松', '搞笑', '严谨', '黑暗', '温馨', '虐心', '爽文', '慢热']
const roleOptions = [{ label: '主角', value: 'PROTAGONIST' }, { label: '反派', value: 'ANTAGONIST' }, { label: '配角', value: 'SUPPORTING' }]
const route = useRoute(), router = useRouter(), novelId = Number(route.params.id)
const novel = ref<Novel | null>(null), profile = ref<ProfileResponse | null>(null), statistics = ref<NovelStatistics | null>(null)
const genStatus = ref<GenerationStatus | null>(null), outlineStatus = ref<GenerationStatus | null>(null)
const generatingOutline = ref(false), extractingCharacters = ref(false), savingNovel = ref(false), savingCharacter = ref(false)
const generatingChapterId = ref<number | null>(null), deletingChapterId = ref<number | null>(null), deletingCharacterId = ref<number | null>(null)
const outlineCount = ref(10), chapterPage = ref(1), chapterPageSize = ref(10), showNovelDialog = ref(false), showCharacterDialog = ref(false)
const novelForm = reactive({ title: '', genre: '', style: '', description: '', worldSetting: '' })
const characterForm = reactive({ id: undefined as number | undefined, originalName: '', name: '', roleType: 'SUPPORTING', personality: '', background: '', appearance: '', relationships: '' })
let pollTimer: ReturnType<typeof setInterval> | null = null

const currentPoints = computed(() => profile.value?.points ?? 0)
const isAdmin = computed(() => (profile.value?.roles || '').includes('ROLE_ADMIN'))
const isGenerating = computed(() => genStatus.value?.status === 'RUNNING')
const isOutlineRunning = computed(() => outlineStatus.value?.status === 'RUNNING')
const chapters = computed(() => [...(novel.value?.chapters ?? [])].sort((a, b) => a.chapterNumber - b.chapterNumber))
const characters = computed(() => [...(novel.value?.characters ?? [])].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')))
const outlineOnlyCount = computed(() => chapters.value.filter(i => i.status === 'OUTLINE').length)
const pendingCount = computed(() => chapters.value.filter(i => !isChapterDone(i.status)).length)
const outlineRequiredPoints = computed(() => isAdmin.value ? 0 : outlineCount.value * OUTLINE_COST_PER_CHAPTER)
const canGenerateOutline = computed(() => !isGenerating.value && !isOutlineRunning.value && (isAdmin.value || currentPoints.value >= outlineRequiredPoints.value))
const canStartGeneration = computed(() => !isGenerating.value && !isOutlineRunning.value && pendingCount.value > 0 && (isAdmin.value || currentPoints.value >= pendingCount.value * CHAPTER_COST))
const outlineCostText = computed(() =>
  isAdmin.value
    ? '管理员生成大纲免扣点'
    : `生成大纲每章 ${OUTLINE_COST_PER_CHAPTER} 点，合计 ${outlineRequiredPoints.value} 点`
)
const continuousCostText = computed(() =>
  isAdmin.value
    ? '管理员连续创作免扣点'
    : pendingCount.value > 0
      ? `连续创作启动时一次性预扣 ${pendingCount.value * CHAPTER_COST} 点（${CHAPTER_COST} 点/章），失败/停止不退点`
      : '没有待生成的章节'
)
const adminTipText = computed(() => isAdmin.value ? '当前账号为管理员，AI 生成功能不会扣点。' : '点数不足时将无法触发 AI 生成。')
const genPercent = computed(() => (genStatus.value?.totalChapters ? Math.round(((genStatus.value.completedChapters ?? 0) / genStatus.value.totalChapters) * 100) : 0))
const totalChapterPages = computed(() => Math.max(1, Math.ceil(chapters.value.length / chapterPageSize.value)))
const pagedChapters = computed(() =>
  chapters.value.slice((chapterPage.value - 1) * chapterPageSize.value, chapterPage.value * chapterPageSize.value)
)

watch([() => chapters.value.length, chapterPageSize], () => {
  if (chapterPage.value > totalChapterPages.value) chapterPage.value = totalChapterPages.value
  if (chapterPage.value < 1) {
    chapterPage.value = 1
  }
})

function jumpToLastChapterPage() {
  chapterPage.value = totalChapterPages.value
}

watch(
  () => outlineStatus.value?.status,
  (next, prev) => {
    // When outline generation finishes (or leaves RUNNING), jump to the last page so newly appended OUTLINE chapters are visible.
    if (prev === 'RUNNING' && next !== 'RUNNING') {
      jumpToLastChapterPage()
    }
  }
)

async function loadAll() {
  try {
    const [n, p, s, g, o] = await Promise.all([novelApi.get(novelId), profileApi.getProfile(), novelApi.getStatistics(novelId), novelApi.getGenerationStatus(novelId), novelApi.getOutlineStatus(novelId)])
    novel.value = n.data; profile.value = p.data; statistics.value = s.data; genStatus.value = g.data; outlineStatus.value = o.data
    if (isGenerating.value || isOutlineRunning.value) startPolling(); else stopPolling()
  } catch (error) { handleViewError('NovelDetail', error, '小说详情加载失败') }
}

function startPolling() { if (pollTimer) return; pollTimer = setInterval(loadAll, 3000) }
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }
function openNovelDialog() { if (!novel.value) return; Object.assign(novelForm, { title: novel.value.title, genre: novel.value.genre, style: novel.value.style || '', description: novel.value.description || '', worldSetting: novel.value.worldSetting || '' }); showNovelDialog.value = true }
function openCharacterDialog(item?: CharacterInfo) { Object.assign(characterForm, item ? { id: item.id, originalName: item.name, name: item.name, roleType: item.roleType || 'SUPPORTING', personality: item.personality || '', background: item.background || '', appearance: item.appearance || '', relationships: item.relationships || '' } : { id: undefined, originalName: '', name: '', roleType: 'SUPPORTING', personality: '', background: '', appearance: '', relationships: '' }); showCharacterDialog.value = true }

async function handleSaveNovel() {
  if (!novelForm.title.trim() || !novelForm.genre) return ElMessage.warning('请先完善标题和类型')
  savingNovel.value = true
  try { await novelApi.update(novelId, { title: novelForm.title.trim(), genre: novelForm.genre, style: novelForm.style || undefined, description: novelForm.description.trim(), worldSetting: novelForm.worldSetting.trim() }); ElMessage.success('小说信息已更新'); showNovelDialog.value = false; await loadAll() } catch (error) { handleViewError('NovelDetail', error, '保存小说信息失败') } finally { savingNovel.value = false }
}

async function handleSaveCharacter() {
  if (!characterForm.name.trim()) return ElMessage.warning('请输入角色名')
  const payload = { name: characterForm.name.trim(), roleType: characterForm.roleType, personality: characterForm.personality.trim(), background: characterForm.background.trim(), appearance: characterForm.appearance.trim(), relationships: characterForm.relationships.trim() }
  savingCharacter.value = true
  try {
    if (characterForm.id) characterForm.originalName && characterForm.originalName !== payload.name ? await novelApi.updateCharacterWithReplace(novelId, characterForm.id, characterForm.originalName, payload) : await novelApi.updateCharacter(novelId, characterForm.id, payload)
    else await novelApi.addCharacter(novelId, payload)
    ElMessage.success(characterForm.id ? '角色已更新' : '角色已添加'); showCharacterDialog.value = false; await loadAll()
  } catch (error) { handleViewError('NovelDetail', error, '保存角色失败') } finally { savingCharacter.value = false }
}

async function handleDeleteCharacter(item: CharacterInfo) {
  const confirmed = await ElMessageBox.confirm(`确定删除角色“${item.name}”吗？`, '删除确认', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  deletingCharacterId.value = item.id
  try { await novelApi.deleteCharacter(novelId, item.id); ElMessage.success('角色已删除'); await loadAll() } catch (error) { handleViewError('NovelDetail', error, '删除角色失败') } finally { deletingCharacterId.value = null }
}

async function handleExtractCharacters() {
  if (!chapters.value.length) return ElMessage.info('还没有章节内容，暂时无法进行角色检测')
  extractingCharacters.value = true
  try {
    const detected = (await novelApi.extractCharacters(novelId)).data || []
    const exists = new Set((novel.value?.characters ?? []).map(i => i.name.trim().toLowerCase()))
    const fresh = detected.filter(i => i.name?.trim() && !exists.has(i.name.trim().toLowerCase()))
    if (!fresh.length) return ElMessage.info('没有检测到可新增的角色')
    for (const item of fresh) await novelApi.addCharacter(novelId, { name: item.name.trim(), roleType: item.roleType || 'SUPPORTING', personality: item.personality || '', background: item.background || '' })
    ElMessage.success(`角色检测完成，已导入 ${fresh.length} 个新角色`); await loadAll()
  } catch (error) { handleViewError('NovelDetail', error, '角色检测失败') } finally { extractingCharacters.value = false }
}

async function handleDeleteOutlineChapters() {
  const confirmed = await ElMessageBox.confirm(`确定清空 ${outlineOnlyCount.value} 个大纲章节吗？`, '清空确认', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  try { await novelApi.deleteOutlineChapters(novelId); ElMessage.success('已清空大纲章节'); await loadAll() } catch (error) { handleViewError('NovelDetail', error, '清空大纲章节失败') }
}

async function handleGenerateOutline() {
  if (!canGenerateOutline.value) return ElMessage.warning(`点数不足，生成大纲需要 ${outlineRequiredPoints.value} 点`)
  generatingOutline.value = true
  outlineStatus.value = {
    status: 'RUNNING',
    totalChapters: outlineCount.value,
    completedChapters: 0,
    message: '大纲生成排队中...'
  }
  try {
    await novelApi.generateOutline(novelId, outlineCount.value)
    ElMessage.success('大纲生成任务已启动')
    await loadAll()
    jumpToLastChapterPage()
    startPolling()
  } catch (error) {
    handleViewError('NovelDetail', error, '生成大纲失败')
  } finally {
    generatingOutline.value = false
  }
}

async function handleStartGeneration() {
  if (!pendingCount.value) return ElMessage.info('没有待生成的章节')
  if (!canStartGeneration.value) return ElMessage.warning(`点数不足，连续创作需要 ${pendingCount.value * CHAPTER_COST} 点`)
  try { await novelApi.startGeneration(novelId); ElMessage.success('连续创作已启动'); await loadAll(); startPolling() } catch (error) { handleViewError('NovelDetail', error, '启动连续创作失败') }
}

async function handleStopGeneration() {
  try { await novelApi.stopGeneration(novelId); ElMessage.success('已停止连续创作'); await loadAll(); stopPolling() } catch (error) { handleViewError('NovelDetail', error, '停止连续创作失败') }
}

async function handleGenerateSingleChapter(chapterId: number) {
  if (!isAdmin.value && currentPoints.value < CHAPTER_COST) return ElMessage.warning(`点数不足，单章生成需要 ${CHAPTER_COST} 点`)
  generatingChapterId.value = chapterId
  try { await novelApi.generateChapter(novelId, chapterId); ElMessage.success('章节已生成'); await loadAll() } catch (error) { handleViewError('NovelDetail', error, '单章生成失败') } finally { generatingChapterId.value = null }
}

async function handleDeleteChapter(chapterId: number) {
  const confirmed = await ElMessageBox.confirm('确定删除这个章节吗？删除后无法恢复。', '删除确认', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  deletingChapterId.value = chapterId
  try { await novelApi.deleteChapter(novelId, chapterId); ElMessage.success('章节已删除'); await loadAll() } catch (error) { handleViewError('NovelDetail', error, '删除章节失败') } finally { deletingChapterId.value = null }
}

function handleExport(format: 'txt' | 'md' | 'epub') { novelApi.exportNovel(novelId, format) }
function goToChapter(chapterId: number) { router.push(`/novels/${novelId}/chapters/${chapterId}`) }
function isChapterDone(status: string) { return status === 'GENERATED' || status === 'EDITED' }
function statusLabel(status: string) { return ({ DRAFT: '草稿', IN_PROGRESS: '创作中', COMPLETED: '已完结' } as Record<string, string>)[status] || status }
function statusType(status: string) { return ({ DRAFT: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' } as Record<string, '' | 'success' | 'warning' | 'info' | 'danger'>)[status] || 'info' }
function roleLabel(roleType: string) { return ({ PROTAGONIST: '主角', ANTAGONIST: '反派', SUPPORTING: '配角' } as Record<string, string>)[roleType] || roleType }
function roleTagType(roleType: string) { return ({ PROTAGONIST: 'danger', ANTAGONIST: 'warning', SUPPORTING: 'info' } as Record<string, '' | 'success' | 'warning' | 'info' | 'danger'>)[roleType] || 'info' }
function chapterStatusLabel(status: string) { return ({ OUTLINE: '大纲', GENERATING: '生成中', GENERATED: '已生成', EDITED: '已编辑' } as Record<string, string>)[status] || status }
function chapterStatusType(status: string) { return ({ OUTLINE: 'info', GENERATING: 'warning', GENERATED: 'success', EDITED: '' } as Record<string, '' | 'success' | 'warning' | 'info' | 'danger'>)[status] || 'info' }
function formatWords(value: number) { return !value ? '0 字' : value >= 10000 ? `${(value / 10000).toFixed(1)} 万字` : `${value} 字` }

onMounted(loadAll)
onUnmounted(stopPolling)
</script>

<style scoped>
.novel-detail{display:flex;flex-direction:column;gap:16px}.hero-card,.stat-card{background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(15,23,42,.06)}.hero-card{padding:24px}.hero{display:flex;justify-content:space-between;gap:20px;align-items:flex-start}.title-row{display:flex;flex-wrap:wrap;gap:12px;align-items:center;margin-top:8px}.title-row h1{margin:0;font-size:30px;color:#111827}.meta{margin:8px 0 0;color:#6b7280}.hero-actions{display:flex;flex-wrap:wrap;gap:10px;justify-content:flex-end;align-items:flex-start}.stats-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.stat-card{padding:18px}.stat-card span{display:block;font-size:13px;color:#6b7280;margin-bottom:8px}.stat-card strong{font-size:24px;color:#111827}.layout{display:grid;grid-template-columns:minmax(0,1.9fr) minmax(320px,1fr);gap:16px}.main-col,.side-col{display:flex;flex-direction:column;gap:16px}.head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.head h3{margin:0 0 4px;font-size:18px}.head p,.head span{margin:0;color:#6b7280;font-size:13px}.toolbar{display:flex;flex-wrap:wrap;gap:12px;align-items:center}.outline-box{display:flex;flex-direction:column;gap:6px}.outline-box span,.info-list span{color:#6b7280;font-size:13px}.tips{margin-top:14px;display:flex;flex-direction:column;gap:6px;color:#6b7280;font-size:13px}.progress-box{margin-top:16px;padding:14px;border:1px solid #e5e7eb;border-radius:14px;background:#f8fafc}.progress-head{display:flex;justify-content:space-between;margin-bottom:10px;font-size:14px;font-weight:600}.progress-box p{margin:10px 0 0;color:#6b7280;font-size:13px}.chapter-list,.character-list{display:flex;flex-direction:column;gap:12px}.chapter-item,.character-item{border:1px solid #e5e7eb;border-radius:14px;background:#fff;padding:14px}.chapter-item{display:flex;justify-content:space-between;gap:14px;cursor:pointer;transition:.2s}.chapter-item:hover{border-color:#93c5fd;box-shadow:0 8px 20px rgba(59,130,246,.08)}.chapter-main{display:flex;gap:12px;min-width:0}.chapter-no{white-space:nowrap;color:#6b7280;font-size:13px;padding-top:2px}.chapter-info{min-width:0}.chapter-title{font-size:16px;font-weight:600;color:#111827;margin-bottom:6px}.chapter-outline{font-size:13px;color:#6b7280;line-height:1.6;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.chapter-side{display:flex;flex-direction:column;align-items:flex-end;gap:10px;min-width:220px}.chapter-meta{display:flex;align-items:center;gap:8px;color:#6b7280;font-size:12px}.chapter-btns{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:8px}.pagination{margin-top:14px}.info-list{display:flex;flex-direction:column;gap:12px}.info-list>div{display:flex;justify-content:space-between;gap:12px}.info-list .block{flex-direction:column}.info-list p{margin:0;color:#111827;line-height:1.7}.character-top{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.character-top strong{display:inline-block;margin-right:8px;font-size:16px}.character-item p{margin:8px 0 0;color:#374151;font-size:13px;line-height:1.7}.character-item p span{color:#6b7280}.empty-wrap{min-height:420px;display:flex;align-items:center;justify-content:center}
@media (max-width:1200px){.stats-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.layout{grid-template-columns:1fr}}
@media (max-width:768px){.hero,.hero-actions,.head,.chapter-item,.chapter-main,.chapter-side,.chapter-btns,.character-top,.info-list>div{flex-direction:column;align-items:flex-start}.hero-card{padding:18px}.hero-actions,.chapter-btns{justify-content:flex-start}.stats-grid{grid-template-columns:1fr}.chapter-side{min-width:0}}
.outline-progress{margin-top:14px;padding:12px;border:1px solid #e5e7eb;border-radius:14px;background:#fff}.outline-progress-head{display:flex;justify-content:space-between;gap:12px;font-size:14px;font-weight:600;color:#111827;margin-bottom:10px}.outline-progress-msg{margin-top:10px;font-size:13px;color:#6b7280}
.indeterminate-bar{position:relative;height:10px;border-radius:999px;overflow:hidden;background:rgba(64,158,255,.12)}
.indeterminate-bar:before{content:"";position:absolute;left:-40%;top:0;height:100%;width:40%;border-radius:999px;background:linear-gradient(90deg, rgba(64,158,255,0), rgba(64,158,255,.7), rgba(64,158,255,0));animation:indeterminate 1.2s infinite}
@keyframes indeterminate{0%{left:-40%}100%{left:100%}}
</style>
