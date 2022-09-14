package com.petrunkov.diskapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ValidationErrorException extends ResponseStatusException {


    public ValidationErrorException() {
        super(HttpStatus.BAD_REQUEST, "Validation Failed");
    }
}
