<template>
  <div class="storage-page">
    <h1 class="page-title">存储管理</h1>

    <!-- ============================================================
         Storage Overview
         ============================================================ -->
    <el-card shadow="hover" class="overview-card">
      <div class="overview-grid">
        <!-- Total -->
        <div class="overview-stat">
          <div class="stat-icon-box" style="background: var(--primary-100); color: var(--primary-600);">
            <el-icon :size="24"><Odometer /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">总存储空间</div>
            <div class="stat-value">{{ formatSize(totalStorage) }}</div>
          </div>
        </div>

        <!-- Used -->
        <div class="overview-stat">
          <div class="stat-icon-box" style="background: var(--warning-bg); color: var(--warning);">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">已使用空间</div>
            <div class="stat-value used">{{ formatSize(usedStorage) }}</div>
          </div>
        </div>

        <!-- Remaining -->
        <div class="overview-stat">
          <div class="stat-icon-box" style="background: var(--success-bg); color: var(--success);">
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">剩余可用空间</div>
            <div class="stat-value remaining">{{ formatSize(remainingStorage) }}</div>
          </div>
        </div>
      </div>

      <!-- Progress Bar -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">空间使用率</span>
          <span class="progress-percent">{{ usagePercent }}%</span>
        </div>
        <div class="progress-track">
          <div
            class="progress-fill"
            :style="{ width: usagePercent + '%' }"
            :class="{ 'nearing-limit': usagePercent > 80, 'critical': usagePercent > 95 }"
          ></div>
        </div>
        <div class="progress-legend">
          <span>已用 {{ formatSize(usedStorage) }}</span>
          <span>剩余 {{ formatSize(remainingStorage) }}</span>
        </div>
      </div>
    </el-card>

    <!-- ============================================================
         Storage Breakdown
         ============================================================ -->
    <el-row :gutter="20" class="breakdown-row">
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" class="breakdown-card">
          <template #header>
            <div class="breakdown-header">
              <div class="breakdown-icon" style="background: var(--primary-100);">
                <el-icon :size="20" color="var(--primary-500)"><Cloudy /></el-icon>
              </div>
              <span>免费云端网盘内存</span>
            </div>
          </template>
          <div class="breakdown-value">{{ formatSize(cloudStorage) }}</div>
          <div class="breakdown-detail">
            <div class="detail-row">
              <span>已使用</span>
              <span>{{ formatSize(cloudUsed) }}</span>
            </div>
            <el-progress
              :percentage="cloudUsedPercent"
              :stroke-width="8"
              :show-text="false"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" class="breakdown-card">
          <template #header>
            <div class="breakdown-header">
              <div class="breakdown-icon" style="background: var(--warning-bg);">
                <el-icon :size="20" color="var(--warning)"><Monitor /></el-icon>
              </div>
              <span>本地硬盘挂载内存</span>
            </div>
          </template>
          <div class="breakdown-value">{{ formatSize(localStorage) }}</div>
          <div class="breakdown-detail">
            <div class="detail-row">
              <span>已使用</span>
              <span>{{ formatSize(localUsed) }}</span>
            </div>
            <el-progress
              :percentage="localUsedPercent"
              :stroke-width="8"
              :show-text="false"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============================================================
         Beginner Guides
         ============================================================ -->
    <el-card shadow="hover" class="guide-card">
      <template #header>
        <div class="guide-card-title">
          <el-icon :size="20" color="var(--primary-500)"><Reading /></el-icon>
          <span>新手指引 — 快速上手</span>
        </div>
      </template>

      <div class="guide-list">
        <!-- Guide 1: Bind Local Disk -->
        <div class="guide-item">
          <div class="guide-number">01</div>
          <div class="guide-content">
            <h3 class="guide-title">如何绑定配置本地硬盘空间？</h3>
            <p class="guide-text">
              本地硬盘挂载可将您电脑中的指定文件夹映射为云盘存储空间。操作步骤如下：
            </p>
            <ul class="guide-steps">
              <li>点击右上角菜单进入「设置」或本页下方的「本地存储配置」面板</li>
              <li>选择「添加本地磁盘路径」，浏览并选择您希望挂载的文件夹</li>
              <li>系统将自动识别该文件夹的总空间与剩余空间，点击「确认挂载」即可完成</li>
              <li>挂载后，该文件夹内的文件将自动同步至云盘，您可在此统一管理</li>
            </ul>
            <div class="guide-tip">
              💡 <strong>提示：</strong>建议选择剩余空间充裕的磁盘分区作为挂载路径，避免频繁更换挂载目录。
            </div>
          </div>
        </div>

        <!-- Guide 2: Get More Cloud Storage -->
        <div class="guide-item">
          <div class="guide-number">02</div>
          <div class="guide-content">
            <h3 class="guide-title">如何领取、扩容免费云端存储空间？</h3>
            <p class="guide-text">
              平台为新用户提供 5GB 免费云端存储空间，您可通过以下方式获得更多空间：
            </p>
            <ul class="guide-steps">
              <li><strong>每日签到：</strong>连续签到可逐步扩容，第 7 天额外奖励 2GB</li>
              <li><strong>邀请好友：</strong>每成功邀请一位好友注册，双方各得 1GB 永久空间</li>
              <li><strong>参与活动：</strong>关注平台公告，不定期推出扩容福利活动</li>
              <li><strong>完成任务：</strong>在「任务中心」完成新手引导任务可获得额外存储奖励</li>
            </ul>
            <div class="guide-tip">
              💡 <strong>提示：</strong>当前免费额度可在本页顶部概览区域实时查看，空间不足时系统将发送提醒。
            </div>
          </div>
        </div>

        <!-- Guide 3: File Storage Explanation -->
        <div class="guide-item">
          <div class="guide-number">03</div>
          <div class="guide-content">
            <h3 class="guide-title">文件占用内存说明</h3>
            <p class="guide-text">
              以下类型的文件会占用您的云盘存储空间，请合理管理：
            </p>
            <ul class="guide-steps">
              <li><strong>上传文件：</strong>通过上传功能存入云盘的所有文件，均计入已用空间</li>
              <li><strong>转存文件：</strong>从其他网盘转存至当前云盘的文件，同样占用存储空间</li>
              <li><strong>分享副本：</strong>他人分享给您且您保存至自己网盘的文件</li>
              <li><strong>本地挂载同步：</strong>本地硬盘挂载路径下的文件会被索引统计，但不会重复占用云端物理存储</li>
            </ul>
            <div class="guide-note">
              ⚠️ <strong>注意：</strong>回收站中的文件仍会占用存储空间，彻底清空回收站后方可释放空间。
            </div>
          </div>
        </div>

        <!-- Guide 4: Cross-Disk Transfer -->
        <div class="guide-item">
          <div class="guide-number">04</div>
          <div class="guide-content">
            <h3 class="guide-title">跨盘转存教程 — 一键聚合网盘文件</h3>
            <p class="guide-text">
              您可以将分散在其他网盘（如百度网盘、阿里云盘等）的文件一键转存至当前云盘统一管理：
            </p>
            <ul class="guide-steps">
              <li><strong>第一步：</strong>在左侧菜单中进入「转存任务」页面</li>
              <li><strong>第二步：</strong>点击「新建任务」按钮，在弹出的窗口中粘贴源文件的分享链接</li>
              <li><strong>第三步：</strong>如有提取密码，请一并填入密码框</li>
              <li><strong>第四步：</strong>指定目标存储路径（如 /downloads），点击「创建」即可提交转存任务</li>
              <li><strong>第五步：</strong>系统将在后台自动下载并存储文件，您可在任务列表中实时查看进度</li>
            </ul>
            <div class="guide-tip">
              💡 <strong>提示：</strong>转存任务支持断点续传，网络中断后会自动恢复。您可同时提交多个转存任务并行处理。
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Odometer, TrendCharts, CircleCheck, Cloudy,
  Monitor, Reading
} from '@element-plus/icons-vue'

