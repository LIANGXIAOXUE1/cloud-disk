<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="pdf-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown="onKeydown"
      >
        <!-- Toolbar -->
        <div class="pdf-toolbar">
          <span class="pdf-name" :title="file?.fileName">{{ file?.fileName || 'PDF' }}</span>

          <div class="pdf-controls">
            <!-- Nav -->
            <button class="pdf-btn" @click="prevPage" :disabled="currentPage <= 1">◀</button>
            <input
              class="pdf-page-input"
              type="number"
              :value="currentPage"
              :min="1"
              :max="totalPages"
              @change="jumpToPage"
            />
            <span class="pdf-page-total">/ {{ totalPages }}</span>
            <button class="pdf-btn" @click="nextPage" :disabled="currentPage >= totalPages">▶</button>

            <span class="pdf-sep">|</span>

            <!-- Zoom -->
            <button class="pdf-btn" @click="zoomOut">−</button>
            <span class="pdf-zoom-label">{{ Math.round(scale * 100) }}%</span>
            <button class="pdf-btn" @click="zoomIn">+</button>
            <button class="pdf-btn" @click="zoomFit" title="适应宽度">⊡</button>

            <span class="pdf-sep">|</span>

            <!-- Search -->
            <button class="pdf-btn" @click="toggleSearch" :class="{ active: showSearch }" title="搜索">🔍</button>

            <!-- TOC -->
            <button v-if="outline.length" class="pdf-btn" @click="showToc = !showToc" :class="{ active: showToc }" title="目录">☰</button>

            <button class="pdf-btn pdf-btn-close" @click="close" title="关闭 (Esc)">✕</button>
          </div>
        </div>

        <!-- Search panel -->
        <div v-if="showSearch" class="pdf-search-panel">
          <input
            ref="searchInput"
            v-model="searchQuery"
            class="pdf-search-input"
            placeholder="搜索关键词..."
            @keydown.enter="doSearch"
          />
          <span class="pdf-search-result" v-if="searchResults.total > 0">
            {{ searchResults.current }} / {{ searchResults.total }}
          </span>
          <span class="pdf-search-result" v-else-if="searched">无匹配结果</span>
          <button class="pdf-btn" @click="prevSearch" :disabled="searchResults.total === 0">▲</button>
          <button class="pdf-btn" @click="nextSearch" :disabled="searchResults.total === 0">▼</button>
        </div>

        <!-- Body -->
        <div class="pdf-body" ref="scrollContainer" @scroll.passive="onScroll">
          <!-- Loading -->
          <div v-if="loading" class="pdf-status">
            <span v-if="loadProgress >= 0">加载中 {{ loadProgress }}%</span>
            <span v-else>加载中...</span>
          </div>

          <!-- Error -->
          <div v-else-if="error" class="pdf-status pdf-error">PDF 加载失败</div>

          <!-- Pages -->
          <div v-else class="pdf-pages" :style="{ width: canvasWidth + 'px' }">
            <div
              v-for="pageNum in renderedPages"
              :key="pageNum"
              class="pdf-page-wrapper"
              :ref="el => setPageRef(pageNum, el)"
            >
              <canvas :ref="el => setCanvasRef(pageNum, el)" class="pdf-canvas" />
              <span class="pdf-page-num">{{ pageNum }}</span>
            </div>
          </div>
        </div>

        <!-- TOC sidebar -->
        <Transition name="slide">
          <div v-if="showToc && outline.length" class="pdf-toc">
            <h4>目录</h4>
            <div
              v-for="(item, idx) in outline"
              :key="idx"
              class="pdf-toc-item"
              :style="{ paddingLeft: (item.depth || 1) * 12 + 'px' }"
              @click="goToOutline(item)"
            >
              {{ item.title }}
              <span class="pdf-toc-page">{{ item.pageNumber }}</span>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'

let pdfjsLib = null

// ── Lazy load pdfjs-dist (avoids top-level await build error) ──
async function ensurePdfJs() {
  if (pdfjsLib) return pdfjsLib
  const mod = await import('pdfjs-dist')
  pdfjsLib = mod
  pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/pdf.worker.min.mjs'
  return pdfjsLib
}

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

// ── state ──
const overlayRef = ref(null)
const scrollContainer = ref(null)
const loading = ref(true)
const loadProgress = ref(-1)
const error = ref(false)

let pdfDoc = null
const totalPages = ref(0)
const currentPage = ref(1)
const scale = ref(1.5)
const canvasWidth = ref(800)

