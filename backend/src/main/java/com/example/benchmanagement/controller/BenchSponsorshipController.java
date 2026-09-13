package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.BenchSponsorshipDTO;
import com.example.benchmanagement.dto.BenchSponsorshipRequest;
import com.example.benchmanagement.service.BenchSponsorshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bench-sponsorships")
@RequiredArgsConstructor
public class BenchSponsorshipController {

    private final BenchSponsorshipService sponsorshipService;

    /**
     * 商户冠名台账列表，可按街区、路段、点位、展示状态（1-待生效，2-生效中，3-已过期）筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BenchSponsorshipDTO>>> listSponsorships(
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long pointId,
            @RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(ApiResponse.success(
                sponsorshipService.listSponsorships(districtId, sectionId, pointId, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BenchSponsorshipDTO>> createSponsorship(
            @Valid @RequestBody BenchSponsorshipRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "商户冠名记录提交成功", sponsorshipService.createSponsorship(request)));
    }
}
