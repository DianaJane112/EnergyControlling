import { ref } from 'vue'

const message = ref('')
const visible = ref(false)
const type = ref('info')
let timer = null

export function useToast() {
  function show(msg, opts = {}) {
    message.value = msg
    type.value = opts.type || 'info'
    visible.value = true
    clearTimeout(timer)
    timer = setTimeout(() => { visible.value = false }, opts.duration || 3000)
  }

  function success(msg) { show(msg, { type: 'success' }) }
  function error(msg) { show(msg, { type: 'error' }) }
  function info(msg) { show(msg, { type: 'info' }) }
  function hide() { visible.value = false; clearTimeout(timer) }

  return { message, visible, type, show, success, error, info, hide }
}
