package br.com.rafaellbarros.order.controller;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    @PostMapping("/receive")
    public ResponseEntity<Order> receive(final @RequestBody @Valid Order request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(request));
    }

    @PostMapping("/receive/all")
    public ResponseEntity<List<Order>> receiveAll(final @RequestBody @Valid List<Order> listRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.receiveAll(listRequest));
    }


    @GetMapping("/externalId/{id}")
    public ResponseEntity<Order> getOrderByEsternalId(@PathVariable final String id) {
        return service.getOrderByEsternalId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable final OrderStatus status) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getOrdersByStatus(status));
    }

}
