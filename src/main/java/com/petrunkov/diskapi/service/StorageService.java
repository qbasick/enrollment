package com.petrunkov.diskapi.service;

import com.petrunkov.diskapi.dto.SystemItemDto;
import com.petrunkov.diskapi.dto.SystemItemHistoryResponse;
import com.petrunkov.diskapi.dto.SystemItemImportRequest;

import java.time.Instant;

public interface StorageService {
    void importItem(SystemItemImportRequest request);
    void deleteItem(String id, Instant time);
    SystemItemDto getItemInfo(String id);
    SystemItemHistoryResponse getItemUpdates(Instant dateTime);
    SystemItemHistoryResponse getItemHistory(String id, Instant getStart, Instant getEnd);

}
