<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createWaterMeterVendor,
  getWaterMeterVendorDetail,
  getWaterMeterVendorPage,
  updateWaterMeterVendorStatus,
} from '@/api/waterMeterVendor.js'

const vendorCodeOptions = [
  { value: 'JICHENG', label: '积成' },
  { value: 'JIEXIAN', label: '捷先' },
  { value: 'CUSTOM', label: '自定义' },
]

const protocolOptions = [
  { value: 'JICHENG_HTTP', label: '积成 HTTP' },
  { value: 'JIEXIAN_HTTP', label: '捷先 HTTP' },
]

const authTypeOptions = [
  { value: 'SIGN', label: '签名' },
  { value: 'COMPANY_CODE', label: '公司编码' },
  { value: 'APP_SECRET', label: 'AppSecret' },
  { value: 'TOKEN', label: 'Token' },
  { value: 'NONE', label: '无认证' },
]

const keyword = ref('')
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const toast = reactive({ show: false, message: '' })
let toastTimer = null

function showToast(message) {
  toast.message = message
  toast.show = true
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.show = false
  }, 2600)
}

async function fetchPage() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await getWaterMeterVendorPage(params)
    const data = res.data || {}
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取水表厂商列表失败', e)
    tableData.value = []
    total.value = 0
    showToast(e?.response?.data?.msg || e.message || '获取水表厂商列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchPage()
}

function goPage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  fetchPage()
}

const createModal = reactive({
  show: false,
  saving: false,
})

const createForm = reactive({
  vendorName: '',
  vendorCode: 'JICHENG',
  protocolType: 'JICHENG_HTTP',
  apiUrl: '',
  authType: 'SIGN',
  appId: '',
  appSecret: '',
  webhookSignature: '',
  configJson: '',
  status: 1,
})

function resetCreateForm() {
  createForm.vendorName = ''
  createForm.vendorCode = 'JICHENG'
  createForm.protocolType = 'JICHENG_HTTP'
  createForm.apiUrl = ''
  createForm.authType = 'SIGN'
  createForm.appId = ''
  createForm.appSecret = ''
  createForm.webhookSignature = ''
  createForm.configJson = ''
  createForm.status = 1
}

function openCreateModal() {
  resetCreateForm()
  createModal.show = true
}

function closeCreateModal() {
  if (createModal.saving) return
  createModal.show = false
}

function syncProtocolByVendorCode() {
  if (createForm.vendorCode === 'JICHENG') {
    createForm.protocolType = 'JICHENG_HTTP'
    createForm.authType = 'SIGN'
  } else if (createForm.vendorCode === 'JIEXIAN') {
    createForm.protocolType = 'JIEXIAN_HTTP'
    createForm.authType = 'COMPANY_CODE'
  }
}

function validateCreateForm() {
  if (!createForm.vendorName.trim()) return '请输入水表厂商名称'
  if (!createForm.vendorCode.trim()) return '请选择厂商编码'
  if (!createForm.protocolType.trim()) return '请选择协议类型'
  if (createForm.configJson.trim()) {
    try {
      JSON.parse(createForm.configJson)
    } catch {
      return '扩展配置必须是合法 JSON'
    }
  }
  return ''
}

async function submitCreate() {
  const error = validateCreateForm()
  if (error) {
    showToast(error)
    return
  }
  createModal.saving = true
  try {
    await createWaterMeterVendor({
      vendorName: createForm.vendorName.trim(),
      vendorCode: createForm.vendorCode.trim(),
      protocolType: createForm.protocolType.trim(),
      apiUrl: createForm.apiUrl.trim(),
      authType: createForm.authType.trim(),
      appId: createForm.appId.trim(),
      appSecret: createForm.appSecret.trim(),
      webhookSignature: createForm.webhookSignature.trim(),
      configJson: createForm.configJson.trim(),
      status: createForm.status,
    })
    createModal.show = false
    currentPage.value = 1
    await fetchPage()
    showToast('保存成功')
  } catch (e) {
    console.error('新增水表厂商失败', e)
    showToast(e?.response?.data?.msg || e.message || '保存失败')
  } finally {
    createModal.saving = false
  }
}

const detailModal = reactive({
  show: false,
  loading: false,
  saving: false,
  data: null,
})

