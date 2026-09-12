package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.AdditionalBenchPlanDTO;
import com.example.benchmanagement.dto.AdditionalBenchPlanRequest;
import com.example.benchmanagement.dto.AdditionalBenchVerifyRequest;
import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.service.AdditionalBenchPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/additional-bench-plans")
@RequiredArgsConstructor
public class AdditionalBenchPlanController {

    private final AdditionalBenchPlanService planService;

    /**
     * 加凳预案列表，可按街区、路段、展示状态（1-待投放，2-已投放，3-逾期）筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdditionalBenchPlanDTO>>> listPlans(
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(ApiResponse.success(planService.listPlans(districtId, sectionId, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdditionalBenchPlanDTO>> createPlan(
            @Valid @RequestBody AdditionalBenchPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("加凳预案提交成功", planService.createPlan(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdditionalBenchPlanDTO>> getPlanDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlanDetail(id)));
    }

    /**
     * 已投放数量核销；支持分批核销，每次记录操作人，全部核销后自动变为已投放。
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<AdditionalBenchPlanDTO>> verifyPlan(
            @PathVariable Long id,
            @Valid @RequestBody AdditionalBenchVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("核销成功", planService.verify(id, request)));
    }
}
