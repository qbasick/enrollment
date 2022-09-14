package com.petrunkov.diskapi.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
public class SystemItemImportRequest {
    @NotNull
    List<SystemItemImport> items;
    @NotNull
    String updateDate;
}
