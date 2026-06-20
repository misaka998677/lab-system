import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import { getToken, removeToken } from './auth'
import router from '../router'

/** 按请求路径维护飞行中的 CancelToken。用于：
 *   - 路由切换时统一取消全部飞行请求
 *   - 同一 URL 有新请求时取消旧请求，避免“搜索结果错序”
 */
const pendingTokens = new Map()

function makeKey(config) {
  const params = config.params
    ? JSON.stringify(config.params, (k, v) => (typeof v === 'object' ? v : String(v)))
    : ''
  return [config.method, config.url, params].join('|')
}

function addPending(config) {
  const key = makeKey(config)
  if (pendingTokens.has(key)) {
    const old = pendingTokens.get(key)
    old.cancel('DUPLICATE_REQUEST_CANCELLED')
  }
  const source = axios.CancelToken.source()
  config.cancelToken = source.token
  pendingTokens.set(key, source)
}

function removePending(config) {
  const key = makeKey(config)
  pendingTokens.delete(key)
}

/** 供路由守卫调用：取消全部飞行请求。 */
export function cancelAllPending() {
  pendingTokens.forEach(s => s.cancel('ROUTE_CHANGE_CANCELLED'))
  pendingTokens.clear()
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 15000
})

/** 从 JWT token 中解析 userId（payload base64 decode，不验证签名） */
function getUserIdFromToken(token) {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.sub || null
  } catch (e) { return null }
}

service.interceptors.request.use(cfg => {
  const t = getToken()
  if (t) cfg.headers['Authorization'] = 'Bearer ' + t
  // 防重放唯一请求ID：时间戳 + 随机数，确保60秒窗口内几乎不可能重复
  cfg.headers['X-Request-Id'] = Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
  // 携带当前用户ID，供后端 ReplayGuardInterceptor 关联登录状态
  const uid = getUserIdFromToken(t)
  if (uid) cfg.headers['X-User-Id'] = uid
  addPending(cfg)
  return cfg
})

const CODE_MESSAGE = new Map([
  [403, '无权限'],
  [401, '登录已过期，请重新登录'],
  [500, '服务器内部错误'],
  [404, '请求的资源不存在']
])

const DEFAULT_MSG = '请求失败'
let redirecting401 = false

service.interceptors.response.use(
  res => {
    removePending(res.config)

    // 文件流导出：直接透传 response.data (Blob)，由调用方自己解析文件名/下载
    if (res.config && res.config.responseType === 'blob') {
      // 后端可能返回 JSON（如 { code: 403, message: 'xxx' }）被解析成 Blob，做二次判断
      if (res.data && res.data.type === 'application/json') {
        return res.data.text().then(t => {
          let msg = DEFAULT_MSG
          try { const j = JSON.parse(t); msg = j.message || msg } catch (e) {}
          Message.error({ message: msg, duration: 2500 })
          const err = new Error(msg)
          err.code = 403
          return Promise.reject(err)
        })
      }
      // 尝试从 Content-Disposition 读取文件名挂到 blob 上
      try {
        const cd = res.headers['content-disposition']
        if (cd) {
          const m = cd.match(/filename\*?=?['"]?([^'";\n]+)['"]?/i)
          if (m && m[1]) {
            const rawName = m[1].startsWith("UTF-8''")
              ? decodeURIComponent(m[1].slice(7))
              : m[1]
            Object.defineProperty(res.data, '_fileName', { value: rawName })
          }
        }
      } catch (e) {}
      return res.data
    }

    const r = res.data
    if (r.code === 200) return r
    if (r.code === 403) {
      Message.error({ message: r.message || CODE_MESSAGE.get(403), duration: 2500 })
      const err = new Error(r.message || CODE_MESSAGE.get(403))
      err.code = 403
      return Promise.reject(err)
    }
    if (r.code === 401) {
      handle401()
      const err = new Error(r.message || CODE_MESSAGE.get(401))
      err.code = 401
      return Promise.reject(err)
    }
    Message.error({ message: r.message || DEFAULT_MSG, duration: 2500 })
    const err = new Error(r.message || 'error')
    err.code = r.code
    return Promise.reject(err)
  },
  err => {
    if (err && err.config) removePending(err.config)

    // axios 主动取消的请求，静默处理
    if (axios.isCancel(err)) {
      return Promise.reject(err)
    }

    const status = err.response && err.response.status
    if (status === 401) {
      handle401()
    } else if (status === 403) {
      const msg =
        (err.response && err.response.data && err.response.data.message) ||
        '无访问权限'
      Message.error({ message: msg, duration: 2500 })
      const permErr = new Error(msg)
      permErr.code = 403
      return Promise.reject(permErr)
    } else {
      const msg =
        (err.response && err.response.data && err.response.data.message) ||
        err.message ||
        '网络错误'
      Message.error({ message: msg, duration: 2500 })
    }
    return Promise.reject(err)
  }
)

function handle401() {
  if (redirecting401) return
  if (router.currentRoute && router.currentRoute.path === '/login') return
  redirecting401 = true
  removeToken()
  MessageBox.alert(CODE_MESSAGE.get(401), '提示', { type: 'warning' })
    .then(() => {
      const target = router.currentRoute && router.currentRoute.fullPath
      router.replace({ path: '/login', query: target && target !== '/login' ? { redirect: target } : undefined })
    })
    .catch(() => {})
    .finally(() => { redirecting401 = false })
}

export default service
