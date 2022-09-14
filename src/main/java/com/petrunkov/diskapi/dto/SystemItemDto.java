package com.petrunkov.diskapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(name = "SystemItem")
public class SystemItemDto {
    @NotNull
    private String id;
    private String url;
    @NotNull
    private String date;
    private String parentId;
    private String type;
    private Long size;
    private List<SystemItemDto> children;
}
