<template>
  <div class="login-page">
    <!-- Decorative background blobs -->
    <div class="bg-blob bg-blob-1"></div>
    <div class="bg-blob bg-blob-2"></div>
    <div class="bg-blob bg-blob-3"></div>

    <div class="login-card">
      <!-- Logo -->
      <div class="login-logo">
        <CloudLogo />
      </div>

      <p class="login-subtitle">私人聚合云盘</p>

      <!-- Form -->
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            class="login-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            native-type="submit"
            class="login-btn"
          >
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-footer">
        还没有账号？<a href="#" @click.prevent="handleRegister">立即注册</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/modules/auth'
import CloudLogo from '@/components/CloudLogo.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    if (res.code === 200) {
      localStorage.setItem('token', 'logged-in')
      localStorage.setItem('username', form.username)
      ElMessage.success('登录成功')
      router.push('/home')
    }
  } catch (e) {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}

function handleRegister() {
  ElMessage.info('注册功能即将开放')
}
</script>

<style scoped>
/* ============================================================
   Login Page — Bright Morandi Style
   ============================================================ */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f8f6 0%, #f0f5f3 30%, #fafaf9 60%, #f3f7f5 100%);
  position: relative;
  overflow: hidden;
  padding: var(--space-4);
}

/* Decorative blobs */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.25;
  pointer-events: none;
}
.bg-blob-1 {
  width: 320px;
  height: 320px;
  background: var(--primary-200);
  top: -80px;
  right: -60px;
}
.bg-blob-2 {
  width: 260px;
  height: 260px;
  background: var(--primary-100);
  bottom: -60px;
  left: -40px;
}
.bg-blob-3 {
  width: 180px;
  height: 180px;
  background: var(--primary-300);
  top: 50%;
  left: 60%;
  opacity: 0.15;
}

/* Card */
.login-card {
  width: 420px;
  max-width: 100%;
  padding: var(--space-12) var(--space-10);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(143, 185, 168, 0.15);
  position: relative;
  z-index: 1;
  animation: cardIn 0.5s var(--ease-out);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Logo area */
.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: var(--space-3);
}
.login-logo :deep(.logo-svg) {
  width: 56px;
  height: 56px;
}
.login-logo :deep(.logo-text) {
  font-size: var(--fs-3xl);
}

.login-subtitle {
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-bottom: var(--space-8);
  letter-spacing: 0.06em;
}

/* Form */
.login-form {
  margin-top: var(--space-2);
}
.login-form .el-form-item {
  margin-bottom: var(--space-5);
}
.login-form .el-form-item:last-of-type {
  margin-bottom: 0;
  margin-top: var(--space-2);
}

.login-input :deep(.el-input__wrapper) {
  padding: 4px 16px;
  height: 46px;
  background: var(--neutral-50) !important;
  border-radius: var(--radius-md) !important;
}

/* Button */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: var(--fs-base);
  font-weight: 600;
  letter-spacing: 0.1em;
  border-radius: var(--radius-md) !important;
  margin-top: var(--space-2);
}

/* Footer */
.login-footer {
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-top: var(--space-6);
}
.login-footer a {
  color: var(--primary-500);
  cursor: pointer;
  font-weight: 600;
  transition: color var(--duration-fast);
}
.login-footer a:hover {
  color: var(--primary-600);
}

/* Responsive */
@media (max-width: 480px) {
  .login-card {
    padding: var(--space-8) var(--space-6);
  }
}
</style>