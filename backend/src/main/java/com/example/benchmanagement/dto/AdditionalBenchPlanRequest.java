package com.example.benchmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 加凳预案提交请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalBenchPlanRequest {

    /** 所属街区。表单先选街区，再选路段，后端同时校验归属关系。 */
    @NotNull(message = "请选择街区")
    private Long districtId;

    /** 投放路段（必须是所选街区下的二级节点） */
    @NotNull(message = "请选择投放路段")
    private Long sectionId;

    @NotNull(message = "请选择计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @NotNull(message = "加凳数量不能为空")
    @Positive(message = "加凳数量必须大于0")
    private Integer benchCount;

    /** 预案状态：1-待投放，2-已投放。新建只允许待投放，已投放通过核销产生。 */
    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
