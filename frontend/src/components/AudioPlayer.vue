<template>
  <Teleport to="body">
    <Transition name="viewer-fade">
      <div
        v-if="visible"
        class="audio-overlay"
        tabindex="0"
        ref="overlayRef"
        @keydown.escape="close"
        @click.self="close"
      >
        <div class="audio-card" @click.stop>
          <!-- Header -->
          <div class="audio-header">
            <span class="audio-icon">🎵</span>
            <span class="audio-name" :title="file?.fileName">{{ file?.fileName || '音频播放' }}</span>
            <button class="audio-close" @click="close" title="关闭 (Esc)">✕</button>
          </div>

          <!-- Status -->
          <div v-if="loading" class="audio-status">加载中...</div>
          <div v-else-if="error" class="audio-status audio-error">音频加载失败</div>

          <!-- Player -->
          <audio
            ref="audioRef"
            class="video-js vjs-big-play-centered"
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
const audioRef = ref(null)
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
  if (!audioRef.value || !streamUrl.value) return
  loading.value = true
  error.value = false

  try {
    player = videojs(audioRef.value, {
      controls: true,
      autoplay: false,
      preload: 'auto',
      fluid: false,
      width: 600,
      height: 60,
      playbackRates: [0.5, 1, 1.25, 1.5, 2],
      controlBar: {
        volumePanel: { inline: false },
        children: [
          'playToggle',
          'progressControl',
          'currentTimeDisplay',
          'timeDivider',
          'durationDisplay',
          'volumePanel',
          'playbackRateMenuButton'
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
  const types = { mp3: 'audio/mpeg', wav: 'audio/wav', ogg: 'audio/ogg', oga: 'audio/ogg', flac: 'audio/flac', aac: 'audio/aac', m4a: 'audio/mp4', wma: 'audio/x-ms-wma' }
  return types[ext] || 'audio/mpeg'
}

function close() {
  emit('update:modelValue', false)
}

onUnmounted(() => { destroyPlayer() })
</script>

<style scoped>
.audio-overlay {
  position: fixed;
  inset: 0;
  z-index: 9994;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.audio-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  width: 640px;
  max-width: 94vw;
  padding: 20px 24px 16px;
}

.audio-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.audio-icon { font-size: 20px; }
.audio-name {
  font-size: 14px;
  font-weight: 500;
  color: #24292e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.audio-close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #999;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.audio-close:hover { background: #fee; color: #d73a49; }

.audio-status {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 16px 0;
}
.audio-error { color: #d73a49; }

/* Video.js audio mode — compact bar */
.audio-card :deep(.video-js) {
  border-radius: 8px;
  overflow: hidden;
}

.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 0.2s ease; }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }
</style>
