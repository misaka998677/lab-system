/**
 * Vuex CRUD mixin 工厂函数。
 *
 * 业务层只需一行：
 *   const lab = makeCrudModule({
 *     fetchList: (p) => request.get('/lab/room/page', { params: p }),
 *     fetchDetail: (id) => request.get(`/lab/room/${id}`),
 *     create:     (d) => request.post('/lab/room', d),
 *     update:     (d) => request.put('/lab/room', d),
 *     remove:     (id) => request.delete(`/lab/room/${id}`)
 *   })
 *   // 注册：new Vuex.Store({ modules: { lab } })
 *
 * 页面中使用：
 *   this.$store.dispatch('lab/fetchList', { pageNum:1, pageSize:10 })
 *   this.$store.commit('lab/setForm', {...})
 *   this.$store.dispatch('lab/saveForm')  // 自动根据 id 切换 create/update
 *   this.$store.dispatch('lab/remove', id)
 *
 * state 字段：
 *   list, total, pageNum, pageSize, loading,
 *   detail, detailLoading,
 *   form, dialog, formLoading,
 *   keyword, extraQuery, lastQuery
 */

export const makeCrudState = (initial = {}) => ({
  // 列表
  list: [],
  total: 0,
  pageNum: initial.pageNum || 1,
  pageSize: initial.pageSize || 10,
  loading: false,
  // 详情
  detail: null,
  detailLoading: false,
  // 表单
  form: {},
  dialog: false,
  formLoading: false,
  // 查询
  keyword: '',
  extraQuery: initial.extraQuery || {},
  lastQuery: null
})

export const makeCrudMutations = () => ({
  setList(state, { records, total }) {
    state.list = records || []
    if (typeof total === 'number') state.total = total
  },
  setPage(state, { pageNum, pageSize }) {
    if (pageNum != null) state.pageNum = pageNum
    if (pageSize != null) state.pageSize = pageSize
  },
  setLoading(state, v) { state.loading = !!v },
  setKeyword(state, v) { state.keyword = v || '' },
  setExtra(state, obj) { state.extraQuery = { ...(state.extraQuery || {}), ...(obj || {}) } },
  setDetail(state, v) { state.detail = v || null },
  setDetailLoading(state, v) { state.detailLoading = !!v },
  setForm(state, v) { state.form = { ...(v || {}) } },
  setDialog(state, v) { state.dialog = !!v },
  setFormLoading(state, v) { state.formLoading = !!v },
  setLastQuery(state, q) { state.lastQuery = q }
})

/**
 * @param {Object} handlers
 * @param {(q)=>Promise} handlers.fetchList
 * @param {(id)=>Promise} [handlers.fetchDetail]
 * @param {(form)=>Promise} [handlers.create]
 * @param {(form)=>Promise} [handlers.update]
 * @param {(id)=>Promise} [handlers.remove]
 * @param {(resp)=>{records,total}} [handlers.extractList]
 *   默认取 resp.data.records / resp.data.total；若后端返回结构特殊，可在此处覆盖
 * @param {(resp)=>Object} [handlers.extractDetail]
 *   默认取 resp.data
 */
