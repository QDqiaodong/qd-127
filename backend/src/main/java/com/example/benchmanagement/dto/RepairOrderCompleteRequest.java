package com.example.benchmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 完成维修工单请求：必须填写维修结果和完成时间。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrderCompleteRequest {

    @NotBlank(message = "维修结果不能为空")
    private String repairResult;

    @NotNull(message = "维修完成时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
}
