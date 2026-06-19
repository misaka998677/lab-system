import router from './index'
import store from '../store'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '../utils/auth'
import { defaultRouteForRole } from '../utils/role-map'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const token = getToken()

  if (token) {
    if (to.path === '/login' || to.path === '/register') {
      next()
      NProgress.done()
      return
    }

    const needUser  = !store.state.user.user?.id
    const needRoutes = !store.state.permission.dynamicRoutes?.length

    if (needUser || needRoutes) {
      try {
        const promises = []
        if (needUser)  promises.push(store.dispatch('user/fetchInfo'))
        if (needRoutes) promises.push(store.dispatch('permission/generateRoutes'))
        await Promise.all(promises)
        if (needRoutes) {
          store.state.permission.dynamicRoutes.forEach(route => router.addRoute(route))
        }
        next({ ...to, replace: true })
      } catch (e) {
        await store.dispatch('user/logout')
        next(`/login?redirect=${to.path}`)
      }
    } else {
      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})

router.afterEach(() => NProgress.done())
