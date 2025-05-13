package com.example.service;

import com.example.dto.ReserveProductRequest;
import com.example.dto.StockReservationProjection;
import com.example.model.StockReservation;
import com.example.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseService {

    private final StockReservationRepository reservationRepository;

    public List<StockReservationProjection> getStockReservations(){
        return reservationRepository.getAll().stream()
                .map(reservation -> new StockReservationProjection(reservation.getProductId(), reservation.getQuantity()))
                .toList();
    }

    public void reserveStock(ReserveProductRequest productRequest, String clientId){
        var stock = new StockReservation(clientId ,productRequest.id(), productRequest.quantity());
        reservationRepository.save(stock);
    }

    public StockReservationProjection getStock(String clientId){
        var reservation = reservationRepository.get(clientId);

        if(reservation.isPresent()){
            log.info("Found reservation for client: {}", clientId);
            return new StockReservationProjection(reservation.get().getProductId(), reservation.get().getQuantity());
        }

        log.info("No reservation found for client: {}", clientId);
        return null;
    }
}
