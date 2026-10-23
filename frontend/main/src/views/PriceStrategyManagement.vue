<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  getPriceStrategyPage, getPriceStrategyDetail, createPriceStrategy,
  updatePriceStrategy, deletePriceStrategy, togglePriceStrategyStatus,
} from '@/api/priceStrategy.js'

// ── 常量映射 ──────────────────────────────────────────────────────────
const types = { 1: '固定电价', 2: '分时电价', 3: '阶梯电价' }
const typeTag = { 1: 'status-blue', 2: 'status-orange', 3: 'status-cyan' }
const names = { SHARP: '尖', PEAK: '峰', FLAT: '平', VALLEY: '谷' }
const colors = { SHARP: '#dc2626', PEAK: '#f97316', FLAT: '#2563eb', VALLEY: '#16a34a' }
const periodTypes = computed(() => Object.keys(names))

// ── 列表状态 ──────────────────────────────────────────────────────────
const rows = ref([])
const total = ref(0)
const keyword = ref('')
const meterType = ref('')
const loading = ref(false)

// ── 统计 chip 筛选 ────────────────────────────────────────────────────
const activeStat = ref('')
const statChips = [
  { label: '总数', key: null },
  { label: '启用', key: 1 },
  { label: '停用', key: 0 },
]
const statCount = computed(() => ({
  总数: total.value,
  启用: rows.value.filter(r => r.status === 1).length,
  停用: rows.value.filter(r => r.status === 0).length,
}))
const filteredRows = computed(() => {
  if (activeStat.value === '') return rows.value
  const chip = statChips.find(c => c.label === activeStat.value)
  return rows.value.filter(r => r.status === chip.key)
})
function applyStatFilter(label) {
  activeStat.value = activeStat.value === label ? '' : label
}
function resetFilters() {
  keyword.value = ''
  meterType.value = ''
  activeStat.value = ''
  load()
}

// ── 表单状态 ──────────────────────────────────────────────────────────
const fresh = () => ({
  strategyName: '', strategyCode: '', meterType: 1, priceType: 1,
  effectiveDate: '', expireDate: '', fixedPrice: '', plusPrice: '', remark: '',
  prices: { SHARP: '', PEAK: '', FLAT: '', VALLEY: '' },
  tiers: [{ amount: '', price: '' }],
  slots: Array.from({ length: 24 }, (_, hour) => ({ hour, type: null })),
})
const form = ref(fresh())
const dialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const error = ref('')

// ── 列表加载 ──────────────────────────────────────────────────────────
async function load() {
  loading.value = true
  try {
    const r = await getPriceStrategyPage({
      pageNum: 1,
      pageSize: 200,
      keyword: keyword.value || undefined,
      meterType: meterType.value === '' ? undefined : Number(meterType.value),
    })
    rows.value = r.data?.records || []
    total.value = r.data?.total || 0
  } finally {
    loading.value = false
  }
}

// ── 新增 / 编辑 ───────────────────────────────────────────────────────
function openCreate() {
  editing.value = null
  form.value = fresh()
  error.value = ''
  dialog.value = true
}

function parseTimeSlots() {
  try { return JSON.parse(form.value.timeSlots || '[]') } catch { return [] }
}

function resolveSlotKey(name) {
  if (!name) return null
  if (Object.prototype.hasOwnProperty.call(names, name)) return name
  return { 尖: 'SHARP', 峰: 'PEAK', 平: 'FLAT', 谷: 'VALLEY' }[name] || null
}

async function openEdit(r) {
  editing.value = r.id
  form.value = fresh()
  Object.assign(form.value, r)
  error.value = ''
  try {
    const d = await getPriceStrategyDetail(r.id)
    Object.assign(form.value, d.data || {})
  } catch (e) { /* 详情拉取失败时用列表数据兜底 */ }

  if (form.value.priceType === 2) {
    for (const x of parseTimeSlots()) {
      const t = resolveSlotKey(x.slotName)
      if (!t) continue
      const start = +x.startHour, end = +x.endHour
      if (end === 24) {
        for (let h = start; h < 24; h++) form.value.slots[h].type = t
      } else {
        for (let h = start; h !== end; h = (h + 1) % 24) form.value.slots[h].type = t
      }
    }
  } else if (form.value.priceType === 3) {
    const arr = parseTimeSlots()
    form.value.tiers = arr.length
      ? arr.map(x => ({ amount: x.amount ?? '', price: x.price ?? '' }))
      : [{ amount: '', price: '' }]
  }
  dialog.value = true
}

