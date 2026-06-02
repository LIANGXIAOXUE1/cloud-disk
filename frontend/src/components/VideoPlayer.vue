<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="video-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown.escape="onEsc"
      >
        <!-- Close button, floating top-right -->
        <button class="video-close-btn" @click="close" title="关闭">✕</button>

        <!-- Loading / Error -->
        <div v-if="loading" class="video-status">加载中...</div>
        <div v-else-if="error" class="video-status video-error">视频加载失败</div>

        <!-- Video player -->
        <video
          ref="videoRef"
          class="video-js vjs-big-play-centered"
          crossorigin="anonymous"
        />
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
      fill: true,
      playbackRates: [0.5, 1, 1.25, 1.5, 2],
      userActions: {
        hotkeys: true
      },
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

    player.on('ready', () => { loading.value = false })
    player.on('error', () => { loading.value = false; error.value = true })
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

function onEsc() {
  if (document.fullscreenElement && player?.isFullscreen()) {
    player.exitFullscreen()
  } else {
    close()
  }
}

function close() {
  emit('update:modelValue', false)
}

onUnmounted(() => { destroyPlayer() })
</script>

<style scoped>
.video-overlay {
  position: fixed;
  inset: 0;
  z-index: 9995;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.video-close-btn {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.video-close-btn:hover {
  background: rgba(255, 80, 80, 0.6);
  color: #fff;
}

/* video.js fills the overlay */
.video-overlay :deep(.video-js) {
  width: 100%;
  height: 100%;
}

.video-status {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  z-index: 1;
}
.video-error { color: rgba(255, 120, 120, 0.7); }

.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.2s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }
</style>
