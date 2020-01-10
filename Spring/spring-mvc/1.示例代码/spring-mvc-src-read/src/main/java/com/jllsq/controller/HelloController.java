package com.jllsq.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/v1")
public class HelloController {

    @GetMapping("/hello")
    public String hello(){
        return "Hello";
    }
}
