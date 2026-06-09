<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="office-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown="onKeydown"
        @click.self="close"
      >
        <!-- Header -->
        <div class="office-header">
          <span class="office-name" :title="file?.fileName">{{ file?.fileName || '文档预览' }}</span>
          <span class="office-type">{{ fileTypeLabel }}</span>
          <div class="office-actions">
            <button class="office-btn office-btn-close" @click="close" title="关闭 (Esc)">✕</button>
          </div>
        </div>

        <!-- Body -->
        <div class="office-body">
          <!-- Loading -->
          <div v-if="loading" class="office-status">正在解析文档...</div>

          <!-- Error -->
          <div v-else-if="error" class="office-status office-error">
            <p>{{ errorMsg }}</p>
            <p class="office-hint" v-if="isOldFormat">旧版 .{{ fileExt }} 格式暂不支持，请转换为 .{{ newFormat }} 后重试</p>
          </div>

          <!-- Excel: sheet tabs -->
          <div v-else-if="isExcel && sheets.length > 1" class="office-sheets">
            <button
              v-for="(name, idx) in sheets"
              :key="idx"
              class="office-sheet-tab"
              :class="{ active: activeSheet === idx }"
              @click="activeSheet = idx"
            >{{ name }}</button>
          </div>

          <!-- Content -->
          <div
            v-else-if="!loading && !error"
            class="office-content"
            :class="{ 'office-excel': isExcel }"
            v-html="htmlContent"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'
import mammoth from 'mammoth'
import * as XLSX from 'xlsx'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed(() => props.modelValue)
const fileExt = computed(() => (props.file?.fileType || '').toLowerCase())
const streamUrl = computed(() => {
  const id = props.file?.id
  return id ? `/api/file/stream/${id}` : ''
})

const isExcel = computed(() => ['xlsx', 'xls'].includes(fileExt.value))
const isWord = computed(() => ['docx', 'doc'].includes(fileExt.value))
const isOldFormat = computed(() => ['doc', 'xls'].includes(fileExt.value))
const newFormat = computed(() => fileExt.value === 'doc' ? 'docx' : fileExt.value === 'xls' ? 'xlsx' : '')

const fileTypeLabel = computed(() => {
  const m = { docx: 'Word', doc: 'Word(旧)', xlsx: 'Excel', xls: 'Excel(旧)' }
  return m[fileExt.value] || fileExt.value.toUpperCase()
})

const overlayRef = ref(null)
const loading = ref(true)
const error = ref(false)
const errorMsg = ref('')
const htmlContent = ref('')
const sheets = ref([])
const activeSheet = ref(0)

// Cache workbook for sheet switching
let cachedWorkbook = null

watch(() => props.modelValue, async (val) => {
  if (val) {
    await loadAndConvert()
    await nextTick()
    overlayRef.value?.focus()
  } else {
    reset()
  }
})

watch(activeSheet, () => {
  if (isExcel.value && cachedWorkbook && sheets.value.length > 0) {
    const sheet = cachedWorkbook.Sheets[sheets.value[activeSheet.value]]
    htmlContent.value = XLSX.utils.sheet_to_html(sheet, { editable: false })
  }
})

function reset() {
  loading.value = true
  error.value = false
  errorMsg.value = ''
  htmlContent.value = ''
  sheets.value = []
  activeSheet.value = 0
  cachedWorkbook = null
}

async function loadAndConvert() {
  loading.value = true
  error.value = false
  htmlContent.value = ''
  sheets.value = []
  cachedWorkbook = null

  try {
    const resp = await fetch(streamUrl.value)
    if (!resp.ok) throw new Error('文件加载失败')
    const buffer = await resp.arrayBuffer()

    if (isWord.value) {
      await convertDocx(buffer)
    } else if (isExcel.value) {
      await convertXlsx(buffer)
    } else {
      throw new Error('不支持的格式')
    }
  } catch (e) {
    error.value = true
    errorMsg.value = e.message || '文档解析失败'
    loading.value = false
  }
}

