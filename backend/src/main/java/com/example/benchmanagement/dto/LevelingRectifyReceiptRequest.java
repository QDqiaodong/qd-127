package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 整改回执：整改完成后必须写回执（整改说明 + 提交人）才能拿掉“待整改”标记。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelingRectifyReceiptRequest {

    /** 整改情况说明（回执内容），必填 */
    @NotBlank(message = "整改回执内容不能为空")
    private String rectifyNote;

    /** 回执提交人，必填 */
    @NotBlank(message = "回执提交人不能为空")
    private String rectifyBy;
}
