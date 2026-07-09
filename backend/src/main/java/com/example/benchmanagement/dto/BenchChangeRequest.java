package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchChangeRequest {

    @NotNull(message = "长凳ID列表不能为空")
    private List<Long> benchIds;

    @NotNull(message = "新点位ID不能为空")
    private Long newNodeId;

    private String changeReason;
}
