<template>
  <!-- Simple line-art cloud disk logo — mint green themed -->
  <div class="cloud-logo" :class="{ collapsed: collapsed }">
    <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg" class="logo-svg">
      <!-- Cloud outline -->
      <path d="M30 72 C12 72 6 52 20 42 C16 24 38 16 50 28 C54 14 74 12 82 26 C100 24 110 42 96 52 C108 62 102 82 86 82 L34 82 C22 82 24 72 30 72Z"
            stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
      <!-- Inner disk platter -->
      <ellipse cx="58" cy="58" rx="24" ry="14" stroke="currentColor" stroke-width="4" stroke-linecap="round" />
      <ellipse cx="58" cy="58" rx="6" ry="4" fill="currentColor" />
      <!-- Read head -->
      <line x1="58" y1="46" x2="58" y2="24" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" />
      <!-- Up arrow -->
      <path d="M52 30 L58 22 L64 30" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <span v-if="!collapsed" class="logo-text">Cloud<span class="logo-accent">Disk</span></span>
  </div>
</template>

<script setup>
defineProps({
  collapsed: { type: Boolean, default: false }
})
</script>

<style scoped>
.cloud-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--primary-500);
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-smooth);
}
.cloud-logo.collapsed {
  gap: 0;
  justify-content: center;
}

.logo-svg {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-smooth);
}
.cloud-logo.collapsed .logo-svg {
  width: 34px;
  height: 34px;
}

.logo-text {
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--neutral-800);
  white-space: nowrap;
  letter-spacing: 0.02em;
  font-family: var(--font-family);
}
.logo-accent {
  color: var(--primary-500);
  font-weight: 600;
}
</style>