async function convertDocx(buffer) {
  try {
    const result = await mammoth.convertToHtml({ arrayBuffer: buffer })
    htmlContent.value = result.value
    if (result.messages?.length) {
      console.warn('Mammoth warnings:', result.messages)
    }
    loading.value = false
  } catch (e) {
    throw new Error('Word 文档解析失败: ' + (e.message || ''))
  }
}

async function convertXlsx(buffer) {
  try {
    cachedWorkbook = XLSX.read(buffer, { type: 'array' })
    sheets.value = cachedWorkbook.SheetNames
    if (sheets.value.length === 0) throw new Error('Excel 文件中没有工作表')
    const sheet = cachedWorkbook.Sheets[sheets.value[0]]
    htmlContent.value = XLSX.utils.sheet_to_html(sheet, { editable: false })
    loading.value = false
  } catch (e) {
    throw new Error('Excel 文档解析失败: ' + (e.message || ''))
  }
}

function onKeydown(e) {
  if (!visible.value) return
  if (e.key === 'Escape') close()
}

function close() {
  emit('update:modelValue', false)
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.office-overlay {
  position: fixed;
  inset: 0;
  z-index: 9996;
  background: #fff;
  display: flex;
  flex-direction: column;
  outline: none;
}

.office-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 44px;
  min-height: 44px;
  background: #f6f8fa;
  border-bottom: 1px solid #e1e4e8;
  z-index: 2;
}
.office-name {
  font-size: 13px;
  font-weight: 500;
  color: #24292e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}
.office-type {
  font-size: 11px;
  color: #0366d6;
  background: rgba(3,102,214,0.08);
  padding: 1px 8px;
  border-radius: 3px;
  text-transform: uppercase;
  font-weight: 600;
}
.office-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.office-btn {
  height: 28px;
  border: none;
  background: transparent;
  color: #586069;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  padding: 0 10px;
  transition: all 0.15s;
  display: flex;
  align-items: center;
}
.office-btn:hover { background: rgba(0,0,0,0.06); color: #24292e; }
.office-btn-close:hover { background: rgba(255,80,80,0.15) !important; color: #d73a49; }

.office-body {
  flex: 1;
  overflow: auto;
}

.office-content {
  padding: 32px 48px;
  max-width: 900px;
  margin: 0 auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 14px;
  line-height: 1.8;
  color: #24292e;
}
.office-content :deep(h1) { font-size: 1.8em; margin: 0.67em 0; }
.office-content :deep(h2) { font-size: 1.5em; margin: 0.75em 0; }
.office-content :deep(h3) { font-size: 1.2em; margin: 0.83em 0; }
.office-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}
.office-content :deep(td), .office-content :deep(th) {
  border: 1px solid #e1e4e8;
  padding: 6px 12px;
  text-align: left;
}
.office-content :deep(th) { background: #f6f8fa; font-weight: 600; }
.office-content :deep(img) { max-width: 100%; }

/* Excel specific */
.office-excel {
  max-width: none;
  padding: 0;
  overflow: auto;
}
.office-excel :deep(table) {
  font-size: 13px;
}
.office-excel :deep(td) {
  white-space: nowrap;
  min-width: 60px;
}

/* Sheet tabs */
.office-sheets {
  display: flex;
  gap: 0;
  padding: 0 16px;
  background: #f6f8fa;
  border-bottom: 1px solid #e1e4e8;
  overflow-x: auto;
}
.office-sheet-tab {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: #586069;
  font-size: 12px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
  white-space: nowrap;
}
.office-sheet-tab:hover { color: #24292e; }
.office-sheet-tab.active {
  color: #0366d6;
  border-bottom-color: #0366d6;
  font-weight: 600;
}

.office-status {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #999;
  font-size: 15px;
  text-align: center;
}
.office-error { color: #d73a49; }
.office-hint { font-size: 13px; color: #666; margin-top: 8px; }

.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.2s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .office-content { padding: 16px 20px; }
  .office-name { max-width: 140px; }
}
</style>
