<template>
  <div class="layout-container">
    <!-- ========== Desktop / Tablet Sidebar ========== -->
    <aside v-if="!isMobile" class="layout-sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <CloudLogo :collapsed="isCollapsed" />
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :unique-opened="true"
        router
        class="sidebar-menu"
      >
        <template v-for="item in visibleMenuItems" :key="item.path">
          <el-menu-item :index="'/' + item.path" v-if="!item.children">
            <el-icon :size="20"><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta.title }}</template>
          </el-menu-item>
          <el-sub-menu :index="item.path" v-else>
            <template #title>
              <el-icon :size="20"><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="'/' + child.path">
              {{ child.meta.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>

      <div class="sidebar-collapse-btn" @click="toggleSidebar">
        <el-icon :size="18">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </aside>

    <!-- ========== Mobile Drawer ========== -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      :size="260"
      :with-header="false"
      :z-index="400"
      class="mobile-drawer"
    >
      <div class="mobile-drawer-header">
        <CloudLogo />
        <el-button :icon="Close" circle size="small" @click="mobileDrawerVisible = false" />
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="mobile-drawer-menu"
        @select="mobileDrawerVisible = false"
      >
        <template v-for="item in visibleMenuItems" :key="item.path">
          <el-menu-item :index="'/' + item.path" v-if="!item.children">
            <el-icon :size="20"><component :is="item.meta.icon" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
          <el-sub-menu :index="item.path" v-else>
            <template #title>
              <el-icon :size="20"><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="'/' + child.path">
              {{ child.meta.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-drawer>

    <!-- ========== Main Area ========== -->
    <div class="layout-main">
      <!-- Header -->
      <header class="layout-header">
        <div class="header-left">
          <button v-if="isMobile" class="mobile-menu-btn" @click="mobileDrawerVisible = true">
            <el-icon :size="22"><Expand /></el-icon>
          </button>
          <CloudLogo v-if="isMobile" :collapsed="false" class="header-logo-mobile" />
          <el-breadcrumb v-if="!isMobile" separator="/" class="header-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/home' }">
              <el-icon :size="14"><HomeFilled /></el-icon>
              <span style="margin-left:4px">首页</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute && currentRoute !== 'Home'">{{ currentRoute }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- Profile link replacing dropdown -->
          <router-link to="/user/profile" class="user-profile-link">
            <el-avatar :size="34" icon="UserFilled" style="background:var(--primary-200);color:var(--primary-600)" />
            <span v-if="!isMobile" class="user-name">{{ userName }}</span>
          </router-link>
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

      <!-- Mobile Bottom Tab Bar -->
      <nav v-if="isMobile" class="mobile-bottom-bar">
        <div
          v-for="item in bottomBarItems"
          :key="item.path"
          class="bottom-bar-item"
          :class="{ active: activeMenu === '/' + item.path }"
          @click="router.push('/' + item.path)"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span class="bottom-bar-label">{{ item.label }}</span>
        </div>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, FolderOpened, Share, Connection, Delete,
  User, Coin, Fold, Expand, Close
} from '@element-plus/icons-vue'
import CloudLogo from '@/components/CloudLogo.vue'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)
const isMobile = ref(false)
const mobileDrawerVisible = ref(false)

const userName = ref(localStorage.getItem('username') || 'Admin')

const menuItems = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  return mainRoute ? mainRoute.children.filter(r => !r.meta?.hidden) : []
})

// Show storage in sidebar too (not hidden)
const visibleMenuItems = computed(() => {
  return menuItems.value
})

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.meta?.title || '')

const bottomBarItems = computed(() => {
  // Show home, files, storage + profile in bottom bar (limited to 5)
  const items = menuItems.value.slice(0, 4).map(r => ({
    path: r.path,
    label: r.meta?.title || r.path,
    icon: r.meta?.icon || 'Menu'
  }))
  // Add profile at the end
  items.push({
    path: 'user/profile',
    label: '我的',
    icon: 'User'
  })
  return items.slice(0, 5)
})

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

/* ============================================================
   Sidebar — Warm Morandi
   ============================================================ */
