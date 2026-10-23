<script setup>
import { ref, watch } from 'vue'
import { getMeterQrCode, getMeterQrCodeImage } from '@/api/meters_1.js'
import { getChildQrcode, getChildQrcodeImage } from '@/api/shadowMeter_1.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  meterId: { type: [Number, String], default: null },
  meterType: { type: String, default: 'physical' }, // 'physical' | 'shadow'
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const error = ref('')
const qrInfo = ref(null)
const qrImageUrl = ref('')

watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v) loadQrCode()
})

function close() {
  visible.value = false
  emit('update:modelValue', false)
}

function normalize(data, isShadow) {
  if (!data) return null
  if (!isShadow) return data
  // Shadow meter returns full MeterResponse — flatten to match MeterQrCodeVO shape
  const g = data.general || {}
  const cm = data.chenman || {}
  const jc = data.jicheng || {}
  return {
    meterId: data.id,
    dbNo: cm.dbNo || jc.stealNo || '',
    meterName: cm.name || data.customerName || '',
    meterType: g.meterType,
    meterTypeName: g.meterType === 1 ? '电表' : g.meterType === 0 ? '水表' : '',
    customerName: data.customerName || '',
    qrContent: '',
  }
}

async function loadQrCode() {
  if (!props.meterId) return
  loading.value = true
  error.value = ''
  qrInfo.value = null
  qrImageUrl.value = ''
  try {
    const isShadow = props.meterType === 'shadow'
    const [infoRes, imgUrl] = await Promise.all([
      isShadow ? getChildQrcode(props.meterId) : getMeterQrCode(props.meterId),
      isShadow ? getChildQrcodeImage(props.meterId) : getMeterQrCodeImage(props.meterId),
    ])
    qrInfo.value = normalize(infoRes?.data, isShadow)
    qrImageUrl.value = imgUrl
  } catch (e) {
    error.value = '二维码加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handleDownload() {
  if (!qrImageUrl.value) return
  const a = document.createElement('a')
  a.href = qrImageUrl.value
  const name = qrInfo.value?.meterName || qrInfo.value?.dbNo || props.meterId
  a.download = `二维码_${name}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="fixed inset-0 bg-black/40 z-[90] flex items-center justify-center" @click.self="close">
        <div class="bg-white rounded-xl shadow-2xl w-[420px] max-w-[92vw] overflow-hidden">
          <!-- Header -->
          <div class="px-5 py-4 border-b border-[#f0f0f0] flex justify-between items-center">
            <h3 class="text-[15px] font-semibold text-[#333]">表计二维码</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="close">&times;</button>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="flex items-center justify-center py-16">
            <span class="text-[#3a64ab] text-[14px] animate-pulse">加载中...</span>
          </div>

          <!-- Error -->
          <div v-else-if="error" class="flex flex-col items-center py-16 gap-3">
            <span class="text-[13px] text-[#ff4d4f]">{{ error }}</span>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] border-none cursor-pointer" @click="loadQrCode">重试</button>
          </div>

          <!-- Content -->
          <template v-else-if="qrInfo">
            <!-- QR Image -->
            <div class="px-5 py-8 flex flex-col items-center gap-5">
              <div class="w-48 h-48 bg-[#f7f8fa] rounded-lg flex items-center justify-center border border-[#f0f0f0] overflow-hidden">
                <img v-if="qrImageUrl" :src="qrImageUrl" alt="QR Code" class="w-full h-full object-contain" />
                <span v-else class="text-[13px] text-[#999]">二维码加载失败</span>
              </div>

              <!-- Meter Info -->
              <div class="w-full grid grid-cols-2 gap-x-5 gap-y-2.5">
                <div class="flex flex-col">
                  <span class="text-[11px] text-[#999]">表号</span>
                  <span class="text-[13px] text-[#333] font-medium">{{ qrInfo.dbNo || '—' }}</span>
                </div>
                <div class="flex flex-col">
                  <span class="text-[11px] text-[#999]">表计名称</span>
                  <span class="text-[13px] text-[#333] font-medium">{{ qrInfo.meterName || '—' }}</span>
                </div>
                <div class="flex flex-col">
                  <span class="text-[11px] text-[#999]">表计类型</span>
                  <span class="text-[13px] text-[#333] font-medium">{{ qrInfo.meterTypeName || (qrInfo.meterType === 1 ? '电表' : '水表') }}</span>
                </div>
                <div class="flex flex-col">
                  <span class="text-[11px] text-[#999]">绑定商户</span>
                  <span class="text-[13px] text-[#333] font-medium">{{ qrInfo.customerName || '未绑定' }}</span>
                </div>
              </div>
            </div>
          </template>

          <!-- Footer -->
          <div class="px-5 py-3 border-t border-[#f0f0f0] flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:border-[#3a64ab] transition-colors" @click="close">关闭</button>
            <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors disabled:opacity-50" :disabled="!qrImageUrl" @click="handleDownload">下载二维码</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
</style>
