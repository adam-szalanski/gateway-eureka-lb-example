package com.example.controller;

import com.example.dto.ReserveProductRequest;
import com.example.dto.StockReservationProjection;
import com.example.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping( "/stocks")
    ResponseEntity<List<StockReservationProjection>> getAllStocks(@RequestHeader(name = "X-Unique-Id") String uniqueId) {
        MDC.put( "uniqueId", uniqueId);
        log.info("Received request for all stocks");
        var reservations = warehouseService.getStockReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping( "/stocks/{clientId}")
    ResponseEntity<StockReservationProjection> getStock(@RequestHeader(name = "X-Unique-Id") String uniqueId, @PathVariable String clientId) {
        MDC.put( "uniqueId", uniqueId);
        log.info("Received read stock request for client: {}", clientId);
        if(clientId == null || clientId.isBlank()){
            return ResponseEntity.badRequest().build();
        }
        var stock = warehouseService.getStock(clientId);
        if(stock == null){
            log.info("No stock found for client: {}", clientId);
            return ResponseEntity.notFound().build();
        }
        log.info("Found stock for client: {}", clientId);
        return ResponseEntity.ok().body(stock);
    }

    @PostMapping( "/stocks")
    ResponseEntity<Void> createOrUpdateStock(@RequestHeader(name = "X-Unique-Id") String uniqueId, @RequestBody ReserveProductRequest request) {
        MDC.put( "uniqueId", uniqueId);
        log.info("Received request to reserve stock for product: {}", request.id());
        if (request.quantity() < 1){
            return ResponseEntity.badRequest().build();
        }
        warehouseService.reserveStock(request, uniqueId);
        log.info("Stock reserved for product: {}", request.id());
        return ResponseEntity.ok().build();
    }
}
