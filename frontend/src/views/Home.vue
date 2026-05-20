<template>
  <div class="home-page">
    <h1 class="page-title">Dashboard</h1>

    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: stat.color + '15', color: stat.color }">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Activity -->
    <el-card shadow="hover" class="activity-card">
      <template #header>
        <span class="card-header">Recent Activity</span>
      </template>
      <el-table :data="activities" style="width: 100%" stripe>
        <el-table-column prop="file" label="File Name" />
        <el-table-column prop="action" label="Action" width="180">
          <template #default="{ row }">
            <el-tag :type="row.tagType" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="Time" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { FolderOpened, Coin, Share, List } from '@element-plus/icons-vue'

const stats = ref([
  { icon: FolderOpened, value: '128', label: 'Total Files', color: 'var(--primary-500)' },
  { icon: Coin, value: '2.4 GB', label: 'Used Storage', color: 'var(--success)' },
  { icon: Share, value: '6', label: 'Active Shares', color: 'var(--warning)' },
  { icon: List, value: '3', label: 'Transfer Tasks', color: 'var(--info)' }
])

const activities = ref([
  { file: 'project-docs.zip', action: 'Uploaded', tagType: 'success', time: '2026-05-18 10:30' },
  { file: 'images/', action: 'Created', tagType: '', time: '2026-05-18 09:15' },
  { file: 'design-v2.psd', action: 'Shared', tagType: 'warning', time: '2026-05-17 16:45' },
  { file: 'backup-2026.zip', action: 'Recycled', tagType: 'danger', time: '2026-05-17 14:20' },
  { file: 'notes.txt', action: 'Transferred', tagType: 'info', time: '2026-05-17 11:00' }
])
</script>

<style scoped>
.home-page {
  max-width: 1200px;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

.stats-row {
  margin-bottom: var(--space-6);
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--neutral-800);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--fs-sm);
  color: var(--neutral-500);
  margin-top: 2px;
}

.activity-card {
  border: 1px solid var(--neutral-200);
}

.card-header {
  font-size: var(--fs-lg);
  font-weight: 600;
}
</style>