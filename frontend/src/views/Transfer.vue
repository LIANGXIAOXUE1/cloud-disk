<template>
  <div class="transfer-page">
    <h1 class="page-title">转存任务</h1>

    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">新建任务</el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="taskList" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="Task No" width="140" />
        <el-table-column prop="sourceUrl" label="Source URL" min-width="200">
          <template #default="{ row }">
            <el-link :href="row.sourceUrl" target="_blank" type="primary" :underline="false" class="url-cell">
              {{ row.sourceUrl }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="targetPath" label="目标路径" width="140" />
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="getProgressStatus(row.taskStatus)" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.taskStatus)" size="small">{{ getStatusText(row.taskStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.taskStatus === 4" link type="primary" size="small" @click="handleRetry(row)">重试</el-button>
            <el-button v-if="row.taskStatus === 1" link type="danger" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && taskList.length === 0" description="No transfer tasks" />
    </el-card>

    <!-- Create Task Dialog -->
    <el-dialog v-model="showCreateDialog" title="New Transfer Task" width="480px">
      <el-form label-width="120px">
        <el-form-item label="Source URL">
          <el-input v-model="createForm.sourceUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="Extract Code">
          <el-input v-model="createForm.extractCode" placeholder="Optional" />
        </el-form-item>
        <el-form-item label="Target Path">
          <el-input v-model="createForm.targetPath" placeholder="/downloads" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" @click="confirmCreate">Create</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTransferList, createTransferTask, cancelTransferTask, retryTransferTask } from '@/api/modules/transfer'

const userId = 1

const taskList = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const createForm = ref({ sourceUrl: '', extractCode: '', targetPath: '/' })

onMounted(() => loadTasks())

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTransferList(userId)
    taskList.value = res.data?.list || []
  } catch (e) {
    taskList.value = []
  } finally {
    loading.value = false
  }
}

async function confirmCreate() {
  if (!createForm.value.sourceUrl) return
  try {
    await createTransferTask(userId, createForm.value.sourceUrl, createForm.value.extractCode, createForm.value.targetPath)
    ElMessage.success('Task created')
    showCreateDialog.value = false
    createForm.value = { sourceUrl: '', extractCode: '', targetPath: '/' }
    loadTasks()
  } catch (e) {
    ElMessage.error('Failed to create task')
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('Cancel this task?', 'Confirm', { type: 'warning' })
    await cancelTransferTask(row.taskNo, userId)
    ElMessage.success('Task cancelled')
    loadTasks()
  } catch (e) { /* cancelled */ }
}

async function handleRetry(row) {
  try {
    await retryTransferTask(row.taskNo, userId)
    ElMessage.success('Task retrying')
    loadTasks()
  } catch (e) {
    ElMessage.error('Failed to retry')
  }
}

function getStatusText(status) {
  const map = { 1: 'Running', 2: 'Done', 3: 'Cancelled', 4: 'Failed' }
  return map[status] || 'Unknown'
}

function getStatusType(status) {
  const map = { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

function getProgressStatus(taskStatus) {
  if (taskStatus === 2) return 'success'
  if (taskStatus === 4) return 'exception'
  return ''
}
</script>

<style scoped>
.transfer-page {
  max-width: 1200px;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

.toolbar {
  margin-bottom: var(--space-4);
}

.url-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
</style>