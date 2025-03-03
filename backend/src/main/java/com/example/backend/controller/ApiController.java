package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // For development only
public class ApiController {

    @GetMapping("/status")
    public Map getStatus() {
        Map response = new HashMap<>();
        response.put("status", "running");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}