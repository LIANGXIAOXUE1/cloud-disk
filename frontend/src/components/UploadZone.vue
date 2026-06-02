<template>
  <div class="upload-zone-wrapper">
    <!-- Drop Zone -->
    <div
      class="upload-zone"
      :class="{ 'is-dragover': isDragOver, 'is-disabled': disabled }"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
      @click="triggerInput"
    >
      <input
        ref="fileInput"
        type="file"
        :accept="accept"
        :multiple="multiple"
        class="upload-input-hidden"
        @change="onFileSelect"
      />
      <div class="upload-zone-content">
        <div class="upload-icon-wrapper">
          <svg viewBox="0 0 48 48" fill="none" class="upload-icon-svg">
            <path d="M12 28 C6 28 2 22 6 18 C4 10 14 6 20 12 C22 6 30 4 34 12 C42 10 46 18 40 22 C46 26 42 34 36 34 L14 34 C8 34 10 28 12 28Z" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
            <path d="M24 22V34M18 28L24 22L30 28" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <p class="upload-title">拖拽文件到此处上传</p>
        <p class="upload-hint">{{ acceptHint || '支持图片、视频、文档、压缩包等全格式' }}</p>
      </div>
    </div>

    <!-- Upload Queue -->
    <div v-if="queue.length > 0" class="upload-queue">
      <div
        v-for="(task, idx) in queue"
        :key="idx"
        class="upload-task"
      >
        <div class="task-preview">
          <img v-if="task.previewUrl" :src="task.previewUrl" class="task-thumb" />
          <div v-else class="task-file-icon">
            <el-icon :size="24"><component :is="getFileIcon(task.file)" /></el-icon>
          </div>
        </div>
        <div class="task-info">
          <span class="task-name" :title="task.file.name">{{ task.file.name }}</span>
          <span class="task-size">{{ formatSize(task.file.size) }}</span>
          <div class="task-progress-bar">
            <el-progress
              :percentage="task.percent"
              :status="getProgressStatus(task.status)"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
          <span class="task-status-text">
            <template v-if="task.status === 'uploading'">
              <template v-if="task.isChunked">分片 {{ task.chunkIndex + 1 }}/{{ task.totalChunks }} — {{ task.percent.toFixed(0) }}%</template>
              <template v-else>{{ task.percent.toFixed(0) }}% — {{ formatSize(task.uploaded) }} / {{ formatSize(task.file.size) }}</template>
            </template>
            <template v-else-if="task.status === 'paused'">已暂停</template>
            <template v-else-if="task.status === 'completed'">上传完成</template>
            <template v-else-if="task.status === 'error'">{{ task.error || '上传失败' }}</template>
            <template v-else>等待中</template>
          </span>
        </div>
        <div class="task-actions">
          <el-button
            v-if="task.status === 'uploading'"
            size="small"
            circle
            :icon="VideoPause"
            @click.stop="pauseTask(idx)"
            title="暂停"
          />
          <el-button
            v-if="task.status === 'paused'"
            size="small"
            circle
            type="primary"
            :icon="VideoPlay"
            @click.stop="resumeTask(idx)"
            title="继续"
          />
          <el-button
            v-if="task.status === 'pending'"
            size="small"
            circle
            type="success"
            :icon="Check"
            @click.stop="startTask(idx)"
            title="开始上传"
          />
          <el-button
            v-if="task.status === 'error'"
            size="small"
            circle
            type="primary"
            :icon="VideoPlay"
            @click.stop="retryTask(idx)"
            title="重试"
          />
          <el-button
            size="small"
            circle
            type="danger"
            :icon="Close"
            @click.stop="removeTask(idx)"
            title="取消"
          />
        </div>
      </div>
    </div>

    <!-- Batch Actions -->
    <div v-if="queue.length > 0" class="upload-batch-actions">
      <el-button size="small" @click="clearCompleted">清除已完成</el-button>
      <el-button v-if="hasPending" size="small" type="primary" @click="startAll">全部上传</el-button>
      <el-button v-if="hasUploading" size="small" type="warning" @click="pauseAll">全部暂停</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import {
  VideoPause, VideoPlay, Check, Close,
  PictureFilled, VideoCameraFilled, Document, Files
} from '@element-plus/icons-vue'
import {
  uploadFile as apiUploadFile,
  initChunkUpload,
  uploadChunk as apiUploadChunk,
  getChunkProgress,
  mergeChunks
} from '@/api/modules/file'

const props = defineProps({
  accept: { type: String, default: '' },
  acceptHint: { type: String, default: '' },
  multiple: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  autoUpload: { type: Boolean, default: false },
  parentId: { type: [Number, String], default: null }
})

const emit = defineEmits(['upload-start', 'upload-progress', 'upload-complete', 'upload-error', 'upload-cancel'])

// 分片大小 5MB，大于 100MB 自动切片
const CHUNK_SIZE = 5 * 1024 * 1024
const DIRECT_THRESHOLD = 100 * 1024 * 1024

