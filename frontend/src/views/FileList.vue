<template>
  <div class="file-page">
    <h1 class="page-title">文件管理</h1>

    <!-- Upload Zone -->
    <UploadZone
      accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z,.tar,.gz"
      accept-hint="支持图片、视频、文档、压缩包等全格式文件"
      class="upload-section"
      @upload-complete="onUploadComplete"
    />

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="FolderAdd" @click="showCreateDialog = true">新建文件夹</el-button>
      </div>
      <div class="toolbar-right">
        <el-input v-model="searchQuery" placeholder="搜索文件…" :prefix-icon="Search" style="width: 260px" clearable @input="handleSearch" />
      </div>
    </div>

    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item @click="goToRoot">
        <el-icon :size="14"><HomeFilled /></el-icon>
        <span style="margin-left:4px">全部文件</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx" @click="goToPath(idx)">{{ item }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- File Table -->
    <el-card shadow="hover" class="file-card">
      <el-table :data="fileList" style="width: 100%" stripe @row-dblclick="handleRowClick" v-loading="loading">
        <el-table-column label="文件名" min-width="280">
          <template #default="{ row }">
            <div class="file-name-cell">
              <div class="file-icon-box" :style="{ background: getFileIconBg(row) }">
                <el-icon :size="22" :color="getFileIconColor(row)">
                  <component :is="getFileIcon(row)" />
                </el-icon>
              </div>
              <div class="file-name-info">
                <span class="file-name-text">{{ row.fileName }}</span>
                <span class="file-type-tag" v-if="!row.isFolder">{{ getFileExt(row) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ row.isFolder ? '-' : formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="修改时间" width="180">
          <template #default="{ row }">{{ row.updatedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleRename(row)">重命名</el-button>
            <el-button link type="primary" size="small" @click="handleMove(row)">移动</el-button>
            <el-button link type="warning" size="small" @click="handleShare(row)">分享</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && fileList.length === 0" description="暂无文件，拖拽文件到上方区域上传" />
    </el-card>

    <!-- Create Folder Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建文件夹" width="420px">
      <el-input v-model="newFolderName" placeholder="输入文件夹名称" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder">创建</el-button>
      </template>
    </el-dialog>

    <!-- Rename Dialog -->
    <el-dialog v-model="showRenameDialog" title="重命名" width="420px">
      <el-input v-model="renameValue" placeholder="新名称" />
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确认</el-button>
      </template>
    </el-dialog>

    <!-- Share Dialog -->
    <el-dialog v-model="showShareDialog" title="创建分享链接" width="420px">
      <el-input v-model="sharePassword" placeholder="提取密码（可选）" />
      <template #footer>
        <el-button @click="showShareDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmShare">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderAdd, Search, FolderOpened, Document, HomeFilled,
  PictureFilled, VideoCameraFilled, Files, Memo
} from '@element-plus/icons-vue'
import { getFileList, createFolder, renameFile, deleteFile } from '@/api/modules/file'
import { createShare } from '@/api/modules/share'
import UploadZone from '@/components/UploadZone.vue'

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

function onUploadComplete() {
  ElMessage.success('文件上传完成，正在刷新列表…')
  setTimeout(() => loadFiles(), 500)
}

async function handleCreateFolder() {
  if (!newFolderName.value.trim()) return
  try {
    await createFolder(userId, newFolderName.value, currentParentId.value)
    ElMessage.success('文件夹创建成功')
    showCreateDialog.value = false
    newFolderName.value = ''
    loadFiles()
  } catch (e) {
    ElMessage.error('创建失败')
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
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    loadFiles()
  } catch (e) {
    ElMessage.error('重命名失败')
  }
}

function handleMove(row) {
  ElMessage.info('移动功能即将上线')
}

function handleShare(row) {
  shareTarget.value = row
  sharePassword.value = ''
  showShareDialog.value = true
}

async function confirmShare() {
  try {
    const res = await createShare(shareTarget.value.id, userId, sharePassword.value, 1)
    ElMessage.success('分享链接：' + res.data)
    showShareDialog.value = false
  } catch (e) {
    ElMessage.error('分享创建失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.fileName}」？删除后将移入回收站。`, '确认删除', { type: 'warning' })
    await deleteFile(row.id)
    ElMessage.success('已移入回收站')
    loadFiles()
  } catch (e) { /* cancelled */ }
}

function handleSearch() {
  loadFiles()
}

function getFileIcon(row) {
  if (row.isFolder) return FolderOpened
  const name = (row.fileName || '').toLowerCase()
  const ext = name.split('.').pop()
  if (['jpg','jpeg','png','gif','webp','svg','bmp','ico','heic'].includes(ext)) return PictureFilled
  if (['mp4','avi','mov','wmv','flv','mkv','webm','m4v'].includes(ext)) return VideoCameraFilled
  if (['zip','rar','7z','tar','gz','bz2','xz'].includes(ext)) return Files
  if (['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv'].includes(ext)) return Memo
  return Document
}

function getFileIconColor(row) {
  if (row.isFolder) return '#e6a23c'
  const name = (row.fileName || '').toLowerCase()
  const ext = name.split('.').pop()
  if (['jpg','jpeg','png','gif','webp','svg','bmp','ico','heic'].includes(ext)) return '#e75c8a'
  if (['mp4','avi','mov','wmv','flv','mkv','webm','m4v'].includes(ext)) return '#6b7fd4'
  if (['zip','rar','7z','tar','gz','bz2','xz'].includes(ext)) return '#e69a45'
  if (['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv'].includes(ext)) return '#5b9bd5'
  return 'var(--primary-500)'
}

function getFileIconBg(row) {
  if (row.isFolder) return 'rgba(230, 162, 60, 0.1)'
  const name = (row.fileName || '').toLowerCase()
  const ext = name.split('.').pop()
  if (['jpg','jpeg','png','gif','webp','svg','bmp','ico','heic'].includes(ext)) return 'rgba(231, 92, 138, 0.08)'
  if (['mp4','avi','mov','wmv','flv','mkv','webm','m4v'].includes(ext)) return 'rgba(107, 127, 212, 0.08)'
  if (['zip','rar','7z','tar','gz','bz2','xz'].includes(ext)) return 'rgba(230, 154, 69, 0.08)'
  if (['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv'].includes(ext)) return 'rgba(91, 155, 213, 0.08)'
  return 'rgba(143, 185, 168, 0.1)'
}

function getFileExt(row) {
  if (row.isFolder) return ''
  const name = (row.fileName || '')
  const dot = name.lastIndexOf('.')
  return dot > -1 ? name.slice(dot + 1).toUpperCase() : ''
}

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}
</script>

<style scoped>
.file-page {
  max-width: 1200px;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

.upload-section {
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
  cursor: pointer;
}

.file-card {
  border: 1px solid var(--neutral-100);
}

/* File Name Cell */
.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.file-icon-box {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-smooth);
}
.el-table__row:hover .file-icon-box {
  transform: scale(1.08);
}

.file-name-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.file-name-text {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--neutral-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-type-tag {
  font-size: 10px;
  font-weight: 600;
  color: var(--neutral-400);
  background: var(--neutral-100);
  padding: 1px 6px;
  border-radius: 4px;
  letter-spacing: 0.05em;
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .page-title {
    font-size: var(--fs-xl);
    margin-bottom: var(--space-4);
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right .el-input {
    width: 100% !important;
  }
}
</style>