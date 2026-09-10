package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.CapacityAdjustRequest;
import com.example.benchmanagement.dto.NodeCapacityLogDTO;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable Long id) {
        treeNodeService.deleteNode(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