// ── 分时：时段网格交互（保持不变） ────────────────────────────────────
const drag = ref({ active: false, type: null, moved: false, suppress: false })
function pick(h, t) { form.value.slots[h].type = form.value.slots[h].type === t ? null : t }
function startDrag(h, t) { drag.value = { active: true, type: t, moved: false, suppress: false }; form.value.slots[h].type = t }
function enterDrag(h, t) { if (drag.value.active && drag.value.type === t) { drag.value.moved = true; form.value.slots[h].type = t } }
function endDrag() { drag.value.suppress = drag.value.moved; drag.value.active = false; drag.value.type = null; drag.value.moved = false }
function cellClick(h, t) { if (drag.value.suppress) { drag.value.suppress = false; return } pick(h, t) }
function all(t) { form.value.slots.forEach(s => s.type = t) }
function clear(t) { form.value.slots.forEach(s => { if (!t || s.type === t) s.type = null }) }
function ranges(t) {
  const a = form.value.slots.filter(s => s.type === t).map(s => s.hour)
  const o = []
  for (const h of a) {
    if (o.length && h === o[o.length - 1][1]) o[o.length - 1][1] = h + 1
    else o.push([h, h + 1])
  }
  return o
}

// ── 阶梯：档位编辑 ────────────────────────────────────────────────────
function addTier() { form.value.tiers.push({ amount: '', price: '' }) }
function removeTier(i) { form.value.tiers.splice(i, 1) }

// ── 校验 / 保存 ───────────────────────────────────────────────────────
function validate() {
  if (!form.value.strategyName.trim()) { error.value = '请填写策略名称'; return false }
  if (!form.value.strategyCode.trim()) { error.value = '请填写策略编码'; return false }
  if (!form.value.effectiveDate) { error.value = '请选择生效日期'; return false }
  if (form.value.priceType === 1) {
    if (form.value.fixedPrice === '' || form.value.fixedPrice == null || +form.value.fixedPrice < 0) { error.value = '请输入合法的固定电价'; return false }
  } else if (form.value.priceType === 2) {
    const m = form.value.slots.filter(s => !s.type)
    if (m.length) {
      error.value = '以下时段尚未配置费率类型：' + m.map(s => `${String(s.hour).padStart(2, '0')}:00—${String(s.hour + 1).padStart(2, '0')}:00`).join('、')
      return false
    }
  } else if (form.value.priceType === 3) {
    const tiers = form.value.tiers.filter(t => t.price !== '' && t.price != null)
    if (tiers.length === 0) { error.value = '请至少配置一个阶梯档位'; return false }
    let last = -1
    for (const t of tiers) {
      if (+t.price < 0) { error.value = '阶梯电价不能为负'; return false }
      if (t.amount !== '' && t.amount != null) {
        if (+t.amount <= last) { error.value = '阶梯电量上限需从小到大递增'; return false }
        last = +t.amount
      }
    }
  }
  return true
}

