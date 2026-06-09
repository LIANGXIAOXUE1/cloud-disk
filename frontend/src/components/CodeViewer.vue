<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="code-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown="onKeydown"
        @click.self="close"
      >
        <!-- Header -->
        <div class="code-header">
          <span class="code-name" :title="file?.fileName">{{ file?.fileName || '文本预览' }}</span>
          <span class="code-lang">{{ detectedLang }}</span>
          <span class="code-info" v-if="content">共 {{ lineCount }} 行 · {{ formatSize(content.length) }}</span>
          <div class="code-actions">
            <button class="code-btn" @click="copyContent" title="复制全文">
              <span v-if="copied">✓ 已复制</span>
              <span v-else>📋 复制</span>
            </button>
            <button class="code-btn code-btn-close" @click="close" title="关闭 (Esc)">✕</button>
          </div>
        </div>

        <!-- Body -->
        <div class="code-body" ref="scrollRef">
          <!-- Loading -->
          <div v-if="loading" class="code-status">加载中...</div>

          <!-- Error -->
          <div v-else-if="error" class="code-status code-error">文件加载失败</div>

          <!-- Code display -->
          <div v-else class="code-display">
            <table class="code-table">
              <tbody>
                <tr v-for="(line, idx) in lines" :key="idx">
                  <td class="code-line-num">{{ idx + 1 }}</td>
                  <td class="code-line-content" v-html="line"></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'
import hljs from 'highlight.js/lib/core'
import 'highlight.js/styles/github.css'

// Register common languages — add more as needed
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import markdown from 'highlight.js/lib/languages/markdown'
import css from 'highlight.js/lib/languages/css'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import plaintext from 'highlight.js/lib/languages/plaintext'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('css', css)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('plaintext', plaintext)
// aliases
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('py', python)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('txt', plaintext)

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed(() => props.modelValue)
const streamUrl = computed(() => {
  const id = props.file?.id
  return id ? `/api/file/stream/${id}` : ''
})
const fileExt = computed(() => {
  return (props.file?.fileType || '').toLowerCase()
})

const overlayRef = ref(null)
const scrollRef = ref(null)
const loading = ref(true)
const error = ref(false)
const content = ref('')
const lines = ref([])
const detectedLang = ref('')
const copied = ref(false)

const lineCount = computed(() => lines.value.length)

// ── Language detection ──
const EXT_LANG_MAP = {
  txt: 'plaintext', log: 'plaintext', cfg: 'plaintext', ini: 'plaintext',
  md: 'markdown', markdown: 'markdown',
  json: 'json',
  xml: 'xml', html: 'xml', htm: 'xml', svg: 'xml',
  yaml: 'yaml', yml: 'yaml',
  py: 'python',
  java: 'java',
  js: 'javascript', jsx: 'javascript', mjs: 'javascript', cjs: 'javascript',
  ts: 'typescript', tsx: 'typescript',
  css: 'css', scss: 'css', less: 'css',
  sql: 'sql',
  sh: 'bash', bat: 'bash', ps1: 'bash',
  c: 'c', cpp: 'cpp', cs: 'csharp', go: 'go', rs: 'rust',
  php: 'php', rb: 'ruby', swift: 'swift', kt: 'kotlin'
}

// ── Lifecycle ──
watch(() => props.modelValue, async (val) => {
  if (val) {
    await loadContent()
    await nextTick()
    overlayRef.value?.focus()
  } else {
    reset()
  }
})

function reset() {
  content.value = ''
  lines.value = []
  loading.value = true
  error.value = false
  copied.value = false
}

async function loadContent() {
  loading.value = true
  error.value = false
  content.value = ''
  lines.value = []
  detectedLang.value = EXT_LANG_MAP[fileExt.value] || fileExt.value || 'plaintext'

  try {
    const response = await fetch(streamUrl.value)
    if (!response.ok) throw new Error('Failed to load')
    const text = await response.text()
    content.value = text

    // Highlight
    const lang = detectedLang.value
    const highlighted = hljs.highlight(text, { language: lang, ignoreIllegals: true }).value
    // Split into lines and preserve HTML
    lines.value = highlighted.split('\n').map(line => line || '&nbsp;')

    loading.value = false
  } catch (e) {
    loading.value = false
    error.value = true
  }
}

// ── Copy ──
async function copyContent() {
  try {
    await navigator.clipboard.writeText(content.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch (_) {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = content.value
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }
}

// ── Keyboard ──
function onKeydown(e) {
  if (!visible.value) return
  if (e.key === 'Escape') close()
}

// ── Close ──
function close() {
  emit('update:modelValue', false)
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.code-overlay {
  position: fixed;
  inset: 0;
  z-index: 9997;
  background: #fff;
  display: flex;
  flex-direction: column;
  outline: none;
}

.code-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 44px;
  min-height: 44px;
  background: #f6f8fa;
  border-bottom: 1px solid #e1e4e8;
  color: #24292e;
  z-index: 2;
}
.code-name {
  font-size: 13px;
  font-weight: 500;
  color: #24292e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
}
.code-lang {
  font-size: 11px;
  color: #0366d6;
  background: rgba(3,102,214,0.08);
  padding: 1px 8px;
  border-radius: 3px;
  text-transform: uppercase;
  font-weight: 600;
}
.code-info {
  font-size: 11px;
  color: #6a737d;
  flex: 1;
}
.code-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.code-btn {
  height: 28px;
  border: none;
  background: transparent;
  color: #586069;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  padding: 0 10px;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.code-btn:hover { background: rgba(0,0,0,0.06); color: #24292e; }
.code-btn-close:hover { background: rgba(255,80,80,0.15) !important; color: #d73a49; }

.code-body {
  flex: 1;
  overflow: auto;
}

.code-display {
  padding: 8px 0;
}

.code-table {
  border-collapse: collapse;
  width: 100%;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.code-line-num {
  width: 1%;
  min-width: 48px;
  padding: 0 12px 0 16px;
  text-align: right;
  color: #959da5;
  user-select: none;
  vertical-align: top;
  border-right: 1px solid #e1e4e8;
}

.code-line-content {
  padding: 0 16px;
  white-space: pre;
  vertical-align: top;
}

.code-table tr:hover {
  background: rgba(0,0,0,0.03);
}

/* highlight.js theme overrides */
:deep(.hljs) {
  background: transparent;
  padding: 0;
}

.code-status {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #999;
  font-size: 15px;
}
.code-error { color: #d73a49; }

/* transitions */
.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.2s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .code-name { max-width: 120px; }
  .code-line-num { min-width: 36px; padding: 0 8px 0 10px; font-size: 11px; }
  .code-table { font-size: 12px; }
}
</style>
