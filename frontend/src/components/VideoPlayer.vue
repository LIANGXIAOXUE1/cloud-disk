<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="video-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown="onKeydown"
      >
        <!-- Header -->
        <div class="video-header">
          <span class="video-name" :title="file?.fileName">{{ file?.fileName || '视频播放' }}</span>
          <button class="video-btn video-btn-close" @click="close" title="关闭 (Esc)">✕</button>
        </div>

        <!-- Player -->
        <div class="video-body">
          <div v-if="loading" class="video-status">加载中...</div>
          <div v-else-if="error" class="video-status video-error">视频加载失败</div>
          <video
            ref="videoRef"
            class="video-js vjs-big-play-centered"
            controls
            preload="auto"
            playsinline
            crossorigin="anonymous"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted, computed } from 'vue'
import videojs from 'video.js'
import 'video.js/dist/video-js.css'

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

const overlayRef = ref(null)
const videoRef = ref(null)
const loading = ref(true)
const error = ref(false)
let player = null

watch(() => props.modelValue, async (val) => {
  if (val) {
    await nextTick()
    initPlayer()
    overlayRef.value?.focus()
  } else {
    destroyPlayer()
  }
})

function initPlayer() {
  if (!videoRef.value || !streamUrl.value) return
  loading.value = true
  error.value = false

  try {
    player = videojs(videoRef.value, {
      controls: true,
      autoplay: false,
      preload: 'auto',
      fluid: true,
      playbackRates: [0.5, 1, 1.25, 1.5, 2],
      controlBar: {
        children: [
          'playToggle',
          'volumePanel',
          'currentTimeDisplay',
          'timeDivider',
          'durationDisplay',
          'progressControl',
          'playbackRateMenuButton',
          'pictureInPictureToggle',
          'fullscreenToggle'
        ]
      },
      sources: [{
        src: streamUrl.value,
        type: getMimeType()
      }]
    })

    player.on('ready', () => {
      loading.value = false
    })

    player.on('error', () => {
      loading.value = false
      error.value = true
    })

    // Keyboard: seek with arrow keys
    player.on('keydown', (e) => {
      if (e.code === 'ArrowLeft') {
        e.preventDefault()
        player.currentTime(Math.max(0, player.currentTime() - 5))
      } else if (e.code === 'ArrowRight') {
        e.preventDefault()
        player.currentTime(Math.min(player.duration(), player.currentTime() + 5))
      }
    })
  } catch (e) {
    loading.value = false
    error.value = true
  }
}

function destroyPlayer() {
  if (player) {
    player.dispose()
    player = null
  }
  loading.value = true
  error.value = false
}

function getMimeType() {
  const ext = (props.file?.fileType || '').toLowerCase()
  const types = { mp4: 'video/mp4', webm: 'video/webm', ogg: 'video/ogg', ogv: 'video/ogg', mov: 'video/quicktime', avi: 'video/x-msvideo', mkv: 'video/x-matroska', flv: 'video/x-flv', wmv: 'video/x-ms-wmv' }
  return types[ext] || 'video/mp4'
}

function onKeydown(e) {
  if (!visible.value) return
  if (e.key === 'Escape') {
    // Don't close if exiting fullscreen
    if (document.fullscreenElement) return
    close()
  }
}

function close() {
  emit('update:modelValue', false)
}

onUnmounted(() => {
  destroyPlayer()
})
</script>

<style scoped>
.video-overlay {
  position: fixed;
  inset: 0;
  z-index: 9995;
  background: rgba(0, 0, 0, 0.94);
  display: flex;
  flex-direction: column;
  outline: none;
}

.video-header {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 44px;
  min-height: 44px;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2;
}
.video-name {
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
  flex: 1;
}
.video-btn {
  height: 32px;
  min-width: 32px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.video-btn:hover { background: rgba(255, 255, 255, 0.12); color: #fff; }
.video-btn-close:hover { background: rgba(255, 80, 80, 0.4) !important; }

.video-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.video-body :deep(.video-js) {
  max-width: 100%;
  max-height: calc(100vh - 100px);
  border-radius: 4px;
}

/* 确保控制栏内的按钮能完全展开 */
.video-body :deep(.vjs-control-bar) {
  flex-wrap: nowrap;
}

@media (max-width: 768px) {
  .video-body { padding: 0; align-items: flex-start; }
  .video-name { max-width: 180px; }
}
</style>
