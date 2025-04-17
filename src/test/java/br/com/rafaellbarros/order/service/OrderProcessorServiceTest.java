package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorServiceTest {

    private OrderProcessorService service;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        service = new OrderProcessorService(registry);
    }

    @Test
    void shouldCalculateTotalAndMarkOrderAsCalculated() {
        OrderItem item1 = new OrderItem("Produto A", new BigDecimal("10.00"), 2);
        OrderItem item2 = new OrderItem("Produto B", new BigDecimal("15.00"), 1);

        Order order = new Order();
        order.setItems(List.of(item1, item2));
        order.setTraceId("TRACE-001");
        order.setExternalId("EXT-001");

        List<Order> processed = service.processOrders(List.of(order));

        Order result = processed.get(0);

        assertEquals(OrderStatus.CALCULATED, result.getStatus());
        assertEquals(new BigDecimal("35.00"), result.getTotalAmount());
        assertEquals("Pedido calculado com sucesso", result.getProcessingMessage());
        assertNotNull(result.getUpdateAt());
    }

    @Test
    void shouldHandleErrorWhenItemPriceIsNull() {
        OrderItem item = new OrderItem("Produto Bugado", null, 2);

        Order order = new Order();
        order.setItems(List.of(item));
        order.setTraceId("TRACE-002");
        order.setExternalId("EXT-002");

        List<Order> processed = service.processOrders(List.of(order));

        Order result = processed.get(0);

        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertTrue(result.getProcessingMessage().startsWith("Erro:"));
    }
}
