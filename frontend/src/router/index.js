import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { titleKey: 'nav.dashboard', icon: 'Odometer' }
      },
      {
        path: 'enterprises',
        name: 'Enterprises',
        component: () => import('@/views/Enterprises.vue'),
        meta: { titleKey: 'nav.enterprises', icon: 'OfficeBuilding' }
      },
      {
        path: 'receivables',
        name: 'Receivables',
        component: () => import('@/views/Receivables.vue'),
        meta: { titleKey: 'nav.receivables', icon: 'Document' }
      },
      {
        path: 'receivables/:id',
        name: 'ReceivableDetail',
        component: () => import('@/views/ReceivableDetail.vue'),
        meta: { titleKey: 'nav.tokenDetail', hidden: true }
      },
      {
        path: 'financing',
        name: 'Financing',
        component: () => import('@/views/Financing.vue'),
        meta: { titleKey: 'nav.financing', icon: 'Money' }
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('@/views/Transfer.vue'),
        meta: { titleKey: 'nav.transfer', icon: 'Switch' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth === false) {
    next()
  } else if (!userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
