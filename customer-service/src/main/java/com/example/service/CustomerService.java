package com.example.service;

import com.example.dto.ClientRequest;
import com.example.dto.ReserveProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static com.example.config.ApplicationConfig.UNIQUE_ID_HEADER;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private static final Map<String, String> nameToProductId = Map.ofEntries(
            Map.entry("Product-1", "ce54687c-d6b1-4688-81a4-a736363f2ddf"),
            Map.entry("Product-2", "2417c119-d964-4229-a77d-8adf40acd266"),
            Map.entry("Product-3", "3b92b3ac-163c-46da-adf3-31de486ce0c2"),
            Map.entry("Product-4", "435e6a16-d4c8-43af-90cc-f1b6f444f9d4")
    );

    private final WebClient webClient;

    public void reserveStock(ClientRequest clientRequest, String uniqueId){
        var productId = nameToProductId.get(clientRequest.productName());
        if(productId == null){
            log.error("Invalid product name");
            throw new IllegalArgumentException("Invalid product name");
        }
        var stockRequest = new ReserveProductRequest(productId, clientRequest.quantity());
        log.info("Sending stock request to warehouse service: {}", stockRequest);

        webClient.post().uri("http://warehouse-service/api/v1/warehouse/stocks")
                .bodyValue(stockRequest)
                .header(UNIQUE_ID_HEADER, uniqueId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
