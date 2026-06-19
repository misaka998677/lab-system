<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">签到记录</h2>
        <p class="page-desc">查看实验室预约的签到签退记录，统计实际使用时长。</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card" body-style="padding:0">
      <el-table :data="list" v-loading="loading" :border="false" style="width:100%" :header-cell-style="headerStyle" :row-style="rowStyle" empty-text="暂无签到记录">
        <el-table-column prop="reserveNo" label="预约号" width="160" />
        <el-table-column prop="userName" label="申请人" width="120" />
        <el-table-column prop="labName" label="实验室" width="160" />
        <el-table-column prop="checkInTime" label="签到时间" width="170">
          <template #default="{ row }">
            <span v-if="row.checkInTime">{{ row.checkInTime }}</span>
            <span v-else style="color:#C0C4CC">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="checkOutTime" label="签退时间" width="170">
          <template #default="{ row }">
            <span v-if="row.checkOutTime">{{ row.checkOutTime }}</span>
            <span v-else style="color:#C0C4CC">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" v-if="row.status === 3" type="primary" effect="dark">已签到</el-tag>
            <el-tag size="small" v-else-if="row.status === 4" type="info" effect="dark">已签退</el-tag>
            <span v-else style="color:#909399">—</span>
          </template>
        </el-table-column>
        <el-table-column label="使用时长" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.checkInTime && row.checkOutTime" style="color:#409EFF">{{ calcDuration(row.checkInTime, row.checkOutTime) }}</span>
            <span v-else style="color:#C0C4CC">—</span>
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
import { reserveCheckRecords } from '@/api/reserve'

export default {
  name: 'ReserveCheck',
  data() {
    return {
      query: { pageNum: 1, pageSize: 10 },
      list: [], total: 0, loading: false,
      headerStyle: { background: '#FAFBFC', color: '#606266', fontWeight: 500 }
    }
  },
  mounted() { this.reload() },
  methods: {
    rowStyle({ rowIndex }) { return rowIndex % 2 === 1 ? { background: '#FAFBFC' } : {} },
    async reload() {
      this.loading = true
      try { const r = await reserveCheckRecords(this.query); this.list = r.data.records || []; this.total = r.data.total || 0 } catch (e) {} finally { this.loading = false }
    },
    calcDuration(inTime, outTime) {
      if (!inTime || !outTime) return '—'
      const diff = new Date(outTime) - new Date(inTime)
      const mins = Math.floor(diff / 60000)
      if (mins < 60) return mins + '分钟'
      const hrs = Math.floor(mins / 60)
      const remain = mins % 60
      return hrs + '小时' + (remain > 0 ? remain + '分钟' : '')
    }
  }
}
</script>

<style scoped lang="scss">
</style>