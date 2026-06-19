import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  OPCUANode, DataValue, AlarmEvent, SubscriptionConfig,
  DeviceHealthScore, DimensionScore, NodeHealthDetail, RiskLevel
} from '../types'

export const useOpcuaStore = defineStore('opcua', () => {
  // 状态
  const nodeTree = ref<OPCUANode[]>([])
  const selectedNode = ref<OPCUANode | null>(null)
  const subscriptions = ref<Map<string, SubscriptionConfig>>(new Map())
  const alarms = ref<AlarmEvent[]>([])
  const realTimeData = ref<Map<string, DataValue>>(new Map())
  const isConnected = ref(false)
  const dataHistory = ref<Map<string, Array<{ timestamp: number; value: number }>>>(new Map())
  const healthScores = ref<DeviceHealthScore[]>([])

  // 初始化模拟节点树
  function initNodeTree() {
    nodeTree.value = [
      {
        id: 'server',
        name: 'Server',
        nodeId: 'ns=0;i=2253',
        type: 'Object',
        description: 'OPC-UA 服务器根节点',
        children: [
          {
            id: 'objects',
            name: 'Objects',
            nodeId: 'ns=0;i=85',
            type: 'Object',
            description: '对象文件夹',
            children: [
              {
                id: 'plc_area1',
                name: 'PLC_Area1',
                nodeId: 'ns=2;i=1001',
                type: 'Object',
                description: '1号生产区域 PLC',
                children: [
                  {
                    id: 'temp_sensor',
                    name: 'Temperature_Sensor',
                    nodeId: 'ns=2;i=1002',
                    type: 'Variable',
                    dataType: 'Double',
                    value: 25.6,
                    unit: '°C',
                    quality: 'Good',
                    description: '温度传感器'
                  },
                  {
                    id: 'pressure_transmitter',
                    name: 'Pressure_Transmitter',
                    nodeId: 'ns=2;i=1003',
                    type: 'Variable',
                    dataType: 'Double',
                    value: 3.45,
                    unit: 'MPa',
                    quality: 'Good',
                    description: '压力变送器'
                  },
                  {
                    id: 'pump_status',
                    name: 'Pump_Status',
                    nodeId: 'ns=2;i=1004',
                    type: 'Variable',
                    dataType: 'Boolean',
                    value: true,
                    quality: 'Good',
                    description: '泵运行状态'
                  }
                ]
              },
              {
                id: 'plc_area2',
                name: 'PLC_Area2',
                nodeId: 'ns=2;i=2001',
                type: 'Object',
                description: '2号生产区域 PLC',
                children: [
                  {
                    id: 'flow_meter',
                    name: 'Flow_Meter',
                    nodeId: 'ns=2;i=2002',
                    type: 'Variable',
                    dataType: 'Double',
                    value: 156.7,
                    unit: 'L/min',
                    quality: 'Good',
                    description: '流量计'
                  },
                  {
                    id: 'valve_position',
                    name: 'Valve_Position',
                    nodeId: 'ns=2;i=2003',
                    type: 'Variable',
                    dataType: 'Double',
                    value: 75,
                    unit: '%',
                    quality: 'Good',
                    description: '阀门开度'
                  },
                  {
                    id: 'motor_speed',
                    name: 'Motor_Speed',
                    nodeId: 'ns=2;i=2004',
                    type: 'Variable',
                    dataType: 'Int32',
                    value: 1480,
                    unit: 'RPM',
                    quality: 'Good',
                    description: '电机转速'
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }

  // 模拟实时数据更新
  function simulateDataUpdate() {
    const nodes = getAllVariableNodes()
    nodes.forEach(node => {
      const currentValue = realTimeData.value.get(node.id)?.value ?? node.value
      
      let newValue: number | boolean | string
      if (node.dataType === 'Double') {
        const numVal = typeof currentValue === 'number' ? currentValue : parseFloat(String(currentValue))
        const variation = (Math.random() - 0.5) * 2
        newValue = Math.round((numVal + variation) * 100) / 100
      } else if (node.dataType === 'Int32') {
        const numVal = typeof currentValue === 'number' ? currentValue : parseInt(String(currentValue))
        const variation = Math.floor((Math.random() - 0.5) * 10)
        newValue = numVal + variation
      } else if (node.dataType === 'Boolean') {
        newValue = Math.random() > 0.95 ? !currentValue : currentValue
      } else {
        newValue = currentValue
      }

      const dataValue: DataValue = {
        nodeId: node.nodeId,
        value: newValue,
        quality: Math.random() > 0.98 ? 'Uncertain' : 'Good',
        timestamp: Date.now(),
        sourceTimestamp: Date.now(),
        serverTimestamp: Date.now()
      }

      realTimeData.value.set(node.id, dataValue)
      node.value = newValue
      node.quality = dataValue.quality

      // 记录历史数据
      const history = dataHistory.value.get(node.id) || []
      history.push({ timestamp: Date.now(), value: typeof newValue === 'number' ? newValue : 0 })
      if (history.length > 100) history.shift()
      dataHistory.value.set(node.id, history)

      // 检查报警条件
      checkAlarms(node, newValue)
    })
  }

  // 检查报警
  function checkAlarms(node: OPCUANode, value: number | boolean | string) {
    if (node.id === 'temp_sensor' && typeof value === 'number' && value > 28) {
      addAlarm({
        nodeId: node.nodeId,
        nodeName: node.name,
        severity: 'High',
        message: `温度过高: ${value}°C (阈值: 28°C)`,
        value,
        threshold: 28
      })
    }
    if (node.id === 'pressure_transmitter' && typeof value === 'number' && value > 4.0) {
      addAlarm({
        nodeId: node.nodeId,
        nodeName: node.name,
        severity: 'Critical',
        message: `压力超限: ${value} MPa (阈值: 4.0 MPa)`,
        value,
        threshold: 4.0
      })
    }
    if (node.id === 'motor_speed' && typeof value === 'number' && value > 1550) {
      addAlarm({
        nodeId: node.nodeId,
        nodeName: node.name,
        severity: 'Medium',
        message: `电机转速偏高: ${value} RPM (阈值: 1550 RPM)`,
        value,
        threshold: 1550
      })
    }
  }

  // 添加报警
  function addAlarm(alarm: Omit<AlarmEvent, 'id' | 'timestamp' | 'acknowledged'>) {
    const newAlarm: AlarmEvent = {
      ...alarm,
      id: `alarm_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: Date.now(),
      acknowledged: false
    }
    alarms.value.unshift(newAlarm)
    if (alarms.value.length > 50) alarms.value.pop()
  }

  // 获取所有变量节点
  function getAllVariableNodes(): OPCUANode[] {
    const variables: OPCUANode[] = []
    function traverse(nodes: OPCUANode[]) {
      nodes.forEach(node => {
        if (node.type === 'Variable') {
          variables.push(node)
        }
        if (node.children) {
          traverse(node.children)
        }
      })
    }
    traverse(nodeTree.value)
    return variables
  }

  // 选择节点
  function selectNode(node: OPCUANode) {
    selectedNode.value = node
  }

  // 添加订阅
  function addSubscription(nodeId: string, config: Partial<SubscriptionConfig> = {}) {
    const subscription: SubscriptionConfig = {
      nodeId,
      publishingInterval: config.publishingInterval || 1000,
      samplingInterval: config.samplingInterval || 500,
      queueSize: config.queueSize || 10,
      discardOldest: config.discardOldest ?? true,
      enabled: true
    }
    subscriptions.value.set(nodeId, subscription)
  }

  // 移除订阅
  function removeSubscription(nodeId: string) {
    subscriptions.value.delete(nodeId)
  }

  // 确认报警
  function acknowledgeAlarm(alarmId: string) {
    const alarm = alarms.value.find(a => a.id === alarmId)
    if (alarm) {
      alarm.acknowledged = true
    }
  }

  // 清空报警
  function clearAlarms() {
    alarms.value = []
  }

  // ===== 设备健康评分逻辑 =====
  const QUALITY_WEIGHT = 0.30
  const ALARM_WEIGHT = 0.40
  const FLUCTUATION_WEIGHT = 0.30

  const SEVERITY_SCORE: Record<string, number> = {
    Critical: 0,
    High: 25,
    Medium: 55,
    Low: 80,
    Info: 95
  }

  const DEVICE_IDS = ['plc_area1', 'plc_area2']
  const NODE_DEVICE_MAP: Record<string, string> = {
    temp_sensor: 'plc_area1',
    pressure_transmitter: 'plc_area1',
    pump_status: 'plc_area1',
    flow_meter: 'plc_area2',
    valve_position: 'plc_area2',
    motor_speed: 'plc_area2'
  }

  function getDeviceNodes(deviceId: string): OPCUANode[] {
    const variables: OPCUANode[] = []
    function traverse(nodes: OPCUANode[]) {
      nodes.forEach(node => {
        if (node.id === deviceId && node.children) {
          node.children.forEach(c => {
            if (c.type === 'Variable') variables.push(c)
          })
          return
        }
        if (node.children) traverse(node.children)
      })
    }
    traverse(nodeTree.value)
    return variables
  }

  function findNodeById(id: string): OPCUANode | undefined {
    function search(nodes: OPCUANode[]): OPCUANode | undefined {
      for (const n of nodes) {
        if (n.id === id) return n
        if (n.children) {
          const f = search(n.children)
          if (f) return f
        }
      }
      return undefined
    }
    return search(nodeTree.value)
  }

  function calculateQualityScore(nodes: OPCUANode[]): number {
    if (nodes.length === 0) return 0
    let good = 0, uncertain = 0, bad = 0
    nodes.forEach(node => {
      const q = node.quality || 'Good'
      if (q === 'Good') good++
      else if (q === 'Uncertain') uncertain++
      else if (q === 'Bad') bad++
      else good++
    })
    const total = Math.max(1, good + uncertain + bad)
    return Math.round((good * 100 + uncertain * 60 + bad * 0) / total)
  }

  function calculateAlarmScore(deviceId: string): number {
    const cutoff = Date.now() - 5 * 60 * 1000
    const deviceAlarms = alarms.value.filter(a => {
      const mappedDevice = NODE_DEVICE_MAP[extractNodeIdFromNodeIdStr(a.nodeId)] || ''
      return (a as any).deviceId === deviceId || mappedDevice === deviceId
    }).filter(a => a.timestamp >= cutoff)

    if (deviceAlarms.length === 0) return 100

    const severityOrder = ['Critical', 'High', 'Medium', 'Low', 'Info']
    let highest = 'Info'
    let unacked = 0
    deviceAlarms.forEach(a => {
      if (severityOrder.indexOf(a.severity) < severityOrder.indexOf(highest)) {
        highest = a.severity
      }
      if (!a.acknowledged) unacked++
    })

    const base = SEVERITY_SCORE[highest] ?? 95
    const penalty = Math.min(unacked * 5, 25)
    return Math.max(0, base - penalty)
  }

  function extractNodeIdFromNodeIdStr(nodeIdStr: string): string {
    for (const [k, v] of Object.entries(NODE_DEVICE_MAP)) {
      if (nodeIdStr.includes(k)) return k
    }
    return nodeIdStr
  }

  function calculateMean(data: number[]): number {
    return data.reduce((s, v) => s + v, 0) / data.length
  }

  function calculateStdDev(data: number[], mean: number): number {
    const sumSq = data.reduce((s, v) => s + (v - mean) * (v - mean), 0)
    return Math.sqrt(sumSq / data.length)
  }

  function calculateFluctuationScore(nodes: OPCUANode[]): number {
    if (nodes.length === 0) return 100
    let total = 0
    let count = 0

    nodes.forEach(node => {
      if (node.dataType === 'Boolean') return
      const history = dataHistory.value.get(node.id) || []
      if (history.length < 5) {
        total += 90
        count++
        return
      }
      const values = history.map(h => h.value)
      const mean = calculateMean(values)
      const std = calculateStdDev(values, mean)
      const cv = mean === 0 ? 0 : (std / Math.abs(mean)) * 100

      let score: number
      if (cv < 2) score = 100
      else if (cv < 5) score = 85
      else if (cv < 10) score = 65
      else if (cv < 15) score = 45
      else score = 25

      total += score
      count++
    })

    if (count === 0) return 100
    return Math.round(total / count)
  }

  function applyRiskLevel(totalScore: number): { level: RiskLevel; label: string; color: string } {
    if (totalScore >= 90) return { level: 'EXCELLENT', label: '优秀', color: '#10b981' }
    if (totalScore >= 75) return { level: 'GOOD', label: '良好', color: '#22d3ee' }
    if (totalScore >= 60) return { level: 'FAIR', label: '一般', color: '#eab308' }
    if (totalScore >= 40) return { level: 'WARNING', label: '警告', color: '#f97316' }
    return { level: 'CRITICAL', label: '危险', color: '#ef4444' }
  }

  function buildNodeDetails(deviceId: string, nodes: OPCUANode[]): NodeHealthDetail[] {
    const cutoff = Date.now() - 5 * 60 * 1000
    const details: NodeHealthDetail[] = []

    nodes.forEach(node => {
      if (node.type !== 'Variable') return

      const history = dataHistory.value.get(node.id) || []
      let fluctuationRate = 0
      if (history.length >= 5) {
        const values = history.map(h => h.value)
        const mean = calculateMean(values)
        const std = calculateStdDev(values, mean)
        fluctuationRate = mean === 0 ? 0 : (std / Math.abs(mean)) * 100
      }

      const nodeAlarms = alarms.value.filter(a => {
        const mapped = NODE_DEVICE_MAP[extractNodeIdFromNodeIdStr(a.nodeId)]
        return (extractNodeIdFromNodeIdStr(a.nodeId) === node.id || a.nodeId.includes(node.name))
          && a.timestamp >= cutoff
      })

      const severityOrder = ['Critical', 'High', 'Medium', 'Low', 'Info']
      let highest = 'None'
      nodeAlarms.forEach(a => {
        if (highest === 'None' || severityOrder.indexOf(a.severity) < severityOrder.indexOf(highest)) {
          highest = a.severity
        }
      })

      const val = typeof node.value === 'number' ? node.value : 0

      details.push({
        nodeId: node.id,
        nodeName: node.name,
        quality: (node.quality || 'Good') as 'Good' | 'Bad' | 'Uncertain',
        currentValue: val,
        unit: node.unit,
        dataType: node.dataType,
        fluctuationRate: Math.round(fluctuationRate * 100) / 100,
        alarmCount: nodeAlarms.length,
        highestAlarmSeverity: highest
      })
    })

    return details
  }

  function calculateAllHealthScores(): DeviceHealthScore[] {
    const scores: DeviceHealthScore[] = []
    for (const deviceId of DEVICE_IDS) {
      const deviceNode = findNodeById(deviceId)
      const variableNodes = getDeviceNodes(deviceId)

      const qualityScore = calculateQualityScore(variableNodes)
      const alarmScore = calculateAlarmScore(deviceId)
      const fluctuationScore = calculateFluctuationScore(variableNodes)

      const total = Math.round(
        qualityScore * QUALITY_WEIGHT +
        alarmScore * ALARM_WEIGHT +
        fluctuationScore * FLUCTUATION_WEIGHT
      )
      const risk = applyRiskLevel(total)

      const qualityDetail = buildQualityDetail(variableNodes)
      const alarmDetail = buildAlarmDetail(deviceId)
      const fluctuationDetail = buildFluctuationDetail(variableNodes)

      scores.push({
        deviceId,
        deviceName: deviceNode?.name || deviceId,
        description: deviceNode?.description,
        totalScore: total,
        riskLevel: risk.level,
        riskLevelLabel: risk.label,
        riskColor: risk.color,
        qualityScore: {
          score: qualityScore,
          weight: QUALITY_WEIGHT,
          weightedScore: Math.round(qualityScore * QUALITY_WEIGHT * 100) / 100,
          detail: qualityDetail
        },
        alarmScore: {
          score: alarmScore,
          weight: ALARM_WEIGHT,
          weightedScore: Math.round(alarmScore * ALARM_WEIGHT * 100) / 100,
          detail: alarmDetail
        },
        fluctuationScore: {
          score: fluctuationScore,
          weight: FLUCTUATION_WEIGHT,
          weightedScore: Math.round(fluctuationScore * FLUCTUATION_WEIGHT * 100) / 100,
          detail: fluctuationDetail
        },
        nodeDetails: buildNodeDetails(deviceId, variableNodes),
        updateTime: Date.now()
      })
    }
    healthScores.value = scores
    return scores
  }

  function buildQualityDetail(nodes: OPCUANode[]): string {
    let good = 0, uncertain = 0, bad = 0
    nodes.forEach(n => {
      if (n.type !== 'Variable') return
      const q = n.quality || 'Good'
      if (q === 'Good') good++
      else if (q === 'Uncertain') uncertain++
      else if (q === 'Bad') bad++
    })
    return `正常:${good} 不确定:${uncertain} 异常:${bad}`
  }

  function buildAlarmDetail(deviceId: string): string {
    const cutoff = Date.now() - 5 * 60 * 1000
    const records = alarms.value.filter(a => {
      const mapped = NODE_DEVICE_MAP[extractNodeIdFromNodeIdStr(a.nodeId)]
      return mapped === deviceId && a.timestamp >= cutoff
    })
    const unacked = records.filter(a => !a.acknowledged).length
    return `近5分钟报警${records.length}条(未确认${unacked}条)`
  }

  function buildFluctuationDetail(nodes: OPCUANode[]): string {
    const parts: string[] = []
    nodes.forEach(node => {
      if (node.type !== 'Variable' || node.dataType === 'Boolean') return
      const history = dataHistory.value.get(node.id) || []
      if (history.length < 5) return
      const values = history.map(h => h.value)
      const mean = calculateMean(values)
      const std = calculateStdDev(values, mean)
      const cv = mean === 0 ? 0 : (std / Math.abs(mean)) * 100
      parts.push(`${node.name}:CV${cv.toFixed(1)}%`)
    })
    return parts.length === 0 ? '数据不足' : parts.join('; ')
  }

  // 连接模拟
  function connect() {
    isConnected.value = true
    initNodeTree()
  }

  // 断开连接
  function disconnect() {
    isConnected.value = false
  }

  // 计算属性
  const activeAlarmsCount = computed(() => alarms.value.filter(a => !a.acknowledged).length)
  const criticalAlarmsCount = computed(() => alarms.value.filter(a => a.severity === 'Critical' && !a.acknowledged).length)
  const worstRiskLevel = computed(() => {
    const order: RiskLevel[] = ['CRITICAL', 'WARNING', 'FAIR', 'GOOD', 'EXCELLENT']
    let worst: RiskLevel = 'EXCELLENT'
    for (const s of healthScores.value) {
      if (order.indexOf(s.riskLevel) < order.indexOf(worst)) {
        worst = s.riskLevel
      }
    }
    return worst
  })

  return {
    // 状态
    nodeTree,
    selectedNode,
    subscriptions,
    alarms,
    realTimeData,
    isConnected,
    dataHistory,
    healthScores,
    // 方法
    initNodeTree,
    simulateDataUpdate,
    selectNode,
    addSubscription,
    removeSubscription,
    acknowledgeAlarm,
    clearAlarms,
    connect,
    disconnect,
    getAllVariableNodes,
    calculateAllHealthScores,
    // 计算属性
    activeAlarmsCount,
    criticalAlarmsCount,
    worstRiskLevel
  }
})
