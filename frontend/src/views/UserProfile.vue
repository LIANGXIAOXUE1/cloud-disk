<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>

    <div class="profile-layout">
      <!-- Left: Avatar & Basic Info -->
      <el-card shadow="hover" class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerUpload">
            <el-avatar :size="96" :src="avatarUrl" class="profile-avatar">
              <el-icon :size="48"><UserFilled /></el-icon>
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon :size="22"><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            style="display:none"
            @change="handleAvatarChange"
          />
          <div class="avatar-hint">点击头像上传更换，支持 JPG / PNG，不超过 2MB</div>
        </div>

        <div class="info-section">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ registerDate || '—' }}</span>
          </div>
        </div>
      </el-card>

      <!-- Right: Edit Profile & Password -->
      <div class="profile-right">
        <!-- Edit Nickname -->
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header-title">
              <el-icon :size="18" color="var(--primary-500)"><EditPen /></el-icon>
              <span>编辑资料</span>
            </div>
          </template>
          <el-form label-width="80px" label-position="left" class="profile-form">
            <el-form-item label="昵称">
              <el-input
                v-model="nickname"
                placeholder="请输入你的昵称"
                maxlength="20"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">
                保存资料
              </el-button>
              <el-button @click="resetNickname">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Change Password -->
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header-title">
              <el-icon :size="18" color="var(--primary-500)"><Lock /></el-icon>
              <span>修改密码</span>
            </div>
          </template>
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            label-position="left"
            class="profile-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="6-20 位新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Switch Account & Logout -->
        <el-card shadow="hover" class="section-card actions-card">
          <template #header>
            <div class="card-header-title">
              <el-icon :size="18" color="var(--primary-500)"><Setting /></el-icon>
              <span>账号管理</span>
            </div>
          </template>
          <div class="actions-grid">
            <div class="action-item" @click="handleSwitchAccount">
              <div class="action-icon" style="background: var(--info-bg); color: var(--info);">
                <el-icon :size="24"><Switch /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-title">切换账号</div>
                <div class="action-desc">更换登录账号，返回登录页</div>
              </div>
              <el-icon :size="16" class="action-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-item action-item-danger" @click="handleLogout">
              <div class="action-icon" style="background: var(--error-bg); color: var(--error);">
                <el-icon :size="24"><SwitchButton /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-title">退出登录</div>
                <div class="action-desc">安全退出当前账号</div>
              </div>
              <el-icon :size="16" class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UserFilled, Camera, EditPen, Lock, Setting,
  Switch, SwitchButton, ArrowRight
} from '@element-plus/icons-vue'
import { changePassword, getUserInfo } from '@/api/modules/auth'

const router = useRouter()

const userId = 1
const userName = ref(localStorage.getItem('username') || 'Admin')
const registerDate = ref('')
const nickname = ref(localStorage.getItem('nickname') || userName.value)
const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')
const avatarInputRef = ref(null)
const saving = ref(false)

const passwordFormRef = ref(null)
const changingPwd = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
})

async function loadUserInfo() {
  try {
    const res = await getUserInfo(userId)
    if (res.data) {
      registerDate.value = res.data.createdAt || res.data.registerDate || ''
      if (res.data.nickname) {
        nickname.value = res.data.nickname
        localStorage.setItem('nickname', res.data.nickname)
      }
    }
  } catch (e) {
    // Use local storage fallback
    registerDate.value = localStorage.getItem('registerDate') || '—'
  }
}

function triggerUpload() {
  avatarInputRef.value?.click()
}

function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  const reader = new FileReader()
  reader.onload = (ev) => {
    avatarUrl.value = ev.target.result
    localStorage.setItem('avatarUrl', ev.target.result)
    ElMessage.success('头像已更新')
  }
  reader.readAsDataURL(file)

  // Reset input so same file can be re-selected
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

function resetNickname() {
  nickname.value = userName.value
}

async function handleSaveProfile() {
  saving.value = true
  try {
    localStorage.setItem('nickname', nickname.value)
    // Update username if changed
    if (nickname.value !== userName.value) {
      localStorage.setItem('username', nickname.value)
      userName.value = nickname.value
    }
    ElMessage.success('个人资料已保存')
  } catch (e) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPwd.value = true
  try {
    await changePassword({
      userId,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
  } catch (e) {
    ElMessage.error('密码修改失败，请检查当前密码是否正确')
  } finally {
    changingPwd.value = false
  }
}

function handleSwitchAccount() {
  ElMessageBox.confirm('切换账号将返回登录页，确定继续吗？', '切换账号', {
    type: 'info',
    confirmButtonText: '确定切换',
    cancelButtonText: '取消'
  }).then(() => {
    localStorage.removeItem('token')
    router.push('/login')
  }).catch(() => {})
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '确认退出', {
    type: 'warning',
    confirmButtonText: '确定退出',
    cancelButtonText: '取消'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.profile-page {
  max-width: 880px;
  margin: 0 auto;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

.profile-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: var(--space-6);
}

/* ============================================================
   Avatar Card (Left)
   ============================================================ */
.profile-card {
  height: fit-content;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-6) 0 var(--space-2);
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: transform var(--duration-fast) var(--ease-smooth);
}
.avatar-wrapper:hover {
  transform: scale(1.04);
}

.profile-avatar {
  border: 3px solid var(--primary-200);
  transition: border-color var(--duration-fast);
}
.avatar-wrapper:hover .profile-avatar {
  border-color: var(--primary-400);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity var(--duration-fast);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-hint {
  margin-top: var(--space-3);
  font-size: var(--fs-xs);
  color: var(--neutral-400);
  text-align: center;
}

.info-section {
  border-top: 1px solid var(--neutral-200);
  padding: var(--space-4) 0 0;
  margin-top: var(--space-4);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
}

.info-label {
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  font-weight: 500;
}

.info-value {
  font-size: var(--fs-sm);
  color: var(--neutral-700);
  font-weight: 500;
}

/* ============================================================
   Right Cards
   ============================================================ */
.profile-right {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.section-card {
  border: 1px solid var(--neutral-200);
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--neutral-800);
}

.profile-form {
  margin-top: var(--space-2);
}

.profile-form .el-form-item:last-child {
  margin-bottom: 0;
}

/* ============================================================
   Actions Card
   ============================================================ */
.actions-card {
  border: 1px solid var(--neutral-200);
}

.actions-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.action-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-smooth);
  border: 1px solid transparent;
}
.action-item:hover {
  background: var(--primary-50);
  border-color: var(--primary-200);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-text {
  flex: 1;
  min-width: 0;
}

.action-title {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--neutral-800);
}

.action-desc {
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-top: 2px;
}

.action-arrow {
  color: var(--neutral-300);
  flex-shrink: 0;
  transition: transform var(--duration-fast);
}
.action-item:hover .action-arrow {
  color: var(--primary-500);
  transform: translateX(3px);
}

.action-item-danger:hover {
  background: var(--error-bg);
  border-color: hsl(2, 22%, 85%);
}
.action-item-danger:hover .action-title {
  color: var(--error);
}

/* ============================================================
   Responsive
   ============================================================ */
@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .profile-card {
    order: -1;
  }

  .page-title {
    font-size: var(--fs-xl);
    margin-bottom: var(--space-4);
  }
}
</style>
