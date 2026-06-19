package com.opcua.controller;

import com.opcua.model.DataValueModel;
import com.opcua.model.DeviceHealthScore;
import com.opcua.model.NodeModel;
import com.opcua.service.AlarmService;
import com.opcua.service.HealthScoreService;
import com.opcua.service.OpcuaClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NodeController {

    private final OpcuaClientService opcuaClientService;
    private final HealthScoreService healthScoreService;
    private final AlarmService alarmService;

    public NodeController(OpcuaClientService opcuaClientService,
                          HealthScoreService healthScoreService,
                          AlarmService alarmService) {
        this.opcuaClientService = opcuaClientService;
        this.healthScoreService = healthScoreService;
        this.alarmService = alarmService;
    }

    /**
     * 获取所有 OPC-UA 节点（树形结构）
     */
    @GetMapping("/nodes")
    public ResponseEntity<List<NodeModel>> getAllNodes() {
        List<NodeModel> nodes = opcuaClientService.browseNodes();
        return ResponseEntity.ok(nodes);
    }

    /**
     * 获取指定节点的当前值
     */
    @GetMapping("/nodes/{id}/value")
    public ResponseEntity<DataValueModel> getNodeValue(@PathVariable String id) {
        DataValueModel value = opcuaClientService.readValue(id);
        if (value != null) {
            return ResponseEntity.ok(value);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 订阅节点数据变更
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody Map<String, Object> request) {
        String nodeId = (String) request.get("nodeId");
        Integer publishingInterval = (Integer) request.getOrDefault("publishingInterval", 1000);
        Integer samplingInterval = (Integer) request.getOrDefault("samplingInterval", 500);

        boolean success = opcuaClientService.subscribe(nodeId, publishingInterval, samplingInterval);

        return ResponseEntity.ok(Map.of(
                "success", success,
                "nodeId", nodeId,
                "publishingInterval", publishingInterval,
                "samplingInterval", samplingInterval
        ));
    }

    /**
     * 取消订阅
     */
    @DeleteMapping("/subscribe/{nodeId}")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable String nodeId) {
        boolean success = opcuaClientService.unsubscribe(nodeId);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "nodeId", nodeId
        ));
    }

    /**
     * 获取连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "connected", opcuaClientService.isConnected(),
                "serverUrl", opcuaClientService.getServerUrl()
        ));
    }

    /**
     * 获取所有设备的健康评分
     */
    @GetMapping("/health-scores")
    public ResponseEntity<List<DeviceHealthScore>> getAllHealthScores() {
        List<DeviceHealthScore> scores = healthScoreService.calculateAllDevices();
        return ResponseEntity.ok(scores);
    }

    /**
     * 获取指定设备的健康评分
     */
    @GetMapping("/health-scores/{deviceId}")
    public ResponseEntity<DeviceHealthScore> getHealthScore(@PathVariable String deviceId) {
        DeviceHealthScore score = healthScoreService.calculateDeviceScore(deviceId);
        if (score == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(score);
    }

    /**
     * 获取所有报警记录
     */
    @GetMapping("/alarms")
    public ResponseEntity<List<AlarmService.AlarmRecord>> getAllAlarms() {
        return ResponseEntity.ok(alarmService.getAllAlarms());
    }

    /**
     * 获取活跃（未确认）报警
     */
    @GetMapping("/alarms/active")
    public ResponseEntity<List<AlarmService.AlarmRecord>> getActiveAlarms() {
        return ResponseEntity.ok(alarmService.getActiveAlarms());
    }

    /**
     * 确认报警
     */
    @PostMapping("/alarms/{alarmId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlarm(@PathVariable String alarmId) {
        boolean success = alarmService.acknowledgeAlarm(alarmId);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "alarmId", alarmId
        ));
    }

    /**
     * 清空所有报警
     */
    @DeleteMapping("/alarms")
    public ResponseEntity<Map<String, Object>> clearAlarms() {
        alarmService.clearAll();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
