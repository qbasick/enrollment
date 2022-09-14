package com.petrunkov.diskapi.mapper;

import com.petrunkov.diskapi.dto.SystemItemDto;
import com.petrunkov.diskapi.dto.SystemItemHistoryUnit;
import com.petrunkov.diskapi.dto.SystemItemImport;
import com.petrunkov.diskapi.model.SystemItem;
import com.petrunkov.diskapi.model.SystemItemArchived;
import com.petrunkov.diskapi.model.SystemItemType;

import java.time.Instant;
import java.util.ArrayList;

public class SystemItemMapper {

    public static SystemItemHistoryUnit mapToHistoryUnit(SystemItem systemItem) {
        return SystemItemHistoryUnit.builder()
                .id(systemItem.getId())
                .url(systemItem.getUrl())
                .date(systemItem.getDate().toString())
                .parentId(systemItem.getParentId())
                .type(systemItem.getType().toString())
                .size(systemItem.getSize())
                .build();
    }

    public static SystemItemDto mapToDto(SystemItem root) {
        return SystemItemDto.builder()
                .id(root.getId())
                .url(root.getUrl())
                .date(root.getDate().toString())
                .parentId(root.getParentId())
                .type(root.getType().toString())
                .size(root.getSize())
                .children(root.getType() == SystemItemType.FOLDER ? new ArrayList<>() : null)
                .build();

    }

    public static SystemItem mapFromSystemItemImport(SystemItemImport itemImport, Instant instant) {
        return new SystemItem(
                itemImport.getId(),
                itemImport.getUrl(),
                instant,
                itemImport.getParentId(),
                SystemItemType.valueOf(itemImport.getType()),
                itemImport.getSize()
        );
    }

    public static SystemItemArchived mapToArchive(SystemItem systemItem) {
        return SystemItemArchived.builder()
                .id(systemItem.getId())
                .url(systemItem.getUrl())
                .date(systemItem.getDate())
                .parentId(systemItem.getParentId())
                .type(systemItem.getType())
                .size(systemItem.getSize())
                .build();
    }
    public static SystemItemHistoryUnit mapToHistoryUnitFromArchive(SystemItemArchived systemItem) {
        return SystemItemHistoryUnit.builder()
                .id(systemItem.getId())
                .url(systemItem.getUrl())
                .date(systemItem.getDate().toString())
                .parentId(systemItem.getParentId())
                .type(systemItem.getType().toString())
                .size(systemItem.getSize())
                .build();
    }
}
