/**
 * WebSocket 客户端。统一入口：init(token) / on(type, cb) / off(type, cb) / close()。
 *
 * 后端消息协议：{ "type": "refresh-overview", "ts": 123456789, "payload": {...} }
 * - type 为必填字段；payload 为可选扩展字段。
 * - 断线后按指数退避重连（3s → 6s → ... → 最大 32s）
 */

const MAX_RECONNECT = 32000
const MIN_RECONNECT = 2000
let reconnectDelay  = MIN_RECONNECT

let ws = null
let reconnectTimer = null
let manualClose = false
let currentToken = null
// type -> Set(fn)
const listenersByType = new Map()
// 全部消息监听（用于调试等）
const allListeners = new Set()

function buildUrl() {
  // 优先使用前端配置的 WebSocket URL
  const envUrl = process.env.VUE_APP_WS_URL
  if (envUrl) return envUrl
  const scheme = (typeof location !== 'undefined' && location.protocol === 'https:') ? 'wss:' : 'ws:'
  const host = (typeof location !== 'undefined') ? location.host : 'localhost'
  return scheme + '//' + host + '/ws/stat'
}

function startReconnect() {
  if (manualClose) return
  if (reconnectTimer) return
  const delay = reconnectDelay
  reconnectDelay = Math.min(MAX_RECONNECT, reconnectDelay * 2)
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (currentToken) connect(currentToken)
  }, delay)
}

function clearReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

/** 建立连接。若已经处于 OPEN / CONNECTING，则保持原连接。 */
function connect(token) {
  currentToken = token || currentToken
  if (!currentToken) return
  manualClose = false
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }

  const url = buildUrl() + '?token=' + encodeURIComponent(currentToken)
  ws = new WebSocket(url)

  ws.onopen = () => {
    reconnectDelay = MIN_RECONNECT
    clearReconnect()
    allListeners.forEach(fn => safeCall(fn, { type: '_open' }))
  }

  ws.onmessage = (event) => {
    try {
      const msg = typeof event.data === 'string'
        ? JSON.parse(event.data)
        : null
      if (!msg || !msg.type) return
      const typeListeners = listenersByType.get(msg.type)
      if (typeListeners) typeListeners.forEach(fn => safeCall(fn, msg))
      allListeners.forEach(fn => safeCall(fn, msg))
    } catch (e) {
      // 解析失败，丢弃
    }
  }

  ws.onclose = () => {
    ws = null
    allListeners.forEach(fn => safeCall(fn, { type: '_close' }))
    startReconnect()
  }

  ws.onerror = () => {
    try { if (ws) ws.close() } catch (e) {}
    ws = null
    allListeners.forEach(fn => safeCall(fn, { type: '_error' }))
    startReconnect()
  }
}

function safeCall(fn, arg) {
  try { fn(arg) } catch (e) {}
}

/** 公开 API */

export function init(token) {
  connect(token)
  return {
    on: (type, fn) => on(type, fn),
    off: (type, fn) => off(type, fn),
    close: () => close()
  }
}

export function on(type, fn) {
  if (!fn) return () => {}
  if (!listenersByType.has(type)) listenersByType.set(type, new Set())
  listenersByType.get(type).add(fn)
  return () => off(type, fn)
}

export function off(type, fn) {
  const set = listenersByType.get(type)
  if (set) set.delete(fn)
}

export function onAll(fn) {
  allListeners.add(fn)
  return () => allListeners.delete(fn)
}

export function close() {
  manualClose = true
  clearReconnect()
  if (ws) {
    try { ws.close() } catch (e) {}
    ws = null
  }
  listenersByType.clear()
  allListeners.clear()
}

export function isConnected() {
  return ws && ws.readyState === WebSocket.OPEN
}

export function ensureConnected(token) {
  connect(token)
}
