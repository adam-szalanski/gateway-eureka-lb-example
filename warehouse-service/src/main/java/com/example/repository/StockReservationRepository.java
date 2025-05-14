package com.example.repository;

import com.example.model.StockReservation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class StockReservationRepository {

    private static final Map<String, StockReservation> RESERVATION_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public Optional<StockReservation> get(String id){
        return Optional.ofNullable(RESERVATION_CONCURRENT_HASH_MAP.get(id));
    }

    public List<StockReservation> getAll(){
        return RESERVATION_CONCURRENT_HASH_MAP.values().stream().toList();
    }

    public void save(StockReservation stockReservation) {
        if (stockReservation == null){
            throw new IllegalArgumentException("StockReservation cannot be null");
        }

        RESERVATION_CONCURRENT_HASH_MAP.compute(stockReservation.getClientId(),
                (id, existingReservation) -> {
                    if (existingReservation == null) {
                        return stockReservation;
                    } else {
                        existingReservation.setQuantity(
                                existingReservation.getQuantity() + stockReservation.getQuantity()
                        );
                        return existingReservation;
                    }
                });
    }
}
