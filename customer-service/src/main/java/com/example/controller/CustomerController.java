package com.example.controller;

import com.example.dto.ClientRequest;
import com.example.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/reserve")
    ResponseEntity<String> respondWithUniqueId(@RequestHeader(name = "X-Unique-Id") String uniqueId, @RequestBody ClientRequest clientRequest) {
        MDC.put("uniqueId", uniqueId);
        log.info("Received client request for stock reservation: {} with id: {}", clientRequest, uniqueId);
        customerService.reserveStock(clientRequest, uniqueId);
        log.info("Stock reserved successfully");
        return ResponseEntity.ok().body(uniqueId);
    }
}