const renderedPages = ref([])    // pages currently rendered
const pageRefs = {}
const canvasRefs = {}

// search
const showSearch = ref(false)
const searchInput = ref(null)
const searchQuery = ref('')
const searched = ref(false)
const searchResults = ref({ total: 0, current: 0, matches: [] })
let currentSearchIdx = -1

// TOC
const showToc = ref(false)
const outline = ref([])

// ── computed ──
// dummy for template (handled in script setup)

// ── lifecycle ──
watch(() => props.modelValue, async (val) => {
  if (val && streamUrl.value) {
    await loadPdf()
    await nextTick()
    overlayRef.value?.focus()
  } else if (!val) {
    destroyPdf()
  }
})

function destroyPdf() {
  pdfDoc?.destroy()
  pdfDoc = null
  totalPages.value = 0
  currentPage.value = 1
  renderedPages.value = []
  loading.value = true
  loadProgress.value = -1
  error.value = false
  showSearch.value = false
  showToc.value = false
  searchQuery.value = ''
  searched.value = false
  searchResults.value = { total: 0, current: 0, matches: [] }
  outline.value = []
  canvasRefs = {}
  pageRefs = {}
}

// ── PDF loading ──
async function loadPdf() {
  loading.value = true
  loadProgress.value = 0
  error.value = false

  try {
    const pdfjs = await ensurePdfJs()

    const loadingTask = pdfjs.getDocument({
      url: streamUrl.value,
      cMapUrl: 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/cmaps/',
      cMapPacked: true
    })

    loadingTask.onProgress = (data) => {
      if (data.total > 0) {
        loadProgress.value = Math.round((data.loaded / data.total) * 100)
      }
    }

    pdfDoc = await loadingTask.promise
    totalPages.value = pdfDoc.numPages
    loadProgress.value = 100

    // Load TOC
    try {
      const toc = await pdfDoc.getOutline()
      if (toc && toc.length) {
        outline.value = flattenOutline(toc)
      }
    } catch (_) { /* no TOC */ }

    loading.value = false

    await nextTick()
    renderVisiblePages()
  } catch (e) {
    loading.value = false
    error.value = true
  }
}

function flattenOutline(items, depth = 1) {
  let result = []
  for (const item of items) {
    result.push({
      title: item.title,
      pageNumber: item.dest ? null : null,
      dest: item.dest,
      depth
    })
    if (item.items && item.items.length) {
      result.push(...flattenOutline(item.items, depth + 1))
    }
  }
  // Resolve page numbers from destinations
  pdfDoc?.getPageIndex && result.forEach(async (r) => {
    if (r.dest && !r.pageNumber) {
      try {
        const idx = await pdfDoc.getPageIndex(r.dest[0])
        r.pageNumber = idx + 1
      } catch (_) { r.pageNumber = '?' }
    }
  })
  return result
}

// ── rendering ──
async function renderVisiblePages() {
  if (!pdfDoc || loading.value || error.value) return

  const container = scrollContainer.value
  if (!container) return

  const containerTop = container.scrollTop
  const containerHeight = container.clientHeight
  const containerBottom = containerTop + containerHeight

  // Determine which pages are visible
  const estimatedPageHeight = canvasWidth.value * 1.4 * scale.value / 1.5 + 30
  const firstVisible = Math.max(1, Math.floor(containerTop / estimatedPageHeight) - 1)
  const lastVisible = Math.min(totalPages.value, Math.ceil(containerBottom / estimatedPageHeight) + 1)

  const pagesToRender = []
  for (let i = firstVisible; i <= lastVisible; i++) {
    if (!renderedPages.value.includes(i)) {
      pagesToRender.push(i)
    }
  }

  // Remove pages far out of view (keep a buffer)
  const minRender = Math.max(1, firstVisible - 3)
  const maxRender = Math.min(totalPages.value, lastVisible + 3)
  renderedPages.value = renderedPages.value.filter(p => p >= minRender && p <= maxRender)
  for (let i = firstVisible; i <= lastVisible; i++) {
    if (!renderedPages.value.includes(i)) {
      renderedPages.value.push(i)
    }
  }
  renderedPages.value.sort((a, b) => a - b)

  await nextTick()

  // Render each new page
  for (const pageNum of pagesToRender) {
    await renderPage(pageNum)
  }
}

