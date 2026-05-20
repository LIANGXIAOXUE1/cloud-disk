<template>
  <div class="share-page">
    <h1 class="page-title">Share Manager</h1>

    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">New Share</el-button>
      <el-input v-model="searchQuery" placeholder="Search shares..." :prefix-icon="Search" style="width: 260px" clearable />
    </div>

    <el-card shadow="hover">
      <el-table :data="shareList" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="shareId" label="Share ID" width="140" />
        <el-table-column prop="fileName" label="File" min-width="180">
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="16" color="var(--primary-500)"><Link /></el-icon>
              <span>{{ row.fileName || 'file-' + row.fileId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sharePassword" label="Password" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.sharePassword" type="warning" size="small">Protected</el-tag>
            <el-tag v-else type="success" size="small">Public</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Downloads" width="100">
          <template #default="{ row }">{{ row.downloadCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="Views" width="80">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small">
              {{ row.status === 1 ? 'Active' : row.status === 2 ? 'Expired' : 'Cancelled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created" width="180" />
        <el-table-column label="Actions" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="copyLink(row)">Copy</el-button>
            <el-button link type="danger" size="small" @click="handleCancel(row)">Cancel</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && shareList.length === 0" description="No shares" />
    </el-card>

    <!-- Create Share Dialog -->
    <el-dialog v-model="showCreateDialog" title="Create Share Link" width="440px">
      <el-form label-width="100px">
        <el-form-item label="File ID">
          <el-input v-model="createForm.fileId" placeholder="File ID" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="createForm.password" placeholder="Optional password" />
        </el-form-item>
        <el-form-item label="Expiry">
          <el-select v-model="createForm.expireType" style="width: 100%">
            <el-option :value="1" label="Permanent" />
            <el-option :value="2" label="7 Days" />
          </el-select>
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
import { Plus, Search, Link } from '@element-plus/icons-vue'
import { getShareList, createShare, cancelShare } from '@/api/modules/share'

const userId = 1

const shareList = ref([])
const loading = ref(false)
const searchQuery = ref('')
const showCreateDialog = ref(false)
const createForm = ref({ fileId: '', password: '', expireType: 1 })

onMounted(() => loadShares())

async function loadShares() {
  loading.value = true
  try {
    const res = await getShareList(userId)
    shareList.value = res.data?.list || []
  } catch (e) {
    shareList.value = []
  } finally {
    loading.value = false
  }
}

async function confirmCreate() {
  if (!createForm.value.fileId) return
  try {
    const res = await createShare(
      createForm.value.fileId, userId,
      createForm.value.password,
      createForm.value.expireType
    )
    ElMessage.success('Share created: ' + res.data)
    showCreateDialog.value = false
    createForm.value = { fileId: '', password: '', expireType: 1 }
    loadShares()
  } catch (e) {
    ElMessage.error('Failed to create share')
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('Cancel this share?', 'Confirm', { type: 'warning' })
    await cancelShare(row.shareId, userId)
    ElMessage.success('Share cancelled')
    loadShares()
  } catch (e) { /* cancelled */ }
}

function copyLink(row) {
  const link = window.location.origin + '/#/share/' + row.shareId
  navigator.clipboard.writeText(link).then(() => {
    ElMessage.success('Link copied')
  })
}
</script>

<style scoped>
.share-page {
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
  gap: var(--space-3);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-sm);
}
</style>