// --- Storage Stats (demo data — replace with API calls when available) ---

const totalStorage = ref(10 * 1024 * 1024 * 1024)   // 10 GB total
const usedStorage  = ref(2.4 * 1024 * 1024 * 1024)  // 2.4 GB used

const cloudStorage = ref(5 * 1024 * 1024 * 1024)    // 5 GB cloud
const cloudUsed    = ref(1.8 * 1024 * 1024 * 1024)  // 1.8 GB cloud used

const localStorage = ref(5 * 1024 * 1024 * 1024)    // 5 GB local
const localUsed    = ref(0.6 * 1024 * 1024 * 1024) // 0.6 GB local used

const remainingStorage = computed(() => totalStorage.value - usedStorage.value)
const usagePercent = computed(() => {
  if (totalStorage.value <= 0) return 0
  return Math.round((usedStorage.value / totalStorage.value) * 100)
})
const cloudUsedPercent = computed(() => {
  if (cloudStorage.value <= 0) return 0
  return Math.round((cloudUsed.value / cloudStorage.value) * 100)
})
const localUsedPercent = computed(() => {
  if (localStorage.value <= 0) return 0
  return Math.round((localUsed.value / localStorage.value) * 100)
})

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}
</script>

<style scoped>
.storage-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--neutral-800);
  margin-bottom: var(--space-6);
}

