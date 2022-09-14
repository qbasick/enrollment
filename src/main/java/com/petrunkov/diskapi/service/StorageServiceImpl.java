package com.petrunkov.diskapi.service;

import com.petrunkov.diskapi.dto.SystemItemDto;
import com.petrunkov.diskapi.dto.SystemItemHistoryResponse;
import com.petrunkov.diskapi.dto.SystemItemImport;
import com.petrunkov.diskapi.dto.SystemItemImportRequest;
import com.petrunkov.diskapi.exception.ItemNotFoundException;
import com.petrunkov.diskapi.exception.ValidationErrorException;
import com.petrunkov.diskapi.mapper.SystemItemMapper;
import com.petrunkov.diskapi.model.SystemItem;
import com.petrunkov.diskapi.model.SystemItemArchived;
import com.petrunkov.diskapi.model.SystemItemType;
import com.petrunkov.diskapi.repository.ArchiveRepository;
import com.petrunkov.diskapi.repository.StorageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final StorageRepository repository;
    private final ArchiveRepository archive;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void importItem(SystemItemImportRequest request) {

        //Валидация текстовых данных
        Instant instant;
        try {
            instant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(request.getUpdateDate()));
        } catch (Exception e) {
            throw new ValidationErrorException();
        }
        validateSchema(request);

        List<SystemItem> systemItems = request.getItems().stream()
                .map(item -> SystemItemMapper.mapFromSystemItemImport(item, instant)).toList();

        // Обработать 3 случая: обновление и сохранение parentId, обновление и смена parentId, вставка нового элемента
        for (SystemItem newItem : systemItems) {
            // Проверка типа parentId. В случае FILE ошибка и роллбэк
            if (newItem.getParentId() != null) {
                Optional<SystemItem> parent = repository.findById(newItem.getParentId());
                if (parent.isEmpty()) {
                    throw new ValidationErrorException();
                }
                if (parent.get().getType() == SystemItemType.FILE) {
                    log.warn("Validation Error. Transaction rollback");
                    throw new ValidationErrorException();
                }
            }

            archive.save(SystemItemMapper.mapToArchive(newItem));

            Optional<SystemItem> fromStorage = repository.findById(newItem.getId());
            long newItemSize = newItem.getSize() == null ? 0L : newItem.getSize();
            // Случай обновления
            if (fromStorage.isPresent()) {
                SystemItem oldItem = fromStorage.get();
                // Проверка на смену типа. Если тип изменен - ошибка и роллбэк
                if (oldItem.getType() != newItem.getType()) {
                    log.warn("Validation error. Transaction rollback");
                    throw new ValidationErrorException();
                }
                long oldItemSize = oldItem.getSize() == null ? 0L : oldItem.getSize();

                // Сохранить размер папки в случае обновления
                if (newItem.getType() == SystemItemType.FOLDER) {
                    newItem.setSize(oldItemSize);
                    newItemSize = oldItemSize;
                }
                // Случай, когда parentId не меняется

                if ((oldItem.getParentId() == null && newItem.getParentId() == null) ||
                                oldItem.getParentId() != null &&
                                newItem.getParentId() != null &&
                                oldItem.getParentId().equals(newItem.getParentId())) {
                    repository.save(newItem);
                    updatePredecessors(repository.getPredecessors(oldItem.getId()), instant, newItemSize - oldItemSize);
                // Случай со сменой parentId. Требуется обновление дерева в двух местах
                } else {
                    repository.save(newItem);
                    updatePredecessors(repository.getPredecessors(oldItem.getId()), instant, -oldItemSize);
                    updatePredecessors(repository.getPredecessors(newItem.getId()), instant, newItemSize);
                }
            // Вставка нового элемента
            } else {
                repository.save(newItem);
                updatePredecessors(repository.getPredecessors(newItem.getId()), instant, newItemSize);
            }
        }
        log.info("Items successfully imported");

    }

    /**
     * Обновить всех предшественников по parentId и добавить в архив изменения
     * **/
    private void updatePredecessors(Collection<SystemItem> items, Instant updateTime, long sizeChange) {
        items.forEach(systemItem -> {
            systemItem.setDate(updateTime);
            long size = systemItem.getSize() == null ? 0 :systemItem.getSize();
            systemItem.setSize(size + sizeChange);
        });
        addToArchive(items);
        repository.saveAllAndFlush(items);
    }
    private void addToArchive(Collection<SystemItem> items) {
        archive.saveAll(items.stream().map(SystemItemMapper::mapToArchive).collect(Collectors.toList()));
    }

    @Override
    public SystemItemDto getItemInfo(String id) {

        Map<String, List<SystemItem>> map = new HashMap<>();
        // Получить все элементы из запроса
        Set<SystemItem> itemSet = repository.getItemInfo(id);
        final SystemItem[] root = {null};
        // Реорганизовать в хэш-таблицу где ключ - Id, значение - список потомков
        itemSet.forEach(item -> {
            if (item.getId().equals(id)) {
                root[0] = item;
            } else {
                map.putIfAbsent(item.getParentId(), new ArrayList<>());
                map.get(item.getParentId()).add(item);
            }});
        // Элемент не найден
        if (root[0] == null) {
            throw new ItemNotFoundException();
        }
        // Элемент - файл, построение дерева не требуется
        if (root[0].getType() == SystemItemType.FILE) {
            return SystemItemMapper.mapToDto(root[0]);
        }
        // Элемент - папка, требуется построить дерево из хэш-таблицы
        return buildTree(root[0], map);
    }

    /**
     * Построение дерева для запроса получения информации о папке
     * **/
    private SystemItemDto buildTree(SystemItem root, Map<String, List<SystemItem>> map) {
        LinkedList<SystemItem> queue = new LinkedList<>(map.get(root.getId()));
        Map<String, SystemItemDto> dtoMap = new HashMap<>();
        SystemItemDto dtoRoot = SystemItemMapper.mapToDto(root);
        SystemItemDto dtoCur = dtoRoot;

        while (!queue.isEmpty()) {
            while (queue.peek() != null && queue.peek().getParentId().equals(dtoCur.getId())) {

                SystemItem cur = queue.poll();
                SystemItemDto newDto = SystemItemMapper.mapToDto(cur);
                dtoMap.put(newDto.getId(), newDto);

                dtoCur.getChildren().add(newDto);
                if (cur.getType() == SystemItemType.FOLDER) {
                    queue.addAll(map.get(cur.getId()));
                }
            }
            if (queue.peek() != null) {
                dtoCur = dtoMap.get(queue.peek().getParentId());
            }
        }

        return dtoRoot;
    }


    /**
     * Удалить элемент из основного хранилища и из архива
     * **/
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteItem(String id, Instant dateTime) {
        SystemItem item = repository.findById(id).orElseThrow(ItemNotFoundException::new);
        long size = item.getSize() == null ? 0 : -item.getSize();
        // Обновить дерево по parentId
        updatePredecessors(repository.getPredecessors(item.getId()), dateTime, size);
        // Удалить файл
        if (item.getType() == SystemItemType.FILE) {
            archive.deleteFileById(id);
            repository.delete(item);
        // Удалить папку
        } else {
            archive.deleteFolderById(id);
            repository.deleteFolder(id);
        }
        log.info("Элемент с id {} удалён", id);
    }

    @Override
    public SystemItemHistoryResponse getItemUpdates(Instant instant) {
        return new SystemItemHistoryResponse(
                repository
                        .getFileHistory(instant.minus(24, ChronoUnit.HOURS), instant)
                        .stream()
                        .map(SystemItemMapper::mapToHistoryUnit)
                        .collect(Collectors.toList()));
    }

    @Override
    public SystemItemHistoryResponse getItemHistory(String id, Instant dateStart, Instant dateEnd) {
        Collection<SystemItemArchived> archivedItems = archive.getHistoryInterval(id, dateStart, dateEnd.minusMillis(1));
        if (archivedItems.size() == 0) {
            throw new ItemNotFoundException();
        }
        return new SystemItemHistoryResponse(
                archivedItems.stream()
                        .map(SystemItemMapper::mapToHistoryUnitFromArchive)
                        .collect(Collectors.toList()));
    }

    /**
     * Проверка текстовых данных, которые можно обработать без обращения к базе данных
     * **/
    private void validateSchema(SystemItemImportRequest request) {
        List<SystemItemImport> importList = request.getItems();
        Set<String> set = importList.stream().map(SystemItemImport::getId).collect(Collectors.toSet());
        if (set.size() != importList.size()) {
            throw new ValidationErrorException();
        }

        Predicate<SystemItemImport> nullTest = i -> i.getId() != null;
        Predicate<SystemItemImport> folderUrlTest = i -> !(i.getType().equals("FOLDER") && i.getUrl() != null);
        Predicate<SystemItemImport> folderSizeTest = i -> !(i.getType().equals("FOLDER") && i.getSize() != null);
        Predicate<SystemItemImport> fileUrlTest = i -> !(i.getType().equals("FILE") && (i.getUrl() == null || i.getUrl().length() > 255));
        Predicate<SystemItemImport> fileSizeTest = i -> !(i.getType().equals("FILE") && (i.getSize() == null || i.getSize() == 0));

        List<Predicate<SystemItemImport>> predicateList = List.of(nullTest, folderUrlTest, folderSizeTest, fileUrlTest, fileSizeTest);


        if (!request.getItems().stream()
                .allMatch(item -> predicateList.stream()
                        .allMatch(v -> v.test(item)))) {
            throw new ValidationErrorException();
        }

    }


}
