<template>
  <div class="recycle-page">
    <h1 class="page-title">回收站</h1>

    <div class="toolbar">
      <el-button type="danger" :icon="Delete" @click="handleClear" :disabled="recycleList.length === 0">清空回收站</el-button>
      <span class="toolbar-hint">文件将在 30 天后自动清除</span>
    </div>

    <el-card shadow="hover">
      <el-table :data="recycleList" style="width: 100%" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="名称" min-width="240">
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="18" :color="row.isFolder ? 'var(--warning)' : 'var(--primary-500)'">
                <FolderOpened v-if="row.isFolder" />
                <Document v-else />
              </el-icon>
              <span>{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ row.isFolder ? '-' : formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="deletedAt" label="删除时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleRestore(row)">恢复</el-button>
            <el-button link type="danger" size="small" @click="handlePermanentDelete(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recycleList.length === 0" description="Recycle bin is empty" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, FolderOpened, Document } from '@element-plus/icons-vue'
import { getRecycleList, restoreFile, permanentDelete, clearRecycle } from '@/api/modules/recycle'

const userId = 1

const recycleList = ref([])
const loading = ref(false)
const selectedRows = ref([])

onMounted(() => loadRecycle())

async function loadRecycle() {
  loading.value = true
  try {
    const res = await getRecycleList(userId)
    recycleList.value = res.data?.list || []
  } catch (e) {
    recycleList.value = []
  } finally {
    loading.value = false
  }
}

async function handleRestore(row) {
  try {
    await restoreFile(row.id)
    ElMessage.success('File restored')
    loadRecycle()
  } catch (e) {
    ElMessage.error('Failed to restore')
  }
}

async function handlePermanentDelete(row) {
  try {
    await ElMessageBox.confirm(`Permanently delete "${row.fileName}"? This cannot be undone.`, 'Warning', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'error'
    })
    await permanentDelete(row.id)
    ElMessage.success('Deleted permanently')
    loadRecycle()
  } catch (e) { /* cancelled */ }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('Delete all items in recycle bin? This cannot be undone.', 'Clear Recycle Bin', {
      confirmButtonText: 'Clear All',
      cancelButtonText: 'Cancel',
      type: 'error'
    })
    await clearRecycle(userId)
    ElMessage.success('Recycle bin cleared')
    loadRecycle()
  } catch (e) { /* cancelled */ }
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}
</script>

<style scoped>
.recycle-page {
  max-width: 1200px;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.toolbar-hint {
  font-size: var(--fs-sm);
  color: var(--neutral-400);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-sm);
}
</style>