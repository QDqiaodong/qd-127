package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.InspectionCreateRequest;
import com.example.benchmanagement.dto.InspectionDTO;
import com.example.benchmanagement.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspection")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    /**
     * 按街区/路段/点位发起巡检。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<InspectionDTO>>> createInspection(
            @Valid @RequestBody InspectionCreateRequest request) {
        List<InspectionDTO> created = inspectionService.createInspection(request);
        return ResponseEntity.ok(ApiResponse.success("巡检提交成功", created));
    }

    /**
     * 巡检记录列表，可按范围节点、巡检结果、严重程度、关键字筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionDTO>>> getInspections(
            @RequestParam(value = "nodeId", required = false) Long nodeId,
            @RequestParam(value = "result", required = false) Integer result,
            @RequestParam(value = "severity", required = false) Integer severity,
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<InspectionDTO> inspections = inspectionService.getInspections(nodeId, result, severity, keyword);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionDTO>> getInspectionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.getInspectionById(id)));
    }

    @GetMapping("/bench/{benchId}")
    public ResponseEntity<ApiResponse<List<InspectionDTO>>> getInspectionsByBench(@PathVariable Long benchId) {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.getInspectionsByBench(benchId)));
    }
}
