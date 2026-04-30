package com.hstk.shortlink.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateShortLinkStatusRequest {
    @NotNull(message="状态不能为空")
    private Integer status;
}
