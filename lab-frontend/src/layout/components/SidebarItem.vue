<template>
  <div>
    <!-- 情况1: 有children且parentId !== 0 (子菜单的子菜单) -->
    <el-submenu v-if="hasChildren && route.parentId !== 0 && !route.hidden" :index="resolvePath(route.path)">
      <template #title>
        <i v-if="icon" :class="icon"></i>
        <span>{{ title }}</span>
      </template>
      <sidebar-item v-for="c in visibleChildren" :key="c.path" :route="c" :base="resolvePath(route.path)" />
    </el-submenu>

    <!-- 情况2: 有children且parentId === 0 (一级菜单) -->
    <el-submenu v-else-if="hasChildren && route.parentId === 0 && !route.hidden" :index="route.path">
      <template #title>
        <i v-if="route.meta && route.meta.icon" :class="route.meta.icon"></i>
        <span>{{ route.meta.title || route.name }}</span>
      </template>
      <template v-for="c in visibleChildren">
        <sidebar-item v-if="hasGrandChildren(c)" :key="c.path" :route="c" :base="resolvePath(route.path, c.path)" />
        <el-menu-item v-else :key="c.path" :index="resolvePath(route.path, c.path)" @click="handleMenuClick(c)">
          <i v-if="c.meta && c.meta.icon" :class="c.meta.icon"></i>
          <span>{{ c.meta.title || c.name }}</span>
        </el-menu-item>
      </template>
    </el-submenu>

    <!-- 情况3: 没有children (叶子菜单) -->
    <el-menu-item v-else-if="!route.hidden" :index="resolvePath(route.path)" @click="handleMenuClick(route)">
      <i v-if="icon" :class="icon"></i>
      <span slot="title">{{ title }}</span>
    </el-menu-item>
  </div>
</template>

<script>
export default {
  name: 'SidebarItem',
  props: {
    route: Object,
    base: { type: String, default: '' }
  },
  computed: {
    title() {
      if (this.route.meta && this.route.meta.title) return this.route.meta.title
      return this.route.name
    },
    icon() {
      if (this.route.meta) return this.route.meta.icon
      return ''
    },
    visibleChildren() {
      if (!this.route.children) return []
      return this.route.children.filter(c => !c.hidden)
    },
    hasChildren() {
      return this.visibleChildren.length > 0
    }
  },
  methods: {
    hasGrandChildren(c) {
      return !!(c.children && c.children.length > 0)
    },
    resolvePath(base, sub) {
      const b = (base != null ? base : this.base || '').replace(/\/$/, '') || ''
      let s = sub != null ? sub : (this.route ? this.route.path : null)
      if (!s) s = (this.route && this.route.path) ? this.route.path : ''
      s = s || ''

      if (!b && !s) return '/'
      if (s.startsWith && s.startsWith('/')) return s
      if (b.startsWith('/')) {
        return b + (s ? '/' + s : '')
      }
      return (b ? '/' + b : '') + (s ? '/' + s : '') || '/'
    },
    handleMenuClick(menuRoute) {
      let targetPath
      const path = menuRoute.path
      if (!path.startsWith('/')) {
        const basePath = this.route.path
        targetPath = basePath === '/' ? '/' + path : basePath + '/' + path
      } else {
        targetPath = path
      }
      if (this.$route.path === targetPath) return
      this.$router.push(targetPath).catch(err => {
        // 忽略 NavigationDuplicated 和 NavigationCancelled 错误
        if (err && err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') {
          console.error(err)
        }
      })
    }
  }
}
</script>
