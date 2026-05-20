<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="logo-circle">
          <el-icon :size="28"><CloudFilled /></el-icon>
        </div>
        <h1 class="brand-name">CloudDisk</h1>
        <p class="brand-sub">Private Aggregator</p>
      </div>

      <h2 class="form-title">Welcome back</h2>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="Username" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="Password" :prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" class="login-btn">
            Sign In
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-footer">
        Don't have an account?&nbsp;<a href="#" @click.prevent="handleRegister">Register</a>
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

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }]
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
      ElMessage.success('Login successful')
      router.push('/home')
    }
  } catch (e) {
    ElMessage.error('Invalid username or password')
  } finally {
    loading.value = false
  }
}

function handleRegister() {
  ElMessage.info('Registration coming soon')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--neutral-50);
}

.login-card {
  width: 420px;
  padding: var(--space-12) var(--space-10);
  background: #fff;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--neutral-100);
}

.card-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.logo-circle {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: var(--primary-100);
  color: var(--primary-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-4);
}

.brand-name {
  font-size: var(--fs-2xl);
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: var(--space-1);
  letter-spacing: 0.03em;
}

.brand-sub {
  font-size: var(--fs-xs);
  color: var(--neutral-400);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.form-title {
  font-size: var(--fs-lg);
  font-weight: 500;
  color: var(--neutral-600);
  margin-bottom: var(--space-6);
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: var(--fs-base);
  letter-spacing: 0.02em;
}

.login-footer {
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--neutral-400);
  margin-top: var(--space-4);
}

.login-footer a {
  color: var(--primary-500);
  cursor: pointer;
  font-weight: 500;
}
.login-footer a:hover {
  color: var(--primary-600);
}
</style>