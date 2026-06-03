<template>
  <el-dialog v-model="visible" title="AI 摘要" width="600px" destroy-on-close @close="cancel">
    <div v-if="loading" style="text-align:center;padding:40px 0">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="margin-top:12px;color:#999">AI 正在分析文档内容...</p>
    </div>
    <div v-else-if="error" style="text-align:center;padding:40px 0;color:#d73a49">
      {{ error }}
    </div>
    <div v-else class="summary-content" v-html="renderedMd"></div>
    <template #footer>
      <el-button @click="cancel">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import request from '@/api/request'

const props = defineProps({
  modelValue: Boolean,
  fileId: Number
})
const emit = defineEmits(['update:modelValue'])
const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const loading = ref(true)
const error = ref('')
const summary = ref('')
const renderedMd = ref('')

watch(() => props.modelValue, async (val) => {
  if (val && props.fileId) {
    loading.value = true
    error.value = ''
    summary.value = ''
    try {
      const res = await request({ url: `/file/summary/${props.fileId}`, method: 'post' })
      summary.value = res.data || ''
      renderedMd.value = simpleMarkdown(res.data || '')
    } catch (e) {
      error.value = 'AI 服务暂时不可用，请稍后重试'
    }
    loading.value = false
  }
})

function cancel() {
  emit('update:modelValue', false)
}

function simpleMarkdown(md) {
  if (!md) return ''
  return md
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/# (.+)/g, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.+)/g, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/gs, m => `<ul>${m}</ul>`)
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.summary-content { line-height: 1.8; font-size: 14px; }
.summary-content :deep(h2) { font-size: 1.2em; margin: 12px 0 6px; }
.summary-content :deep(h3) { font-size: 1.1em; margin: 10px 0 4px; }
.summary-content :deep(h4) { font-size: 1em; margin: 8px 0 4px; }
.summary-content :deep(ul) { padding-left: 20px; margin: 8px 0; }
.summary-content :deep(li) { margin: 4px 0; }
.summary-content :deep(strong) { color: var(--primary-600); }
</style>
