package com.github.konstantinevashalomidze.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RequestMapping("/api/v1/health")
@RestController
public class HealthCheck {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return new ResponseEntity<>(Map.of(
                "status", "OK",
                "version", "1.0.0"
        ), HttpStatus.OK);
    }
}