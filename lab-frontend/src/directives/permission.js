import Vue from 'vue'
import { hasRole, hasPerm } from '@/utils/permission'

Vue.directive('role', {
  inserted(el, binding) {
    const roles = binding.instance.$store.state.user.roles || []
    if (!hasRole(roles, binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

Vue.directive('perm', {
  inserted(el, binding) {
    const perms = binding.instance.$store.state.user.perms || []
    if (!hasPerm(perms, binding.value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

Vue.directive('hide', {
  inserted(el, binding) {
    if (binding.value) {
      el.style.display = 'none'
    }
  }
})