<template>
  <div class="file-page">
    <h1 class="page-title">文件管理</h1>

    <!-- Upload Zone -->
    <UploadZone
      accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z,.tar,.gz"
      accept-hint="支持图片、视频、文档、压缩包等全格式文件"
      class="upload-section"
      :parent-id="currentParentId"
      @upload-complete="onUploadComplete"
      @upload-error="onUploadError"
    />

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="FolderAdd" @click="showCreateDialog = true">新建文件夹</el-button>
        <template v-if="selectedRows.length > 0">
          <span class="batch-count">已选 {{ selectedRows.length }} 项</span>
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
        </template>
        <el-button v-if="isSearching" size="small" @click="clearSearch">← 返回</el-button>
      </div>
      <div class="toolbar-right">
        <el-select v-model="searchType" placeholder="全部类型" size="default" style="width:110px" @change="doSearch">
          <el-option label="全部" value="" />
          <el-option label="图片" value="image" />
          <el-option label="文档" value="doc" />
          <el-option label="视频" value="video" />
          <el-option label="音频" value="audio" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-input v-model="searchQuery" placeholder="搜索文件…" :prefix-icon="Search" style="width: 220px" clearable @clear="clearSearch" @keyup.enter="doSearch" />
      </div>
    </div>

    <!-- Search results count -->
    <div v-if="isSearching" class="search-info">
      搜索"<strong>{{ searchQuery }}</strong>"，找到 {{ fileList.length }} 个结果
    </div>

    <!-- Breadcrumb -->
    <div v-if="!isSearching">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item @click="goToRoot">
        <el-icon :size="14"><HomeFilled /></el-icon>
        <span style="margin-left:4px">全部文件</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="idx" @click="goToPath(idx)">{{ item }}</el-breadcrumb-item>
    </el-breadcrumb>
    </div>

    <!-- File Table -->
    <el-card shadow="hover" class="file-card">
      <el-table :data="fileList" style="width: 100%" stripe @row-dblclick="handleRowClick" v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="42" />
        <el-table-column label="文件名" min-width="280">
          <template #default="{ row }">
            <div class="file-name-cell">
              <div class="file-icon-box" :style="{ background: getFileIconBg(row) }">
                <img
                  v-if="isImageFile(row)"
                  :src="`/api/file/thumb/${row.id}`"
                  class="file-thumb"
                  @error="e => e.target.style.display='none'"
                />
                <el-icon v-show="!isImageFile(row)" :size="22" :color="getFileIconColor(row)">
                  <component :is="getFileIcon(row)" />
                </el-icon>
              </div>
              <div class="file-name-info">
                <span class="file-name-text" v-html="highlightName(row.fileName)"></span>
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

    <!-- Image Viewer -->
    <ImageViewer v-model="viewerVisible" :images="viewerImages" :initial-index="viewerIndex" />

    <!-- PDF Viewer -->
    <PdfViewer v-model="pdfVisible" :file="pdfFile" />

    <!-- Code Viewer -->
    <CodeViewer v-model="codeVisible" :file="codeFile" />

    <!-- Office Viewer -->
    <OfficeViewer v-model="officeVisible" :file="officeFile" />

    <!-- Video Player -->
    <VideoPlayer v-model="videoVisible" :file="videoFile" />

    <!-- Audio Player -->
    <AudioPlayer v-model="audioVisible" :file="audioFile" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderAdd, Search, FolderOpened, Document, HomeFilled,
  PictureFilled, VideoCameraFilled, Files, Memo
} from '@element-plus/icons-vue'
import { getFileList, searchFiles, createFolder, renameFile, deleteFile } from '@/api/modules/file'
import { createShare } from '@/api/modules/share'
import UploadZone from '@/components/UploadZone.vue'
import ImageViewer from '@/components/ImageViewer.vue'
import PdfViewer from '@/components/PdfViewer.vue'
import CodeViewer from '@/components/CodeViewer.vue'
import OfficeViewer from '@/components/OfficeViewer.vue'
import VideoPlayer from '@/components/VideoPlayer.vue'
import AudioPlayer from '@/components/AudioPlayer.vue'

