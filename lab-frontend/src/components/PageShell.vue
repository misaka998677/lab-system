<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ title }}</h2>
        <p v-if="desc" class="page-desc">{{ desc }}</p>
      </div>
      <div class="page-actions">
        <slot name="actions"></slot>
        <slot name="extra"></slot>
      </div>
    </div>
    <div class="page-content" v-if="$slots.default">
      <slot></slot>
    </div>
    <div v-else class="page-content-bare">
      <slot name="content"></slot>
    </div>
  </div>
</template>

<script>
/**
 * 通用页面外壳。统一三件事：
 *   1. 页面标题 / 描述
 *   2. 右上角操作区（slot=actions 或 extra）
 *   3. 主体内容（默认 slot）
 *
 * 使用：
 *   <page-shell title="实验室管理" desc="管理所有实验室信息">
 *     <template #actions>
 *       <el-button type="primary" icon="el-icon-plus">新增</el-button>
 *     </template>
 *     <div>列表…</div>
 *   </page-shell>
 */
export default {
  name: 'PageShell',
  props: {
    title: { type: String, default: '' },
    desc:  { type: String, default: '' },
    bare:  { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.page-shell { padding: 16px 24px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.page-desc { margin: 4px 0 0; color: #909399; font-size: 13px; }
.page-actions { min-width: 200px; text-align: right; }
.page-content { background: #fff; border-radius: 4px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.page-content-bare { padding: 0; }
</style>