async function save() {
  if (!validate()) return
  saving.value = true
  error.value = ''
  try {
    const p = { ...form.value }
    if (form.value.priceType === 1) {
      p.fixedPrice = form.value.fixedPrice === '' ? null : +form.value.fixedPrice
      p.timeSlots = null
    } else if (form.value.priceType === 2) {
      const a = []
      for (const t of periodTypes.value)
        for (const r of ranges(t))
          a.push({ slotName: names[t], startHour: r[0], endHour: r[1], price: form.value.prices[t] === '' ? null : +form.value.prices[t] })
      p.timeSlots = JSON.stringify(a)
      p.fixedPrice = null
    } else if (form.value.priceType === 3) {
      const ts = form.value.tiers
        .filter(t => t.price !== '' && t.price != null)
        .map(t => {
          const o = { price: +t.price }
          if (t.amount !== '' && t.amount != null) o.amount = +t.amount
          return o
        })
      p.timeSlots = JSON.stringify(ts)
      p.fixedPrice = null
    }
    if (p.plusPrice === '' || p.plusPrice == null) p.plusPrice = null
    else p.plusPrice = +p.plusPrice
    delete p.slots
    delete p.prices
    delete p.tiers
    editing.value ? await updatePriceStrategy(editing.value, p) : await createPriceStrategy(p)
    dialog.value = false
    await load()
  } catch (e) {
    error.value = e?.response?.data?.msg || e?.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

// ── 删除 / 启停 ───────────────────────────────────────────────────────
const deleteConfirm = ref({ show: false, row: null })
const deleting = ref(false)
function confirmDelete(row) { deleteConfirm.value = { show: true, row } }
async function executeDelete() {
  const row = deleteConfirm.value.row
  if (!row) return
  deleting.value = true
  try { await deletePriceStrategy(row.id); deleteConfirm.value = { show: false, row: null }; load() }
  catch (e) { console.error('删除失败', e) }
  finally { deleting.value = false }
}
async function toggle(r) {
  try { await togglePriceStrategyStatus(r.id, r.status === 1 ? 0 : 1); load() }
  catch (e) { console.error('启停失败', e) }
}

function meterTypeLabel(v) { return v === 1 ? '电表' : v === 0 ? '水表' : '--' }
function statusLabel(v) { return v === 1 ? '启用' : '停用' }

onMounted(() => { load(); window.addEventListener('mouseup', endDrag) })
onUnmounted(() => window.removeEventListener('mouseup', endDrag))
</script>

<template>
  <div>
    <!-- 筛选栏 -->
    <div class="bg-white rounded p-[15px] mb-[10px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-[15px] items-center">
        <input v-model="keyword" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] w-[200px] outline-none" placeholder="策略名称 / 编码" type="text" @keyup.enter="load" />
        <select v-model="meterType" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white outline-none">
          <option value="">全部表计</option>
          <option value="1">电表</option>
          <option value="0">水表</option>
        </select>
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d]" @click="load">查询</button>
        <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb]" @click="resetFilters">重置</button>
        <button class="py-1.5 px-4 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#264c8d] transition-colors" @click="openCreate">新增策略</button>
      </div>
    </div>

    <!-- 统计 chips -->
    <div class="bg-white rounded p-[15px] mb-[15px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="flex flex-wrap gap-2.5">
        <div v-for="stat in statChips" :key="stat.label" class="py-1.5 px-3 border rounded text-xs cursor-pointer select-none transition-colors" :class="activeStat === stat.label ? 'bg-[#3a64ab] border-[#3a64ab] text-white' : 'bg-[#e8f0fe] border-[#3a64ab] text-[#3a64ab] hover:bg-[#dce8fb]'" @click="applyStatFilter(stat.label)">
          {{ stat.label }}:
          <span class="ml-1">{{ statCount[stat.label] }}</span>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="bg-white rounded p-5 shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
      <div class="overflow-x-auto relative" style="min-height:150px">
        <div v-if="loading" class="absolute inset-0 z-10 bg-white/70 flex items-center justify-center">
          <span class="text-[#3a64ab] text-[13px] font-bold animate-pulse">数据加载中...</span>
        </div>
        <table v-if="filteredRows.length > 0" class="w-full border-collapse text-left">
          <thead>
            <tr>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap w-[60px]">序号</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">策略名称</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">策略编码</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">表计</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">计费类型</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">生效日期</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">失效日期</th>
              <th class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">状态</th>
              <th class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] font-medium bg-[#f5f6fb] whitespace-nowrap">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, idx) in filteredRows" :key="r.id" class="transition-colors hover:bg-[#f5f6fb]">
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ idx + 1 }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ r.strategyName }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] font-mono">{{ r.strategyCode }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">{{ meterTypeLabel(r.meterType) }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="typeTag[r.priceType] || 'status-gray'">{{ types[r.priceType] || '--' }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ r.effectiveDate || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px] whitespace-nowrap">{{ r.expireDate || '--' }}</td>
              <td class="py-3.5 px-3 border-b border-r border-[#e8e8e8] text-[13px]">
                <span class="status-tag" :class="r.status === 1 ? 'status-green' : 'status-red'">{{ statusLabel(r.status) }}</span>
              </td>
              <td class="py-3.5 px-3 border-b border-[#e8e8e8] text-[13px] whitespace-nowrap">
                <span class="text-[#3a64ab] cursor-pointer mr-3 hover:text-[#264c8d]" @click="openEdit(r)">编辑</span>
                <span class="text-[#3a64ab] cursor-pointer mr-3 hover:text-[#264c8d]" @click="toggle(r)">{{ r.status === 1 ? '停用' : '启用' }}</span>
                <span class="text-[#ff4d4f] cursor-pointer hover:text-[#ff7875]" @click="confirmDelete(r)">删除</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="!loading" class="py-16 text-center text-[14px] text-[#999]">暂无数据</div>
      </div>
      <div v-if="total > 0" class="flex justify-between items-center mt-4 pt-3 border-t border-[#f0f0f0]">
        <span class="text-[13px] text-[#666]">共 {{ total }} 条记录</span>
      </div>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <Teleport to="body">
      <div v-if="dialog" class="fixed inset-0 bg-black/40 z-[60] flex items-center justify-center" @click.self="dialog = false">
        <div class="bg-white rounded-lg shadow-2xl w-[980px] max-w-[95vw] max-h-[90vh] flex flex-col">
          <div class="flex justify-between items-center px-6 py-4 border-b border-[#f0f0f0] shrink-0">
            <h3 class="text-base font-medium">{{ editing ? '编辑' : '新增' }}费率策略</h3>
            <button class="border-none bg-transparent cursor-pointer text-xl text-[#999] leading-none" @click="dialog = false">&times;</button>
          </div>

          <div class="px-6 py-5 overflow-y-auto flex-1">
            <!-- 基本信息 -->
            <div class="mb-5">
              <div class="text-[13px] font-medium text-[#333] mb-2.5">基本信息</div>
              <div class="grid grid-cols-3 gap-4">
                <div>
                  <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 策略名称</label>
                  <input v-model="form.strategyName" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如：夏季分时电价" type="text" />
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 策略编码</label>
                  <input v-model="form.strategyCode" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如：SUMMER_TOU" type="text" />
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1">表计类型</label>
                  <select v-model.number="form.meterType" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] bg-white outline-none">
                    <option :value="1">电表</option>
                    <option :value="0">水表</option>
                  </select>
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 生效日期</label>
                  <input v-model="form.effectiveDate" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="date" />
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1">失效日期</label>
                  <input v-model="form.expireDate" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" type="date" />
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1">备注</label>
                  <input v-model="form.remark" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="选填" type="text" />
                </div>
              </div>
            </div>

            <!-- 计费类型切换 -->
            <div class="mb-4">
              <div class="text-[13px] font-medium text-[#333] mb-2.5">计费类型</div>
              <div class="flex gap-2">
                <button v-for="(label, key) in types" :key="key" class="py-1.5 px-5 rounded border text-[13px] cursor-pointer transition-colors" :class="form.priceType == key ? 'bg-[#3a64ab] text-white border-[#3a64ab]' : 'bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]'" @click="form.priceType = Number(key)">{{ label }}</button>
              </div>
            </div>

            <!-- 固定电价 -->
            <div v-if="form.priceType === 1" class="mb-5">
              <div class="text-[13px] font-medium text-[#333] mb-2.5">固定电价设置</div>
              <div class="grid grid-cols-2 gap-4 max-w-[560px]">
                <div>
                  <label class="text-[13px] text-[#666] block mb-1"><span class="text-[#ff4d4f]">*</span> 电价（元/kWh）</label>
                  <input v-model="form.fixedPrice" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="如 0.85" type="number" min="0" step="0.01" />
                </div>
                <div>
                  <label class="text-[13px] text-[#666] block mb-1">附加费（元/kWh）</label>
                  <input v-model="form.plusPrice" class="w-full py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="选填，默认 0" type="number" min="0" step="0.01" />
                </div>
              </div>
            </div>

            <!-- 分时电价 -->
            <div v-else-if="form.priceType === 2" class="mb-5">
              <div class="text-[13px] font-medium text-[#333] mb-2.5">分时时段设置</div>
              <div class="flex flex-wrap gap-2 mb-3">
                <button v-for="t in periodTypes" :key="'all-' + t" class="py-1.5 px-3 rounded border text-[13px] cursor-pointer transition-colors bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]" @click="all(t)">全部设为{{ names[t] }}</button>
                <button class="py-1.5 px-3 rounded border text-[13px] cursor-pointer transition-colors bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]" @click="clear()">清空</button>
                <button v-for="t in periodTypes" :key="'clear-' + t" class="py-1.5 px-3 rounded border text-[13px] cursor-pointer transition-colors bg-white text-[#333] border-[#d9d9d9] hover:border-[#3a64ab]" @click="clear(t)">清空{{ names[t] }}</button>
              </div>

              <div class="slot-grid">
                <b>时段</b>
                <span v-for="h in 24" :key="h">{{ String(h - 1).padStart(2, '0') }}</span>
                <template v-for="t in periodTypes" :key="'row-' + t">
                  <b :style="{ color: colors[t] }">{{ names[t] }}</b>
                  <button v-for="s in form.slots" :key="'cell-' + t + '-' + s.hour" :title="`${names[t]}：${s.hour}:00—${s.hour + 1}:00`" :style="s.type === t ? { background: colors[t], color: '#fff' } : {}" @click="pick(s.hour, t)">{{ s.type === t ? names[t] : '' }}</button>
                </template>
              </div>

              <div class="flex flex-wrap gap-3 mt-3">
                <div v-for="t in periodTypes" :key="'price-' + t" class="flex items-center gap-2">
                  <span class="text-[13px]" :style="{ color: colors[t] }">{{ names[t] }}电价</span>
                  <input v-model="form.prices[t]" class="w-[100px] py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none" placeholder="元/kWh" type="number" min="0" step="0.01" />
                </div>
              </div>
            </div>

            <!-- 阶梯电价 -->
            <div v-else class="mb-5">
              <div class="text-[13px] font-medium text-[#333] mb-2.5">阶梯电价设置</div>
              <div class="space-y-2">
                <div v-for="(tier, i) in form.tiers" :key="i" class="flex items-center gap-3">
                  <span class="text-[13px] text-[#999] w-12 shrink-0">{{ i + 1 }}档</span>
                  <input v-model="tier.amount" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none w-[240px]" placeholder="电量上限(kWh)，最后一档可留空" type="number" min="0" step="0.01" />
                  <input v-model="tier.price" class="py-1.5 px-3 border border-[#d9d9d9] rounded text-[13px] outline-none w-[160px]" placeholder="电价(元/kWh)" type="number" min="0" step="0.01" />
                  <button class="py-1.5 px-3 rounded border text-[13px] cursor-pointer bg-white text-[#ff4d4f] border-[#d9d9d9] hover:border-[#ff4d4f] disabled:opacity-40 disabled:cursor-not-allowed" :disabled="form.tiers.length === 1" @click="removeTier(i)">删除</button>
                </div>
                <button class="py-1.5 px-4 rounded border text-[13px] cursor-pointer bg-white text-[#3a64ab] border-[#3a64ab] hover:bg-[#eef3fa]" @click="addTier">＋ 添加档位</button>
                <p class="text-[12px] text-[#999]">说明：阶梯按累计电量计费，例如「0-200 度 0.5 元」「200-400 度 0.6 元」「400 度以上 0.85 元」，最后一档电量上限留空表示「以上部分」。</p>
              </div>
            </div>

            <p v-if="error" class="text-[13px] text-[#ff4d4f] bg-[#fff2f0] border border-[#ffccc7] rounded px-3 py-2 mb-2">{{ error }}</p>
          </div>

          <div class="flex gap-2.5 justify-end px-6 py-4 border-t border-[#f0f0f0] shrink-0">
            <button class="py-1.5 px-5 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="dialog = false">取消</button>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteConfirm.show" class="fixed inset-0 bg-black/40 z-[70] flex items-center justify-center" @click.self="deleteConfirm.show = false">
        <div class="bg-white rounded-lg shadow-2xl p-6 w-[400px]">
          <h3 class="text-base font-medium mb-4">确认删除</h3>
          <p class="text-[13px] text-[#666] mb-4">确定要删除费率策略「{{ deleteConfirm.row?.strategyName }}」吗？删除后不可恢复。</p>
          <div class="flex gap-2.5 justify-end">
            <button class="py-1.5 px-4 rounded bg-white border border-[#d9d9d9] text-[#333] text-[13px] cursor-pointer" @click="deleteConfirm.show = false">取消</button>
            <button class="py-1.5 px-4 rounded bg-[#ff4d4f] text-white text-[13px] cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed" :disabled="deleting" @click="executeDelete">{{ deleting ? '删除中...' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.slot-grid {
  display: grid;
  grid-template-columns: 70px repeat(24, 32px);
  overflow: auto;
  border: 1px solid #d9dee8;
  border-radius: 6px;
}
.slot-grid > * {
  border: 1px solid #e5e9ef;
  padding: 6px;
  text-align: center;
}
</style>
