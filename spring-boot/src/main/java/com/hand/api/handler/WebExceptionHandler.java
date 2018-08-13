package com.hand.api.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class WebExceptionHandler
{
    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception ex)
    {
        Map<String,Object> map = new HashMap();
        map.put("type",ex.getClass().getName());
        map.put("message",ex.getMessage());

        return new ResponseEntity(map,HttpStatus.OK);
    }
}
