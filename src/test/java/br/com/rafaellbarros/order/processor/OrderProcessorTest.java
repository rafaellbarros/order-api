package br.com.rafaellbarros.order.processor;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.logger.OrderProcessorLogger;
import br.com.rafaellbarros.order.repository.OrderRepository;
import br.com.rafaellbarros.order.service.OrderProcessorService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderRepository repository;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setup() {
        var meterRegistry = new SimpleMeterRegistry();
        var logger = new OrderProcessorLogger();
        var orderProcessorService = new OrderProcessorService(meterRegistry, logger);
        orderProcessor = new OrderProcessor(orderProcessorService, repository, logger);
    }

    @Test
    void shouldProcessReceivedOrdersSuccessfully() {
        OrderItem item1 = new OrderItem("Camiseta", new BigDecimal("50.00"), 2);
        OrderItem item2 = new OrderItem("Tênis", new BigDecimal("100.00"), 3);

        Order order = new Order();
        order.setExternalId("EXT-123");
        order.setStatus(OrderStatus.RECEIVED);
        order.setItems(List.of(item1, item2));
        order.setTraceId(UUID.randomUUID().toString());

        given(repository.findByStatus(OrderStatus.RECEIVED)).willReturn(List.of(order));
        given(repository.saveAll(anyList())).willReturn(List.of(order));

        orderProcessor.processScheduledOrders();


        verify(repository).saveAll(argThat(orders -> {
            for (Order o : orders) {
                return o.getStatus() == OrderStatus.CALCULATED &&
                        o.getTotalAmount().compareTo(new BigDecimal("400.00")) == 0 &&
                        "Pedido calculado com sucesso".equals(o.getProcessingMessage());
            }
            return false;
        }));
    }

    @Test
    void shouldSkipProcessingWhenNoReceivedOrders() {
        given(repository.findByStatus(OrderStatus.RECEIVED)).willReturn(Collections.emptyList());

        orderProcessor.processScheduledOrders();

        verify(repository, never()).saveAll(any());
    }

    @Test
    void shouldHandleExceptionDuringProcessing() {
        OrderItem item = new OrderItem("Bugado", null, 0);

        Order order = new Order();
        order.setExternalId("FAIL-123");
        order.setStatus(OrderStatus.RECEIVED);
        order.setItems(List.of(item));
        order.setTraceId(UUID.randomUUID().toString());

        given(repository.findByStatus(OrderStatus.RECEIVED)).willReturn(List.of(order));

        orderProcessor.processScheduledOrders();

        verify(repository).saveAll(argThat(orders -> {
            for (Order o : orders) {
                return o.getStatus() == OrderStatus.FAILED &&
                        o.getProcessingMessage().startsWith("Erro:");
            }
            return false;
        }));
    }
}