<template>
  <div class="health-score-container">
    <div class="section-header">
      <h3 class="section-title">设备健康评分</h3>
      <div class="update-time" v-if="lastUpdate">
        <el-icon :size="12"><Timer /></el-icon>
        {{ formatTime(lastUpdate) }}
      </div>
    </div>

    <div v-if="scores.length === 0" class="no-data">
      <el-empty description="暂无健康评分数据" :image-size="50" />
    </div>

    <div class="devices-grid">
      <div
        v-for="score in scores"
        :key="score.deviceId"
        class="device-card"
        :class="`risk-${score.riskLevel.toLowerCase()}`"
        :style="{ borderTopColor: score.riskColor }"
      >
        <div class="device-header">
          <div class="device-info">
            <div class="device-name">
              <el-icon :size="16" :style="{ color: score.riskColor }">
                <Cpu v-if="score.deviceId.includes('plc')" />
                <Monitor v-else />
              </el-icon>
              {{ score.deviceName }}
            </div>
            <div class="device-desc" v-if="score.description">{{ score.description }}</div>
          </div>
          <el-tag
            :color="score.riskColor + '20'"
            :style="{ color: score.riskColor, borderColor: score.riskColor + '60' }"
            size="small"
            effect="dark"
            class="risk-tag"
          >
            {{ score.riskLevelLabel }}
          </el-tag>
        </div>

        <div class="score-section">
          <div class="score-gauge-wrap">
            <el-progress
              type="dashboard"
              :percentage="score.totalScore"
              :color="score.riskColor"
              :width="110"
              :stroke-width="10"
              :format="(p) => p"
            />
            <div class="score-label">综合评分</div>
          </div>

          <div class="dimensions-list">
            <div class="dimension-item">
              <div class="dim-left">
                <span class="dim-icon quality-icon">Q</span>
                <div class="dim-info">
                  <div class="dim-name">质量码评分</div>
                  <div class="dim-detail">{{ score.qualityScore.detail }}</div>
                </div>
              </div>
              <div class="dim-right">
                <el-progress
                  :percentage="score.qualityScore.score"
                  :color="getQualityColor(score.qualityScore.score)"
                  :stroke-width="6"
                  :show-text="false"
                  class="dim-progress"
                />
                <div class="dim-score" :style="{ color: getQualityColor(score.qualityScore.score) }">
                  {{ score.qualityScore.score }}
                  <span class="dim-weight">(×{{ (score.qualityScore.weight * 100).toFixed(0) }}%)</span>
                </div>
              </div>
            </div>

            <div class="dimension-item">
              <div class="dim-left">
                <span class="dim-icon alarm-icon">A</span>
                <div class="dim-info">
                  <div class="dim-name">报警评分</div>
                  <div class="dim-detail">{{ score.alarmScore.detail }}</div>
                </div>
              </div>
              <div class="dim-right">
                <el-progress
                  :percentage="score.alarmScore.score"
                  :color="getAlarmColor(score.alarmScore.score)"
                  :stroke-width="6"
                  :show-text="false"
                  class="dim-progress"
                />
                <div class="dim-score" :style="{ color: getAlarmColor(score.alarmScore.score) }">
                  {{ score.alarmScore.score }}
                  <span class="dim-weight">(×{{ (score.alarmScore.weight * 100).toFixed(0) }}%)</span>
                </div>
              </div>
            </div>

            <div class="dimension-item">
              <div class="dim-left">
                <span class="dim-icon fluct-icon">F</span>
                <div class="dim-info">
                  <div class="dim-name">波动评分</div>
                  <div class="dim-detail">{{ score.fluctuationScore.detail }}</div>
                </div>
              </div>
              <div class="dim-right">
                <el-progress
                  :percentage="score.fluctuationScore.score"
                  :color="getFluctColor(score.fluctuationScore.score)"
                  :stroke-width="6"
                  :show-text="false"
                  class="dim-progress"
                />
                <div class="dim-score" :style="{ color: getFluctColor(score.fluctuationScore.score) }">
                  {{ score.fluctuationScore.score }}
                  <span class="dim-weight">(×{{ (score.fluctuationScore.weight * 100).toFixed(0) }}%)</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="nodes-section" v-if="score.nodeDetails.length > 0">
          <div class="nodes-title">
            <el-icon :size="13"><DataLine /></el-icon>
            节点健康明细
          </div>
          <div class="nodes-table">
            <div
              v-for="nd in score.nodeDetails"
              :key="nd.nodeId"
              class="node-row"
            >
              <div class="node-col node-name-col">
                <span class="quality-indicator" :class="`q-${nd.quality.toLowerCase()}`"></span>
                <span class="node-name-text">{{ nd.nodeName }}</span>
              </div>
              <div class="node-col">
                <span class="node-value">{{ formatValue(nd) }}</span>
                <span class="node-unit">{{ nd.unit || '' }}</span>
              </div>
              <div class="node-col">
                <el-tooltip content="变异系数(CV)" placement="top" :show-after="500">
                  <span :class="getCvClass(nd.fluctuationRate)">
                    CV: {{ nd.fluctuationRate.toFixed(1) }}%
                  </span>
                </el-tooltip>
              </div>
              <div class="node-col">
                <el-tag
                  v-if="nd.highestAlarmSeverity !== 'None'"
                  :type="getSeverityTagType(nd.highestAlarmSeverity)"
                  size="small"
                >
                  {{ getSeverityLabel(nd.highestAlarmSeverity) }}
                  <span v-if="nd.alarmCount > 1">({{ nd.alarmCount }})</span>
                </el-tag>
                <span v-else class="no-alarm">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="risk-legend">
      <span class="legend-title">风险等级说明:</span>
      <span class="legend-item"><i style="background:#10b981"></i>优秀(90-100)</span>
      <span class="legend-item"><i style="background:#22d3ee"></i>良好(75-89)</span>
      <span class="legend-item"><i style="background:#eab308"></i>一般(60-74)</span>
      <span class="legend-item"><i style="background:#f97316"></i>警告(40-59)</span>
      <span class="legend-item"><i style="background:#ef4444"></i>危险(0-39)</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Timer, Cpu, Monitor, DataLine } from '@element-plus/icons-vue'
