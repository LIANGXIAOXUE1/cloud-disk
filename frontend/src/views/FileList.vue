<template>
  <div class="file-page">
    <h1 class="page-title">File Manager</h1>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="FolderAdd" @click="showCreateDialog = true">New Folder</el-button>
        <el-button :icon="Upload">Upload</el-button>
      </div>
      <div class="toolbar-right">
        <el-input v-model="searchQuery" placeholder="Search files..." :prefix-icon="Search" style="width: 260px" clearable @input="handleSearch" />
      </div>
    </div>

    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item @click="goToRoot">Home</el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx" @click="goToPath(idx)">{{ item }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- File Table -->
    <el-card shadow="hover">
      <el-table :data="fileList" style="width: 100%" stripe @row-dblclick="handleRowClick" v-loading="loading">
        <el-table-column label="Name" min-width="240">
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
        <el-table-column prop="fileSize" label="Size" width="120">
          <template #default="{ row }">{{ row.isFolder ? '-' : formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Modified" width="180">
          <template #default="{ row }">{{ row.updatedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="280">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleRename(row)">Rename</el-button>
            <el-button link type="primary" size="small" @click="handleMove(row)">Move</el-button>
            <el-button link type="warning" size="small" @click="handleShare(row)">Share</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && fileList.length === 0" description="No files" />
    </el-card>

    <!-- Create Folder Dialog -->
    <el-dialog v-model="showCreateDialog" title="New Folder" width="420px">
      <el-input v-model="newFolderName" placeholder="Folder name" />
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" @click="handleCreateFolder">Create</el-button>
      </template>
    </el-dialog>

    <!-- Rename Dialog -->
    <el-dialog v-model="showRenameDialog" title="Rename" width="420px">
      <el-input v-model="renameValue" placeholder="New name" />
      <template #footer>
        <el-button @click="showRenameDialog = false">Cancel</el-button>
        <el-button type="primary" @click="confirmRename">Confirm</el-button>
      </template>
    </el-dialog>

    <!-- Share Dialog -->
    <el-dialog v-model="showShareDialog" title="Create Share Link" width="420px">
      <el-input v-model="sharePassword" placeholder="Password (optional)" />
      <template #footer>
        <el-button @click="showShareDialog = false">Cancel</el-button>
        <el-button type="primary" @click="confirmShare">Create</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderAdd, Upload, Search, FolderOpened, Document } from '@element-plus/icons-vue'
import { getFileList, createFolder, renameFile, deleteFile } from '@/api/modules/file'
import { createShare } from '@/api/modules/share'

const userId = 1

const fileList = ref([])
const loading = ref(false)
const searchQuery = ref('')
const breadcrumb = ref([])
const currentParentId = ref(null)

const showCreateDialog = ref(false)
const newFolderName = ref('')
const showRenameDialog = ref(false)
const renameTarget = ref(null)
const renameValue = ref('')
const showShareDialog = ref(false)
const shareTarget = ref(null)
const sharePassword = ref('')

onMounted(() => loadFiles())

async function loadFiles() {
  loading.value = true
  try {
    const res = await getFileList(userId, currentParentId.value)
    fileList.value = res.data?.list || []
  } catch (e) {
    fileList.value = []
  } finally {
    loading.value = false
  }
}

async function handleCreateFolder() {
  if (!newFolderName.value.trim()) return
  try {
    await createFolder(userId, newFolderName.value, currentParentId.value)
    ElMessage.success('Folder created')
    showCreateDialog.value = false
    newFolderName.value = ''
    loadFiles()
  } catch (e) {
    ElMessage.error('Failed to create folder')
  }
}

function handleRowClick(row) {
  if (row.isFolder) {
    breadcrumb.value.push(row.fileName)
    currentParentId.value = row.id
    loadFiles()
  }
}

function goToRoot() {
  breadcrumb.value = []
  currentParentId.value = null
  loadFiles()
}

function goToPath(idx) {
  breadcrumb.value = breadcrumb.value.slice(0, idx + 1)
  // In a real app, track parent IDs for each breadcrumb level
  currentParentId.value = null
  loadFiles()
}

function handleRename(row) {
  renameTarget.value = row
  renameValue.value = row.fileName
  showRenameDialog.value = true
}

async function confirmRename() {
  if (!renameValue.value.trim()) return
  try {
    await renameFile(renameTarget.value.id, renameValue.value)
    ElMessage.success('Renamed')
    showRenameDialog.value = false
    loadFiles()
  } catch (e) {
    ElMessage.error('Failed to rename')
  }
}

function handleMove(row) {
  ElMessage.info('Move functionality coming soon')
}

function handleShare(row) {
  shareTarget.value = row
  sharePassword.value = ''
  showShareDialog.value = true
}

async function confirmShare() {
  try {
    const res = await createShare(shareTarget.value.id, userId, sharePassword.value, 1)
    ElMessage.success('Share link: ' + res.data)
    showShareDialog.value = false
  } catch (e) {
    ElMessage.error('Failed to create share')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`Delete "${row.fileName}"?`, 'Confirm', { type: 'warning' })
    await deleteFile(row.id)
    ElMessage.success('Moved to recycle bin')
    loadFiles()
  } catch (e) {
    // cancelled
  }
}

function handleSearch() {
  loadFiles()
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
.file-page {
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
  gap: var(--space-3);
}

.toolbar-left {
  display: flex;
  gap: var(--space-3);
}

.breadcrumb {
  margin-bottom: var(--space-4);
}

.breadcrumb :deep(.el-breadcrumb__item) {
  cursor: pointer;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-sm);
}
</style>