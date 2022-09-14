package com.petrunkov.diskapi.controller;

import com.petrunkov.diskapi.dto.SystemItemDto;
import com.petrunkov.diskapi.dto.SystemItemHistoryResponse;
import com.petrunkov.diskapi.dto.SystemItemImportRequest;
import com.petrunkov.diskapi.exception.ErrorBody;
import com.petrunkov.diskapi.exception.ValidationErrorException;
import com.petrunkov.diskapi.service.StorageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@RestController
@AllArgsConstructor
public class StorageController {
    private final StorageServiceImpl storageService;


    @Operation(summary = "Импорт элементов файловой системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Вставка или обновление прошли успешно"),
            @ApiResponse(responseCode = "400",
                    description = "Невалидная схема документа или входные данные не верны",
                    content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorBody.class)))
    })

    @PostMapping("/imports")
    @ResponseStatus(code = HttpStatus.OK)
    public void importFiles(@RequestBody SystemItemImportRequest request) {
        storageService.importItem(request);
    }


    @Operation(summary = "Получить информацию об элементе по идентификатору. " +
            "При получении информации о папке также предоставляется информация о её дочерних элементах.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Информация об элементе",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemItemDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Невалидная схема документа или входные данные не верны",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404",
                    description = "Элемент не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class)))
    })

    @GetMapping("/nodes/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public SystemItemDto getNodeInfo(@Parameter(description = "Идентификатор элемента") @PathVariable String id) {
        return storageService.getItemInfo(id);
    }

    @Operation(summary = "Удалить элемент по идентификатору. " +
            "При удалении папки удаляются все дочерние элементы. " +
            "Доступ к истории обновлений удаленного элемента невозможен.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Удаление прошло успешно"),
            @ApiResponse(responseCode = "400",
                    description = "Невалидная схема документа или входные данные не верны",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404",
                    description = "Элемент не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class)))
    })

    @ResponseStatus(code = HttpStatus.OK)
    @DeleteMapping("/delete/{id}")
    public void deleteNode(@Parameter(description = "Идентификатор элемента") @PathVariable String id,
                           @Parameter(description = "Дата и время запроса") @RequestParam(name = "date") String date) {
        Instant instant;
        try {
            instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
        } catch (Exception e) {
            throw new ValidationErrorException();
        }
        storageService.deleteItem(id, instant);
    }

    @Operation(summary = "Получение списка файлов, которые были обновлены за последние 24 часа включительно от времени переданном в запросе.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Список обновленных файлов",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemItemHistoryResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Невалидная схема документа или входные данные не верны",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class)))
    })

    @GetMapping("/updates")
    @ResponseStatus(code = HttpStatus.OK)
    public SystemItemHistoryResponse getUpdatesIn24h(@Parameter(description = "Дата и время запроса") @RequestParam String date) {
        Instant instant;
        try {
            instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
        } catch (Exception e) {
            throw new ValidationErrorException();
        }
        return storageService.getItemUpdates(instant);
    }

    @Operation(summary = "Получение истории обновлений по элементу за заданный полуинтервал. История по удаленным элементам недоступна")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "История по элементу",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SystemItemHistoryResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Невалидная схема документа или входные данные не верны",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404",
                    description = "Элемент не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorBody.class)))

    })

    @GetMapping("/node/{id}/history")
    @ResponseStatus(code = HttpStatus.OK)
    public SystemItemHistoryResponse getNodeHistory(@Parameter(description = "id элемента для которого будет отображаться история")
                                                        @PathVariable String id,
                                                    @Parameter(description = "Дата и время начала интервала")
                                                        @RequestParam String dateStart,
                                                    @Parameter(description = "Дата и время конца интервала")
                                                        @RequestParam String dateEnd) {
        Instant start;
        Instant end;
        try {
            start = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(dateStart));
            end = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(dateEnd));
        } catch (Exception e) {
            throw new ValidationErrorException();
        }
        return storageService.getItemHistory(id, start, end);
    }

}