async function renderPage(pageNum) {
  const canvas = canvasRefs[pageNum]
  if (!canvas || !pdfDoc) return

  try {
    const page = await pdfDoc.getPage(pageNum)
    const viewport = page.getViewport({ scale: scale.value })

    canvas.width = viewport.width
    canvas.height = viewport.height
    canvas.style.width = viewport.width + 'px'
    canvas.style.height = viewport.height + 'px'
    canvasWidth.value = Math.max(canvasWidth.value, viewport.width)

    const ctx = canvas.getContext('2d')
    await page.render({ canvasContext: ctx, viewport }).promise
  } catch (_) { /* page render error */ }
}

function onScroll() {
  renderVisiblePages()
  // Update current page based on scroll position
  updateCurrentPageFromScroll()
}

function updateCurrentPageFromScroll() {
  const container = scrollContainer.value
  if (!container) return
  const midPoint = container.scrollTop + container.clientHeight / 2
  // Find which page wrapper's center is closest to viewport center
  let closest = 1
  let minDist = Infinity
  for (let i = 1; i <= totalPages.value; i++) {
    const el = pageRefs[i]
    if (!el) continue
    const dist = Math.abs(el.offsetTop + el.offsetHeight / 2 - midPoint)
    if (dist < minDist) {
      minDist = dist
      closest = i
    }
  }
  if (currentPage.value !== closest) {
    currentPage.value = closest
  }
}

// Re-render all on zoom change
watch(scale, async () => {
  if (!pdfDoc || loading.value) return
  const allPages = [...renderedPages.value]
  renderedPages.value = []
  await nextTick()
  for (const p of allPages) {
    renderedPages.value.push(p)
  }
  await nextTick()
  for (const p of allPages) {
    await renderPage(p)
  }
  // Scroll to current page after zoom
  const el = pageRefs[currentPage.value]
  el?.scrollIntoView({ block: 'start' })
})

// ── navigation ──
function prevPage() {
  if (currentPage.value <= 1) return
  const el = pageRefs[currentPage.value - 1]
  el?.scrollIntoView({ block: 'start' })
}
function nextPage() {
  if (currentPage.value >= totalPages.value) return
  const el = pageRefs[currentPage.value + 1]
  el?.scrollIntoView({ block: 'start' })
}
function jumpToPage(e) {
  const v = parseInt(e.target.value)
  if (v >= 1 && v <= totalPages.value) {
    const el = pageRefs[v]
    el?.scrollIntoView({ block: 'start' })
  }
}
function goToOutline(item) {
  const p = item.pageNumber
  if (p && p >= 1 && p <= totalPages.value) {
    const el = pageRefs[p]
    el?.scrollIntoView({ block: 'start' })
    showToc.value = false
  }
}

// ── zoom ──
function zoomIn() { scale.value = Math.min(4, scale.value + 0.25) }
function zoomOut() { scale.value = Math.max(0.5, scale.value - 0.25) }
function zoomFit() { scale.value = 1.5 }

// ── search ──
function toggleSearch() {
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    searchQuery.value = ''
    searched.value = false
    searchResults.value = { total: 0, current: 0, matches: [] }
    nextTick(() => searchInput.value?.focus())
  }
}

async function doSearch() {
  const q = searchQuery.value.trim()
  if (!q || !pdfDoc) return

  searchResults.value = { total: 0, current: 0, matches: [] }
  currentSearchIdx = -1

  // Simple search: find text on each page, count matches
  // (Full text layer search with highlighting would require text layer rendering, which is complex.
  //  This simplified version navigates to pages containing the search term.)
  const matches = []
  for (let i = 1; i <= totalPages.value; i++) {
    try {
      const page = await pdfDoc.getPage(i)
      const textContent = await page.getTextContent()
      const text = textContent.items.map(it => it.str).join(' ')
      if (text.toLowerCase().includes(q.toLowerCase())) {
        matches.push(i)
      }
    } catch (_) {}
  }

  searchResults.value = {
    total: matches.length,
    current: matches.length > 0 ? 1 : 0,
    matches
  }
  searched.value = true

  if (matches.length > 0) {
    currentSearchIdx = 0
    const el = pageRefs[matches[0]]
    el?.scrollIntoView({ block: 'start' })
  }
}

function nextSearch() {
  if (searchResults.value.total === 0) return
  currentSearchIdx = (currentSearchIdx + 1) % searchResults.value.total
  const p = searchResults.value.matches[currentSearchIdx]
  const el = pageRefs[p]
  el?.scrollIntoView({ block: 'start' })
  searchResults.value.current = currentSearchIdx + 1
}

