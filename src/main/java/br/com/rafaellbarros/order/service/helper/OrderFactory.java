package br.com.rafaellbarros.order.service.helper;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderFactory {

    private final OrderItemRepository repository;

    public Order createOrder(final Order request) {
        Order order = new Order();
        order.setExternalId(request.getExternalId());
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> savedItems = repository.saveAll(request.getItems());
        order.setItems(savedItems);

        order.setTraceId(UUID.randomUUID().toString());
        order.setProcessingMessage("Order received successfully");

        return order;
    }
}