const userId = 1

const fileList = ref([])
const loading = ref(false)
const searchQuery = ref('')
const searchType = ref('')
const isSearching = computed(() => !!searchQuery.value?.trim())
const breadcrumb = ref([])
const breadcrumbIds = ref([])   // 对应文件夹 ID 列表（与 breadcrumb 同步）
const currentParentId = ref(null)

const showCreateDialog = ref(false)
const newFolderName = ref('')
const showRenameDialog = ref(false)
const renameTarget = ref(null)
const renameValue = ref('')
const showShareDialog = ref(false)
const shareTarget = ref(null)
const sharePassword = ref('')

// 批量操作
const selectedRows = ref([])

// 图片预览
const viewerVisible = ref(false)
const viewerImages = ref([])
const viewerIndex = ref(0)

// PDF 预览
const pdfVisible = ref(false)
const pdfFile = ref(null)

// 文本/代码预览
const codeVisible = ref(false)
const codeFile = ref(null)

// Office 文档预览
const officeVisible = ref(false)
const officeFile = ref(null)

// 视频播放
const videoVisible = ref(false)
const videoFile = ref(null)

// 音频播放
const audioVisible = ref(false)
const audioFile = ref(null)

onMounted(() => loadFiles())

async function loadFiles() {
  loading.value = true
  try {
    const keyword = searchQuery.value?.trim()
    if (keyword) {
      const res = await searchFiles(userId, keyword)
      let list = res.data || []
      // Apply type filter
      if (searchType.value) {
        list = list.filter(f => matchTypeFilter(f.fileType, searchType.value))
      }
      fileList.value = list
    } else {
      const res = await getFileList(userId, currentParentId.value)
      fileList.value = res.data?.list || []
    }
  } catch (e) {
    fileList.value = []
  } finally {
    loading.value = false
  }
}

function matchTypeFilter(ext, type) {
  if (!ext) ext = ''
  ext = ext.toLowerCase()
  const groups = {
    image: ['jpg','jpeg','png','gif','webp','svg','bmp','ico'],
    doc: ['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','md','csv','json','xml'],
    video: ['mp4','webm','ogg','ogv','mov','avi','mkv','flv','wmv','m4v'],
    audio: ['mp3','wav','ogg','oga','flac','aac','m4a','wma']
  }
  const group = groups[type]
  if (group) return group.includes(ext)
  if (type === 'other') return !groups.image.includes(ext) && !groups.doc.includes(ext) && !groups.video.includes(ext) && !groups.audio.includes(ext)
  return true
}

function clearSearch() {
  searchQuery.value = ''
  searchType.value = ''
  loadFiles()
}

function doSearch() {
  loadFiles()
}

function highlightName(name) {
  const q = searchQuery.value?.trim()
  if (!q || !name) return name
  const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return name.replace(regex, '<mark class="search-highlight">$1</mark>')
}

function onUploadComplete() {
  ElMessage.success('文件上传完成')
  loadFiles()
}

function onUploadError({ file }) {
  ElMessage.error(`「${file?.name || '未知文件'}」上传失败`)
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
    breadcrumbIds.value.push(row.id)
    currentParentId.value = row.id
    loadFiles()
  } else if (isImageFile(row)) {
    viewerImages.value = fileList.value
    viewerIndex.value = fileList.value.findIndex(f => f.id === row.id)
    viewerVisible.value = true
  } else if (isPdfFile(row)) {
    pdfFile.value = row
    pdfVisible.value = true
  } else if (isTextFile(row)) {
    codeFile.value = row
    codeVisible.value = true
  } else if (isOfficeFile(row)) {
    officeFile.value = row
    officeVisible.value = true
  } else if (isVideoFile(row)) {
    videoFile.value = row
    videoVisible.value = true
  } else if (isAudioFile(row)) {
    audioFile.value = row
    audioVisible.value = true
  }
}