import { useOpcuaStore } from '../store/opcua'
import type { DeviceHealthScore, NodeHealthDetail } from '../types'

const store = useOpcuaStore()
const scores = computed<DeviceHealthScore[]>(() => store.healthScores)
const lastUpdate = computed(() => {
  if (scores.value.length === 0) return 0
  return Math.max(...scores.value.map(s => s.updateTime))
})

function formatTime(t: number): string {
  return new Date(t).toLocaleTimeString('zh-CN', { hour12: false })
}

function formatValue(nd: NodeHealthDetail): string {
  if (nd.unit === 'RPM' || nd.dataType === 'Int32') return nd.currentValue.toFixed(0)
  if (nd.unit === '%') return nd.currentValue.toFixed(0)
  if (nd.unit === 'MPa') return nd.currentValue.toFixed(2)
  return nd.currentValue.toFixed(1)
}

function getQualityColor(score: number): string {
  if (score >= 90) return '#10b981'
  if (score >= 70) return '#22d3ee'
  if (score >= 50) return '#eab308'
  return '#ef4444'
}

function getAlarmColor(score: number): string {
  if (score >= 90) return '#10b981'
  if (score >= 70) return '#eab308'
  if (score >= 40) return '#f97316'
  return '#ef4444'
}

function getFluctColor(score: number): string {
  if (score >= 90) return '#10b981'
  if (score >= 75) return '#22d3ee'
  if (score >= 55) return '#eab308'
  if (score >= 35) return '#f97316'
  return '#ef4444'
}

function getCvClass(cv: number): string {
  if (cv < 2) return 'cv-excellent'
  if (cv < 5) return 'cv-good'
  if (cv < 10) return 'cv-fair'
  if (cv < 15) return 'cv-warning'
  return 'cv-critical'
}

