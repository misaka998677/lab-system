<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">预约审核</h2>
        <p class="page-desc">审批实验室预约申请，通过或驳回并填写审批意见。</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="预约号/用途" clearable style="width:220px" @keyup.enter.native="reload" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px; margin-left:12px">
          <el-option :value="0" label="待审核" />
          <el-option :value="1" label="已通过" />
          <el-option :value="2" label="已驳回" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" style="margin-left:12px" @click="reload">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无预约记录">
        <el-table-column prop="reserveNo" label="预约号" width="160" />
        <el-table-column prop="userName" label="申请人" width="120" />
        <el-table-column prop="labName" label="实验室" width="160" />
        <el-table-column prop="purpose" label="用途" min-width="200" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="['', 'success', 'danger', 'primary', 'info', 'warning'][row.status]" effect="dark">
              {{ ['待审核', '已通过', '已驳回', '已签到', '已签退', '已取消'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" v-if="canAudit">
          <template #default="{ row }">
            <el-button size="mini" type="success" v-if="row.status === 0" @click="onAudit(row, true)">通过</el-button>
            <el-button size="mini" type="danger" v-if="row.status === 0" @click="onAudit(row, false)">驳回</el-button>
          </template>
        </el-table-column>
        <el-table-column v-else label="操作" width="100" align="center">
          <template #default>
            <span style="color:#909399">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination background layout="total,prev,pager,next"
        :total="total" :current-page.sync="query.pageNum" :page-size="query.pageSize"
        @current-change="reload"
        style="padding: 16px 20px; border-top: 1px solid #F2F6FC;" />
    </el-card>
  </div>
</template>

<script>
import { reservePage, reserveAudit } from '@/api/reserve'

export default {
  name: 'ReserveAudit',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, keyword: '', status: null },
      list: [], total: 0, loading: false,
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  computed: {
    canAudit() { return (this.$store.state.user.roles || []).some(r => ['ROLE_ADMIN', 'ROLE_LABADMIN', 'ROLE_TEACHER'].includes(r)) }
  },
  mounted() { this.reload() },
  methods: {
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    async reload() {
      this.loading = true
      try { const r = await reservePage(this.query); this.list = r.data.records; this.total = r.data.total } catch (e) {} finally { this.loading = false }
    },
    onAudit(row, pass) {
      this.$prompt(`${pass ? '通过' : '驳回'}意见（可空）`, '审批预约', { inputValue: '' })
        .then(async ({ value }) => {
          try { await reserveAudit(row.id, { pass, note: value || '' }); this.$message.success('已审批'); this.reload() } catch (e) {}
        }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
</style>