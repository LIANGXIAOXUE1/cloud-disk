<template>
  <div class="register-page">
    <!-- Decorative background blobs -->
    <div class="bg-blob bg-blob-1"></div>
    <div class="bg-blob bg-blob-2"></div>
    <div class="bg-blob bg-blob-3"></div>

    <div class="register-card">
      <!-- Logo -->
      <div class="register-logo">
        <CloudLogo />
      </div>

      <p class="register-subtitle">创建你的云盘账号</p>

      <!-- Form -->
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister" class="register-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（6-32位）"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称（选填）"
            :prefix-icon="UserFilled"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            native-type="submit"
            class="register-btn"
          >
            {{ loading ? '注册中…' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="register-footer">
        已有账号？<a href="#" @click.prevent="goLogin">立即登录</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { register } from '@/api/modules/auth'
import CloudLogo from '@/components/CloudLogo.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

/**
 * 自定义校验：两次密码一致
 */
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度需在2-20位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6-32位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误信息由 Axios 响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
/* ============================================================
   Register Page — Bright Morandi Style
   ============================================================ */
.register-page {
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
.register-card {
  width: 420px;
  max-width: 100%;
  padding: var(--space-10) var(--space-10);
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
.register-logo {
  display: flex;
  justify-content: center;
  margin-bottom: var(--space-3);
}
.register-logo :deep(.logo-svg) {
  width: 56px;
  height: 56px;
}

.register-subtitle {
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-bottom: var(--space-6);
  letter-spacing: 0.06em;
}

/* Form */
.register-form {
  margin-top: var(--space-2);
}
.register-form .el-form-item {
  margin-bottom: var(--space-4);
}
.register-form .el-form-item:last-of-type {
  margin-bottom: 0;
  margin-top: var(--space-2);
}

.register-form :deep(.el-input__wrapper) {
  padding: 4px 16px;
  height: 46px;
  background: var(--neutral-50) !important;
  border-radius: var(--radius-md) !important;
}

/* Button */
.register-btn {
  width: 100%;
  height: 48px;
  font-size: var(--fs-base);
  font-weight: 600;
  letter-spacing: 0.1em;
  border-radius: var(--radius-md) !important;
  margin-top: var(--space-2);
}

/* Footer */
.register-footer {
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-top: var(--space-6);
}
.register-footer a {
  color: var(--primary-500);
  cursor: pointer;
  font-weight: 600;
  transition: color var(--duration-fast);
}
.register-footer a:hover {
  color: var(--primary-600);
}

/* Responsive */
@media (max-width: 480px) {
  .register-card {
    padding: var(--space-8) var(--space-6);
  }
}
</style>
