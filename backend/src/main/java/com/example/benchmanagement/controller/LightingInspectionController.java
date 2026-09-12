package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.LightingInspectionCreateRequest;
import com.example.benchmanagement.dto.LightingInspectionDTO;
import com.example.benchmanagement.dto.PointLightingStatusDTO;
import com.example.benchmanagement.service.LightingInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lighting")
@RequiredArgsConstructor
public class LightingInspectionController {

    private final LightingInspectionService lightingInspectionService;

    /**
     * 登记点位夜间照明巡查（点位必选）。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LightingInspectionDTO>> createInspection(
            @Valid @RequestBody LightingInspectionCreateRequest request) {
        LightingInspectionDTO created = lightingInspectionService.createInspection(request);
        return ResponseEntity.ok(ApiResponse.success("照明巡查登记成功", created));
    }

    /**
     * 照明巡查记录列表，可按点位、照明结论、关键字筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LightingInspectionDTO>>> getInspections(
            @RequestParam(value = "pointId", required = false) Long pointId,
            @RequestParam(value = "result", required = false) Integer result,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                lightingInspectionService.getInspections(pointId, result, keyword)));
    }

    /**
     * 全部点位照明状态；inspected=false 可单独筛出未巡查点位。
     */
    @GetMapping("/point-status")
    public ResponseEntity<ApiResponse<List<PointLightingStatusDTO>>> getPointStatuses(
            @RequestParam(value = "inspected", required = false) Boolean inspected) {
        return ResponseEntity.ok(ApiResponse.success(lightingInspectionService.getPointStatuses(inspected)));
    }

    /**
     * 单个点位的照明巡查记录（点位详情）。
     */
    @GetMapping("/point/{pointId}")
    public ResponseEntity<ApiResponse<List<LightingInspectionDTO>>> getInspectionsByPoint(
            @PathVariable Long pointId) {
        return ResponseEntity.ok(ApiResponse.success(lightingInspectionService.getInspectionsByPoint(pointId)));
    }
}
