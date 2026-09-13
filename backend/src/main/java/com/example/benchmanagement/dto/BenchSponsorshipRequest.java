package com.example.benchmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 商户长凳冠名提交请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchSponsorshipRequest {

    /** 冠名点位（必须是三级节点） */
    @NotNull(message = "请选择冠名点位")
    private Long pointId;

    @NotBlank(message = "请填写商户名称")
    @Size(max = 100, message = "商户名称不能超过100个字符")
    private String merchantName;

    @NotBlank(message = "请填写冠名文案")
    @Size(max = 200, message = "冠名文案不能超过200个字符")
    private String sponsorshipText;

    @NotNull(message = "请选择冠名开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "请选择冠名结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
