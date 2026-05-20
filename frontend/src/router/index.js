import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: 'Login', noLayout: true }
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
        meta: { title: 'Home', icon: 'HomeFilled' }
      },
      {
        path: 'files',
        name: 'FileList',
        component: () => import('@/views/FileList.vue'),
        meta: { title: 'File Manager', icon: 'FolderOpened' }
      },
      {
        path: 'share',
        name: 'Share',
        component: () => import('@/views/Share.vue'),
        meta: { title: 'Share Manager', icon: 'Share' }
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('@/views/Transfer.vue'),
        meta: { title: 'Transfer Tasks', icon: 'Connection' }
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/Recycle.vue'),
        meta: { title: 'Recycle Bin', icon: 'Delete' }
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
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router