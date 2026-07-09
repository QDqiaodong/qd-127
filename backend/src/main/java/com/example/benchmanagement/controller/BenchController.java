package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.BenchChangeRequest;
import com.example.benchmanagement.dto.ChangeLogDTO;
import com.example.benchmanagement.service.BenchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bench")
@RequiredArgsConstructor
public class BenchController {

    private final BenchService benchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BenchDTO>>> getAllBenches() {
        List<BenchDTO> benches = benchService.getAllBenches();
        return ResponseEntity.ok(ApiResponse.success(benches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BenchDTO>> getBenchById(@PathVariable Long id) {
        BenchDTO bench = benchService.getBenchById(id);
        return ResponseEntity.ok(ApiResponse.success(bench));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<BenchDTO>> getBenchByCode(@PathVariable String code) {
        BenchDTO bench = benchService.getBenchByCode(code);
        return ResponseEntity.ok(ApiResponse.success(bench));
    }

    @GetMapping("/node/{nodeId}")
    public ResponseEntity<ApiResponse<List<BenchDTO>>> getBenchesByNode(@PathVariable Long nodeId) {
        List<BenchDTO> benches = benchService.getBenchesByNode(nodeId);
        return ResponseEntity.ok(ApiResponse.success(benches));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BenchDTO>> createBench(@Valid @RequestBody BenchDTO dto) {
        BenchDTO created = benchService.createBench(dto);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BenchDTO>> updateBench(@PathVariable Long id, @RequestBody BenchDTO dto) {
        BenchDTO updated = benchService.updateBench(id, dto);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBench(@PathVariable Long id) {
        benchService.deleteBench(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/change-node")
    public ResponseEntity<ApiResponse<List<BenchDTO>>> changeBenchNode(@Valid @RequestBody BenchChangeRequest request) {
        List<BenchDTO> changed = benchService.changeBenchNode(request);
        return ResponseEntity.ok(ApiResponse.success("变更成功", changed));
    }

    @GetMapping("/change-logs")
    public ResponseEntity<ApiResponse<List<ChangeLogDTO>>> getAllChangeLogs() {
        List<ChangeLogDTO> logs = benchService.getAllChangeLogs();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/{id}/change-logs")
    public ResponseEntity<ApiResponse<List<ChangeLogDTO>>> getChangeLogs(@PathVariable Long id) {
        List<ChangeLogDTO> logs = benchService.getChangeLogs(id);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/export/{sectionId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportAssets(@PathVariable Long sectionId) {
        List<Map<String, Object>> assets = benchService.exportBenchAssets(sectionId);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }
}
