import { ref } from 'vue'

const toast = ref('')
const toastType = ref('success') // 'success' | 'error'

let timer = null

export function useToast() {
  function showToast(msg, type = 'success') {
    toast.value = msg
    toastType.value = type
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { toast.value = '' }, 3000)
  }

  return { toast, toastType, showToast }
}
