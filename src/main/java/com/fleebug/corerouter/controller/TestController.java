package com.fleebug.corerouter.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class TestController {
    
    @GetMapping("/")
    public String getMethodName() {
        return new String("HELLO WORD");
    }
    
}
