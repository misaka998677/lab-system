export function downloadBlob(blob, fileName) {
  const fallback = `${Date.now()}.xlsx`
  const name = fileName || (blob && blob._fileName) || fallback
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function triggerFileInput(accept, onChange) {
  const input = document.createElement('input')
  input.type = 'file'
  if (accept) input.accept = accept
  input.style.display = 'none'
  input.onchange = e => {
    try {
      const file = e.target.files && e.target.files[0]
      onChange(file)
    } finally {
      e.target.value = ''
    }
  }
  document.body.appendChild(input)
  input.click()
  document.body.removeChild(input)
}
