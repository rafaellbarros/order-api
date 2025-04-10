package br.com.rafaellbarros.order.controller;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> receive(final @RequestBody @Valid Order request) {
        var createdOrder = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create("/v1/orders/" + createdOrder.getId()))
                .body(createdOrder);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Order>> createOrders(@RequestBody @Valid List<Order> requests) {
        List<Order> createdOrders = orderService.createOrders(requests);
        return ResponseEntity
                .created(URI.create("/v1/orders/batch"))
                .body(createdOrders);
    }


    @GetMapping("/external-id/{externalId}")
    public ResponseEntity<Order> getOrderByExternalId(@PathVariable String externalId) {
        return orderService.getOrderByExternalId(externalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }


}