function prevSearch() {
  if (searchResults.value.total === 0) return
  currentSearchIdx = (currentSearchIdx - 1 + searchResults.value.total) % searchResults.value.total
  const p = searchResults.value.matches[currentSearchIdx]
  const el = pageRefs[p]
  el?.scrollIntoView({ block: 'start' })
  searchResults.value.current = currentSearchIdx + 1
}

// ── keyboard ──
function onKeydown(e) {
  if (!visible.value) return
  if (e.key === 'Escape') {
    if (showSearch.value) { showSearch.value = false; return }
    if (showToc.value) { showToc.value = false; return }
    close()
  }
  if (e.target.tagName === 'INPUT') return
  if (e.key === 'ArrowLeft') prevPage()
  if (e.key === 'ArrowRight') nextPage()
}

// ── ref helpers ──
function setPageRef(num, el) { if (el) pageRefs[num] = el }
function setCanvasRef(num, el) { if (el) canvasRefs[num] = el }

// ── close ──
function close() {
  emit('update:modelValue', false)
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  destroyPdf()
})
</script>

<style scoped>
.pdf-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(35, 35, 38, 0.96);
  display: flex;
  flex-direction: column;
  outline: none;
}

/* toolbar */
.pdf-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  height: 48px;
  min-height: 48px;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(10px);
  color: #ccc;
  z-index: 3;
}
.pdf-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
  flex-shrink: 0;
}
.pdf-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}
.pdf-sep { color: rgba(255,255,255,0.2); margin: 0 4px; }
.pdf-btn {
  height: 32px;
  min-width: 32px;
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.7);
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  padding: 0 6px;
  transition: all 0.15s;
}
.pdf-btn:hover:not(:disabled) { background: rgba(255,255,255,0.12); color: #fff; }
.pdf-btn:disabled { opacity: 0.3; cursor: default; }
.pdf-btn.active { background: rgba(255,255,255,0.15); color: #fff; }
.pdf-btn-close:hover { background: rgba(255,80,80,0.4) !important; }
.pdf-page-input {
  width: 44px;
  height: 28px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  text-align: center;
  font-size: 13px;
  outline: none;
}
.pdf-page-input:focus { border-color: rgba(255,255,255,0.4); }
.pdf-page-total { font-size: 12px; color: rgba(255,255,255,0.5); }
.pdf-zoom-label { font-size: 12px; color: rgba(255,255,255,0.5); min-width: 36px; text-align: center; }

/* search panel */
.pdf-search-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(0,0,0,0.5);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  z-index: 2;
}
.pdf-search-input {
  width: 220px;
  height: 30px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  padding: 0 10px;
  font-size: 13px;
  outline: none;
}
.pdf-search-input:focus { border-color: rgba(255,255,255,0.5); }
.pdf-search-result { font-size: 12px; color: rgba(255,255,255,0.5); }

/* body */
.pdf-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  display: flex;
  justify-content: center;
}
.pdf-pages {
  padding: 24px 0 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.pdf-page-wrapper {
  box-shadow: 0 2px 12px rgba(0,0,0,0.4);
  position: relative;
  background: #fff;
}
.pdf-canvas {
  display: block;
}
.pdf-page-num {
  position: absolute;
  bottom: 4px;
  right: 8px;
  font-size: 10px;
  color: #999;
  user-select: none;
}

/* status */
.pdf-status {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255,255,255,0.5);
  font-size: 16px;
}
.pdf-error { color: rgba(255,120,120,0.7); }

/* TOC sidebar */
.pdf-toc {
  position: absolute;
  top: 48px;
  right: 0;
  width: 280px;
  max-height: calc(100% - 48px);
  overflow-y: auto;
  background: rgba(20,20,22,0.95);
  border-left: 1px solid rgba(255,255,255,0.08);
  padding: 16px;
  z-index: 2;
}
.pdf-toc h4 {
  color: #ccc;
  font-size: 14px;
  margin-bottom: 12px;
  font-weight: 600;
}
.pdf-toc-item {
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  display: flex;
  justify-content: space-between;
  transition: background 0.1s;
}
.pdf-toc-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
.pdf-toc-page { color: rgba(255,255,255,0.3); font-size: 11px; flex-shrink: 0; margin-left: 8px; }

/* transitions */
.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.2s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: transform 0.2s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

@media (max-width: 768px) {
  .pdf-name { max-width: 100px; }
  .pdf-btn { height: 28px; min-width: 28px; font-size: 12px; padding: 0 4px; }
  .pdf-toc { width: 240px; }
}
</style>
