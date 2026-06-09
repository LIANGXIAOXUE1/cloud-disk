<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="viewer-overlay"
        @click.self="close"
        @wheel.prevent="onWheel"
        tabindex="0"
        ref="overlayRef"
      >
        <!-- Header bar -->
        <div class="viewer-header">
          <span class="viewer-name">{{ currentImage?.fileName || '' }}</span>
          <span v-if="imageList.length > 1" class="viewer-counter">{{ currentIndex + 1 }} / {{ imageList.length }}</span>
          <div class="viewer-actions">
            <button class="viewer-btn" @click="zoomOut" title="缩小 (Ctrl+滚轮)"><ZoomOut /></button>
            <span class="viewer-zoom-label">{{ Math.round(scale * 100) }}%</span>
            <button class="viewer-btn" @click="zoomIn" title="放大 (Ctrl+滚轮)"><ZoomIn /></button>
            <button class="viewer-btn" @click="zoomReset" title="自适应">1:1</button>
            <button class="viewer-btn" @click="rotateLeft" title="逆时针旋转"><RefreshLeft /></button>
            <button class="viewer-btn" @click="rotateRight" title="顺时针旋转"><RefreshRight /></button>
            <button class="viewer-btn viewer-btn-close" @click="close" title="关闭 (Esc)"><Close /></button>
          </div>
        </div>

        <!-- Body -->
        <div class="viewer-body">
          <!-- Prev arrow -->
          <button
            v-if="imageList.length > 1"
            class="viewer-nav viewer-prev"
            @click.stop="prev"
            title="上一张 (←)"
          ><ArrowLeft /></button>

          <!-- Image area -->
          <div class="viewer-stage" @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp">
            <!-- Loading -->
            <div v-if="loading" class="viewer-status">
              <el-icon class="is-loading" :size="40"><Loading /></el-icon>
              <span>加载中...</span>
            </div>

            <!-- Error -->
            <div v-else-if="error" class="viewer-status viewer-error-status">
              <PictureFilled :size="48" />
              <span>图片加载失败</span>
            </div>

            <!-- Image -->
            <img
              v-show="!loading && !error"
              :key="currentKey"
              :src="currentSrc"
              :style="imageStyle"
              :alt="currentImage?.fileName"
              draggable="false"
              @load="onImageLoad"
              @error="onImageError"
            />
          </div>

          <!-- Next arrow -->
          <button
            v-if="imageList.length > 1"
            class="viewer-nav viewer-next"
            @click.stop="next"
            title="下一张 (→)"
          ><ArrowRight /></button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  ZoomIn, ZoomOut, RefreshLeft, RefreshRight, Close,
  ArrowLeft, ArrowRight, Loading, PictureFilled
} from '@element-plus/icons-vue'
import { getStreamUrl } from '@/api/modules/file'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** Array of FileInfo objects */
  images: { type: Array, default: () => [] },
  /** Starting index in images array */
  initialIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue'])

// ── state ──
const overlayRef = ref(null)

const currentIndex = ref(0)
const scale = ref(1)
const rotation = ref(0)
const loading = ref(true)
const error = ref(false)

// drag/pan
const isDragging = ref(false)
const panX = ref(0)
const panY = ref(0)
let dragStartX = 0
let dragStartY = 0
let panStartX = 0
let panStartY = 0

// ── computed ──
const visible = computed(() => props.modelValue)

// Filter only image files from the list
const imageList = computed(() => {
  if (!props.images || !props.images.length) return []
  const imgExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico']
  return props.images.filter(f =>
    !f.isFolder && imgExts.includes((f.fileType || '').toLowerCase())
  )
})

const currentImage = computed(() => imageList.value[currentIndex.value] || null)
const currentKey = computed(() => currentImage.value?.id ?? currentIndex.value)
const currentSrc = computed(() => {
  const img = currentImage.value
  return img ? getStreamUrl(img.id) : ''
})

const imageStyle = computed(() => ({
  transform: `translate(${panX.value}px, ${panY.value}px) scale(${scale.value}) rotate(${rotation.value}deg)`,
  cursor: scale.value > 1 ? (isDragging.value ? 'grabbing' : 'grab') : 'default',
  transition: isDragging.value ? 'none' : 'transform 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94)'
}))

// ── zoom ──
const ZOOM_STEPS = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 2, 3, 4]
const MIN_ZOOM = 0.1
const MAX_ZOOM = 10