const fileInput = ref(null)
const isDragOver = ref(false)
const queue = reactive([])

const hasPending = computed(() => queue.some(t => t.status === 'pending'))
const hasUploading = computed(() => queue.some(t => t.status === 'uploading'))

function triggerInput() {
  if (props.disabled) return
  fileInput.value?.click()
}

function onDragOver() {
  if (props.disabled) return
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(e) {
  isDragOver.value = false
  if (props.disabled) return
  const files = Array.from(e.dataTransfer.files)
  if (files.length) addFiles(files)
}

function onFileSelect(e) {
  const files = Array.from(e.target.files)
  if (files.length) addFiles(files)
  e.target.value = ''
}

function addFiles(files) {
  for (const file of files) {
    const isChunked = file.size >= DIRECT_THRESHOLD
    const totalChunks = isChunked ? Math.ceil(file.size / CHUNK_SIZE) : 0

    const task = reactive({
      file,
      status: props.autoUpload ? 'uploading' : 'pending',
      percent: 0,
      uploaded: 0,
      error: '',
      previewUrl: '',
      abortController: null,
      isChunked,
      totalChunks,
      chunkIndex: 0,
      uploadId: ''
    })

    if (file.type.startsWith('image/')) {
      const reader = new FileReader()
      reader.onload = (e) => { task.previewUrl = e.target.result }
      reader.readAsDataURL(file)
    }

    queue.push(task)
    emit('upload-start', { file, task })

    if (props.autoUpload) {
      startUpload(queue.length - 1)
    }
  }
}

// ========== Upload Control ==========

function startTask(idx) {
  const task = queue[idx]
  if (!task || task.status === 'uploading' || task.status === 'completed') return
  task.status = 'uploading'
  task.error = ''
  task.percent = 0
  task.uploaded = 0
  task.chunkIndex = 0
  startUpload(idx)
}

function retryTask(idx) {
  const task = queue[idx]
  if (!task) return
  task.status = 'uploading'
  task.error = ''
  task.percent = 0
  task.uploaded = 0
  task.chunkIndex = 0
  task.uploadId = ''
  startUpload(idx)
}

function startAll() {
  queue.forEach((t, i) => {
    if (t.status === 'pending' || t.status === 'error') startTask(i)
  })
}

function pauseTask(idx) {
  const task = queue[idx]
  if (!task || task.status !== 'uploading') return
  task.status = 'paused'
  task.abortController?.abort()
}

function pauseAll() {
  queue.forEach((t, i) => {
    if (t.status === 'uploading') pauseTask(i)
  })
}

async function resumeTask(idx) {
  const task = queue[idx]
  if (!task || task.status !== 'paused') return
  task.status = 'uploading'
  task.error = ''
  startUpload(idx)
}

function removeTask(idx) {
  const task = queue[idx]
  task?.abortController?.abort()
  emit('upload-cancel', { file: task?.file, idx })
  queue.splice(idx, 1)
}

function clearCompleted() {
  for (let i = queue.length - 1; i >= 0; i--) {
    if (queue[i].status === 'completed' || queue[i].status === 'error') {
      removeTask(i)
    }
  }
}

// ========== Real Upload Logic ==========

function startUpload(idx) {
  const task = queue[idx]
  if (!task) return

  if (task.file.size < DIRECT_THRESHOLD) {
    doDirectUpload(idx)
  } else {
    doChunkUpload(idx)
  }
}

/** 小文件直接上传，通过 axios onUploadProgress 获取进度 */
async function doDirectUpload(idx) {
  const task = queue[idx]
  const controller = new AbortController()
  task.abortController = controller

  try {
    task.percent = 0
    task.uploaded = 0

    await apiUploadFile(
      task.file,
      props.parentId,
      (e) => {
        if (task.status !== 'uploading') return
        task.uploaded = e.loaded
        task.percent = Math.round((e.loaded / (e.total || task.file.size)) * 100)
        emit('upload-progress', { file: task.file, idx, task, percent: task.percent, uploaded: e.loaded })
      },
      controller.signal
    )

    task.percent = 100
    task.uploaded = task.file.size
    task.status = 'completed'
    task.abortController = null
    emit('upload-complete', { file: task.file, idx, task })
  } catch (e) {
    if (e?.code === 'ERR_CANCELED' || e?.name === 'CanceledError') {
      // paused, do nothing
      return
    }
    task.status = 'error'
    task.error = e?.message || '上传失败'
    task.abortController = null
    emit('upload-error', { file: task.file, idx, task, error: e })
  }
}

/** 大文件分片上传 */
async function doChunkUpload(idx) {
  const task = queue[idx]
  const controller = new AbortController()
  task.abortController = controller

  try {
    // 1. Init — 如果已有 uploadId 则复用（断点续传）
    if (!task.uploadId || task.totalChunks === 0) {
      task.totalChunks = Math.ceil(task.file.size / CHUNK_SIZE)
      const initRes = await initChunkUpload(
        task.file.name,
        task.file.size,
        task.totalChunks,
        props.parentId
      )
      task.uploadId = initRes.data.uploadId
    }

    // 2. 查询已上传分片（断点续传）
    let completedChunks = []
    try {
      const progressRes = await getChunkProgress(task.uploadId)
      completedChunks = progressRes.data.completedChunks || []
    } catch (_) { /* 首次上传或查询失败，从 0 开始 */ }

    const completedSet = new Set(completedChunks)

    // 3. 逐片上传
    for (let i = 0; i < task.totalChunks; i++) {
      if (task.status !== 'uploading') return

      // 跳过已上传分片
      if (completedSet.has(i)) {
        task.chunkIndex = i
        task.uploaded = Math.min((i + 1) * CHUNK_SIZE, task.file.size)
        task.percent = Math.round(((i + 1) / task.totalChunks) * 100)
        continue
      }

      const start = i * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, task.file.size)
      const chunkBlob = task.file.slice(start, end)

      await apiUploadChunk(task.uploadId, i, chunkBlob, controller.signal)

      task.chunkIndex = i
      task.uploaded = end
      task.percent = Math.round(((i + 1) / task.totalChunks) * 100)
      emit('upload-progress', { file: task.file, idx, task, percent: task.percent, uploaded: end, chunkIndex: i })
    }

    // 4. Merge
    await mergeChunks(task.uploadId)

    task.percent = 100
    task.uploaded = task.file.size
    task.status = 'completed'
    task.abortController = null
    emit('upload-complete', { file: task.file, idx, task })
  } catch (e) {
    if (e?.code === 'ERR_CANCELED' || e?.name === 'CanceledError') {
      return
    }
    task.status = 'error'
    task.error = e?.message || '分片上传失败'
    task.abortController = null
    emit('upload-error', { file: task.file, idx, task, error: e })
  }
}

