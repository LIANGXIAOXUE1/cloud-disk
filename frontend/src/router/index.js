import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: 'Login', noLayout: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: 'Register', noLayout: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '控制台', icon: 'HomeFilled' }
      },
      {
        path: 'files',
        name: 'FileList',
        component: () => import('@/views/FileList.vue'),
        meta: { title: '文件管理', icon: 'FolderOpened' }
      },
      {
        path: 'share',
        name: 'Share',
        component: () => import('@/views/Share.vue'),
        meta: { title: '分享管理', icon: 'Share' }
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('@/views/Transfer.vue'),
        meta: { title: '转存任务', icon: 'Connection' }
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/Recycle.vue'),
        meta: { title: '回收站', icon: 'Delete' }
      },
      {
        path: 'storage',
        name: 'Storage',
        component: () => import('@/views/Storage.vue'),
        meta: { title: '存储管理', icon: 'Coin' }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/UserProfile.vue'),
        meta: { title: '个人中心', icon: 'User', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - Cloud Disk' : 'Cloud Disk'
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