export const makeCrudActions = (handlers = {}) => ({
  async fetchList({ commit, state }, query = {}) {
    const q = {
      pageNum: query.pageNum || state.pageNum,
      pageSize: query.pageSize || state.pageSize,
      keyword: query.keyword !== undefined ? query.keyword : state.keyword,
      ...(state.extraQuery || {}),
      ...(query.extra || {})
    }
    commit('setLastQuery', q)
    commit('setPage', { pageNum: q.pageNum, pageSize: q.pageSize })
    commit('setLoading', true)
    try {
      const resp = await handlers.fetchList(q)
      if (handlers.extractList) {
        const r = handlers.extractList(resp)
        commit('setList', { records: r.records, total: r.total })
      } else {
        const body = (resp && resp.data) || resp
        commit('setList', { records: body.records, total: body.total })
      }
    } catch (e) {
      commit('setList', { records: [], total: 0 })
      throw e
    } finally {
      commit('setLoading', false)
    }
  },

  reload({ dispatch, state }) {
    return dispatch('fetchList', state.lastQuery || {})
  },

  async fetchDetail({ commit }, id) {
    if (!handlers.fetchDetail) return null
    commit('setDetailLoading', true)
    try {
      const resp = await handlers.fetchDetail(id)
      const body = handlers.extractDetail ? handlers.extractDetail(resp) : (resp && resp.data != null ? resp.data : resp)
      commit('setDetail', body)
      return body
    } finally {
      commit('setDetailLoading', false)
    }
  },

  async saveForm({ dispatch, state }, form = null) {
    const target = form || state.form
    const isUpdate = !!(target && (target.id != null || target.ID != null))
    const fn = isUpdate ? (handlers.update || handlers.create) : (handlers.create || handlers.update)
    if (!fn) throw new Error('saveForm: missing create/update handler')
    commit('setFormLoading', true)
    try {
      const resp = await fn(target)
      commit('setForm', {})
      commit('setDialog', false)
      dispatch('reload')
      return resp
    } finally {
      commit('setFormLoading', false)
    }
  },

  async remove({ dispatch }, id) {
    if (!handlers.remove) throw new Error('remove: missing handler')
    await handlers.remove(id)
    dispatch('reload')
  },

  openDialog({ commit }, form = {}) {
    commit('setForm', form)
    commit('setDialog', true)
  },

  closeDialog({ commit }) {
    commit('setForm', {})
    commit('setDialog', false)
  }
})

export const makeCrudGetters = () => ({
  pageInfo: s => ({ pageNum: s.pageNum, pageSize: s.pageSize, total: s.total })
})

/**
 * 生成一个完整的 Vuex module（namespaced）。
 */
export const makeCrudModule = (handlers, initialState) => ({
  namespaced: true,
  state: makeCrudState(initialState || {}),
  mutations: makeCrudMutations(),
  actions: makeCrudActions(handlers),
  getters: makeCrudGetters()
})

/**
 * 页面辅助：把 Vuex CRUD state 映射到 computed 与 methods。
 * 页面内：
 *   import { crudMixins } from '@/store/utils/crud-mixin'
 *   export default {
 *     mixins: [crudMixins('lab')],
 *     mounted() { this.$_crud_fetchList() }
 *   }
 */
export function crudMixins(moduleName) {
  return {
    computed: {
      // 列表
      [moduleName + '_list']() { return this.$store.state[moduleName].list },
      [moduleName + '_total']() { return this.$store.state[moduleName].total },
      [moduleName + '_pageNum']() { return this.$store.state[moduleName].pageNum },
      [moduleName + '_pageSize']() { return this.$store.state[moduleName].pageSize },
      [moduleName + '_loading']() { return this.$store.state[moduleName].loading },
      // 表单
      [moduleName + '_form']() { return this.$store.state[moduleName].form },
      [moduleName + '_dialog']() { return this.$store.state[moduleName].dialog },
      [moduleName + '_formLoading']() { return this.$store.state[moduleName].formLoading },
      [moduleName + '_detail']() { return this.$store.state[moduleName].detail }
    },
    methods: {
      ['$_' + moduleName + '_fetchList'](query) { return this.$store.dispatch(moduleName + '/fetchList', query || {}) },
      ['$_' + moduleName + '_reload']() { return this.$store.dispatch(moduleName + '/reload') },
      ['$_' + moduleName + '_fetchDetail'](id) { return this.$store.dispatch(moduleName + '/fetchDetail', id) },
      ['$_' + moduleName + '_save'](form) { return this.$store.dispatch(moduleName + '/saveForm', form) },
      ['$_' + moduleName + '_remove'](id) { return this.$store.dispatch(moduleName + '/remove', id) },
      ['$_' + moduleName + '_openDialog'](form) { return this.$store.dispatch(moduleName + '/openDialog', form || {}) },
      ['$_' + moduleName + '_closeDialog']() { return this.$store.dispatch(moduleName + '/closeDialog') },
      ['$_' + moduleName + '_setKeyword'](v) { return this.$store.commit(moduleName + '/setKeyword', v) },
      ['$_' + moduleName + '_setExtra'](obj) { return this.$store.commit(moduleName + '/setExtra', obj) }
    }
  }
}
