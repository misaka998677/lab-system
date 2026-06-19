let echartsPromise = null

export function loadECharts() {
  if (echartsPromise) return echartsPromise
  echartsPromise = import('echarts').then(mod => mod.default || mod)
  return echartsPromise
}

export function getECharts(vueInstance) {
  const onProto = vueInstance && vueInstance.$echarts
  if (onProto) return Promise.resolve(onProto)
  return loadECharts()
}