async function openDetail(row) {
  detailModal.show = true
  detailModal.loading = true
  detailModal.data = { ...row }
  try {
    const res = await getWaterMeterVendorDetail(row.id)
    detailModal.data = res.data
  } catch (e) {
    console.error('获取水表厂商详情失败', e)
    showToast(e?.response?.data?.msg || e.message || '获取详情失败')
  } finally {
    detailModal.loading = false
  }
}

function closeDetail() {
  if (detailModal.saving) return
  detailModal.show = false
  detailModal.data = null
}

async function enableCurrentVendor() {
  if (!detailModal.data?.id) return
  detailModal.saving = true
  try {
    await updateWaterMeterVendorStatus(detailModal.data.id, 1)
    detailModal.data.status = 1
    await fetchPage()
    showToast('已启用')
  } catch (e) {
    console.error('启用水表厂商失败', e)
    showToast(e?.response?.data?.msg || e.message || '启用失败')
  } finally {
    detailModal.saving = false
  }
}

const stopConfirm = reactive({
  show: false,
  loading: false,
})

function openStopConfirm() {
  if (!detailModal.data?.id || detailModal.data.status === 0) return
  stopConfirm.show = true
}

function cancelStop() {
  if (stopConfirm.loading) return
  stopConfirm.show = false
}

async function confirmStop() {
  if (!detailModal.data?.id) return
  stopConfirm.loading = true
  detailModal.saving = true
  try {
    await updateWaterMeterVendorStatus(detailModal.data.id, 0)
    detailModal.data.status = 0
    stopConfirm.show = false
    await fetchPage()
    showToast('已停用')
  } catch (e) {
    console.error('停用水表厂商失败', e)
    showToast(e?.response?.data?.msg || e.message || '停用失败')
  } finally {
    stopConfirm.loading = false
    detailModal.saving = false
  }
}

function statusText(status) {
  if (status === 1) return '启用中'
  if (status === 0) return '已停用'
  return '--'
}

function optionLabel(options, value) {
  return options.find(item => item.value === value)?.label || value || '--'
}

function maskSecret(value) {
  if (!value) return '--'
  if (value.length <= 6) return '******'
  return `${value.slice(0, 3)}******${value.slice(-3)}`
}

onMounted(() => {
  fetchPage()
})
</script>

