package com.petrunkov.diskapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemItemHistoryUnit {
    @NotNull
    private String id;
    private String url;
    @NotNull
    private String date;
    private String parentId;
    private String type;
    private Long size;
}
