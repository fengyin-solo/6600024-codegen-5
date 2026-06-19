package com.opcua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    private static final int MAX_ALARMS = 500;
    private static final Map<String, String> NODE_TO_DEVICE = Map.of(
            "temp_sensor", "plc_area1",
            "pressure_transmitter", "plc_area1",
            "pump_status", "plc_area1",
            "flow_meter", "plc_area2",
            "valve_position", "plc_area2",
            "motor_speed", "plc_area2"
    );

    public static class AlarmRecord {
        private final String id;
        private final String nodeId;
        private final String nodeName;
        private final String deviceId;
        private final String severity;
        private final String message;
        private final long timestamp;
        private final Object value;
        private final Object threshold;
        private volatile boolean acknowledged;

        public AlarmRecord(String id, String nodeId, String nodeName, String deviceId,
                           String severity, String message, long timestamp,
                           Object value, Object threshold) {
            this.id = id;
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.deviceId = deviceId;
            this.severity = severity;
            this.message = message;
            this.timestamp = timestamp;
            this.value = value;
            this.threshold = threshold;
            this.acknowledged = false;
        }

        public String getId() { return id; }
        public String getNodeId() { return nodeId; }
        public String getNodeName() { return nodeName; }
        public String getDeviceId() { return deviceId; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public Object getValue() { return value; }
        public Object getThreshold() { return threshold; }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }

    private final Deque<AlarmRecord> alarmRecords = new ConcurrentLinkedDeque<>();

    public AlarmRecord recordAlarm(String nodeId, String nodeName, String severity,
                                   String message, Object value, Object threshold) {
        String deviceId = NODE_TO_DEVICE.getOrDefault(nodeId, "unknown");
        AlarmRecord record = new AlarmRecord(
                "alarm_" + System.currentTimeMillis() + "_" + Math.random(),
                nodeId, nodeName, deviceId, severity, message,
                System.currentTimeMillis(), value, threshold
        );
        alarmRecords.addFirst(record);
        while (alarmRecords.size() > MAX_ALARMS) {
            alarmRecords.removeLast();
        }
        log.info("记录报警: [{}] {} - {} (设备: {})", severity, nodeName, message, deviceId);
        return record;
    }

    public List<AlarmRecord> getRecentAlarms(String deviceId, long timeWindowMs) {
        long cutoff = System.currentTimeMillis() - timeWindowMs;
        List<AlarmRecord> result = new ArrayList<>();
        for (AlarmRecord r : alarmRecords) {
            if (r.getTimestamp() < cutoff) break;
            if (deviceId == null || deviceId.equals(r.getDeviceId())) {
                result.add(r);
            }
        }
        return result;
    }

    public List<AlarmRecord> getAllAlarms() {
        return new ArrayList<>(alarmRecords);
    }

    public List<AlarmRecord> getActiveAlarms() {
        List<AlarmRecord> result = new ArrayList<>();
        for (AlarmRecord r : alarmRecords) {
            if (!r.isAcknowledged()) result.add(r);
        }
        return result;
    }

    public boolean acknowledgeAlarm(String alarmId) {
        for (AlarmRecord r : alarmRecords) {
            if (r.getId().equals(alarmId)) {
                r.setAcknowledged(true);
                log.info("报警已确认: {}", alarmId);
                return true;
            }
        }
        return false;
    }

    public void clearAll() {
        alarmRecords.clear();
        log.info("已清空所有报警记录");
    }
}