<template>
  <div class="vendor-page">
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="toast.show" class="toast">{{ toast.message }}</div>
      </Transition>
    </Teleport>

    <div class="toolbar">
      <div class="toolbar-left">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索水表厂商、编码或协议"
          type="text"
          @keyup.enter="handleSearch"
        />
        <button class="btn btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span class="material-symbols-outlined btn-icon">add</span>
          新增水表厂商
        </button>
      </div>
    </div>

    <div class="table-panel">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="text-align:left">水表厂商名称</th>
              <th style="width:130px">厂商编码</th>
              <th style="width:150px">协议类型</th>
              <th style="width:120px">状态</th>
              <th style="width:120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="empty-cell">加载中...</td>
            </tr>
            <tr v-else-if="tableData.length === 0">
              <td colspan="5" class="empty-cell">暂无数据</td>
            </tr>
            <tr v-for="row in tableData" :key="row.id">
              <td class="vendor-name" style="text-align:left">
                <div>{{ row.vendorName }}</div>
                <div class="sub-text">{{ row.apiUrl || '--' }}</div>
              </td>
              <td>{{ optionLabel(vendorCodeOptions, row.vendorCode) }}</td>
              <td>{{ optionLabel(protocolOptions, row.protocolType) }}</td>
              <td>
                <span class="status-pill" :class="row.status === 1 ? 'status-on' : 'status-off'">
                  {{ statusText(row.status) }}
                </span>
              </td>
              <td>
                <button class="action-link" @click="openDetail(row)">操作</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager" v-if="total > pageSize">
        <span class="pager-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <div class="pager-actions">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="createModal.show" class="modal-mask" @click.self="closeCreateModal">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>新增水表厂商</h3>
              <button class="modal-close" @click="closeCreateModal">&times;</button>
            </div>
            <div class="modal-body">
              <div class="form-grid">
                <div class="form-row">
                  <label>水表厂商名称</label>
                  <input v-model="createForm.vendorName" placeholder="例如：青岛积成电子" type="text" />
                </div>
                <div class="form-row">
                  <label>厂商编码</label>
                  <select v-model="createForm.vendorCode" @change="syncProtocolByVendorCode">
                    <option v-for="item in vendorCodeOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </option>
                  </select>
                </div>
                <div class="form-row">
                  <label>协议类型</label>
                  <select v-model="createForm.protocolType">
                    <option v-for="item in protocolOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </option>
                  </select>
                </div>
                <div class="form-row">
                  <label>认证方式</label>
                  <select v-model="createForm.authType">
                    <option v-for="item in authTypeOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </option>
                  </select>
                </div>
              </div>
              <div class="form-row">
                <label>API 地址</label>
                <input v-model="createForm.apiUrl" placeholder="https://api.example.com" type="text" />
              </div>
              <div class="form-grid">
                <div class="form-row">
                  <label>AppId / 公司编码</label>
                  <input v-model="createForm.appId" placeholder="积成可填 AppId，捷先可填 companyCode" type="text" />
                </div>
                <div class="form-row">
                  <label>AppSecret / 签名密钥</label>
                  <input v-model="createForm.appSecret" placeholder="输入密钥" type="password" />
                </div>
              </div>
              <div class="form-row">
                <label>Webhook 签名密钥</label>
                <input v-model="createForm.webhookSignature" placeholder="需要回调验签时填写" type="text" />
              </div>
              <div class="form-row">
                <label>扩展配置 JSON</label>
                <textarea
                  v-model="createForm.configJson"
                  placeholder='例如：{"sign":"xxx","companyCode":"0001"}'
                  rows="4"
                ></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button
                class="btn btn-default enable-choice"
                :class="{ active: createForm.status === 1 }"
                :disabled="createModal.saving"
                @click="createForm.status = 1"
              >启用</button>
              <button
                class="btn btn-default enable-choice"
                :class="{ active: createForm.status === 0 }"
                :disabled="createModal.saving"
                @click="createForm.status = 0"
              >停用</button>
              <button class="btn btn-primary" :disabled="createModal.saving" @click="submitCreate">
                {{ createModal.saving ? '保存中...' : '保存' }}
              </button>
              <button class="btn btn-default" :disabled="createModal.saving" @click="closeCreateModal">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="detailModal.show" class="modal-mask" @click.self="closeDetail">
          <div class="modal-panel detail-panel">
            <div class="modal-header">
              <h3>水表厂商简介</h3>
              <button class="modal-close" @click="closeDetail">&times;</button>
            </div>
            <div class="modal-body">
              <div v-if="detailModal.loading" class="detail-loading">加载中...</div>
              <template v-else>
                <div class="detail-row">
                  <span>水表厂商名称</span>
                  <strong>{{ detailModal.data?.vendorName || '--' }}</strong>
                </div>
                <div class="detail-row">
                  <span>厂商编码</span>
                  <strong>{{ optionLabel(vendorCodeOptions, detailModal.data?.vendorCode) }}</strong>
                </div>
                <div class="detail-row">
                  <span>协议类型</span>
                  <strong>{{ optionLabel(protocolOptions, detailModal.data?.protocolType) }}</strong>
                </div>
                <div class="detail-row">
                  <span>API 地址</span>
                  <strong>{{ detailModal.data?.apiUrl || '--' }}</strong>
                </div>
                <div class="detail-row">
                  <span>认证方式</span>
                  <strong>{{ optionLabel(authTypeOptions, detailModal.data?.authType) }}</strong>
                </div>
                <div class="detail-row">
                  <span>AppId / 公司编码</span>
                  <strong>{{ detailModal.data?.appId || '--' }}</strong>
                </div>
                <div class="detail-row">
                  <span>AppSecret / 签名密钥</span>
                  <strong>{{ maskSecret(detailModal.data?.appSecret) }}</strong>
                </div>
                <div class="detail-row">
                  <span>Webhook 签名密钥</span>
                  <strong>{{ maskSecret(detailModal.data?.webhookSignature) }}</strong>
                </div>
                <div class="detail-row detail-row-block">
                  <span>扩展配置 JSON</span>
                  <pre>{{ detailModal.data?.configJson || '--' }}</pre>
                </div>
                <div class="detail-row">
                  <span>状态</span>
                  <strong>{{ statusText(detailModal.data?.status) }}</strong>
                </div>
              </template>
            </div>
            <div class="modal-footer">
              <button class="btn btn-primary" :disabled="detailModal.saving" @click="enableCurrentVendor">启用</button>
              <button
                class="btn btn-danger"
                :disabled="detailModal.saving || detailModal.data?.status === 0"
                @click="openStopConfirm"
              >停用</button>
              <button class="btn btn-default" :disabled="detailModal.saving" @click="closeDetail">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="stopConfirm.show" class="modal-mask confirm-mask">
          <div class="confirm-panel">
            <h3>确定是否停用</h3>
            <p>停用后该厂商不会作为水表接口调用的启用配置。</p>
            <div class="confirm-actions">
              <button class="btn btn-danger" :disabled="stopConfirm.loading" @click="confirmStop">
                {{ stopConfirm.loading ? '处理中...' : '是' }}
              </button>
              <button class="btn btn-default" :disabled="stopConfirm.loading" @click="cancelStop">否</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.vendor-page {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.search-input {
  width: min(340px, 100%);
  height: 38px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
  background: #fff;
  outline: none;
}

.search-input::placeholder {
  color: rgba(102, 102, 102, 0.45);
}

.search-input:focus {
  border-color: #3a64ab;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-primary {
  background: #3a64ab;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #264c8d;
}

.btn-default {
  background: #fff;
  color: #333;
  border-color: #d9d9d9;
}

.btn-default:hover:not(:disabled) {
  border-color: #3a64ab;
  color: #3a64ab;
}

.btn-danger {
  background: #ff4d4f;
  color: #fff;
}

.btn-danger:hover:not(:disabled) {
  background: #d9363e;
}

.btn-icon {
  font-size: 17px;
  line-height: 1;
}

.table-panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

th {
  padding: 14px 16px;
  background: #fafafa;
  color: #555;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

td {
  padding: 15px 16px;
  border-bottom: 1px solid #f5f5f5;
  color: #333;
  font-size: 14px;
}

tbody tr:hover {
  background: #fafcff;
}

.vendor-name {
  font-weight: 600;
}

.sub-text {
  max-width: 520px;
  margin-top: 4px;
  overflow: hidden;
  color: #999;
  font-size: 12px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-cell {
  padding: 56px 0;
  color: #999;
  text-align: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.status-on {
  color: #52c41a;
  background: #f6ffed;
}

.status-off {
  color: #999;
  background: #f5f5f5;
}

.action-link {
  border: none;
  background: transparent;
  color: #3a64ab;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
}

.action-link:hover {
  background: #eef3fa;
}

.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.pager-info {
  color: #999;
  font-size: 13px;
}

.pager-actions {
  display: flex;
  gap: 8px;
}

.page-btn {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #666;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.4);
}

.modal-panel {
  width: min(680px, 100%);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}

.detail-panel {
  width: min(720px, 100%);
}

.modal-header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.modal-close {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  font-size: 24px;
  line-height: 1;
}

.modal-body {
  padding: 22px;
  overflow-y: auto;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  margin-bottom: 6px;
  color: #666;
  font-size: 13px;
}

.form-row input,
.form-row select,
.form-row textarea {
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 9px 11px;
  color: #333;
  font-size: 14px;
  outline: none;
}

.form-row textarea {
  resize: vertical;
  font-family: Consolas, 'Courier New', monospace;
  line-height: 1.5;
}

.form-row input:focus,
.form-row select:focus,
.form-row textarea:focus {
  border-color: #3a64ab;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.enable-choice.active {
  border-color: #3a64ab;
  color: #3a64ab;
  background: #eef3fa;
}

.detail-loading {
  padding: 44px 0;
  text-align: center;
  color: #999;
}

.detail-row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-row span {
  color: #999;
  font-size: 13px;
}

.detail-row strong {
  min-width: 0;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.detail-row-block pre {
  margin: 0;
  max-height: 160px;
  overflow: auto;
  border: 1px solid #edf0f5;
  border-radius: 6px;
  padding: 10px;
  background: #fafbfc;
  color: #333;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.confirm-mask {
  z-index: 80;
}

.confirm-panel {
  width: min(360px, 100%);
  padding: 26px;
  background: #fff;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 16px 45px rgba(0, 0, 0, 0.18);
}

.confirm-panel h3 {
  margin: 0 0 10px;
  color: #333;
  font-size: 16px;
}

.confirm-panel p {
  margin: 0 0 20px;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.toast {
  position: fixed;
  top: 78px;
  left: 50%;
  z-index: 120;
  transform: translateX(-50%);
  padding: 10px 18px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.78);
  color: #fff;
  font-size: 14px;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.16);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .modal-footer,
  .pager {
    flex-wrap: wrap;
  }
}
</style>
