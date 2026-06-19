import { getMenus } from '@/api/auth'
import Layout from '@/layout/index.vue'
import { resetRouter } from '@/router'

/** 把 import.meta-style 的组件路径映射成动态 import */
const componentMap = (component) => {
  if (!component) return null
  if (component === 'Layout') return Layout
  return () => import(`@/views/${component}.vue`)
}

const normalizeRootPath = (path = '') => {
  if (!path) return '/'
  return path.startsWith('/') ? path : '/' + path
}

const buildRoutes = (menus = []) => menus.map(m => {
  const menuPath = m.path || ''
  const isRootMenu = m.parentId === 0
  const routePath = isRootMenu ? normalizeRootPath(menuPath) : menuPath

  const route = {
    path: routePath,
    name: m.name,
    meta: { title: m.name, icon: m.icon, perm: m.perm },
    component: isRootMenu ? Layout : componentMap(m.component),
    parentId: m.parentId
  }
  if (m.children && m.children.length) {
    route.children = buildRoutes(m.children)
    route.redirect = routePath + '/' + (route.children[0] ? route.children[0].path : '').replace(/^\//, '')
  } else if (isRootMenu) {
    route.children = [{
      path: '',
      name: m.name + 'Index',
      component: componentMap(m.component),
      meta: { title: m.name, icon: m.icon }
    }]
  }
  return route
})

const joinPath = (base = '', path = '') => {
  if (!path) return base || '/'
  if (path.startsWith('/')) return path
  if (!base || base === '/') return '/' + path
  return (base.endsWith('/') ? base : base + '/') + path
}

const collectAccessiblePaths = (routes = [], base = '', paths = new Set()) => {
  routes.forEach(route => {
    if (!route || route.path === '*' || route.hidden) return
    const fullPath = joinPath(base, route.path)
    paths.add(fullPath)
    if (route.children && route.children.length) {
      collectAccessiblePaths(route.children, fullPath, paths)
    }
  })
  return Array.from(paths)
}

const state  = { routes: [], dynamicRoutes: [], accessiblePaths: [] }
const mutations = {
  SET_ROUTES: (s, r) => {
    s.dynamicRoutes = r
    s.routes = r
    s.accessiblePaths = collectAccessiblePaths(r)
  },
  CLEAR_ROUTES: (s) => {
    s.dynamicRoutes = []
    s.routes = []
    s.accessiblePaths = []
  }
}
const actions = {
  generateRoutes({ commit }) {
    return getMenus().then(res => {
      const routes = buildRoutes(res.data || [])
      routes.push({ path: '*', component: () => import('@/views/error/404.vue'), hidden: true })
      commit('SET_ROUTES', routes)
      return routes
    })
  },
  clearRoutes({ commit }) {
    commit('CLEAR_ROUTES')
    resetRouter()
  }
}

export default { namespaced: true, state, mutations, actions }
