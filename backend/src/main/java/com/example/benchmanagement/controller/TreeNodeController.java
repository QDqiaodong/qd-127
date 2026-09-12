package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.CapacityAdjustRequest;
import com.example.benchmanagement.dto.NodeCapacityLogDTO;
import com.example.benchmanagement.dto.NodeClosureLogDTO;
import com.example.benchmanagement.dto.PointClosureRequest;
import com.example.benchmanagement.dto.PointReopenRequest;
import com.example.benchmanagement.dto.SectionCapacityAlarmDTO;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.service.TreeNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tree")
@RequiredArgsConstructor
public class TreeNodeController {

    private final TreeNodeService treeNodeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TreeNodeDTO>>> getTree() {
        List<TreeNodeDTO> tree = treeNodeService.getTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<ApiResponse<List<TreeNodeDTO>>> getNodesByLevel(@PathVariable Integer level) {
        List<TreeNodeDTO> nodes = treeNodeService.getNodesByLevel(level);
        return ResponseEntity.ok(ApiResponse.success(nodes));
    }

    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<TreeNodeDTO>>> getChildren(@PathVariable Long parentId) {
        List<TreeNodeDTO> children = treeNodeService.getChildren(parentId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TreeNodeDTO>> getNodeById(@PathVariable Long id) {
        TreeNodeDTO node = treeNodeService.getNodeById(id);
        return ResponseEntity.ok(ApiResponse.success(node));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TreeNodeDTO>> createNode(@Valid @RequestBody TreeNodeDTO dto) {
        TreeNodeDTO created = treeNodeService.createNode(dto);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TreeNodeDTO>> updateNode(@PathVariable Long id, @RequestBody TreeNodeDTO dto) {
        TreeNodeDTO updated = treeNodeService.updateNode(id, dto);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @PutMapping("/{id}/capacity")
    public ResponseEntity<ApiResponse<TreeNodeDTO>> adjustCapacity(@PathVariable Long id,
                                                                   @Valid @RequestBody CapacityAdjustRequest request) {
        TreeNodeDTO updated = treeNodeService.adjustCapacity(id, request);
        return ResponseEntity.ok(ApiResponse.success("容量调整成功", updated));
    }

    @GetMapping("/{id}/capacity-logs")
    public ResponseEntity<ApiResponse<List<NodeCapacityLogDTO>>> getCapacityLogs(@PathVariable Long id) {
        List<NodeCapacityLogDTO> logs = treeNodeService.getCapacityLogs(id);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 路段容量告警详情：剩余容量加总、生效阈值及已满/将满点位明细（与树上告警标记同源）。
     */
    @GetMapping("/sections/{sectionId}/capacity-alarm")
    public ResponseEntity<ApiResponse<SectionCapacityAlarmDTO>> getSectionCapacityAlarm(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(treeNodeService.getSectionCapacityAlarm(sectionId)));
    }

    /**
     * 临时封闭点位：填写起止时间和原因。
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<TreeNodeDTO>> closePoint(@PathVariable Long id,
                                                               @Valid @RequestBody PointClosureRequest request) {
        TreeNodeDTO updated = treeNodeService.closePoint(id, request);
        return ResponseEntity.ok(ApiResponse.success("点位已临时封闭", updated));
    }

    /**
     * 人工解封点位。
     */
    @PutMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<TreeNodeDTO>> reopenPoint(@PathVariable Long id,
                                                                @Valid @RequestBody PointReopenRequest request) {
        TreeNodeDTO updated = treeNodeService.reopenPoint(id, request);
        return ResponseEntity.ok(ApiResponse.success("点位已解封", updated));
    }

    /**
     * 单个点位的封闭/解封台账。
     */
    @GetMapping("/{id}/closure-logs")
    public ResponseEntity<ApiResponse<List<NodeClosureLogDTO>>> getClosureLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(treeNodeService.getClosureLogs(id)));
    }

    /**
     * 全部分点位封闭/解封台账。
     */
    @GetMapping("/closure-logs/all")
    public ResponseEntity<ApiResponse<List<NodeClosureLogDTO>>> getAllClosureLogs() {
        return ResponseEntity.ok(ApiResponse.success(treeNodeService.getAllClosureLogs()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable Long id) {
        treeNodeService.deleteNode(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
