<template>
  <div class="analytics-view">
    <div class="header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>{{ novel?.title }} - 数据分析</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>高频词统计</template>
          <v-chart :option="wordCloudOption" style="height: 400px" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>章节字数分布</template>
          <v-chart :option="chapterWordsOption" style="height: 400px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>创作进度</template>
          <v-chart :option="progressOption" style="height: 300px" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>统计数据</template>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-label">总字数</div>
              <div class="stat-value">{{ stats?.totalWords || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总章节</div>
              <div class="stat-value">{{ stats?.totalChapters || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">完成章节</div>
              <div class="stat-value">{{ stats?.completedChapters || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">平均字数</div>
              <div class="stat-value">{{ stats?.averageWordsPerChapter || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { novelApi } from '@/api'
import type { Novel, NovelStatistics } from '@/api'
import { handleViewError } from '@/utils/error'

use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const route = useRoute()
const novelId = Number(route.params.novelId)

const novel = ref<Novel | null>(null)
const stats = ref<NovelStatistics | null>(null)
const wordFreq = ref<Array<{ word: string; count: number }>>([])

const wordCloudOption = computed(() => ({
  tooltip: { trigger: 'item' },
  grid: { left: 80, right: 20, bottom: 100 },
  xAxis: {
    type: 'category',
    data: wordFreq.value.slice(0, 20).map((w) => w.word),
    axisLabel: { interval: 0, rotate: 45, fontSize: 12 },
  },
  yAxis: { type: 'value', name: '出现次数' },
  series: [
    {
      type: 'bar',
      data: wordFreq.value.slice(0, 20).map((w) => w.count),
      itemStyle: { color: '#409EFF' },
    },
  ],
}))

const chapterWordsOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: novel.value?.chapters?.map((c) => `第${c.chapterNumber}章`) || [],
  },
  yAxis: { type: 'value', name: '字数' },
  series: [{ type: 'bar', data: novel.value?.chapters?.map((c) => c.wordCount) || [] }],
}))

const progressOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 10 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: stats.value?.completedChapters || 0, name: '已完成' },
        {
          value: (stats.value?.totalChapters || 0) - (stats.value?.completedChapters || 0),
          name: '未完成',
        },
      ],
    },
  ],
}))

onMounted(async () => {
  try {
    const [novelRes, statsRes, freqRes] = await Promise.all([
      novelApi.get(novelId),
      novelApi.getStatistics(novelId),
      novelApi.getWordFrequency(novelId),
    ])
    novel.value = novelRes.data
    stats.value = statsRes.data
    wordFreq.value = freqRes.data
  } catch (error) {
    handleViewError('NovelAnalyticsView', error, '数据分析加载失败')
  }
})
</script>

<style scoped>
.analytics-view {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
</style>
