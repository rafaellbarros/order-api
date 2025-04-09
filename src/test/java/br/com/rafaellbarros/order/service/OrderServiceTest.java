package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderRepository;
import br.com.rafaellbarros.order.service.helper.OrderFactory;
import br.com.rafaellbarros.order.service.helper.OrderLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;


    @Mock
    private OrderFactory factory;

    @Mock
    private OrderLogger orderLogger;

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
        given(factory.createOrder(validOrder)).willReturn(validOrder);
        given(repository.save(validOrder)).willReturn(validOrder);

        Order result = orderService.receive(validOrder);

        assertNotNull(result);
        assertEquals(OrderStatus.RECEIVED, result.getStatus());
        assertEquals("order-123", result.getExternalId());
        then(repository).should().findByExternalId("order-123");
        then(factory).should().createOrder(validOrder);
        then(repository).should().save(validOrder);
        then(orderLogger).should().logSavedOrder(validOrder);
    }

    @Test
    void shouldThrowConflictWhenOrderIsDuplicated() {
        given(repository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.receive(validOrder));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        then(orderLogger).should().logDuplicate("order-123");
        then(factory).should(never()).createOrder(any());
        then(repository).should(never()).save(any());
    }


    @Test
    void shouldReceiveAllOrdersSuccessfully() {
        Order order2 = new Order();
        order2.setExternalId("order-456");
        order2.setItems(List.of(item));

        given(repository.findByExternalId(anyString())).willReturn(Optional.empty());
        given(factory.createOrder(validOrder)).willReturn(validOrder);
        given(factory.createOrder(order2)).willReturn(order2);
        given(repository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Order> savedOrders = orderService.receiveAll(List.of(validOrder, order2));

        assertEquals(2, savedOrders.size());
        then(factory).should(times(2)).createOrder(any());
        then(repository).should(times(2)).findByExternalId(anyString());
        then(repository).should().saveAll(anyList());
    }

    @Test
    void shouldIgnoreInvalidOrdersInReceiveAll() {

        Order invalidOrder = new Order();
        invalidOrder.setExternalId("order-invalid");

        Order validOrder = new Order();
        validOrder.setExternalId("order-123");


        given(repository.findByExternalId("order-123")).willReturn(Optional.empty());
        given(factory.createOrder(validOrder)).willReturn(validOrder);


        willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido inválido"))
                .given(factory)
                .createOrder(argThat(order -> "order-invalid".equals(order.getExternalId())));


        given(repository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Order> saved = orderService.receiveAll(List.of(validOrder, invalidOrder));

        assertEquals(1, saved.size());
        assertEquals("order-123", saved.get(0).getExternalId());

        then(orderLogger).should().logInvalidIgnored(eq("order-invalid"), anyString());
        then(factory).should(times(1)).createOrder(validOrder);
    }



    @Test
    void shouldThrowConflictWhenAllOrdersDuplicated() {
        Order duplicatedOrder = new Order();
        duplicatedOrder.setExternalId("123");


        when(repository.findByExternalId(any())).thenReturn(Optional.of(duplicatedOrder));



        List<Order> list = List.of(duplicatedOrder, duplicatedOrder);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.receiveAll(list));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Nenhum pedido válido para processar", ex.getReason());
    }


    @Test
    void shouldReturnOrderWhenExternalIdExists() {
        given(repository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        Optional<Order> result = orderService.getOrderByEsternalId("order-123");

        assertTrue(result.isPresent());
        assertEquals("order-123", result.get().getExternalId());

        then(orderLogger).should().logSearchByExternalId("order-123");
        then(orderLogger).should().logFound("1");
    }

    @Test
    void shouldReturnEmptyWhenExternalIdNotFound() {
        given(repository.findByExternalId("not-found")).willReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderByEsternalId("not-found");

        assertTrue(result.isEmpty());
        then(orderLogger).should().logSearchByExternalId("not-found");
        then(orderLogger).should(never()).logFound(anyString());
    }

    @Test
    void shouldReturnOrdersByStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        List<Order> mockOrders = List.of(validOrder);

        given(repository.findByStatus(status)).willReturn(mockOrders);

        List<Order> result = orderService.getOrdersByStatus(status);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.RECEIVED, result.get(0).getStatus());
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersWithStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        given(repository.findByStatus(status)).willReturn(Collections.emptyList());

        List<Order> result = orderService.getOrdersByStatus(status);

        assertNotNull(result);
        assertTrue(result.isEmpty());
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
