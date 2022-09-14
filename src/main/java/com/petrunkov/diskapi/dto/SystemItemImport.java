package com.petrunkov.diskapi.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;


@Data
@Builder
public class SystemItemImport {
    @NotNull
    private String id;
    private String url;

    private String parentId;
    private String type;
    private Long size;
}
