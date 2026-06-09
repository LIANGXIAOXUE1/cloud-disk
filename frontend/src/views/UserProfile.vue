<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>

    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="profile-card">
          <template #header><span class="card-title">基本信息</span></template>
          <el-form :model="form" label-width="80px" label-position="top">
            <el-form-item label="用户名">
              <el-input :model-value="user.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="设置你的昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="你的邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="profile-card">
          <template #header><span class="card-title">修改密码</span></template>
          <el-form :model="pwForm" label-width="80px" label-position="top">
            <el-form-item label="旧密码">
              <el-input v-model="pwForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="changePw" :loading="pwSaving">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover" class="profile-card" style="margin-top:16px">
          <template #header><span class="card-title">其他</span></template>
          <el-button type="danger" @click="handleLogout">退出登录</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { useRouter } from "vue-router"
import request from "@/api/request"

const router = useRouter()
const user = ref({ username: "", nickname: "", email: "" })
const form = ref({ nickname: "", email: "" })
const pwForm = ref({ oldPassword: "", newPassword: "", confirmPassword: "" })
const saving = ref(false)
const pwSaving = ref(false)

onMounted(() => loadUser())

async function loadUser() {
  try {
    const res = await request({ url: "/auth/userInfo", method: "get" })
    const d = res.data
    user.value = d
    form.value.nickname = d.nickname || ""
    form.value.email = d.email || ""
  } catch (_) {}
}

async function saveProfile() {
  if (!form.value.nickname) { ElMessage.warning("昵称不能为空"); return }
  saving.value = true
  try {
    const res = await request({ url: "/auth/profile", method: "put", data: form.value })
    user.value = res.data
    localStorage.setItem("username", res.data.nickname)
    ElMessage.success("保存成功")
  } catch (e) { ElMessage.error("保存失败") }
  saving.value = false
}

async function changePw() {
  if (!pwForm.value.oldPassword) { ElMessage.warning("请输入旧密码"); return }
  if (!pwForm.value.newPassword) { ElMessage.warning("请输入新密码"); return }
  if (pwForm.value.newPassword !== pwForm.value.confirmPassword) { ElMessage.warning("两次密码不一致"); return }
  pwSaving.value = true
  try {
    await request({ url: "/auth/changePassword", method: "post", data: { oldPassword: pwForm.value.oldPassword, newPassword: pwForm.value.newPassword } })
    ElMessage.success("密码修改成功")
    pwForm.value = { oldPassword: "", newPassword: "", confirmPassword: "" }
  } catch (e) { ElMessage.error("密码修改失败") }
  pwSaving.value = false
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm("确定退出登录？", "退出", { type: "warning" })
    await request({ url: "/auth/logout", method: "post" })
    localStorage.removeItem("token")
    localStorage.removeItem("username")
    router.push("/login")
  } catch (_) {}
}
</script>

<style scoped>
.profile-page { max-width: 800px; }
.page-title { font-size: var(--fs-2xl); font-weight: 700; color: var(--neutral-800); margin-bottom: var(--space-6); }
.profile-card { border: 1px solid var(--neutral-200); margin-bottom: 0; }
.card-title { font-size: var(--fs-base); font-weight: 600; }
</style>