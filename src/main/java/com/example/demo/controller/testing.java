package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testing {

    @GetMapping("/hi")
    public String Hello() {
        return "Hello this is my intern project";
    }
}
