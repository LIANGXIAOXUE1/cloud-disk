<template>
  <div class="storage-page">
    <div class="page-header">
      <h1 class="page-title">存储管理</h1>
      <el-button type="primary" @click="openAdd">+ 添加存储空间</el-button>
    </div>

    <!-- Binding List -->
    <el-card shadow="hover" class="list-card" v-loading="loading">
      <el-empty v-if="!loading && bindings.length === 0" description="暂无存储空间，点击上方按钮添加" />
      <div v-else class="binding-list">
        <div v-for="b in bindings" :key="b.id" class="binding-item">
          <div class="binding-left">
            <span class="binding-icon">{{ typeIcon(b.type) }}</span>
            <div class="binding-info">
              <div class="binding-name">
                {{ b.name }}
                <el-tag v-if="b.isDefault" size="small" type="success" effect="plain">当前默认</el-tag>
              </div>
              <div class="binding-root">{{ b.rootInfo }}</div>
            </div>
          </div>
          <div class="binding-right">
            <el-button v-if="!b.isDefault" size="small" @click="handleDelete(b)">删除</el-button>
            <el-button v-else size="small" disabled>默认存储</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog v-model="dialogVisible" :title="'添加存储空间'" width="540px" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="top">
        <el-form-item label="存储类型">
          <el-select v-model="form.type" style="width:100%" @change="onTypeChange">
            <el-option v-for="t in types" :key="t.type" :label="t.name + (t.available ? '' : ' (即将支持)')" :value="t.type" :disabled="!t.available" />
          </el-select>
          <div class="form-hint">{{ selectedType?.description }}</div>
        </el-form-item>

        <!-- LOCAL fields -->
        <template v-if="form.type === 'LOCAL'">
          <el-form-item label="名称"><el-input v-model="form.name" placeholder="例如：我的本地硬盘" /></el-form-item>
          <el-form-item label="根路径"><el-input v-model="form.rootPath" placeholder="D:\\cloud-disk-files" /></el-form-item>
        </template>

        <!-- MINIO fields -->
        <template v-if="form.type === 'MINIO'">
          <el-form-item label="名称"><el-input v-model="form.name" placeholder="例如：我的 MinIO" /></el-form-item>
          <el-form-item label="API 地址"><el-input v-model="form.endpoint" placeholder="http://localhost:9000" /></el-form-item>
          <el-form-item label="Access Key"><el-input v-model="form.accessKey" placeholder="admin" /></el-form-item>
          <el-form-item label="Secret Key"><el-input v-model="form.secretKey" type="password" placeholder="密钥" show-password /></el-form-item>
          <el-form-item label="Bucket 名称"><el-input v-model="form.bucket" placeholder="cloud-disk" /></el-form-item>
        </template>

        <!-- OSS fields -->
        <template v-if="form.type === 'ALIYUN_OSS'">
          <el-form-item label="名称"><el-input v-model="form.name" placeholder="例如：我的阿里云OSS" /></el-form-item>
          <el-form-item label="Endpoint"><el-input v-model="form.endpoint" placeholder="oss-cn-hangzhou.aliyuncs.com" /></el-form-item>
          <el-form-item label="Access Key"><el-input v-model="form.accessKey" placeholder="LTAI5t..." /></el-form-item>
          <el-form-item label="Secret Key"><el-input v-model="form.secretKey" type="password" placeholder="密钥" show-password /></el-form-item>
          <el-form-item label="Bucket 名称"><el-input v-model="form.bucket" placeholder="my-cloud-disk" /></el-form-item>
        </template>

        <el-alert v-if="testResult" :title="testResult" :type="testOk ? 'success' : 'error'" :closable="false" show-icon style="margin-bottom:16px" />
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="testConnection" :loading="testing">测试连接</el-button>
        <el-button type="primary" @click="saveBinding" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Tutorial -->
    <el-card shadow="hover" class="tutorial-card">
      <template #header><span style="font-weight:600">使用说明</span></template>
      <div class="tutorial">
        <div class="tutorial-item">
          <h4>💻 本地磁盘</h4>
          <p>选择服务器上的一个文件夹作为存储空间。无需任何配置，填路径即可使用。</p>
        </div>
        <div class="tutorial-item">
          <h4>🪣 MinIO</h4>
          <p>需要自建 MinIO 服务器。如果你的电脑装了 Docker Desktop，执行以下命令一键启动：</p>
          <pre class="tutorial-code">docker run -d --name minio -p 9000:9000 -p 9001:9001 -e MINIO_ROOT_USER=admin -e MINIO_ROOT_PASSWORD=admin123456 minio/minio server /data --console-address ":9001"</pre>
          <p>启动后访问 <code>http://localhost:9001</code> 进入管理后台（账号 admin / admin123456），创建一个 Bucket，然后在本页面连接。</p>
        </div>
        <div class="tutorial-item">
          <h4>☁️ 阿里云 OSS</h4>
          <p>需注册<a href="https://www.aliyun.com/product/oss" target="_blank">阿里云</a>并开通 OSS 服务，获取 AccessKey 后在本页面连接。</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const bindings = ref([])
