package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.AntiSlipMatIssueRequest;
import com.example.benchmanagement.dto.AntiSlipMatRecordDTO;
import com.example.benchmanagement.dto.AntiSlipMatReturnRequest;
import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.PointAntiSlipMatDTO;
import com.example.benchmanagement.service.AntiSlipMatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 雨天防滑垫领用台账：按点位登记领出、归还（含破损），汇总未还数量并在街区树标记未还清点位。
 */
@RestController
@RequestMapping("/api/anti-slip-mat")
@RequiredArgsConstructor
public class AntiSlipMatController {

    private final AntiSlipMatService antiSlipMatService;

    /**
     * 点位领用台账（每点位一行，含领出/归还/破损/未还汇总）；
     * outstanding=true 只看未还清点位，可按街区/路段/点位范围筛选。
     */
    @GetMapping("/point-ledgers")
    public ResponseEntity<ApiResponse<List<PointAntiSlipMatDTO>>> getPointLedgers(
            @RequestParam(value = "outstanding", required = false) Boolean outstanding,
            @RequestParam(value = "districtId", required = false) Long districtId,
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @RequestParam(value = "pointId", required = false) Long pointId) {
        return ResponseEntity.ok(ApiResponse.success(
                antiSlipMatService.getPointLedgers(outstanding, districtId, sectionId, pointId)));
    }

    /**
     * 领出登记（点位必选，数量大于0）。
     */
    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<AntiSlipMatRecordDTO>> issue(
            @Valid @RequestBody AntiSlipMatIssueRequest request) {
        return ResponseEntity.ok(ApiResponse.success("防滑垫领出登记成功", antiSlipMatService.issue(request)));
    }

    /**
     * 归还登记（本次归还数量 = 完好归还 + 破损，不能超过当前未还数量）。
     */
    @PostMapping("/return")
    public ResponseEntity<ApiResponse<AntiSlipMatRecordDTO>> recordReturn(
            @Valid @RequestBody AntiSlipMatReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("防滑垫归还登记成功", antiSlipMatService.recordReturn(request)));
    }

    /**
     * 领用/归还流水，可按点位、动作类型、关键字筛选。
     */
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<AntiSlipMatRecordDTO>>> getRecords(
            @RequestParam(value = "pointId", required = false) Long pointId,
            @RequestParam(value = "actionType", required = false) Integer actionType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                antiSlipMatService.getRecords(pointId, actionType, keyword)));
    }

    /**
     * 单个点位的领用/归还流水（台账详情）。
     */
    @GetMapping("/point/{pointId}/records")
    public ResponseEntity<ApiResponse<List<AntiSlipMatRecordDTO>>> getRecordsByPoint(
            @PathVariable Long pointId) {
        return ResponseEntity.ok(ApiResponse.success(antiSlipMatService.getRecordsByPoint(pointId)));
    }
}
