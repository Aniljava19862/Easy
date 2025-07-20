package com.easy.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/application")
public class ApplicationController {
    @PostMapping("/createapp")
    public ResponseEntity<String> createApplication(@RequestParam(required = true) String appName,
                                                    @RequestBody(required = true) String userId) {

    return ResponseEntity.ok("");
    }
}
