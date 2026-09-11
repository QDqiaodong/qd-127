package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.InspectionDTO;
import com.example.benchmanagement.dto.InspectionPlanDTO;
import com.example.benchmanagement.dto.InspectionPlanDetailDTO;
import com.example.benchmanagement.dto.InspectionPlanRequest;
import com.example.benchmanagement.dto.InspectionTaskDTO;
import com.example.benchmanagement.dto.TaskExecuteRequest;
import com.example.benchmanagement.service.InspectionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspection-plan")
@RequiredArgsConstructor
public class InspectionPlanController {

    private final InspectionPlanService planService;

    /**
     * 巡检计划列表（含已执行/待执行/逾期统计）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionPlanDTO>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.success(planService.listPlans()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InspectionPlanDTO>> createPlan(
            @Valid @RequestBody InspectionPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("巡检计划创建成功", planService.createPlan(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionPlanDTO>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody InspectionPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("巡检计划已更新", planService.updatePlan(id, request)));
    }

    /**
     * 启用/停用计划。停用后不再生成新任务。
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<InspectionPlanDTO>> togglePlan(@PathVariable Long id) {
        InspectionPlanDTO plan = planService.togglePlan(id);
        String message = Boolean.TRUE.equals(plan.getEnabled())
                ? "计划已启用，将按周期生成待执行任务"
                : "计划已停用，将不再生成新任务";
        return ResponseEntity.ok(ApiResponse.success(message, plan));
    }

    /**
     * 计划详情：统计与任务列表。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionPlanDetailDTO>> getPlanDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlanDetail(id)));
    }

    /**
     * 巡检任务列表，可按计划、状态（1-待执行，2-已执行，3-逾期）筛选。
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<InspectionTaskDTO>>> listTasks(
            @RequestParam(value = "planId", required = false) Long planId,
            @RequestParam(value = "status", required = false) Integer status) {
        return ResponseEntity.ok(ApiResponse.success(planService.listTasks(planId, status)));
    }

    /**
     * 执行任务时自动带入范围内全部长凳。
     */
    @GetMapping("/tasks/{taskId}/benches")
    public ResponseEntity<ApiResponse<List<BenchDTO>>> getTaskBenches(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(planService.getTaskBenches(taskId)));
    }

    /**
     * 执行巡检任务：提交范围内长凳的巡检记录并关联任务。
     */
    @PostMapping("/tasks/{taskId}/execute")
    public ResponseEntity<ApiResponse<List<InspectionDTO>>> executeTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskExecuteRequest request) {
        List<InspectionDTO> created = planService.executeTask(taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务执行成功", created));
    }

    /**
     * 手动触发为启用中的计划生成到期任务。
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Integer>> generateTasks() {
        int created = planService.generateDueTasks();
        return ResponseEntity.ok(ApiResponse.success("成功生成" + created + "个待执行任务", created));
    }
}
