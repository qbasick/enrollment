package com.petrunkov.diskapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "Error")
@Data
public class ErrorBody {
    private Integer code;
    private String message;
}
