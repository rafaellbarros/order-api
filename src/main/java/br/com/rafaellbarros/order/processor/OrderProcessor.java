package br.com.rafaellbarros.order.processor;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessor {

    private final OrderRepository repository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void initMetrics() {
        successCounter = meterRegistry.counter("order_processor_processed_total");
        errorCounter = meterRegistry.counter("order_processor_errors_total");
        processingTimer = Timer.builder("order_processor_duration_seconds")
                .description("Duração do processamento dos pedidos")
                .register(meterRegistry);
    }

    private Counter successCounter;
    private Counter errorCounter;
    private Timer processingTimer;

    @Scheduled(cron = "${order.processor.schedule}")
    public void process() {
        processingTimer.record(() -> {
            List<Order> receivedOrders = repository.findByStatus(OrderStatus.RECEIVED);

            if (receivedOrders.isEmpty()) {
                log.info("Nenhum pedido com status RECEIVED encontrado.");
                return;
            }

            log.info("🔄 Iniciando processamento de {} pedidos RECEIVED", receivedOrders.size());

            List<Order> processedOrders = receivedOrders.stream()
                    .map(this::processOrderSafely)
                    .toList();

            repository.saveAll(processedOrders);

            log.info("✅ Finalizado processamento de {} pedidos.", processedOrders.size());
        });
    }

    private Order processOrderSafely(Order order) {
        try {
            BigDecimal totalAmount = calculateTotalAmount(order);
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.CALCULATED);
            order.setUpdateAt(LocalDateTime.now());
            order.setProcessingMessage("Pedido calculado com sucesso");

            successCounter.increment();

            log.info("📦 [{}] Pedido {} processado com sucesso. Total R$ {}", order.getTraceId(), order.getExternalId(), totalAmount);

        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
            order.setProcessingMessage("Erro: " + e.getMessage());
            errorCounter.increment();

            log.error("❌ [{}] Erro ao processar pedido {}: {}", order.getTraceId(), order.getExternalId(), e.getMessage(), e);
        }

        return order;
    }

    private BigDecimal calculateTotalAmount(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
