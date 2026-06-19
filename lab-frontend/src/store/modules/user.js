import { login, logout, getInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'

const state = { token: getToken(), user: {}, roles: [], perms: [] }

const mutations = {
  SET_TOKEN: (s, t) => s.token = t,
  SET_USER:  (s, u) => s.user  = u || {},
  SET_ROLES: (s, r) => s.roles = r || [],
  SET_PERMS: (s, p) => s.perms = p || []
}

const actions = {
  login({ commit, dispatch }, data) {
    commit('SET_USER', {})
    commit('SET_ROLES', [])
    commit('SET_PERMS', [])
    try { dispatch('permission/clearRoutes', null, { root: true }) } catch (e) {}
    return login(data).then(res => {
      commit('SET_TOKEN', res.data.token)
      setToken(res.data.token)
      return res.data
    })
  },
  fetchInfo({ commit }) {
    return getInfo().then(res => {
      commit('SET_USER',  res.data.user)
      commit('SET_ROLES', res.data.roles)
      commit('SET_PERMS', res.data.perms)
      return res.data
    })
  },
  logout({ commit, dispatch }) {
    return logout().finally(async () => {
      commit('SET_TOKEN', '')
      commit('SET_USER', {})
      commit('SET_ROLES', [])
      commit('SET_PERMS', [])
      try { await dispatch('permission/clearRoutes', null, { root: true }) } catch (e) {}
      resetRouter()
      removeToken()
    })
  }
}

export default { namespaced: true, state, mutations, actions }
