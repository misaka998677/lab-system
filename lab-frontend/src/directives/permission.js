import Vue from 'vue'
import store from '@/store'
import { hasRole, hasPerm, KEY_MAP, installGetters, installPermGetter } from '@/utils/permission'

// 让 permission.js 的简便 API 能在运行态取到 Vuex 的当前用户
installGetters(() => (store.state && store.state.user && store.state.user.roles) || [])
installPermGetter(() => (store.state && store.state.user && store.state.user.perms) || [])

/**
 * v-role="['ROLE_ADMIN']"  或  v-role="'ROLE_ADMIN'"
 */
Vue.directive('role', {
  inserted(el, binding) {
    const roles = (store.state && store.state.user && store.state.user.roles) || []
    if (!hasRole(roles, binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

/**
 * v-perm="['reservation:audit']"
 */
Vue.directive('perm', {
  inserted(el, binding) {
    const perms = (store.state && store.state.user && store.state.user.perms) || []
    if (!hasPerm(perms, binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

/**
 * 通用 v-permission 指令。
 *   - v-permission:admin             — 仅 ADMIN 可见
 *   - v-permission:manageLab         — 可管理实验室
 *   - v-permission="['ROLE_ADMIN']"  — 指定角色
 */
Vue.directive('permission', {
  inserted(el, binding) {
    const key = binding.arg
    if (key) {
      const fn = KEY_MAP[key]
      if (fn && !fn()) {
        el.parentNode && el.parentNode.removeChild(el)
        return
      }
      // 未知 key，认为有权限（开发者笔误也不会隐藏）
      return
    }
    // 数组形式：指定某角色
    const roles = (store.state && store.state.user && store.state.user.roles) || []
    if (!hasRole(roles, binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

/**
 * v-hide="condition" — 为 true 时 display:none
 */
Vue.directive('hide', {
  inserted(el, binding) {
    if (binding.value) el.style.display = 'none'
  },
  update(el, binding) {
    el.style.display = binding.value ? 'none' : ''
  }
})