.layout-sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  background: var(--bg-elevated);
  border-right: 1px solid var(--neutral-200);
  display: flex;
  flex-direction: column;
  transition: width var(--duration-normal) var(--ease-smooth);
  overflow: hidden;
  position: relative;
  z-index: var(--z-sticky);
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
  border-bottom: 1px solid var(--neutral-200);
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) 0 52px;
}

:deep(.sidebar-menu .el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: var(--radius-md) !important;
  font-size: var(--fs-sm);
  color: var(--neutral-600);
  font-weight: 500;
  transition: all var(--duration-fast) var(--ease-smooth);
}
:deep(.sidebar-menu .el-menu-item:hover) {
  background: var(--primary-50) !important;
  color: var(--primary-600) !important;
}
:deep(.sidebar-menu .el-menu-item.is-active) {
  background: var(--primary-50) !important;
  color: var(--primary-600) !important;
  font-weight: 600 !important;
}
:deep(.sidebar-menu .el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: var(--primary-500);
  border-radius: 0 3px 3px 0;
}
:deep(.sidebar-menu .el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: var(--radius-md);
  color: var(--neutral-600);
  font-size: var(--fs-sm);
  font-weight: 500;
}
:deep(.sidebar-menu .el-sub-menu__title:hover) {
  background: var(--primary-50);
  color: var(--primary-600);
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
  background: var(--bg-elevated);
  border-top: 1px solid var(--neutral-200);
  transition: all var(--duration-fast) var(--ease-smooth);
}
.sidebar-collapse-btn:hover {
  color: var(--primary-500);
  background: var(--primary-50);
}

/* ============================================================
   Mobile Drawer
   ============================================================ */
:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
}
.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--neutral-200);
}
.mobile-drawer-menu {
  padding: var(--space-2) 0;
}
:deep(.mobile-drawer-menu .el-menu-item) {
  height: 46px;
  line-height: 46px;
  font-size: var(--fs-base);
  color: var(--neutral-600);
  font-weight: 500;
}
:deep(.mobile-drawer-menu .el-menu-item:hover) {
  background: var(--primary-50);
}
:deep(.mobile-drawer-menu .el-menu-item.is-active) {
  background: var(--primary-50);
  color: var(--primary-600);
  font-weight: 600;
}

/* ============================================================
   Main Area
   ============================================================ */
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  background: var(--bg-page);
}

/* ============================================================
   Header
   ============================================================ */
.layout-header {
  height: var(--header-height);
  min-height: var(--header-height);
  background: var(--bg-elevated);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--neutral-200);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-5);
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  flex: 1;
}

.mobile-menu-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--neutral-600);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--duration-fast);
  flex-shrink: 0;
}
.mobile-menu-btn:hover {
  background: var(--neutral-100);
}

.header-logo-mobile {
  flex-shrink: 0;
}

.header-breadcrumb {
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* Profile link — replaces dropdown */
.user-profile-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  transition: background var(--duration-fast);
  text-decoration: none;
  color: inherit;
}
.user-profile-link:hover {
  background: var(--primary-50);
}
.user-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--neutral-700);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================================================
   Content
   ============================================================ */
.layout-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-6);
}

/* ============================================================
   Mobile Bottom Tab Bar
   ============================================================ */
.mobile-bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: var(--mobile-bottom-bar-height);
  min-height: var(--mobile-bottom-bar-height);
  background: var(--bg-elevated);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid var(--neutral-200);
  padding: 0 var(--space-2);
  padding-bottom: env(safe-area-inset-bottom, 0);
  z-index: var(--z-sticky);
  flex-shrink: 0;
}

.bottom-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--neutral-400);
  transition: all var(--duration-fast) var(--ease-smooth);
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}
.bottom-bar-item:hover,
.bottom-bar-item:active {
  color: var(--primary-500);
}
.bottom-bar-item.active {
  color: var(--primary-500);
}
.bottom-bar-label {
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
}

/* ============================================================
   Responsive
   ============================================================ */
@media (max-width: 768px) {
  .layout-content {
    padding: var(--space-4) var(--space-3);
    padding-bottom: calc(var(--space-4) + var(--mobile-bottom-bar-height));
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .layout-sidebar:not(.collapsed) {
    width: 180px;
    min-width: 180px;
  }
}
</style>
