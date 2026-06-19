// OPC-UA 节点类型定义
export interface OPCUANode {
  id: string
  name: string
  nodeId: string
  type: 'Object' | 'Variable' | 'Method' | 'DataType'
  dataType?: string
  value?: any
  unit?: string
  quality?: 'Good' | 'Bad' | 'Uncertain'
  children?: OPCUANode[]
  description?: string
  browseName?: string
}

// 数据值模型
export interface DataValue {
  nodeId: string
  value: number | boolean | string
  quality: 'Good' | 'Bad' | 'Uncertain'
  timestamp: number
  sourceTimestamp?: number
  serverTimestamp?: number
}

// 报警事件
export interface AlarmEvent {
  id: string
  nodeId: string
  nodeName: string
  severity: 'Critical' | 'High' | 'Medium' | 'Low' | 'Info'
  message: string
  timestamp: number
  acknowledged: boolean
  value?: number | boolean | string
  threshold?: number
  deviceId?: string
}

// 订阅配置
export interface SubscriptionConfig {
  nodeId: string
  publishingInterval: number
  samplingInterval: number
  queueSize: number
  discardOldest: boolean
  enabled: boolean
}

// 历史数据点
export interface HistoryDataPoint {
  timestamp: number
  value: number
  quality: 'Good' | 'Bad' | 'Uncertain'
}

// 节点详情
export interface NodeDetail {
  node: OPCUANode
  currentValue?: DataValue
  history?: HistoryDataPoint[]
  subscriptions?: SubscriptionConfig[]
}

// 风险等级枚举
export type RiskLevel = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'WARNING' | 'CRITICAL'

// 维度评分
export interface DimensionScore {
  score: number
  weight: number
  weightedScore: number
  detail: string
}

// 节点健康明细
export interface NodeHealthDetail {
  nodeId: string
  nodeName: string
  quality: 'Good' | 'Bad' | 'Uncertain'
  currentValue: number
  unit?: string
  fluctuationRate: number
  alarmCount: number
  highestAlarmSeverity: string
}

// 设备健康评分
export interface DeviceHealthScore {
  deviceId: string
  deviceName: string
  description?: string
  totalScore: number
  riskLevel: RiskLevel
  riskLevelLabel: string
  riskColor: string
  qualityScore: DimensionScore
  alarmScore: DimensionScore
  fluctuationScore: DimensionScore
  nodeDetails: NodeHealthDetail[]
  updateTime: number
}
