package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
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
public class TreeNodeDTO {

    private Long id;

    private Long parentId;

    @NotNull(message = "层级不能为空")
    private Integer level;

    @NotBlank(message = "名称不能为空")
    private String name;

    private Integer sortOrder;

    private List<TreeNodeDTO> children;

    private String path;
}
