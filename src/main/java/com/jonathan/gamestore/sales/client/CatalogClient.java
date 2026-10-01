package com.jonathan.gamestore.sales.client;

import com.jonathan.gamestore.sales.dto.GameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", fallback = CatalogClientFallback.class)
public interface CatalogClient {

 @GetMapping("/api/games/{id}")
 GameResponse getGameById(@PathVariable("id") String id);
}