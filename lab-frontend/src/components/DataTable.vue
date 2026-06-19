<template>
  <div class="data-table">
    <el-table
      :data="data"
      :border="border"
      :stripe="stripe"
      style="width: 100%"
      v-loading="loading"
      @selection-change="$emit('selection-change', $event)"
      v-bind="$attrs"
    >
      <el-table-column
        v-if="selectable"
        type="selection"
        width="48"
      ></el-table-column>

      <el-table-column
        v-for="col in plainColumns"
        :key="'plain-' + col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width || undefined"
        :min-width="col.minWidth || undefined"
        :align="col.align || 'left'"
        :sortable="!!col.sortable"
      ></el-table-column>

      <el-table-column
        v-for="col in slotColumns"
        :key="'slot-' + col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width || undefined"
        :min-width="col.minWidth || undefined"
        :align="col.align || 'left'"
        :sortable="!!col.sortable"
      >
        <template slot-scope="scope">
          <slot :name="col.slot" :row="scope.row" :column="col" :index="scope.$index"></slot>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="showPager"
      class="data-pager"
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    ></el-pagination>
  </div>
</template>

<script>
export default {
  name: 'DataTable',
  inheritAttrs: false,
  props: {
    columns:    { type: Array, default: () => [] },
    data:       { type: Array, default: () => [] },
    total:      { type: Number, default: 0 },
    pageNum:    { type: Number, default: 1 },
    pageSize:   { type: Number, default: 10 },
    loading:    { type: Boolean, default: false },
    selectable: { type: Boolean, default: false },
    border:     { type: Boolean, default: true },
    stripe:     { type: Boolean, default: true },
    showPager:  { type: Boolean, default: true }
  },
  computed: {
    plainColumns() {
      return this.columns.filter(c => !c.slot)
    },
    slotColumns() {
      return this.columns.filter(c => c.slot)
    }
  },
  methods: {
    handleSizeChange(size) { this.$emit('size-change', size) },
    handlePageChange(page) { this.$emit('page-change', page) }
  }
}
</script>

<style scoped>
.data-table { background: #fff; padding: 8px 4px; }
.data-pager { margin-top: 12px; justify-content: flex-end; display: flex; }
</style>
