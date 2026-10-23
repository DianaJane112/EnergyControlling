import { ref } from 'vue'

const state = ref({ show: false, message: '', title: '操作确认', resolve: null })

export function useConfirm() {
  function showConfirm(message, title = '操作确认') {
    return new Promise((resolve) => {
      state.value = { show: true, message, title, resolve }
    })
  }

  function onOk() {
    state.value.resolve?.(true)
    state.value = { show: false, message: '', title: '操作确认', resolve: null }
  }

  function onCancel() {
    state.value.resolve?.(false)
    state.value = { show: false, message: '', title: '操作确认', resolve: null }
  }

  return { confirmState: state, showConfirm, onOk, onCancel }
}
