import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/index.vue'

Vue.use(VueRouter)

/** 静态路由：登录、注册、首页 */
export const constantRoutes = [
  { path: '/login', component: () => import('@/views/auth/login/index.vue'), hidden: true },
  { path: '/register', component: () => import('@/views/auth/register/index.vue'), hidden: true },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'el-icon-s-home', affix: true } }
    ]
  }
]

const createRouter = () => new VueRouter({
  mode: 'history',
  base: '/',
  routes: constantRoutes
})

const router = createRouter()

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
