package com.opcua.service;

import com.opcua.model.DeviceHealthScore;
import com.opcua.model.DeviceHealthScore.DimensionScore;
import com.opcua.model.DeviceHealthScore.NodeHealthDetail;
import com.opcua.model.DeviceHealthScore.RiskLevel;
import com.opcua.model.NodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HealthScoreService {

    private static final Logger log = LoggerFactory.getLogger(HealthScoreService.class);

    private static final double QUALITY_WEIGHT = 0.30;
    private static final double ALARM_WEIGHT = 0.40;
    private static final double FLUCTUATION_WEIGHT = 0.30;

    private static final Map<String, Integer> SEVERITY_SCORE_MAP = new LinkedHashMap<>();
    static {
        SEVERITY_SCORE_MAP.put("Critical", 0);
        SEVERITY_SCORE_MAP.put("High", 25);
        SEVERITY_SCORE_MAP.put("Medium", 55);
        SEVERITY_SCORE_MAP.put("Low", 80);
        SEVERITY_SCORE_MAP.put("Info", 95);
    }

    private static final List<String> DEVICE_IDS = List.of("plc_area1", "plc_area2");

    private final OpcuaClientService opcuaClientService;
    private final AlarmService alarmService;

    public HealthScoreService(OpcuaClientService opcuaClientService, AlarmService alarmService) {
        this.opcuaClientService = opcuaClientService;
        this.alarmService = alarmService;
    }

    public List<DeviceHealthScore> calculateAllDevices() {
        List<DeviceHealthScore> scores = new ArrayList<>();
        for (String deviceId : DEVICE_IDS) {
            scores.add(calculateDeviceScore(deviceId));
        }
        return scores;
    }

    public DeviceHealthScore calculateDeviceScore(String deviceId) {
        NodeModel device = opcuaClientService.getDeviceNode(deviceId);
        if (device == null) {
            log.warn("设备节点不存在: {}", deviceId);
            return createEmptyScore(deviceId);
        }

        List<NodeModel> variableNodes = getVariableNodes(device);

        int qualityDimScore = calculateQualityScore(variableNodes);
        int alarmDimScore = calculateAlarmScore(deviceId, variableNodes);
        int fluctuationDimScore = calculateFluctuationScore(variableNodes);

        double totalWeighted = qualityDimScore * QUALITY_WEIGHT
                + alarmDimScore * ALARM_WEIGHT
                + fluctuationDimScore * FLUCTUATION_WEIGHT;
        int totalScore = (int) Math.round(totalWeighted);

        DeviceHealthScore score = new DeviceHealthScore();
        score.setDeviceId(deviceId);
        score.setDeviceName(device.getName());
        score.setDescription(device.getDescription());
        score.setTotalScore(totalScore);

        applyRiskLevel(score, totalScore);

        score.setQualityScore(new DimensionScore(qualityDimScore, QUALITY_WEIGHT, buildQualityDetail(variableNodes)));
        score.setAlarmScore(new DimensionScore(alarmDimScore, ALARM_WEIGHT, buildAlarmDetail(deviceId)));
        score.setFluctuationScore(new DimensionScore(fluctuationDimScore, FLUCTUATION_WEIGHT, buildFluctuationDetail(variableNodes)));

        score.setNodeDetails(buildNodeDetails(deviceId, variableNodes));
        score.setUpdateTime(System.currentTimeMillis());

        return score;
    }

    private int calculateQualityScore(List<NodeModel> nodes) {
        if (nodes.isEmpty()) return 0;
        int goodCount = 0, uncertainCount = 0, badCount = 0;
        for (NodeModel node : nodes) {
            if (!"Variable".equals(node.getType())) continue;
            String q = node.getQuality() == null ? "Good" : node.getQuality();
            switch (q) {
                case "Good" -> goodCount++;
                case "Uncertain" -> uncertainCount++;
                case "Bad" -> badCount++;
                default -> goodCount++;
            }
        }
        int total = Math.max(1, goodCount + uncertainCount + badCount);
        double score = (goodCount * 100.0 + uncertainCount * 60.0 + badCount * 0.0) / total;
        return (int) Math.round(score);
    }

    private int calculateAlarmScore(String deviceId, List<NodeModel> nodes) {
        List<String> nodeIds = new ArrayList<>();
        for (NodeModel n : nodes) nodeIds.add(n.getId());

        List<AlarmService.AlarmRecord> recentAlarms = alarmService.getRecentAlarms(deviceId, 5 * 60 * 1000);

        if (recentAlarms.isEmpty()) {
            return 100;
        }

        String highestSeverity = "Info";
        int unacknowledgedCount = 0;
        for (AlarmService.AlarmRecord alarm : recentAlarms) {
            if (isHigherSeverity(alarm.getSeverity(), highestSeverity)) {
                highestSeverity = alarm.getSeverity();
            }
            if (!alarm.isAcknowledged()) {
                unacknowledgedCount++;
            }
        }

        int baseScore = SEVERITY_SCORE_MAP.getOrDefault(highestSeverity, 95);
        int penalty = Math.min(unacknowledgedCount * 5, 25);

        return Math.max(0, baseScore - penalty);
    }

    private int calculateFluctuationScore(List<NodeModel> nodes) {
        if (nodes.isEmpty()) return 100;

        double totalNodeScore = 0;
        int countedNodes = 0;

        for (NodeModel node : nodes) {
            if (!"Variable".equals(node.getType())) continue;
            if (node.getDataType() == null || node.getDataType().equals("Boolean")) continue;

            double[] history = opcuaClientService.getValueHistory(node.getId());
            if (history == null || history.length < 5) {
                totalNodeScore += 90;
                countedNodes++;
                continue;
            }

            double mean = calculateMean(history);
            double stdDev = calculateStdDev(history, mean);
            double cv = mean == 0 ? 0 : (stdDev / Math.abs(mean)) * 100;

            int nodeScore;
            if (cv < 2) nodeScore = 100;
            else if (cv < 5) nodeScore = 85;
            else if (cv < 10) nodeScore = 65;
            else if (cv < 15) nodeScore = 45;
            else nodeScore = 25;

            totalNodeScore += nodeScore;
            countedNodes++;
        }

        if (countedNodes == 0) return 100;
        return (int) Math.round(totalNodeScore / countedNodes);
    }

    private void applyRiskLevel(DeviceHealthScore score, int totalScore) {
        RiskLevel level;
        String label;
        String color;

        if (totalScore >= 90) {
            level = RiskLevel.EXCELLENT;
            label = "优秀";
            color = "#10b981";
        } else if (totalScore >= 75) {
            level = RiskLevel.GOOD;
            label = "良好";
            color = "#22d3ee";
        } else if (totalScore >= 60) {
            level = RiskLevel.FAIR;
            label = "一般";
            color = "#eab308";
        } else if (totalScore >= 40) {
            level = RiskLevel.WARNING;
            label = "警告";
            color = "#f97316";
        } else {
            level = RiskLevel.CRITICAL;
            label = "危险";
            color = "#ef4444";
        }

        score.setRiskLevel(level);
        score.setRiskLevelLabel(label);
        score.setRiskColor(color);
    }

    private String buildQualityDetail(List<NodeModel> nodes) {
        int good = 0, uncertain = 0, bad = 0;
        for (NodeModel n : nodes) {
            if (!"Variable".equals(n.getType())) continue;
            String q = n.getQuality() == null ? "Good" : n.getQuality();
            switch (q) {
                case "Good" -> good++;
                case "Uncertain" -> uncertain++;
                case "Bad" -> bad++;
            }
        }
        return String.format("正常:%d 不确定:%d 异常:%d", good, uncertain, bad);
    }

    private String buildAlarmDetail(String deviceId) {
        long now = System.currentTimeMillis();
        List<AlarmService.AlarmRecord> records = alarmService.getRecentAlarms(deviceId, 5 * 60 * 1000);
        long unacked = records.stream().filter(a -> !a.isAcknowledged()).count();
        return String.format("近5分钟报警%d条(未确认%d条)", records.size(), unacked);
    }

    private String buildFluctuationDetail(List<NodeModel> nodes) {
        StringBuilder sb = new StringBuilder();
        for (NodeModel node : nodes) {
            if (!"Variable".equals(node.getType())) continue;
            if ("Boolean".equals(node.getDataType())) continue;
            double[] history = opcuaClientService.getValueHistory(node.getId());
            if (history == null || history.length < 5) continue;
            double mean = calculateMean(history);
            double stdDev = calculateStdDev(history, mean);
            double cv = mean == 0 ? 0 : (stdDev / Math.abs(mean)) * 100;
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(String.format("%s:CV%.1f%%", node.getName(), cv));
        }
        return sb.isEmpty() ? "数据不足" : sb.toString();
    }

    private List<NodeHealthDetail> buildNodeDetails(String deviceId, List<NodeModel> nodes) {
        List<NodeHealthDetail> details = new ArrayList<>();
        Map<String, List<AlarmService.AlarmRecord>> nodeAlarmMap = new HashMap<>();
        for (AlarmService.AlarmRecord rec : alarmService.getRecentAlarms(deviceId, 5 * 60 * 1000)) {
            nodeAlarmMap.computeIfAbsent(rec.getNodeId(), k -> new ArrayList<>()).add(rec);
        }

        for (NodeModel node : nodes) {
            if (!"Variable".equals(node.getType())) continue;
            NodeHealthDetail d = new NodeHealthDetail();
            d.setNodeId(node.getId());
            d.setNodeName(node.getName());
            d.setQuality(node.getQuality() == null ? "Good" : node.getQuality());
            d.setUnit(node.getUnit());
            Object val = node.getValue();
            d.setCurrentValue(val instanceof Number ? ((Number) val).doubleValue() : 0);

            double[] history = opcuaClientService.getValueHistory(node.getId());
            if (history != null && history.length >= 5) {
                double mean = calculateMean(history);
                double stdDev = calculateStdDev(history, mean);
                d.setFluctuationRate(mean == 0 ? 0 : (stdDev / Math.abs(mean)) * 100);
            } else {
                d.setFluctuationRate(0);
            }

            List<AlarmService.AlarmRecord> nodeAlarms = nodeAlarmMap.getOrDefault(node.getId(), Collections.emptyList());
            d.setAlarmCount(nodeAlarms.size());
            String highest = null;
            for (AlarmService.AlarmRecord a : nodeAlarms) {
                if (highest == null || isHigherSeverity(a.getSeverity(), highest)) {
                    highest = a.getSeverity();
                }
            }
            d.setHighestAlarmSeverity(highest == null ? "None" : highest);

            details.add(d);
        }
        return details;
    }

    private List<NodeModel> getVariableNodes(NodeModel device) {
        List<NodeModel> result = new ArrayList<>();
        if (device.getChildren() != null) {
            for (NodeModel child : device.getChildren()) {
                if ("Variable".equals(child.getType())) {
                    result.add(child);
                }
            }
        }
        return result;
    }

    private boolean isHigherSeverity(String s1, String s2) {
        List<String> order = List.of("Critical", "High", "Medium", "Low", "Info", "None");
        int i1 = order.indexOf(s1);
        int i2 = order.indexOf(s2);
        if (i1 < 0) i1 = 5;
        if (i2 < 0) i2 = 5;
        return i1 < i2;
    }

    private double calculateMean(double[] data) {
        double sum = 0;
        for (double v : data) sum += v;
        return sum / data.length;
    }

    private double calculateStdDev(double[] data, double mean) {
        double sumSq = 0;
        for (double v : data) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / data.length);
    }

    private DeviceHealthScore createEmptyScore(String deviceId) {
        DeviceHealthScore s = new DeviceHealthScore();
        s.setDeviceId(deviceId);
        s.setDeviceName(deviceId);
        s.setTotalScore(0);
        s.setRiskLevel(RiskLevel.CRITICAL);
        s.setRiskLevelLabel("未知");
        s.setRiskColor("#94a3b8");
        s.setQualityScore(new DimensionScore(0, QUALITY_WEIGHT, "无数据"));
        s.setAlarmScore(new DimensionScore(0, ALARM_WEIGHT, "无数据"));
        s.setFluctuationScore(new DimensionScore(0, FLUCTUATION_WEIGHT, "无数据"));
        s.setNodeDetails(Collections.emptyList());
        s.setUpdateTime(System.currentTimeMillis());
        return s;
    }
}