function getSeverityTagType(s: string): 'danger' | 'warning' | 'info' | 'success' {
  switch (s) {
    case 'Critical': return 'danger'
    case 'High': return 'danger'
    case 'Medium': return 'warning'
    case 'Low': return 'info'
    case 'Info': return 'info'
    default: return 'success'
  }
}

function getSeverityLabel(s: string): string {
  switch (s) {
    case 'Critical': return '严重'
    case 'High': return '高'
    case 'Medium': return '中'
    case 'Low': return '低'
    case 'Info': return '信息'
    default: return s
  }
}
</script>

<style scoped>
.health-score-container {
  padding: 12px;
  height: 100%;
  overflow-y: auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #22d3ee;
  padding-left: 8px;
  border-left: 3px solid #06b6d4;
  margin: 0;
}

.update-time {
  font-size: 11px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: monospace;
}

.no-data {
  padding: 40px 0;
}

.devices-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(460px, 1fr));
  gap: 12px;
}

.device-card {
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(71, 85, 105, 0.5);
  border-radius: 10px;
  padding: 14px;
  border-top: 3px solid;
  transition: all 0.3s;
}

.device-card:hover {
  background: rgba(30, 41, 59, 0.95);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.device-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.device-desc {
  font-size: 11px;
  color: #64748b;
  padding-left: 22px;
}

.risk-tag {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.score-section {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 14px;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid rgba(71, 85, 105, 0.3);
  border-bottom: 1px solid rgba(71, 85, 105, 0.3);
}

.score-gauge-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.score-label {
  font-size: 11px;
  color: #64748b;
  margin-top: -4px;
}

.dimensions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dimension-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.dim-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.dim-icon {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
  color: #fff;
}

.quality-icon { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.alarm-icon { background: linear-gradient(135deg, #ef4444, #dc2626); }
.fluct-icon { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }

.dim-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.dim-name {
  font-size: 12px;
  font-weight: 500;
  color: #cbd5e1;
}

.dim-detail {
  font-size: 10px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dim-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.dim-progress {
  width: 60px;
}

.dim-score {
  font-size: 13px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  min-width: 52px;
  text-align: right;
}

.dim-weight {
  font-size: 10px;
  font-weight: 400;
  color: #64748b;
}

.nodes-section {
  margin-top: 10px;
}

.nodes-title {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nodes-table {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 6px;
  padding: 6px;
}

.node-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr 0.9fr 0.9fr;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  align-items: center;
  font-size: 11px;
}

.node-row:hover {
  background: rgba(71, 85, 105, 0.2);
}

.node-col {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.node-name-col {
  overflow: hidden;
}

.quality-indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.q-good { background: #10b981; box-shadow: 0 0 4px #10b981; }
.q-uncertain { background: #eab308; box-shadow: 0 0 4px #eab308; }
.q-bad { background: #ef4444; box-shadow: 0 0 4px #ef4444; }

.node-name-text {
  color: #cbd5e1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-value {
  color: #22d3ee;
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.node-unit {
  color: #64748b;
  font-size: 10px;
}

.cv-excellent { color: #10b981; font-weight: 500; }
.cv-good { color: #22d3ee; font-weight: 500; }
.cv-fair { color: #eab308; font-weight: 500; }
.cv-warning { color: #f97316; font-weight: 500; }
.cv-critical { color: #ef4444; font-weight: 500; }

.no-alarm {
  color: #475569;
}

.risk-legend {
  margin-top: 14px;
  padding: 10px 12px;
  background: rgba(30, 41, 59, 0.5);
  border-radius: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #64748b;
}

.legend-title {
  font-weight: 600;
  color: #94a3b8;
  margin-right: 4px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
}

.legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

@media (max-width: 1200px) {
  .devices-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .score-section {
    grid-template-columns: 1fr;
  }
  .node-row {
    grid-template-columns: 1.2fr 1fr 0.8fr 0.8fr;
  }
}
</style>
