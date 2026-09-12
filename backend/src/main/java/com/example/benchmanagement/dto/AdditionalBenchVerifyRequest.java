package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加凳核销请求：支持按实际投放数量分批核销。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalBenchVerifyRequest {

    @NotNull(message = "核销数量不能为空")
    @Positive(message = "核销数量必须大于0")
    private Integer verifiedCount;

    @NotBlank(message = "请填写核销操作人")
    @Size(max = 50, message = "操作人不能超过50个字符")
    private String operator;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