const types = ref([])
const dialogVisible = ref(false)
const testing = ref(false)
const saving = ref(false)
const testResult = ref('')
const testOk = ref(false)

const form = ref({ type: 'LOCAL', name: '', rootPath: '', endpoint: '', accessKey: '', secretKey: '', bucket: '' })

const selectedType = computed(() => types.value.find(t => t.type === form.value.type))

const typeIcon = (type) => ({ LOCAL: '💻', MINIO: '🪣', ALIYUN_OSS: '☁️' }[type] || '📦')

onMounted(() => { loadTypes(); loadBindings() })

async function loadTypes() {
  try { const res = await request({ url: '/storage/types', method: 'get' }); types.value = res.data || [] } catch (_) {}
}

async function loadBindings() {
  loading.value = true
  try { const res = await request({ url: '/storage/bindings', method: 'get' }); bindings.value = res.data || [] } catch (_) { bindings.value = [] }
  loading.value = false
}

function openAdd() {
  form.value = { type: 'LOCAL', name: '', rootPath: '', endpoint: '', accessKey: '', secretKey: '', bucket: '' }
  testResult.value = ''
  testOk.value = false
  dialogVisible.value = true
}

function onTypeChange() {
  testResult.value = ''
  testOk.value = false
}

function buildConfig() {
  const config = { name: form.value.name || (selectedType.value?.name || '') }
  if (form.value.type === 'LOCAL') config.rootPath = form.value.rootPath || 'D:\\cloud-disk-files'
  if (form.value.type === 'MINIO' || form.value.type === 'ALIYUN_OSS') {
    config.endpoint = form.value.endpoint || ''
    config.accessKey = form.value.accessKey || ''
    config.secretKey = form.value.secretKey || ''
    config.bucket = form.value.bucket || ''
  }
  return config
}

async function testConnection() {
  testing.value = true
  testResult.value = ''
  try {
    const res = await request({ url: '/storage/test', method: 'post', data: { type: form.value.type, config: buildConfig() } })
    testOk.value = true
    testResult.value = '连接成功！'
  } catch (e) {
    testOk.value = false
    testResult.value = '连接失败：' + (e?.message || '请检查配置')
  }
  testing.value = false
}

async function saveBinding() {
  saving.value = true
  try {
    await request({ url: '/storage/binding', method: 'post', data: { type: form.value.type, config: buildConfig() } })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadBindings()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || ''))
  }
  saving.value = false
}

async function handleDelete(b) {
  try {
    await ElMessageBox.confirm(`确定删除「${b.name}」？`, '确认', { type: 'warning' })
    await request({ url: `/storage/binding/${b.id}`, method: 'delete' })
    ElMessage.success('已删除')
    loadBindings()
  } catch (_) {}
}
</script>

<style scoped>
.storage-page { max-width: 900px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 1.5rem; font-weight: 700; color: var(--neutral-800); }
.list-card { margin-bottom: 20px; }
.binding-list { display: flex; flex-direction: column; gap: 0; }
.binding-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid var(--neutral-100); }
.binding-item:last-child { border-bottom: none; }
.binding-left { display: flex; align-items: center; gap: 12px; }
.binding-icon { font-size: 24px; }
.binding-name { font-size: 14px; font-weight: 600; color: var(--neutral-800); display: flex; align-items: center; gap: 8px; }
.binding-root { font-size: 12px; color: var(--neutral-400); margin-top: 2px; }
.form-hint { font-size: 12px; color: var(--neutral-400); margin-top: 4px; }
.tutorial { display: flex; flex-direction: column; gap: 20px; }
.tutorial-item h4 { font-size: 14px; margin-bottom: 6px; }
.tutorial-item p { font-size: 13px; color: var(--neutral-500); line-height: 1.6; }
.tutorial-code { background: #1e1e1e; color: #d4d4d4; padding: 12px 16px; border-radius: 6px; font-size: 12px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
.tutorial-card { margin-top: 0; }
@media (max-width: 768px) { .page-header { flex-direction: column; align-items: flex-start; gap: 12px; } }
</style>
