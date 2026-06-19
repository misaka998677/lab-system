<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar" :class="{ collapsed }">
      <div class="logo">
        <span class="logo-icon">
          <i class="el-icon-office-building"></i>
        </span>
        <span v-if="!collapsed" class="logo-text">实验室管理系统</span>
      </div>
      <div v-if="!collapsed" class="role-tag-wrap">
        <span class="role-tag" :style="{ borderColor: roleColor, color: roleColor }">
          <i class="el-icon-user-solid"></i>{{ rolePrimaryText }}
        </span>
      </div>

      <el-menu
        :default-active="$route.path"
        :collapse="collapsed"
        background-color="#1F2D3D"
        text-color="#BFC8D1"
        active-text-color="#409EFF"
        :collapse-transition="false"
      >
        <el-menu-item index="/dashboard" @click="handleNavClick('/dashboard')">
          <i class="el-icon-s-home"></i>
          <span slot="title">首页</span>
        </el-menu-item>

        <template v-for="r in routes">
          <sidebar-item :key="r.path" :route="r" :base="''" />
        </template>
      </el-menu>

      <div class="sidebar-footer" @click="collapsed = !collapsed">
        <i :class="collapsed ? 'el-icon-d-arrow-right' : 'el-icon-d-arrow-left'"></i>
        <span v-if="!collapsed">收起菜单</span>
      </div>
    </el-aside>

    <el-container class="main-area">
      <div class="header-top-bar" :style="{ background: roleColor }"></div>
      <el-header class="header">
        <div class="header-left">
          <div class="crumb">
            <i class="el-icon-s-home crumb-home" @click="handleNavClick('/dashboard')"></i>
            <template v-if="breadcrumbTrail.length > 0">
              <span class="crumb-sep" v-for="(_, i) in breadcrumbTrail.slice(0, -1)" :key="'sep-' + i">/</span>
              <span class="crumb-item" v-for="(name, i) in breadcrumbTrail" :key="'item-' + i" :class="{ last: i === breadcrumbTrail.length - 1 }">
                {{ name }}
              </span>
            </template>
            <span v-else class="crumb-item last">首页 / 工作台</span>
          </div>
        </div>
        <div class="header-right">
          <div class="user-chip">
            <div class="user-avatar">{{ avatarInitial }}</div>
            <div class="user-info">
              <div class="user-name">{{ user.realName || user.username || '用户' }}</div>
              <div class="user-role">{{ roleText }}</div>
            </div>
          </div>
          <el-dropdown trigger="click" @command="onCommand" class="menu-dropdown">
            <span class="dropdown-trigger">
              <i class="el-icon-setting"></i>
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown" align="end">
              <el-dropdown-item command="profile">
                <i class="el-icon-user"></i>个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <i class="el-icon-switch-button"></i>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="page-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import SidebarItem from './components/SidebarItem.vue'
import { ROLE_TEXT, ROLE_THEME, primaryRole } from '@/utils/role-map'

export default {
  name: 'Layout',
  components: { SidebarItem },
  data() {
    return {
      collapsed: false
    }
  },
  computed: {
    routes() { return this.$store.state.permission.dynamicRoutes || [] },
    user() { return this.$store.state.user.user || {} },
    _roles() { return this.$store.state.user.roles || [] },
    rolePrimaryText() {
      const r = primaryRole(this._roles)
      return r ? (ROLE_TEXT[r] || r) : '未分配角色'
    },
    roleColor() {
      const r = primaryRole(this._roles)
      return r ? (ROLE_THEME[r] || '#409EFF') : '#909399'
    },
    roleText() {
      const rs = this._roles
      if (rs.length === 0) return '未分配角色'
      return rs.map(r => ROLE_TEXT[r] || r).join(' / ')
    },
    avatarInitial() {
      const n = this.user.realName || this.user.username || 'U'
      return (n + '').charAt(0)
    },
    breadcrumbTrail() {
      const route = this.$route
      if (!route) return []
      if (route.path === '/dashboard' || route.path === '/') return ['工作台']
      const trail = []
      if (route.matched && route.matched.length > 0) {
        route.matched.forEach(r => {
          if (r.meta && r.meta.title) trail.push(r.meta.title)
        })
      } else if (route.meta && route.meta.title) {
        trail.push(route.meta.title)
      }
      return trail
    }
  },
  methods: {
    handleNavClick(path) {
      if (this.$route.path === path) return
      this.$router.push(path).catch(err => {
        // 忽略 NavigationDuplicated 和 NavigationCancelled 错误
        if (err && err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') {
          console.error(err)
        }
      })
    },
    async onCommand(c) {
      if (c === 'logout') {
        await this.$store.dispatch('user/logout')
        // 整页刷新：彻底重置 Vue Router 的 addRoute 状态，避免旧角色路由残留
        window.location.replace('/login')
      } else if (c === 'profile') {
        this.$message.info('个人中心功能即将上线')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-layout {
  height: 100vh;
  background: #F5F7FA;
}

.sidebar {
  background: #1F2D3D;
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width .25s ease;
  overflow: hidden;
  position: relative;
}

.sidebar .logo {
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  color: #fff;
  font-weight: bold;
  background: #17202A;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  white-space: nowrap;
}
.sidebar .logo-icon {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #409EFF, #67B0FF);
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.sidebar .logo-text {
  font-size: 15px;
  letter-spacing: 1px;
}
.sidebar.collapsed .logo-text { display: none; }

.role-tag-wrap {
  padding: 12px 20px;
  background: #17202A;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}
.role-tag {
  display: inline-block;
  padding: 4px 10px;
  font-size: 12px;
  border: 1px solid;
  border-radius: 12px;
  background: rgba(255,255,255,0.04);
}
.role-tag i { margin-right: 4px; font-size: 12px; }

.sidebar .el-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar .el-menu::-webkit-scrollbar { width: 4px; }
.sidebar .el-menu::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }

.sidebar-footer {
  height: 44px;
  line-height: 44px;
  color: #7A8699;
  text-align: center;
  cursor: pointer;
  background: #17202A;
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
  transition: color .2s;
}
.sidebar-footer:hover { color: #409EFF; }
.sidebar-footer i { font-size: 16px; }

.main-area {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header-top-bar {
  height: 4px;
  width: 100%;
  flex-shrink: 0;
}

.header {
  background: #fff;
  border-bottom: 1px solid #EBEEF5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.crumb {
  color: #606266;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.crumb-home {
  font-size: 16px;
  cursor: pointer;
  color: #409EFF;
}
.crumb-sep {
  color: #C0C4CC;
}
.crumb-item {
  color: #606266;
}
.crumb-item.last {
  color: #303133;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: 22px;
  background: #F5F7FA;
  transition: background .2s;
}
.user-chip:hover { background: #ECF5FF; }

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #67B0FF);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  text-transform: uppercase;
  font-size: 14px;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.user-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}
.user-role {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.menu-dropdown {
  color: #606266;
  cursor: pointer;
  padding: 0 6px;
}
.menu-dropdown .el-icon--right { font-size: 12px; }

.page-content {
  background: #F5F7FA;
  padding: 20px 24px;
  flex: 1;
  overflow-y: auto;
}
.page-content::-webkit-scrollbar { width: 8px; height: 8px; }
.page-content::-webkit-scrollbar-thumb { background: #D8DEE9; border-radius: 4px; }
.page-content::-webkit-scrollbar-thumb:hover { background: #C0C4CC; }
</style>
