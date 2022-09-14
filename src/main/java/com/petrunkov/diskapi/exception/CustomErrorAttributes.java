package com.petrunkov.diskapi.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorMap = super.getErrorAttributes(webRequest, options);
        Map<String, Object> customMap = new HashMap<>();

        customMap.put("code", errorMap.get("status"));
        customMap.put("message", errorMap.get("message"));

        return customMap;
    }
}
