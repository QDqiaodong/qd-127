package com.example.benchmanagement.controller;

import com.example.benchmanagement.dto.ApiResponse;
import com.example.benchmanagement.dto.RepairOrderCompleteRequest;
import com.example.benchmanagement.dto.RepairOrderCreateRequest;
import com.example.benchmanagement.dto.RepairOrderDTO;
import com.example.benchmanagement.service.RepairOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair-order")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    /**
     * 手动为异常长凳创建维修工单（巡检异常工单一般由巡检流程自动生成）。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RepairOrderDTO>> createOrder(
            @Valid @RequestBody RepairOrderCreateRequest request) {
        RepairOrderDTO created = repairOrderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("工单创建成功", created));
    }

    /**
     * 工单列表，可按状态、严重程度、关键字筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepairOrderDTO>>> getOrders(
            @RequestParam(value = "status", required = false) Long status,
            @RequestParam(value = "severity", required = false) Integer severity,
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<RepairOrderDTO> orders = repairOrderService.getOrders(status, severity, keyword);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairOrderDTO>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(repairOrderService.getOrderById(id)));
    }

    @GetMapping("/bench/{benchId}")
    public ResponseEntity<ApiResponse<List<RepairOrderDTO>>> getOrdersByBench(@PathVariable Long benchId) {
        return ResponseEntity.ok(ApiResponse.success(repairOrderService.getOrdersByBench(benchId)));
    }

    /** 待处理 -> 维修中 */
    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<RepairOrderDTO>> startOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("已开始维修", repairOrderService.startOrder(id)));
    }

    /** 维修中 -> 已完成，必须填写维修结果和完成时间 */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RepairOrderDTO>> completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody RepairOrderCompleteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("维修完成", repairOrderService.completeOrder(id, request)));
    }

    /** 待处理/已完成 -> 已关闭 */
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<RepairOrderDTO>> closeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("工单已关闭", repairOrderService.closeOrder(id)));
    }
}
