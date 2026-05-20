<template>
  <div class="layout-container">
    <!-- Sidebar -->
    <aside class="layout-sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <el-icon :size="24"><CloudFilled /></el-icon>
          <span v-show="!isCollapsed" class="sidebar-title">CloudDisk</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :unique-opened="true"
        background-color="var(--neutral-800)"
        text-color="var(--neutral-300)"
        active-text-color="#fff"
        router
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path" v-if="!item.children">
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta.title }}</template>
          </el-menu-item>
          <el-sub-menu :index="item.path" v-else>
            <template #title>
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.meta.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>

      <div class="sidebar-collapse-btn" @click="isCollapsed = !isCollapsed">
        <el-icon :size="18">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </aside>

    <!-- Main -->
    <div class="layout-main">
      <!-- Header -->
      <header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">Home</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute">{{ currentRoute }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="user-name">{{ userName }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon><User /></el-icon> Profile
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> Logout
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Content -->
      <main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)

const userName = ref(localStorage.getItem('username') || 'Admin')

const menuItems = computed(() => {
  const routes = router.options.routes.find(r => r.path === '/')
  return routes ? routes.children.filter(r => !r.meta?.hidden) : []
})

const activeMenu = computed(() => route.path)

const currentRoute = computed(() => route.meta?.title || '')

function handleLogout() {
  ElMessageBox.confirm('Are you sure you want to logout?', 'Confirm', {
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* === Sidebar === */
.layout-sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  background: var(--neutral-800);
  display: flex;
  flex-direction: column;
  transition: width var(--duration-normal) var(--ease-smooth);
  overflow: hidden;
  position: relative;
}
.layout-sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
  min-width: var(--sidebar-collapsed-width);
}

.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 var(--space-4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--neutral-200);
  white-space: nowrap;
}
.sidebar-title {
  font-size: var(--fs-base);
  font-weight: 500;
  letter-spacing: 0.03em;
}

.sidebar-collapse-btn {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--neutral-400);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  transition: color var(--duration-fast);
}
.sidebar-collapse-btn:hover {
  color: var(--neutral-200);
}

:deep(.el-menu) {
  border-right: none !important;
  flex: 1;
  overflow-y: auto;
  padding-bottom: 52px;
}
:deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 2px 8px;
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-smooth);
}
:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}
:deep(.el-menu-item.is-active) {
  background: var(--primary-600) !important;
  color: #fff !important;
}
:deep(.el-sub-menu .el-menu-item) {
  padding-left: 56px !important;
}

/* === Main Area === */
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* === Header === */
.layout-header {
  height: var(--header-height);
  min-height: var(--header-height);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--neutral-100);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-md);
  transition: background var(--duration-fast);
}
.user-info:hover {
  background: var(--neutral-50);
}
.user-name {
  font-size: var(--fs-sm);
  color: var(--neutral-600);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-arrow {
  font-size: var(--fs-xs);
  color: var(--neutral-400);
}

/* === Content === */
.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  background: var(--neutral-50);
}
</style>