/* ============================================================
   Overview Card
   ============================================================ */
.overview-card {
  margin-bottom: var(--space-5);
  border: 1px solid var(--neutral-200);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
  margin-bottom: var(--space-6);
}

.overview-stat {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-page);
  border-radius: var(--radius-lg);
}

.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: var(--fs-sm);
  color: var(--neutral-500);
  font-weight: 500;
  margin-bottom: 2px;
}

.stat-value {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--neutral-800);
}

.stat-value.used {
  color: var(--warning);
}

.stat-value.remaining {
  color: var(--success);
}

/* Progress */
.progress-section {
  padding-top: var(--space-4);
  border-top: 1px solid var(--neutral-200);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.progress-label {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--neutral-600);
}

.progress-percent {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--primary-600);
}

.progress-track {
  height: 14px;
  background: var(--neutral-200);
  border-radius: 7px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-400), var(--primary-500));
  border-radius: 7px;
  transition: width 0.8s var(--ease-out);
  min-width: 0;
  position: relative;
}

.progress-fill.nearing-limit {
  background: linear-gradient(90deg, var(--warning), hsl(34, 28%, 48%));
}

.progress-fill.critical {
  background: linear-gradient(90deg, var(--error), hsl(2, 24%, 48%));
}

.progress-legend {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-2);
  font-size: var(--fs-xs);
  color: var(--neutral-400);
}

/* ============================================================
   Breakdown Cards
   ============================================================ */
.breakdown-row {
  margin-bottom: var(--space-5);
}

.breakdown-card {
  border: 1px solid var(--neutral-200);
  height: 100%;
}

.breakdown-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--neutral-800);
}

.breakdown-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.breakdown-value {
  font-size: var(--fs-2xl);
  font-weight: 700;
  color: var(--neutral-800);
  margin: var(--space-4) 0 var(--space-3);
}

.breakdown-detail {
  margin-top: var(--space-3);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-sm);
  color: var(--neutral-500);
  margin-bottom: var(--space-2);
}

/* ============================================================
   Guide Card
   ============================================================ */
.guide-card {
  border: 1px solid var(--neutral-200);
}

.guide-card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--neutral-800);
}

.guide-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.guide-item {
  display: flex;
  gap: var(--space-5);
}

.guide-number {
  width: 40px;
  height: 40px;
  background: var(--primary-50);
  color: var(--primary-600);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-lg);
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

.guide-content {
  flex: 1;
  min-width: 0;
}

.guide-title {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: var(--space-2);
}

.guide-text {
  font-size: var(--fs-sm);
  color: var(--neutral-500);
  line-height: var(--lh-relaxed);
  margin-bottom: var(--space-3);
}

.guide-steps {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--space-3);
}

.guide-steps li {
  position: relative;
  padding-left: var(--space-5);
  font-size: var(--fs-sm);
  color: var(--neutral-600);
  line-height: var(--lh-relaxed);
  margin-bottom: var(--space-2);
}

.guide-steps li::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 9px;
  width: 6px;
  height: 6px;
  background: var(--primary-400);
  border-radius: 50%;
}

.guide-tip {
  background: var(--primary-50);
  border-left: 3px solid var(--primary-400);
  padding: var(--space-3) var(--space-4);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  font-size: var(--fs-sm);
  color: var(--neutral-600);
  line-height: var(--lh-relaxed);
}

.guide-note {
  background: var(--warning-bg);
  border-left: 3px solid var(--warning);
  padding: var(--space-3) var(--space-4);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  font-size: var(--fs-sm);
  color: var(--neutral-600);
  line-height: var(--lh-relaxed);
}

.guide-tip strong,
.guide-note strong {
  color: var(--neutral-700);
}

/* ============================================================
   Responsive
   ============================================================ */
@media (max-width: 768px) {
  .page-title {
    font-size: var(--fs-xl);
    margin-bottom: var(--space-4);
  }

  .overview-grid {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .guide-item {
    flex-direction: column;
    gap: var(--space-3);
  }

  .guide-number {
    width: 32px;
    height: 32px;
    font-size: var(--fs-base);
  }
}
</style>
