package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StockReservation {

    private String clientId;

    private String productId;

    private int quantity;
}