function goToRoot() {
  breadcrumb.value = []
  breadcrumbIds.value = []
  currentParentId.value = null
  loadFiles()
}

function goToPath(idx) {
  breadcrumb.value = breadcrumb.value.slice(0, idx + 1)
  breadcrumbIds.value = breadcrumbIds.value.slice(0, idx + 1)
  currentParentId.value = breadcrumbIds.value.length > 0 ? breadcrumbIds.value[breadcrumbIds.value.length - 1] : null
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

function onSelectionChange(rows) {
  selectedRows.value = rows
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 个文件？删除后将移入回收站。`,
      '批量删除', { type: 'warning' }
    )
    const ids = selectedRows.value.map(r => r.id)
    await deleteFile(ids[0]) // fallback: delete one by one
    for (let i = 1; i < ids.length; i++) {
      await deleteFile(ids[i])
    }
    ElMessage.success(`已移入回收站 (${ids.length} 个文件)`)
    selectedRows.value = []
    loadFiles()
  } catch (e) { /* cancelled */ }
}

let searchTimer = null
watch(searchQuery, (val) => {
  clearTimeout(searchTimer)
  if (!val?.trim()) {
    searchTimer = setTimeout(() => {
      searchType.value = ''
      loadFiles()
    }, 400)
  }
})

// ====== Helpers ======
function isImageFile(row) {
  const imgExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico']
  return !row.isFolder && imgExts.includes((row.fileType || '').toLowerCase())
}

function isPdfFile(row) {
  return !row.isFolder && (row.fileType || '').toLowerCase() === 'pdf'
}

function isTextFile(row) {
  const textExts = ['txt', 'md', 'markdown', 'json', 'xml', 'html', 'htm', 'yaml', 'yml',
    'py', 'java', 'js', 'jsx', 'ts', 'tsx', 'css', 'scss', 'less',
    'sql', 'sh', 'bat', 'ps1', 'ini', 'cfg', 'log', 'c', 'cpp', 'cs',
    'go', 'rs', 'php', 'rb', 'swift', 'kt', 'vue', 'svelte']
  return !row.isFolder && textExts.includes((row.fileType || '').toLowerCase())
}

function isOfficeFile(row) {
  const officeExts = ['docx', 'doc', 'xlsx', 'xls']
  return !row.isFolder && officeExts.includes((row.fileType || '').toLowerCase())
}

function isVideoFile(row) {
  const videoExts = ['mp4', 'webm', 'ogg', 'ogv', 'mov', 'avi', 'mkv', 'flv', 'wmv', 'm4v']
  return !row.isFolder && videoExts.includes((row.fileType || '').toLowerCase())
}

function isAudioFile(row) {
  const audioExts = ['mp3', 'wav', 'ogg', 'oga', 'flac', 'aac', 'm4a', 'wma']
  return !row.isFolder && audioExts.includes((row.fileType || '').toLowerCase())
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
  align-items: center;
}

.batch-count {
  font-size: 13px;
  color: var(--neutral-500);
  font-weight: 500;
}

.breadcrumb {
  margin-bottom: var(--space-4);
  cursor: pointer;
}

.search-info {
  font-size: 13px;
  color: var(--neutral-500);
  margin-bottom: 12px;
  padding: 2px 0;
}

:deep(.search-highlight) {
  background: #fff3cd;
  color: #856404;
  padding: 1px 2px;
  border-radius: 2px;
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
  overflow: hidden;
  transition: transform var(--duration-fast) var(--ease-smooth);
}
.file-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  .toolbar-right {
    flex-wrap: wrap;
  }
  .toolbar-right .el-input {
    width: 100% !important;
  }
  .toolbar-right .el-select {
    width: 100% !important;
  }
}
</style>

