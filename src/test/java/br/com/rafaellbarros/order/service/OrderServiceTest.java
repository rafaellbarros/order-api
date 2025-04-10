package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.factory.OrderFactory;
import br.com.rafaellbarros.order.logger.OrderLogger;
import br.com.rafaellbarros.order.repository.OrderItemRepository;
import br.com.rafaellbarros.order.repository.OrderRepository;
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
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderFactory orderFactory;

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
    void shouldCreateOrderSuccessfully() {
        given(orderRepository.findByExternalId("order-123")).willReturn(Optional.empty());
        given(orderItemRepository.saveAll(validOrder.getItems())).willReturn(validOrder.getItems());
        given(orderFactory.createFrom(validOrder)).willReturn(validOrder);
        given(orderRepository.save(validOrder)).willReturn(validOrder);

        Order result = orderService.createOrder(validOrder);

        assertNotNull(result);
        assertEquals(OrderStatus.RECEIVED, result.getStatus());
        assertEquals("order-123", result.getExternalId());
        then(orderRepository).should().findByExternalId("order-123");
        then(orderItemRepository).should().saveAll(validOrder.getItems());
        then(orderLogger).should().sevedItems(validOrder.getItems());
        then(orderFactory).should().createFrom(validOrder);
        then(orderRepository).should().save(validOrder);
        then(orderLogger).should().saved(validOrder);
    }

    @Test
    void shouldThrowConflictWhenOrderIsDuplicated() {
        given(orderRepository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(validOrder));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        then(orderLogger).should().duplicated("order-123");
        then(orderItemRepository).shouldHaveNoInteractions();
        then(orderFactory).shouldHaveNoInteractions();
        then(orderRepository).should(never()).save(any());
    }

    @Test
    void shouldCreateAllValidOrdersSuccessfully() {
        Order order2 = new Order();
        order2.setExternalId("order-456");
        order2.setItems(List.of(item));

        given(orderRepository.findByExternalId(anyString())).willReturn(Optional.empty());
        given(orderItemRepository.saveAll(anyList())).willReturn(List.of(item));
        given(orderFactory.createFrom(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Order> savedOrders = orderService.createOrders(List.of(validOrder, order2));

        assertEquals(2, savedOrders.size());
        then(orderRepository).should(times(2)).findByExternalId(anyString());
        then(orderItemRepository).should(times(2)).saveAll(anyList());
        then(orderFactory).should(times(2)).createFrom(any(Order.class));
        then(orderRepository).should().saveAll(anyList());
        then(orderLogger).should().savedOrders(savedOrders);
    }

    @Test
    void shouldIgnoreOrderWhenResponseStatusExceptionOccurs() {
        Order order = new Order();
        order.setExternalId("order-123");
        order.setItems(List.of(new OrderItem()));

        given(orderRepository.findByExternalId("order-123")).willReturn(Optional.empty());
        given(orderItemRepository.saveAll(order.getItems()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados inválidos"));

        Optional<Order> result = orderService.createFromIfValid(order);

        assertTrue(result.isEmpty());
        then(orderLogger).should().invalidIgnored("order-123", "Dados inválidos");
    }

    @Test
    void shouldIgnoreOrderWhenUnexpectedRuntimeExceptionOccurs() {
        Order order = new Order();
        order.setExternalId("order-456");
        order.setItems(List.of(new OrderItem()));

        given(orderRepository.findByExternalId("order-456")).willReturn(Optional.empty());
        given(orderItemRepository.saveAll(order.getItems()))
                .willThrow(new NullPointerException("Erro simulado"));

        Optional<Order> result = orderService.createFromIfValid(order);

        assertTrue(result.isEmpty());
        then(orderLogger).should().invalidIgnored("order-456", "Erro inesperado: NullPointerException");
    }




    @Test
    void shouldThrowConflictWhenAllOrdersDuplicated() {
        Order duplicatedOrder = new Order();
        duplicatedOrder.setExternalId("123");

        when(orderRepository.findByExternalId(any())).thenReturn(Optional.of(duplicatedOrder));

        List<Order> list = List.of(duplicatedOrder, duplicatedOrder);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrders(list));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Nenhum pedido válido para processar", ex.getReason());
    }

    @Test
    void shouldReturnOrderWhenExternalIdExists() {
        given(orderRepository.findByExternalId("order-123")).willReturn(Optional.of(validOrder));

        Optional<Order> result = orderService.getOrderByExternalId("order-123");

        assertTrue(result.isPresent());
        assertEquals("order-123", result.get().getExternalId());

        then(orderLogger).should().searchByExternalId("order-123");
        then(orderLogger).should().found("1");
    }

    @Test
    void shouldReturnEmptyWhenExternalIdNotFound() {
        given(orderRepository.findByExternalId("not-found")).willReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderByExternalId("not-found");

        assertTrue(result.isEmpty());
        then(orderLogger).shouldHaveNoInteractions(); // Nenhum log em Optional.empty()
    }

    @Test
    void shouldReturnOrdersByStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        List<Order> mockOrders = List.of(validOrder);

        given(orderRepository.findByStatus(status)).willReturn(mockOrders);

        List<Order> result = orderService.getOrdersByStatus(status);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.RECEIVED, result.get(0).getStatus());
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersWithStatus() {
        OrderStatus status = OrderStatus.RECEIVED;
        given(orderRepository.findByStatus(status)).willReturn(Collections.emptyList());

        List<Order> result = orderService.getOrdersByStatus(status);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenCreateOrdersWithEmptyList() {
        List<Order> emptyList = Collections.emptyList();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.createOrders(emptyList)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lista de pedidos não pode estar vazia", exception.getReason());
    }
}
