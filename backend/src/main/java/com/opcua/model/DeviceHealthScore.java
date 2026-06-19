package com.opcua.model;

import java.util.List;

public class DeviceHealthScore {

    private String deviceId;
    private String deviceName;
    private String description;
    private int totalScore;
    private RiskLevel riskLevel;
    private String riskLevelLabel;
    private String riskColor;

    private DimensionScore qualityScore;
    private DimensionScore alarmScore;
    private DimensionScore fluctuationScore;

    private List<NodeHealthDetail> nodeDetails;
    private long updateTime;

    public enum RiskLevel {
        EXCELLENT, GOOD, FAIR, WARNING, CRITICAL
    }

    public static class DimensionScore {
        private int score;
        private double weight;
        private double weightedScore;
        private String detail;

        public DimensionScore() {}

        public DimensionScore(int score, double weight, String detail) {
            this.score = score;
            this.weight = weight;
            this.weightedScore = score * weight;
            this.detail = detail;
        }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public double getWeightedScore() { return weightedScore; }
        public void setWeightedScore(double weightedScore) { this.weightedScore = weightedScore; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }

    public static class NodeHealthDetail {
        private String nodeId;
        private String nodeName;
        private String quality;
        private double currentValue;
        private String unit;
        private double fluctuationRate;
        private int alarmCount;
        private String highestAlarmSeverity;

        public NodeHealthDetail() {}

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public double getFluctuationRate() { return fluctuationRate; }
        public void setFluctuationRate(double fluctuationRate) { this.fluctuationRate = fluctuationRate; }
        public int getAlarmCount() { return alarmCount; }
        public void setAlarmCount(int alarmCount) { this.alarmCount = alarmCount; }
        public String getHighestAlarmSeverity() { return highestAlarmSeverity; }
        public void setHighestAlarmSeverity(String highestAlarmSeverity) { this.highestAlarmSeverity = highestAlarmSeverity; }
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskLevelLabel() { return riskLevelLabel; }
    public void setRiskLevelLabel(String riskLevelLabel) { this.riskLevelLabel = riskLevelLabel; }
    public String getRiskColor() { return riskColor; }
    public void setRiskColor(String riskColor) { this.riskColor = riskColor; }
    public DimensionScore getQualityScore() { return qualityScore; }
    public void setQualityScore(DimensionScore qualityScore) { this.qualityScore = qualityScore; }
    public DimensionScore getAlarmScore() { return alarmScore; }
    public void setAlarmScore(DimensionScore alarmScore) { this.alarmScore = alarmScore; }
    public DimensionScore getFluctuationScore() { return fluctuationScore; }
    public void setFluctuationScore(DimensionScore fluctuationScore) { this.fluctuationScore = fluctuationScore; }
    public List<NodeHealthDetail> getNodeDetails() { return nodeDetails; }
    public void setNodeDetails(List<NodeHealthDetail> nodeDetails) { this.nodeDetails = nodeDetails; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
