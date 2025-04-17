package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.logger.OrderProcessorLogger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderProcessorService {

    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final OrderProcessorLogger logger;

    public OrderProcessorService(MeterRegistry meterRegistry, OrderProcessorLogger logger) {
        this.successCounter = meterRegistry.counter("order_processor_processed_total");
        this.errorCounter = meterRegistry.counter("order_processor_errors_total");
        this.processingTimer = Timer.builder("order_processor_duration_seconds")
                .description("Duração do processamento dos pedidos")
                .register(meterRegistry);
        this.logger = logger;
    }

    public List<Order> processOrders(List<Order> orders) {
        return processingTimer.record(() ->
                orders.stream()
                        .map(this::processOrderSafely)
                        .toList()
        );
    }

    private Order processOrderSafely(Order order) {
        try {
            var totalAmount = calculateTotalAmount(order);

            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.CALCULATED);
            order.setUpdateAt(LocalDateTime.now());
            order.setProcessingMessage("Pedido calculado com sucesso");

            successCounter.increment();

            logger.processedSuccessfully(order);

        } catch (Exception ex) {
            order.setStatus(OrderStatus.FAILED);
            order.setProcessingMessage("Erro: " + ex.getMessage());

            errorCounter.increment();
            logger.errorProcessing(order, ex);
        }

        return order;
    }

    private BigDecimal calculateTotalAmount(Order order) {
        return order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}