function zoomIn() { zoomTo(scale.value * 1.25) }
function zoomOut() { zoomTo(scale.value / 1.25) }
function zoomTo(target) {
  scale.value = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, target))
  clampPan()
}
function zoomReset() {
  scale.value = 1
  panX.value = 0
  panY.value = 0
  rotation.value = 0
}
function onWheel(e) {
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  zoomTo(scale.value * delta)
}

// ── rotate ──
function rotateRight() { rotation.value = (rotation.value + 90) % 360 }
function rotateLeft() { rotation.value = (rotation.value - 90) % 360 }

// ── navigation ──
function prev() {
  if (imageList.value.length <= 1) return
  resetImage()
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : imageList.value.length - 1
}
function next() {
  if (imageList.value.length <= 1) return
  resetImage()
  currentIndex.value = currentIndex.value < imageList.value.length - 1 ? currentIndex.value + 1 : 0
}

// ── close ──
function close() {
  emit('update:modelValue', false)
}

// ── image events ──
function onImageLoad() {
  loading.value = false
  error.value = false
}
function onImageError() {
  loading.value = false
  error.value = true
}

function resetImage() {
  loading.value = true
  error.value = false
  scale.value = 1
  rotation.value = 0
  panX.value = 0
  panY.value = 0
}

// ── pan/drag (only when zoomed in) ──
function onMouseDown(e) {
  if (scale.value <= 1) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  panStartX = panX.value
  panStartY = panY.value
}
function onMouseMove(e) {
  if (!isDragging.value) return
  panX.value = panStartX + (e.clientX - dragStartX)
  panY.value = panStartY + (e.clientY - dragStartY)
}
function onMouseUp() {
  isDragging.value = false
}
function clampPan() {
  if (scale.value <= 1) {
    panX.value = 0
    panY.value = 0
  }
}

// ── keyboard ──
function onKeydown(e) {
  if (!visible.value) return
  switch (e.key) {
    case 'Escape':
      close()
      break
    case 'ArrowLeft':
      e.preventDefault()
      prev()
      break
    case 'ArrowRight':
      e.preventDefault()
      next()
      break
    case ' ':
      e.preventDefault()
      if (scale.value > 1) zoomReset()
      else zoomTo(2)
      break
    case '+':
    case '=':
      zoomIn()
      break
    case '-':
      zoomOut()
      break
  }
}

// ── watch visibility ──
watch(() => props.modelValue, async (val) => {
  if (val) {
    currentIndex.value = Math.min(props.initialIndex, imageList.value.length - 1)
    resetImage()
    await nextTick()
    overlayRef.value?.focus()
  }
})

watch(() => props.initialIndex, (val) => {
  currentIndex.value = Math.min(val, imageList.value.length - 1)
  resetImage()
})

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ── overlay ── */
.viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  flex-direction: column;
  outline: none;
}

/* ── header ── */
.viewer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  height: 52px;
  min-height: 52px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  color: #fff;
  z-index: 2;
}

.viewer-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.viewer-counter {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  flex: 1;
}

.viewer-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.viewer-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.15s;
}
.viewer-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}
.viewer-btn-close:hover {
  background: rgba(255, 80, 80, 0.3) !important;
}

.viewer-zoom-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  min-width: 40px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* ── body ── */
.viewer-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* ── nav arrows ── */
.viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 56px;
  height: 56px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  transition: all 0.15s;
  backdrop-filter: blur(4px);
}
.viewer-nav:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.viewer-prev { left: 16px; }
.viewer-next { right: 16px; }

/* ── stage ── */
.viewer-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.viewer-stage img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
  will-change: transform;
}

/* ── status (loading / error) ── */
.viewer-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
}
.viewer-error-status {
  color: rgba(255, 120, 120, 0.7);
}

/* ── transition ── */
.viewer-fade-enter-active,
.viewer-fade-leave-active {
  transition: opacity 0.25s ease;
}
.viewer-fade-enter-from,
.viewer-fade-leave-to {
  opacity: 0;
}

/* ── responsive ── */
@media (max-width: 768px) {
  .viewer-header {
    padding: 0 12px;
    height: 44px;
    min-height: 44px;
    gap: 8px;
  }
  .viewer-name { max-width: 140px; font-size: 13px; }
  .viewer-zoom-label { display: none; }
  .viewer-btn { width: 32px; height: 32px; font-size: 16px; }
  .viewer-nav { width: 40px; height: 40px; font-size: 24px; }
  .viewer-prev { left: 8px; }
  .viewer-next { right: 8px; }
}
</style>
