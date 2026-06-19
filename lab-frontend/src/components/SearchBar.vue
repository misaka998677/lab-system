<template>
  <div class="search-bar">
    <el-input
      v-if="keywordVisible"
      v-model="keyword"
      :placeholder="keywordPlaceholder"
      clearable
      size="small"
      style="width: 220px; margin-right: 12px;"
      @keyup.enter.native="onSearch"
    ></el-input>

    <el-select
      v-for="opt in opts"
      :key="opt.prop"
      v-model="opt.value"
      :placeholder="opt.placeholder || opt.label"
      clearable
      :filterable="!!opt.filterable"
      size="small"
      style="width: 160px; margin-right: 12px;"
    >
      <el-option
        v-for="item in opt.items"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      ></el-option>
    </el-select>

    <slot></slot>

    <el-button size="small" type="primary" icon="el-icon-search" @click="onSearch">搜索</el-button>
    <el-button size="small" icon="el-icon-refresh-left" @click="onReset">重置</el-button>
  </div>
</template>

<script>
/**
 * 通用搜索栏。
 * 支持：
 *   - keyword: 关键字输入（可选，默认可见）
 *   - options: [{ prop, label, placeholder, filterable, items: [{label,value}] }]
 *
 * 事件：
 *   - @search({ keyword, [prop]: value ... })
 *   - @reset
 *
 * 示例：
 *   <SearchBar
 *     :keyword-visible="true"
 *     keyword-placeholder="按名称搜索"
 *     :options="[
 *       { prop: 'status', label: '状态', items: [{label:'启用',value:1},{label:'禁用',value:0}] },
 *       { prop: 'deptId', label: '部门', filterable: true, items: deptOptions }
 *     ]"
 *     @search="onSearch"
 *     @reset="onReset"
 *   />
 */
export default {
  name: 'SearchBar',
  props: {
    keywordVisible:    { type: Boolean, default: true },
    keywordPlaceholder:{ type: String,  default: '请输入关键字' },
    defaultKeyword:    { type: String,  default: '' },
    options:           { type: Array,   default: () => [] }
  },
  data() {
    const opts = (this.options || []).map(o => ({ ...o, value: o.value != null ? o.value : '' }))
    return {
      keyword: this.defaultKeyword,
      opts
    }
  },
  watch: {
    options: {
      deep: true,
      handler(newVal) {
        // 保留已有 value
        const oldMap = {}
        this.opts.forEach(o => { oldMap[o.prop] = o.value })
        this.opts = (newVal || []).map(o => ({
          ...o,
          value: (o.prop in oldMap) ? oldMap[o.prop] : (o.value != null ? o.value : '')
        }))
      }
    }
  },
  computed: {
    _query() {
      const q = {}
      if (this.keywordVisible) q.keyword = this.keyword
      this.opts.forEach(o => {
        if (o.value !== '' && o.value != null) q[o.prop] = o.value
      })
      return q
    }
  },
  methods: {
    onSearch() { this.$emit('search', this._query) },
    onReset() {
      this.keyword = this.defaultKeyword || ''
      this.opts.forEach(o => { o.value = o.defaultValue != null ? o.defaultValue : '' })
      this.$emit('reset')
    },
    setQuery(q) {
      const qq = q || {}
      if ('keyword' in qq) this.keyword = qq.keyword
      this.opts.forEach(o => {
        if (o.prop in qq) o.value = qq[o.prop]
      })
    }
  }
}
</script>

<style scoped>
.search-bar {
  display: flex; align-items: center; flex-wrap: wrap;
  padding: 8px 4px 12px;
}
.search-bar .el-button + .el-button { margin-left: 8px; }
</style>
