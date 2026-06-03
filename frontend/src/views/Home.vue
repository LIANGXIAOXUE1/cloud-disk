<template>
  <div class="home-page">
    <h1 class="page-title">控制台</h1>

    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row" v-loading="loading">
      <el-col :xs="24" :sm="12" :lg="6" v-for="s in cardStats" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
              <el-icon :size="28"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- Usage bar -->
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">存储空间</span></template>
          <div class="usage-section">
            <div class="usage-bar-wrapper">
              <div class="usage-info">
                <span>已用 {{ formatSize(stats.usedSpace || 0) }}</span>
                <span>剩余 {{ formatSize((stats.totalSpace || 0) - (stats.usedSpace || 0)) }}</span>
              </div>
              <div class="usage-track">
                <div class="usage-fill" :style="{ width: usagePercent + '%' }" :class="usageClass"></div>
              </div>
              <div class="usage-label">{{ usagePercent }}%</div>
              <div class="usage-total">总容量 {{ formatSize(stats.totalSpace || 0) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Pie chart -->
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">文件类型分布</span></template>
          <div ref="chartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { FolderOpened, Coin, Share, List } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const loading = ref(false)
const chartRef = ref(null)
let chart = null

const stats = ref({ totalSpace: 0, usedSpace: 0, typeStats: [] })

const usagePercent = computed(() => {
  if (!stats.value.totalSpace) return 0
  return Math.round((stats.value.usedSpace / stats.value.totalSpace) * 100)
})

const usageClass = computed(() => {
  if (usagePercent.value > 95) return 'danger'
  if (usagePercent.value > 80) return 'warning'
  return ''
})

const totalFiles = computed(() => {
  return stats.value.typeStats.reduce((s, t) => s + t.count, 0)
})

const cardStats = computed(() => [
  { icon: FolderOpened, value: totalFiles.value, label: '文件总数', color: '#8FB9A8', bg: 'rgba(143,185,168,0.12)' },
  { icon: Coin, value: formatSize(stats.value.usedSpace || 0), label: '已用空间', color: '#67c2a3', bg: 'rgba(103,194,163,0.12)' },
  { icon: Share, value: formatSize((stats.value.totalSpace || 0) - (stats.value.usedSpace || 0)), label: '剩余空间', color: '#e6a23c', bg: 'rgba(230,162,60,0.12)' },
  { icon: List, value: '10 GB', label: '总容量', color: '#6b7fd4', bg: 'rgba(107,127,212,0.12)' }
])

onMounted(() => loadStats())

async function loadStats() {
  loading.value = true
  try {
    const res = await request({ url: '/file/stats', method: 'get' })
    stats.value = res.data || { totalSpace: 0, usedSpace: 0, typeStats: [] }
    await nextTick()
    renderPie()
  } catch (_) {
    stats.value = { totalSpace: 0, usedSpace: 0, typeStats: [] }
  }
  loading.value = false
}

function renderPie() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const labels = { image: '图片', doc: '文档', video: '视频', audio: '音频', other: '其他' }
  const colors = ['#8FB9A8', '#67c2a3', '#6b7fd4', '#e6a23c', '#ccc']
  const data = (stats.value.typeStats || [])
    .filter(t => t.count > 0)
    .map((t, i) => ({ name: labels[t.type] || t.type, value: t.count, itemStyle: { color: colors[i] } }))

  if (data.length === 0) data.push({ name: '暂无文件', value: 1, itemStyle: { color: '#eee' } })

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 个 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{d}%' },
      emphasis: { label: { fontSize: 16, fontWeight: 'bold' } },
      data
    }]
  })
}

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}
</script>

<style scoped>
.home-page { max-width: 1200px; }
.page-title { font-size: var(--fs-2xl); font-weight: 700; color: var(--neutral-800); margin-bottom: var(--space-6); }
.stats-row { margin-bottom: var(--space-5); }
.stat-inner { display: flex; align-items: center; gap: var(--space-4); }
.stat-icon { width: 56px; height: 56px; border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-value { font-size: var(--fs-2xl); font-weight: 700; color: var(--neutral-800); line-height: 1.2; }
.stat-label { font-size: var(--fs-sm); color: var(--neutral-500); margin-top: 2px; }
.chart-card { border: 1px solid var(--neutral-200); }
.card-title { font-size: var(--fs-base); font-weight: 600; }
.chart-box { width: 100%; height: 260px; }
.usage-section { padding: 8px 0; }
.usage-bar-wrapper { max-width: 400px; }
.usage-info { display: flex; justify-content: space-between; font-size: 13px; color: var(--neutral-500); margin-bottom: 8px; }
.usage-track { height: 12px; background: var(--neutral-200); border-radius: 6px; overflow: hidden; margin-bottom: 6px; }
.usage-fill { height: 100%; background: var(--primary-500); border-radius: 6px; transition: width 0.6s; }
.usage-fill.warning { background: var(--warning); }
.usage-fill.danger { background: var(--error); }
.usage-label { font-size: 24px; font-weight: 700; color: var(--neutral-800); }
.usage-total { font-size: 13px; color: var(--neutral-400); margin-top: 4px; }

@media (max-width: 768px) {
  .page-title { font-size: var(--fs-xl); }
  .stat-value { font-size: var(--fs-xl); }
}
</style>
