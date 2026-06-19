<template>
  <div class="app-container page-shell not-found-page">
    <div class="nf-hero">
      <div class="nf-code">404</div>
      <div class="nf-title">很抱歉，你访问的页面不存在</div>
      <div class="nf-sub">或许你走错了路径，我们为你提供以下导航。</div>
      <div class="nf-actions">
        <el-button type="primary" round icon="el-icon-s-home" @click="goHome">回到首页</el-button>
        <el-button round icon="el-icon-arrow-left" @click="goBack">返回上一页</el-button>
      </div>
      <div class="nf-tips">
        <el-alert title="如果你输入的是链接，请检查是否存在拼写错误。" type="info" show-icon :closable="false"></el-alert>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  methods: {
    goHome() { this.$router.push('/dashboard').catch(() => {}) },
    goBack() {
      if (window.history.length > 1) this.$router.back()
      else this.$router.push('/dashboard').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.not-found-page {
  min-height: calc(100vh - 80px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4edf5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.nf-hero {
  background: #fff;
  border-radius: 12px;
  padding: 48px 60px;
  text-align: center;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  max-width: 620px;
}
.nf-code {
  font-size: 96px;
  font-weight: 800;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 12px;
}
.nf-title { font-size: 20px; color: #303133; margin-bottom: 10px; font-weight: 600; }
.nf-sub { color: #909399; margin-bottom: 24px; }
.nf-actions { margin-bottom: 20px; display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
.nf-tips { max-width: 480px; margin: 0 auto; }
</style>
