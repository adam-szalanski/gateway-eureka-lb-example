package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/dummy")
public class DummyController {
    @GetMapping
    ResponseEntity<String> respondWithUniqueId(@RequestHeader(required = false, name = "X-Unique-Id") String uniqueId) {
        log.info("Received request with unique id: {}", uniqueId);
        return ResponseEntity.ok(uniqueId);
    }
}
