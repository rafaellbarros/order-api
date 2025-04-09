package br.com.rafaellbarros.order.controller;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
public class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;


    @Test
    void testReceive() {
        Order request = buildOrder();
        when(orderService.receive(request)).thenReturn(request);

        ResponseEntity<Order> response = orderController.receive(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(request, response.getBody());
        verify(orderService).receive(request);
    }

    @Test
    void testReceiveAll() {
        Order order1 = buildOrder();
        Order order2 = buildOrder();
        order2.setExternalId("order-123"); // muda para não ser duplicado

        List<Order> requestList = List.of(order1, order2);
        when(orderService.receiveAll(requestList)).thenReturn(requestList);

        ResponseEntity<List<Order>> response = orderController.receiveAll(requestList);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(orderService).receiveAll(requestList);
    }

    @Test
    void testGetOrderByEsternalIdFound() {
        Order order = buildOrder();
        when(orderService.getOrderByEsternalId("order-123")).thenReturn(Optional.of(order));

        ResponseEntity<Order> response = orderController.getOrderByEsternalId("order-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(order, response.getBody());
        verify(orderService).getOrderByEsternalId("order-123");
    }

    @Test
    void testGetOrderByEsternalIdNotFound() {
        when(orderService.getOrderByEsternalId("not-exist")).thenReturn(Optional.empty());

        ResponseEntity<Order> response = orderController.getOrderByEsternalId("not-exist");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(orderService).getOrderByEsternalId("not-exist");
    }

    @Test
    void testGetOrdersByStatus() {
        Order order1 = buildOrder();
        Order order2 = buildOrder();
        List<Order> orders = List.of(order1, order2);

        when(orderService.getOrdersByStatus(OrderStatus.RECEIVED)).thenReturn(orders);

        ResponseEntity<List<Order>> response = orderController.getOrdersByStatus(OrderStatus.RECEIVED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(orderService).getOrdersByStatus(OrderStatus.RECEIVED);
    }


    private Order buildOrder() {
        OrderItem item = new OrderItem("Notebook", new BigDecimal("3500.00"), 1);
        Order order = new Order();
        order.setId("1");
        order.setExternalId("order-123");
        order.setItems(List.of(item));
        order.setStatus(OrderStatus.RECEIVED);
        order.setTotalAmount(new BigDecimal("3500.00"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdateAt(LocalDateTime.now());
        order.setTraceId("trace-xyz");
        order.setProcessingMessage("Order received successfully");
        return order;
    }
}
