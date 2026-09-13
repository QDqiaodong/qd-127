package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.PointSunshadeStatusDTO;
import com.example.benchmanagement.dto.SectionSunshadeSummaryDTO;
import com.example.benchmanagement.dto.SunshadeInspectionCreateRequest;
import com.example.benchmanagement.dto.SunshadeInspectionDTO;
import com.example.benchmanagement.service.SunshadeInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sunshade")
@RequiredArgsConstructor
public class SunshadeInspectionController {

    private final SunshadeInspectionService sunshadeInspectionService;

    /**
     * 登记点位遮阳棚巡查（点位必选，异常时必须写清破损位置）。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SunshadeInspectionDTO>> createInspection(
            @Valid @RequestBody SunshadeInspectionCreateRequest request) {
        SunshadeInspectionDTO created = sunshadeInspectionService.createInspection(request);
        return ResponseEntity.ok(ApiResponse.success("遮阳棚巡查登记成功", created));
    }

    /**
     * 遮阳棚巡查记录列表，可按点位、巡查结论、关键字筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SunshadeInspectionDTO>>> getInspections(
            @RequestParam(value = "pointId", required = false) Long pointId,
            @RequestParam(value = "result", required = false) Integer result,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                sunshadeInspectionService.getInspections(pointId, result, keyword)));
    }

    /**
     * 全部点位遮阳棚状态；inspected=false 可单独筛出未巡查点位；
     * sectionId 下钻单个路段，result 按最近一次巡查结论筛选（路段异常点位明细用）。
     */
    @GetMapping("/point-status")
    public ResponseEntity<ApiResponse<List<PointSunshadeStatusDTO>>> getPointStatuses(
            @RequestParam(value = "inspected", required = false) Boolean inspected,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "result", required = false) Integer result) {
        return ResponseEntity.ok(ApiResponse.success(
                sunshadeInspectionService.getPointStatuses(inspected, sectionId, result)));
    }

    /**
     * 按路段汇总遮阳棚异常：异常点数、破损面积加总；没有异常的路段不出汇总。
     */
    @GetMapping("/section-summary")
    public ResponseEntity<ApiResponse<List<SectionSunshadeSummaryDTO>>> getSectionSummaries() {
        return ResponseEntity.ok(ApiResponse.success(sunshadeInspectionService.getSectionSummaries()));
    }

    /**
     * 单个点位的遮阳棚巡查记录（点位详情）。
     */
    @GetMapping("/point/{pointId}")
    public ResponseEntity<ApiResponse<List<SunshadeInspectionDTO>>> getInspectionsByPoint(
            @PathVariable Long pointId) {
        return ResponseEntity.ok(ApiResponse.success(sunshadeInspectionService.getInspectionsByPoint(pointId)));
    }
}
