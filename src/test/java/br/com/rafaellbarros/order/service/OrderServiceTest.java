package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.repository.OrderItemRepository;
import br.com.rafaellbarros.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderItemRepository itemRepository;

    @InjectMocks
    private OrderService orderService;

    private Order validOrder;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        item = new OrderItem();
        item.setPrice(BigDecimal.valueOf(100.00));
        item.setQuantity(2);

        validOrder = new Order();
        validOrder.setId("1");
        validOrder.setExternalId("order-123");
        validOrder.setStatus(OrderStatus.RECEIVED);
        validOrder.setItems(List.of(item));
        validOrder.setCreatedAt(LocalDateTime.now());
        validOrder.setTraceId(UUID.randomUUID().toString());
        validOrder.setProcessingMessage("Test order");
    }

    @Test
    void shouldReceiveOrderSuccessfully() {
        given(repository.findByExternalId("order-123")).willReturn(Optional.empty());
        given(itemRepository.saveAll(anyList())).willReturn(List.of(item));
        given(repository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.receive(validOrder);

        assertNotNull(result);
        assertEquals(OrderStatus.RECEIVED, result.getStatus());
        assertEquals("order-123", result.getExternalId());
        assertNotNull(result.getTraceId());

        then(repository).should().save(any(Order.class));
        then(itemRepository).should().saveAll(anyList());
    }

    @Test
    void shouldThrowConflictWhenOrderIsDuplicated() {
        given(repository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.receive(validOrder));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        then(repository).should(never()).save(any());
        then(itemRepository).should(never()).saveAll(any());
    }

    @Test
    void shouldThrowBadRequestWhenOrderHasNoProducts() {
        Order orderWithoutProducts = new Order();
        orderWithoutProducts.setExternalId("order-456");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.receive(orderWithoutProducts));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        then(repository).should(never()).save(any());
        then(itemRepository).should(never()).saveAll(any());
    }

    @Test
    void shouldReceiveAllOrdersSuccessfully() {
        Order order2 = new Order();
        order2.setExternalId("order-456");
        order2.setItems(List.of(item));

        given(repository.findByExternalId(anyString())).willReturn(Optional.empty());
        given(itemRepository.saveAll(anyList())).willReturn(List.of(item));
        given(repository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Order> savedOrders = orderService.receiveAll(List.of(validOrder, order2));

        assertEquals(2, savedOrders.size());
        then(repository).should().saveAll(anyList());
        then(itemRepository).should(times(2)).saveAll(anyList());
    }

    @Test
    void shouldIgnoreInvalidOrdersInReceiveAll() {
        Order invalid = new Order();

        given(repository.findByExternalId(anyString())).willReturn(Optional.empty());
        given(itemRepository.saveAll(anyList())).willReturn(List.of(item));
        given(repository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Order> saved = orderService.receiveAll(List.of(validOrder, invalid));

        assertEquals(1, saved.size());
        assertEquals(validOrder.getExternalId(), saved.get(0).getExternalId());
        then(itemRepository).should().saveAll(anyList());
        then(repository).should().saveAll(anyList());
    }

    @Test
    void shouldThrowConflictWhenAllOrdersInvalidOrDuplicated() {
        Order duplicated = new Order();
        duplicated.setExternalId("order-123");
        duplicated.setItems(List.of(item));

        Order invalid = new Order();

        given(repository.findByExternalId("order-123")).willReturn(Optional.of(duplicated));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.receiveAll(List.of(duplicated, invalid)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        then(repository).should(never()).saveAll(anyList());
        then(itemRepository).should(never()).saveAll(anyList());
    }

    @Test
    void shouldReturnOrderWhenExternalIdExists() {
        given(repository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        Optional<Order> result = orderService.getOrderByEsternalId("order-123");

        assertTrue(result.isPresent());
        assertEquals("order-123", result.get().getExternalId());
        then(repository).should().findByExternalId("order-123");
    }

    @Test
    void shouldReturnEmptyWhenExternalIdNotFound() {
        given(repository.findByExternalId("not-found")).willReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderByEsternalId("not-found");

        assertTrue(result.isEmpty());
        then(repository).should().findByExternalId("not-found");
    }

    @Test
    void shouldReturnOrdersByStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        List<Order> mockOrders = List.of(validOrder);

        given(repository.findByStatus(status)).willReturn(mockOrders);

        List<Order> result = orderService.getOrdersByStatus(status);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.RECEIVED, result.get(0).getStatus());
        then(repository).should().findByStatus(status);
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersWithStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        given(repository.findByStatus(status)).willReturn(Collections.emptyList());

        List<Order> result = orderService.getOrdersByStatus(status);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        then(repository).should().findByStatus(status);
    }

    @Test
    void shouldThrowExceptionWhenReceiveAllWithEmptyList() {
        List<Order> emptyList = Collections.emptyList();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.receiveAll(emptyList)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lista de pedidos não pode estar vazia", exception.getReason());
    }
}