// ========== Helpers ==========

function getFileIcon(file) {
  if (!file) return Document
  const type = file.type || ''
  if (type.startsWith('image/')) return PictureFilled
  if (type.startsWith('video/')) return VideoCameraFilled
  if (type.includes('pdf') || type.includes('document') || type.includes('text')) return Document
  const ext = (file.name || '').split('.').pop()?.toLowerCase()
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return Files
  return Document
}

function getProgressStatus(status) {
  if (status === 'completed') return 'success'
  if (status === 'error') return 'exception'
  return ''
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
.upload-zone-wrapper {
  width: 100%;
}

/* --- Drop Zone --- */
.upload-zone {
  border: 2px dashed var(--neutral-200);
  border-radius: var(--radius-lg);
  padding: var(--space-10) var(--space-6);
  text-align: center;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-smooth);
  background: var(--bg-surface);
  position: relative;
}
.upload-zone:hover {
  border-color: var(--primary-300);
  background: var(--primary-50);
  box-shadow: var(--shadow-sm);
}
.upload-zone.is-dragover {
  border-color: var(--primary-400);
  background: var(--primary-50);
  box-shadow: var(--shadow-md);
  transform: scale(1.01);
}
.upload-zone.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.upload-input-hidden {
  display: none;
}

.upload-zone-content {
  pointer-events: none;
}

.upload-icon-wrapper {
  margin-bottom: var(--space-4);
  color: var(--primary-400);
}
.upload-icon-svg {
  width: 56px;
  height: 56px;
}
.upload-zone.is-dragover .upload-icon-wrapper {
  color: var(--primary-500);
}

.upload-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--neutral-700);
  margin-bottom: var(--space-2);
}
.upload-hint {
  font-size: var(--fs-sm);
  color: var(--neutral-400);
}

/* --- Upload Queue --- */
.upload-queue {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.upload-task {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-200);
  box-shadow: var(--shadow-xs);
  transition: box-shadow var(--duration-fast);
}
.upload-task:hover {
  box-shadow: var(--shadow-sm);
}

.task-preview {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--neutral-100);
  display: flex;
  align-items: center;
  justify-content: center;
}
.task-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.task-file-icon {
  color: var(--primary-400);
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-info {
  flex: 1;
  min-width: 0;
}
.task-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--neutral-700);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.task-size {
  font-size: var(--fs-xs);
  color: var(--neutral-400);
  display: block;
  margin-top: 2px;
}
.task-progress-bar {
  margin-top: var(--space-1);
}
.task-status-text {
  font-size: var(--fs-xs);
  color: var(--neutral-500);
  margin-top: 2px;
  display: block;
}

.task-actions {
  display: flex;
  gap: var(--space-1);
  flex-shrink: 0;
}

/* --- Batch Actions --- */
.upload-batch-actions {
  margin-top: var(--space-3);
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .upload-zone {
    padding: var(--space-6) var(--space-4);
  }
  .upload-icon-svg {
    width: 42px;
    height: 42px;
  }
  .upload-title {
    font-size: var(--fs-base);
  }
  .upload-task {
    flex-wrap: wrap;
    gap: var(--space-2);
  }
  .task-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

