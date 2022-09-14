package com.petrunkov.diskapi.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ItemNotFoundException extends ResponseStatusException {

    public ItemNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Item not found");
    }
}
