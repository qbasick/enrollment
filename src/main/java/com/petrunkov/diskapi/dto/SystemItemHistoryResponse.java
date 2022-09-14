package com.petrunkov.diskapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemItemHistoryResponse {
    List<SystemItemHistoryUnit> items;
}
